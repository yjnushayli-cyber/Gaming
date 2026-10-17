package com.yrc.gaming.session5;

import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Controls only Android display/settings and conservative supported CPU/GPU sysfs floors. */
final class SystemTuner {
    private final SharedPreferences prefs;
    private static final String SAVED_KEYS="s5_saved_sysfs";
    private static final String SYSFS_ORIGINAL="s5_sysfs_";
    SystemTuner(SharedPreferences preferences){prefs=preferences;}

    static String shell(String command)throws Exception{
        Process process=new ProcessBuilder("su","-c",command).redirectErrorStream(true).start();
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buffer=new byte[2048];int len;
        try(InputStream stream=process.getInputStream()){
            while((len=stream.read(buffer))!=-1)
                if(out.size()<16000)out.write(buffer,0,Math.min(len,16000-out.size()));
        }
        int code=process.waitFor();
        String text=out.toString("UTF-8").trim();
        if(code!=0)throw new IOException("Root command "+code+": "+brief(text));
        return text;
    }
    static void checkRoot()throws Exception{
        if(!"0".equals(shell("id -u")))throw new IOException("امنح YRC Pro 5 صلاحية root من KernelSU Next");
    }
    static String brief(String msg){
        if(msg==null)return "unknown";
        return msg.length()>180?msg.substring(0,180):msg;
    }
    static String safeDisplaySize(String current){
        Matcher m=Pattern.compile("Physical size:\\s*(\\d+)x(\\d+)").matcher(current);
        return m.find()?m.group(1)+"x"+m.group(2):null;
    }
    private String pref(String table,String key){
        return "s5_setting_"+table+"_"+key;
    }
    private void setSetting(String table,String key,String newValue)throws Exception{
        String p=pref(table,key);
        if(!prefs.contains(p)){
            String original=shell("settings get "+table+" "+key);
            if(!prefs.edit().putString(p,original).commit())
                throw new IOException("لم أتمكن من حفظ القيمة الأصلية "+key);
        }
        shell("settings put "+table+" "+key+" "+newValue);
    }
    private void restoreSetting(String table,String key,List<String> report){
        String p=pref(table,key);
        if(!prefs.contains(p))return;
        String original=prefs.getString(p,"null");
        try{
            if(original==null||"null".equals(original)||original.isEmpty())
                shell("settings delete "+table+" "+key);
            else if(original.matches("[A-Za-z0-9_.-]{1,32}"))
                shell("settings put "+table+" "+key+" "+original);
            else throw new IOException("قيمة النسخة المحفوظة غير صالحة");
            prefs.edit().remove(p).commit();
            report.add("✓ تمت استعادة "+key);
        }catch(Exception e){report.add("! تعذرت استعادة "+key+": "+brief(e.getMessage()));}
    }
    void setPerformanceDisplay(List<String> result){
        try{
            setSetting("system","tran_refresh_mode","120");
            setSetting("system","peak_refresh_rate","120.0");
            setSetting("global","low_power","0");
            String xos=shell("settings get system tran_refresh_mode");
            String peak=shell("settings get system peak_refresh_rate");
            result.add("الشاشة: XOS="+xos+" | peak="+peak+"Hz (طلب 120Hz)");
        }catch(Exception e){result.add("الشاشة: "+brief(e.getMessage()));}
    }
    private static boolean allowedSysfs(String p){
        return p.matches("/sys/devices/system/cpu/cpufreq/policy\\d{1,3}/scaling_min_freq")
            ||p.matches("/sys/class/devfreq/[A-Za-z0-9_.:-]{1,110}/min_freq");
    }
    private static long number(String data)throws Exception{
        if(!data.matches("[0-9]{1,17}"))throw new IOException("unrecognized frequency");
        return Long.parseLong(data);
    }
    private String sysfsRead(String path)throws Exception{
        if(!allowedSysfs(path)&&
           !path.matches("/sys/devices/system/cpu/cpufreq/policy\\d{1,3}/(scaling_available_frequencies|scaling_max_freq)")&&
           !path.matches("/sys/class/devfreq/[A-Za-z0-9_.:-]{1,110}/(available_frequencies|max_freq)"))
            throw new IOException("non-allowlisted sysfs read");
        return shell("cat '"+path+"'");
    }
    private void sysfsWrite(String path,long value)throws Exception{
        if(!allowedSysfs(path)||value<0)throw new IOException("non-allowlisted sysfs write");
        shell("printf '%s\\n' '"+value+"' > '"+path+"'");
    }
    private List<Long> parseFrequencies(String data){
        ArrayList<Long> values=new ArrayList<>();
        for(String token:data.split("[,\\s]+")){
            if(token.matches("[0-9]{1,17}")){
                try{
                    long n=Long.parseLong(token);
                    if(n>0&&!values.contains(n))values.add(n);
                }catch(Exception ignored){}
            }
        }
        java.util.Collections.sort(values);
        return values;
    }
    private void applyGentleFloor(String path,int fraction,List<String> report){
        try{
            String currentStr=sysfsRead(path);
            long current=number(currentStr);
            String listPath=path.replace("scaling_min_freq","scaling_available_frequencies")
                .replace("/min_freq","/available_frequencies");
            // Do not guess minimum supported clocks. If vendor table is missing, skip.
            List<Long> available=parseFrequencies(sysfsRead(listPath));
            if(available.size()<2){
                report.add("CPU/GPU: لا تتوفر قائمة ترددات واضحة؛ تخطينا "+path);
                return;
            }
            long min=available.get(0);
            long maxAllowed=number(sysfsRead(path.replace("scaling_min_freq","scaling_max_freq")
                .replace("/min_freq","/max_freq")));
            long max=Math.min(available.get(available.size()-1),maxAllowed);
            if(max<=min)return;
            long aim=min+(max-min)*fraction/100L;
            long chosen=0;
            for(Long candidate:available){
                if(candidate>=aim && candidate<=max){chosen=candidate;break;}
            }
            if(chosen<=0)return;
            String key=SYSFS_ORIGINAL+path;
            if(prefs.contains(key)){
                // A second press never compounds a prior session's clock request.
                report.add("DVFS: هذا الملف مُفعّل مسبقًا؛ لن نرفع التردد مرة أخرى.");
                return;
            }
            if(current>=chosen){
                report.add("DVFS: التردد الأدنى الحالي "+current+" أعلى من الهدف "+chosen+
                    "، لن نرفع الحمل الحراري. تأكد من إيقاف Boost القديم إن كان مفعّلًا.");
                return;
            }
            String oldKeys=prefs.getString(SAVED_KEYS,"");
            if(!prefs.edit().putString(key,currentStr)
                .putString(SAVED_KEYS,oldKeys.isEmpty()?path:oldKeys+"|"+path).commit())
                throw new IOException("can't save kernel baseline");
            sysfsWrite(path,chosen);
            long actual=number(sysfsRead(path));
            if(actual>=chosen&&actual<=max){
                report.add("DVFS: "+label(path)+" "+current+" → "+actual+" (حد أدنى معتدل)");
            }else{
                report.add("DVFS: نظام الجهاز لم يحتفظ بالهدف؛ "+label(path)+" = "+actual);
            }
        }catch(Exception e){
            report.add("DVFS: "+label(path)+" غير مدعوم: "+brief(e.getMessage()));
        }
    }
    private static String label(String path){
        Matcher m=Pattern.compile("policy\\d+|devfreq/[^/]+").matcher(path);
        return m.find()?m.group():"CPU/GPU";
    }
    void applyGentleDvfs(List<String> report){
        try{
            String cpus=shell("for d in /sys/devices/system/cpu/cpufreq/policy[0-9]*; do [ -f \"$d/scaling_min_freq\" ] && echo \"$d/scaling_min_freq\"; done");
            int count=0;
            for(String path:cpus.split("\\r?\\n")){
                if(!path.matches("/sys/devices/system/cpu/cpufreq/policy\\d{1,3}/scaling_min_freq"))continue;
                if(count++>=8)break;
                applyGentleFloor(path,10,report);
            }
            String gpus=shell("for d in /sys/class/devfreq/*; do [ -f \"$d/min_freq\" ] || continue; "+
                "case \"$(basename \"$d\")\" in *gpu*|*GPU*|*mali*|*Mali*|*3d*) echo \"$d/min_freq\";; esac; done");
            count=0;
            for(String path:gpus.split("\\r?\\n")){
                if(!path.matches("/sys/class/devfreq/[A-Za-z0-9_.:-]{1,110}/min_freq"))continue;
                if(count++>=2)break;
                applyGentleFloor(path,14,report);
            }
        }catch(Exception e){report.add("DVFS: تعذر الوصول لبعض الواجهات: "+brief(e.getMessage()));}
    }
    void applyResolution(int percent,List<String> report)throws Exception{
        if(percent!=95&&percent!=90&&percent!=85)
            throw new IOException("ملف دقة غير مدعوم");
        String current=shell("wm size");
        String original=safeDisplaySize(current);
        if(original==null)throw new IOException("لم أتمكن من معرفة أبعاد الشاشة الفعلية");
        int a=Integer.parseInt(original.split("x")[0]);
        int b=Integer.parseInt(original.split("x")[1]);
        int w=2*((int)Math.round(a*percent/100.0)/2);
        int h=2*((int)Math.round(b*percent/100.0)/2);
        if(w<700||h<1000)throw new IOException("ناتج تصغير الشاشة أصغر من الحد الآمن");
        if(!prefs.contains("s5_original_wm")){
            if(!prefs.edit().putString("s5_original_wm",current).commit())
                throw new IOException("تعذر حفظ مقاس الشاشة الأصلي");
        }
        // This intentionally does NOT modify game config, renderer or package files.
        shell("wm size "+w+"x"+h);
        String verified=shell("wm size");
        if(verified.contains("Override size: "+w+"x"+h))
            report.add("العرض: "+w+" × "+h+" ("+percent+"% من أبعاد Android)");
        else report.add("العرض: طلب "+w+"x"+h+" لكن النظام لم يؤكد التنفيذ.");
        report.add("تقليل دقة Android لا يضمن تغيير دقة الرندر الداخلية للعبة.");
    }
    void restore(List<String> report){
        // Restore modified kernel nodes in reverse order, with readback verification.
        String paths=prefs.getString(SAVED_KEYS,"");
        StringBuilder failed=new StringBuilder();
        for(String path:paths.split("\\|")){
            if(!allowedSysfs(path))continue;
            String key=SYSFS_ORIGINAL+path;
            String original=prefs.getString(key,null);
            if(original==null)continue;
            try{
                long freq=number(original);
                sysfsWrite(path,freq);
                if(number(sysfsRead(path))!=freq)
                    throw new IOException("the kernel did not retain original frequency");
                prefs.edit().remove(key).commit();
                report.add("✓ تمت استعادة "+label(path));
            }catch(Exception e){
                if(failed.length()>0)failed.append("|");
                failed.append(path);
                report.add("! تعذرت استعادة "+label(path)+": "+brief(e.getMessage()));
            }
        }
        prefs.edit().putString(SAVED_KEYS,failed.toString()).commit();
        restoreSetting("system","tran_refresh_mode",report);
        restoreSetting("system","peak_refresh_rate",report);
        restoreSetting("global","low_power",report);
        String old=prefs.getString("s5_original_wm",null);
        if(old!=null){
            try{
                Matcher m=Pattern.compile("Override size:\\s*(\\d+x\\d+)").matcher(old);
                if(m.find())shell("wm size "+m.group(1));
                else shell("wm size reset");
                prefs.edit().remove("s5_original_wm").commit();
                report.add("✓ تمت استعادة مقاس الشاشة قبل الجلسة");
            }catch(Exception e){report.add("! تعذر إرجاع الشاشة: "+brief(e.getMessage()));}
        }
    }
    void factoryDisplayReset(List<String> report){
        try{
            checkRoot();
            shell("wm size reset");
            prefs.edit().remove("s5_original_wm").commit();
            report.add("تم طلب أبعاد الشاشة المصنعية: "+shell("wm size"));
        }catch(Exception e){report.add("تعذرت إعادة أبعاد المصنع: "+brief(e.getMessage()));}
    }
}
