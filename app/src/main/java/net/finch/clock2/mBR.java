package net.finch.clock2;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.util.Log;

public class mBR extends Application {
    private static String TAG = "FINCH_BR";
    public static BroadcastReceiver br;
    private static Context context;

    public  final static String ACTION_SCR = "android.intent.action.SCREEN_ON";
    public  final static String ACTION_TIME = "android.intent.action.TIME_TICK";

    public void onCreate() {
        super.onCreate();
        mBR.context = getApplicationContext();
    }

    public static Context getAppContext() {
        return mBR.context;
    }

    public static void register(){
//        updTemp();
        
        br = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive: start");
                
                AppWidgetManager widgetManager = AppWidgetManager.getInstance(mBR.getAppContext());
                ComponentName widgetComponent = new ComponentName(mBR.getAppContext(), iCLockWidget2.class);
                int[] widgetIds = widgetManager.getAppWidgetIds(widgetComponent);
                Intent update = new Intent();
                update.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIds);
                update.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
                mBR.getAppContext().sendBroadcast(update);

//                updTemp();

                Log.d(TAG, "onReceive: end");
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_SCR);
//        filter.addAction(ACTION_TIME);

        Log.d(TAG, "register: ");
        try{
            mBR.getAppContext().registerReceiver(br, filter);
            Log.d(TAG, "BRregister: OK");
        }catch (Exception e){
            Log.d(TAG, "BRregister: ERROR");
        }
    }

    public static void unregister(){
        Log.d(TAG, "unregister: ");
        try{
            if(br != null) mBR.getAppContext().unregisterReceiver(br);
            Log.d(TAG, "BRunregister: OK");
        }catch (Exception e){
            Log.d(TAG, "BRunregister: ERROR");
        }
    }

    private static void updTemp() {
        httpTask ht = new httpTask();
        ht.execute();
    }
}
