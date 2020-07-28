package i2p;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class RouterService extends Service {


    private final static  String TAG = "RouterService";

    private static I2pModule mModuleManager;
    private CountDownTimer mServiceTimer = null;
    private CountDownTimer mMissingConnectionTimer = null;
    private ConnectivityManager connectivity;

    @Override
    public void onCreate() {
        super.onCreate();

        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);

            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setContentTitle("Service")
                    .setContentText("is service").build();

            startForeground(1, notification);
        }
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        connectivity = (ConnectivityManager) getBaseContext()
                .getSystemService(Context.CONNECTIVITY_SERVICE);

        Log.i(TAG, "Executed here");
        // set check timer onCreate service that check status for ten seconds each one second
        // and restart if finish
        mServiceTimer = new CountDownTimer(10000, 1000) {
            public void onTick(long millisUntilFinished) {
                checkConnectionStatus();
            };
            public void onFinish() {
                mServiceTimer.start();
            }
        };
        mServiceTimer.start();

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        mServiceTimer.cancel();
        if (mModuleManager != null) {
            mModuleManager.removeServiceReference();
        }
        super.onDestroy();
    }


    // Let me get the module manager reference to pass information
    public static void setUpdateListener(I2pModule moduleManager) {
        mModuleManager = moduleManager;
    }

    public void checkConnectionStatus(){
        // verify connection status and set
        boolean mConnected = false;
        String mName = null;
        if (connectivity != null && connectivity.getActiveNetworkInfo() != null) {
            NetworkInfo activeNetwork = connectivity.getActiveNetworkInfo();
            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE){
                mName = activeNetwork.getTypeName();
                mConnected = true;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                mName = activeNetwork.getTypeName();
                mConnected = true;
            };
            stopMissingConnectionTimer();
        } else {
            startMissingConnectionTimer();
        }
        if (mModuleManager != null) {
            mModuleManager.setModuleParams(mConnected, mName);
        }
    }

    private void startMissingConnectionTimer() {
        if (mMissingConnectionTimer == null) {
            mMissingConnectionTimer = new CountDownTimer(10000, 10000) {
                public void onTick(long millisUntilFinished) {
                };
                public void onFinish() {
                    Toast.makeText(getBaseContext(), "Llevas 10 segundos sin internet, ¿no sientes comezón?", Toast.LENGTH_LONG).show();
//                    stopSelf();
                }
            };
            mMissingConnectionTimer.start();
        }

    }

    private void stopMissingConnectionTimer() {
        if (mMissingConnectionTimer != null) {
            mMissingConnectionTimer.cancel();
            mMissingConnectionTimer = null;
        }
    }

}
