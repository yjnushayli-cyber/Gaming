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
    title("ملفات الأداء");
    profiles=new RadioGroup(this);
    profiles.setOrientation(RadioGroup.VERTICAL);
    profiles.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
    addRadio(101,"ثبات الفريمات — أولوية 120FPS");
    addRadio(102,"متوازن — أداء وصورة");
    addRadio(103,"جودة العرض — دون تعديل ملفات اللعبة");
    profiles.check(prefs.getInt("profile",101));
    layout.addView(profiles);
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
  private String root(String cmd)throws Exception{
    java.lang.Process p=new ProcessBuilder("su","-c",cmd).redirectErrorStream(true).start();
    ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buf=new byte[1024];int n;
    try(InputStream in=p.getInputStream()){while((n=in.read(buf))!=-1){if(out.size()>16000)break;out.write(buf,0,n);}}
    int code=p.waitFor();String text=out.toString("UTF-8").trim();
    if(code!=0)throw new IOException("رمز "+code+": "+text);
    return text;
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
      for(String line:current.split("\\n"))if(line.startsWith("Physical size:"))physical=line.substring(14).trim();
      if(physical==null||!physical.matches("[0-9]+x[0-9]+"))throw new IOException("تعذر معرفة الدقة الأصلية: "+current);
      String[] dims=physical.split("x");
      int shortSide=Math.min(Integer.parseInt(dims[0]),Integer.parseInt(dims[1]));
      int longSide=(int)Math.round(shortSide*16.0/9.0);
      longSide-=longSide%2;
      if(!prefs.contains("pc_old_size"))prefs.edit().putString("pc_old_size",current).commit();
      root("wm size "+shortSide+"x"+longSide);
      message("تم تطبيق شاشة PC 16:9: "+root("wm size")+"\\nللرجوع اضغط «استعادة الشاشة الأصلية».");
    }catch(Exception e){message("تعذر تفعيل شاشة PC: "+e.getMessage());}
  }
  private void restorePcScreen(){
    try{
      String old=prefs.getString("pc_old_size","");
      if(old.isEmpty()){message("لا يوجد مقاس سابق محفوظ. لم نغيّر الشاشة.");return;}
      String override=null;
      for(String line:old.split("\\n"))if(line.startsWith("Override size:"))override=line.substring(14).trim();
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
      String report=String.format(java.util.Locale.US,"Wi-Fi → الراوتر %s\\nفقد الحزم: %s%%\\nمتوسط الاستجابة: %.1f ms\\nأعلى استجابة: %.1f ms\\nالتذبذب التقريبي: %.1f ms\\n",ip,loss.group(1),avg,max,jitter);
      double lost=Double.parseDouble(loss.group(1));
      if(lost>0||jitter>15||max>80)report+="النتيجة: تذبذب محلي محتمل؛ جرّب 5GHz والاقتراب من الراوتر وإيقاف التحميلات.";
      else report+="الاتصال بالراوتر مستقر في هذه العينة؛ قد يكون تذبذب PUBG خارج الشبكة المحلية.";
      message(report);
    }catch(Exception e){message("تعذر اختبار Wi-Fi: "+e.getMessage());}
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
    for(String[] entry:new String[][]{{"global","low_power"},{"system","peak_refresh_rate"},{"system","min_refresh_rate"}}){
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
  private void analyzePubgConfig(){
    String path="/sdcard/Android/data/com.tencent.ig/files/UE4Game/ShadowTrackerExtra/ShadowTrackerExtra/Saved/Config/Android/UserCustom.ini";
    try{
      if(!"com.tencent.ig".equals(pkg())){message("هذا القارئ مخصص لنسخة PUBG Global com.tencent.ig");return;}
      String raw=root("cat '"+path+"'");
      if(raw.length()>60000){message("ملف الإعدادات كبير؛ تم إيقاف القراءة الآمنة.");return;}
      String section="";int count=0;StringBuilder report=new StringBuilder("PUBG 4.6.0 | قراءة فقط\n");
      for(String line:raw.split("\\r?\\n")){
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