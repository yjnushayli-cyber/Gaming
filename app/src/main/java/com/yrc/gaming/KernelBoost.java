package com.yrc.gaming;

import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Conservative, reversible CPU/GPU minimum-frequency adjustments.
 * No file access to the game, no hooks or thermal-limit modifications.
 */
final class KernelBoost {
    private static final String KEY_PATHS="boost4_paths";
    private static final String PREFIX="boost4_original_";
    private final SharedPreferences prefs;
    KernelBoost(SharedPreferences prefs){this.prefs=prefs;}

    private String root(String cmd)throws Exception{
        Process p=new ProcessBuilder("su","-c",cmd).redirectErrorStream(true).start();
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] buf=new byte[1024];int n;
        try(InputStream in=p.getInputStream()){
            while((n=in.read(buf))!=-1){
                if(out.size()<8000)out.write(buf,0,Math.min(n,8000-out.size()));
            }
        }
        int code=p.waitFor();
        String answer=out.toString("UTF-8").trim();
        if(code!=0)throw new IOException("su failed "+code+": "+answer);
        return answer;
    }
    private boolean whitelisted(String path){
        return path.matches("/sys/devices/system/cpu/cpufreq/policy[0-9]{1,3}/scaling_min_freq")
            ||path.matches("/sys/class/devfreq/[A-Za-z0-9_.:-]{1,100}/min_freq");
    }
    private String read(String path)throws Exception{
        if(!whitelisted(path)&&
            !path.matches("/sys/devices/system/cpu/cpufreq/policy[0-9]{1,3}/(scaling_max_freq|scaling_available_frequencies)")&&
            !path.matches("/sys/class/devfreq/[A-Za-z0-9_.:-]{1,100}/(max_freq|available_frequencies)"))
            throw new IOException("Untrusted sysfs path");
        return root("cat '"+path+"'").trim();
    }
    private long parse(String text)throws Exception{
        if(text==null||!text.trim().matches("[0-9]{1,17}"))throw new IOException("Unrecognized frequency");
        return Long.parseLong(text.trim());
    }
    private void save(String path,String current){
        if(prefs.contains(PREFIX+path))return;
        String old=prefs.getString(KEY_PATHS,"");
        prefs.edit().putString(PREFIX+path,current)
            .putString(KEY_PATHS,old.isEmpty()?path:old+"|"+path).commit();
    }
    private void write(String path,long value)throws Exception{
        if(!whitelisted(path)||value<0)throw new IOException("Unsafe sysfs write");
        root("printf '%s\\n' '"+value+"' > '"+path+"'");
    }
    private long chooseAvailable(String frequencyList,long wanted,long max){
        long best=Long.MAX_VALUE;
        for(String token:frequencyList.split("[,\\s]+")){
            if(!token.matches("[0-9]{1,17}"))continue;
            try{
                long value=Long.parseLong(token);
                if(value>=wanted&&value<=max&&value<best)best=value;
            }catch(NumberFormatException ignored){}
        }
        return best==Long.MAX_VALUE?wanted:best;
    }
    private String shortName(String p){
        if(p.contains("/cpufreq/")){
            java.util.regex.Matcher m=java.util.regex.Pattern.compile("policy\\d+").matcher(p);
            return m.find()?"CPU "+m.group():"CPU";
        }
        java.util.regex.Matcher m=java.util.regex.Pattern.compile("/devfreq/([^/]+)").matcher(p);
        return m.find()?"GPU "+m.group(1):"GPU";
    }
    private boolean boost(String path,List<String> report){
        try{
            if(!whitelisted(path))return false;
            // Idempotent: a second tap must never compound minimum frequencies.
            if(prefs.contains(PREFIX+path)){
                report.add(shortName(path)+": وضع Boost مفعّل سابقًا، لم نرفع التردد مرة أخرى.");
                return false;
            }
            String savedValue=read(path);
            long current=parse(savedValue);
            String maxName=path.replace("scaling_min_freq","scaling_max_freq").replace("/min_freq","/max_freq");
            long maximum=parse(read(maxName));
            if(maximum<=current||maximum<=0)return false;
            long goal=current+(maximum-current)*35/100;
            String listName=path.replace("scaling_min_freq","scaling_available_frequencies").replace("/min_freq","/available_frequencies");
            long target=goal;
            try{target=chooseAvailable(read(listName),goal,maximum);}
            catch(Exception ignored){}
            if(target<=current||target>maximum)return false;
            save(path,savedValue);
            write(path,target);
            long applied=parse(read(path));
            if(applied>current&&applied<=maximum){
                report.add(shortName(path)+": "+current+" → "+applied+" (رفع الحد الأدنى)");
                return true;
            }
            try{write(path,current);}catch(Exception ignored){}
            report.add(shortName(path)+": نواة الجهاز رفضت تثبيت الحد الأدنى.");
            return false;
        }catch(Exception e){
            report.add(shortName(path)+": غير مدعوم ("+safeError(e)+")");
            return false;
        }
    }
    private String safeError(Exception e){
        String text=String.valueOf(e.getMessage());
        return text.length()>110?text.substring(0,110):text;
    }
    List<String> apply(){
        ArrayList<String> notes=new ArrayList<>();
        int changed=0,cpuCount=0,gpuCount=0;
        try{
            if(!"0".equals(root("id -u")))throw new IOException("Root permission unavailable");
            String cpuDirs=root("for d in /sys/devices/system/cpu/cpufreq/policy[0-9]*; do [ -f \"$d/scaling_min_freq\" ] && echo \"$d\"; done");
            for(String path:cpuDirs.split("\\r?\\n")){
                if(!path.matches("/sys/devices/system/cpu/cpufreq/policy[0-9]{1,3}"))continue;
                if(cpuCount++>=8)break;
                if(boost(path+"/scaling_min_freq",notes))changed++;
            }
            if(cpuCount==0)notes.add("CPU: واجهات cpufreq غير موجودة على هذه النواة.");

            String gpuDirs=root("for d in /sys/class/devfreq/*; do "+
                "[ -f \"$d/min_freq\" ] || continue; "+
                "case \"$(basename \"$d\")\" in *gpu*|*GPU*|*mali*|*Mali*|*3d*) "+
                "echo \"$d\";; esac; done");
            for(String path:gpuDirs.split("\\r?\\n")){
                if(!path.matches("/sys/class/devfreq/[A-Za-z0-9_.:-]{1,100}"))continue;
                if(gpuCount++>=2)break;
                if(boost(path+"/min_freq",notes))changed++;
            }
            if(gpuCount==0)notes.add("GPU: لا توجد واجهة devfreq موثوقة؛ تركناه للنظام.");
        }catch(Exception e){notes.add("Kernel Boost: "+safeError(e));}
        notes.add("تعديلات النواة المؤكدة: "+changed);
        notes.add("لم نغيّر ترددات قصوى أو حرارة الجهاز أو ملفات PUBG.");
        return notes;
    }
    List<String> restore(){
        ArrayList<String> notes=new ArrayList<>();
        String all=prefs.getString(KEY_PATHS,"");
        if(all.isEmpty())return notes;
        StringBuilder pending=new StringBuilder();
        for(String path:all.split("\\|")){
            if(!whitelisted(path))continue;
            String old=prefs.getString(PREFIX+path,null);
            if(old==null)continue;
            try{
                long previous=parse(old);
                write(path,previous);
                if(parse(read(path))!=previous)throw new IOException("Kernel did not restore original value");
                prefs.edit().remove(PREFIX+path).commit();
                notes.add("استعادة "+shortName(path)+": تمت.");
            }catch(Exception e){
                if(pending.length()>0)pending.append("|");
                pending.append(path);
                notes.add("استعادة "+shortName(path)+": "+safeError(e));
            }
        }
        prefs.edit().putString(KEY_PATHS,pending.toString()).commit();
        return notes;
    }
}
