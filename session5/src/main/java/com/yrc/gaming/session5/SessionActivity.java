package com.yrc.gaming.session5;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * YRC Pro 5: independent, user-triggered game preparation.
 * No screen monitoring, hidden overlays, game hooks, game files, injection or anti-cheat bypass.
 */
public class SessionActivity extends Activity {
    private static final int BG=Color.rgb(10,16,27);
    private static final int CARD=Color.rgb(21,34,51);
    private static final int ACCENT=Color.rgb(80,224,190);
    private static final int SOFT=Color.rgb(212,228,233);
    private static final String PUBG="com.tencent.ig";

    private final ExecutorService work=Executors.newSingleThreadExecutor();
    private final AtomicBoolean busy=new AtomicBoolean(false);
    private SharedPreferences prefs;
    private TextView status;
    private String lastMessage="جاهز. هذه نسخة مستقلة عن YRC Gaming Pro 4.x.";
    private RadioGroup resolution,aspect;
    private CheckBox gentleCpu,dnd;

    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);
        prefs=getSharedPreferences("yrc_pro5_session_prefs",MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        LinearLayout screen=new LinearLayout(this);
        screen.setOrientation(LinearLayout.VERTICAL);
        screen.setBackgroundColor(BG);
        screen.setOnApplyWindowInsetsListener((view,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
            screen.setPadding(0,bars.top,0,bars.bottom);
            return insets;
        });
        setContentView(screen);
        screen.requestApplyInsets();

        status=new TextView(this);
        status.setText("جاهز • اضغط هنا لعرض تفاصيل آخر إجراء");
        status.setTextColor(SOFT);
        status.setTextSize(14);
        status.setMaxLines(2);
        status.setEllipsize(android.text.TextUtils.TruncateAt.END);
        status.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        status.setPadding(dp(17),dp(15),dp(17),dp(15));
        GradientDrawable statusBg=new GradientDrawable();
        statusBg.setColor(CARD);
        statusBg.setCornerRadius(dp(16));
        statusBg.setStroke(dp(1),ACCENT);
        status.setBackground(statusBg);
        LinearLayout.LayoutParams statLp=new LinearLayout.LayoutParams(-1,-2);
        statLp.setMargins(dp(15),dp(13),dp(15),dp(8));
        screen.addView(status,statLp);
        status.setOnClickListener(v->new AlertDialog.Builder(this)
            .setTitle("التفاصيل").setMessage(lastMessage)
            .setPositiveButton("إغلاق",null).show());

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        LinearLayout body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        body.setPadding(dp(18),dp(15),dp(18),dp(36));
        scroll.addView(body);
        screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));

        section(body,"YRC PRO 5.0");
        note(body,"INDEPENDENT GAMING SESSION  •  INFINIX GT30 PRO");
        note(body,"تطبيق منفصل عن YRC 4.3: أربع تغييرات عملية في جلسة واحدة، بدون أدوات مراقبة أو تغيير ملفات PUBG.");

        primary(body,"< بدء جلسة PUBG المحسّنة >",this::confirmStart);
        secondary(body,"استعادة كل التغييرات بعد اللعب",this::confirmRestore);
        note(body,"عند الرجوع إلى هذا التطبيق من PUBG سيحاول استعادة التغييرات تلقائيًا. ويمكنك دائمًا استخدام زر الاستعادة يدويًا.");

        section(body,"01  اختيار ملف اللعب");
        note(body,"تقليل دقة العرض يستهدف تخفيف حمل GPU. ابدأ بملف Competitive 90%. قد تختلف الفائدة حسب طريقة PUBG في الرسم.");
        resolution=new RadioGroup(this);
        resolution.setOrientation(LinearLayout.VERTICAL);
        resolution.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        radio(resolution,"Competitive 90% — الأداء المتوازن",90,true);
        radio(resolution,"Ultra Competitive 85% — تخفيف أكبر",85,false);
        radio(resolution,"Visual 95% — وضوح أعلى",95,false);
        body.addView(resolution);

        section(body,"02  منظور PC");
        note(body,"يمكن تجربة 16:9 أو 16:10 على مستوى شاشة Android؛ هذا لا يوسع مجال رؤية الكاميرا داخل PUBG أو يضمن ظهور جسم اللاعب كاملًا.");
        aspect=new RadioGroup(this);
        aspect.setOrientation(LinearLayout.VERTICAL);
        aspect.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        radio(aspect,"أبعاد الهاتف الأصلية (أنسب للثبات)",1,true);
        radio(aspect,"PC 16:9 (تجريبي)",2,false);
        radio(aspect,"PC 16:10 (تجريبي)",3,false);
        body.addView(aspect);

        section(body,"03  تحسينات الجلسة");
        note(body,"يطلب التطبيق 120Hz ويعطل توفير الطاقة مؤقتًا. وعند دعم Wi-Fi، يفعّل وضع التأخير المنخفض.");
        gentleCpu=new CheckBox(this);
        gentleCpu.setText("CPU/GPU Balanced Boost (معتدل، دون رفع تدريجي)");
        gentleCpu.setChecked(true);
        styleCheck(gentleCpu);body.addView(gentleCpu);
        note(body,"إذا كانت نسخة YRC القديمة قد رفعت الترددات بقوة، ابدأ بإلغاء Boost القديم. لن يضيف هذا التطبيق رفعًا فوق حد مرتفع موجود مسبقًا.");

        dnd=new CheckBox(this);
        dnd.setText("تقليل المقاطعات عبر وضع عدم الإزعاج (اختياري)");
        dnd.setChecked(false);
        styleCheck(dnd);body.addView(dnd);
        secondary(body,"السماح بإعداد عدم الإزعاج من Android",this::openDndSettings);

        section(body,"04  تهيئة اللعب الرسمية");
        secondary(body,"فتح XArena / XBOOST إن توفر",this::openGameCenter);
        note(body,"داخل XArena اختر eSports/Performance إن توفر. ومن PUBG اختر Smooth + أعلى خيار FPS رسمي. لا نعطّل نظام الحرارة.");

        section(body,"استعادة الطوارئ");
        secondary(body,"إعادة أبعاد شاشة الهاتف المصنعية فورًا",this::confirmFactoryReset);
        note(body,"جميع الإعدادات المحفوظة مستقلة عن النسخة القديمة. لا تحذف التطبيق أثناء جلسة مفعلة قبل استخدام الاستعادة.");

        if(prefs.getBoolean("s5_active",false))
            update("توجد جلسة سابقة لم تُستعد بالكامل. اضغط استعادة الإعدادات قبل بدء جلسة جديدة.");
    }

    private int dp(int v){return Math.round(v*getResources().getDisplayMetrics().density);}
    private void section(LinearLayout parent,String text){
        TextView t=new TextView(this);
        t.setText(text);
        t.setTextSize(21);
        t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        t.setTextColor(ACCENT);
        t.setPadding(dp(2),dp(20),dp(2),dp(9));
        parent.addView(t);
    }
    private void note(LinearLayout parent,String text){
        TextView t=new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTextColor(SOFT);
        t.setLineSpacing(dp(3),1f);
        t.setPadding(dp(3),dp(4),dp(3),dp(12));
        parent.addView(t);
    }
    private void radio(RadioGroup group,String text,int id,boolean checked){
        RadioButton r=new RadioButton(this);
        r.setId(id);
        r.setText(text);
        r.setTextSize(15);
        r.setTextColor(Color.WHITE);
        r.setPadding(0,dp(8),0,dp(8));
        group.addView(r);
        if(checked)group.check(id);
    }
    private void styleCheck(CheckBox cb){
        cb.setTextSize(15);
        cb.setTextColor(Color.WHITE);
        cb.setPadding(0,dp(5),0,dp(10));
    }
    private void makeButton(LinearLayout parent,String label,boolean primary,Runnable action){
        Button button=new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(primary?16:14);
        button.setTextColor(primary?Color.rgb(4,26,25):Color.WHITE);
        button.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        GradientDrawable shape=new GradientDrawable();
        shape.setColor(primary?ACCENT:Color.rgb(37,74,77));
        shape.setCornerRadius(dp(14));
        button.setBackground(shape);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(primary?62:54));
        params.setMargins(0,dp(6),0,dp(8));
        parent.addView(button,params);
        button.setOnClickListener(v->action.run());
    }
    private void primary(LinearLayout parent,String label,Runnable run){
        makeButton(parent,label,true,run);
    }
    private void secondary(LinearLayout parent,String label,Runnable run){
        makeButton(parent,label,false,run);
    }
    private void update(String message){
        runOnUiThread(()->{
            lastMessage=message;
            String first=message.split("\n",2)[0];
            if(first.length()>85)first=first.substring(0,85)+"…";
            status.setText(first+"\nاضغط هنا لقراءة التفاصيل");
        });
    }
    private int resolutionPercent(){
        int selected=resolution.getCheckedRadioButtonId();
        return selected==85||selected==95?selected:90;
    }
    private String aspectRatio(){
        int selection=aspect.getCheckedRadioButtonId();
        return selection==2?"16:9":selection==3?"16:10":"native";
    }
    private void run(Runnable workItem){
        if(!busy.compareAndSet(false,true)){
            update("هناك عملية جارية بالفعل؛ انتظر حتى تكتمل.");
            return;
        }
        work.execute(()->{
            try{workItem.run();}
            finally{busy.set(false);}
        });
    }
    private boolean gameInstalled(){
        try{return getPackageManager().getLaunchIntentForPackage(PUBG)!=null;}
        catch(Exception e){return false;}
    }
    private boolean tooHot(){
        PowerManager p=(PowerManager)getSystemService(POWER_SERVICE);
        int state=p==null?0:p.getCurrentThermalStatus();
        Intent battery=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int temp=battery==null?-1:battery.getIntExtra("temperature",-1);
        return state>=PowerManager.THERMAL_STATUS_SEVERE||temp>=420;
    }
    private void confirmStart(){
        if(prefs.getBoolean("s5_active",false)){
            new AlertDialog.Builder(this).setTitle("هناك جلسة نشطة")
                .setMessage("قبل بدء جلسة جديدة، استعد الإعدادات الحالية أولًا. يمكن أن تكون PUBG ما زالت تستخدم التعديلات السابقة.")
                .setNegativeButton("إغلاق",null)
                .setPositiveButton("استعادة",(d,w)->restoreAsync()).show();
            return;
        }
        if(!gameInstalled()){
            update("لم يتم العثور على PUBG Mobile Global. لا يمكن بدء جلسة لعب دون تثبيت اللعبة.");
            return;
        }
        final int profile=resolutionPercent();
        final String aspectChoice=aspectRatio();
        final boolean clock=gentleCpu.isChecked();
        final boolean silence=dnd.isChecked();
        new AlertDialog.Builder(this)
            .setTitle("بدء جلسة YRC Pro 5")
            .setMessage("سأجهز Android قبل فتح PUBG:\n"+
                "• دقة "+profile+"%، العرض "+aspectChoice+"\n"+
                "• طلب تردد 120Hz وتعطيل توفير الطاقة مؤقتًا\n"+
                (clock?"• تهيئة حد أدنى معتدل لتردد CPU/GPU إذا دعمته النواة\n":"• ترك CPU/GPU لإدارة XOS\n")+
                "• Wi-Fi Low Latency عندما يكون متاحًا\n"+
                (silence?"• تقليل المقاطعات بإذنك\n":"• عدم تغيير إشعاراتك\n")+
                "\nسيحاول التطبيق استعادة إعداداته عند الرجوع إليه. هذه التغييرات لا تضمن 120FPS ولا تغير ملفات PUBG.")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("ابدأ اللعبة",(d,w)->run(()->prepareAndLaunch(profile,aspectChoice,clock,silence)))
            .show();
    }
    private void prepareAndLaunch(int profile,String mode,boolean clock,boolean silence){
        ArrayList<String> result=new ArrayList<>();
        result.add("YRC Pro 5 — تهيئة اللعبة");
        try{
            SystemTuner.checkRoot();
        }catch(Exception e){
            update("لم تبدأ الجلسة: "+SystemTuner.brief(e.getMessage()));
            return;
        }
        if(!prefs.edit().putBoolean("s5_active",true).commit()){
            update("تعذّر حفظ حالة الجلسة قبل تعديل النظام. لم نبدأ.");
            return;
        }
        SystemTuner system=new SystemTuner(prefs);
        if(tooHot())result.add("الحرارة مرتفعة: تم تجاوز تعديلات تردد CPU/GPU.");
        system.setPerformanceDisplay(result);
        if(clock&&!tooHot())system.applyGentleDvfs(result);
        else result.add("CPU/GPU: تركنا إدارة الترددات لـXOS.");
        try{system.applyResolution(profile,mode,result);}
        catch(Exception e){result.add("تغيير الدقة غير مكتمل: "+SystemTuner.brief(e.getMessage()));}
        new NetworkTuner(this,prefs).apply(result);
        if(silence)configureDnd(result);
        result.add("لا تعديل لملفات اللعبة ولا حقن أو إخفاء روت.");
        result.add("عند الرجوع إلى YRC Pro 5 ستبدأ الاستعادة تلقائيًا.");
        update(android.text.TextUtils.join("\n",result));
        runOnUiThread(()->launchPubg());
    }
    private void configureDnd(List<String> report){
        try{
            NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            if(manager==null||!manager.isNotificationPolicyAccessGranted()){
                report.add("عدم الإزعاج: غير مسموح للتطبيق؛ لم نغيّر الإشعارات.");
                return;
            }
            int original=manager.getCurrentInterruptionFilter();
            if(!prefs.contains("s5_original_dnd"))
                prefs.edit().putInt("s5_original_dnd",original).commit();
            // Honor the user's priority-exception rules; do not suppress alarms/calls blindly.
            manager.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            report.add("عدم الإزعاج: فُعّل وفق استثناءات الأولوية في نظام Android.");
        }catch(Exception e){report.add("عدم الإزعاج: لم يتغير: "+SystemTuner.brief(e.getMessage()));}
    }
    private void restoreDnd(List<String> report){
        if(!prefs.contains("s5_original_dnd"))return;
        try{
            NotificationManager manager=(NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
            if(manager==null||!manager.isNotificationPolicyAccessGranted())
                throw new java.io.IOException("الأذن لم يعد متاحًا");
            manager.setInterruptionFilter(prefs.getInt("s5_original_dnd",
                NotificationManager.INTERRUPTION_FILTER_ALL));
            prefs.edit().remove("s5_original_dnd").commit();
            report.add("✓ استُعيد وضع الإشعارات السابق.");
        }catch(Exception e){
            report.add("! تعذرت استعادة عدم الإزعاج: "+SystemTuner.brief(e.getMessage()));
        }
    }
    private void launchPubg(){
        try{
            Intent launch=getPackageManager().getLaunchIntentForPackage(PUBG);
            if(launch==null){
                update("تعذّر إيجاد PUBG. طبّقت بعض الإعدادات بالفعل؛ استخدم زر الاستعادة.");
                return;
            }
            prefs.edit().putBoolean("s5_launch_pending",true).commit();
            startActivity(launch);
        }catch(Exception e){
            prefs.edit().remove("s5_launch_pending").commit();
            update("تعذّر فتح PUBG: "+SystemTuner.brief(e.getMessage())+
                ". استخدم زر الاستعادة قبل محاولة ثانية.");
        }
    }
    @Override protected void onPause(){
        if(prefs!=null&&prefs.getBoolean("s5_launch_pending",false))
            prefs.edit().putBoolean("s5_left_for_game",true).commit();
        super.onPause();
    }
    @Override protected void onResume(){
        super.onResume();
        if(prefs!=null&&prefs.getBoolean("s5_left_for_game",false)){
            prefs.edit().remove("s5_left_for_game").remove("s5_launch_pending").commit();
            restoreAsync();
        }
    }
    private void confirmRestore(){
        new AlertDialog.Builder(this).setTitle("استعادة الجلسة")
            .setMessage("سنعيد الإعدادات التي حفظها YRC Pro 5 فقط: الشاشة والترددات المعتدلة وWi-Fi والإشعارات. لن نغير إعدادات النسخة القديمة 4.x.")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("استعادة",(d,w)->restoreAsync()).show();
    }
    private void restoreAsync(){run(this::restoreSession);}
    private void restoreSession(){
        ArrayList<String> report=new ArrayList<>();
        report.add("استعادة إعدادات YRC Pro 5");
        try{
            SystemTuner.checkRoot();
            new SystemTuner(prefs).restore(report);
            new NetworkTuner(this,prefs).restore(report);
        }catch(Exception e){
            report.add("! تعذرت استعادة إعدادات الجذر: "+SystemTuner.brief(e.getMessage()));
        }
        restoreDnd(report);
        boolean failed=false;
        for(String value:report)if(value.startsWith("!"))failed=true;
        if(!failed){
            prefs.edit().remove("s5_active").remove("s5_launch_pending")
                .remove("s5_left_for_game").commit();
            report.add("✓ استعادة جلسة YRC Pro 5 اكتملت.");
        }else{
            report.add("بعض الإعدادات قد تظل نشطة. لا تحذف التطبيق؛ جرّب زر الاستعادة مرة أخرى.");
        }
        update(android.text.TextUtils.join("\n",report));
    }
    private void confirmFactoryReset(){
        new AlertDialog.Builder(this)
            .setTitle("استعادة أبعاد الشاشة المصنعية")
            .setMessage("هذه استعادة طوارئ للشاشة فقط. لا تحذف إعدادات الترددات المحفوظة ولا تغيّر ملفات PUBG.")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("استعادة",(d,w)->run(()->{
                ArrayList<String> result=new ArrayList<>();
                new SystemTuner(prefs).factoryDisplayReset(result);
                update(android.text.TextUtils.join("\n",result));
            })).show();
    }
    private void openDndSettings(){
        try{
            startActivity(new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
            update("من إعدادات Android يمكنك منح YRC Pro 5 إذن عدم الإزعاج. يظل الخيار اختياريًا.");
        }catch(Exception e){update("تعذر فتح الإعدادات: "+SystemTuner.brief(e.getMessage()));}
    }
    private void openGameCenter(){
        String[] possible={
            "com.transsion.xarena","com.transsion.gamezone","com.transsion.xboost",
            "com.transsion.gamespace","com.infinix.gamezone","com.infinix.xarena"
        };
        for(String name:possible){
            try{
                Intent launch=getPackageManager().getLaunchIntentForPackage(name);
                if(launch!=null){startActivity(launch);return;}
            }catch(Exception ignored){}
        }
        new AlertDialog.Builder(this).setTitle("XArena / XBOOST")
            .setMessage("لم يعثر YRC على حزمة XArena قابلة للفتح مباشرة. افتح XArena من هاتفك ثم جرّب وضع eSports الرسمي إن كان متاحًا. ويمكنك استخدام Bypass Charging من قائمة الهاتف الرسمية عند توفرها.\n\nهذه الخيارات تخص XOS ولا نزوّر تنفيذها من داخل YRC.")
            .setPositiveButton("إغلاق",null).show();
    }
    @Override protected void onDestroy(){
        work.shutdown();
        super.onDestroy();
    }
}
