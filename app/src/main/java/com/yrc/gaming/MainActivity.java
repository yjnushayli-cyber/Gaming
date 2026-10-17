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

        heading("YRC GAMING PRO 4.1 • BOOST");
        info("GT30 Pro • تحسينات Android فعلية فقط، بدون تعديل ملفات PUBG أو تعطيل الحماية الحرارية.");
        info("Boost يعيد فحص الترددات الحقيقية لـ CPU/GPU، ويعيد تطبيق الحد الأدنى الآمن عند زواله دون مضاعفة الرفع. قد يزيد الحرارة والبطارية، ولا يضمن 119FPS ثابتًا.");
        action("تفعيل YRC COMPETITIVE BOOST",this::confirmCompetitiveBoost);
        action("إيقاف BOOST واستعادة الإعدادات",this::confirmRestore);

        heading("01  ثبات الفريمات");
        info("نفس إعداد 120Hz الذي اقترب معك سابقًا من 119FPS. وضع Game Mode غير مدعوم للعبة على جهازك؛ سيتم تجاوزه إن رفضه XOS. لا نغيّر حد التحديث الأدنى الديناميكي ولا نضمن قفل FPS.");
        action("تفعيل وضع ثبات 120FPS",()->background(this::applyFpsProfile));

        heading("02  تحسين الرسومات");
        info("جودة الشاشة من Android فقط. جودة الخامات وHDR والفريمات الحقيقية تختارها من قائمة PUBG الرسمية.");
        action("وضوح أصلي 100% — أعلى دقة للشاشة",()->background(()->applyResolution(100)));
        action("متوازن 95% — لتخفيف حمل العرض",()->background(()->applyResolution(95)));
        action("إعدادات الرسومات الرسمية الموصى بها",this::showGraphicsGuide);

        heading("03  ثبات البنق");
        info("وضع Wi-Fi Low Latency رسمي من Android قد يقلّل التأخير إذا دعمه XOS. يزيد استهلاك البطارية ولا يعمل على بيانات الجوال. الاختبار ليس قياسًا لسيرفر PUBG.");
        action("تفعيل Wi-Fi Low Latency (تجريبي)",()->background(()->setLowLatency(true)));
        action("إلغاء Wi-Fi Low Latency",()->background(()->setLowLatency(false)));
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
        action("إرجاع إعدادات الشاشة والفريمات الأصلية",()->confirmRestore());
        action("فتح PUBG Mobile",this::launchGame);
        info("زر الاستعادة يرجع إعدادات CPU/GPU التي حفظها التطبيق ودقة الشاشة وتحديثها والشبكة. قد يرفض XOS بعض إعدادات النواة. لا توجد تغييرات على ملفات اللعبة.");
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
    private void confirmCompetitiveBoost(){
        new AlertDialog.Builder(this)
            .setTitle("YRC Competitive Boost")
            .setMessage("سينفّذ تغييرات فعلية بنظام Android:\n"+
                "• طلب 120Hz وإيقاف توفير الطاقة.\n"+
                "• رفع محدود وقابل للاستعادة للحد الأدنى لتردد CPU/GPU إذا دعمته النواة.\n"+
                "• دقة شاشة 95% لتخفيف الحمل، مع الحفاظ على منظور PC إذا كان مفعّلًا.\n"+
                "• طلب Wi-Fi Low Latency عند الاتصال بالواي فاي.\n\n"+
                "قد يزيد استهلاك البطارية والحرارة أو يُرفض بعض التغييرات. لا يتجاوز الحدود الحرارية ولا يعدّل ملفات PUBG. استخدم زر الاستعادة بعد اللعب.")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("تفعيل",(d,w)->background(this::applyCompetitiveBoost))
            .show();
    }
    private void applyCompetitiveBoost(){
        ArrayList<String> report=new ArrayList<>();
        try{
            rootReady();
            if(tooHot()){
                message("حرارة الجهاز مرتفعة. لم يبدأ Competitive Boost حفاظًا على الجهاز.");
                return;
            }
            // A single session changes only whitelisted Android sysfs and settings.
            report.add("YRC Competitive Boost — النتيجة الفعلية");
            try{
                changeSetting("system","tran_refresh_mode","120");
                changeSetting("system","peak_refresh_rate","120.0");
                changeSetting("global","low_power","0");
                report.add("الشاشة: XOS="+readSetting("system","tran_refresh_mode")+
                    " | peak="+readSetting("system","peak_refresh_rate"));
                if(prefs.contains(prefName("system","min_refresh_rate"))){
                    undoSetting("system","min_refresh_rate",report);
                }
            }catch(Exception e){report.add("الشاشة: تعذر أحد التغييرات: "+e.getMessage());}

            // CPU/GPU floor on *documented* frequency nodes, no voltage/thermal/governor hacks.
            KernelBoost kernel=new KernelBoost(prefs);
            report.addAll(kernel.apply());

            // 95% Android screen resolution only; preserve PC aspect if set.
            if(prefs.contains("clean_pc_ratio")){
                report.add("الرسومات: احتفظنا بنسبة PC "+prefs.getString("clean_pc_ratio","")+
                    " حتى لا يتغير المنظور. لخفض الدقة يمكنك اختيار 95% يدويًا.");
            }else{
                try{
                    String[] dims=physicalSize(currentSize()).split("x");
                    int a=Integer.parseInt(dims[0]),b=Integer.parseInt(dims[1]);
                    int x=Math.max(480,((int)Math.round(a*.95)/2)*2);
                    int y=Math.max(800,((int)Math.round(b*.95)/2)*2);
                    saveOldSize();
                    root("wm size "+x+"x"+y);
                    report.add("دقة Android: "+x+"x"+y+" (95% من البكسلات الطولية والعرضية).");
                }catch(Exception e){report.add("تخفيف الدقة: غير متاح "+e.getMessage());}
            }
            try{
                ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                Network n=cm==null?null:cm.getActiveNetwork();
                NetworkCapabilities caps=n==null||cm==null?null:cm.getNetworkCapabilities(n);
                if(caps!=null&&caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                    String output=root("cmd wifi force-low-latency-mode enabled");
                    String lowered=output.toLowerCase(Locale.US);
                    if(lowered.contains("error")||lowered.contains("failed")||
                        lowered.contains("not supported")||lowered.contains("unknown")||
                        lowered.contains("exception")||lowered.contains("usage")){
                        report.add("Wi-Fi Low Latency: رفضه XOS — "+output);
                    }else{
                        prefs.edit().putBoolean("clean_wifi_latency_on",true).commit();
                        report.add("Wi-Fi Low Latency: تم إرسال طلب التفعيل.");
                    }
                }else report.add("Wi-Fi Low Latency: لم يفعّل؛ الاتصال الحالي ليس Wi-Fi.");
            }catch(Exception e){report.add("Wi-Fi Low Latency: تعذر الطلب "+e.getMessage());}

            report.add("لا تعديل لملفات PUBG. تفعيل 120Hz لا يعني ضمان 120FPS.");
            report.add("عند الانتهاء اضغط «إيقاف BOOST واستعادة الإعدادات».");
            message(android.text.TextUtils.join("\n",report));
        }catch(Exception e){
            report.add("تعذر تنفيذ Boost: "+e.getMessage());
            report.add("قد تكون تغييرات جزئية قد نُفذت؛ استعد إعدادات النظام من الزر.");
            message(android.text.TextUtils.join("\n",report));
        }
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

    private void setLowLatency(boolean enable){
        try{
            rootReady();
            if(enable){
                ConnectivityManager cm=(ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
                Network net=cm==null?null:cm.getActiveNetwork();
                NetworkCapabilities caps=net==null||cm==null?null:cm.getNetworkCapabilities(net);
                if(caps==null||!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)){
                    message("Wi-Fi Low Latency يعمل على Wi-Fi فقط. اتصل بالشبكة قبل التفعيل.");
                    return;
                }
            }
            String option=enable?"enabled":"disabled";
            // AOSP documented shell command; can be absent or unsupported on OEM firmware.
            String out=root("cmd wifi force-low-latency-mode "+option);
            String lower=out.toLowerCase(Locale.US);
            if(lower.contains("failed")||lower.contains("error")||
               lower.contains("unknown")||lower.contains("not supported")||
               lower.contains("exception")||lower.contains("usage")){
                message("لم يقبل XOS تغيير وضع تأخير Wi-Fi. لم نعتبر العملية ناجحة:\n"+out);
                return;
            }
            prefs.edit().putBoolean("clean_wifi_latency_on",enable).commit();
            message(enable?
                "تم إرسال طلب Wi-Fi Low Latency للنظام. قد يقلل التأخير مع Wi-Fi المدعوم، لكنه لا يثبت بنق PUBG ولا يضمن ثباته. قد يزيد استهلاك البطارية. اضغط إلغاء بعد اللعب.":
                "تم إرسال طلب إلغاء Wi-Fi Low Latency وإعادة الشبكة للوضع المعتاد.");
        }catch(Exception e){message("لم يمكن تغيير وضع Wi-Fi Low Latency: "+e.getMessage());}
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
            if(prefs.getBoolean("clean_wifi_latency_on",false)){
                try{
                    String wifiResult=root("cmd wifi force-low-latency-mode disabled");
                    if(wifiResult.toLowerCase(Locale.US).contains("failed"))
                        throw new IOException(wifiResult);
                    prefs.edit().remove("clean_wifi_latency_on").commit();
                    out.add("Wi-Fi Low Latency: تم طلب الإيقاف");
                }catch(Exception e){
                    out.add("تعذر إلغاء Wi-Fi Low Latency: "+e.getMessage());
                }
            }
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
