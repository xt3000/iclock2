package net.finch.clock2;

import android.app.Service;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.util.Log;

public class timeService extends Service {
    final String TAG = "TAG_finch";
    public static BroadcastReceiver br;
    private static boolean brReg = false;
    public  final static String ACTION_SCR = "android.intent.action.SCREEN_ON";
    public  final static String ACTION_TIME = "android.intent.action.TIME_TICK";
    
    public timeService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
//        throw new UnsupportedOperationException("Not yet implemented");
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand: ");
        taskService();

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy: ");
        if (brReg){
            Log.d(TAG, "onDestroy: unREG");
            this.unregisterReceiver(br);
            brReg = false;
        }
        Log.d(TAG, "onDestroy: upd()");
        updWidget();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: ");
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: start");

                updWidget();

                Log.d(TAG, "onReceive: end");
            }
        };
    }

    void taskService(){
        try{
            this.unregisterReceiver(br);
            Log.d(TAG, "unregister: OK");
        } catch (Exception e) {
            Log.d(TAG, "unregister: ERROR ");
        }
        try{
            IntentFilter filter = new IntentFilter();
            filter.addAction(ACTION_SCR);
            filter.addAction(ACTION_TIME);
            this.registerReceiver(br, filter);
            Log.d(TAG, "register: OK");
        } catch (Exception e) {
            Log.d(TAG, "register: ERROR");
        }


//        new Thread(new Runnable() {
//            public void run() {
//
//            }
//        }).start();
    }

    void updWidget(){
        Log.d(TAG, "updWidget: ");
        
        AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        ComponentName widgetComponent = new ComponentName(this, iCLockWidget2.class);
        int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
        Intent update = new Intent();
        update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
        update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        this.sendBroadcast(update);
    }
}
