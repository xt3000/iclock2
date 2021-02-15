package net.finch.clock2;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.icu.text.RelativeDateTimeFormatter;
import android.provider.AlarmClock;
import android.util.Log;
import android.widget.RemoteViews;

import androidx.lifecycle.Observer;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Implementation of App Widget functionality.
 */

public class iCLockWidget2 extends AppWidgetProvider {

    public static Context mContext;
    public static AppWidgetManager mAWM;
    public static int[] mIDs;
    static String TAG = "FINCH_Widget";

    public static AlarmManager am;
    public static PeriodicWorkRequest mWR;

    private httpTask ht;

    private static String time = "";
    private static String act = "noACT";

    public  final static String ACTION_SCR = "android.intent.action.SCREEN_ON";
    public  final static String ACTION_TIME = "android.intent.action.TIME_TICK";


    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int id, String temp, String pgd) {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.i_clock_widget2);
        views.setTextViewText(R.id.tvTime, getTime());
        views.setTextViewText(R.id.tvData, getDate());
        views.setTextViewText(R.id.tvWeak, getWeak());
        views.setTextViewText(R.id.tvTemp, temp);
        views.setImageViewResource(R.id.iv_pogoda, prognoz(pgd));

//        Intent updIntent = new Intent(context, iCLockWidget2.class);
//        updIntent.setAction(appWidgetManager.ACTION_APPWIDGET_UPDATE);
//        updIntent.putExtra(appWidgetManager.EXTRA_APPWIDGET_IDS, new int[]{id});

        Intent updIntent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        updIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pIntent = PendingIntent.getActivity(context, id, updIntent, 0);
        views.setOnClickPendingIntent(R.id.rlClock, pIntent);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(id, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] IDs) {
        super.onUpdate(context, appWidgetManager, IDs);
        Log.d(TAG, "onUpdate: ");

//        context.stopService(new Intent(context, timeService.class));

        mContext = context;
        mAWM = appWidgetManager;
        ComponentName widgetComponent = new ComponentName(context, iCLockWidget2.class);
        mIDs = mAWM.getAppWidgetIds(widgetComponent);

        am = (AlarmManager) context.getSystemService(context.ALARM_SERVICE);

//        ht = new httpTask();
//        ht.execute();

        SharedPreferences sp = context.getSharedPreferences(httpWorker.PREF_WIDGET, Context.MODE_PRIVATE);
        String temp = sp.getString(httpWorker.PREF_KEY_TEMP, "");
        String pgd = sp.getString(httpWorker.PREF_KEY_POGODA, "");
        if(temp.equals("")) {
            ht = new httpTask();
            ht.execute();
        }

        for (int id : mIDs) {
            Log.d(TAG, "onUpdate: id" + id);
            updateAppWidget(context, appWidgetManager, id, temp, pgd);
        }

        Intent amIntent = new Intent(context, iCLockWidget2.class);
        amIntent.setAction(appWidgetManager.ACTION_APPWIDGET_UPDATE);
        amIntent.putExtra(appWidgetManager.EXTRA_APPWIDGET_IDS, mIDs);
        PendingIntent amPIntent = PendingIntent.getBroadcast(context, 0, amIntent, 0);

        am.cancel(amPIntent);
        am.setExact(AlarmManager.RTC_WAKEUP, nextAlarmTime(), amPIntent);



    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
        Log.d(TAG, "onEnabled: ");

        super.onEnabled(context);
        mBR.register();

        Constraints c = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();
        mWR = new PeriodicWorkRequest.Builder(httpWorker.class, 30, TimeUnit.MINUTES, 20, TimeUnit.MINUTES).setConstraints(c).build();
        WorkManager.getInstance(context).enqueue(mWR);

        WorkManager.getInstance(context).getWorkInfoByIdLiveData(mWR.getId()).observeForever(new Observer<WorkInfo>() {
            @Override
            public void onChanged(WorkInfo workInfo) {
                Log.d(TAG, "onChanged: " + workInfo.getState());

            }
        });


    }

    @Override
    public void onDisabled(Context context) {
        Log.d(TAG, "onDisabled: ");
        Intent amIntent = new Intent(context, iCLockWidget2.class);
        amIntent.setAction(mAWM.ACTION_APPWIDGET_UPDATE);
        amIntent.putExtra(mAWM.EXTRA_APPWIDGET_IDS, mIDs);
        PendingIntent amPIntent = PendingIntent.getBroadcast(context, 0, amIntent, 0);
        am.cancel(amPIntent);

        mBR.unregister();

        WorkManager.getInstance(context).cancelAllWork();

        SharedPreferences.Editor editor = context.getSharedPreferences(
                httpWorker.PREF_WIDGET, Context.MODE_PRIVATE).edit();
        editor.remove(httpWorker.PREF_KEY_TEMP).apply();
    }



    public static String getTime() {
        Date d = Calendar.getInstance().getTime();
        return DateFormat.getTimeInstance(DateFormat.SHORT).format(d);
    }

    public static String getDate() {
        Date d = Calendar.getInstance().getTime();
        return DateFormat.getDateInstance(DateFormat.LONG).format(d);
    }

    public static String getWeak() {
        Date d = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("EEEE");
        return df.format(d);
    }

    public static int getHour() {
        Date d = Calendar.getInstance().getTime();
        SimpleDateFormat df = new SimpleDateFormat("H");
        return Integer.parseInt(df.format(d));
    }


    public static long nextAlarmTime(){
        Log.d(TAG, "nextAlarmTime: ");
        
        Date d = Calendar.getInstance().getTime();
        String sec = new SimpleDateFormat("s").format(d);
        int s = Integer.valueOf(sec);
        String msec = new SimpleDateFormat("SSS").format(d);
        int ms = Integer.valueOf(msec);
        ms += s*1000;

        return d.getTime()+60000-ms;
    }


    private static int prognoz(String pgd) {
        int h = getHour();
        switch (pgd) {
            case "s":
                if(h>20 || h<8) return R.drawable.ic_moon;
                return R.drawable.ic_sun;
            case "c":
                return R.drawable.ic_cloudy;
            case "cr":
                return R.drawable.ic_cloudy_rain;
            case "csw":
                return R.drawable.ic_cloudy_snow;
            case "sr":
                if(h>20 || h<8) return R.drawable.ic_moon_rain;
                return R.drawable.ic_sun_rain;
            case "ssw":
                if(h>20 || h<8) return R.drawable.ic_moon_snow;
                return R.drawable.ic_sun_snow;
            default:
                if(h>20 || h<8) return R.drawable.ic_moon_cloudy;
                return R.drawable.ic_sun_cloudy;
        }
    }
}

