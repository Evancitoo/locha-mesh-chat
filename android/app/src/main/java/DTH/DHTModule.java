package DTH;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import org.libtorrent4j.*;
import org.libtorrent4j.alerts.*;
import org.libtorrent4j.swig.session;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import DeviceInfo.Utils;

public  class DHTModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
//    private SessionManager sessionManager;
    private final static String TAG = "DHT_MODULE" ;

    private final String magnet = "magnet:?xt=urn:btih:ab803d390cfdee02097a6464f135cbd59eb28794&dn=The.Mentalist.S03E19.1080p.HEVC.x265-MeGusta%5Beztv.io%5D.mkv%5Beztv%5D&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A80&tr=udp%3A%2F%2Fglotorrents.pw%3A6969%2Fannounce&tr=udp%3A%2F%2Ftracker.opentrackr.org%3A1337%2Fannounce&tr=udp%3A%2F%2Fexodus.desync.com%3A6969";
    public DHTModule(@Nonnull ReactApplicationContext reactContext ) {
        super(reactContext);
        this.reactContext = reactContext;
//        sessionManager = new SessionManager();

    }

    AlertListener mainListener = new AlertListener() {
        @Override
        public int[] types() {
            return null;
        }
        @Override
        public void alert(Alert<?> alert) {
            AlertType type = alert.type();
            if (type == AlertType.LISTEN_SUCCEEDED) {
                ListenSucceededAlert a = (ListenSucceededAlert) alert;
                Log.i(TAG,"AlertType.LISTEN_SUCCEEDED"+a.message());
            }
            if (type == AlertType.LISTEN_FAILED) {
                ListenFailedAlert a = (ListenFailedAlert) alert;
                Log.i(TAG, "AlertType.LISTEN_FAILED"+ a.message());
            }
            if (type == AlertType.DHT_PUT) {
                DhtPutAlert a = (DhtPutAlert) alert;
                Log.i(TAG , "AlertType.DHT_PUT" + a.message());
            }
        }
    };

    @ReactMethod
    public void startSession() {
        Thread SesionThread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    SessionManager sessionManager = new SessionManager();
                    final CountDownLatch signal = new CountDownLatch(1);

                    SettingsPack settingsPack = new SettingsPack();
                    settingsPack.listenInterfaces("[0.0.0.0]:6881");
                    settingsPack.enableDht(true);

                    SessionParams params = new SessionParams(settingsPack);


                    if (sessionManager.isRunning() != true) {
                        sessionManager.addListener(mainListener);
                        sessionManager.start(params);
                    }
                    waitForNodesInDHT(sessionManager);

                    Log.i(TAG, "is running:  " + sessionManager.isDhtRunning());
                    byte[] data = sessionManager.fetchMagnet(magnet, 10);
                    if (data != null) {
                        System.out.println(Entry.bdecode(data));
                        Sha1Hash dios =  new Sha1Hash("976b9c53254817ff2cb466a38d1f9d15528795a8");
                        ArrayList<TcpEndpoint>  peers =   sessionManager.dhtGetPeers(dios, 10);
                        Log.i(TAG,"persss" + peers.toString());
                    } else {
                        System.out.println("Failed to retrieve the magnet");
                    }
                } catch(InterruptedException e){
                    Log.i(TAG, "execute this Exception" + e.toString());
                }
            }

        });

        SesionThread.run();
    }


    private static void get_peers(SessionManager sm, String s) {
        String sha1 = s.split(" ")[1];
        Log.i(TAG,"Waiting a max of 20 seconds to get peers for key: " + new Sha1Hash(sha1) );
       // ArrayList<TcpEndpoint> peers = sm.dhtGetPeers(new Sha1Hash(sha1), 10);
       // Log.i(TAG, peers.toString());
    }


    private static void waitForNodesInDHT(final SessionManager s) throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long nodes = s.stats().dhtNodes();
                if (nodes >= 1) {
                    System.out.println("DHT contains " + nodes + " nodes");
                    signal.countDown();
//                    timer.cancel();
                }
            }
        }, 0, 1000);

        System.out.println("Waiting for nodes in DHT (10 seconds)...");
        boolean r = signal.await(10, TimeUnit.SECONDS);
        if (!r) {
            System.out.println("DHT bootstrap timeout");
        }
    }


    @NonNull
    @Override
    public String getName() {
        return "DHTModule";
    }
}
