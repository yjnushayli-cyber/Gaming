package com.yrc.gaming;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.os.PowerManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.provider.Settings;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int BG=Color.rgb(12,18,29);
    private static final int TEAL=Color.rgb(72,216,182);
    private static final int PANEL=Color.rgb(24,39,54);
    private static final String PUBG="com.tencent.ig";
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private SharedPreferences prefs;
    private LinearLayout main;
    private TextView status;
    private String lastReport="جاهز • تحسينات نظام Android فقط، دون تعديل ملفات PUBG.";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("yrc_clean_30",MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout frame=new LinearLayout(this);
        frame.setOrientation(LinearLayout.VERTICAL);
        frame.setBackgroundColor(BG);
        // Android 15/16 enforce edge-to-edge with targetSdk 35. Honor safe areas.
        frame.setOnApplyWindowInsetsListener((view,insets)->{
            android.graphics.Insets bars=insets.getInsets(WindowInsets.Type.systemBars());
            frame.setPadding(0,bars.top,0,bars.bottom);
            return insets;
        });
        setContentView(frame);
        frame.requestApplyInsets();

        status=new TextView(this);
        status.setText("جاهز • اضغط هنا لعرض التقرير الكامل");
        status.setTextDirection(View.TEXT_DIRECTION_FIRST_STRONG);
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setMaxLines(2);
        status.setEllipsize(android.text.TextUtils.TruncateAt.END);
        status.setPadding(dp(15),dp(13),dp(15),dp(13));
        GradientDrawable panel=new GradientDrawable();
        panel.setColor(PANEL);
        panel.setCornerRadius(dp(13));
        panel.setStroke(dp(1),TEAL);
        status.setBackground(panel);
        LinearLayout.LayoutParams statParams=new LinearLayout.LayoutParams(-1,-2);
        statParams.setMargins(dp(14),dp(14),dp(14),dp(7));
        frame.addView(status,statParams);
        status.setOnClickListener(v->new AlertDialog.Builder(this)
            .setTitle("التقرير الكامل لآخر عملية").setMessage(lastReport)
            .setPositiveButton("إغلاق",null).show());

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        main=new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        main.setPadding(dp(19),dp(10),dp(19),dp(34));
        scroll.addView(main);
        frame.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        heading("YRC GAMING PRO 4.3 • STABILITY");
        info("GT30 Pro • تحسينات Android فعلية فقط، بدون تعديل ملفات PUBG أو تعطيل الحماية الحرارية.");
        info("وضع ثبات الاشتباكات يخفّف حمل الرسومات ويخفض Boost المرتفع السابق للمعالج وGPU. بدون مراقبة مستمرة أو تعديل ملفات PUBG.");
        action("ثبات الاشتباكات 120FPS — دقة 90%",()->confirmCombatProfile(90));
        action("تخفيف أقوى للاشتباكات — دقة 85%",()->confirmCombatProfile(85));
        action("إيقاف التحسينات واستعادة إعدادات الجهاز",this::confirmRestore);

        heading("01  ثبات الفريمات");
        info("الهبوط 60↔120 أثناء الحركة قد يكون من ضغط GPU أو الحرارة أو قيد اللعبة. جرّب أولًا وضع 90%، ثم 85% إذا بقي التذبذب. وضع 120Hz ليس قفلًا لفريمات PUBG.");
        action("تفعيل وضع ثبات 120FPS",()->background(this::applyFpsProfile));

        heading("02  تحسين الرسومات");
        info("جودة الشاشة من Android فقط. جودة الخامات وHDR والفريمات الحقيقية تختارها من قائمة PUBG الرسمية.");
        action("وضوح أصلي 100% — أعلى دقة للشاشة",()->background(()->applyResolution(100)));
        action("متوازن 95% — لتخفيف حمل العرض",()->background(()->applyResolution(95)));
        action("إعدادات الرسومات الرسمية الموصى بها",this::showGraphicsGuide);

        heading("03  ثبات البنق وتقليل الدمج الوهمي");
        info("زر فعلي للشبكة: يفعّل Wi-Fi Low Latency، ويحاول تعطيل توفير طاقة شريحة Wi-Fi إذا دعمه تعريف الجهاز. قد يقلل التأخير والتذبذب، لكنه لا يغير احتساب إصابات PUBG في السيرفر.");
        action("تقليل الدمج الوهمي — تحسين الاتصال",this::confirmHitAssist);
        action("إلغاء تحسين الدمج الوهمي واستعادة Wi-Fi",()->background(this::restoreHitAssist));
        action("فتح إعدادات Wi-Fi والشبكة",()->{
            try{startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));}
            catch(Exception e){message("تعذّر فتح إعدادات الشبكة: "+e.getMessage());}
        });

        heading("04  منظور PC");
        info("عرض PC 16:9 أو 16:10 لتجربة إطار أكثر إحكامًا. قد يتغير حجم اللاعب على الشاشة، لكن لا يوجد ضمان لإظهار جسمه كاملًا؛ ضبط بُعد كاميرا TPP الحقيقي من خيارات PUBG الرسمية فقط.");
        action("منظور PC كلاسيكي 16:9",()->confirmPcRatio());
        action("منظور PC 16:10 — مساحة أصغر للتجربة",()->confirmPcCompact());
        action("ضبط TPP الواسع من PUBG رسميًا",this::showTppGuide);

        heading("التحكم والاستعادة");
        action("إرجاع جميع الإعدادات المحفوظة",this::confirmRestore);
        action("إرجاع أبعاد الشاشة المصنعية فقط",this::confirmFactoryScreenRestore);
        action("فتح PUBG Mobile",this::launchGame);
        info("تعديلات قابلة للاستعادة: ترددات CPU/GPU الدنيا، دقة الشاشة، 120Hz والشبكة. لا يوجد ضمان 120FPS ثابت، ولا تعديل لملفات اللعبة.");
    }

    private int dp(int v){return Math.round(getResources().getDisplayMetrics().density*v);}

    private void heading(String text){
        TextView v=new TextView(this);
        v.setText(text);
        v.setTextSize(20);
        v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        v.setTextColor(TEAL);
        v.setPadding(0,dp(19),0,dp(8));
        main.addView(v);
    }
    private void info(String text){
        TextView v=new TextView(this);
        v.setText(text);
        v.setTextSize(14);
        v.setLineSpacing(dp(3),1f);
        v.setTextColor(Color.rgb(220,229,237));
        v.setPadding(dp(3),dp(3),dp(3),dp(9));
        main.addView(v);
    }
    private void action(String label,Runnable click){
        Button b=new Button(this);
        b.setText(label);
        b.setTextSize(14);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        GradientDrawable bg=new GradientDrawable();
        bg.setColor(label.contains("إرجاع")?Color.rgb(121,72,39):Color.rgb(33,111,106));
        bg.setCornerRadius(dp(11));
        b.setBackground(bg);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,dp(51));
        params.setMargins(0,dp(3),0,dp(7));
        main.addView(b,params);
        b.setOnClickListener(v->click.run());
    }
    private void message(String text){
        runOnUiThread(()->{
            lastReport=text;
            String first=text.split("\\n",2)[0];
            if(first.length()>100)first=first.substring(0,100)+"…";
            status.setText(first+"\nاضغط هنا لقراءة تفاصيل التقرير بالكامل");
        });
    }
    private void background(Runnable task){
        worker.execute(task);
    }

    // All rooted modifications target Android display/performance settings only.
    private String root(String command)throws Exception{
        java.lang.Process p=new ProcessBuilder("su","-c",command)
            .redirectErrorStream(true).start();
        ByteArrayOutputStream buffer=new ByteArrayOutputStream();
        byte[] bytes=new byte[1024];int n;
        try(InputStream in=p.getInputStream()){
            while((n=in.read(bytes))!=-1){
                if(buffer.size()<8192)
                    buffer.write(bytes,0,Math.min(n,8192-buffer.size()));
            }
        }
        int code=p.waitFor();
        String out=buffer.toString("UTF-8").trim();
        if(code!=0)throw new IOException("رفض أمر النظام ("+code+"): "+out);
        return out;
    }
    private boolean rootReady()throws Exception{
        String id=root("id -u").trim();
        if(!"0".equals(id))throw new IOException("لم يتم منح YRC صلاحية Superuser من KernelSU Next");
        return true;
    }
    private int thermalStatus(){
        PowerManager power=(PowerManager)getSystemService(POWER_SERVICE);
        return power==null?0:power.getCurrentThermalStatus();
    }
    private int batteryTenths(){
        Intent b=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        return b==null?-1:b.getIntExtra("temperature",-1);
    }
    private boolean tooHot(){
        int temp=batteryTenths();
        return (temp>=420)||thermalStatus()>=PowerManager.THERMAL_STATUS_SEVERE;
    }
    private String prefName(String table,String key){return "clean_original_"+table+"_"+key;}
    private String readSetting(String table,String key)throws Exception{
        return root("settings get "+table+" "+key).trim();
    }
    private void rememberSetting(String table,String key)throws Exception{
        String p=prefName(table,key);
        if(!prefs.contains(p))
            prefs.edit().putString(p,readSetting(table,key)).commit();
    }
    private void changeSetting(String table,String key,String value)throws Exception{
        rememberSetting(table,key);
        root("settings put "+table+" "+key+" "+value);
    }
    private void undoSetting(String table,String key,List<String> output){
        String p=prefName(table,key);
        if(!prefs.contains(p))return;
        String old=prefs.getString(p,"null");
        try{
            if(old==null||old.isEmpty()||"null".equals(old))
                root("settings delete "+table+" "+key);
            else if(old.matches("[a-zA-Z0-9_.-]{1,40}"))
                root("settings put "+table+" "+key+" "+old);
            else throw new IOException("القيمة القديمة غير صالحة للاستعادة الآمنة");
            prefs.edit().remove(p).commit();
            output.add("استعادة "+key+": تمت");
        }catch(Exception e){output.add("استعادة "+key+": تعذرت ("+e.getMessage()+")");}
    }
    private boolean gameInstalled(){
        try{
            getPackageManager().getPackageInfo(PUBG,0);
            return true;
        }catch(Exception e){return false;}
    }
    private void confirmCombatProfile(int percent){
        if(percent!=90&&percent!=85)return;
        new AlertDialog.Builder(this)
            .setTitle("ثبات الاشتباكات — "+percent+"%")
            .setMessage("لاحظت أن PUBG تتذبذب عندك بين 60 و120FPS أثناء القتال.\n\n"+
                "سيغيّر YRC إعدادات النظام فعليًا:\n"+
                "• تقليل أبعاد عرض Android إلى "+percent+"% من المقاس الأصلي، مع الإبقاء على نسبة PC إذا كانت مفعلة.\n"+
                "• تخفيف الحد الأدنى العالي السابق لترددات CPU/GPU إلى مستوى معتدل قابل للاستعادة.\n"+
                "• طلب شاشة 120Hz وإيقاف توفير الطاقة.\n\n"+
                "الهدف تقليل ضغط الرسم والحرارة؛ لا يمكن ضمان تثبيت 120FPS ولا التحكم في FPS محرك PUBG. قد تظهر أشرطة أو تتغير أماكن اللمس.")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("تطبيق",(d,w)->background(()->applyCombatStability(percent)))
            .show();
    }

    private void applyCombatStability(int percent){
        ArrayList<String> report=new ArrayList<>();
        report.add("ثبات الاشتباكات "+percent+"% — تعديلات Android الفعلية");
        try{
            rootReady();
            boolean hot=tooHot();
            KernelBoost kernel=new KernelBoost(prefs);
            if(hot){
                // Undo elevated DVFS minima if device is already too hot.
                report.add("حرارة الجهاز مرتفعة؛ لن نرفع ترددات CPU/GPU.");
                report.addAll(kernel.restore());
            }else{
                report.addAll(kernel.applyStability());
            }
            try{
                changeSetting("system","tran_refresh_mode","120");
                changeSetting("system","peak_refresh_rate","120.0");
                changeSetting("global","low_power","0");
                report.add("تردد الشاشة: XOS="+readSetting("system","tran_refresh_mode")+
                    " | أعلى معدل="+readSetting("system","peak_refresh_rate")+
                    " | حد أدنى تكيفي="+readSetting("system","min_refresh_rate"));
            }catch(Exception e){
                report.add("تحديث الشاشة: لم تكتمل جميع الإعدادات "+e.getMessage());
            }
            // The target is based on the physical display so repeat presses do not
            // compound resizing. Honor existing 16:9 / 16:10 aspect preference.
            try{
                String[] physical=physicalSize(currentSize()).split("x");
                int shortOriginal=Math.min(Integer.parseInt(physical[0]),Integer.parseInt(physical[1]));
                int longOriginal=Math.max(Integer.parseInt(physical[0]),Integer.parseInt(physical[1]));
                int shortTarget=Math.round(shortOriginal*percent/100f);
                shortTarget=2*(shortTarget/2);
                if(shortTarget<800)throw new IOException("الدقة المقترحة صغيرة جدًا");
                String pc=prefs.getString("clean_pc_ratio","");
                int longTarget;
                if("16:9".equals(pc)){
                    longTarget=(int)Math.round(shortTarget*16.0/9.0);
                }else if("16:10".equals(pc)){
                    longTarget=(int)Math.round(shortTarget*16.0/10.0);
                }else{
                    longTarget=(int)Math.round(longOriginal*percent/100.0);
                }
                longTarget=2*(longTarget/2);
                if(longTarget<1300)throw new IOException("الارتفاع المقترح صغير جدًا");
                saveOldSize();
                String desired=shortTarget+"x"+longTarget;
                root("wm size "+desired);
                String actual=currentSize();
                if(actual.contains("Override size: "+desired)){
                    report.add("دقة Android: "+desired+" بدل "+shortOriginal+"x"+longOriginal+
                        ("".equals(pc)?"":" — منظور PC "+pc+" محفوظ."));
                    report.add("هذه دقة عرض النظام وليست تأكيدًا لتغيّر دقة محرك PUBG الداخلية.");
                }else{
                    report.add("أرسلنا أمر الدقة "+desired+" لكن XOS لم يؤكد تطبيقه: "+actual);
                }
            }catch(Exception e){
                report.add("تخفيف عبء العرض لم يكتمل: "+e.getMessage());
            }
            report.add("الأفضل من داخل PUBG: Smooth + أعلى FPS رسمي متاح، مع XArena eSports إذا توفر.");
            report.add("إذا استمر التذبذب إلى 60FPS، قد تكون اللعبة أو XOS تفرض الحد؛ لا يمكن إجبارهما دون تعديل اللعبة.");
            report.add("بعد اللعب: استخدم زر الاستعادة. وإذا اضطرب العرض، زر «أبعاد الشاشة المصنعية» يعيدها فورًا.");
        }catch(Exception e){
            report.add("تعذر تطبيق وضع الاشتباكات: "+e.getMessage());
            report.add("قد تكون هناك تغييرات جزئية؛ استخدم زر الاستعادة.");
        }
        message(android.text.TextUtils.join("\n",report));
    }

    private void confirmFactoryScreenRestore(){
        new AlertDialog.Builder(this)
            .setTitle("إرجاع أبعاد الشاشة المصنعية")
            .setMessage("يعيد مقاس الشاشة إلى قيمة المصنع عبر Android. يلغي 16:9 و16:10 وتخفيف الدقة فقط، دون مساس بترددات CPU/GPU أو ملفات PUBG. هل تريد الاستعادة؟")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("استعادة",(d,w)->background(this::restoreFactoryScreen))
            .show();
    }
    private void restoreFactoryScreen(){
        try{
            rootReady();
            root("wm size reset");
            prefs.edit().remove("clean_original_wm").remove("clean_pc_ratio").commit();
            message("تم طلب إعادة أبعاد الشاشة إلى إعداد المصنع.\n"+currentSize()+
                "\nإذا ظل العرض غير طبيعي، أعد تشغيل الجهاز. لم تُعدّل ملفات PUBG.");
        }catch(Exception e){message("فشل إرجاع دقة المصنع: "+e.getMessage());}
    }

    private void applyFpsProfile(){
        try{
            rootReady();
            if(tooHot()){
                message("الحرارة مرتفعة؛ لم يتم تغيير أداء الشاشة. انتظر حتى تبرد البطارية ولا تعطّل الحماية الحرارية.");
                return;
            }
            ArrayList<String> output=new ArrayList<>();
            changeSetting("system","tran_refresh_mode","120");
            changeSetting("system","peak_refresh_rate","120.0");
            changeSetting("global","low_power","0");
            // XOS can legitimately keep an adaptive floor at 60Hz. Do not fight it.
            if(prefs.contains(prefName("system","min_refresh_rate"))){
                undoSetting("system","min_refresh_rate",output);
            }
            output.add("تم طلب تردد 120Hz وتعطيل توفير الطاقة.");
            if(gameInstalled()){
                if(prefs.getBoolean("clean_game_mode_unavailable",false)){
                    output.add("Game Mode: الجهاز سبق أن رفض هذا الأمر لـ PUBG؛ تجاهلناه.");
                }else{
                    try{
                        String res=root("cmd game mode 2 "+PUBG);
                        String lower=res.toLowerCase(Locale.US);
                        if(lower.contains("not supported")||lower.contains("error")||
                            lower.contains("unknown")||lower.contains("255")){
                            prefs.edit().putBoolean("clean_game_mode_unavailable",true).apply();
                            output.add("Game Mode غير مدعوم للعبة، تم إيقاف تكرار المحاولة.");
                        }else{
                            prefs.edit().putBoolean("clean_requested_game_mode",true).apply();
                            output.add("Game Mode: تم إرسال طلب Performance.");
                        }
                    }catch(Exception e){
                        String error=String.valueOf(e.getMessage());
                        if(error.toLowerCase(Locale.US).contains("not supported")){
                            prefs.edit().putBoolean("clean_game_mode_unavailable",true).apply();
                            output.add("Game Mode غير مدعوم من PUBG/XOS؛ سيتم تجاهل الأمر من الآن.");
                        }else{
                            output.add("Game Mode: تعذر تطبيقه، بدون تأثير على إعداد الشاشة. "+error);
                        }
                    }
                }
            }
            String mode=readSetting("system","tran_refresh_mode");
            String peak=readSetting("system","peak_refresh_rate");
            String min=readSetting("system","min_refresh_rate");
            output.add("الفحص الفعلي بعد الطلب:");
            output.add("XOS="+mode+" | peak="+peak+"Hz | min="+min+"Hz");
            if(!"120".equals(mode)||!"120.0".equals(peak)){
                output.add("تنبيه: لم يحتفظ XOS بجميع القيم المطلوبة؛ قد يتولى النظام التحكم بالتحديث.");
            }
            output.add("min=60Hz قد يكون طبيعيًا في الوضع التكيفي، ولا يعني أن اللعبة تعمل 60FPS.");
            output.add("اضبط PUBG رسميًا على Smooth + 120FPS إن كان الخيار متاحًا.");
            output.add("هذه القيم ليست قياسًا لعدد FPS الحقيقي أثناء اللعب.");
            message(android.text.TextUtils.join("\n",output));
        }catch(Exception e){
            message("تعذر تطبيق ملف 120Hz: "+e.getMessage()+
                "\nيمكنك استخدام زر الاستعادة لإرجاع إعدادات النظام المحفوظة.");
        }
    }
    private void checkFpsProfile(){
        try{
            rootReady();
            String[] keys={"tran_refresh_mode","peak_refresh_rate","min_refresh_rate"};
            StringBuilder sb=new StringBuilder("وضع الشاشة والنظام:\n");
            for(String key:keys){
                sb.append(key).append(": ").append(readSetting("system",key)).append("\n");
            }
            sb.append("Low Power: ").append(readSetting("global","low_power")).append("\n");
            int temp=batteryTenths();
            if(temp>=0)sb.append("حرارة البطارية: ")
                .append(String.format(Locale.US,"%.1f",temp/10.0)).append("°C\n");
            sb.append("Thermal status: ").append(thermalStatus()).append("\n");
            sb.append("الحد الأدنى 60Hz قد يكون XOS Adaptive Refresh Rate؛ هذا لا يثبت 60FPS داخل PUBG.\n");
            sb.append("120Hz هو معدل تحديث الشاشة، وليس قياس FPS الفعلي. افحص عداد الفريمات أثناء اللعب.");
            message(sb.toString());
        }catch(Exception e){message("تعذر قراءة إعدادات الفريمات: "+e.getMessage());}
    }

    private String currentSize()throws Exception{
        return root("wm size").trim();
    }
    private String physicalSize(String output)throws Exception{
        java.util.regex.Matcher m=java.util.regex.Pattern
            .compile("Physical size:\\s*(\\d+)x(\\d+)").matcher(output);
        if(!m.find())throw new IOException("تعذر معرفة المقاس الأصلي: "+output);
        return m.group(1)+"x"+m.group(2);
    }
    private void saveOldSize()throws Exception{
        if(!prefs.contains("clean_original_wm"))
            prefs.edit().putString("clean_original_wm",currentSize()).commit();
    }
    private void setRelativeScreen(int percent,boolean pcView){
        try{
            rootReady();
            if(tooHot()&&percent==100){
                message("الحرارة مرتفعة؛ تقليل الحمل أولًا أفضل من رفع الدقة.");
                return;
            }
            String[] physical=physicalSize(currentSize()).split("x");
            int w=Integer.parseInt(physical[0]),h=Integer.parseInt(physical[1]);
            int shortSide=Math.min(w,h);
            int longSide=Math.max(w,h);
            if(pcView)longSide=(int)Math.round(shortSide*16.0/9.0);
            else{
                shortSide=(int)Math.round(shortSide*percent/100.0);
                longSide=(int)Math.round(longSide*percent/100.0);
            }
            shortSide=Math.max(480,shortSide-(shortSide%2));
            longSide=Math.max(800,longSide-(longSide%2));
            saveOldSize();
            root("wm size "+shortSide+"x"+longSide);
            message((pcView?"تم طلب عرض 16:9 شبيه بالكمبيوتر. ":
                "تم طلب دقة عرض "+percent+"% من الشاشة الأصلية. ")+
                "\n"+currentSize()+"\nهذه دقة نظام Android وليست جودة خامات PUBG. "+
                "إذا تغير لمس الشاشة أو ظهرت أشرطة، استخدم زر الاستعادة.");
        }catch(Exception e){message("تعذر تغيير دقة العرض: "+e.getMessage());}
    }
    private void applyResolution(int percent){
        if(percent!=95&&percent!=100){
            message("ملف دقة غير معروف.");return;
        }
        prefs.edit().remove("clean_pc_ratio").commit();
        setRelativeScreen(percent,false);
    }
    private void confirmPcCompact(){
        new AlertDialog.Builder(this)
            .setTitle("منظور PC 16:10 — تجربة")
            .setMessage("هذا الخيار يقلّل مساحة عرض Android أكثر من 16:9، وقد يغيّر طريقة عرض اللاعب لكن لا يغيّر بعد الكاميرا أو FOV داخل PUBG. إذا ظهرت أشرطة أو تغير اللمس، اضغط استعادة. هل تريد التجربة؟")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("تفعيل",(d,w)->background(()->applyPcAspect(16,10)))
            .show();
    }
    private void applyPcAspect(int width,int height){
        try{
            if(width!=16||(height!=9&&height!=10))throw new IOException("Unsupported PC aspect");
            rootReady();
            String[] physical=physicalSize(currentSize()).split("x");
            int shortest=Math.min(Integer.parseInt(physical[0]),Integer.parseInt(physical[1]));
            int longest=(int)Math.round(shortest*width/(double)height);
            longest-=longest%2;
            saveOldSize();
            root("wm size "+shortest+"x"+longest);
            prefs.edit().putString("clean_pc_ratio","16:"+height).commit();
            message("تم تغيير شاشة Android إلى PC "+width+":"+height+
                " ("+shortest+"x"+longest+").\nقد تظهر أشرطة أو يتغير حجم اللاعب، ولا يمكن ضمان إظهار الجسم كاملًا لأن زاوية كاميرا PUBG لا تتغير.\n"+
                "من داخل اللعبة اختر أقصى TPP Camera View رسميًا. للاستعادة اضغط زر إيقاف BOOST.");
        }catch(Exception e){message("تعذر تطبيق شاشة PC: "+e.getMessage());}
    }

    private void confirmPcRatio(){
        new AlertDialog.Builder(this)
            .setTitle("تجربة عرض 16:9")
            .setMessage("يغيّر نسبة أبعاد شاشة Android مؤقتًا ولا يفتح FOV حقيقيًا داخل اللعبة. قد يظهر شريط أسود أو تتغير أماكن أزرار اللمس. هل تريد التطبيق؟")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("تطبيق",(d,w)->background(()->applyPcAspect(16,9)))
            .show();
    }
    private void showGraphicsGuide(){
        new AlertDialog.Builder(this)
            .setTitle("تحسين الرسومات دون ملفات خارجية")
            .setMessage("لأعلى ثبات قريب من تجربة 119FPS:\n\n"+
                "1. داخل PUBG > Graphics: اختر Smooth مع أعلى Frame Rate رسمي متاح حتى 120FPS.\n"+
                "2. اضبط Auto-adjust Graphics على Off إن كان الخيار متاحًا.\n"+
                "3. لجودة صور أعلى، ارفع مستوى Graphics رسميًا خطوة بخطوة، لكن قد يختفي خيار 120FPS وتنخفض الفريمات.\n"+
                "4. زر «وضوح أصلي 100%» يستخدم دقة شاشة Android الأصلية ولا يغيّر خامات اللعبة.")
            .setNegativeButton("إغلاق",null)
            .setPositiveButton("فتح PUBG",(d,w)->launchGame())
            .show();
    }
    private void showTppGuide(){
        new AlertDialog.Builder(this).setTitle("منظور TPP الواسع")
            .setMessage("افتح PUBG > Settings، وابحث عن TPP Camera View / زاوية كاميرا الشخص الثالث إن توفر في إصدارك، وارفعه لأعلى قيمة رسمية.\n\n"+
                "خيار 16:9 يغيّر أبعاد العرض فقط؛ لا يوسّع مجال الرؤية الحقيقي ولا يحوّل اللعبة إلى PC.")
            .setNegativeButton("إغلاق",null)
            .setPositiveButton("فتح اللعبة",(d,w)->launchGame())
            .show();
    }

    private void confirmHitAssist(){
        new AlertDialog.Builder(this)
            .setTitle("تقليل الدمج الوهمي — اتصال منخفض التأخير")
            .setMessage("إذا كنت تقصد إصابة الرصاص دون احتساب الضرر، فهذا مرتبط أيضًا بالسيرفر والمزامنة ولا يوجد حل مضمون من الهاتف.\n\n"+
                "الزر سيغيّر إعدادات الشبكة فعليًا عند دعمها:\n"+
                "1. تفعيل Wi-Fi Low Latency من Android.\n"+
                "2. إيقاف توفير طاقة Wi-Fi من التعريف إذا كان أمر iw مدعومًا.\n"+
                "3. حفظ الحالة السابقة لاستعادتها.\n\n"+
                "لن يغيّر أي ملف داخل PUBG. قد يزيد استهلاك البطارية. يتطلب الاتصال بشبكة Wi-Fi.")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("تطبيق التحسين",(d,w)->background(this::applyHitAssist))
            .show();
    }

    private void applyHitAssist(){
        try{
            rootReady();
            ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            Network wifi=cm==null?null:cm.getActiveNetwork();
            NetworkCapabilities caps=wifi==null||cm==null?null:cm.getNetworkCapabilities(wifi);
            if(caps==null||!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                message("تقليل الدمج الوهمي: الاتصال الحالي ليس Wi-Fi. لا تغييرات؛ هذا الوضع خاص بشبكة Wi-Fi ولا يصلح إصابات السيرفر.");
                return;
            }
            android.net.LinkProperties lp=cm.getLinkProperties(wifi);
            String iface=lp==null?"":lp.getInterfaceName();
            ArrayList<String> report=new ArrayList<>();
            report.add("تم تنفيذ أوامر تقليل تأخير اتصال Wi-Fi حيث يدعمها النظام:");
            report.addAll(new NetworkAssist(prefs).enable(iface));
            report.add("نتيجة الاتصال قد تختلف داخل PUBG؛ لا ضمان لحل الدمج الوهمي من الهاتف.");
            message(android.text.TextUtils.join("\n",report));
        }catch(Exception e){
            message("تعذّر تنفيذ تحسين الاتصال: "+e.getMessage()+
                "\nلم نغيّر بيانات PUBG. استخدم زر الاستعادة إذا طُبّق جزء من الإعدادات.");
        }
    }

    private void restoreHitAssist(){
        try{
            rootReady();
            List<String> result=new NetworkAssist(prefs).restore();
            message(android.text.TextUtils.join("\n",result));
        }catch(Exception e){message("تعذرت استعادة اتصال Wi-Fi: "+e.getMessage());}
    }

    private void checkNetwork(){
        try{
            ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
            if(cm==null){message("خدمة الشبكة غير متاحة.");return;}
            Network network=cm.getActiveNetwork();
            NetworkCapabilities caps=network==null?null:cm.getNetworkCapabilities(network);
            if(caps==null){message("ليس هناك اتصال نشط بالإنترنت.");return;}
            String access=caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)?"Wi-Fi":
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)?"Mobile Data":"Other";
            message("جاري قياس الاتصال "+access+" إلى نقطة اختبار عامة، وليس سيرفر PUBG...");
            int attempts=10,ok=0;double sum=0,maximum=0,jitter=0,last=-1;
            for(int i=0;i<attempts;i++){
                try(Socket socket=new Socket()){
                    long t=System.nanoTime();
                    socket.connect(new InetSocketAddress("1.1.1.1",443),1500);
                    double ms=(System.nanoTime()-t)/1_000_000.0;
                    sum+=ms;maximum=Math.max(maximum,ms);
                    if(last>=0)jitter+=Math.abs(ms-last);
                    last=ms;ok++;
                }catch(Exception ignored){}
                if(i<attempts-1)Thread.sleep(100);
            }
            if(ok==0){
                message("لم ينجح اتصال TCP مع نقطة الاختبار. قد يكون العنوان محجوبًا؛ لا يثبت وجود مشكلة في PUBG.");
                return;
            }
            double average=sum/ok;
            double approxJitter=ok>1?jitter/(ok-1):0;
            int failed=attempts-ok;
            String report=String.format(Locale.US,
                "الاتصال: %s\nنجاح TCP: %d/%d | فشل: %d\nمتوسط التأخير: %.1f ms\nأعلى تأخير: %.1f ms\nالتذبذب التقريبي: %.1f ms\n",
                access,ok,attempts,failed,average,maximum,approxJitter);
            report+="\nاختبار اتصال TCP إلى 1.1.1.1 وليس Ping سيرفر PUBG. "+
                "لتحسين الثبات استخدم Wi-Fi 5GHz قويًا، أوقف التحميلات وابتعد عن VPN غير الضروري.";
            message(report);
        }catch(Exception e){message("تعذر اختبار ثبات الشبكة: "+e.getMessage());}
    }

    private void confirmRestore(){
        new AlertDialog.Builder(this)
            .setTitle("استعادة إعدادات النظام")
            .setMessage("سيحاول إرجاع الحد الأدنى الأصلي لتردد CPU/GPU، ودقة Android، وتحديث الشاشة، وتوفير الطاقة، وWi-Fi Low Latency. لا يلمس PUBG. بعض قيم النواة قد تعود تلقائيًا بعد إعادة التشغيل. هل تريد الاستعادة؟")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("استعادة",(d,w)->background(this::restoreAll))
            .show();
    }
    private void restoreAll(){
        try{
            rootReady();
            ArrayList<String> out=new ArrayList<>();
            out.addAll(new KernelBoost(prefs).restore());
            String original=prefs.getString("clean_original_wm",null);
            if(original!=null){
                try{
                    java.util.regex.Matcher m=java.util.regex.Pattern
                        .compile("Override size:\\s*(\\d+x\\d+)").matcher(original);
                    if(m.find())root("wm size "+m.group(1));
                    else root("wm size reset");
                    prefs.edit().remove("clean_original_wm").remove("clean_pc_ratio").commit();
                    out.add("مقاس شاشة Android: تم استعادته.");
                }catch(Exception e){out.add("فشل استعادة المقاس: "+e.getMessage());}
            }
            undoSetting("system","tran_refresh_mode",out);
            undoSetting("system","peak_refresh_rate",out);
            undoSetting("system","min_refresh_rate",out);
            undoSetting("global","low_power",out);
            out.addAll(new NetworkAssist(prefs).restore());
            if(prefs.getBoolean("clean_requested_game_mode",false)&&gameInstalled()){
                try{
                    root("cmd game mode 1 "+PUBG);
                    prefs.edit().remove("clean_requested_game_mode").commit();
                    out.add("Game Mode: تم طلب Standard.");
                }catch(Exception e){out.add("تعذرت استعادة Game Mode: "+e.getMessage());}
            }
            if(out.isEmpty())out.add("لا توجد إعدادات محفوظة لتغييرها.");
            out.add("ملفات PUBG لم تُفتح أو تُعدّل.");
            message(android.text.TextUtils.join("\n",out));
        }catch(Exception e){message("تعذرت الاستعادة: "+e.getMessage());}
    }
    private void launchGame(){
        if(!gameInstalled()){
            message("PUBG Mobile Global غير مثبتة أو غير مرئية لهذا التطبيق.");
            return;
        }
        Intent start=getPackageManager().getLaunchIntentForPackage(PUBG);
        try{
            if(start!=null)startActivity(start);
            else message("لم أجد نشاط تشغيل PUBG.");
        }catch(Exception e){message("تعذر تشغيل PUBG: "+e.getMessage());}
    }
    @Override protected void onDestroy(){
        worker.shutdownNow();
        super.onDestroy();
    }
}
