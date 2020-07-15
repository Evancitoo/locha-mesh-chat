package DTH;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

import org.libtorrent4j.*;
import org.libtorrent4j.alerts.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

import javax.annotation.Nonnull;

public  class DHTModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;

    public DHTModule(@Nonnull ReactApplicationContext reactContext ) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @NonNull
    @Override
    public String getName() {
        return "DHTModule";
    }
}
