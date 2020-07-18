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
    private SessionManager sessionManager;
    private final static String TAG = "DHT_MODULE" ;

    final String magnet = "magnet:?xt=urn:btih:737d38ed01da1df727a3e0521a6f2c457cb812de&dn=HOME+-+a+film+by+Yann+Arthus-Bertrand+%282009%29+%5BEnglish%5D+%5BHD+MP4%5D&tr=udp%3A%2F%2Ftracker.leechers-paradise.org%3A6969&tr=udp%3A%2F%2Ftracker.zer0day.to%3A1337&tr=udp%3A%2F%2Ftracker.coppersurfer.tk%3A6969";

    public DHTModule(@Nonnull ReactApplicationContext reactContext ) {
        super(reactContext);
        this.reactContext = reactContext;
        sessionManager = new SessionManager();

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
                Log.i(TAG,a.message());
            }
            if (type == AlertType.LISTEN_FAILED) {
                ListenFailedAlert a = (ListenFailedAlert) alert;
                Log.i(TAG, a.message());
            }
            if (type == AlertType.DHT_PUT) {
                DhtPutAlert a = (DhtPutAlert) alert;
                Log.i(TAG , a.message());
            }
        }
    };

    @ReactMethod
    public void startSession() {

        try {
            SettingsPack settingsPack =  new SettingsPack();
            settingsPack.listenInterfaces("[::1]:43567");

            SessionParams params = new SessionParams(settingsPack);

            if( sessionManager.isRunning() != true){
                sessionManager.addListener(mainListener);
                sessionManager.start();
            }

            waitForNodesInDHT(sessionManager);
            byte[] data = sessionManager.fetchMagnet(magnet, 30);
            TorrentInfo ti = TorrentInfo.bdecode(data);
            int i = 0;
            while (i < 20) {
                TimeUnit.SECONDS.sleep(1);
                Log.i(TAG,sessionManager.find(ti.infoHash()).status().state() + " state");
                Log.i(TAG,sessionManager.find(ti.infoHash()).status().progress() * 100 + " progress");
                i++;
            }
        } catch (InterruptedException e){
            Log.i(TAG, e.toString());
        }

    }


    private static void waitForNodesInDHT(final SessionManager s) throws InterruptedException {
        final CountDownLatch signal = new CountDownLatch(1);

        final Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                long nodes = s.stats().dhtNodes();
                if (nodes >= 10) {
                    System.out.println("DHT contains " + nodes + " nodes");
                    signal.countDown();
                    timer.cancel();
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
