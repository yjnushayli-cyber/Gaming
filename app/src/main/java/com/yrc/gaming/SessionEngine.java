package com.yrc.gaming;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.display.DisplayManager;
import android.os.PowerManager;
import android.view.Display;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Session-based Android-only display controller.
 *
 * Invariants:
 *  - original state is durably saved before the FIRST modification;
 *  - a second launch cannot compound downscaling or overwrite backups;
 *  - each modified setting is read back; rejected writes are reported;
 *  - CPU, GPU, thermal, PUBG data, PUBG processes are NEVER modified;
 *  - recovery is idempotent and leaves failed-restore backups in place.
 */
final class SessionEngine {
    static final String NAME="yrc_v6_session";
    static final String ACTIVE="v6_active";
    static final String REPORT="v6_report";
    static final String PC_BACKUP="v6_pc_original";
    private static final String SCREEN_BACKUP="v6_display_original";
    private static final String KEY_PREFIX="v6_old_";
    private static final String[][] SETTINGS={
        {"system","tran_refresh_mode","120"},
        {"system","peak_refresh_rate","120.0"},
        {"system","min_refresh_rate","120.0"},
        {"global","low_power","0"}
    };

    private SessionEngine(){}
    static SharedPreferences prefs(Context c){return c.getSharedPreferences(NAME,Context.MODE_PRIVATE);}
    static boolean active(Context c){return prefs(c).getBoolean(ACTIVE,false);}
    static String lastReport(Context c){return prefs(c).getString(REPORT,"لم تبدأ جلسة بعد.");}
    private static String key(String table,String name){return KEY_PREFIX+table+"_"+name;}

    static final class Result {
        final boolean started;
        final String report;
        Result(boolean started,String report){this.started=started;this.report=report;}
    }
    private static String error(Throwable x){
        String t=String.valueOf(x.getMessage());
        return t.length()>190?t.substring(0,190):t;
    }
    private static String join(List<String> notes){
        return android.text.TextUtils.join("\n",notes);
    }
    private static void logResult(SharedPreferences p,List<String> notes){
        p.edit().putString(REPORT,join(notes)).commit();
    }

    static int batteryTempTenths(Context c){
        Intent in=c.registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return in==null?-1:in.getIntExtra("temperature",-1);
    }
    static int thermal(Context c){
        PowerManager power=(PowerManager)c.getSystemService(Context.POWER_SERVICE);
        return power==null?-1:power.getCurrentThermalStatus();
    }

    static boolean supports120(Context c){
        try{
            DisplayManager dm=(DisplayManager)c.getSystemService(Context.DISPLAY_SERVICE);
            if(dm==null)return false;
            Display display=dm.getDisplay(Display.DEFAULT_DISPLAY);
            if(display==null)return false;
            for(Display.Mode mode:display.getSupportedModes()){
                if(Math.abs(mode.getRefreshRate()-120f)<1.5f)return true;
            }
        }catch(Exception ignored){}
        return false;
    }

    private static void applySetting(SharedPreferences p,String table,String name,
                                     String target,List<String> notes)throws Exception{
        String storeKey=key(table,name);
        if(!p.contains(storeKey)){
            String current=RootRunner.get(table,name);
            if(!p.edit().putString(storeKey,current).commit())
                throw new IOException("Cannot persist backup of "+name);
        }
        String read=RootRunner.put(table,name,target);
        if(RootRunner.equalSetting(read,target)){
            notes.add("✓ "+name+": تم قبول "+read+" من جدول إعدادات النظام.");
        }else{
            notes.add("! "+name+": XOS لم يحتفظ بالقيمة المطلوبة "+
                target+" (القراءة "+read+").");
        }
    }

    static Result start(Context ctx,int renderPercent){
        ArrayList<String> notes=new ArrayList<>();
        SharedPreferences p=prefs(ctx);
        if(renderPercent!=100&&renderPercent!=90)
            return new Result(false,"وضع غير مدعوم");
        if(active(ctx))
            return new Result(false,"جلسة YRC نشطة أو استعادتها معلقة. استخدم زر الاستعادة أولًا.");
        SharedPreferences legacy=SessionManager.prefs(ctx);
        if(legacy.getBoolean(SessionManager.ACTIVE,false)||
            !legacy.getString("boost4_paths","").isEmpty()){
            return new Result(false,"توجد بقايا جلسة/ترددات من نسخة YRC سابقة. اضغط «استعادة آثار النسخ السابقة» أولًا.");
        }
        try{
            RootRunner.requireRoot();
            int bt=batteryTempTenths(ctx);
            int t=thermal(ctx);
            notes.add("YRC 6.0 • جلسة عرض ثابتة 120Hz — "+renderPercent+"%");
            notes.add("حرارة البطارية: "+(bt<0?"غير متاحة":String.format(Locale.US,"%.1f°C",bt/10.0))+
                " | Android Thermal: "+t);
            if(bt>=420||t>=PowerManager.THERMAL_STATUS_MODERATE){
                notes.add("مرفوض: الجهاز في حالة حرارة مرتفعة. لا نغير إعدادات الشاشة.");
                logResult(p,notes);
                return new Result(false,join(notes));
            }
            if(!supports120(ctx)){
                notes.add("لم نجد وضع 120Hz ضمن Display Modes الرسمية. لن ندّعي فرض معدل غير مدعوم.");
                logResult(p,notes);
                return new Result(false,join(notes));
            }

            if(!p.edit().putBoolean(ACTIVE,true)
                .putInt("v6_render_percent",renderPercent).commit())
                throw new IOException("Cannot save recovery marker");

            int confirmed=0;
            for(String[] field:SETTINGS){
                try{
                    applySetting(p,field[0],field[1],field[2],notes);
                    if(RootRunner.equalSetting(RootRunner.get(field[0],field[1]),field[2]))confirmed++;
                }catch(Exception e){notes.add("! "+field[1]+": "+error(e));}
            }
            if(renderPercent==90){
                try{
                    String original=RootRunner.run("wm size");
                    DisplaySizing.Size current=DisplaySizing.effective(original);
                    DisplaySizing.Size target=DisplaySizing.scaled(current,90);
                    if(!p.contains(SCREEN_BACKUP)&&
                        !p.edit().putString(SCREEN_BACKUP,original).commit())
                        throw new IOException("Cannot persist original screen state");
                    RootRunner.run("wm size "+target);
                    String after=RootRunner.run("wm size");
                    DisplaySizing.Size observed=DisplaySizing.effective(after);
                    if(target.equals(observed)){
                        confirmed++;
                        notes.add("✓ تخفيف أبعاد Android: "+current+" → "+observed+
                            " (نفس نسبة منظور الشاشة الحالية).");
                    }else{
                        notes.add("! XOS لم يؤكد تغيير الدقة: "+after);
                    }
                }catch(Exception e){notes.add("! تغيير الدقة: "+error(e));}
            }else{
                notes.add("دقة Android: لم تتغير؛ نحافظ على 16:10 إن كان مفعّلًا.");
            }

            if(confirmed==0){
                notes.add("لم يثبت قبول أي تغيير. نلغي الجلسة بدل الادعاء بتحسين.");
                notes.addAll(restore(ctx));
                logResult(p,notes);
                return new Result(false,join(notes));
            }
            notes.add("تم قبول "+confirmed+" تعديل/تعديلات؛ هذا ليس دليلًا على 120FPS داخل PUBG.");
            notes.add("إذا بقي FPS=60 وشاشة Android على 120Hz فالمشكلة من إخراج اللعبة وليس من هرتز الشاشة.");
            notes.add("لا تحكم في CPU/GPU أو حرارة الجهاز، ولا تعديل PUBG أو تجسس على الشبكة.");
            notes.add("سيحاول إشعار الجلسة الاستعادة عند إغلاق اللعبة؛ والاستعادة اليدوية متاحة.");
            logResult(p,notes);
            return new Result(true,join(notes));
        }catch(Exception e){
            notes.add("تعذرت الجلسة: "+error(e));
            notes.addAll(restore(ctx));
            logResult(p,notes);
            return new Result(false,join(notes));
        }
    }

    static List<String> restore(Context ctx){
        SharedPreferences p=prefs(ctx);
        ArrayList<String> notes=new ArrayList<>();
        notes.add("YRC 6.0 — استعادة الإعدادات");
        try{RootRunner.requireRoot();}
        catch(Exception e){
            notes.add("لا توجد صلاحية روت للاستعادة: "+error(e));
            logResult(p,notes);
            return notes;
        }
        String screen=p.getString(SCREEN_BACKUP,null);
        if(screen!=null){
            try{
                DisplaySizing.Size original=DisplaySizing.parseOverride(screen);
                if(original==null)RootRunner.run("wm size reset");
                else RootRunner.run("wm size "+original);
                DisplaySizing.Size current=DisplaySizing.effective(RootRunner.run("wm size"));
                DisplaySizing.Size expected=original==null?
                    DisplaySizing.parsePhysical(RootRunner.run("wm size")):original;
                if(!current.equals(expected))throw new IOException("Screen restoration not confirmed");
                p.edit().remove(SCREEN_BACKUP).commit();
                notes.add("✓ أبعاد الشاشة عادت لما كانت عليه قبل الجلسة: "+current);
            }catch(Exception e){notes.add("! فشل استعادة الأبعاد: "+error(e));}
        }
        for(String[] field:SETTINGS){
            String k=key(field[0],field[1]);
            if(!p.contains(k))continue;
            String old=p.getString(k,"null");
            try{
                String result=RootRunner.restore(field[0],field[1],old);
                if(!("null".equals(old)?"null".equals(result):
                    RootRunner.equalSetting(result,old)))
                    throw new IOException("Readback "+result+" != "+old);
                p.edit().remove(k).commit();
                notes.add("✓ تم استرجاع "+field[1]);
            }catch(Exception e){notes.add("! تعذر استرجاع "+field[1]+": "+error(e));}
        }
        boolean pending=p.contains(SCREEN_BACKUP);
        for(String[] field:SETTINGS)pending|=p.contains(key(field[0],field[1]));
        p.edit().putBoolean(ACTIVE,pending).commit();
        notes.add(pending?
            "هناك تغييرات لم يستعدها النظام. أعد المحاولة؛ لم نحذف النسخة الاحتياطية.":
            "اكتملت الاستعادة المحفوظة لهذه الجلسة.");
        logResult(p,notes);
        return notes;
    }

    /** Read-only process presence used only to detect the end of the user's game session. */
    static Boolean pubgIsRunning(){
        try{
            String s=RootRunner.run(
                "if pidof com.tencent.ig >/dev/null 2>&1; then echo YES; else echo NO; fi");
            if("YES".equals(s))return Boolean.TRUE;
            if("NO".equals(s))return Boolean.FALSE;
        }catch(Exception ignored){}
        return null;
    }

    static String diagnostic(Context ctx){
        ArrayList<String> notes=new ArrayList<>();
        notes.add("YRC — تشخيص الشاشة خارج PUBG");
        notes.add("يدعم الهاتف 120Hz: "+(supports120(ctx)?"نعم":"لا يظهر"));
        notes.add("درجة البطارية: "+(batteryTempTenths(ctx)/10.0)+"°C");
        try{
            DisplayManager dm=(DisplayManager)ctx.getSystemService(Context.DISPLAY_SERVICE);
            Display d=dm==null?null:dm.getDisplay(Display.DEFAULT_DISPLAY);
            if(d!=null){
                notes.add("تحديث شاشة التطبيق الآن: "+d.getRefreshRate()+"Hz (ليس FPS الخاص بـ PUBG)");
                StringBuilder supported=new StringBuilder();
                for(Display.Mode m:d.getSupportedModes()){
                    if(supported.length()>0)supported.append(", ");
                    supported.append((int)m.getRefreshRate());
                }
                notes.add("أوضاع الشاشة المعلنة: "+supported+"Hz");
            }
            RootRunner.requireRoot();
            notes.add("wm size: "+RootRunner.run("wm size").replace("\n"," | "));
            for(String[] field:SETTINGS){
                notes.add(field[1]+"="+RootRunner.get(field[0],field[1]));
            }
        }catch(Exception e){notes.add("أحد الفحوصات لم يكتمل: "+error(e));}
        notes.add("هذا التقرير لا يقيس FPS اللعبة، بل يحدد إذا كانت إعدادات الشاشة مقبولة.");
        return join(notes);
    }

    static String setPc16by10(Context ctx){
        try{
            if(active(ctx))return "أوقف الجلسة الحالية أولًا حتى لا تضيع أبعاد الاستعادة.";
            RootRunner.requireRoot();
            SharedPreferences p=prefs(ctx);
            String screen=RootRunner.run("wm size");
            DisplaySizing.Size original=DisplaySizing.parsePhysical(screen);
            DisplaySizing.Size target=DisplaySizing.pc16by10(original);
            if(!p.contains(PC_BACKUP)&&!p.edit().putString(PC_BACKUP,screen).commit())
                throw new IOException("Cannot save current display dimensions");
            RootRunner.run("wm size "+target);
            if(!target.equals(DisplaySizing.effective(RootRunner.run("wm size"))))
                throw new IOException("Display did not accept 16:10 override");
            return "تم تطبيق منظور Android 16:10: "+target+
                "\nالمنظور ليس تغييرًا لكاميرا PUBG/FOV. زر استعادة 16:10 يعيد الأبعاد السابقة.";
        }catch(Exception e){return "تعذر تطبيق منظور PC: "+error(e);}
    }

    static String restorePc(Context ctx){
        if(active(ctx))return "أوقف جلسة اللعب أولًا.";
        try{
            RootRunner.requireRoot();
            SharedPreferences p=prefs(ctx);
            String original=p.getString(PC_BACKUP,null);
            if(original==null)
                return "لا توجد نسخة محفوظة للمنظور الجديد. استخدم استعادة YRC القديم إذا لزم.";
            DisplaySizing.Size old=DisplaySizing.parseOverride(original);
            if(old==null)RootRunner.run("wm size reset");
            else RootRunner.run("wm size "+old);
            DisplaySizing.Size expected=old==null?
                DisplaySizing.parsePhysical(RootRunner.run("wm size")):old;
            if(!expected.equals(DisplaySizing.effective(RootRunner.run("wm size"))))
                throw new IOException("Screen size not restored");
            p.edit().remove(PC_BACKUP).commit();
            return "تمت استعادة أبعاد الشاشة السابقة: "+expected;
        }catch(Exception e){return "تعذر استعادة المنظور: "+error(e);}
    }
}
