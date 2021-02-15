package net.finch.clock2;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.ListenableWorker;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.neovisionaries.ws.client.WebSocket;
import com.neovisionaries.ws.client.WebSocketAdapter;
import com.neovisionaries.ws.client.WebSocketException;
import com.neovisionaries.ws.client.WebSocketExtension;
import com.neovisionaries.ws.client.WebSocketFactory;
import com.neovisionaries.ws.client.WebSocketFrame;

import org.json.JSONObject;

import java.io.IOException;

public class httpWorker extends Worker {
    String TAG = "FINCH_WORKER";

    public final static String PREF_WIDGET = "widget_pref";
    public final static String PREF_KEY_TEMP = "temp";
    public final static String PREF_KEY_DATE = "date";
    public final static String PREF_KEY_POGODA = "pogoda";

    private static final String HOST = "ws://31.25.28.132/ws?type=interface";
    WebSocket ws;
    private volatile String msg = "";
    private volatile String pgd = "";

    public httpWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        Log.d(TAG, "doWork: ");

        msg = "";
        try
        {
            ws = connect();
        }
        catch (IOException e){return Result.failure();}
        catch (WebSocketException e){return Result.failure();}

        Log.d(TAG, "doInBackground: msg = " + msg);
//        while (msg == ""){}

        Log.d(TAG, "doInBackground: msg returned");
//        ws.disconnect();

//        Data output = new Data.Builder()
//                .putString("temp", msg)
//                .build();


        return Result.success();
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

                        SharedPreferences sp = getApplicationContext().getSharedPreferences(PREF_WIDGET, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = sp.edit();

                        JSONObject jo = new JSONObject(massage);
                        if (jo.has("temp")) {
                            int t = (int) Math.round(jo.getDouble("temp"));
                            msg = t + "°C";
                            Log.d(TAG, "onTextMessage: msg = " + msg);
                            editor.putString(PREF_KEY_TEMP, msg);
                        }
                        if (jo.has("pogoda")) {
                            pgd = jo.getString("pogoda");
                            Log.d(TAG, "onTextMessage: pogoda = " + pgd);
                            editor.putString(PREF_KEY_POGODA, pgd);
                        }

                        editor.apply();
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
}
