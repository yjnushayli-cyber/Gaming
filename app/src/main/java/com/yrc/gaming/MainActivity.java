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
  private RadioGroup profiles;
  private CheckBox network, visual, touch, pcView;
  private final int BG=Color.rgb(12,18,29), ACCENT=Color.rgb(56,207,174);
  private final String[] PACKAGES={"com.tencent.ig","com.pubg.imobile","com.vng.pubgmobile","com.rekoo.pubgm"};

  @Override public void onCreate(Bundle b){
    super.onCreate(b);
    getWindow().setStatusBarColor(BG);
    getWindow().setNavigationBarColor(BG);
    prefs=getSharedPreferences("yrc_profiles",MODE_PRIVATE);
    ScrollView sc=new ScrollView(this);
    sc.setFillViewport(true);
    layout=new LinearLayout(this);
    layout.setOrientation(LinearLayout.VERTICAL);
    layout.setPadding(30,28,30,50);
    layout.setBackgroundColor(BG);
    layout.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    sc.addView(layout);
    setContentView(sc);
    title("YRC | محرك الألعاب");
    label("إنفينكس GT30 Pro • ببجي موبايل");
    status=label("اختر ملف الأداء ثم اضغط تطبيق. التغييرات على النظام فقط، وليست على ملفات اللعبة.");
    title("حماية الحساب — وضع آمن (لا يضمن منع الباند)");
    label("يبدأ مفعّلًا تلقائيًا. يمنع YRC من إجراء تعديلات جديدة على UserCustom.ini وActive.sav، دون إخفاء الروت أو تجاوز مكافحة الغش. أي تغييرات قديمة لا تُلغى تلقائيًا.");
    CheckBox accountSafety=new CheckBox(this);
    accountSafety.setText("وضع أمان الحساب: منع تعديل ملفات PUBG");
    accountSafety.setTextSize(16);
    accountSafety.setTextColor(Color.WHITE);
    accountSafety.setChecked(prefs.getBoolean("account_safety_mode",true));
    layout.addView(accountSafety);
    accountSafety.setOnCheckedChangeListener((control,checked)->{
      if(checked){
        prefs.edit().putBoolean("account_safety_mode",true).apply();
        message("وضع أمان الحساب مفعّل. YRC لن يكتب تعديلات جديدة لملفات PUBG، لكن التعديلات السابقة قد تظل موجودة.");
      }else{
        new AlertDialog.Builder(this).setTitle("تعطيل وضع أمان الحساب")
          .setMessage("تعطيل الحماية يسمح لتطبيق YRC بتعديل ملفات PUBG غير الرسمية. هذه التعديلات قد تسبب حظر الحساب، ولا يوجد Anti-Ban مضمون. لن نخفي الروت أو نتجاوز كشف مكافحة الغش. هل تريد المتابعة رغم المخاطر؟")
          .setNegativeButton("إلغاء",(d,w)->accountSafety.setChecked(true))
          .setPositiveButton("أفهم المخاطر",(d,w)->{
            prefs.edit().putBoolean("account_safety_mode",false).apply();
            message("وضع أمان الحساب معطّل بطلبك. تعديل ملفات PUBG قد يسبب حظرًا، حتى إن نجح.");
          })
          .setOnCancelListener(d->accountSafety.setChecked(true)).show();
      }
    });
    button("فحص وضع حماية الحساب",()->showAccountSafetyStatus());
    button("فحص صلاحية الروت في KernelSU Next",()->run(()->diagnoseRootAccess()));
    title("ملفات الأداء");
    profiles=new RadioGroup(this);
    profiles.setOrientation(RadioGroup.VERTICAL);
    profiles.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    addRadio(101,"ثبات الفريمات — أولوية 120FPS");
    addRadio(102,"متوازن — أداء وصورة");
    addRadio(103,"جودة العرض — دون تعديل ملفات اللعبة");
    profiles.check(prefs.getInt("profile",101));
    layout.addView(profiles);
    title("ملفا الرسومات والفريمات — تنفيذ فعلي");
    label("الجودة المتوازنة: دقة الشاشة الأصلية و120Hz مطلوب. الثبات: دقة عرض 90% لتخفيف الحمل، مع إبقاء إعدادات PUBG الرسمية على Smooth و120FPS.");
    button("تفعيل جودة متوازنة + 120FPS",()->run(()->applyGraphicsProfile(false)));
    button("تفعيل أولوية ثبات 120FPS",()->run(()->applyGraphicsProfile(true)));
    button("استعادة دقة العرض الأصلية",()->run(()->restoreGraphicsProfile()));
    title("GFX Tool — فحص وضبط ملف Active.sav");
    label("طريقة ثانية وجدناها في أدوات GFX: إعدادات FPS ورسومات القتال داخل Active.sav (UE4 GVAS)، وليست UserCustom.ini. تفحص بنية الملف أولًا، وتكتب فقط عند التحقق من الحقول الثلاثة.");
    button("فحص ملفات GFX وقراءة Active.sav",()->run(()->inspectActiveSav()));
    button("تجربة HDR + 120FPS داخل Active.sav",()->confirmActiveSavPreset(5));
    button("تجربة UltraHD + 120FPS داخل Active.sav",()->confirmActiveSavPreset(6));
    button("فحص بقاء جودة GFX بعد اللعب (Active.sav)",()->run(()->verifyActiveSavAfterGame()));
    button("إلغاء تعديل Active.sav واستعادة نسخته",()->run(()->restoreActiveSav()));
    title("MAX Graphics — رسومات ثقيلة قريبة من PC (تجريبي)");
    label("أعلى تجربة بصرية داخل الملف: رندر 1.50 + ظلال محسّنة + تفاصيل المباني والأشجار والخامات. هذا ليس 4K ولا رسومات PC الحقيقية، وقد تهبط الفريمات بقوة. أغلق PUBG أولًا.");
    button("تطبيق MAX Graphics (رندر 1.5 + تفاصيل قصوى)",()->confirmMaxGraphics());
    button("إلغاء MAX وإرجاع الملف لما قبل التجربة",()->new AlertDialog.Builder(this)
      .setTitle("إلغاء MAX Graphics")
      .setMessage("سيعيد ملف الرسومات الذي احتفظنا به قبل تجربة MAX. أغلق PUBG تمامًا قبل الاستعادة.")
      .setNegativeButton("إلغاء",null)
      .setPositiveButton("استرجاع",(d,w)->run(()->restoreBeforeMax())).show());
    button("فحص ثبات قيم MAX بعد اللعب",()->run(()->verifyIniQualityAfterGame()));
    title("تعديل الرندر والتفاصيل بضغطة واحدة — PUBG");
    label("ملف موحد: رندر 1.15 + 6 إعدادات تفاصيل وخامات من إعدادات اللعبة الاحتياطية. يُكتب إلى UserCustom فقط، وليس إلى Android. لا يضمن استمرار القيم بعد تشغيل اللعبة.");
    button("تطبيق رندر 1.15 + تفاصيل أعلى (لا تستخدمه مع MAX)",()->confirmCombinedIniGraphics());
    button("فحص بقاء جميع تحسينات الرسومات بعد اللعب",()->run(()->verifyIniQualityAfterGame()));
    title("تحسين الرسومات داخل ملف PUBG — تجريبي");
    label("تعديلات غير رسمية للملف. في اختبارك عادت إعدادات التفاصيل بعد اللعب؛ نجاح كتابة CVars لا يعني اعتمادها في المحرك. قد تتجاهلها PUBG وقد يتأثر الحساب. جرّب الخيار الرسمي بالأعلى أولًا.");
    button("تطبيق تحسين وضوح 10% داخل UserCustom.ini",()->new AlertDialog.Builder(this).setTitle("تعديل ملف رسومات PUBG").setMessage("سيُنشئ التطبيق نسخة احتياطية ثم يرفع r.MobileContentScaleFactor من 1 إلى 1.1 إن كانت القيمة موجودة. هذا تعديل غير رسمي وقد لا تقبله اللعبة أو قد يعرّض الحساب للقيود. أغلق PUBG قبل المتابعة.").setNegativeButton("إلغاء",null).setPositiveButton("تطبيق", (d,w)->run(()->applyIniClarity())).show());
    button("وضوح أعلى 15% — تعديل داخل PUBG",()->confirmIniQuality("1.15"));
    button("وضوح أعلى 25% — تجريبي ثقيل",()->confirmIniQuality("1.25"));
    button("رفع تفاصيل البيئة والخامات من ملف PUBG — تجريبي",()->confirmIniDetails());
    button("افحص هل PUBG احتفظت بتحسين الوضوح بعد اللعب",()->run(()->verifyIniQualityAfterGame()));
    button("استعادة ملف رسومات PUBG من النسخة الاحتياطية",()->run(()->restoreIniClarity()));
    title("رسومات أوضح + منظور TPP واسع");
    label("ملف موحد يحافظ على دقة الشاشة الأصلية و120Hz، ثم يفتح إرشادات رفع TPP من داخل PUBG. لا يغيّر ملفات اللعبة أو يزعم 4K/240FPS.");
    button("تفعيل رسومات أوضح + TPP واسع",()->run(()->applyPcLikeProfile()));
    title("التحسينات الإضافية");
    network=check("الشبكة: إبقاء Wi-Fi نشطًا أثناء اتصال الجهاز به",true);
    visual=check("العرض: إلغاء توفير الطاقة وطلب وضع الأداء",true);
    touch=check("الاستجابة: منع إطفاء الشاشة أثناء فتح التطبيق فقط",false);
    title("منظور الكمبيوتر (TPP)");
    pcView=check("فتح إرشادات المنظور الواسع قبل تشغيل ببجي",true);
    label("وضع PC المصغّر القديم يغيّر مساحة عرض أندرويد إلى 16:9. يمكنك استعادة دقة 1224×2720 الأصلية بضغطة زر. لا يغيّر FOV الحقيقي للعبة.");
    button("تفعيل شاشة PC المصغّرة 16:9",()->new AlertDialog.Builder(this).setTitle("تصغير مساحة العرض إلى 16:9").setMessage("هذا يغيّر دقة عرض نظام الهاتف مؤقتًا وقد يسبب أشرطة سوداء أو تغيير أماكن اللمس. لا يغيّر كاميرا ببجي الحقيقية. هل تريد التطبيق؟").setNegativeButton("إلغاء",null).setPositiveButton("تفعيل",(d,w)->run(()->applyPcScreen())).show());
    button("استعادة الشاشة الأصلية",()->run(()->restorePcScreen()));
    button("إرشادات زاوية كاميرا TPP",()->showPcViewGuide());
    title("ضبط GT30 Pro بناءً على فحص جهازك");
    label("XOS: tran_refresh_mode=120، الشاشة تدعم 120/144Hz، وPUBG تعرض 120FPS كحد أعلى. يطبّق وضعًا قابلًا للاستعادة ويتحقق من النتائج.");
    button("تطبيق إعداد 120FPS المستقر (روت)",()->run(()->applyGt30Profile()));
    button("فحص وضع الشاشة وقيود XOS",()->run(()->diagnoseGt30()));
    title("خيارات متقدمة قابلة للتطبيق");
    button("طلب شاشة 144Hz والتحقق من استجابة XOS",()->run(()->request144()));
    button("وضع أداء قوي قابل للاستعادة (روت)",()->new AlertDialog.Builder(this).setTitle("تطبيق أداء قوي").setMessage("سيطلب 144Hz ويعطل توفير الطاقة ويطلب Game Mode performance و120FPS. لا يغيّر حرارة الجهاز أو ملفات اللعبة، وقد يتجاهل XOS بعض الطلبات.").setNegativeButton("إلغاء",null).setPositiveButton("تطبيق",(d,w)->run(()->strongMode())).show());
    button("تخفيف الحمل الرسومي لببجي 90% (تجريبي)",()->new AlertDialog.Builder(this).setTitle("تقليل حمل GPU").setMessage("يطلب من Android Game Mode عرض اللعبة بدقة رسم 90% إن كان مدعومًا، لتحسين ثبات الفريمات مقابل انخفاض طفيف في وضوح الصورة. لا يغير دقة الهاتف أو ملفات اللعبة. قد ترفضه PUBG أو XOS.").setNegativeButton("إلغاء",null).setPositiveButton("تطبيق",(d,w)->run(()->applyDownscale())).show());
    button("إلغاء تخفيف الحمل الرسومي",()->run(()->resetDownscale()));
    button("طلب 120FPS من Game Mode (تجريبي)",()->run(()->requestGameFps(120)));
    button("إلغاء طلب FPS المخصص",()->run(()->requestGameFps(0)));
    title("Wi-Fi | ثبات البنق");
    label("اختبار مباشر للاتصال بين هاتفك والراوتر. يكشف فقد الحزم وتذبذب الاستجابة داخل شبكة المنزل؛ لا يقيس بنق سيرفر ببجي.");
    button("اختبار Wi-Fi والراوتر (10 حزم)",()->run(()->testWifi()));
    button("فتح إعدادات Wi-Fi",()->{try{startActivity(new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS));}catch(Exception e){message("تعذر فتح Wi-Fi");}});
    button("إيقاف وضع الأداء القوي واستعادة الإعدادات",()->run(()->stopStrongMode()));
    title("تحليل رسومات PUBG 4.6.0");
    label("قراءة فقط لملف UserCustom.ini وفك CVars دون تعديل ملفات اللعبة.");
    button("تحليل إعدادات PUBG 4.6.0",()->run(()->analyzePubgConfig()));
    button("ربط مسار رسومات وفريمات PUBG والتحقق منه",()->run(()->verifyGraphicsPath()));
    button("نسخ مسار ملف الرسومات",()->copyGraphicsPath());
    button("نسخ ملف PUBG الأصلي احتياطيًا (روت)",()->run(()->backupPubgConfig()));
    button("تجهيز معاينة رسومات PUBG التجريبية",()->run(()->previewPubgGraphics()));
    title("التحكم");
    button("تطبيق التحسينات وتشغيل ببجي",()->{final int selected=profiles.getCheckedRadioButtonId();final boolean net=network.isChecked(),vis=visual.isChecked(),input=touch.isChecked(),guide=pcView.isChecked();run(()->applyAndLaunch(selected,net,vis,input,guide));});
    button("استعادة إعدادات النظام",()->run(()->restore()));
    button("فتح إعدادات الرسومات في ببجي",()->launchGame());
    button("فتح إعدادات الشاشة",()->{try{startActivity(new Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS));}catch(Exception e){message("تعذر فتح الإعدادات");}});
    title("حدود التحسين");
    label("المعدل 144Hz هو معدل تحديث الشاشة، وليس 144FPS داخل ببجي. اللعبة تعرض حاليًا 120FPS كحد أعلى. لا نغيّر دقة الشاشة أو ملفات اللعبة، ولا نعطّل الحماية الحرارية. الشبكة والرسومات تُحسّن ضمن إمكانيات أندرويد والجهاز.");
  }

  private void addRadio(int id,String text){
    RadioButton b=new RadioButton(this);
    b.setId(id);b.setText(text);b.setTextColor(Color.WHITE);b.setTextSize(16);b.setPadding(0,10,0,10);
    profiles.addView(b);
  }
  private CheckBox check(String text,boolean enabled){
    CheckBox c=new CheckBox(this);c.setText(text);c.setTextColor(Color.WHITE);c.setTextSize(15);
    c.setChecked(prefs.getBoolean("check_"+text,enabled));layout.addView(c);return c;
  }
  private void title(String s){TextView t=label(s);t.setTextSize(23);t.setTextColor(ACCENT);t.setPadding(0,24,0,10);}
  private TextView label(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(15);t.setTextColor(Color.WHITE);t.setPadding(0,10,0,10);layout.addView(t);return t;}
  private void button(String name,Runnable action){Button b=new Button(this);b.setText(name);layout.addView(b);b.setOnClickListener(v->action.run());}
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
  private void saveSetting(String table,String key)throws Exception{
    String pref="old_"+table+"_"+key;
    if(!prefs.contains(pref))prefs.edit().putString(pref,root("settings get "+table+" "+key)).commit();
  }
  private void setSetting(String table,String key,String value)throws Exception{
    saveSetting(table,key);
    root("settings put "+table+" "+key+" "+value);
    String actual=root("settings get "+table+" "+key);
    if(!value.equals(actual))throw new IOException("النظام لم يقبل "+key+" (القيمة: "+actual+")");
  }
  private void applyAndLaunch(int profile,boolean net,boolean vis,boolean input,boolean guide){
    String game=pkg();
    if(game==null){message("ببجي غير موجودة أو غير ظاهرة للتطبيق.");return;}
    int t=temperature(),th=thermal();
    if((t>=420)||(th>=PowerManager.THERMAL_STATUS_SEVERE)){
      message("الحرارة مرتفعة؛ لم يُفعّل وضع الأداء. حرارة البطارية: "+(t/10f)+"°C");return;
    }
    prefs.edit().putInt("profile",profile)
      .putBoolean("check_"+network.getText(),net)
      .putBoolean("check_"+visual.getText(),vis)
      .putBoolean("check_"+touch.getText(),input).commit();
    ArrayList<String> result=new ArrayList<>();
    try{root("id");result.add("الروت: جاهز");}
    catch(Exception e){message("لم تتوفر صلاحية الروت: "+e.getMessage());return;}
    if(vis){
      try{setSetting("global","low_power","0");result.add("توفير الطاقة: معطّل");}
      catch(Exception e){result.add("توفير الطاقة: "+e.getMessage());}
    }
    try{
      String mode=(profile==103?"standard":"performance");
      root("cmd game mode "+(profile==103?"1":"2")+" "+game);
      result.add("وضع اللعبة: "+(profile==103?"قياسي":"أداء")+" (طلب للنظام)");
    }catch(Exception e){result.add("وضع اللعبة غير مدعوم: "+e.getMessage());}
    if(profile!=103){
      try{root("cmd power set-fixed-performance-mode-enabled true");prefs.edit().putBoolean("fixed",true).commit();result.add("الأداء الثابت: تم طلبه");}
      catch(Exception e){result.add("الأداء الثابت غير مدعوم");}
    }else{
      try{root("cmd power set-fixed-performance-mode-enabled false");prefs.edit().putBoolean("fixed",false).commit();}catch(Exception ignored){}
    }
    if(net){
      try{
        android.net.ConnectivityManager cm=(android.net.ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        android.net.Network active=cm.getActiveNetwork();
        android.net.NetworkCapabilities caps=cm.getNetworkCapabilities(active);
        if(caps!=null&&caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_WIFI)){
          result.add("الشبكة: Wi-Fi متصل. لا يمكن فرض بنق ثابت أو منع تبديل الشبكة بواجهة عامة موثوقة.");
        }else result.add("الشبكة: الاتصال الحالي ليس Wi-Fi؛ لم نغيّر الشبكة");
      }catch(Exception e){result.add("إعداد Wi-Fi غير متاح: "+e.getMessage());}
    }
    if(input){runOnUiThread(()->getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));result.add("إبقاء الشاشة نشطة أثناء فتح YRC");}
    else runOnUiThread(()->getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    result.add("الرسومات: دون تعديل ملفات اللعبة. شاشة 16:9 تُفعّل من زر مستقل.");
    message(android.text.TextUtils.join("\n",result));
    runOnUiThread(()->launchGame());
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
  private void applyPcLikeProfile(){
    ArrayList<String> logs=new ArrayList<>();
    try{
      root("id");
      if(thermal()>=PowerManager.THERMAL_STATUS_SEVERE||temperature()>=420){
        message("الحرارة مرتفعة؛ لم نطبق وضع الجودة.");return;
      }
      if(!prefs.contains("gfx_original_wm"))prefs.edit().putString("gfx_original_wm",root("wm size")).commit();
      if(!prefs.contains("gfx_old_xos"))prefs.edit().putString("gfx_old_xos",root("settings get system tran_refresh_mode")).commit();
      if(!prefs.contains("gfx_old_peak"))prefs.edit().putString("gfx_old_peak",root("settings get system peak_refresh_rate")).commit();
      root("wm size reset");
      logs.add("دقة الشاشة: الأصلية 1224×2720 (إلغاء تخفيض الدقة)");
      root("settings put system tran_refresh_mode 120");
      root("settings put system peak_refresh_rate 120.0");
      logs.add("XOS: طلب 120Hz، ولا يثبت ذلك FPS اللعبة");
      String game=pkg();
      if(game!=null){
        try{root("cmd game mode 2 "+game);logs.add("تم إرسال طلب Game Mode performance");}
        catch(Exception e){logs.add("Game Mode غير مدعوم");}
      }
      logs.add("PUBG: اترك 120FPS وجرّب Anti-Aliasing 2x فقط، ثم الظلال إذا بقي الأداء مستقرًا.");
      logs.add("TPP: ارفع زاوية كاميرا الشخص الثالث لأعلى قيمة رسمية داخل إعدادات اللعبة.");
      logs.add("زر استعادة دقة العرض الأصلية يعيد إعدادات XOS السابقة.");
      message(android.text.TextUtils.join("\n",logs));
      runOnUiThread(()->showPcViewGuide());
    }catch(Exception e){logs.add("تعذر إكمال الملف: "+e.getMessage());message(android.text.TextUtils.join("\n",logs));}
  }
  private void applyGraphicsProfile(boolean stable){
    ArrayList<String> logs=new ArrayList<>();
    try{
      root("id");
      if(thermal()>=PowerManager.THERMAL_STATUS_SEVERE||temperature()>=420){
        message("الحرارة مرتفعة؛ أوقف التطبيق التعديل حفاظًا على الجهاز.");return;
      }
      String current=root("wm size");
      if(!prefs.contains("gfx_original_wm"))prefs.edit().putString("gfx_original_wm",current).commit();
      if(!prefs.contains("gfx_old_xos"))prefs.edit().putString("gfx_old_xos",root("settings get system tran_refresh_mode")).commit();
      if(!prefs.contains("gfx_old_peak"))prefs.edit().putString("gfx_old_peak",root("settings get system peak_refresh_rate")).commit();
      try{root("settings put global low_power 0");logs.add("توفير الطاقة: تم إرسال طلب الإيقاف");}
      catch(Exception e){logs.add("توفير الطاقة: تعذر تغييره");}
      try{root("settings put system tran_refresh_mode 120");root("settings put system peak_refresh_rate 120.0");
        logs.add("XOS: طلب 120Hz، الوضع الفعلي يتغير حسب التطبيق");}
      catch(Exception e){logs.add("XOS: تعذر طلب 120Hz");}
      if(stable){
        root("wm size 1102x2448");
        logs.add("دقة عرض Android: "+root("wm size").replace("\n"," | "));
        logs.add("تم تقليل عدد بكسلات العرض نحو 19% لتخفيف الحمل المحتمل؛ لا يضمن تقليل دقة رسم PUBG الداخلية.");
      }else{
        root("wm size reset");
        logs.add("دقة العرض: تم إلغاء أي override (قد يؤثر على وضع PC السابق)");
      }
      String game=pkg();
      if(game!=null){
        try{String out=root("cmd game mode 2 "+game);
          logs.add(looksLikeHelp(out)?"Game Mode غير مدعوم":"طلب وضع الأداء: أُرسل");}
        catch(Exception e){logs.add("Game Mode: رفضه النظام");}
      }
      logs.add("داخل PUBG اختر Smooth + 120FPS، وأوقف Anti-Aliasing وAuto-adjust Graphics إن توفرا.");
      logs.add("لم نعدّل ملفات اللعبة. تحقق من FPS أثناء الاشتباك.");
      message(android.text.TextUtils.join("\n",logs));
    }catch(Exception e){logs.add("فشل التنفيذ: "+e.getMessage());message(android.text.TextUtils.join("\n",logs));}
  }
  private void restoreGraphicsProfile(){
    ArrayList<String> logs=new ArrayList<>();
    try{
      root("id");
      String old=prefs.getString("gfx_original_wm","");
      if(!old.isEmpty()){
        java.util.regex.Matcher match=java.util.regex.Pattern.compile("Override size: ([0-9]+x[0-9]+)").matcher(old);
        if(match.find())root("wm size "+match.group(1));else root("wm size reset");
        prefs.edit().remove("gfx_original_wm").commit();
        logs.add("تمت استعادة دقة العرض السابقة");
      }
      for(String[] entry:new String[][]{{"gfx_old_xos","tran_refresh_mode"},{"gfx_old_peak","peak_refresh_rate"}}){
        if(!prefs.contains(entry[0]))continue;
        String value=prefs.getString(entry[0],"null");
        if("null".equals(value))root("settings delete system "+entry[1]);
        else root("settings put system "+entry[1]+" "+value);
        prefs.edit().remove(entry[0]).commit();
        logs.add("استعادة "+entry[1]);
      }
      message(logs.isEmpty()?"لا توجد تغييرات محفوظة للاستعادة":android.text.TextUtils.join("\n",logs));
    }catch(Exception e){logs.add("تعذرت الاستعادة: "+e.getMessage());message(android.text.TextUtils.join("\n",logs));}
  }
  private void applyGt30Profile(){
    ArrayList<String> logs=new ArrayList<>();
    try{
      root("id");
      if(thermal()>=PowerManager.THERMAL_STATUS_SEVERE||temperature()>=420){
        message("الحرارة مرتفعة؛ لن يتم تفعيل ملف الأداء الآن.");return;
      }
      setSetting("global","low_power","0");
      logs.add("توفير الطاقة: متوقف");
      saveSetting("system","tran_refresh_mode");
      saveSetting("system","last_tran_refresh_mode_in_refresh_setting");
      setSetting("system","tran_refresh_mode","120");
      logs.add("وضع XOS المطلوب: 120Hz");
      setSetting("system","peak_refresh_rate","120.0");
      logs.add("الحد الأعلى المطلوب: 120Hz");
      String min=root("settings get system min_refresh_rate");
      logs.add("الحد الأدنى الذي يفرضه XOS: "+min+" (لم نحاول تجاوزه)");
      String active=root("dumpsys display | grep 'mActiveModeId=' | head -1");
      logs.add("الوضع الحالي: "+active.trim()+" (قد يتغير حسب التطبيق)");
      String game=pkg();
      if(game!=null){
        try{
          String out=root("cmd game mode 2 "+game);
          logs.add(looksLikeHelp(out)?"Game Mode: غير مدعوم":"Game Mode: تم إرسال طلب الأداء");
        }catch(Exception e){logs.add("Game Mode: رفضه النظام");}
        logs.add("PUBG: اضبط Smooth + 120FPS داخل اللعبة");
      }
      logs.add("لا نعدّل ملفات اللعبة أو نوقف التحكم الحراري.");
      message(android.text.TextUtils.join("\n",logs));
    }catch(Exception e){logs.add("تعذر إكمال الضبط: "+e.getMessage());message(android.text.TextUtils.join("\n",logs));}
  }
  private void diagnoseGt30(){
    try{
      String active=root("dumpsys display | grep 'mActiveModeId=' | head -1").trim();
      String mode=root("settings get system tran_refresh_mode");
      String peak=root("settings get system peak_refresh_rate");
      String min=root("settings get system min_refresh_rate");
      String game=pkg();
      String interventions=game==null?"اللعبة غير ظاهرة":root("cmd game list-configs "+game);
      message("فحص GT30 Pro\n"+active+"\nXOS tran_refresh_mode="+mode+
        "\npeak="+peak+" / min="+min+"\nThermal Status="+thermal()+
        "\nGame Mode: "+brief(interventions)+
        "\nMode 1=144Hz، 2=120Hz، 3=90Hz، 4=60Hz. هذا قياس للشاشة لا FPS اللعبة.");
    }catch(Exception e){message("فشل فحص XOS: "+e.getMessage());}
  }
  private void strongMode(){
    String game=pkg();if(game==null){message("ببجي غير موجودة");return;}
    ArrayList<String> logs=new ArrayList<>();
    try{root("id");}catch(Exception e){message("صلاحية الروت غير متاحة");return;}
    if(thermal()>=PowerManager.THERMAL_STATUS_SEVERE||temperature()>=420){
      message("الحرارة مرتفعة؛ لا يمكن تفعيل وضع الأداء القوي الآن.");return;
    }
    try{setSetting("global","low_power","0");logs.add("توفير الطاقة: تم تعطيله");}
    catch(Exception e){logs.add("توفير الطاقة: لم يتغير");}
    try{setSetting("system","peak_refresh_rate","144.0");logs.add("144Hz: قُبلت القيمة في الإعدادات");}
    catch(Exception e){logs.add("144Hz: رفض XOS الطلب");}
    try{setSetting("system","min_refresh_rate","120.0");logs.add("الحد الأدنى: قُبلت القيمة في الإعدادات");}
    catch(Exception e){logs.add("الحد الأدنى: رفض XOS الطلب");}
    try{String out=root("cmd game mode 2 "+game);logs.add(looksLikeHelp(out)?"Game Mode: غير مدعوم":"Game Mode: أُرسل طلب الأداء");}
    catch(Exception e){logs.add("Game Mode: تعذر التفعيل");}
    try{String out=root("cmd game set --mode 2 --fps 120 "+game);logs.add(looksLikeHelp(out)?"120FPS: الأمر غير مدعوم":"120FPS: أُرسل الطلب للنظام");}
    catch(Exception e){logs.add("120FPS: تعذر إرسال الطلب");}
    prefs.edit().putBoolean("strong_mode",true).commit();
    message(android.text.TextUtils.join(String.valueOf((char)10),logs)+"\nهذه طلبات للنظام، وليست قراءة FPS الفعلية.");
  }
  private void stopStrongMode(){
    ArrayList<String> logs=new ArrayList<>();
    try{root("id");}catch(Exception e){message("صلاحية الروت غير متاحة");return;}
    for(String[] entry:new String[][]{{"global","low_power"},{"system","peak_refresh_rate"},{"system","min_refresh_rate"},{"system","tran_refresh_mode"},{"system","last_tran_refresh_mode_in_refresh_setting"}}){
      String table=entry[0],key=entry[1],pref="old_"+table+"_"+key;
      if(!prefs.contains(pref))continue;
      try{
        String old=prefs.getString(pref,"null");
        if("null".equals(old))root("settings delete "+table+" "+key);
        else root("settings put "+table+" "+key+" "+old);
        prefs.edit().remove(pref).commit();logs.add(key+": استُعيد");
      }catch(Exception e){logs.add(key+": تعذرت الاستعادة");}
    }
    String game=pkg();
    if(game!=null)try{root("cmd game reset "+game);logs.add("Game Mode: أُرسل أمر الاستعادة");}
    catch(Exception e){logs.add("Game Mode: تعذرت الاستعادة");}
    try{root("cmd power set-fixed-performance-mode-enabled false");}catch(Exception ignored){}
    prefs.edit().putBoolean("strong_mode",false).commit();
    message(android.text.TextUtils.join(String.valueOf((char)10),logs));
  }
  private void verifyGraphicsPath(){
    String path=pubgIniPath();
    try{
      root("id");
      String info=root("if [ -f '"+path+"' ]; then wc -c < '"+path+"'; sha256sum '"+path+"'; else echo MISSING; fi");
      if(info.contains("MISSING")){message("ملف الرسومات غير موجود في المسار المتوقع:\n"+path);return;}
      String[] lines=info.split("\n");
      prefs.edit().putString("pubg_graphics_path",path).commit();
      message("تم ربط مسار رسومات PUBG 4.6.0 والتحقق من وجوده:\n"+path+
        "\nالحجم: "+lines[0].trim()+" بايت\nSHA256: "+(lines.length>1?lines[1].trim().split(" ")[0]:"غير متاح")+
        "\nالربط للقراءة والنسخ الاحتياطي فقط؛ أزرار تحسين الشاشة لا تكتب تلقائيًا داخل ملف اللعبة.");
    }catch(Exception e){message("تعذر التحقق من مسار الرسومات: "+e.getMessage());}
  }
  private void copyGraphicsPath(){
    String path=prefs.getString("pubg_graphics_path",pubgIniPath());
    android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
    cm.setPrimaryClip(android.content.ClipData.newPlainText("PUBG graphics path",path));
    message("تم نسخ مسار الرسومات:\n"+path);
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
  private void applyIniClarity(){applyIniQuality("1.1");}
  private void confirmIniQuality(String scale){
    new AlertDialog.Builder(this).setTitle("رفع جودة رسومات PUBG")
      .setMessage("سيُعدل r.MobileContentScaleFactor إلى "+scale+" داخل UserCustom.ini مع نسخة احتياطية. هذا لا يرفع الحد الرسمي 120FPS وقد يقلل الفريمات أو ترفضه اللعبة، والتعديل غير الرسمي قد يعرض الحساب للتقييد. أغلق PUBG بالكامل.")
      .setNegativeButton("إلغاء",null)
      .setPositiveButton("تطبيق",(d,w)->run(()->applyIniQuality(scale))).show();
  }
  private void applyIniQuality(String scale){
    if(gameFileWritesBlocked("applyIniQuality"))return;
    String stage="بدء التنفيذ";
    try{
      if(!java.util.Arrays.asList("1.1","1.15","1.25").contains(scale)){message("مستوى غير مدعوم");return;}
      stage="فحص الروت";
      root("id");
      if(!root("pidof com.tencent.ig 2>/dev/null || true").trim().isEmpty()){message("أغلق PUBG بالكامل أولًا.");return;}
      stage="قراءة الملف";
      String path=pubgIniPath(),backup=iniBackupPath();
      String original=new String(android.util.Base64.decode(root("base64 '"+path+"'"),android.util.Base64.DEFAULT),java.nio.charset.StandardCharsets.UTF_8);
      if(original.length()<100||original.length()>60000){message("تعذر قراءة الملف بأمان.");return;}
      String[] lines=original.split("\n",-1);
      boolean inUser=false;int matches=0;String prior="";
      for(int i=0;i<lines.length;i++){
        String line=lines[i].trim();
        if(line.startsWith("[")&&line.endsWith("]"))inUser=line.equals("[UserCustom DeviceProfile]");
        if(!inUser||!line.startsWith("+CVars="))continue;
        String hex=line.substring(7);
        if(hex.length()%2!=0||!hex.matches("[0-9a-fA-F]+"))continue;
        String decoded=iniDecode(hex);
        if(!decoded.startsWith("r.MobileContentScaleFactor="))continue;
        prior=decoded.substring("r.MobileContentScaleFactor=".length());
        matches++;
        if(!java.util.Arrays.asList("1","1.0","1.1","1.15","1.25").contains(prior)){
          message("قيمة رندر غير متوقعة: "+prior+"؛ لن أعدل الملف.");return;
        }
        lines[i]="+CVars="+iniEncode("r.MobileContentScaleFactor="+scale);
      }
      if(matches!=1){message("لم أجد قيمة رندر واحدة مؤكدة. العدد: "+matches);return;}
      if(prior.equals(scale)){message("المستوى "+scale+" موجود بالفعل.");return;}
      String edited=android.text.TextUtils.join("\n",java.util.Arrays.asList(lines));
      String encoded=android.util.Base64.encodeToString(edited.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
      String temp="/data/local/tmp/yrc_pubg_render_temp.ini";
      // Handle an absent backup as a normal first run (without shell exit code 1).
      stage="النسخة الاحتياطية";
      String backupState=root("if [ ! -e '"+backup+"' ]; then echo MISSING; elif [ -s '"+backup+"' ]; then echo READY; else echo EMPTY; fi");
      if("EMPTY".equals(backupState)){message("النسخة الاحتياطية الموجودة فارغة؛ لم نكتب داخل PUBG.");return;}
      if("MISSING".equals(backupState)){
        stage="إنشاء نسخة احتياطية";
        root("cp '"+path+"' '"+backup+"'");
      }
      if(!"READY".equals(root("if [ -s '"+backup+"' ]; then echo READY; else echo MISSING; fi"))){
        message("تعذر إنشاء نسخة احتياطية سليمة؛ لم نكتب داخل PUBG.");return;
      }
      stage="تحضير التعديل";
      root("printf '%s' '"+encoded+"' | base64 -d > '"+temp+"'");
      String tempHash=root("sha256sum '"+temp+"'").trim();
      if(!tempHash.matches("(?s)^[0-9a-f]{64}.*")){message("فشل تجهيز ملف الجودة، لم نكتب إلى اللعبة.");return;}
      stage="كتابة الملف";
      root("cat '"+temp+"' > '"+path+"'");
      stage="التحقق من التعديل";
      String actual=root("sha256sum '"+path+"'").trim();
      if(!actual.substring(0,64).equals(tempHash.substring(0,64))){
        root("cat '"+backup+"' > '"+path+"'");
        message("فشل التحقق من التعديل؛ تمت محاولة استعادة الملف الأصلي.");return;
      }
      root("rm -f '"+temp+"'");
      message("تمت كتابة قيمة الرندر "+scale+" والتحقق من الملف فقط، وليس من تطبيقها داخل PUBG. نسخة الاستعادة: "+backup+"\nقد تعيد اللعبة الإعداد أو تتجاهله. الحد الرسمي 120FPS لم يتغير.");
    }catch(Exception e){message("تعذر تعديل PUBG عند «"+stage+"»: "+e.getMessage());}
  }
  private void confirmIniDetails(){
    new AlertDialog.Builder(this).setTitle("رفع تفاصيل PUBG داخل الملف")
      .setMessage("هذا اختبار لتغيير ست قيم رسومية في UserCustom.ini إلى قيم مأخوذة من قسم BackUp DeviceProfile داخل نفس الملف، دون تعديل النظام أو رفع حد 120FPS. قد تتجاهلها اللعبة، وينخفض الأداء، وقد يعرّض التعديل غير الرسمي الحساب للتقييد. أغلق PUBG بالكامل أولًا.")
      .setNegativeButton("إلغاء",null)
      .setPositiveButton("تجربة مع نسخة احتياطية",(d,w)->run(()->applyIniDetails())).show();
  }
  private void applyIniDetails(){
    if(gameFileWritesBlocked("applyIniDetails"))return;
    String stage="البدء";
    try{
      stage="فحص الروت";
      root("id");
      stage="التأكد من إغلاق PUBG";
      if(!root("pidof com.tencent.ig 2>/dev/null || true").trim().isEmpty()){
        message("أغلق PUBG بالكامل قبل تعديل الرسومات.");return;
      }
      String path=pubgIniPath(),backup=iniBackupPath();
      stage="قراءة UserCustom.ini";
      String base64=root("base64 '"+path+"'");
      String original=new String(android.util.Base64.decode(base64,android.util.Base64.DEFAULT),java.nio.charset.StandardCharsets.UTF_8);
      if(original.length()<100||original.length()>60000){message("ملف PUBG غير مكتمل؛ لم يحدث أي تعديل.");return;}
      java.util.LinkedHashMap<String,String> desired=new java.util.LinkedHashMap<>();
      desired.put("r.DetailMode","2");
      desired.put("r.MaterialQualityLevel","1");
      desired.put("r.Streaming.PoolSize","300");
      desired.put("r.StaticMeshLODDistanceScale","0.8");
      desired.put("r.ParticleLODBias","0");
      desired.put("fx.NiagaraLODBias","0");
      String[] lines=original.split("\n",-1);
      java.util.HashMap<String,Integer> counts=new java.util.HashMap<>();
      java.util.HashMap<String,String> backupValues=new java.util.HashMap<>();
      java.util.HashMap<String,String> currentValues=new java.util.HashMap<>();
      String section="";
      for(String record:lines){
        String line=record.trim();
        if(line.startsWith("[")&&line.endsWith("]")){section=line;continue;}
        if(!line.startsWith("+CVars="))continue;
        String hex=line.substring(7);
        if((hex.length()%2)!=0||!hex.matches("[A-Fa-f0-9]+"))continue;
        String decoded=iniDecode(hex);
        int equals=decoded.indexOf('=');
        if(equals<0)continue;
        String key=decoded.substring(0,equals),value=decoded.substring(equals+1);
        if(!desired.containsKey(key))continue;
        if("[BackUp DeviceProfile]".equals(section))backupValues.put(key,value);
        if("[UserCustom DeviceProfile]".equals(section)){
          counts.put(key,counts.containsKey(key)?counts.get(key)+1:1);
          currentValues.put(key,value);
        }
      }
      for(String key:desired.keySet()){
        if(!desired.get(key).equals(backupValues.get(key))){
          message("قسم BackUp لا يحتوي على القيمة المتوقعة لـ "+key+"؛ لم نغيّر الملف.");return;
        }
        if(!Integer.valueOf(1).equals(counts.get(key))){
          message("تعذر تأكيد قيمة واحدة لـ "+key+" في UserCustom؛ لم نغيّر الملف.");return;
        }
      }
      java.util.HashMap<String,String> defaults=new java.util.HashMap<>();
      defaults.put("r.DetailMode","0");
      defaults.put("r.MaterialQualityLevel","0");
      defaults.put("r.Streaming.PoolSize","200");
      defaults.put("r.StaticMeshLODDistanceScale","1.3");
      defaults.put("r.ParticleLODBias","2");
      defaults.put("fx.NiagaraLODBias","2");
      for(String key:desired.keySet()){
        String existing=currentValues.get(key);
        if(!desired.get(key).equals(existing)&&!defaults.get(key).equals(existing)){
          message("القيمة الحالية غير متوقعة لـ "+key+": "+existing+"؛ لم نغيّر الملف.");return;
        }
      }
      int changed=0;
      section="";
      for(int i=0;i<lines.length;i++){
        String line=lines[i].trim();
        if(line.startsWith("[")&&line.endsWith("]")){section=line;continue;}
        if(!"[UserCustom DeviceProfile]".equals(section)||!line.startsWith("+CVars="))continue;
        String hex=line.substring(7);
        if((hex.length()%2)!=0||!hex.matches("[A-Fa-f0-9]+"))continue;
        String decoded=iniDecode(hex);
        int equals=decoded.indexOf('=');
        if(equals<0)continue;
        String key=decoded.substring(0,equals),value=decoded.substring(equals+1);
        if(!desired.containsKey(key)||desired.get(key).equals(value))continue;
        lines[i]="+CVars="+iniEncode(key+"="+desired.get(key))+(lines[i].endsWith("\r")?"\r":"");
        changed++;
      }
      if(changed==0){message("إعدادات التفاصيل الست موجودة أصلًا في UserCustom.ini.");return;}
      String edited=android.text.TextUtils.join("\n",java.util.Arrays.asList(lines));
      String encoded=android.util.Base64.encodeToString(edited.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
      stage="تأكيد النسخة الاحتياطية";
      String state=root("if [ ! -e '"+backup+"' ]; then echo MISSING; elif [ -s '"+backup+"' ]; then echo READY; else echo EMPTY; fi");
      if("EMPTY".equals(state)){message("نسخة الاستعادة الموجودة فارغة؛ تم إيقاف التعديل.");return;}
      if("MISSING".equals(state)){
        stage="حفظ نسخة أصلية";
        root("cp '"+path+"' '"+backup+"'");
      }
      if(!"READY".equals(root("if [ -s '"+backup+"' ]; then echo READY; else echo MISSING; fi"))){
        message("لم نستطع حفظ النسخة الاحتياطية؛ لم نعدّل PUBG.");return;
      }
      String temp="/data/local/tmp/yrc_pubg_detail_temp.ini";
      stage="تجهيز الملف";
      root("printf '%s' '"+encoded+"' | base64 -d > '"+temp+"'");
      String expected=root("sha256sum '"+temp+"'").trim().split(" ")[0];
      if(!expected.matches("[a-fA-F0-9]{64}"))throw new IOException("بصمة الملف المؤقت غير صالحة");
      stage="كتابة إعدادات الجودة";
      root("cat '"+temp+"' > '"+path+"'");
      String actual=root("sha256sum '"+path+"'").trim().split(" ")[0];
      if(!expected.equalsIgnoreCase(actual)){
        root("cat '"+backup+"' > '"+path+"'");
        message("فشلت مطابقة الملف؛ جرت محاولة استعادة النسخة الاحتياطية.");return;
      }
      root("rm -f '"+temp+"'");
      message("تمت كتابة "+changed+" قيم للتفاصيل والخامات والتحقق من تطابق الملف.\n"+
        "أغلق اللعبة قبل التعديل ثم اختبر مشهدًا مماثلًا أثناء اللعب. لا يعني حفظ القيم أن PUBG استخدمتها أو أن FPS ارتفع.\n"+
        "للتراجع استخدم زر استعادة ملف رسومات PUBG الأصلي.");
    }catch(Exception e){message("فشل وضع التفاصيل عند «"+stage+"»: "+e.getMessage());}
  }
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
  private void restoreActiveSav(){
    try{
      root("id");
      if(!root("pidof com.tencent.ig 2>/dev/null || true").trim().isEmpty()){
        message("أغلق PUBG قبل استعادة Active.sav.");return;
      }
      String path=activeSavPath(),backup=activeSavBackup();
      if(!"YES".equals(root("if [ -s '"+backup+"' ]; then echo YES; else echo NO; fi"))){
        message("لا توجد نسخة Active.sav محفوظة في YRC.");return;
      }
      root("cat '"+backup+"' > '"+path+"'");
      String a=root("sha256sum '"+backup+"'").trim().split(" ")[0];
      String b=root("sha256sum '"+path+"'").trim().split(" ")[0];
      if(a.equalsIgnoreCase(b))prefs.edit().remove("active_graphics_quality").remove("active_graphics_sha").apply();
      message(a.equalsIgnoreCase(b)?"تم استرجاع Active.sav كما كان قبل تجربة GFX.":
        "لم نستطع تأكيد الاستعادة؛ لا تفتح اللعبة قبل فحص الملف.");
    }catch(Exception e){message("تعذرت استعادة Active.sav: "+e.getMessage());}
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
  private void restoreBeforeMax(){
    try{
      root("id");
      if(!root("pidof com.tencent.ig 2>/dev/null || true").trim().isEmpty()){
        message("أغلق PUBG بالكامل قبل إلغاء MAX.");return;
      }
      String backup="/sdcard/Download/YRC-PUBG-before-MAX.ini";
      String path=pubgIniPath();
      if(!"READY".equals(root("if [ -s '"+backup+"' ]; then echo READY; else echo MISSING; fi"))){
        message("لا توجد نسخة محفوظة قبل MAX، لم يحدث تغيير.");return;
      }
      root("cat '"+backup+"' > '"+path+"'");
      String expected=root("sha256sum '"+backup+"'").trim().split(" ")[0];
      String actual=root("sha256sum '"+path+"'").trim().split(" ")[0];
      if(!expected.equalsIgnoreCase(actual)){
        message("لم أستطع تأكيد استعادة الملف، توقف عن التعديل حتى تفحصه.");return;
      }
      prefs.edit().remove("last_graphics_preset").remove("last_graphics_profile_sha")
        .remove("last_graphics_profile_scale").apply();
      message("تمت استعادة UserCustom.ini إلى حالته قبل تجربة MAX والتحقق من تطابق الملف.");
    }catch(Exception e){message("تعذرت استعادة ملف ما قبل MAX: "+e.getMessage());}
  }
  private void confirmCombinedIniGraphics(){
    new AlertDialog.Builder(this).setTitle("تعديل الرسومات داخل PUBG")
      .setMessage("هذا وضع تجريبي يكتب 7 قيم رسومية في UserCustom.ini بعد حفظ نسخة احتياطية: رندر 1.15 وتفاصيل وخامات أعلى. لن يعدّل ملفات أخرى أو يعطّل الحماية. قد تتجاهل PUBG القيم أو تعيدها عند التشغيل، وقد ينخفض FPS أو يتعرض الحساب للتقييد. أغلق اللعبة بالكامل أولًا.")
      .setNegativeButton("إلغاء",null)
      .setPositiveButton("كتابة وفحص",(d,w)->run(()->applyCombinedIniGraphics())).show();
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
  private void applyCombinedIniGraphics(){
    if(gameFileWritesBlocked("applyCombinedIniGraphics"))return;
    String stage="البدء";
    try{
      stage="فحص الروت";
      root("id");
      stage="التحقق من إغلاق PUBG";
      if(!root("pidof com.tencent.ig 2>/dev/null || true").trim().isEmpty()){
        message("أغلق PUBG بالكامل قبل تطبيق التعديل.");return;
      }
      String path=pubgIniPath(),backup=iniBackupPath();
      stage="قراءة ملف الرسومات";
      String raw64=root("base64 '"+path+"'");
      String original=new String(android.util.Base64.decode(raw64,android.util.Base64.DEFAULT),java.nio.charset.StandardCharsets.UTF_8);
      if(original.length()<100||original.length()>60000)throw new IOException("حجم UserCustom.ini غير متوقع");
      java.util.LinkedHashMap<String,String> desired=combinedGraphicsValues();
      java.util.HashMap<String,Integer> counts=new java.util.HashMap<>();
      java.util.HashMap<String,String> oldValues=new java.util.HashMap<>();
      java.util.HashMap<String,String> backupValues=new java.util.HashMap<>();
      java.util.HashMap<String,String> defaults=new java.util.HashMap<>();
      defaults.put("r.MobileContentScaleFactor","1");
      defaults.put("r.DetailMode","0");
      defaults.put("r.MaterialQualityLevel","0");
      defaults.put("r.Streaming.PoolSize","200");
      defaults.put("r.StaticMeshLODDistanceScale","1.3");
      defaults.put("r.ParticleLODBias","2");
      defaults.put("fx.NiagaraLODBias","2");
      String section="";
      String[] lines=original.split("\n",-1);
      for(String record:lines){
        String line=record.trim();
        if(line.startsWith("[")&&line.endsWith("]")){section=line;continue;}
        if(!line.startsWith("+CVars="))continue;
        String hex=line.substring(7);
        if(hex.length()%2!=0||!hex.matches("[a-fA-F0-9]+"))continue;
        String decoded=iniDecode(hex);
        int eq=decoded.indexOf('=');
        if(eq<0)continue;
        String key=decoded.substring(0,eq),val=decoded.substring(eq+1);
        if(!desired.containsKey(key))continue;
        if("[UserCustom DeviceProfile]".equals(section)){
          counts.put(key,counts.containsKey(key)?counts.get(key)+1:1);
          oldValues.put(key,val);
        }else if("[BackUp DeviceProfile]".equals(section)){
          backupValues.put(key,val);
        }
      }
      for(String key:desired.keySet()){
        if(!Integer.valueOf(1).equals(counts.get(key))){
          message("أوقف التعديل: لم أجد قيمة واحدة لـ "+key+" في القسم الحالي.");return;
        }
        String old=oldValues.get(key),val=desired.get(key);
        if("r.MobileContentScaleFactor".equals(key)&&"1.5".equals(old)){
          message("الرندر الحالي 1.5 يخص وضع MAX. لا تستخدم وضع 1.15 لأنه أقل؛ اضغط زر تطبيق MAX Graphics في القسم الأعلى، ثم افحص القيم بعد اللعب.");
          return;
        }
        boolean known=val.equals(old)||defaults.get(key).equals(old)||
          ("r.MobileContentScaleFactor".equals(key)&&"1.0".equals(old));
        if(!known){
          message("القيمة الحالية لـ "+key+" غير متوقعة: "+old+"؛ لم أعدل الملف.");return;
        }
        if(!"r.MobileContentScaleFactor".equals(key)&&!val.equals(backupValues.get(key))){
          message("ملف BackUp لا يؤكد قيمة "+key+"؛ أوقفت التعديل.");return;
        }
      }
      int changed=0;
      section="";
      for(int i=0;i<lines.length;i++){
        String line=lines[i].trim();
        if(line.startsWith("[")&&line.endsWith("]")){section=line;continue;}
        if(!"[UserCustom DeviceProfile]".equals(section)||!line.startsWith("+CVars="))continue;
        String hex=line.substring(7);
        if(hex.length()%2!=0||!hex.matches("[a-fA-F0-9]+"))continue;
        String decoded=iniDecode(hex);
        int eq=decoded.indexOf('=');
        if(eq<0)continue;
        String key=decoded.substring(0,eq),val=decoded.substring(eq+1);
        if(!desired.containsKey(key)||desired.get(key).equals(val))continue;
        lines[i]="+CVars="+iniEncode(key+"="+desired.get(key))+
          (lines[i].endsWith("\r")?"\r":"");
        changed++;
      }
      if(changed==0){message("قيم الرندر والتفاصيل السبعة موجودة بالفعل داخل UserCustom.ini. يلزم فحصها بعد تشغيل اللعبة.");return;}
      String edited=android.text.TextUtils.join("\n",java.util.Arrays.asList(lines));
      String content64=android.util.Base64.encodeToString(edited.getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);
      stage="حفظ نسخة احتياطية";
      String state=root("if [ ! -e '"+backup+"' ]; then echo MISSING; elif [ -s '"+backup+"' ]; then echo READY; else echo EMPTY; fi");
      if("EMPTY".equals(state)){message("نسخة YRC الاحتياطية فارغة؛ لم أعدل PUBG.");return;}
      if("MISSING".equals(state))root("cp '"+path+"' '"+backup+"'");
      if(!"READY".equals(root("if [ -s '"+backup+"' ]; then echo READY; else echo FAIL; fi"))){
        message("فشل تأكيد النسخة الاحتياطية، لم أكتب أي قيم.");return;
      }
      String temp="/data/local/tmp/yrc_pubg_combined_temp.ini";
      stage="تجهيز القيم";
      root("printf '%s' '"+content64+"' | base64 -d > '"+temp+"'");
      String tempSha=root("sha256sum '"+temp+"'").trim().split(" ")[0];
      if(!tempSha.matches("[a-fA-F0-9]{64}"))throw new IOException("بصمة الملف غير صالحة");
      stage="تعديل UserCustom.ini";
      root("cat '"+temp+"' > '"+path+"'");
      stage="التحقق من الكتابة";
      String writtenSha=root("sha256sum '"+path+"'").trim().split(" ")[0];
      if(!tempSha.equalsIgnoreCase(writtenSha)){
        root("cat '"+backup+"' > '"+path+"'");
        message("لم تطابق الكتابة الملف المقصود؛ حاولت استعادة النسخة الأصلية.");return;
      }
      root("rm -f '"+temp+"'");
      prefs.edit().putString("last_graphics_preset","COMBINED")
        .putString("last_graphics_profile_sha",writtenSha)
        .putString("last_graphics_profile_scale","1.15").apply();
      message("نجحت كتابة "+changed+" من أصل 7 إعدادات داخل UserCustom.ini والتحقق من تطابق SHA256.\n"+
        "هذه النتيجة تثبت كتابة الملف فقط؛ لا تثبت تطبيق PUBG لها. افتح اللعبة ثم استخدم فحص البقاء بعد اللعب.");
    }catch(Exception e){message("تعذر تعديل الرسومات عند «"+stage+"»: "+e.getMessage());}
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
  private void restoreIniAndOfferGraphics(){
    try{
      root("id");
      if(!root("pidof com.tencent.ig 2>/dev/null || true").trim().isEmpty()){
        message("أغلق PUBG بالكامل قبل استعادة ملف الرسومات.");return;
      }
      String backup=iniBackupPath(),path=pubgIniPath();
      String state=root("if [ -s '"+backup+"' ]; then echo READY; else echo MISSING; fi");
      if(!"READY".equals(state)){
        message("لم أجد نسخة استعادة من YRC. افتح PUBG واستخدم «إعادة ضبط الرسومات» من إعدادات اللعبة.");
        runOnUiThread(()->showOfficialGraphicsGuide());
        return;
      }
      // Restore only the known graphics file, then verify byte-for-byte via SHA256.
      root("cat '"+backup+"' > '"+path+"'");
      String expected=root("sha256sum '"+backup+"'").trim().split(" ")[0];
      String actual=root("sha256sum '"+path+"'").trim().split(" ")[0];
      if(!expected.equalsIgnoreCase(actual)){
        message("فشل التحقق من استعادة UserCustom.ini. لا تجرّب تعديلًا جديدًا قبل فحص الملف.");return;
      }
      message("تمت استعادة UserCustom.ini من نسخة YRC وتأكيد تطابق الملف. اضبط جودة PUBG من القائمة الرسمية.");
      runOnUiThread(()->showOfficialGraphicsGuide());
    }catch(Exception e){message("تعذرت استعادة إعدادات PUBG: "+e.getMessage());}
  }
  private void showOfficialGraphicsGuide(){
    new AlertDialog.Builder(this).setTitle("ضبط رسومات PUBG 4.6.0")
      .setMessage("لأعلى FPS مع تحسين الوضوح:\n1. الإعدادات ← الرسومات الجرافيكية ← القتال.\n2. جرّب «سلسة» بدل «سلاسة فائقة».\n3. إن بقي خيار «ألترا إكستريم» متاحًا، اختر 120FPS.\n4. جرّب تنعيم الحواف 2x إن سمحت اللعبة، وأبقِ الظلال والتكيف معطّلين.\n5. لو اختفى 120FPS، ارجع إلى «سلاسة فائقة» أو اختر معدل الإطارات الأعلى المتاح.\n\nلأعلى رسومات فعلية: اختر «متوازنة» أو «عالية الدقة» واقبل الحد الذي تعرضه اللعبة من FPS. لا يمكن ضمان شكل PC أو 4K/240FPS.")
      .setNegativeButton("إغلاق",null)
      .setPositiveButton("فتح PUBG",(d,w)->launchGame()).show();
  }
  private void restoreIniClarity(){
    try{
      root("id");
      if(!root("pidof com.tencent.ig 2>/dev/null || true").trim().isEmpty()){message("أغلق PUBG بالكامل قبل الاستعادة.");return;}
      String backup=iniBackupPath(),path=pubgIniPath();
      if(!"READY".equals(root("if [ -s '"+backup+"' ]; then echo READY; else echo MISSING; fi"))){message("لا توجد نسخة احتياطية صالحة للاستعادة.");return;}
      root("cat '"+backup+"' > '"+path+"'");
      String a=root("sha256sum '"+backup+"'").trim().split(" ")[0];
      String b=root("sha256sum '"+path+"'").trim().split(" ")[0];
      message(a.equals(b)?"تمت استعادة ملف PUBG الأصلي بنجاح.":"فشل التحقق من الاستعادة؛ لا تفتح PUBG قبل فحص الملف.");
    }catch(Exception e){message("فشل استرجاع ملف الرسومات: "+e.getMessage());}
  }
  private String pubgIniPath(){
    return "/sdcard/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini";
  }
  private void backupPubgConfig(){
    try{
      root("id");
      String src=pubgIniPath();
      String dst="/sdcard/Download/YRC-PUBG-UserCustom-backup.ini";
      root("test -f '"+src+"' && cp -p '"+src+"' '"+dst+"'");
      String size=root("wc -c < '"+dst+"'").trim();
      message("تم حفظ نسخة احتياطية من إعدادات PUBG في:\n"+dst+"\nالحجم: "+size+" بايت\nلم يتم تعديل ملف اللعبة.");
    }catch(Exception e){message("تعذر إنشاء النسخة الاحتياطية: "+e.getMessage());}
  }
  private void previewPubgGraphics(){
    try{
      String raw=root("cat '"+pubgIniPath()+"'");
      StringBuilder out=new StringBuilder("معاينة إعدادات PUBG الحالية (قراءة فقط):\n");
      int found=0;
      for(String line:raw.split("\\r?\n")){
        if(!line.startsWith("+CVars="))continue;
        String hex=line.substring(7).trim();
        if(hex.length()%2!=0||!hex.matches("[0-9A-Fa-f]+"))continue;
        StringBuilder d=new StringBuilder();
        for(int i=0;i<hex.length();i+=2){
          int ch=Integer.parseInt(hex.substring(i,i+2),16)^0x79;
          d.append(ch>=32&&ch<=126?(char)ch:'?');
        }
        String value=d.toString();
        if(value.matches("(?i).*(quality|resolution|scale|shadow|msaa|fps|frame|texture|render).*")){
          if(found++<25)out.append(value).append('\n');
        }
      }
      out.append("\nالنتيجة: ").append(found).append(" إعدادًا ذا صلة. لا يمكن ضمان 4K أو 240FPS من CVars.");
      message(out.toString());
    }catch(Exception e){message("تعذرت معاينة إعدادات PUBG: "+e.getMessage());}
  }
  private void analyzePubgConfig(){
    String path="/sdcard/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini";
    try{
      if(!"com.tencent.ig".equals(pkg())){message("هذا القارئ مخصص لنسخة PUBG Global com.tencent.ig");return;}
      String raw=root("cat '"+path+"'");
      if(raw.length()>60000){message("ملف الإعدادات كبير؛ تم إيقاف القراءة الآمنة.");return;}
      String section="";int count=0;StringBuilder report=new StringBuilder("PUBG 4.6.0 | قراءة فقط\n");
      for(String line:raw.split("\\r?\n")){
        line=line.trim();
        if(line.startsWith("[")&&line.endsWith("]")){section=line;continue;}
        if(!line.startsWith("+CVars="))continue;
        String hex=line.substring(7).trim();
        if(hex.length()%2!=0||!hex.matches("[0-9A-Fa-f]+"))continue;
        StringBuilder decoded=new StringBuilder();
        for(int i=0;i<hex.length();i+=2){
          int value=Integer.parseInt(hex.substring(i,i+2),16)^0x79;
          decoded.append(value>=32&&value<=126?(char)value:'?');
        }
        if(section.contains("UserCustom")){
          if(count<60)report.append(decoded).append('\n');
          count++;
        }
      }
      report.append("عدد قيم UserCustom: ").append(count).append("\nلم يتم تعديل الملف. القيم لا تثبت FPS فعليًا.");
      message(report.toString());
    }catch(Exception e){message("تعذرت قراءة إعدادات PUBG بالروت: "+e.getMessage());}
  }
  private void request144(){
    try{
      root("id");
      String modes=root("dumpsys display | grep -E '144\\.0|144Hz' | head -n 4");
      if(modes.isEmpty()){message("لم يظهر وضع 144Hz في قراءة النظام. لم نغير الإعدادات.");return;}
      setSetting("system","peak_refresh_rate","144.0");
      try{setSetting("system","min_refresh_rate","120.0");}catch(Exception ignored){}
      String peak=root("settings get system peak_refresh_rate");
      String min=root("settings get system min_refresh_rate");
      message("طلب الشاشة: 144Hz.\npeak="+peak+" / min="+min+"\nإذا أعاد XOS القيمة إلى 60 فهذا قيد بالنظام، وليس 144FPS داخل PUBG.");
    }catch(Exception e){message("XOS لم يقبل طلب 144Hz أو تعذر تطبيقه: "+e.getMessage());}
  }
  private void requestGameFps(int fps){
    String game=pkg();if(game==null){message("لم يتم العثور على ببجي");return;}
    try{
      String out=root("cmd game set --mode 2 --fps "+fps+" "+game);
      if(looksLikeHelp(out)){message("أمر FPS غير مدعوم على هذا الإصدار؛ لم يتم تأكيد تطبيقه.");return;}
      String config=root("cmd game list-configs "+game);
      message("تم إرسال طلب "+fps+"FPS إلى Game Mode.\n"+brief(config)+"\nهذا ليس قياس FPS داخل اللعبة، وقد تتجاهل ببجي الطلب.");
    }catch(Exception e){message("فشل طلب FPS: "+e.getMessage());}
  }
  private boolean looksLikeHelp(String out){
    return out!=null&&(out.contains("NumberFormatException")||out.contains("Exception")||out.contains("Error:")||out.contains("Intervention configs consists of")||out.contains("Set app to run in the specified game mode")||out.contains("Usage:")||out.contains("Unknown option"));
  }
  private String brief(String out){
    if(out==null)return "";
    return out.length()>650?out.substring(0,650)+"…":out;
  }
  private void applyDownscale(){
    String game=pkg();
    if(game==null){message("لم يتم العثور على ببجي");return;}
    try{
      root("id");
      String result=root("cmd game set --mode 2 --downscale 0.9 "+game);
      if(looksLikeHelp(result)){message("رفض XOS أمر تخفيف الرسم؛ لم يتم تأكيد التعديل.");return;}
      String modes=root("cmd game list-configs "+game);
      message("أُرسل طلب الرسم 90%.\n"+brief(modes)+"\nأعد تشغيل ببجي. لا يمكن تأكيد تطبيقه إلا بقياس فعلي.");
    }catch(Exception e){message("لم يُطبّق تخفيف الرسم: "+e.getMessage());}
  }
  private void resetDownscale(){
    String game=pkg();if(game==null){message("لم يتم العثور على ببجي");return;}
    try{root("cmd game reset --mode 2 "+game);message("تم إرسال أمر استعادة إعدادات رسم اللعبة الافتراضية. أعد تشغيل ببجي.");}
    catch(Exception e){message("تعذرت الاستعادة: "+e.getMessage());}
  }
  private void showPcViewGuide(){
    new AlertDialog.Builder(this).setTitle("منظور TPP الواسع")
      .setMessage("لمنظور قريب من الكمبيوتر دون تصغير الشاشة:\n\n١. افتح إعدادات ببجي.\n٢. ابحث عن «منظور كاميرا الشخص الثالث» أو TPP Camera View (قد يختلف مكانه حسب الإصدار).\n٣. ارفع زاوية الرؤية إلى أعلى قيمة تسمح بها اللعبة.\n٤. للأداء اختر الرسومات السلسة وأعلى معدل إطارات رسمي متاح (120 عندك).\n\nلا يستطيع التطبيق تجاوز الحد الذي تفرضه ببجي على الكاميرا أو الإطارات.")
      .setNegativeButton("إغلاق",null)
      .setPositiveButton("فتح ببجي",(d,w)->launchGame()).show();
  }
  private void launchGame(){
    String game=pkg();
    if(game==null){message("لم يتم العثور على ببجي");return;}
    Intent i=getPackageManager().getLaunchIntentForPackage(game);
    if(i!=null)startActivity(i);else message("تعذر تشغيل ببجي");
  }
  private void restore(){
    ArrayList<String> result=new ArrayList<>();
    try{root("id");}catch(Exception e){message("تحتاج صلاحية الروت للاستعادة");return;}
    for(String[] entry:new String[][]{{"global","low_power"},{"global","wifi_sleep_policy"},{"system","peak_refresh_rate"},{"system","min_refresh_rate"}}){
      String table=entry[0],key=entry[1],pref="old_"+table+"_"+key;
      if(!prefs.contains(pref))continue;
      String old=prefs.getString(pref,"null");
      try{
        if("null".equals(old))root("settings delete "+table+" "+key);
        else root("settings put "+table+" "+key+" "+old);
        prefs.edit().remove(pref).commit();result.add(key+": تمت الاستعادة");
      }catch(Exception e){result.add(key+": فشلت الاستعادة: "+e.getMessage());}
    }
    String game=pkg();
    if(game!=null)try{root("cmd game mode 1 "+game);result.add("وضع اللعبة: قياسي");}catch(Exception e){result.add("تعذرت استعادة وضع اللعبة");}
    try{root("cmd power set-fixed-performance-mode-enabled false");prefs.edit().putBoolean("fixed",false).commit();result.add("وضع الأداء الثابت: متوقف");}catch(Exception e){result.add("تعذر إيقاف وضع الأداء الثابت");}
    runOnUiThread(()->getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    message(android.text.TextUtils.join("\n",result));
  }
  @Override protected void onDestroy(){executor.shutdown();super.onDestroy();}
}