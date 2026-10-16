package com.yrc.gaming;

import android.app.*;import android.os.*;import android.content.*;import android.graphics.Color;import android.view.*;import android.widget.*;import java.io.*;import java.util.*;import java.util.concurrent.*;

public class MainActivity extends Activity {
 LinearLayout layout; TextView status; ExecutorService worker=Executors.newSingleThreadExecutor();
 final String[] keys={"peak_refresh_rate","min_refresh_rate","low_power"};
 final String[] tables={"system","system","global"};
 final String[] labels={"144Hz Display Preference","Keep Refresh Rate High","Disable Battery Saver"};
 final String[] desired={"144.0","144.0","0"};
 boolean[] enabled={false,false,false}; String[] original=new String[3];
 android.content.SharedPreferences prefs;
 String root(String cmd) throws Exception {Process p=new ProcessBuilder("su","-c",cmd).redirectErrorStream(true).start();ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] a=new byte[1024];int n;while((n=p.getInputStream().read(a))!=-1)b.write(a,0,n);int exit=p.waitFor();if(exit!=0)throw new Exception(b.toString());return b.toString().trim();}
 void task(Runnable r){worker.execute(r);}
 void info(String msg){runOnUiThread(()->status.setText(msg));}
 public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("settings",0);layout=new LinearLayout(this);layout.setOrientation(1);layout.setPadding(28,30,28,24);layout.setBackgroundColor(Color.rgb(13,18,29));ScrollView scroll=new ScrollView(this);scroll.addView(layout);setContentView(scroll);
 heading("YRC GAMING PRO");status=text("Root controller • reversible settings");
 Button grant=button("AUTHORIZE ROOT",()->task(()->{try{root("id");info("Root permission granted");}catch(Exception e){info("Root unavailable: "+e.getMessage());}}));
 for(int i=0;i<3;i++){final int k=i;Switch sw=new Switch(this);sw.setText(labels[i]);sw.setTextColor(Color.WHITE);sw.setTextSize(17);sw.setPadding(0,20,0,20);sw.setChecked(prefs.getBoolean("on"+i,false));layout.addView(sw);sw.setOnCheckedChangeListener((v,on)->task(()->apply(k,on,sw)));}
 heading("PC-LIKE WIDE VIEW");text("PUBG camera FOV is controlled by PUBG. No safe external root API can expand its in-game camera. Use the in-game FPP camera view slider where available; a 144Hz screen does not equal 144 game FPS.");
 button("OPEN PUBG FOR FOV & GRAPHICS",()->{Intent x=getPackageManager().getLaunchIntentForPackage("com.tencent.ig");if(x==null)x=getPackageManager().getLaunchIntentForPackage("com.pubg.imobile");if(x!=null)startActivity(x);else info("Install PUBG Mobile first. In PUBG: Settings > Controls/Camera > FPP Camera View; Graphics > Smooth and highest officially available frame rate.");});
 heading("DISPLAY PROFILES");
 button("SMOOTH / 120Hz DISPLAY",()->task(()->displayProfile("120.0")));
 button("HIGH REFRESH / 144Hz DISPLAY",()->task(()->displayProfile("144.0")));
 button("RESTORE DISPLAY PROFILE",()->task(()->{try{for(int i=0;i<2;i++)restore(i);info("Display settings restored");}catch(Exception e){info(e.toString());}}));
 heading("GAME CONTROLS");button("LAUNCH PUBG MOBILE",()->{Intent x=getPackageManager().getLaunchIntentForPackage("com.tencent.ig");if(x==null)x=getPackageManager().getLaunchIntentForPackage("com.pubg.imobile");if(x!=null)startActivity(x);else info("PUBG package not found");});
 button("FRAME INTERPOLATION / GAME SETTINGS",()->{startActivity(new Intent(android.provider.Settings.ACTION_SETTINGS));info("Open XArena/Game Mode and disable MEMC/frame interpolation if available. Vendor toggle is not exposed as a verified Android API.");});
 button("RESTORE ALL SETTINGS",()->task(()->{for(int i=0;i<3;i++)if(prefs.getBoolean("on"+i,false))restore(i);info("Restore requested; reopen app to refresh switches");}));
 heading("SAFETY");button("BATTERY TEMPERATURE",()->{Intent x=registerReceiver(null,new IntentFilter(Intent.ACTION_BATTERY_CHANGED));if(x!=null){int t=x.getIntExtra("temperature",-1);info("Battery: "+(t/10f)+" °C. Performance may throttle under load.");}});
 button("PING DIAGNOSTIC",()->task(()->{try{Process p=new ProcessBuilder("ping","-c","4","-W","2","1.1.1.1").redirectErrorStream(true).start();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] a=new byte[512];int n;while((n=p.getInputStream().read(a))!=-1)out.write(a,0,n);p.waitFor();info(out.toString());}catch(Exception e){info(e.toString());}}));
 }
 void displayProfile(String hz){try{for(int i=0;i<2;i++){String old=root("settings get system "+keys[i]);if(!prefs.contains("old"+i))prefs.edit().putString("old"+i,old).commit();root("settings put system "+keys[i]+" "+hz);String actual=root("settings get system "+keys[i]);if(!hz.equals(actual))throw new Exception("Refresh setting rejected: "+actual);prefs.edit().putBoolean("on"+i,true).commit();}info("Requested "+hz+" Hz display preference. Actual refresh and PUBG FPS depend on device/game support.");}catch(Exception e){info("Display profile failed: "+e.getMessage());}}
 void apply(int i,boolean on,Switch sw){try{if(on){String old=root("settings get "+tables[i]+" "+keys[i]);prefs.edit().putString("old"+i,old).apply();root("settings put "+tables[i]+" "+keys[i]+" "+desired[i]);String actual=root("settings get "+tables[i]+" "+keys[i]);if(!actual.equals(desired[i]))throw new Exception("Setting rejected: "+actual);prefs.edit().putBoolean("on"+i,true).apply();info(labels[i]+" enabled");}else{restore(i);info(labels[i]+" restored");}}catch(Exception e){info("Unable to apply: "+e.getMessage());runOnUiThread(()->sw.setChecked(prefs.getBoolean("on"+i,false)));}}
 void restore(int i){try{String old=prefs.getString("old"+i,"null");if(old.equals("null"))root("settings delete "+tables[i]+" "+keys[i]);else root("settings put "+tables[i]+" "+keys[i]+" "+old);prefs.edit().putBoolean("on"+i,false).apply();}catch(Exception e){info("Restore error: "+e.getMessage());}}
 void heading(String s){TextView t=text(s);t.setTextSize(23);t.setPadding(0,26,0,12);t.setTextColor(Color.rgb(66,209,173));}
 TextView text(String s){TextView t=new TextView(this);t.setText(s);t.setTextColor(Color.WHITE);t.setTextSize(15);t.setPadding(0,12,0,12);layout.addView(t);return t;}
 Button button(String s,Runnable action){Button b=new Button(this);b.setText(s);layout.addView(b);b.setOnClickListener(v->action.run());return b;}
 protected void onDestroy(){worker.shutdown();super.onDestroy();}
}
