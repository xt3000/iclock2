package net.finch.clock2;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.RemoteViews;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.json.JSONObject;

import java.io.IOException;

import static net.finch.clock2.httpWorker.PREF_KEY_TEMP;
import static net.finch.clock2.httpWorker.PREF_KEY_POGODA;
import static net.finch.clock2.httpWorker.PREF_WIDGET;

public class httpTask extends AsyncTask<Void,Void,String> {

    private static final String TAG = "FINCH_httpTASK:";

    private static final String HOST = "ws://31.25.28.132/ws?type=interface";
    WebSocket ws;
    private volatile String msg = "";
    private volatile String pgd = "";

    @Override
    protected String doInBackground(Void... voids) {
        Log.d(TAG, "doInBackground: ");

        msg = "";
        try
        {
            ws = connect();
        }
        catch (IOException | WebSocketException ignored){}

        Log.d(TAG, "doInBackground: msg = " + msg);
        while (msg == ""){}

        Log.d(TAG, "doInBackground: msg returned");
        ws.disconnect();


        return msg;
    }

    @Override
    protected void onPostExecute(String temp) {
        super.onPostExecute(temp);

        SharedPreferences sp = iCLockWidget2.mContext.getSharedPreferences(PREF_WIDGET, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putString(PREF_KEY_TEMP, msg);
        editor.putString(PREF_KEY_POGODA, pgd);
        editor.apply();


        for (int id : iCLockWidget2.mIDs) {
            Log.d(TAG, "onUpdate: id" + id);
            updTemp(iCLockWidget2.mContext, iCLockWidget2.mAWM, id, temp);
        }
    }

    private WebSocket connect() throws IOException, WebSocketException
    {
        Log.d(TAG, "connect: ");
        return new WebSocketFactory()
                .setConnectionTimeout(1000)
                .createSocket(HOST)
                .addListener(new WebSocketAdapter(){

                    @Override
                    public void onTextMessage(WebSocket websocket, String massage) throws Exception {
                        super.onTextMessage(websocket, massage);

                        Log.d(TAG, "onTextMessage: " + massage);

                        JSONObject jo = new JSONObject(massage);
                        if (jo.has("pogoda")) {
                            pgd = jo.getString("pogoda");
                            Log.d(TAG, "onTextMessage: pogoda = " + pgd);
                        }
                        if (jo.has("temp")) {
                            int t = (int) Math.round(jo.getDouble("temp"));
                            msg = t + "°C";
                            Log.d(TAG, "onTextMessage: temp = " + msg);
                        }

                        ws.disconnect();
                    }

                    @Override
                    public void onDisconnected(WebSocket websocket, WebSocketFrame serverCloseFrame, WebSocketFrame clientCloseFrame, boolean closedByServer) throws Exception {
                        super.onDisconnected(websocket, serverCloseFrame, clientCloseFrame, closedByServer);

                        Log.d(TAG, "onDisconnected: ");
                    }
                })
                .addExtension(WebSocketExtension.PERMESSAGE_DEFLATE)
                .connect();
    }

    private void updTemp(Context context, AppWidgetManager awm, int id, String temp)
    {
        Log.d(TAG, "updTemp:  ");
        
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.i_clock_widget2);
        views.setTextViewText(R.id.tvTemp, temp);

        awm.updateAppWidget(id, views);
    }
}
