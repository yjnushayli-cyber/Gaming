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

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs=getSharedPreferences("yrc_clean_30",MODE_PRIVATE);
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);

        LinearLayout frame=new LinearLayout(this);
        frame.setOrientation(LinearLayout.VERTICAL);
        frame.setBackgroundColor(BG);
        setContentView(frame);

        status=new TextView(this);
        status.setText("جاهز • تحسينات الشاشة والنظام فقط — لا تعديل لملفات PUBG");
        status.setTextColor(Color.WHITE);
        status.setTextSize(14);
        status.setMaxLines(4);
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
            .setTitle("تفاصيل آخر عملية").setMessage(status.getText())
            .setPositiveButton("إغلاق",null).show());

        ScrollView scroll=new ScrollView(this);
        scroll.setFillViewport(true);
        main=new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        main.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        main.setPadding(dp(19),dp(10),dp(19),dp(34));
        scroll.addView(main);
        frame.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));

        heading("YRC CLEAN • 119/120");
        info("GT30 Pro • PUBG Mobile • أربعة أقسام فقط، بدون تعديل ملفات اللعبة أو تجاوز حدود الحرارة.");

        heading("01  ثبات الفريمات");
        info("إعادة نفس فكرة تجربة 119 FPS السابقة: طلب 120Hz من XOS + وضع أداء Android. ليس قفلًا مضمونًا على 119 أو 120 FPS.");
        action("تفعيل وضع ثبات 120FPS",()->background(this::applyFpsProfile));
        action("فحص إعداد الفريمات والحرارة",()->background(this::checkFpsProfile));

        heading("02  تحسين الرسومات");
        info("جودة الشاشة من Android فقط. جودة الخامات وHDR والفريمات الحقيقية تختارها من قائمة PUBG الرسمية.");
        action("وضوح أصلي 100% — أعلى دقة للشاشة",()->background(()->applyResolution(100)));
        action("متوازن 95% — لتخفيف حمل العرض",()->background(()->applyResolution(95)));
        action("إعدادات الرسومات الرسمية الموصى بها",this::showGraphicsGuide);

        heading("03  ثبات البنق");
        info("اختبار الاتصال الخارجي وفقد الاتصال والتذبذب؛ ليس قياسًا لخادم PUBG، ولا يمكن ضمان بنق ثابت من الهاتف.");
        action("اختبار البنق والتذبذب (10 محاولات)",()->background(this::checkNetwork));
        action("فتح إعدادات Wi-Fi والشبكة",()->{
            try{startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));}
            catch(Exception e){message("تعذّر فتح إعدادات الشبكة: "+e.getMessage());}
        });

        heading("04  منظور PC");
        info("عرض شاشة Android بنسبة 16:9 شبيه بالشاشات العريضة، ولا يغيّر مجال رؤية كاميرا اللعبة FOV الحقيقي.");
        action("تفعيل شاشة 16:9 (منظور PC)",()->confirmPcRatio());
        action("ضبط TPP الواسع من PUBG رسميًا",this::showTppGuide);

        heading("التحكم والاستعادة");
        action("إرجاع إعدادات الشاشة والفريمات الأصلية",()->confirmRestore());
        action("فتح PUBG Mobile",this::launchGame);
        info("تعديلات النظام قابلة للاستعادة. لا توجد أدوات GFX/MAX أو Injection أو تعديل ملفات PUBG في هذه النسخة.");
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
        runOnUiThread(()->status.setText(text));
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
    private void applyFpsProfile(){
        try{
            rootReady();
            if(tooHot()){
                message("الحرارة مرتفعة؛ لم يتم تطبيق الثبات. اترك الجهاز يبرد أولًا.");
                return;
            }
            ArrayList<String> output=new ArrayList<>();
            changeSetting("system","tran_refresh_mode","120");
            changeSetting("system","peak_refresh_rate","120.0");
            changeSetting("system","min_refresh_rate","120.0");
            changeSetting("global","low_power","0");
            output.add("XOS: طلب 120Hz، min/peak=120Hz وتوفير الطاقة معطل.");
            if(gameInstalled()){
                try{
                    String res=root("cmd game mode 2 "+PUBG);
                    if(res.toLowerCase(Locale.US).contains("not supported")||
                        res.toLowerCase(Locale.US).contains("error")){
                        output.add("Android Game Mode: غير مدعوم / تم رفض الطلب.");
                    }else{
                        prefs.edit().putBoolean("clean_requested_game_mode",true).apply();
                        output.add("Android Game Mode: تم طلب Performance.");
                    }
                }catch(Exception e){output.add("وضع أداء اللعبة غير متاح: "+e.getMessage());}
            }else output.add("PUBG Global غير مثبتة أو غير ظاهرة للتطبيق.");
            output.add("داخل PUBG اختر Smooth وأعلى FPS رسمي متاح (120FPS عند توفره).");
            output.add("هذا طلب إعدادات وليس قياس FPS أو ضمان 119 ثابت.");
            message(android.text.TextUtils.join("\n",output));
        }catch(Exception e){message("تعذر تشغيل وضع الفريمات: "+e.getMessage());}
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
            sb.append("ملاحظة: 120Hz معدل تحديث الشاشة وليس قياس FPS الفعلي داخل PUBG. استخدم عداد اللعبة لمقارنة الثبات.");
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
        setRelativeScreen(percent,false);
    }
    private void confirmPcRatio(){
        new AlertDialog.Builder(this)
            .setTitle("تجربة عرض 16:9")
            .setMessage("يغيّر نسبة أبعاد شاشة Android مؤقتًا ولا يفتح FOV حقيقيًا داخل اللعبة. قد يظهر شريط أسود أو تتغير أماكن أزرار اللمس. هل تريد التطبيق؟")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("تطبيق",(d,w)->background(()->setRelativeScreen(100,true)))
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
            .setMessage("يعيد خيارات دقة عرض Android ومعدل التحديث وتوفير الطاقة المحفوظة بواسطة هذا التطبيق، ويطلب Game Mode القياسي إن سبق تغيير الوضع. لا يلمس أي ملف من ملفات PUBG. هل تريد الاستعادة؟")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("استعادة",(d,w)->background(this::restoreAll))
            .show();
    }
    private void restoreAll(){
        try{
            rootReady();
            ArrayList<String> out=new ArrayList<>();
            String original=prefs.getString("clean_original_wm",null);
            if(original!=null){
                try{
                    java.util.regex.Matcher m=java.util.regex.Pattern
                        .compile("Override size:\\s*(\\d+x\\d+)").matcher(original);
                    if(m.find())root("wm size "+m.group(1));
                    else root("wm size reset");
                    prefs.edit().remove("clean_original_wm").commit();
                    out.add("مقاس شاشة Android: تم استعادته.");
                }catch(Exception e){out.add("فشل استعادة المقاس: "+e.getMessage());}
            }
            undoSetting("system","tran_refresh_mode",out);
            undoSetting("system","peak_refresh_rate",out);
            undoSetting("system","min_refresh_rate",out);
            undoSetting("global","low_power",out);
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
