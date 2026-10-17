package com.yrc.gaming;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * YRC Gaming Pro 6.0: rebuilt one-screen controller over a single SessionEngine.
 * No magic anti-ban, no memory patches, no PUBG configuration file access.
 */
public final class MainActivity extends Activity {
    private static final String PUBG="com.tencent.ig";
    private static final int NOTIFICATIONS=617;
    private static final int BG=Color.rgb(10,16,26);
    private static final int SURFACE=Color.rgb(22,32,48);
    private static final int TEAL=Color.rgb(60,225,185);
    private static final int TEXT=Color.rgb(238,245,251);
    private static final int MUTED=Color.rgb(171,187,203);
    // Survives short activity recreation caused by Android wm size override.
    private static final ExecutorService WORK=Executors.newSingleThreadExecutor();
    private LinearLayout body;
    private TextView status;
    private int requestedPercent=100;
    private String lastReport="YRC جاهز — لم يتم تعديل أي إعداد في هذه الجلسة.";

    @Override public void onCreate(Bundle state){
        super.onCreate(state);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        LinearLayout frame=new LinearLayout(this);
        frame.setOrientation(LinearLayout.VERTICAL);
        frame.setBackgroundColor(BG);
        frame.setOnApplyWindowInsetsListener((v,insets)->{
            android.graphics.Insets i=insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(0,i.top,0,i.bottom);
            return insets;
        });
        setContentView(frame);
        frame.requestApplyInsets();

        LinearLayout header=new LinearLayout(this);
        header.setPadding(dp(22),dp(22),dp(20),dp(12));
        header.setOrientation(LinearLayout.VERTICAL);
        TextView brand=text("YRC  /  GAMING PRO",22,TEXT,true);
        header.addView(brand);
        TextView subtitle=text("ENGINE 6.0    •    GT30 PRO    •    ANDROID 16",12,TEAL,false);
        subtitle.setPadding(0,dp(2),0,0);
        header.addView(subtitle);
        frame.addView(header);

        status=text(lastReport,13,TEXT,false);
        status.setPadding(dp(16),dp(16),dp(16),dp(16));
        status.setMaxLines(3);
        status.setEllipsize(android.text.TextUtils.TruncateAt.END);
        status.setBackground(panel(SURFACE,TEAL));
        LinearLayout.LayoutParams statusParams=new LinearLayout.LayoutParams(-1,-2);
        statusParams.setMargins(dp(16),dp(5),dp(16),dp(10));
        frame.addView(status,statusParams);
        status.setOnClickListener(v->new AlertDialog.Builder(this)
            .setTitle("التقرير الكامل")
            .setMessage(lastReport)
            .setPositiveButton("إغلاق",null)
            .show());

        ScrollView scroller=new ScrollView(this);
        scroller.setFillViewport(true);
        body=new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        body.setPadding(dp(18),dp(6),dp(18),dp(45));
        scroller.addView(body);
        frame.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));

        section("محرك الجلسة الموحد");
        paragraph("هذه نسخة أعيدت كتابة واجهتها ومحركها. لا نرفع ترددات CPU/GPU ولا نعدل ملفات اللعبة. الاختبار الأساسي: طلب 120Hz كحد أدنى وأقصى للشاشة، مع التحقق من قبول النظام.");

        primaryButton("▶ START — شاشة 120Hz بدون تغيير الأبعاد",()->confirmStart(100));
        primaryButton("▶ STABILITY — شاشة 120Hz + عرض 90%",()->confirmStart(90));
        secondaryButton("■ إنهاء الجلسة واستعادة كل تعديل",this::requestRestore);
        paragraph("الوضع الأول يفصل مشكلة Hz عن أداء PUBG. الوضع الثاني يقلل أبعاد Android اختياريًا مع الحفاظ على نسبة 16:10 الحالية. لا يمكن لأي منهما ضمان أن محرك PUBG يُنتج 120FPS.");

        section("منظور الشاشة");
        secondaryButton("تفعيل منظور PC 16:10",()->confirmPc(true));
        secondaryButton("إلغاء منظور PC واستعادة المقاس السابق",()->confirmPc(false));
        paragraph("تغيير الأبعاد قد يغيّر مواضع اللمس أو يضيف أشرطة. استخدم الاستعادة قبل حذف التطبيق.");

        section("فحص موثوق، بدون أرقام وهمية");
        secondaryButton("فحص أوضاع الشاشة والقيم الفعلية",()->execute(()->SessionEngine.diagnostic(getApplicationContext())));
        secondaryButton("فتح إعدادات XArena / وضع اللعب",this::openGamingSettings);
        paragraph("الفحص يوضح ما قبلته إعدادات Android. لا يدّعي قراءة FPS اللعبة أو حرارتها الداخلية.");

        section("الصيانة والاستعادة");
        secondaryButton("استعادة آثار إصدارات YRC السابقة",this::confirmLegacyRestore);
        secondaryButton("فتح PUBG بدون أي تغييرات",this::launchPubg);
        paragraph("إذا بقيت اللعبة تهبط من 120 إلى 60 بينما الشاشة 120Hz، فالأسباب المحتملة داخل اللعبة/الرسوميات/إدارة النظام؛ لن نخترع زرًا يقول إنه أصلحها.");
        lastReport=SessionEngine.lastReport(this);
        updateStatus(lastReport);
        if(SessionEngine.active(this)){
            new AlertDialog.Builder(this)
                .setTitle("جلسة سابقة لم تُستعد")
                .setMessage("هناك إعدادات محفوظة من جلسة YRC 6.0. يمكنك استعادتها الآن، أو تركها إن كنت ما زلت تلعب.")
                .setNegativeButton("لاحقًا",null)
                .setPositiveButton("استعادة", (d,w)->requestRestore())
                .show();
        }
    }

    @Override protected void onResume(){
        super.onResume();
        if(status!=null){
            String report=SessionEngine.lastReport(this);
            if(!report.equals(lastReport))updateStatus(report);
        }
    }

    private void confirmStart(int percent){
        requestedPercent=percent;
        if(!hasPubg()){
            updateStatus("PUBG Mobile Global غير مثبتة أو غير متاحة لهذا التطبيق. لم نغير أي إعداد.");
            return;
        }
        if(SessionEngine.active(this)){
            new AlertDialog.Builder(this)
                .setTitle("جلسة نشطة")
                .setMessage("لا تبدأ جلسة أخرى قبل إيقاف الحالية واستعادة الإعدادات.")
                .setNegativeButton("إغلاق",null)
                .setPositiveButton("استعادة",(d,w)->requestRestore())
                .show();
            return;
        }
        String details=percent==100?
            "سنطلب من Android تردد شاشة 120Hz (حد أدنى وحد أقصى)، ونحافظ على دقة ومنظور جهازك الحاليين.":
            "سنطلب 120Hz ونخفض أبعاد Android الحالية إلى 90%، مع المحافظة على نسبة 16:10 إذا كانت مفعلة.";
        new AlertDialog.Builder(this)
            .setTitle("بدء جلسة YRC "+(percent==100?"120Hz":"STABILITY 90%"))
            .setMessage(details+"\n\nيحتاج KernelSU Next وظهور إشعار الجلسة. نتحقق من القيم ونحفظ الإعدادات السابقة قبل كل تعديل. لا نغيّر إعدادات PUBG الداخلية ولا نعطّل حماية الحرارة.")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("ابدأ",(d,w)->checkNotificationPermission())
            .show();
    }

    private boolean hasPubg(){
        try{return getPackageManager().getLaunchIntentForPackage(PUBG)!=null;}
        catch(Exception e){return false;}
    }

    private void checkNotificationPermission(){
        if(Build.VERSION.SDK_INT>=33&&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS},NOTIFICATIONS);
            return;
        }
        beginSession(requestedPercent);
    }
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] results){
        super.onRequestPermissionsResult(requestCode,permissions,results);
        if(requestCode!=NOTIFICATIONS)return;
        if(results.length>0&&results[0]==PackageManager.PERMISSION_GRANTED)
            beginSession(requestedPercent);
        else updateStatus("لم تسمح بإشعار الجلسة. لم نغيّر الإعدادات لأن خدمة الاستعادة لن تكون ظاهرة لك.");
    }

    private void beginSession(int percent){
        updateStatus("جارٍ تطبيق الجلسة مع حفظ النسخة الأصلية والتحقق من قبول القيم…");
        Context app=getApplicationContext();
        WORK.execute(()->{
            SessionEngine.Result result=SessionEngine.start(app,percent);
            runOnUiThread(()->{
                updateStatus(result.report);
                if(!result.started)return;
                try{
                    Intent intent=new Intent(this,SessionService.class)
                        .setAction(SessionService.ACTION_START);
                    if(Build.VERSION.SDK_INT>=26)startForegroundService(intent);
                    else startService(intent);
                    launchPubg();
                }catch(Exception e){
                    updateStatus(result.report+"\nتعذّر بدء خدمة الاستعادة: "+e.getMessage()+
                        "\nسيتم الآن استعادة إعدادات الجلسة.");
                    requestRestore();
                }
            });
        });
    }

    private void requestRestore(){
        execute(()->android.text.TextUtils.join("\n",
            SessionEngine.restore(getApplicationContext())));
        try{
            stopService(new Intent(this,SessionService.class));
        }catch(Exception ignored){}
    }

    private void confirmPc(boolean enable){
        if(SessionEngine.active(this)){
            updateStatus("أوقف جلسة YRC أولًا قبل تعديل أبعاد الشاشة.");
            return;
        }
        new AlertDialog.Builder(this)
            .setTitle(enable?"منظور PC 16:10":"استعادة أبعاد الشاشة")
            .setMessage(enable?
                "سيغيّر Android نسبة عرض الشاشة إلى 16:10. هذا ليس توسيع FOV داخل PUBG. يتم حفظ مقاس الشاشة السابق للاستعادة.":
                "سيعيد المقاس المحفوظ قبل تطبيق 16:10 من النسخة الجديدة. إذا كان المنظور قديمًا، استخدم زر استعادة النسخ السابقة.")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("تنفيذ",(d,w)->{
                if(enable)execute(()->SessionEngine.setPc16by10(getApplicationContext()));
                else execute(()->SessionEngine.restorePc(getApplicationContext()));
            })
            .show();
    }

    private void confirmLegacyRestore(){
        new AlertDialog.Builder(this)
            .setTitle("استعادة إعدادات YRC السابقة")
            .setMessage("إذا استخدمت Boost من النسخ السابقة، فهذا الإجراء يحاول استعادة ترددات CPU/GPU ودقة العرض والشبكة والإعدادات المحفوظة قبلها. قد يلغي منظور PC قديمًا. نفّذ الإجراء قبل تثبيت نسخة بتوقيع مختلف.")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("استعادة",(d,w)->execute(()->{
                RootRunner.requireRoot();
                List<String> messages=SessionManager.restore(getApplicationContext());
                return android.text.TextUtils.join("\n",messages);
            }))
            .show();
    }

    private void openGamingSettings(){
        String[] packages={"com.transsion.gamespace.app","com.transsion.gameassistant","com.transsion.xarena"};
        for(String name:packages){
            Intent i=getPackageManager().getLaunchIntentForPackage(name);
            if(i!=null){
                try{startActivity(i);return;}catch(Exception ignored){}
            }
        }
        try{
            startActivity(new Intent(Settings.ACTION_DISPLAY_SETTINGS));
        }catch(Exception e){updateStatus("لم أجد XArena؛ افتحها يدويًا من درج التطبيقات.");}
    }

    private void launchPubg(){
        try{
            Intent i=getPackageManager().getLaunchIntentForPackage(PUBG);
            if(i==null){updateStatus("لم أجد PUBG Global. افتح إصدارك من قائمة التطبيقات.");return;}
            startActivity(i);
        }catch(Exception e){updateStatus("تعذر فتح PUBG: "+e.getMessage());}
    }
    interface WorkResult{String run()throws Exception;}
    private void execute(WorkResult task){
        updateStatus("جارٍ التنفيذ والتحقق؛ انتظر التقرير…");
        WORK.execute(()->{
            String answer;
            try{answer=task.run();}
            catch(Exception e){answer="تعذّر الإجراء: "+e.getMessage();}
            final String done=answer;
            runOnUiThread(()->updateStatus(done));
        });
    }
    private void updateStatus(String content){
        lastReport=content;
        if(status!=null){
            String first=content.replace('\n',' ');
            if(first.length()>160)first=first.substring(0,160)+"…";
            status.setText(first+"\nاضغط لعرض التقرير الكامل");
        }
    }
    private int dp(int amount){return Math.round(amount*getResources().getDisplayMetrics().density);}
    private TextView text(String s,int size,int color,boolean bold){
        TextView v=new TextView(this);
        v.setText(s);v.setTextColor(color);v.setTextSize(size);
        v.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }
    private GradientDrawable panel(int fill,int edge){
        GradientDrawable d=new GradientDrawable();
        d.setColor(fill);d.setCornerRadius(dp(14));d.setStroke(dp(1),edge);
        return d;
    }
    private void section(String name){
        TextView h=text(name,19,TEAL,true);
        h.setPadding(0,dp(22),0,dp(9));
        body.addView(h);
    }
    private void paragraph(String content){
        TextView p=text(content,14,MUTED,false);
        p.setLineSpacing(dp(3),1.07f);
        p.setPadding(dp(2),dp(4),dp(2),dp(14));
        body.addView(p);
    }
    private void primaryButton(String label,Runnable action){button(label,TEAL,Color.rgb(8,26,29),action);}
    private void secondaryButton(String label,Runnable action){
        button(label,Color.rgb(38,66,75),TEXT,action);
    }
    private void button(String label,int bg,int fg,Runnable action){
        Button b=new Button(this);
        b.setText(label);b.setAllCaps(false);b.setTextSize(15);
        b.setTextColor(fg);b.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        GradientDrawable drawable=new GradientDrawable();
        drawable.setColor(bg);drawable.setCornerRadius(dp(13));
        b.setBackground(drawable);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(56));
        params.setMargins(0,dp(4),0,dp(8));
        body.addView(b,params);
        b.setOnClickListener(v->action.run());
    }
}
