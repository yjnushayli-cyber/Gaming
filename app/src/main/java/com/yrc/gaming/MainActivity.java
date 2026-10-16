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
      root("cmd game mode "+mode+" "+game);
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
    for(String[] entry:new String[][]{{"global","low_power"},{"global","wifi_sleep_policy"}}){
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
    if(game!=null)try{root("cmd game mode standard "+game);result.add("وضع اللعبة: قياسي");}catch(Exception e){result.add("تعذرت استعادة وضع اللعبة");}
    try{root("cmd power set-fixed-performance-mode-enabled false");prefs.edit().putBoolean("fixed",false).commit();result.add("وضع الأداء الثابت: متوقف");}catch(Exception e){result.add("تعذر إيقاف وضع الأداء الثابت");}
    runOnUiThread(()->getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON));
    message(android.text.TextUtils.join("\n",result));
  }
  @Override protected void onDestroy(){executor.shutdown();super.onDestroy();}
}