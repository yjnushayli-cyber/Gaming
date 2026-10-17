package com.yrc.gaming;

import android.content.SharedPreferences;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Safe reversible local Wi-Fi latency adjustments.
 * Does not modify PUBG, intercept packets, spoof hits or access game processes.
 * Mobile networking and server hit registration are outside this app's control.
 */
final class NetworkAssist {
    private static final String WIFI_FORCED="clean_wifi_latency_on";
    private static final String WIFI_INTERFACE="hitassist_wifi_interface";
    private static final String WIFI_POWER_SAVE_OLD="hitassist_wifi_ps_old";
    private final SharedPreferences prefs;
    NetworkAssist(SharedPreferences prefs){this.prefs=prefs;}

    private String root(String cmd)throws Exception{
        Process p=new ProcessBuilder("su","-c",cmd).redirectErrorStream(true).start();
        ByteArrayOutputStream out=new ByteArrayOutputStream();
        byte[] bytes=new byte[1024];int n;
        try(InputStream in=p.getInputStream()){
            while((n=in.read(bytes))!=-1){
                if(out.size()<8000)out.write(bytes,0,Math.min(n,8000-out.size()));
            }
        }
        int status=p.waitFor();
        String value=out.toString("UTF-8").trim();
        if(status!=0)throw new IOException("root exit="+status+" "+value);
        return value;
    }
    private static boolean rejected(String text){
        String lower=text.toLowerCase(Locale.US);
        return lower.contains("command execution failed")||
            lower.contains("failed")||lower.contains("error")||
            lower.contains("exception")||lower.contains("not supported")||
            lower.contains("unknown command")||lower.contains("invalid")||
            lower.contains("usage:")||lower.contains("permission denied");
    }
    private static boolean safeIface(String name){
        return name!=null&&(name.matches("wlan[0-9]{1,2}")||name.matches("wifi[0-9]{1,2}"));
    }
    private boolean iwExists(){
        try{
            return "FOUND".equals(root("if command -v iw >/dev/null 2>&1; then echo FOUND; else echo ABSENT; fi"));
        }catch(Exception ignored){return false;}
    }

    List<String> enable(String wifiInterface){
        ArrayList<String> result=new ArrayList<>();
        try{
            if(!"0".equals(root("id -u")))throw new IOException("KernelSU root not available");

            // AOSP-supported option: Wi-Fi controller prioritizes low packet latency.
            String output=root("cmd wifi force-low-latency-mode enabled");
            if(rejected(output)){
                result.add("Wi-Fi Low Latency: النظام رفض الطلب: "+trim(output));
            }else{
                prefs.edit().putBoolean(WIFI_FORCED,true).commit();
                result.add("Wi-Fi Low Latency: تم إرسال أمر النظام بنجاح، لكن فعالية المودم تعتمد على دعم الجهاز.");
            }

            // Optional vendor-level power-save override, only when iw is installed and
            // active interface is an ordinary validated Wi-Fi STA interface.
            if(!safeIface(wifiInterface)){
                result.add("Wi-Fi Power Save: لم أحدد واجهة wlan موثوقة؛ لا تغيير.");
                return result;
            }
            if(!iwExists()){
                result.add("Wi-Fi Power Save: أمر iw غير متوفر بالنظام؛ اعتمدنا على Android Low Latency.");
                return result;
            }
            String existing= root("iw dev '"+wifiInterface+"' get power_save");
            if(rejected(existing)){
                result.add("Wi-Fi Power Save: لا يدعم تعريف الشبكة قراءة الحالة.");
                return result;
            }
            String normalized=existing.toLowerCase(Locale.US);
            boolean on=normalized.contains("power save: on");
            boolean off=normalized.contains("power save: off");
            if(!on&&!off){
                result.add("Wi-Fi Power Save: حالة التعريف غير معروفة؛ لم نغيّرها.");
                return result;
            }

            if(prefs.contains(WIFI_POWER_SAVE_OLD)){
                String savedIface=prefs.getString(WIFI_INTERFACE,"");
                if(!wifiInterface.equals(savedIface)){
                    result.add("Wi-Fi Power Save: توجد حالة محفوظة لشبكة أخرى؛ استخدم الاستعادة أولًا.");
                    return result;
                }
                if(off){
                    result.add("Wi-Fi Power Save: منخفض بالفعل والتعديل السابق ما زال ساريًا.");
                    return result;
                }
            }
            if(off){
                result.add("Wi-Fi Power Save: معطّل أصلًا من نظام الجهاز؛ لم نغيّره.");
                return result;
            }

            // Save original before altering driver state. Can restore on any later run.
            if(!prefs.contains(WIFI_POWER_SAVE_OLD)){
                prefs.edit().putString(WIFI_INTERFACE,wifiInterface)
                    .putString(WIFI_POWER_SAVE_OLD,"on").commit();
            }
            String out=root("iw dev '"+wifiInterface+"' set power_save off");
            String verify=root("iw dev '"+wifiInterface+"' get power_save");
            if(!rejected(out)&&verify.toLowerCase(Locale.US).contains("power save: off")){
                result.add("Wi-Fi Power Save: تم إيقاف توفير طاقة شريحة Wi-Fi على "+wifiInterface+".");
            }else{
                result.add("Wi-Fi Power Save: التعريف لم يؤكد التغيير؛ استخدم الاستعادة عند الانتهاء.");
            }
        }catch(Exception e){
            result.add("تحسين الشبكة: تعذر تطبيق جزء منه: "+trim(e.getMessage()));
        }
        result.add("هذا قد يقلّل تذبذب اتصال Wi-Fi، لكنه لا يغيّر Hit Registration بسيرفر PUBG.");
        return result;
    }

    List<String> restore(){
        ArrayList<String> result=new ArrayList<>();
        String original=prefs.getString(WIFI_POWER_SAVE_OLD,null);
        String iface=prefs.getString(WIFI_INTERFACE,null);
        if(original!=null){
            if(!safeIface(iface)||!("on".equals(original)||"off".equals(original))){
                result.add("Wi-Fi Driver: النسخة المحفوظة غير صالحة؛ لن نطبق أمرًا غير موثوق.");
            }else{
                try{
                    if(!iwExists())throw new IOException("iw غير متوفر");
                    String out=root("iw dev '"+iface+"' set power_save "+original);
                    String current=root("iw dev '"+iface+"' get power_save").toLowerCase(Locale.US);
                    if(rejected(out)||!current.contains("power save: "+original))
                        throw new IOException("التعريف لم يؤكد الاستعادة");
                    prefs.edit().remove(WIFI_POWER_SAVE_OLD).remove(WIFI_INTERFACE).commit();
                    result.add("Wi-Fi Power Save: استعيد للوضع السابق "+original+".");
                }catch(Exception e){
                    result.add("Wi-Fi Power Save: لم تكتمل الاستعادة: "+trim(e.getMessage()));
                }
            }
        }
        if(prefs.getBoolean(WIFI_FORCED,false)){
            try{
                String out=root("cmd wifi force-low-latency-mode disabled");
                if(rejected(out))throw new IOException(out);
                prefs.edit().remove(WIFI_FORCED).commit();
                result.add("Wi-Fi Low Latency: تم إلغاء وضع التأخير المنخفض.");
            }catch(Exception e){
                result.add("Wi-Fi Low Latency: تعذر إلغاء الوضع: "+trim(e.getMessage()));
            }
        }
        if(result.isEmpty())result.add("لم نجد تغييرات محفوظة للشبكة تحتاج استعادة.");
        return result;
    }
    private static String trim(String input){
        if(input==null)return "غير معروف";
        return input.length()>180?input.substring(0,180):input;
    }
}
