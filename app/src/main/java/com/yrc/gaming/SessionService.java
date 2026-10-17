package com.yrc.gaming;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * User-visible FGS watchdog, not a root hook or game injection.
 * Restores saved Android settings after confirmed PUBG shutdown or high heat.
 * OEMs can kill foreground services; manual recovery in MainActivity is required.
 */
public final class SessionService extends Service {
    static final String ACTION_START="com.yrc.gaming.SESSION_START";
    static final String ACTION_STOP="com.yrc.gaming.SESSION_STOP";
    private static final String CHANNEL="yrc_v6_session";
    private static final int NOTICE=6096;
    private volatile boolean interrupted=false;
    private final AtomicBoolean closing=new AtomicBoolean(false);
    private Thread watcher;

    @Override public IBinder onBind(Intent intent){return null;}

    @Override public int onStartCommand(Intent intent,int flags,int id){
        if(intent!=null&&ACTION_STOP.equals(intent.getAction())){
            finish("تم إيقاف الجلسة من إشعار YRC");
            return START_NOT_STICKY;
        }
        if(!SessionEngine.active(this)){
            stopSelf();
            return START_NOT_STICKY;
        }
        try{showForeground();}
        catch(Exception e){
            finish("تعذر عرض إشعار الخدمة؛ سنحاول استعادة الإعدادات.");
            return START_NOT_STICKY;
        }
        if(watcher==null||!watcher.isAlive()){
            interrupted=false;
            watcher=new Thread(this::watch,"YRC-v6-safe-session");
            watcher.setDaemon(true);
            watcher.start();
        }
        return START_STICKY;
    }

    private void showForeground(){
        NotificationManager manager=(NotificationManager)
            getSystemService(Context.NOTIFICATION_SERVICE);
        if(manager==null)throw new IllegalStateException("NotificationManager missing");
        NotificationChannel c=new NotificationChannel(CHANNEL,
            "YRC Gaming Session",NotificationManager.IMPORTANCE_LOW);
        c.setDescription("جلسة لتجربة وضع الشاشة 120Hz مع إمكانية الاستعادة");
        manager.createNotificationChannel(c);
        PendingIntent stop=PendingIntent.getService(this,102,
            new Intent(this,SessionService.class).setAction(ACTION_STOP),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        PendingIntent open=PendingIntent.getActivity(this,103,
            new Intent(this,MainActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification notice=new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("YRC 6.0 — جلسة الشاشة فعالة")
            .setContentText("120Hz مطلوب من Android؛ هذا لا يعني 120FPS")
            .setContentIntent(open)
            .setCategory(Notification.CATEGORY_SERVICE)
            .setOngoing(true)
            .addAction(android.R.drawable.ic_menu_revert,
                "إنهاء الجلسة واستعادة الإعدادات",stop).build();
        if(Build.VERSION.SDK_INT>=34)
            startForeground(NOTICE,notice,ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(NOTICE,notice);
    }

    private void watch(){
        long start=SystemClock.elapsedRealtime();
        boolean gameSeen=false;
        int consecutiveStops=0;
        int highHzSamples=0;
        int lowHzSamples=0;
        int middleHzSamples=0;
        while(!interrupted&&SessionEngine.active(this)){
            try{Thread.sleep(10000);}catch(InterruptedException e){break;}
            if(interrupted)break;
            // Don't infer a game shutdown from root permission errors.
            Boolean running=SessionEngine.pubgIsRunning();
            if(Boolean.TRUE.equals(running)){
                gameSeen=true;consecutiveStops=0;
                // Read-only Android display sampling WHILE the PUBG process runs.
                // Does not read PUBG FPS or modify PUBG's renderer.
                float modeHz=currentPanelRefresh();
                if(modeHz>=110f)highHzSamples++;
                else if(modeHz>=45f&&modeHz<75f)lowHzSamples++;
                else if(modeHz>0f)middleHzSamples++;
                SessionEngine.prefs(this).edit()
                    .putInt("v6_samples_110plus",highHzSamples)
                    .putInt("v6_samples_60",lowHzSamples)
                    .putInt("v6_samples_other",middleHzSamples)
                    .apply();
            }else if(Boolean.FALSE.equals(running)){
                if(gameSeen){
                    consecutiveStops++;
                    if(consecutiveStops>=3){
                        finish("تم تأكيد إغلاق PUBG؛ استعادة إعدادات الشاشة");
                        return;
                    }
                }else if(SystemClock.elapsedRealtime()-start>100000){
                    finish("لم تعمل PUBG خلال 100 ثانية؛ استعادة الإعدادات");
                    return;
                }
            }else consecutiveStops=0;

            int temp=SessionEngine.batteryTempTenths(this);
            int thermal=SessionEngine.thermal(this);
            if(temp>=440||thermal>=android.os.PowerManager.THERMAL_STATUS_SEVERE){
                finish("حماية حرارية: أوقفنا طلب 120Hz واستعدنا الإعدادات.");
                return;
            }
            if(SystemClock.elapsedRealtime()-start>=6*60*60*1000L){
                finish("انتهت مهلة الجلسة القصوى (6 ساعات).");
                return;
            }
        }
    }

    private float currentPanelRefresh(){
        try{
            android.hardware.display.DisplayManager dm=
                (android.hardware.display.DisplayManager)
                    getSystemService(Context.DISPLAY_SERVICE);
            android.view.Display d=dm==null?null:
                dm.getDisplay(android.view.Display.DEFAULT_DISPLAY);
            return d==null?-1f:d.getRefreshRate();
        }catch(Exception ignored){return -1f;}
    }

    private void finish(String reason){
        if(!closing.compareAndSet(false,true))return;
        interrupted=true;
        new Thread(()->{
            try{
                List<String> report=SessionEngine.restore(getApplicationContext());
                int a=SessionEngine.prefs(this).getInt("v6_samples_110plus",0);
                int b=SessionEngine.prefs(this).getInt("v6_samples_60",0);
                int c=SessionEngine.prefs(this).getInt("v6_samples_other",0);
                String displaySample="عينات وضع الشاشة أثناء تشغيل PUBG (كل 10 ثوان تقريبًا): "+
                    "110Hz فأعلى="+a+" | حول 60Hz="+b+" | أخرى="+c+
                    "\\nهذه قراءة تحديث الشاشة وليست FPS اللعبة أو قياسًا لثبات الرندر.";
                String full=reason+"\\n"+displaySample+"\\n"+android.text.TextUtils.join("\\n",report);
                SessionEngine.prefs(this).edit().putString(SessionEngine.REPORT,full).commit();
            }catch(Exception e){
                SessionEngine.prefs(this).edit().putString(SessionEngine.REPORT,
                    reason+"\nلم تكتمل الاستعادة: "+e.getMessage()).commit();
            }finally{
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
        },"YRC-v6-recovery").start();
    }

    @Override public void onDestroy(){
        interrupted=true;
        if(watcher!=null)watcher.interrupt();
        super.onDestroy();
    }
}
