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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.lochameshchat.R;

import net.i2p.data.DataHelper;
import net.i2p.router.Job;
import net.i2p.router.Router;
import net.i2p.router.RouterContext;

import java.util.Timer;
import java.util.TimerTask;

import i2p.I2pModule;
import i2p.Util.Notifications;
import i2p.Util.Util;

public class RouterService extends Service {

    /**
     * A request to this service for the current router state. Broadcasting
     * this will trigger a state notification.
     */
    public static final String LOCAL_BROADCAST_REQUEST_STATE = "net.i2p.android.LOCAL_BROADCAST_REQUEST_STATE";
    /**
     * A notification of the current state. This is informational; the state
     * has not changed.
     */
    public static final String LOCAL_BROADCAST_STATE_NOTIFICATION = "net.i2p.android.LOCAL_BROADCAST_STATE_NOTIFICATION";
    /**
     * The state has just changed.
     */
    public static final String LOCAL_BROADCAST_STATE_CHANGED = "net.i2p.android.LOCAL_BROADCAST_STATE_CHANGED";
    public static final String LOCAL_BROADCAST_EXTRA_STATE = "net.i2p.android.STATE";
    /**
     * The locale has just changed.
     */
    public static final String LOCAL_BROADCAST_LOCALE_CHANGED = "net.i2p.android.LOCAL_BROADCAST_LOCALE_CHANGED";


    private final static  String TAG = "RouterService";
    private final Object _stateLock = new Object();
    private static I2pModule mModuleManager;
    private CountDownTimer mServiceTimer = null;
    private CountDownTimer mMissingConnectionTimer = null;
    private ConnectivityManager connectivity;
    private static final String EXTRA_RESTART = "restar";
    private Thread starterThread;
    private RouterContext routerContext;
    private Notifications _notif;
    private Handler handler;
    private Updater updater;
    private int status = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        _notif = new Notifications(this);
        handler = new Handler();
        updater = new Updater();
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

        Log.d(TAG , " onStart called"
                + " Intent is: " + intent
                + " Flags is: " + flags
                + " ID is: " + startId);

        boolean restart = intent != null && intent.getBooleanExtra(EXTRA_RESTART, false);

        Log.d(TAG, "onStartCommand: " + restart);

        Init init = new Init(this);
        init.initialize();

        synchronized(_stateLock) {
            starterThread = new Thread( new Starter());
            starterThread.start();
        }

        handler.removeCallbacks(updater);
        handler.postDelayed(updater, 50);
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
            Log.i(TAG, "router is running  "+ r.isRunning() ) ;
            synchronized (_stateLock){
                routerContext = r.getContext();
                if(routerContext == null){
                    throw new IllegalStateException("Router has no context?");
                }

                status = 1;
                routerContext.router().setKillVMOnEnd(false);
                Job loadJob = new LoadClientsJob(RouterService.this, routerContext, _notif);
                routerContext.jobQueue().addJob(loadJob);
                starterThread = null;
            }
            Log.d(TAG ," running router");
            updateStatus(routerContext);
        }
    }


    private class Updater implements Runnable {

        public void run() {
            RouterContext ctx = routerContext;
            Log.i(TAG, "status"+ status );
            if(status != 0){
                Router router = ctx.router();
                if(router.isAlive()) {
                    updateStatus(ctx);
                }
                handler.postDelayed(this, 15 * 1000);
            }
        }
    }



    private void updateStatus(RouterContext ctx) {


        int active = ctx.commSystem().countActivePeers();
        int known = Math.max(ctx.netDb().getKnownRouters() - 1, 0);
        int inEx = ctx.tunnelManager().getFreeTunnelCount();
        int outEx = ctx.tunnelManager().getOutboundTunnelCount();
        int inCl = ctx.tunnelManager().getInboundClientTunnelCount();
        int outCl = ctx.tunnelManager().getOutboundClientTunnelCount();
        String uptime = DataHelper.formatDuration(ctx.router().getUptime());
        double inBW = ctx.bandwidthLimiter().getReceiveBps();
        double outBW = ctx.bandwidthLimiter().getSendBps();

        String text =
                getResources().getString(R.string.notification_status_text,
                        Util.formatSpeed(inBW), Util.formatSpeed(outBW));

        String bigText =
                getResources().getString(R.string.notification_status_bw,
                        Util.formatSpeed(inBW), Util.formatSpeed(outBW)) + '\n'
                        + getResources().getString(R.string.notification_status_peers,
                        active, known) + '\n'
                        + getResources().getString(R.string.notification_status_expl,
                        inEx, outEx) + '\n'
                        + getResources().getString(R.string.notification_status_client,
                        inCl, outCl);


        Timer timer = new Timer();
        timer.schedule(new TimerTask() {

            @Override
            public void run() {
                Log.i(TAG, "text one: " + text);
                Log.i(TAG, "text two: " + bigText);
            }
        }, 10000, 10000);


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
