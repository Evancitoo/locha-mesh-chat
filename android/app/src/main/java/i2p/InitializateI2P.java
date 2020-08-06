package i2p;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;

import com.lochameshchat.R;

import net.i2p.data.DataHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Properties;

import DeviceInfo.Utils;
import i2p.Util.Util;

public class InitializateI2P {

    private final Context context;
    private final String myDir;
    private static final String TAG =  "InizialitateI2P";
    private static final String CONFIG_FILE = "android.config";
    private static final String PROP_INSTALLED_VERSION = "i2p.version";
    private final String _ourVersion;

    public InitializateI2P(Context ctx){
        context = ctx;
        Log.i(TAG, "directory path"+ Utils.getFileDir(ctx));
        myDir = Utils.getFileDir(ctx);
        _ourVersion = Utils.getOurVersion(ctx);
    }


    public void init (){
        if(checkNewVersion()){
            List<Properties> lProps = Util.getPropertiesFromPreferences(context);
            Properties props = lProps.get(0);

            props.setProperty("i2p.dir.temp", myDir + "/tmp");
            props.setProperty("i2p.dir.pid", myDir + "/tmp");


            mergeResourceToFile(R.raw.router_config, "router.config", props);
            mergeResourceToFile(R.raw.logger_config, "logger.config", lProps.get(1));

            copyResourceToFileIfAbsent(R.raw.i2ptunnel_config, "i2ptunnel.config");

            mergeResourceToFile(R.raw.more_hosts_txt, "hosts.txt", null);


            File abDir = new File(myDir, "addressbook");
            abDir.mkdir();
            copyResourceToFile(R.raw.subscriptions_txt, "addressbook/subscriptions.txt");
            mergeResourceToFile(R.raw.addressbook_config_txt, "addressbook/config.txt", null);

        }

        System.setProperty("i2p.dir.base", myDir);
        System.setProperty("i2p.dir.config", myDir);
        System.setProperty("wrapper.logfile", myDir + "/wrapper.log");
    }


    /**
     *  @param f relative to base dir
     */
    private void copyResourceToFileIfAbsent(int resID, String f) {
        File file = new File(myDir, f);
        if (!file.exists())
            copyResourceToFile(resID, f);
    }


    /**
     *  @param f relative to base dir
     */
    private void copyResourceToFile(int resID, String f) {
        InputStream in = null;
        FileOutputStream out = null;

        Log.d(TAG,"Creating file " + f + " from resource");
        byte buf[] = new byte[4096];
        try {
            // Context methods
            in = context.getResources().openRawResource(resID);
            out = new FileOutputStream(new File(myDir, f));

            int read;
            while ( (read = in.read(buf)) != -1)
                out.write(buf, 0, read);

        } catch (IOException ioe) {
        } catch (Resources.NotFoundException nfe) {
        } finally {
            if (in != null) try { in.close(); } catch (IOException ioe) {}
            if (out != null) try { out.close(); } catch (IOException ioe) {}
        }
    }

    /**
     *  Load defaults from resource,
     *  then add props from settings,
     *  and write back.
     *
     *  @param f relative to base dir
     *  @param overrides local overrides or null
     */
    private void mergeResourceToFile(int resID, String f, Properties overrides) {
         Util.mergeResourceToFile(context, myDir, f, resID, overrides, null);
    }

    private boolean checkNewVersion() {
        Properties props = new Properties();

        InputStream fin = null;
        try {
            fin = context.openFileInput(CONFIG_FILE);
            DataHelper.loadProps(props,  fin);
        } catch (IOException ioe) {
            Log.d(TAG,"Looks like a new install");
        } finally {
            if (fin != null) try { fin.close(); } catch (IOException ioe) {}
        }

        String oldVersion = props.getProperty(PROP_INSTALLED_VERSION);
        boolean newInstall = oldVersion == null;
        boolean newVersion = !_ourVersion.equals(oldVersion);

        if (newVersion) {
            Log.d(TAG,"New version " + _ourVersion);
            props.setProperty(PROP_INSTALLED_VERSION, _ourVersion);
            try {
                DataHelper.storeProps(props, context.getFileStreamPath(CONFIG_FILE));
            } catch (IOException ioe) {
                Log.e(TAG,"Failed to write " + CONFIG_FILE);
            }
        }
        return newVersion;
    }



}
