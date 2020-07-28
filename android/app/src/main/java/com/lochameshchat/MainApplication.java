package com.lochameshchat;


import android.app.Application;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.ReactPackage;
import com.facebook.soloader.SoLoader;
import com.reactnativecommunity.rnpermissions.RNPermissionsPackage;
import android.content.Context;
import com.facebook.react.PackageList;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import DeviceInfo.DeviceInfoPackage;
import LocalNotification.LocalNotificationPackage;
import RNCoapClient.CoapClientPackage;
import RNWebSocket.RNWebsocketPakage;
import WiFiModule.WifiPackage;
import i2p.I2pPackage;
import upd.RNUdpServerPackage;


public class MainApplication extends Application implements ReactApplication {

  private final ReactNativeHost mReactNativeHost =
      new ReactNativeHost(this) {
        @Override
        public boolean getUseDeveloperSupport() {
          return BuildConfig.DEBUG;
        }


      @Override
        protected List<ReactPackage> getPackages() {
          @SuppressWarnings("UnnecessaryLocalVariable")
          List<ReactPackage> packages = new PackageList(this).getPackages();
          // Packages that cannot be autolinked yet can be added manually here, for example:
           packages.add(new RNPermissionsPackage());
           packages.add(new DeviceInfoPackage());
           packages.add(new RNWebsocketPakage());
           packages.add(new CoapClientPackage());
           packages.add(new RNUdpServerPackage());
           packages.add(new LocalNotificationPackage());
           packages.add(new WifiPackage());
           packages.add(new I2pPackage());
          return packages;
        }

    @Override
    protected String getJSMainModuleName() {
      return "index";
    }
  };

  @Override
  public ReactNativeHost getReactNativeHost() {
    return mReactNativeHost;
  }

  @Override
  public void onCreate() {
    super.onCreate();
    SoLoader.init(this, /* native exopackage */ false);
    initializeLocha(this, getReactNativeHost().getReactInstanceManager());
 
  }


  /**
   * Loads Flipper in React Native templates. Call this in the onCreate method with something like
   * initializeFlipper(this, getReactNativeHost().getReactInstanceManager());
   *
   * @param context
   * @param reactInstanceManager
   */
  private static void initializeLocha(
      Context context, ReactInstanceManager reactInstanceManager) {
    if (BuildConfig.DEBUG) {
      try {
        /*
         We use reflection here to pick up the class that initializes Flipper,
        since Flipper library is not available in release mode
        */
        Class<?> aClass = Class.forName("com.rndiffapp.ReactNativeFlipper");
        aClass
            .getMethod("initializeLocha", Context.class, ReactInstanceManager.class)
            .invoke(null, context, reactInstanceManager);
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }
  }
}
