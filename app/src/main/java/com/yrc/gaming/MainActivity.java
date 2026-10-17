package com.yrc.gaming;

import android.app.*;
import android.os.*;
import android.content.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
  private final ExecutorService executor=Executors.newSingleThreadExecutor();
  private android.content.SharedPreferences prefs;
  private TextView status;
  private LinearLayout layout;
  private final int BG=Color.rgb(12,18,29), ACCENT=Color.rgb(56,207,174);
  private final String[] PACKAGES={"com.tencent.ig","com.pubg.imobile","com.vng.pubgmobile","com.rekoo.pubgm"};

  @Override public void onCreate(Bundle savedInstanceState){
    super.onCreate(savedInstanceState);
    getWindow().setStatusBarColor(BG);
    getWindow().setNavigationBarColor(BG);
    prefs=getSharedPreferences("yrc_profiles",MODE_PRIVATE);
    // The clean edition re-enables the protection once for users upgrading from older builds.
    if(!prefs.getBoolean("clean_v2_initialized",false)){
      prefs.edit().putBoolean("account_safety_mode",true)
        .putBoolean("clean_v2_initialized",true).commit();
    }
    LinearLayout screen=new LinearLayout(this);
    screen.setOrientation(LinearLayout.VERTICAL);
    screen.setBackgroundColor(BG);
    setContentView(screen);

    status=new TextView(this);
    status.setText("جاهز • وضع أمان الحساب مفعّل. اضغط هنا لقراءة تفاصيل أي نتيجة.");
    status.setTextColor(Color.WHITE);
    status.setTextSize(14);
    status.setMaxLines(5);
    status.setEllipsize(android.text.TextUtils.TruncateAt.END);
    status.setPadding(dp(14),dp(12),dp(14),dp(12));
    android.graphics.drawable.GradientDrawable statusBg=new android.graphics.drawable.GradientDrawable();
    statusBg.setColor(Color.rgb(21,37,48));
    statusBg.setCornerRadius(dp(14));
    statusBg.setStroke(dp(1),ACCENT);
    status.setBackground(statusBg);
    LinearLayout.LayoutParams slp=new LinearLayout.LayoutParams(-1,-2);
    slp.setMargins(dp(12),dp(12),dp(12),dp(6));
    screen.addView(status,slp);
    status.setOnClickListener(v->new AlertDialog.Builder(this)
      .setTitle("نتيجة آخر عملية")
      .setMessage(status.getText())
      .setPositiveButton("إغلاق",null).show());

    ScrollView scroll=new ScrollView(this);
    scroll.setFillViewport(true);
    layout=new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    layout.setPadding(dp(18),dp(12),dp(18),dp(36));
    scroll.addView(layout);
    screen.addView(scroll,new LinearLayout.LayoutParams(-1,0,1f));

    title("YRC Gaming Pro  |  Clean 2.0");
    label("Infinix GT30 Pro • PUBG Mobile • نسخة مركزة على ما تحتاجه");

    title("01  |  حماية الحساب");
    label("حماية وقائية: تمنع YRC من كتابة ملفات اللعبة، وتفحص UserCustom.ini وActive.sav قبل تشغيل PUBG من التطبيق، وتوقف التشغيل عند اكتشاف إعدادات مشبوهة. لا تخفي التعديلات عن مكافحة الغش ولا تمنع فتح اللعبة من خارج YRC.");
    CheckBox accountSafety=new CheckBox(this);
    accountSafety.setText("وضع الأمان — منع كتابة تعديلات في ملفات اللعبة");
    accountSafety.setTextSize(16);
    accountSafety.setTextColor(Color.WHITE);
    accountSafety.setChecked(prefs.getBoolean("account_safety_mode",true));
    layout.addView(accountSafety);
    accountSafety.setOnCheckedChangeListener((control,checked)->{
      if(checked){
        prefs.edit().putBoolean("account_safety_mode",true).apply();
        message("وضع الأمان مفعّل. لا تُكتب تعديلات جديدة في UserCustom.ini أو Active.sav.");
      }else{
        new AlertDialog.Builder(this)
          .setTitle("تعطيل وضع الأمان؟")
          .setMessage("هذا يتيح تعديلات GFX وMAX غير الرسمية داخل ملفات PUBG، وقد يؤدي إلى تقييد الحساب أو الجهاز. لا توجد حماية Anti-Ban مضمونة.")
          .setNegativeButton("إبقاء الحماية",(d,w)->accountSafety.setChecked(true))
          .setPositiveButton("أفهم المخاطر",(d,w)->{
            prefs.edit().putBoolean("account_safety_mode",false).apply();
            message("تم تعطيل الحماية بطلبك. تعديلات الملفات غير الرسمية قد تؤدي إلى حظر الحساب.");
          })
          .setOnCancelListener(d->accountSafety.setChecked(true))
          .show();
      }
    });
    button("فحص سلامة ملفات PUBG قبل اللعب",()->run(()->preflightGameSafety(false)));
    button("تشغيل آمن بفحص الملفات أولًا",()->launchGame());
    button("إرشادات إصلاح اللعبة الرسمي",()->showOfficialRepairGuide());
    button("فحص صلاحية الروت والحماية",()->run(()->diagnoseRootAccess()));

    title("02  |  ثبات الفريمات");
    label("طلب 120Hz ووضع الأداء لجهاز GT30 Pro بدون رفع الحد الرسمي للعبة أو تعطيل حماية الحرارة. لا يضمن ثبات FPS.");
    button("تفعيل وضع ثبات 120FPS",()->run(()->applyStable120()));
    button("إلغاء وضع ثبات 120FPS",()->run(()->restoreStable120()));

    title("03  |  البنق والشبكة");
    label("قياس فقد الحزم والتذبذب إلى الراوتر. لا يغيّر خوادم PUBG ولا يضمن تثبيت البنق.");
    button("فحص البنق والتذبذب وفقد الحزم",()->run(()->testWifi()));
    button("فتح إعدادات Wi-Fi",()->{
      try{startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));}
      catch(Exception e){message("تعذر فتح إعدادات Wi-Fi: "+e.getMessage());}
    });

    title("04  |  منظور PC");
    label("أقرب خيار آمن هو توسيع زاوية كاميرا TPP من إعدادات PUBG. خيار 16:9 يغيّر مساحة شاشة Android مؤقتًا، ولا يفتح FOV حقيقيًا.");
    button("ضبط منظور TPP الواسع داخل اللعبة",()->showPcViewGuide());
    button("تفعيل عرض 16:9 شبيه بالكمبيوتر",()->new AlertDialog.Builder(this)
      .setTitle("شاشة 16:9")
      .setMessage("سيغيّر مقاس عرض Android فقط وقد يغيّر مكان اللمس أو يظهر أشرطة سوداء. لا يزيد زاوية كاميرا PUBG الحقيقية.")
      .setNegativeButton("إلغاء",null)
      .setPositiveButton("تفعيل",(d,w)->run(()->applyPcScreen())).show());
    button("إرجاع أبعاد الشاشة",()->run(()->restorePcScreen()));

    title("05  |  تعديلات الرسومات الجديدة (اختيارية)");
    label("GFX / HDR / UltraHD / MAX تعدّل ملفات اللعبة ولا تضمن ظهور الجودة أو 120FPS. تبقى مقفلة ما دام وضع الأمان مفعّلًا.");
    button("فحص دعم Active.sav لهذا الإصدار",()->run(()->inspectActiveSav()));
    button("تجربة GFX HDR + طلب 120FPS",()->confirmActiveSavPreset(5));
    button("تجربة GFX UltraHD + طلب 120FPS",()->confirmActiveSavPreset(6));
    button("تطبيق MAX Graphics — تجريبي ثقيل",()->confirmMaxGraphics());
    button("فحص إعدادات GFX بعد اللعب",()->run(()->verifyActiveSavAfterGame()));
    button("فحص إعدادات MAX بعد اللعب",()->run(()->verifyIniQualityAfterGame()));

    title("06  |  العودة إلى النسخة الأصلية");
    label("زر واحد يستعيد ملفات PUBG من نسخ YRC الاحتياطية المتاحة ويعيد إعدادات الشاشة/الأداء التي حفظها التطبيق. لا يمسح اللعبة أو الحساب؛ النسخة الاحتياطية قد تكون أصلًا معدّلة.");
    button("إلغاء جميع تعديلات YRC والرجوع للنسخ المحفوظة",()->confirmRestoreOriginal());
    button("تشغيل PUBG Mobile",()->launchGame());
    label("الاستعادة من نسخة YRC ليست إصلاحًا مضمونًا لكل ملفات PUBG. إذا فقدت النسخة الأصلية، استخدم ميزة Repair الرسمية داخل اللعبة.");
  }

  private int dp(int value){
    return Math.round(getResources().getDisplayMetrics().density*value);
  }
  private void title(String name){
    TextView t=label(name);
    t.setTextSize(20);
    t.setTypeface(android.graphics.Typeface.DEFAULT,android.graphics.Typeface.BOLD);
    t.setTextColor(ACCENT);
    t.setPadding(0,dp(23),0,dp(8));
  }
  private TextView label(String text){
    TextView t=new TextView(this);
    t.setText(text);
    t.setTextSize(14);
    t.setLineSpacing(dp(3),1f);
    t.setTextColor(Color.rgb(221,229,236));
    t.setPadding(dp(3),dp(7),dp(3),dp(10));
    layout.addView(t);
    return t;
  }
  private void button(String name,Runnable action){
    Button b=new Button(this);
    b.setText(name);
    b.setTextSize(14);
    b.setAllCaps(false);
    b.setTextColor(Color.WHITE);
    boolean restore=name.contains("إلغاء جميع")||name.contains("الرجوع للنسخ");
    boolean diagnostic=name.contains("فحص")||name.contains("فتح إعدادات");
    int bg=restore?Color.rgb(129,72,36):diagnostic?Color.rgb(43,67,79):Color.rgb(38,109,103);
    android.graphics.drawable.GradientDrawable drawable=new android.graphics.drawable.GradientDrawable();
    drawable.setColor(bg);
    drawable.setCornerRadius(dp(11));
    drawable.setStroke(dp(1),restore?Color.rgb(205,140,78):Color.rgb(69,144,140));
    b.setBackground(drawable);
    LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(52));
    lp.setMargins(0,dp(3),0,dp(7));
    layout.addView(b,lp);
    b.setOnClickListener(v->action.run());
  }
  private void run(Runnable r){executor.execute(r);}
  private void message(String msg){runOnUiThread(()->status.setText(msg));}

  private boolean gameFileWritesBlocked(String feature){
    if(prefs.getBoolean("account_safety_mode",true)){
      message("وضع أمان الحساب مفعّل: تم منع "+feature+
        " من تعديل ملفات PUBG. هذه الحماية تقلل التعديل غير المقصود ولا تضمن عدم الحظر. "+
        "يمكن استرجاع ملفاتك من أزرار الاستعادة بعد إغلاق PUBG.");
      return true;
    }
    return false;
  }

  private void showAccountSafetyStatus(){
    boolean enabled=prefs.getBoolean("account_safety_mode",true);
    message(enabled?
      "وضع الأمان: مفعّل. يمنع تعديل الملفات من أزرار GFX/MAX/الرندر داخل YRC. "+
      "التعديلات القديمة لا تُحذف تلقائيًا. لا توجد حماية تضمن عدم حظر الحساب.":
      "وضع الأمان: معطّل. يسمح بالتعديل غير الرسمي لملفات PUBG وقد يؤدي للحظر. "+
      "لخفض المخاطر فعّل الحماية واستعد ملف اللعبة الأصلي من نسخة موثوقة أو أعد تثبيت PUBG رسميًا.");
  }

  private String root(String cmd)throws Exception{
    String[] suPaths={"su","/system/bin/su","/system/xbin/su","/sbin/su","/su/bin/su"};
    IOException launchFailure=null;
    for(String suPath:suPaths){
      if(!"su".equals(suPath)&&!new java.io.File(suPath).exists())continue;
      java.lang.Process p;
      try{
        p=new ProcessBuilder(suPath,"-c",cmd).redirectErrorStream(true).start();
      }catch(IOException error){
        launchFailure=error;
        continue;
      }
      ByteArrayOutputStream out=new ByteArrayOutputStream();
      byte[] buf=new byte[1024];int n;
      try(InputStream in=p.getInputStream()){
        while((n=in.read(buf))!=-1){
          if(out.size()<16000)out.write(buf,0,Math.min(n,16000-out.size()));
        }
      }
      int code=p.waitFor();
      String result=out.toString("UTF-8").trim();
      if(code!=0){
        throw new IOException("تم تشغيل su لكن الأمر رُفض (رمز "+code+"). "+result+
          "\nراجع تصريح YRC Gaming Pro في KernelSU Next > Superuser.");
      }
      return result;
    }
    throw new IOException("تعذر تشغيل su من التطبيق (Permission denied / غير متاح)."+
      "\nافتح KernelSU Next > Superuser > YRC Gaming Pro (com.yrc.gaming) وفعّل صلاحية الروت."+
      "\nإذا لم يظهر التطبيق في القائمة، افتح YRC واضغط فحص صلاحية الروت."+
      (launchFailure==null?"":"\nالسبب التقني: "+launchFailure.getMessage()));
  }

  private void diagnoseRootAccess(){
    StringBuilder report=new StringBuilder("فحص صلاحية الروت لـ YRC Gaming Pro\n"+
      "الحزمة: "+getPackageName()+"\nUID: "+android.os.Process.myUid()+"\n");
    try{
      java.lang.Process probe=new ProcessBuilder("/system/bin/sh","-c",
        "command -v su 2>/dev/null || true; for p in /system/bin/su /system/xbin/su /sbin/su /su/bin/su; do if [ -e \"$p\" ]; then ls -l \"$p\"; fi; done")
        .redirectErrorStream(true).start();
      ByteArrayOutputStream out=new ByteArrayOutputStream();
      byte[] buf=new byte[512];int n;
      try(InputStream in=probe.getInputStream()){
        while((n=in.read(buf))!=-1){
          if(out.size()<2200)out.write(buf,0,Math.min(n,2200-out.size()));
        }
      }
      probe.waitFor();
      report.append("المسارات:\n").append(out.toString("UTF-8").trim()).append("\n");
    }catch(Exception e){
      report.append("فحص المسارات تعذر: ").append(e.getMessage()).append("\n");
    }
    try{
      String uid=root("id -u").trim();
      report.append("اختبار su: ").append("0".equals(uid)?"صلاحية الروت تعمل":"لم نحصل على UID 0: "+uid);
    }catch(Exception e){
      report.append("اختبار su فشل:\n").append(e.getMessage());
    }
    message(report.toString());
  }

  private String pkg(){
    for(String p:PACKAGES){try{getPackageManager().getPackageInfo(p,0);return p;}catch(Exception ignored){}}
    return null;
  }

  private int temperature(){
    Intent i=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    return i==null?-1:i.getIntExtra("temperature",-1);
  }

  private int thermal(){
    PowerManager p=(PowerManager)getSystemService(POWER_SERVICE);
    return Build.VERSION.SDK_INT>=29?p.getCurrentThermalStatus():0;
  }

  private void applyPcScreen(){
    try{
      String current=root("wm size");
      String physical=null;
      for(String line:current.split("\n"))if(line.startsWith("Physical size:"))physical=line.substring(14).trim();
      if(physical==null||!physical.matches("[0-9]+x[0-9]+"))throw new IOException("تعذر معرفة الدقة الأصلية: "+current);
      String[] dims=physical.split("x");
      int shortSide=Math.min(Integer.parseInt(dims[0]),Integer.parseInt(dims[1]));
      int longSide=(int)Math.round(shortSide*16.0/9.0);
      longSide-=longSide%2;
      if(!prefs.contains("pc_old_size"))prefs.edit().putString("pc_old_size",current).commit();
      root("wm size "+shortSide+"x"+longSide);
      message("تم تطبيق شاشة PC 16:9: "+root("wm size")+"\nللرجوع اضغط «استعادة الشاشة الأصلية».");
    }catch(Exception e){message("تعذر تفعيل شاشة PC: "+e.getMessage());}
  }

  private void restorePcScreen(){
    try{
      String old=prefs.getString("pc_old_size","");
      if(old.isEmpty()){message("لا يوجد مقاس سابق محفوظ. لم نغيّر الشاشة.");return;}
      String override=null;
      for(String line:old.split("\n"))if(line.startsWith("Override size:"))override=line.substring(14).trim();
      if(override!=null&&override.matches("[0-9]+x[0-9]+"))root("wm size "+override);
      else root("wm size reset");
      prefs.edit().remove("pc_old_size").commit();
      message("تمت استعادة الشاشة: "+root("wm size"));
    }catch(Exception e){message("تعذرت الاستعادة: "+e.getMessage());}
  }

  private void testWifi(){
    try{
      android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
      android.net.Network n=cm.getActiveNetwork();
      android.net.NetworkCapabilities cap=cm.getNetworkCapabilities(n);
      if(cap==null||!cap.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)){
        message("الاتصال النشط ليس Wi-Fi. اتصل بالشبكة ثم أعد الاختبار.");return;
      }
      android.net.LinkProperties lp=cm.getLinkProperties(n);
      java.net.InetAddress gateway=null;
      if(lp!=null)for(android.net.RouteInfo route:lp.getRoutes()){
        if(route.isDefaultRoute()&&route.getGateway()!=null){gateway=route.getGateway();break;}
      }
      if(gateway==null){message("تعذر اكتشاف عنوان الراوتر تلقائيًا.");return;}
      String ip=gateway.getHostAddress();
      if(!ip.matches("[0-9a-fA-F:.%]+")){message("عنوان راوتر غير متوقع.");return;}
      message("جاري قياس فقد الحزم والتذبذب مع الراوتر: "+ip+" ...");
      java.lang.Process p=new ProcessBuilder("ping","-n","-c","10","-i","0.3","-W","1",ip).redirectErrorStream(true).start();
      ByteArrayOutputStream out=new ByteArrayOutputStream();
      byte[] buf=new byte[2048];int count;
      try(InputStream in=p.getInputStream()){while((count=in.read(buf))!=-1){out.write(buf,0,count);if(out.size()>12000)break;}}
      p.waitFor();
      String raw=out.toString("UTF-8");
      java.util.regex.Matcher loss=java.util.regex.Pattern.compile("([0-9]+(?:\\.[0-9]+)?)% packet loss").matcher(raw);
      java.util.regex.Matcher times=java.util.regex.Pattern.compile("time[=<]([0-9]+(?:\\.[0-9]+)?)\\s*ms").matcher(raw);
      ArrayList<Double> vals=new ArrayList<>();
      while(times.find())vals.add(Double.parseDouble(times.group(1)));
      if(!loss.find()||vals.isEmpty()){message("فشل اختبار الراوتر؛ قد يكون ICMP محظورًا. النتيجة لا تثبت وجود مشكلة بالإنترنت. "+raw.substring(0,Math.min(400,raw.length())));return;}
      double sum=0,jitter=0,max=0;
      for(int i=0;i<vals.size();i++){double v=vals.get(i);sum+=v;max=Math.max(max,v);if(i>0)jitter+=Math.abs(v-vals.get(i-1));}
      double avg=sum/vals.size();jitter=vals.size()>1?jitter/(vals.size()-1):0;
      String report=String.format(java.util.Locale.US,"Wi-Fi → الراوتر %s\nفقد الحزم: %s%%\nمتوسط الاستجابة: %.1f ms\nأعلى استجابة: %.1f ms\nالتذبذب التقريبي: %.1f ms\n",ip,loss.group(1),avg,max,jitter);
      double lost=Double.parseDouble(loss.group(1));
      if(lost>0||jitter>15||max>80)report+="النتيجة: تذبذب محلي محتمل؛ جرّب 5GHz والاقتراب من الراوتر وإيقاف التحميلات.";
      else report+="الاتصال بالراوتر مستقر في هذه العينة؛ قد يكون تذبذب PUBG خارج الشبكة المحلية.";
      message(report);
    }catch(Exception e){message("تعذر اختبار Wi-Fi: "+e.getMessage());}
  }

  private String iniDecode(String hex){
    StringBuilder out=new StringBuilder();
    for(int i=0;i<hex.length();i+=2)out.append((char)(Integer.parseInt(hex.substring(i,i+2),16)^0x79));
    return out.toString();
  }

  private String iniEncode(String val){
    StringBuilder out=new StringBuilder();
    for(int i=0;i<val.length();i++)out.append(String.format(java.util.Locale.US,"%02X",((int)val.charAt(i))^0x79));
    return out.toString();
  }

  private String iniBackupPath(){return "/sdcard/Download/YRC-PUBG-UserCustom-before-clarity.ini";}

  private String activeSavPath(){
    return "/sdcard/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames/Active.sav";
  }

  private String activeSavBackup(){
    return "/sdcard/Download/YRC-PUBG-Active-before-GFX.sav";
  }

  private String rootSav(String command)throws Exception{
    // Ordinary diagnostics truncate at 16 KB; serialized UE4 saves can be larger.
    String[] suPaths={"su","/system/bin/su","/system/xbin/su","/sbin/su","/su/bin/su"};
    IOException launchFailure=null;
    for(String binary:suPaths){
      if(!"su".equals(binary)&&!new java.io.File(binary).exists())continue;
      java.lang.Process proc;
      try{
        proc=new ProcessBuilder(binary,"-c",command).redirectErrorStream(true).start();
      }catch(IOException ex){launchFailure=ex;continue;}
      ByteArrayOutputStream out=new ByteArrayOutputStream();
      byte[] buf=new byte[4096];int n;boolean overflow=false;
      try(InputStream in=proc.getInputStream()){
        while((n=in.read(buf))!=-1){
          if(out.size()+n>512000){overflow=true;proc.destroy();break;}
          out.write(buf,0,n);
        }
      }
      int code=proc.waitFor();
      if(overflow)throw new IOException("ملف الإعدادات كبير جدًا للفحص الآمن");
      String val=out.toString("UTF-8").trim();
      if(code!=0)throw new IOException("رمز "+code+": "+val);
      return val;
    }
    throw new IOException("تعذر تشغيل صلاحية الروت من YRC: "+(launchFailure==null?"لا يوجد su":launchFailure.getMessage()));
  }

  private byte[] activeSavBytes()throws Exception{
    String path=activeSavPath();
    String sizeText=root("if [ -f '"+path+"' ]; then wc -c < '"+path+"'; else echo MISSING; fi").trim();
    if("MISSING".equals(sizeText))throw new IOException("ملف Active.sav غير موجود في Saved/SaveGames على هذا الإصدار");
    int size=Integer.parseInt(sizeText);
    if(size<100||size>320000)throw new IOException("حجم Active.sav غير متوقع: "+size);
    String encoded=rootSav("base64 '"+path+"'");
    byte[] data=android.util.Base64.decode(encoded,android.util.Base64.DEFAULT);
    if(data.length!=size)throw new IOException("القراءة غير مكتملة: "+data.length+" من "+size);
    if(data[0]!='G'||data[1]!='V'||data[2]!='A'||data[3]!='S')
      throw new IOException("صيغة Active.sav ليست UE4 GVAS المعروفة؛ لن نجرب تعديلًا مجهولًا");
    return data;
  }

  private int savInt(byte[] data,int offset){
    if(offset<0||offset>data.length-4)return Integer.MIN_VALUE;
    return (data[offset]&255)|((data[offset+1]&255)<<8)|
      ((data[offset+2]&255)<<16)|((data[offset+3]&255)<<24);
  }

  private void putSavInt(byte[] data,int offset,int value){
    for(int i=0;i<4;i++)data[offset+i]=(byte)(value>>>(8*i));
  }

  private int activePropertyOffset(byte[] bytes,String name)throws IOException{
    byte[] key=name.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
    int found=-1;
    for(int at=4;at+key.length+31<bytes.length;at++){
      if(savInt(bytes,at-4)!=key.length+1)continue;
      boolean match=true;
      for(int j=0;j<key.length;j++)if(bytes[at+j]!=key[j]){match=false;break;}
      if(!match||bytes[at+key.length]!=0)continue;
      int pos=at+key.length+1;
      byte[] type="IntProperty".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
      if(savInt(bytes,pos)!=type.length+1)continue;
      pos+=4;
      for(int j=0;j<type.length;j++)if(bytes[pos+j]!=type[j]){match=false;break;}
      if(!match||bytes[pos+type.length]!=0)continue;
      pos+=type.length+1;
      // UE4 GVAS IntProperty: 4-byte payload size, 4-byte array index,
      // one-byte 'has property GUID' flag, then the 4-byte value.
      if(savInt(bytes,pos)!=4||savInt(bytes,pos+4)!=0||bytes[pos+8]!=0)continue;
      int valueOffset=pos+9;
      if(valueOffset>bytes.length-4)continue;
      if(found!=-1)throw new IOException("توجد نسختان من "+name+"؛ أُلغي تعديل Active.sav");
      found=valueOffset;
    }
    return found;
  }

  private void inspectActiveSav(){
    try{
      root("id");
      String path=activeSavPath();
      String has=root("if [ -f '"+path+"' ]; then echo FOUND; else echo MISSING; fi");
      if(!"FOUND".equals(has)){
        String rootDir="/sdcard/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/SaveGames";
        String names=root("if [ -d '"+rootDir+"' ]; then ls -1 '"+rootDir+"' | head -25; else echo 'SaveGames directory missing'; fi");
        message("لم أجد Active.sav بالمسار المعتاد.\nالملفات الموجودة:\n"+names+
          "\nهذا يعني أن طريقة ملفات GFX المنشورة قد لا تنطبق على إصدارك 4.6.0.");
        return;
      }
      byte[] bytes=activeSavBytes();
      String[] keys={"ArtQuality","FPSLevel","BattleFPS","BattleRenderQuality",
        "BattleRenderStyle","LobbyFPS","LobbyRenderQuality"};
      StringBuilder report=new StringBuilder("فحص Active.sav (قراءة فقط)\nصيغة: GVAS / الحجم: "+bytes.length+" bytes\n");
      for(String key:keys){
        int pos=activePropertyOffset(bytes,key);
        report.append(key).append(" = ").append(pos<0?"غير موجود":savInt(bytes,pos)).append('\n');
      }
      String sha=root("sha256sum '"+path+"'").trim().split(" ")[0];
      report.append("SHA256: ").append(sha).append("\n"+
        "قيم بعض أدوات GameLoop: FPS=8 لطلب 120، RenderQuality=5 HDR / 6 UltraHD.\n"+
        "تلك القيم ليست معتمدة رسميًا لـ PUBG Mobile 4.6.0، ولا تضمن تفعيلها على جهازك.");
      message(report.toString());
    }catch(Exception e){message("فشل فحص Active.sav: "+e.getMessage());}
  }

  private void confirmActiveSavPreset(int graphics){
    final String quality=graphics==5?"HDR":"UltraHD";
    new AlertDialog.Builder(this).setTitle("GFX Active.sav — "+quality+" / 120FPS")
      .setMessage("سيحاول تغيير 3 قيم فقط داخل Active.sav: BattleRenderQuality="+graphics+
        " وBattleFPS=8 وFPSLevel=8، مع نسخ الملف الأصلي قبل التعديل. هذه القيم مأخوذة من أدوات GameLoop وقد تختلف في نسخة الجوال. تعديل ملف حفظ اللعبة غير رسمي وقد لا يعمل أو يسبب مشاكل/حظرًا. أغلق PUBG أولًا.")
      .setNegativeButton("إلغاء",null)
      .setPositiveButton("تجربة",(d,w)->run(()->applyActiveSavPreset(graphics))).show();
  }

  private void applyActiveSavPreset(int graphics){
    if(gameFileWritesBlocked("applyActiveSavPreset"))return;
    String stage="البدء";
    try{
      if(graphics!=5&&graphics!=6){message("ملف جودة غير معروف");return;}
      stage="التحقق من إغلاق PUBG";
      root("id");
      if(!root("pidof com.tencent.ig 2>/dev/null || true").trim().isEmpty()){
        message("أغلق PUBG تمامًا قبل تعديل Active.sav.");return;
      }
      String path=activeSavPath(),backup=activeSavBackup();
      stage="فحص بنية ملف الحفظ";
      byte[] original=activeSavBytes();
      byte[] target=java.util.Arrays.copyOf(original,original.length);
      java.util.LinkedHashMap<String,Integer> desired=new java.util.LinkedHashMap<>();
      desired.put("BattleRenderQuality",graphics);
      desired.put("BattleFPS",8);
      desired.put("FPSLevel",8);
      int changed=0;
      for(java.util.Map.Entry<String,Integer> setting:desired.entrySet()){
        int location=activePropertyOffset(target,setting.getKey());
        if(location<0){message("الملف لا يحتوي على حقل "+setting.getKey()+" بصيغة موثقة؛ لم أكتب شيئًا.");return;}
        int old=savInt(target,location);
        if(old<0||old>12){message("قيمة "+setting.getKey()+" غير متوقعة: "+old+"؛ ألغيت التعديل.");return;}
        if(old!=setting.getValue()){putSavInt(target,location,setting.getValue());changed++;}
      }
      if(changed==0){message("القيم الثلاث موجودة أصلًا في Active.sav؛ لا يعني هذا أن اللعبة تعرض HDR/UltraHD أو 120FPS.");return;}
      stage="تأكيد نسخة احتياطية";
      String state=root("if [ ! -e '"+backup+"' ]; then echo MISSING; elif [ -s '"+backup+"' ]; then echo READY; else echo EMPTY; fi");
      if("EMPTY".equals(state)){message("النسخة الاحتياطية فارغة، لم يحدث تعديل.");return;}
      if("MISSING".equals(state))root("cp '"+path+"' '"+backup+"'");
      if(!"READY".equals(root("if [ -s '"+backup+"' ]; then echo READY; else echo FAILED; fi"))){
        message("لم أستطع حفظ نسخة Active.sav، لم يحدث تعديل.");return;
      }
      stage="كتابة ملف GVAS المعدل";
      String payload=android.util.Base64.encodeToString(target,android.util.Base64.NO_WRAP);
      String temp="/data/local/tmp/yrc_active_graphics_new.sav";
      root("printf '%s' '"+payload+"' | base64 -d > '"+temp+"'");
      String sha=root("sha256sum '"+temp+"'").trim().split(" ")[0];
      if(!sha.matches("[0-9a-fA-F]{64}"))throw new IOException("تعذر تأكيد بصمة الملف المؤقت");
      if(gameFileWritesBlocked("applyActiveSavPreset أثناء تجهيز الكتابة"))return;
      root("cat '"+temp+"' > '"+path+"'");
      String applied=root("sha256sum '"+path+"'").trim().split(" ")[0];
      if(!sha.equalsIgnoreCase(applied)){
        root("cat '"+backup+"' > '"+path+"'");
        message("فشل تأكيد تعديل Active.sav، تمت محاولة استعادة النسخة الاحتياطية.");return;
      }
      root("rm -f '"+temp+"'");
      prefs.edit().putInt("active_graphics_quality",graphics)
        .putString("active_graphics_sha",applied).apply();
      runOnUiThread(()->new AlertDialog.Builder(this)
        .setTitle("تم حفظ GFX")
        .setMessage("نجحت كتابة ملف Active.sav. هذا لا يضمن أن PUBG ستطبق جودة HDR/UltraHD أو 120FPS. يمكنك فتح اللعبة واختبار FPS والوضوح، ثم فحص الملف بعد اللعب.")
        .setNegativeButton("إغلاق",null)
        .setPositiveButton("فتح PUBG",(dialog,which)->launchGame()).show());
      message("تم تعديل "+changed+" حقول داخل Active.sav والتحقق من تطابق SHA256.\n"+
        "الطلب: "+(graphics==5?"HDR":"UltraHD")+" و120FPS.\n"+
        "هذا إثبات كتابة لا إثبات تشغيل الإعدادات؛ قد تتجاهلها اللعبة أو تعيد ضبطها. نسخة الاستعادة: "+backup);
    }catch(Exception e){message("تعذر تطبيق Active.sav عند «"+stage+"»: "+e.getMessage());}
  }

  private void verifyActiveSavAfterGame(){
    try{
      root("id");
      byte[] saved=activeSavBytes();
      String[] keys={"FPSLevel","BattleFPS","BattleRenderQuality"};
      java.util.HashMap<String,Integer> readings=new java.util.HashMap<>();
      for(String key:keys){
        int pos=activePropertyOffset(saved,key);
        if(pos<0){message("لم أجد "+key+" في ملف Active.sav الحالي؛ قد تختلف صيغة الإصدار.");return;}
        readings.put(key,savInt(saved,pos));
      }
      String sha=root("sha256sum '"+activeSavPath()+"'").trim().split(" ")[0];
      String expected=prefs.getString("active_graphics_sha","");
      int requested=prefs.getInt("active_graphics_quality",-1);
      int current=readings.get("BattleRenderQuality");
      boolean retained=requested>=0&&current==requested&&readings.get("BattleFPS")==8&&readings.get("FPSLevel")==8;
      String result;
      if(requested<0)result="لم يتم تسجيل تطبيق وضع HDR/UltraHD بعد في هذه النسخة من YRC.";
      else if(retained)result="القيم الثلاث بقيت محفوظة بعد اللعب، لكن هذا لا يثبت أن محرك PUBG فعّلها.";
      else result="إعدادات Active.sav تغيرت أو عادت لقيم أخرى بعد اللعب؛ التعديل غير ثابت في هذا الإصدار.";
      String fingerprint=expected.isEmpty()?"لا توجد بصمة سابقة":
        expected.equalsIgnoreCase(sha)?"مطابق تمامًا لملف التعديل":"تغير محتوى ملف Active.sav";
      message("نتيجة فحص GFX بعد اللعب (قراءة فقط)\n"+
        "BattleFPS="+readings.get("BattleFPS")+
        " / FPSLevel="+readings.get("FPSLevel")+
        " / BattleRenderQuality="+current+"\n"+
        "الجودة المطلوبة: "+(requested==6?"UltraHD":requested==5?"HDR":"غير محددة")+"\n"+
        "بصمة الملف: "+fingerprint+"\n\n"+result);
    }catch(Exception e){message("تعذر فحص Active.sav بعد اللعب: "+e.getMessage());}
  }

  private java.util.LinkedHashMap<String,String> maxGraphicsValues(){
    java.util.LinkedHashMap<String,String> d=new java.util.LinkedHashMap<>();
    d.put("r.MobileContentScaleFactor","1.5");
    d.put("r.DetailMode","2");
    d.put("r.MaterialQualityLevel","1");
    d.put("r.Streaming.PoolSize","512");
    d.put("r.StaticMeshLODDistanceScale","0.6");
    d.put("foliage.LODDistanceScale","1.5");
    d.put("r.ParticleLODBias","0");
    d.put("fx.NiagaraLODBias","0");
    d.put("r.IndividualParticleLODBias","0");
    d.put("r.EmitterSpawnRateScale","1");
    d.put("r.MaxAnisotropy","16");
    d.put("r.UserShadowSwitch","1");
    d.put("r.ShadowQuality","2");
    d.put("r.Shadow.MaxCSMResolution","2048");
    d.put("r.Shadow.CSM.MaxMobileCascades","2");
    d.put("r.Shadow.DistanceScale","1");
    d.put("r.Mobile.DynamicObjectShadow","1");
    d.put("r.MobileNumDynamicPointLights","1");
    return d;
  }

  private java.util.HashMap<String,String> maxDefaultGraphicsValues(){
    java.util.HashMap<String,String> d=new java.util.HashMap<>();
    d.put("r.MobileContentScaleFactor","1,1.0,1.1,1.15,1.25,1.5");
    d.put("r.DetailMode","0,2");
    d.put("r.MaterialQualityLevel","0,1");
    d.put("r.Streaming.PoolSize","200,300,512");
    d.put("r.StaticMeshLODDistanceScale","1.3,0.8,0.6");
    d.put("foliage.LODDistanceScale","0.6,1,1.5");
    d.put("r.ParticleLODBias","2,0");
    d.put("fx.NiagaraLODBias","2,0");
    d.put("r.IndividualParticleLODBias","2,0");
    d.put("r.EmitterSpawnRateScale","0.5,1,1.0");
    d.put("r.MaxAnisotropy","4,8,16");
    d.put("r.UserShadowSwitch","0,1");
    d.put("r.ShadowQuality","0,1,2");
    d.put("r.Shadow.MaxCSMResolution","4,1024,2048");
    d.put("r.Shadow.CSM.MaxMobileCascades","0,2");
    d.put("r.Shadow.DistanceScale","0,0.5,1");
    d.put("r.Mobile.DynamicObjectShadow","0,1");
    d.put("r.MobileNumDynamicPointLights","0,1");
    return d;
  }

  private void confirmMaxGraphics(){
    new AlertDialog.Builder(this).setTitle("MAX Graphics — تجربة قصوى")
      .setMessage("ستُكتب 18 قيمة للرسومات داخل UserCustom.ini: رندر 1.50، الظلال، تفاصيل الأشجار والمجسمات، الخامات والمؤثرات. قد يهبط FPS من 120 إلى أقل بكثير أو تتجاهل PUBG التغييرات أو تعيدها. تعديل ملفات اللعبة غير رسمي وقد يعرض الحساب للتقييد. سيتم حفظ نسخة احتياطية ولن يُغيّر قسم BackUp أو نظام Android. هل تريد التجربة بعد إغلاق PUBG؟")
      .setNegativeButton("إلغاء",null)
      .setPositiveButton("تطبيق MAX",(d,w)->run(()->applyMaxGraphics())).show();
  }

  private void applyMaxGraphics(){
    if(gameFileWritesBlocked("applyMaxGraphics"))return;
    String stage="البدء";
    try{
      stage="اختبار الروت";root("id");
      stage="التأكد من إغلاق PUBG";
      if(!root("pidof com.tencent.ig 2>/dev/null || true").trim().isEmpty()){
        message("أغلق PUBG بالكامل ثم جرّب MAX.");return;
      }
      String path=pubgIniPath();
      String safety="/sdcard/Download/YRC-PUBG-before-MAX.ini";
      stage="قراءة الملف الأصلي";
      String encodedSource=root("base64 '"+path+"'");
      String original=new String(android.util.Base64.decode(encodedSource,android.util.Base64.DEFAULT),java.nio.charset.StandardCharsets.UTF_8);
      if(original.length()<100||original.length()>60000)throw new IOException("حجم الملف غير متوقع، لا تعديل");
      String[] lines=original.split("\n",-1);
      java.util.LinkedHashMap<String,String> desired=maxGraphicsValues();
      java.util.HashMap<String,String> allowed=maxDefaultGraphicsValues();
      java.util.HashMap<String,Integer> counts=new java.util.HashMap<>();
      java.util.HashMap<String,String> current=new java.util.HashMap<>();
      String section="";
      for(String row:lines){
        String line=row.trim();
        if(line.startsWith("[")&&line.endsWith("]")){section=line;continue;}
        if(!"[UserCustom DeviceProfile]".equals(section)||!line.startsWith("+CVars="))continue;
        String hex=line.substring(7);
        if((hex.length()%2)!=0||!hex.matches("[0-9A-Fa-f]+"))continue;
        String decoded=iniDecode(hex);
        int eq=decoded.indexOf('=');
        if(eq<0)continue;
        String key=decoded.substring(0,eq);
        if(!desired.containsKey(key))continue;
        String value=decoded.substring(eq+1);
        counts.put(key,counts.containsKey(key)?counts.get(key)+1:1);
        current.put(key,value);
      }
      for(String key:desired.keySet()){
        if(!Integer.valueOf(1).equals(counts.get(key))){
          message("تعذر تطبيق MAX: يلزم وجود قيمة واحدة لـ "+key+" في UserCustom، ولم نعدل الملف.");return;
        }
        String currentValue=current.get(key);
        java.util.List<String> known=java.util.Arrays.asList(allowed.get(key).split(","));
        if(!known.contains(currentValue)){
          message("MAX لم يغيّر الملف لأن قيمة "+key+" الحالية = "+currentValue+" خارج القيم التي تم اختبارها. أرسل نتيجة فحص الملف؛ لن أفرض قيمة مجهولة.");return;
        }
      }
      int changed=0;
      section="";
      for(int i=0;i<lines.length;i++){
        String line=lines[i].trim();
        if(line.startsWith("[")&&line.endsWith("]")){section=line;continue;}
        if(!"[UserCustom DeviceProfile]".equals(section)||!line.startsWith("+CVars="))continue;
        String hex=line.substring(7);
        if((hex.length()%2)!=0||!hex.matches("[0-9A-Fa-f]+"))continue;
        String decoded=iniDecode(hex);
        int eq=decoded.indexOf('=');
        if(eq<0)continue;
        String key=decoded.substring(0,eq),value=decoded.substring(eq+1);
        if(!desired.containsKey(key)||desired.get(key).equals(value))continue;
        lines[i]="+CVars="+iniEncode(key+"="+desired.get(key))+(lines[i].endsWith("\r")?"\r":"");
        changed++;
      }
      if(changed==0){
        message("قيم MAX كلها موجودة بالفعل في الملف. افحص بقاء القيم بعد اللعب؛ لا نضمن أن محرك PUBG يستخدمها.");
        return;
      }
      String edited=android.text.TextUtils.join("\n",java.util.Arrays.asList(lines));
      String payload=android.util.Base64.encodeToString(edited.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
      stage="حفظ نسخة MAX الاحتياطية";
      String state=root("if [ ! -e '"+safety+"' ]; then echo NEW; elif [ -s '"+safety+"' ]; then echo SAFE; else echo EMPTY; fi");
      if("EMPTY".equals(state)){message("ملف نسخة MAX الاحتياطية فارغ؛ التعديل أُلغي.");return;}
      if("NEW".equals(state))root("cp '"+path+"' '"+safety+"'");
      if(!"SAFE".equals(root("if [ -s '"+safety+"' ]; then echo SAFE; else echo FAILED; fi"))){
        message("لم تُحفظ نسخة احتياطية سليمة؛ لن يُعدّل الملف.");return;
      }
      String tmp="/data/local/tmp/yrc_pubg_max_temp.ini";
      stage="تجهيز الملف المعدل";
      root("printf '%s' '"+payload+"' | base64 -d > '"+tmp+"'");
      String expected=root("sha256sum '"+tmp+"'").trim().split(" ")[0];
      if(!expected.matches("[0-9a-fA-F]{64}"))throw new IOException("فشل التحقق من الملف المؤقت");
      stage="كتابة ملف الرسومات";
      if(gameFileWritesBlocked("applyMaxGraphics أثناء تجهيز الكتابة"))return;
      root("cat '"+tmp+"' > '"+path+"'");
      stage="فحص تطابق الملف";
      String actual=root("sha256sum '"+path+"'").trim().split(" ")[0];
      if(!expected.equalsIgnoreCase(actual)){
        // Restore the last pre-MAX snapshot if the write was incomplete.
        root("cat '"+safety+"' > '"+path+"'");
        message("فشل التحقق من MAX؛ تمت محاولة استعادة الملف المحفوظ قبل MAX.");return;
      }
      root("rm -f '"+tmp+"'");
      prefs.edit().putString("last_graphics_profile_sha",actual)
        .putString("last_graphics_preset","MAX")
        .putString("last_graphics_profile_scale","1.5").apply();
      message("نجحت كتابة "+changed+" إعدادات من أصل "+desired.size()+" ضمن MAX والتحقق من بصمة الملف. نسخة الأمان: "+safety+
        "\nهذا نجاح في الكتابة فقط، وليس إثباتًا أن PUBG فعّلت التأثيرات. افحص FPS ودرجة الحرارة والوضوح، ثم شغّل فحص القيم بعد اللعب.");
    }catch(Exception e){message("فشل MAX عند «"+stage+"»: "+e.getMessage());}
  }

  private java.util.LinkedHashMap<String,String> combinedGraphicsValues(){
    java.util.LinkedHashMap<String,String> desired=new java.util.LinkedHashMap<>();
    desired.put("r.MobileContentScaleFactor","1.15");
    desired.put("r.DetailMode","2");
    desired.put("r.MaterialQualityLevel","1");
    desired.put("r.Streaming.PoolSize","300");
    desired.put("r.StaticMeshLODDistanceScale","0.8");
    desired.put("r.ParticleLODBias","0");
    desired.put("fx.NiagaraLODBias","0");
    return desired;
  }

  private void verifyIniQualityAfterGame(){
    try{
      root("id");
      String file=pubgIniPath();
      String encoded=root("base64 '"+file+"'");
      byte[] bytes=android.util.Base64.decode(encoded,android.util.Base64.DEFAULT);
      String raw=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);
      if(raw.length()<100||raw.length()>60000)throw new IOException("ملف الرسومات غير مكتمل");
      String section="",userScale="غير موجود",backupScale="غير موجود";
      String quality="غير معروف",detail="غير معروف",shadows="غير معروف";
      String material="غير معروف",streaming="غير معروف",meshLOD="غير معروف",particles="غير معروف";
      int userMatches=0, backupMatches=0;
      java.util.HashMap<String,String> activeGraphics=new java.util.HashMap<>();
      java.util.HashMap<String,Integer> seenGraphics=new java.util.HashMap<>();
      String activePreset=prefs.getString("last_graphics_preset","COMBINED");
      java.util.LinkedHashMap<String,String> expectedGraphics="MAX".equals(activePreset)?maxGraphicsValues():combinedGraphicsValues();
      for(String record:raw.split("\\r?\\n")){
        String line=record.trim();
        if(line.startsWith("[")&&line.endsWith("]")){section=line;continue;}
        if(!line.startsWith("+CVars="))continue;
        String hex=line.substring(7);
        if((hex.length()&1)!=0||!hex.matches("[A-Fa-f0-9]+"))continue;
        String decoded=iniDecode(hex);
        if(decoded.startsWith("r.MobileContentScaleFactor=")){
          String value=decoded.substring("r.MobileContentScaleFactor=".length());
          if("[UserCustom DeviceProfile]".equals(section)){userScale=value;userMatches++;}
          if("[BackUp DeviceProfile]".equals(section)){backupScale=value;backupMatches++;}
        }
        if("[UserCustom DeviceProfile]".equals(section)){
          int eq=decoded.indexOf('=');
          if(eq>0){
            String key=decoded.substring(0,eq),val=decoded.substring(eq+1);
            if(expectedGraphics.containsKey(key)){
              activeGraphics.put(key,val);
              seenGraphics.put(key,seenGraphics.containsKey(key)?seenGraphics.get(key)+1:1);
            }
          }
          if(decoded.startsWith("r.UserQualitySetting="))quality=decoded.substring("r.UserQualitySetting=".length());
          if(decoded.startsWith("r.DetailMode="))detail=decoded.substring("r.DetailMode=".length());
          if(decoded.startsWith("r.UserShadowSwitch="))shadows=decoded.substring("r.UserShadowSwitch=".length());
          if(decoded.startsWith("r.MaterialQualityLevel="))material=decoded.substring("r.MaterialQualityLevel=".length());
          if(decoded.startsWith("r.Streaming.PoolSize="))streaming=decoded.substring("r.Streaming.PoolSize=".length());
          if(decoded.startsWith("r.StaticMeshLODDistanceScale="))meshLOD=decoded.substring("r.StaticMeshLODDistanceScale=".length());
          if(decoded.startsWith("r.ParticleLODBias="))particles=decoded.substring("r.ParticleLODBias=".length());
        }
      }
      int keptGraphics=0;
      java.util.ArrayList<String> reverted=new java.util.ArrayList<>();
      for(String key:expectedGraphics.keySet()){
        if(expectedGraphics.get(key).equals(activeGraphics.get(key))&&Integer.valueOf(1).equals(seenGraphics.get(key)))keptGraphics++;
        else reverted.add(key+" = "+(activeGraphics.containsKey(key)?activeGraphics.get(key):"غير موجود"));
      }
      String previousHash=prefs.getString("last_graphics_profile_sha","");
      String actualHash=root("sha256sum '"+file+"'").trim().split(" ")[0];
      String hashReport=previousHash.isEmpty()?"لا توجد بصمة محفوظة لهذا الوضع":
        previousHash.equalsIgnoreCase(actualHash)?"مطابق لملف تطبيق الجودة":"الملف تغيّر بعد تطبيق الجودة";
      String originalBackup=root("if [ -s '"+iniBackupPath()+"' ]; then echo YES; else echo NO; fi");
      String assessment;
      if(userMatches!=1)assessment="لم أجد قيمة رندر واحدة مؤكدة في القسم الحالي.";
      else if(keptGraphics==expectedGraphics.size())
        assessment="القيم السبع لا تزال مكتوبة بعد اللعب. هذا لا يثبت أن PUBG استخدمتها في الرسومات الفعلية.";
      else
        assessment="قيم لم تستمر بعد اللعب: "+android.text.TextUtils.join("، ",reverted)+
          "\nقد تكون PUBG أعادتها لإعداداتها؛ لا يمكن تثبيت التحسين قسرًا بأمان داخل ملف اللعبة.";
      message("فحص PUBG بعد اللعب (قراءة فقط)\n"+
        "UserCustom Render Scale: "+userScale+" ("+userMatches+" قيمة)\n"+
        "BackUp Render Scale: "+backupScale+" ("+backupMatches+" قيمة)\n"+
        "Quality="+quality+" / Detail="+detail+" / Shadows="+shadows+"\n"+
        "Material="+material+" / Texture Pool="+streaming+"\n"+
        "Mesh LOD="+meshLOD+" / Particles="+particles+"\n"+
        "الوضع المفحوص: "+activePreset+"\n"+
        "بقاء إعدادات ملف الجودة: "+keptGraphics+"/"+expectedGraphics.size()+"\n"+
        "بصمة الملف: "+hashReport+"\n"+
        "نسخة استعادة YRC: "+("YES".equals(originalBackup)?"موجودة":"غير موجودة")+
        "\n\n"+assessment+"\nلن أرفع الجودة أكثر قبل التأكد من قبول اللعبة للتعديل.");
    }catch(Exception e){message("تعذر فحص رسومات PUBG بعد اللعب: "+e.getMessage());}
  }

  private String pubgIniPath(){
    return "/sdcard/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini";
  }

  private void showPcViewGuide(){
    new AlertDialog.Builder(this).setTitle("منظور TPP الواسع")
      .setMessage("لمنظور قريب من الكمبيوتر دون تصغير الشاشة:\n\n١. افتح إعدادات ببجي.\n٢. ابحث عن «منظور كاميرا الشخص الثالث» أو TPP Camera View (قد يختلف مكانه حسب الإصدار).\n٣. ارفع زاوية الرؤية إلى أعلى قيمة تسمح بها اللعبة.\n٤. للأداء اختر الرسومات السلسة وأعلى معدل إطارات رسمي متاح (120 عندك).\n\nلا يستطيع التطبيق تجاوز الحد الذي تفرضه ببجي على الكاميرا أو الإطارات.")
      .setNegativeButton("إغلاق",null)
      .setPositiveButton("فتح ببجي",(d,w)->launchGame()).show();
  }

  private void launchGame(){
    if(prefs.getBoolean("account_safety_mode",true)){
      run(()->preflightGameSafety(true));
      return;
    }
    new AlertDialog.Builder(this)
      .setTitle("وضع الحماية معطّل")
      .setMessage("أنت تستخدم تشغيلًا دون فحص سلامة ملفات اللعبة. قد تكون تعديلات MAX أو GFX السابقة موجودة وقابلة لاكتشاف PUBG. لا توجد حماية Anti-Ban تمنع العقوبات. هل تريد الفحص أولًا؟")
      .setNegativeButton("إلغاء",null)
      .setNeutralButton("تشغيل رغم المخاطر",(d,w)->launchPubgIntent())
      .setPositiveButton("فحص قبل التشغيل",(d,w)->run(()->preflightGameSafety(true)))
      .show();
  }
  private void launchPubgIntent(){
    String game=pkg();
    if(game==null){message("لم يتم العثور على PUBG Mobile");return;}
    Intent i=getPackageManager().getLaunchIntentForPackage(game);
    if(i!=null){
      try{startActivity(i);}
      catch(Exception e){message("تعذر تشغيل PUBG: "+e.getMessage());}
    }else message("لم أجد طريقة تشغيل PUBG المثبتة.");
  }
  private void showOfficialRepairGuide(){
    new AlertDialog.Builder(this).setTitle("إعادة اللعبة للحالة الرسمية")
      .setMessage("لخفض خطر تقييد الحساب بعد تعديل ملفات PUBG:\\n\\n"+
        "1. أغلق PUBG ولا تحاول تمويه التعديلات أو إخفاء الروت عن مكافحة الغش.\\n"+
        "2. استخدم زر YRC للعودة إلى النسخ المحفوظة إذا كانت موثوقة.\\n"+
        "3. للحصول على ملفات PUBG الأصلية من مصدرها، استخدم Repair الرسمي من شاشة بدء اللعبة إن توفر، أو أعد تنزيل اللعبة من المتجر الرسمي بعد التأكد من ربط حسابك.\\n"+
        "4. شغّل فحص السلامة مجددًا قبل اللعب.\\n\\n"+
        "أي نسخة احتياطية أنشأها YRC قد تحتوي على تعديلات؛ تطابق SHA256 لا يثبت اعتماد PUBG للملف.")
      .setNegativeButton("إغلاق",null)
      .setPositiveButton("فحص الملفات",(d,w)->run(()->preflightGameSafety(false)))
      .show();
  }

  private static final class SafetyScan {
    boolean risky=false;
    boolean incomplete=false;
    final ArrayList<String> lines=new ArrayList<>();
    void info(String text){lines.add("• "+text);}
    void danger(String text){risky=true;lines.add("⚠ "+text);}
    void unknown(String text){incomplete=true;lines.add("؟ "+text);}
    String body(){
      return android.text.TextUtils.join("\n",lines)+
        "\n\nالفحص المحلي يقلّل المخاطر ولا يثبت أن اللعبة أو الحساب لن يتعرضا للحظر.";
    }
  }
  private String existingFileState(String file)throws Exception{
    return root("if [ -s '"+file+"' ]; then echo FOUND; elif [ -e '"+file+"' ]; then echo EMPTY; else echo MISSING; fi").trim();
  }
  private String fileHash(String path)throws Exception{
    String hash=root("sha256sum '"+path+"'").trim();
    if(!hash.matches("(?i)[0-9a-f]{64}.*"))throw new IOException("تعذر التحقق من بصمة "+path);
    return hash.substring(0,64).toLowerCase(java.util.Locale.US);
  }
  private void checkBackupFingerprint(SafetyScan out,String file,String backup,String name)throws Exception{
    if(!"FOUND".equals(existingFileState(backup))){
      out.info(name+": لا توجد نسخة YRC احتياطية يمكن مقارنتها.");
      return;
    }
    String current=fileHash(file),old=fileHash(backup);
    if(current.equals(old)){
      out.info(name+": يطابق نسخة YRC المحفوظة (لا يعني أنها أصلية رسمية).");
    }else{
      out.danger(name+": يختلف عن نسخة YRC المحفوظة. قد يكون تعديلًا أو تحديثًا مشروعًا للعبة.");
    }
  }
  private void inspectIniSafety(SafetyScan out)throws Exception{
    String path=pubgIniPath();
    String state=existingFileState(path);
    if("MISSING".equals(state)){out.info("UserCustom.ini غير موجود. قد تستخدم هذه النسخة إعدادات مختلفة.");return;}
    if(!"FOUND".equals(state)){out.danger("UserCustom.ini موجود لكنه فارغ.");return;}
    String bytes64=rootSav("base64 '"+path+"'");
    byte[] data=android.util.Base64.decode(bytes64,android.util.Base64.DEFAULT);
    if(data.length<100||data.length>64000){
      out.unknown("حجم UserCustom.ini غير متوقع؛ لا أستطيع التحقق منه.");
      return;
    }
    String raw=new String(data,java.nio.charset.StandardCharsets.UTF_8);
    String section="";
    java.util.HashMap<String,String> actual=new java.util.HashMap<>();
    java.util.HashMap<String,Integer> repeats=new java.util.HashMap<>();
    for(String record:raw.split("\\r?\\n")){
      String line=record.trim();
      if(line.startsWith("[")&&line.endsWith("]")){section=line;continue;}
      if(!"[UserCustom DeviceProfile]".equals(section)||!line.startsWith("+CVars="))continue;
      String hex=line.substring(7);
      if(hex.length()%2!=0||!hex.matches("[a-fA-F0-9]+"))continue;
      String decoded=iniDecode(hex);
      int equals=decoded.indexOf('=');
      if(equals<=0)continue;
      String key=decoded.substring(0,equals);
      actual.put(key,decoded.substring(equals+1));
      repeats.put(key,repeats.containsKey(key)?repeats.get(key)+1:1);
    }
    if(actual.isEmpty()){out.unknown("تعذرت قراءة أي قيم UserCustom من الملف.");return;}
    String rendered=actual.get("r.MobileContentScaleFactor");
    if(rendered!=null){
      try{
        double scale=Double.parseDouble(rendered);
        if(scale>1.05)out.danger("Render Scale="+rendered+
          " أعلى من قيمة 1.0 المستخدمة قبل تجارب YRC. قد يكون تعديلًا غير رسمي.");
        else out.info("Render Scale="+rendered+" ضمن نطاق الفحص المتوقع.");
      }catch(NumberFormatException ex){out.unknown("Render Scale غير قابل للتفسير: "+rendered);}
    }else out.unknown("لم أجد قيمة Render Scale للتحقق.");
    int matchingMax=0;
    for(java.util.Map.Entry<String,String> kv:maxGraphicsValues().entrySet()){
      if(kv.getValue().equals(actual.get(kv.getKey()))&&
        Integer.valueOf(1).equals(repeats.get(kv.getKey())))matchingMax++;
    }
    if(matchingMax>=9)out.danger("تطابق "+matchingMax+" من 18 إعدادًا مع MAX Graphics التجريبي.");
    int matchingCombined=0;
    for(java.util.Map.Entry<String,String> kv:combinedGraphicsValues().entrySet()){
      if(kv.getValue().equals(actual.get(kv.getKey()))&&
        Integer.valueOf(1).equals(repeats.get(kv.getKey())))matchingCombined++;
    }
    if(matchingCombined>=5)out.danger("تطابق "+matchingCombined+" من 7 إعدادات مع ملف الرندر/التفاصيل التجريبي القديم.");
    if(!prefs.getString("last_graphics_profile_sha","").isEmpty()&&
      prefs.getString("last_graphics_profile_sha","").equalsIgnoreCase(fileHash(path)))
      out.danger("UserCustom.ini يطابق ملف تعديل حفظه YRC مسبقًا.");
    String backup=iniBackupPath();
    if(!"FOUND".equals(existingFileState(backup))){
      backup="/sdcard/Download/YRC-PUBG-before-MAX.ini";
    }
    checkBackupFingerprint(out,path,backup,"UserCustom.ini");
  }
  private void inspectSavSafety(SafetyScan out)throws Exception{
    String path=activeSavPath();
    String state=existingFileState(path);
    if("MISSING".equals(state)){out.info("Active.sav غير موجود في المسار المعتاد لهذا الإصدار.");return;}
    if(!"FOUND".equals(state)){out.danger("Active.sav موجود لكنه فارغ.");return;}
    byte[] data;
    try{data=activeSavBytes();}
    catch(Exception e){out.unknown("لم أتمكن من قراءة Active.sav كملف GVAS: "+e.getMessage());return;}
    String[] keys={"BattleFPS","FPSLevel","BattleRenderQuality"};
    java.util.HashMap<String,Integer> values=new java.util.HashMap<>();
    for(String key:keys){
      int at=activePropertyOffset(data,key);
      if(at<0){out.unknown("لا أستطيع تأكيد حقل "+key+" في Active.sav.");return;}
      values.put(key,savInt(data,at));
    }
    out.info("Active.sav: BattleFPS="+values.get("BattleFPS")+
      ", FPSLevel="+values.get("FPSLevel")+
      ", Quality="+values.get("BattleRenderQuality"));
    if(values.get("BattleFPS")==8&&values.get("FPSLevel")==8&&
      (values.get("BattleRenderQuality")==5||values.get("BattleRenderQuality")==6)){
      out.danger("Active.sav يحتوي على نمط HDR/UltraHD + 120FPS الذي يستخدمه YRC.");
    }
    if(!prefs.getString("active_graphics_sha","").isEmpty()&&
      prefs.getString("active_graphics_sha","").equalsIgnoreCase(fileHash(path)))
      out.danger("Active.sav يطابق تعديل GFX سبق حفظه بواسطة YRC.");
    checkBackupFingerprint(out,path,activeSavBackup(),"Active.sav");
  }
  private void preflightGameSafety(boolean shouldLaunch){
    SafetyScan report=new SafetyScan();
    try{
      if(pkg()==null){report.unknown("لم يتم العثور على PUBG Mobile المثبّتة.");}
      else{
        String uid=root("id -u").trim();
        if(!"0".equals(uid)){report.unknown("صلاحية root غير مؤكدة؛ لا يمكن فحص ملفات اللعبة.");}
        else{
          inspectIniSafety(report);
          inspectSavSafety(report);
          report.info("تم فحص الملفين في هذه اللحظة فقط؛ قد تتغير الملفات لاحقًا.");
        }
      }
    }catch(Exception e){
      report.unknown("تعذر إكمال فحص الحماية: "+e.getMessage());
    }
    final String detail=report.body();
    if(report.risky||report.incomplete){
      message("تم منع التشغيل عبر YRC حتى تراجع فحص السلامة:\n"+detail);
      if(shouldLaunch){
        runOnUiThread(()->new AlertDialog.Builder(this)
          .setTitle(report.risky?"تغييرات قد تعرّض الحساب للحظر":"فحص السلامة غير مكتمل")
          .setMessage(detail+"\n\nلن يفتح YRC اللعبة تلقائيًا قبل معالجة هذه النتيجة.")
          .setNegativeButton("إغلاق",null)
          .setPositiveButton("إرشادات الإصلاح",(d,w)->showOfficialRepairGuide())
          .show());
      }
      return;
    }
    if(shouldLaunch){
      message("لم أجد مؤشرات YRC المعروفة في الملفين. هذا لا يضمن سلامة الحساب.\n"+detail);
      if(prefs.getBoolean("account_safety_mode",true)){
        runOnUiThread(()->launchPubgIntent());
      }else{
        message("توقف التشغيل: وضع الأمان تغيّر أثناء الفحص. أعد محاولة فتح PUBG.");
      }
    }else{
      message("نتيجة فحص سلامة الملفات:\n"+detail+"\nلا يوجد ضمان لعدم الحظر.");
    }
  }

  @Override protected void onDestroy(){executor.shutdown();super.onDestroy();}

  private void applyStable120(){
    ArrayList<String> result=new ArrayList<>();
    try{
      root("id");
      if(thermal()>=PowerManager.THERMAL_STATUS_SEVERE||temperature()>=420){
        message("الحرارة مرتفعة؛ لا يمكن طلب أداء أعلى بأمان. اترك الجهاز يبرد دون تعطيل الحماية الحرارية.");
        return;
      }
      String xos=root("settings get system tran_refresh_mode").trim();
      String peak=root("settings get system peak_refresh_rate").trim();
      String power=root("settings get global low_power").trim();
      android.content.SharedPreferences.Editor editor=prefs.edit();
      if(!prefs.contains("clean_prev_xos"))editor.putString("clean_prev_xos",xos);
      if(!prefs.contains("clean_prev_peak"))editor.putString("clean_prev_peak",peak);
      if(!prefs.contains("clean_prev_power"))editor.putString("clean_prev_power",power);
      editor.commit();
      root("settings put system tran_refresh_mode 120");
      root("settings put system peak_refresh_rate 120.0");
      root("settings put global low_power 0");
      result.add("تم طلب شاشة 120Hz وتعطيل توفير الطاقة.");
      String game=pkg();
      if(game!=null){
        try{root("cmd game mode 2 "+game);result.add("تم طلب Game Mode Performance.");}
        catch(Exception e){result.add("Game Mode غير متاح: "+e.getMessage());}
      }
      result.add("داخل PUBG اختر أعلى FPS رسمي (120 إن توفر) ورسومات Smooth.");
      result.add("لم يتم تعديل ملفات PUBG أو تعطيل حدود الحرارة.");
      message(android.text.TextUtils.join("\n",result));
    }catch(Exception e){message("فشل إعداد 120FPS: "+e.getMessage());}
  }
  private void restoreStable120(){
    try{
      root("id");
      ArrayList<String> result=new ArrayList<>();
      restoreSystemValue("clean_prev_xos","system","tran_refresh_mode",result);
      restoreSystemValue("clean_prev_peak","system","peak_refresh_rate",result);
      restoreSystemValue("clean_prev_power","global","low_power",result);
      String game=pkg();
      if(game!=null){
        try{root("cmd game mode 1 "+game);result.add("Game Mode: تم طلب الوضع القياسي");}
        catch(Exception e){result.add("تعذر استعادة Game Mode: "+e.getMessage());}
      }
      message(result.isEmpty()?"لم أجد تغييرات خاصة بهذا الوضع تحتاج إلى استعادة.":
        android.text.TextUtils.join("\n",result));
    }catch(Exception e){message("فشل إلغاء وضع FPS: "+e.getMessage());}
  }
  private void restoreSystemValue(String key,String table,String setting,ArrayList<String> results){
    if(!prefs.contains(key))return;
    String old=prefs.getString(key,"null");
    try{
      if(old.matches("null|"))root("settings delete "+table+" "+setting);
      else if(old.matches("[a-zA-Z0-9._-]{1,30}"))root("settings put "+table+" "+setting+" "+old);
      else throw new IOException("قيمة غير متوقعة");
      prefs.edit().remove(key).commit();
      results.add("استُعيد "+setting);
    }catch(Exception e){results.add("تعذر استعادة "+setting+": "+e.getMessage());}
  }

  private void confirmRestoreOriginal(){
    new AlertDialog.Builder(this).setTitle("إلغاء تعديلات YRC")
      .setMessage("أغلق PUBG بالكامل. سنستعيد فقط ملفات UserCustom.ini وActive.sav التي تتوفر لها نسخ YRC الاحتياطية، ونعيد إعدادات الشاشة والأداء. قد تتضمن النسخ الاحتياطية تعديلات قديمة؛ استعادة اللعبة الأصلية بالكامل تتطلب Repair من PUBG إذا لم تتوفر نسخة نظيفة. هل تتابع؟")
      .setNegativeButton("إلغاء",null)
      .setPositiveButton("استعادة ما يمكن",(d,w)->run(()->restoreOriginalConfigs())).show();
  }
  private void restoreOriginalConfigs(){
    ArrayList<String> report=new ArrayList<>();
    prefs.edit().putBoolean("account_safety_mode",true).commit();
    try{
      root("id");
      if(!root("pidof com.tencent.ig 2>/dev/null || true").trim().isEmpty()){
        message("أغلق PUBG بالكامل ثم اضغط زر الرجوع للأصل.");return;
      }
      boolean graphicsRestore=restoreGameBackup(
        pubgIniPath(),new String[]{
          iniBackupPath(),
          "/sdcard/Download/YRC-PUBG-before-MAX.ini"
        },"UserCustom.ini",report);
      boolean activeRestore=restoreGameBackup(
        activeSavPath(),new String[]{activeSavBackup()},"Active.sav",report);
      // Revert screen resizing performed by earlier YRC versions only.
      boolean hadScreen=prefs.contains("pc_old_size")||prefs.contains("gfx_original_wm");
      if(hadScreen){
        try{
          root("wm size reset");
          prefs.edit().remove("pc_old_size").remove("gfx_original_wm").commit();
          report.add("حجم الشاشة: عاد إلى مقاس الجهاز الافتراضي");
        }catch(Exception e){report.add("فشلت استعادة عرض الشاشة: "+e.getMessage());}
      }
      restoreSystemValue("clean_prev_xos","system","tran_refresh_mode",report);
      restoreSystemValue("clean_prev_peak","system","peak_refresh_rate",report);
      restoreSystemValue("clean_prev_power","global","low_power",report);
      restoreSystemValue("gfx_old_xos","system","tran_refresh_mode",report);
      restoreSystemValue("gfx_old_peak","system","peak_refresh_rate",report);
      restoreSystemValue("old_global_low_power","global","low_power",report);
      restoreSystemValue("old_system_peak_refresh_rate","system","peak_refresh_rate",report);
      restoreSystemValue("old_system_min_refresh_rate","system","min_refresh_rate",report);
      try{root("cmd power set-fixed-performance-mode-enabled false");report.add("تم إيقاف وضع الأداء الثابت إن كان مفعّلًا");}
      catch(Exception e){report.add("تعذر إيقاف وضع الأداء الثابت: "+e.getMessage());}
      String game=pkg();
      if(game!=null){
        try{root("cmd game mode 1 "+game);report.add("Game Mode: تم طلب الوضع القياسي");}
        catch(Exception e){report.add("تعذر استعادة Game Mode: "+e.getMessage());}
      }
      prefs.edit().putBoolean("account_safety_mode",true)
        .remove("last_graphics_profile_sha").remove("last_graphics_profile_scale")
        .remove("last_graphics_preset").remove("active_graphics_quality")
        .remove("active_graphics_sha").apply();
      report.add("وضع حماية الحساب: مفعّل");
      if(!graphicsRestore||!activeRestore)report.add("الملفات التي لم نجد لها نسخة YRC سليمة تُركت دون تغيير.");
      report.add("لا نضمن أن النسخ المحفوظة هي ملفات PUBG الأصلية. للتحقق الكامل استخدم إصلاح اللعبة الرسمي Repair.");
      message(android.text.TextUtils.join("\n",report));
    }catch(Exception e){
      report.add("توقفت الاستعادة: "+e.getMessage());
      message(android.text.TextUtils.join("\n",report));
    }
  }
  private boolean restoreGameBackup(String destination,String[] options,String label,ArrayList<String> report){
    try{
      String original="MISSING";
      for(String backup:options){
        String has=root("if [ -s '"+backup+"' ]; then echo FOUND; else echo MISSING; fi");
        if("FOUND".equals(has)){original=backup;break;}
      }
      if("MISSING".equals(original)){report.add(label+": لا توجد نسخة YRC احتياطية سليمة، تركنا الملف كما هو");return false;}
      String exists=root("if [ -f '"+destination+"' ]; then echo FOUND; else echo MISSING; fi");
      if(!"FOUND".equals(exists)){report.add(label+": الملف الحالي غير موجود، لن ننشئه من نسخة قديمة");return false;}
      root("cat '"+original+"' > '"+destination+"'");
      String a=root("sha256sum '"+original+"'").trim().split(" ")[0];
      String b=root("sha256sum '"+destination+"'").trim().split(" ")[0];
      if(!a.matches("[a-fA-F0-9]{64}")||!a.equalsIgnoreCase(b)){
        report.add(label+": فشل التحقق من تطابق النسخة، يلزم فحص الملف");
        return false;
      }
      report.add(label+": تم استرجاع نسخة YRC المحفوظة والتحقق من التطابق");
      return true;
    }catch(Exception e){report.add(label+": تعذرت الاستعادة: "+e.getMessage());return false;}
  }
}
