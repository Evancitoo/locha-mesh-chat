package i2p;

import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import javax.annotation.Nonnull;

import DeviceInfo.Utils;
import i2p.Router.RouterService;

public class I2pModule extends ReactContextBaseJavaModule {

    ReactApplicationContext context;
    Utils utils;
    private Intent mService;
    private boolean mConnected = false;
    private String mName = null;
    Callback callbackRouter = null;
    public static final String TAG = "I2P_MODULE";


    public I2pModule (@Nonnull ReactApplicationContext reactContext){
        super(reactContext);
        context = reactContext;
        utils = new Utils();

        RouterService.setUpdateListener(this);
    }

    @NonNull
    @Override
    public String getName() {
        return "i2p";
    }


    public void setModuleParams(boolean connected, String name) {
        mConnected = connected;
        mName = name;
    }

    /**
     * remove service reference
     */
    public void removeServiceReference() {
        mService = null;
    }

    /**
     *
     * it's use for the start router service i2p
     *
     */
    @ReactMethod public void startRouter (Callback callback){
        callbackRouter = callback;
       boolean isConnected = utils.isConnected(context);

       if(isConnected){
           mService = new Intent(context, RouterService.class);
           if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
               Log.i(TAG, "startForegroundService");
               context.startForegroundService(mService);
           }else{
               Log.i(TAG, "normal service");
               context.startService(mService);
           }
       }else{
           callbackRouter.invoke("Error no connected", null);
       }

    }

}
