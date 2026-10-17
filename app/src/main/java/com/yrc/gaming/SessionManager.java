package com.yrc.gaming;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.LinkProperties;
import android.os.PowerManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One-click gameplay session. All writes are to Android settings or validated
 * sysfs, never PUBG's files, process, memory, graphics configs or network packets.
 * The session controller stores state before writes so it can be restored later.
 */
final class SessionManager {
    static final String PREFS="yrc_clean_30";
    static final String ACTIVE="session5_active";
    static final String SESSION_REPORT="session5_last_report";
    static final String DND_OLD="session5_dnd_old";
    static final String DND_WANTED="session5_dnd_opt_in";

    private SessionManager(){}

    static SharedPreferences prefs(Context context){
        return context.getSharedPreferences(PREFS,Context.MODE_PRIVATE);
    }

    static boolean active(Context context){
        return prefs(context).getBoolean(ACTIVE,false);
    }

    static String root(String command)throws Exception{
        Process p=new ProcessBuilder("su","-c",command)
            .redirectErrorStream(true).start();
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        byte[] buffer=new byte[1024];int len;
        try(InputStream in=p.getInputStream()){
            while((len=in.read(buffer))!=-1){
                if(output.size()<16000)
                    output.write(buffer,0,Math.min(len,16000-output.size()));
            }
        }
        int exit=p.waitFor();
        String result=output.toString("UTF-8").trim();
        if(exit!=0)throw new IOException("root code "+exit+": "+result);
        return result;
    }

    private static int batteryTenths(Context ctx){
        Intent battery=ctx.registerReceiver(null,
            new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return battery==null?-1:battery.getIntExtra("temperature",-1);
    }
    private static int thermal(Context ctx){
        PowerManager manager=(PowerManager)ctx.getSystemService(Context.POWER_SERVICE);
        return manager==null?0:manager.getCurrentThermalStatus();
    }
    private static String key(String table,String option){
        return "clean_original_"+table+"_"+option;
    }
    private static void setting(SharedPreferences p,String table,String name,
                                 String value)throws Exception{
        if(!name.matches("[a-z_]{2,40}")||!value.matches("[0-9.]{1,12}")||
            !("system".equals(table)||"global".equals(table)))
            throw new IOException("Setting rejected");
        String save=key(table,name);
        if(!p.contains(save)){
            String old=root("settings get "+table+" "+name);
            if(!p.edit().putString(save,old).commit())
                throw new IOException("Could not save original "+name);
        }
        root("settings put "+table+" "+name+" "+value);
    }
    private static void restoreSetting(SharedPreferences p,String table,String name,
                                        List<String> logs){
        String save=key(table,name);
        if(!p.contains(save))return;
        String old=p.getString(save,"null");
        try{
            if(old==null||old.isEmpty()||"null".equals(old))
                root("settings delete "+table+" "+name);
            else if(old.matches("[A-Za-z0-9._-]{1,36}"))
                root("settings put "+table+" "+name+" "+old);
            else throw new IOException("Invalid saved setting value");
            p.edit().remove(save).commit();
            logs.add("استعادة "+name+": تمت.");
        }catch(Exception e){logs.add("تعذرت استعادة "+name+": "+shortError(e));}
    }
    private static String physicalScreen()throws Exception{
        String screen=root("wm size");
        Matcher match=Pattern.compile("Physical size:\\s*(\\d+)x(\\d+)").matcher(screen);
        if(!match.find())throw new IOException("Physical display size unknown: "+screen);
        return match.group(1)+"x"+match.group(2);
    }
    private static void saveScreen(SharedPreferences p)throws Exception{
        if(!p.contains("clean_original_wm")){
            if(!p.edit().putString("clean_original_wm",root("wm size")).commit())
                throw new IOException("Could not persist original screen size");
        }
    }
    private static void resizeForGaming(SharedPreferences p,int percent,
                                         ArrayList<String> notes)throws Exception{
        String[] dims=physicalScreen().split("x");
        int a=Integer.parseInt(dims[0]),b=Integer.parseInt(dims[1]);
        int shortSide=Math.min(a,b),longSide=Math.max(a,b);
        String ratio=p.getString("clean_pc_ratio","");
        // Preserve chosen PC aspect while shrinking pixel count if preferred.
        int width=2*(Math.round(shortSide*percent/100f)/2);
        int height;
        if("16:9".equals(ratio))height=2*((int)Math.round(width*16.0/9.0)/2);
        else if("16:10".equals(ratio))height=2*((int)Math.round(width*16.0/10.0)/2);
        else height=2*(Math.round(longSide*percent/100f)/2);
        if(width<800||height<1300||width>=height)
            throw new IOException("Requested display size not suitable");
        String desired=width+"x"+height;
        saveScreen(p);
        root("wm size "+desired);
        String actual=root("wm size");
        if(actual.contains("Override size: "+desired))
            notes.add("تم تخفيض دقة Android إلى "+desired+
                (ratio.isEmpty()?"":" مع الحفاظ على منظور PC "+ratio)+".");
        else notes.add("لم يؤكد XOS دقة العرض المقترحة: "+desired+".");
    }

    static final class StartResult{
        final boolean started;
        final String report;
        StartResult(boolean started,String report){
            this.started=started;this.report=report;
        }
    }

    static StartResult begin(Context ctx,int percent){
        ArrayList<String> notes=new ArrayList<>();
        SharedPreferences p=prefs(ctx);
        if(percent!=85&&percent!=90)return new StartResult(false,"ملف الأداء غير صالح");
        if(p.getBoolean(ACTIVE,false))
            return new StartResult(false,"هناك جلسة سابقة لم تُستعد إعداداتها بعد. اضغط الاستعادة أولًا.");
        try{
            if(!"0".equals(root("id -u")))throw new IOException("KernelSU root denied");
            // Previous YRC versions raised minimum CPU/GPU frequencies. Undo them
            // before starting a session: high DVFS floors can worsen sustained heat.
            try{
                notes.addAll(new KernelBoost(p).restore());
            }catch(Exception e){notes.add("استعادة الترددات السابقة: "+shortError(e));}
            if(!p.getString("boost4_paths","").isEmpty()){
                notes.add("بقيت إعدادات ترددات قديمة لم تؤكد النواة استعادتها؛ أوقف الجلسة وأعد تشغيل الهاتف عند الحاجة.");
                return new StartResult(false,android.text.TextUtils.join("\n",notes));
            }
            int battery=batteryTenths(ctx);
            int thermalState=thermal(ctx);
            if(thermalState>=PowerManager.THERMAL_STATUS_MODERATE||
               (battery>=400)){
                notes.add("الجهاز دافئ/حماية الحرارة نشطة ("+
                    (battery<0?"غير معروف":(battery/10.0)+"°C")+
                    "). لن يبدأ أي وضع أداء قبل أن يبرد.");
                notes.add("تمت محاولة إلغاء Boost القديم إن كان محفوظًا.");
                return new StartResult(false,android.text.TextUtils.join("\n",notes));
            }
            // Mark active BEFORE touching any settings; fail and recover safely.
            if(!p.edit().putBoolean(ACTIVE,true)
                .putInt("session5_percent",percent).commit())
                throw new IOException("Could not persist session recovery state");
            notes.add("START COMPETITIVE SESSION — "+percent+"%");

            try{
                setting(p,"system","tran_refresh_mode","120");
                setting(p,"system","peak_refresh_rate","120.0");
                setting(p,"global","low_power","0");
                notes.add("120Hz: "+root("settings get system tran_refresh_mode")+
                    " | peak "+root("settings get system peak_refresh_rate")+"Hz");
            }catch(Exception e){notes.add("إعدادات الشاشة: "+shortError(e));}

            // Explicitly do NOT raise CPU/GPU minimum clocks, at any battery
            // temperature. Android's own DVFS governor needs headroom to cool.
            notes.add("CPU/GPU: ألغينا رفع الحد الأدنى للترددات؛ التحكم يعود لنظام XOS لتقليل السخونة.");

            try{resizeForGaming(p,percent,notes);}
            catch(Exception e){notes.add("تحسين دقة العرض: "+shortError(e));}

            try{
                ConnectivityManager cm=(ConnectivityManager)
                    ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
                Network n=cm==null?null:cm.getActiveNetwork();
                NetworkCapabilities caps=n==null||cm==null?null:cm.getNetworkCapabilities(n);
                if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                    LinkProperties link=cm.getLinkProperties(n);
                    String iface=link==null?"":link.getInterfaceName();
                    notes.addAll(new NetworkAssist(p).enable(iface));
                }else{
                    notes.add("لم نغيّر الشبكة: لا يوجد اتصال Wi-Fi فعّال.");
                }
            }catch(Exception e){notes.add("Wi-Fi: "+shortError(e));}

            if(p.getBoolean(DND_WANTED,false)){
                NotificationManager nm=(NotificationManager)
                    ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                if(nm!=null&&nm.isNotificationPolicyAccessGranted()){
                    try{
                        if(!p.contains(DND_OLD))
                            p.edit().putInt(DND_OLD,nm.getCurrentInterruptionFilter()).commit();
                        nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
                        notes.add("عدم الإزعاج: تم طلب وضع الإشعارات ذات الأولوية فقط.");
                    }catch(Exception e){notes.add("عدم الإزعاج: "+shortError(e));}
                }else notes.add("عدم الإزعاج: لم تتوفر موافقة النظام، لم نغيّر الإشعارات.");
            }

            notes.add("لا رفع إجباري لترددات CPU/GPU ولا تعديل لملفات PUBG أو تعطيل للحرارة.");
            notes.add("الاستعادة التلقائية تعتمد على استمرار خدمة جلسة اللعب؛ الاستعادة اليدوية متاحة دائمًا.");
            String report=android.text.TextUtils.join("\n",notes);
            p.edit().putString(SESSION_REPORT,report).commit();
            return new StartResult(true,report);
        }catch(Exception e){
            notes.add("تعذر بدء الجلسة: "+shortError(e));
            // Do not leave a half-applied root configuration without attempting recovery.
            notes.addAll(restore(ctx));
            return new StartResult(false,android.text.TextUtils.join("\n",notes));
        }
    }

    static List<String> restore(Context ctx){
        SharedPreferences p=prefs(ctx);
        ArrayList<String> logs=new ArrayList<>();
        logs.add("استعادة إعدادات YRC");
        try{logs.addAll(new KernelBoost(p).restore());}
        catch(Exception e){logs.add("CPU/GPU: "+shortError(e));}
        String screen=p.getString("clean_original_wm",null);
        if(screen!=null){
            try{
                Matcher match=Pattern.compile("Override size:\\s*(\\d+x\\d+)").matcher(screen);
                if(match.find())root("wm size "+match.group(1));
                else root("wm size reset");
                p.edit().remove("clean_original_wm")
                    .remove("clean_pc_ratio").commit();
                logs.add("أبعاد الشاشة: تمت الاستعادة.");
            }catch(Exception e){logs.add("الشاشة: "+shortError(e));}
        }
        restoreSetting(p,"system","tran_refresh_mode",logs);
        restoreSetting(p,"system","peak_refresh_rate",logs);
        restoreSetting(p,"system","min_refresh_rate",logs);
        restoreSetting(p,"global","low_power",logs);
        try{logs.addAll(new NetworkAssist(p).restore());}
        catch(Exception e){logs.add("الشبكة: "+shortError(e));}

        if(p.contains(DND_OLD)){
            try{
                NotificationManager nm=(NotificationManager)
                    ctx.getSystemService(Context.NOTIFICATION_SERVICE);
                if(nm==null||!nm.isNotificationPolicyAccessGranted())
                    throw new IOException("صلاحية عدم الإزعاج غير متاحة");
                int old=p.getInt(DND_OLD,NotificationManager.INTERRUPTION_FILTER_ALL);
                nm.setInterruptionFilter(old);
                p.edit().remove(DND_OLD).commit();
                logs.add("الإشعارات: تم إرجاع وضع عدم الإزعاج السابق.");
            }catch(Exception e){logs.add("الإشعارات: "+shortError(e));}
        }
        boolean pending=p.contains("clean_original_wm")||
            p.contains(key("system","tran_refresh_mode"))||
            p.contains(key("system","peak_refresh_rate"))||
            p.contains(key("system","min_refresh_rate"))||
            p.contains(key("global","low_power"))||
            p.contains(DND_OLD)||
            !p.getString("boost4_paths","").isEmpty()||
            p.contains("hitassist_wifi_ps_old")||
            p.getBoolean("clean_wifi_latency_on",false);
        p.edit().putBoolean(ACTIVE,pending)
            .putString(SESSION_REPORT,android.text.TextUtils.join("\n",logs)).commit();
        if(pending)logs.add("بعض القيم لم تستعد بعد؛ افتح YRC وكرر الاستعادة.");
        else logs.add("انتهت الجلسة. جميع الإعدادات المسجلة أعيدت أو لم تتغير.");
        return logs;
    }

    /** null means root status could not be checked, NOT that PUBG was closed. */
    static Boolean pubgRunning(){
        try{
            String answer=root("if pidof com.tencent.ig >/dev/null 2>&1; "+
                "then echo RUNNING; else echo STOPPED; fi").trim();
            if("RUNNING".equals(answer))return Boolean.TRUE;
            if("STOPPED".equals(answer))return Boolean.FALSE;
        }catch(Exception ignored){}
        return null;
    }

    private static String shortError(Exception e){
        String s=String.valueOf(e.getMessage());
        return s.length()>150?s.substring(0,150):s;
    }
}
