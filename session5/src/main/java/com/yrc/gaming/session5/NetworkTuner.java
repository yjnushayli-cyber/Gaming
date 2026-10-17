package com.yrc.gaming.session5;

import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.LinkProperties;
import android.content.Context;
import java.util.List;
import java.util.Locale;

/** Network latency profile limited to the phone's own Wi-Fi stack. */
final class NetworkTuner {
    private final SharedPreferences prefs;
    private final Context context;
    NetworkTuner(Context ctx,SharedPreferences prefs){
        this.context=ctx;
        this.prefs=prefs;
    }
    private static boolean rejected(String output){
        String text=output.toLowerCase(Locale.US);
        return text.contains("not supported")||text.contains("error")||
            text.contains("failed")||text.contains("unknown command")||
            text.contains("exception")||text.contains("usage:")||
            text.contains("permission denied")||text.contains("invalid");
    }
    private boolean hasWifi(){
        ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm==null)return false;
        Network n=cm.getActiveNetwork();
        NetworkCapabilities caps=n==null?null:cm.getNetworkCapabilities(n);
        return caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI);
    }
    private String activeInterface(){
        ConnectivityManager cm=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(cm==null)return null;
        Network n=cm.getActiveNetwork();
        LinkProperties lp=n==null?null:cm.getLinkProperties(n);
        String name=lp==null?null:lp.getInterfaceName();
        return name!=null&&name.matches("(wlan|wifi)[0-9]{1,2}")?name:null;
    }
    void apply(List<String> result){
        if(!hasWifi()){
            result.add("Wi-Fi: غير متصل الآن؛ لم نغيّر إعدادات الشبكة.");
            return;
        }
        try{
            String out=SystemTuner.shell("cmd wifi force-low-latency-mode enabled");
            if(rejected(out)){
                result.add("Wi-Fi Low Latency: رفض XOS الطلب: "+SystemTuner.brief(out));
            }else{
                prefs.edit().putBoolean("s5_wifi_low_latency",true).commit();
                result.add("Wi-Fi Low Latency: تم إرسال طلب التفعيل إلى XOS.");
            }
        }catch(Exception e){
            result.add("Wi-Fi Low Latency: غير متاح: "+SystemTuner.brief(e.getMessage()));
        }
        String iface=activeInterface();
        if(iface==null){
            result.add("Wi-Fi driver power save: لا توجد واجهة موثوقة لإجراء التغيير.");
            return;
        }
        try{
            if(!"YES".equals(SystemTuner.shell("command -v iw >/dev/null 2>&1 && echo YES || echo NO"))){
                result.add("Wi-Fi driver: أمر iw غير متاح، لم يُغيّر توفير الطاقة.");
                return;
            }
            String old=SystemTuner.shell("iw dev '"+iface+"' get power_save").toLowerCase(Locale.US);
            if(rejected(old)){
                result.add("Wi-Fi driver: النظام لا يسمح بقراءة وضع توفير الطاقة.");
                return;
            }
            boolean wasOn=old.contains("power save: on");
            boolean wasOff=old.contains("power save: off");
            if(!wasOn&&!wasOff){
                result.add("Wi-Fi driver: حالة توفير الطاقة غير معروفة؛ لم نغيّرها.");
                return;
            }
            if(prefs.contains("s5_wifi_ps_original")){
                result.add("Wi-Fi driver: تعديل جلسة سابق محفوظ، لا نعيد حفظ الأصل.");
                return;
            }
            if(wasOff){
                result.add("Wi-Fi driver: توفير الطاقة معطّل أصلاً، لم نغيّر شيئًا.");
                return;
            }
            if(!prefs.edit().putString("s5_wifi_interface",iface)
                .putString("s5_wifi_ps_original","on").commit())
                throw new java.io.IOException("تعذّر حفظ الوضع الأصلي للشبكة");
            String out=SystemTuner.shell("iw dev '"+iface+"' set power_save off");
            String verify=SystemTuner.shell("iw dev '"+iface+"' get power_save").toLowerCase(Locale.US);
            if(!rejected(out)&&verify.contains("power save: off"))
                result.add("Wi-Fi driver: أوقفنا توفير الطاقة في "+iface+" لتقليل احتمالات تأخر البيانات.");
            else result.add("Wi-Fi driver: لم يتأكد تغيير توفير الطاقة؛ متاح زر الاستعادة.");
        }catch(Exception e){
            result.add("Wi-Fi driver: لم نتمكن من إجراء تغيير: "+SystemTuner.brief(e.getMessage()));
        }
        result.add("هذه إعدادات Wi-Fi فقط؛ لا تتحكم في خادم PUBG أو احتساب الإصابات.");
    }
    void restore(List<String> result){
        String iface=prefs.getString("s5_wifi_interface",null);
        String old=prefs.getString("s5_wifi_ps_original",null);
        if(old!=null){
            if(iface==null||!iface.matches("(wlan|wifi)[0-9]{1,2}")||
                !("on".equals(old)||"off".equals(old))){
                result.add("Wi-Fi driver: البيانات المحفوظة غير صالحة، لم ننفذ أوامر.");
            }else{
                try{
                    String output=SystemTuner.shell("iw dev '"+iface+"' set power_save "+old);
                    String confirmed=SystemTuner.shell("iw dev '"+iface+"' get power_save").toLowerCase(Locale.US);
                    if(rejected(output)||!confirmed.contains("power save: "+old))
                        throw new java.io.IOException("لم يؤكد التعريف الاستعادة");
                    prefs.edit().remove("s5_wifi_interface").remove("s5_wifi_ps_original").commit();
                    result.add("✓ أعدنا توفير طاقة Wi-Fi إلى "+old);
                }catch(Exception e){
                    result.add("! تعذرت استعادة Wi-Fi driver: "+SystemTuner.brief(e.getMessage()));
                }
            }
        }
        if(prefs.getBoolean("s5_wifi_low_latency",false)){
            try{
                String output=SystemTuner.shell("cmd wifi force-low-latency-mode disabled");
                if(rejected(output))throw new java.io.IOException(output);
                prefs.edit().remove("s5_wifi_low_latency").commit();
                result.add("✓ ألغينا طلب Wi-Fi Low Latency.");
            }catch(Exception e){
                result.add("! تعذّر إلغاء Wi-Fi Low Latency: "+SystemTuner.brief(e.getMessage()));
            }
        }
    }
}
