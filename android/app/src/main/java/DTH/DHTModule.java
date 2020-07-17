package DTH;

import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

import org.libtorrent4j.*;
import org.libtorrent4j.alerts.*;
import org.libtorrent4j.swig.session;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import javax.annotation.Nonnull;

public  class DHTModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private SessionManager sessionManager;
    private final static String TAG = "DHT_MODULE" ;

    public DHTModule(@Nonnull ReactApplicationContext reactContext ) {
        super(reactContext);
        this.reactContext = reactContext;
        SessionManager sessionManager = new SessionManager();

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


    public void startSession() {
        sessionManager.start();
        sessionManager.startDht();
        sessionManager.addListener(mainListener);
    }

        


    @NonNull
    @Override
    public String getName() {
        return "DHTModule";
    }
}
