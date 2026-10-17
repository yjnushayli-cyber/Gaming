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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Foreground session watchdog: checks only whether PUBG's process is running.
 * On exit, restores the Android settings changed for this user-requested session.
 * Never reads or modifies game data. XOS may still stop this service; the
 * always-available manual restore and startup recovery path remain essential.
 */
public final class SessionService extends Service {
    static final String ACTION_START="com.yrc.gaming.SESSION_START";
    static final String ACTION_STOP="com.yrc.gaming.SESSION_STOP";
    private static final String CHANNEL="yrc_competitive_session";
    private static final int NOTIFICATION_ID=5010;

    private final AtomicBoolean finishing=new AtomicBoolean(false);
    private volatile boolean cancelled=false;
    private Thread watch;

    @Override public IBinder onBind(Intent intent){return null;}

    @Override public int onStartCommand(Intent intent,int flags,int startId){
        String action=intent==null?ACTION_START:intent.getAction();
        if(ACTION_STOP.equals(action)){
            endSession("تم إيقاف الجلسة من الإشعار");
            return START_NOT_STICKY;
        }
        if(!SessionManager.active(this)){
            stopSelf();
            return START_NOT_STICKY;
        }
        try{
            foreground();
        }catch(Exception e){
            // No hidden always-on work if notification registration failed.
            endSession("تعذر بدء خدمة استعادة الإعدادات: "+e.getMessage());
            return START_NOT_STICKY;
        }
        if(watch==null||!watch.isAlive()){
            cancelled=false;
            watch=new Thread(this::watchPubg,"YRC-Session-Watch");
            watch.start();
        }
        return START_STICKY;
    }

    private void foreground(){
        NotificationManager nm=(NotificationManager)
            getSystemService(Context.NOTIFICATION_SERVICE);
        if(nm==null)throw new IllegalStateException("No notification manager");
        NotificationChannel channel=new NotificationChannel(
            CHANNEL,"YRC Competitive Session",NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("استعادة إعدادات الأداء عند انتهاء جلسة PUBG");
        nm.createNotificationChannel(channel);

        Intent stopIntent=new Intent(this,SessionService.class).setAction(ACTION_STOP);
        PendingIntent stop=PendingIntent.getService(this,31,stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Intent openIntent=new Intent(this,MainActivity.class)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent open=PendingIntent.getActivity(this,32,openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);

        Notification notification=new Notification.Builder(this,CHANNEL)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle("YRC Gaming Pro — جلسة اللعب مفعلة")
            .setContentText("سيحاول استعادة الإعدادات بعد إغلاق PUBG")
            .setContentIntent(open)
            .addAction(android.R.drawable.ic_menu_revert,
                "إنهاء الجلسة واستعادة الإعدادات",stop)
            .setOngoing(true)
            .setCategory(Notification.CATEGORY_SERVICE)
            .build();
        if(Build.VERSION.SDK_INT>=34)
            startForeground(NOTIFICATION_ID,notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(NOTIFICATION_ID,notification);
    }

    private void watchPubg(){
        long start=android.os.SystemClock.elapsedRealtime();
        boolean seenGame=false;
        int successiveMisses=0;
        while(!cancelled&&SessionManager.active(this)){
            try{Thread.sleep(10000);}catch(InterruptedException e){break;}
            if(cancelled)break;
            boolean game=SessionManager.pubgRunning();
            if(game){
                seenGame=true;successiveMisses=0;
            }else if(seenGame){
                successiveMisses++;
                // Avoid restoring on a brief game-process transition.
                if(successiveMisses>=3){
                    endSession("انتهى تشغيل PUBG؛ أُعيدت إعدادات الجلسة.");
                    break;
                }
            }else if(android.os.SystemClock.elapsedRealtime()-start>90000){
                endSession("لم تبدأ PUBG خلال 90 ثانية؛ أُعيدت الإعدادات.");
                break;
            }
            // Safety deadline: no forgotten high-performance settings for days.
            if(android.os.SystemClock.elapsedRealtime()-start>21600000L){
                endSession("انتهت المهلة القصوى للجلسة (6 ساعات).");
                break;
            }
        }
    }

    private void endSession(String reason){
        if(!finishing.compareAndSet(false,true))return;
        cancelled=true;
        new Thread(()->{
            try{
                List<String> result=SessionManager.restore(getApplicationContext());
                String report=reason+"\n"+android.text.TextUtils.join("\n",result);
                SessionManager.prefs(this).edit()
                    .putString(SessionManager.SESSION_REPORT,report).commit();
            }catch(Exception e){
                SessionManager.prefs(this).edit()
                    .putString(SessionManager.SESSION_REPORT,
                        "لم تكتمل استعادة الجلسة: "+e.getMessage()).commit();
            }finally{
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf();
            }
        },"YRC-Session-Restore").start();
    }

    @Override public void onDestroy(){
        cancelled=true;
        if(watch!=null)watch.interrupt();
        super.onDestroy();
    }
}
