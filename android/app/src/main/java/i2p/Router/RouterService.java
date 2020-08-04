package i2p.Router;

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

import com.lochameshchat.R;

import net.i2p.router.Router;
import net.i2p.router.RouterContext;

import i2p.I2pModule;

public class RouterService extends Service {


    private final static  String TAG = "RouterService";
    private final Object _stateLock = new Object();
    private static I2pModule mModuleManager;
    private CountDownTimer mServiceTimer = null;
    private CountDownTimer mMissingConnectionTimer = null;
    private ConnectivityManager connectivity;
    private static final String EXTRA_RESTART = "restar";
    private Thread starterThread;
    private RouterContext routerContext;

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
                    .setContentTitle("Locha Mesh  is running in the background")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();

            startForeground(233, notification);
        }
    }



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        boolean restart = intent != null && intent.getBooleanExtra(EXTRA_RESTART, false);

        Log.d(TAG, "onStartCommand: " + restart);

        Init init = new Init(this);
        init.initialize();

        synchronized(_stateLock) {
            starterThread = new Thread( new Starter());
            starterThread.start();
        }
        return START_STICKY;
    }


    private class Starter implements Runnable {

        public void run() {
            Log.d(TAG ," starter thread");
            //Util.d(MARKER + this + " JBigI speed test started");
            //NativeBigInteger.main(null);
            //Util.d(MARKER + this + " JBigI speed test finished, launching router");

            // Launch the router!
            // TODO Store this somewhere instead of relying on global context?
            Router r = new Router();
            r.setUPnPScannerCallback(new SSDPLocker(RouterService.this));
            r.runRouter();

            synchronized (_stateLock){
                routerContext = r.getContext();
                if(routerContext == null){
                    throw new IllegalStateException("Router has no context?");
                }

                routerContext.router().setKillVMOnEnd(false);


            }
            Log.d(TAG ," running router");

        }
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

//    public void checkConnectionStatus(){
//        // verify connection status and set
//        boolean mConnected = false;
//        String mName = null;
//        if (connectivity != null && connectivity.getActiveNetworkInfo() != null) {
//            NetworkInfo activeNetwork = connectivity.getActiveNetworkInfo();
//            if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE){
//                mName = activeNetwork.getTypeName();
//                mConnected = true;
//            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
//                mName = activeNetwork.getTypeName();
//                mConnected = true;
//            };
//            stopMissingConnectionTimer();
//        } else {
//            startMissingConnectionTimer();
//        }
//        if (mModuleManager != null) {
//            mModuleManager.setModuleParams(mConnected, mName);
//        }
//    }

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
