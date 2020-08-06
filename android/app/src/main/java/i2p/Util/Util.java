package i2p.Util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.lochameshchat.R;

import net.i2p.data.DataHelper;
import net.i2p.router.RouterContext;
import net.i2p.router.transport.udp.UDPTransport;
import net.i2p.util.OrderedProperties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import i2p.I2PConstants;

public class Util implements I2PConstants {


    /**
     * Get the active RouterContext.
     *
     * @return the active RouterContext, or null
     */
    public static RouterContext getRouterContext() {
        List<RouterContext> contexts = RouterContext.listContexts();
        if (!((contexts == null) || (contexts.isEmpty()))) {
            return contexts.get(0);
        }
        return null;
    }


    public static String formatSize(double size) {
        return formatSize(size, 0);
    }

    public static String formatSpeed(double size) {
        return formatSize(size, 1);
    }

    public static String formatSize(double size, int baseScale) {
        int scale;
        for (int i = 0; i < baseScale; i++) {
            size /= 1024.0D;
        }
        for (scale = baseScale; size >= 1024.0D; size /= 1024.0D) {
            ++scale;
        }

        // control total width
        DecimalFormat fmt;
        if (size >= 1000) {
            fmt = new DecimalFormat("#0");
        } else if (size >= 100) {
            fmt = new DecimalFormat("#0.0");
        } else {
            fmt = new DecimalFormat("#0.00");
        }

        String str = fmt.format(size);
        switch (scale) {
            case 1:
                return str + "K";
            case 2:
                return str + "M";
            case 3:
                return str + "G";
            case 4:
                return str + "T";
            case 5:
                return str + "P";
            case 6:
                return str + "E";
            case 7:
                return str + "Z";
            case 8:
                return str + "Y";
            default:
                return str + "";
        }
    }


    /**
     * Load defaults from resource, then add props from settings, and write back.
     * If resID is 0, defaults are not written over the existing file content.
     *
     * @param dir       the file directory
     * @param file      relative to dir
     * @param resID     the ID of the default resource, or 0
     * @param userProps local properties or null
     * @param toRemove  properties to remove, or null
     */
    public static void mergeResourceToFile(Context ctx, String dir, String file, int resID,
                                           Properties userProps, Collection<String> toRemove) {
        InputStream fin = null;
        InputStream in = null;

        final  String TAG = "mergeResourceToFile";
        try {
            Properties props = new OrderedProperties();
            try {
                fin = new FileInputStream(new File(dir, file));
                DataHelper.loadProps(props, fin);
                if (resID > 0)
                    Log.d(TAG,"Merging resource into file " + file);
                else
                    Log.d(TAG,"Merging properties into file " + file);
            } catch (IOException ioe) {
                if (resID > 0)
                    Log.e(TAG,"Creating file " + file + " from resource");
                else
                    Log.d(TAG,"Creating file " + file + " from properties");
            }

            // write in default settings
            if (resID > 0)
                in = ctx.getResources().openRawResource(resID);
            if (in != null)
                DataHelper.loadProps(props, in);

            // override with user settings
            if (userProps != null)
                props.putAll(userProps);
            if (toRemove != null) {
                for (String key : toRemove) {
                    props.remove(key);
                }
            }

            File path = new File(dir, file);
            DataHelper.storeProps(props, path);
            Log.i("mergeResourceToFile", "Saved " + props.size() + " properties in " + file);
        } catch (IOException ioe) {
        } catch (Resources.NotFoundException nfe) {
        } finally {
            if (in != null) try {
                in.close();
            } catch (IOException ioe) {
            }
            if (fin != null) try {
                fin.close();
            } catch (IOException ioe) {
            }
        }
    }


    /**
     * copied from various private components
     */
    final static String PROP_I2NP_NTCP_PORT = "i2np.ntcp.port";
    final static String PROP_I2NP_NTCP_AUTO_PORT = "i2np.ntcp.autoport";
    public static final String GRAPH_PREFERENCES_SEEN = "graphPreferencesSeen";


    public static List<Properties> getPropertiesFromPreferences(Context context) {
        List<Properties> pList = new ArrayList<>();

        // Copy prefs
        Properties routerProps = new OrderedProperties();

        // List to store stats for graphing
        List<String> statSummaries = new ArrayList<>();

        // Properties to remove
        Properties toRemove = new OrderedProperties();

        // List to store Log settings
        Properties logSettings = new OrderedProperties();

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Map<String, ?> all = preferences.getAll();
        // get values from the Map and make them strings.
        // This loop avoids needing to convert each one, or even know it's type, or if it exists yet.
        for (String x : all.keySet()) {
            if (x.startsWith("stat.summaries.")) {
                String stat = x.substring("stat.summaries.".length());
                String checked = all.get(x).toString();
                if (checked.equals("true")) {
                    statSummaries.add(stat);
                }
            } else if (x.startsWith("logger.")) {
                logSettings.put(x, all.get(x).toString());
            } else if (
                    x.equals("router.hiddenMode") ||
                            x.equals("i2cp.disableInterface")) {
                // special exception, we must invert the bool for these properties only.
                String string = all.get(x).toString();
                String inverted = Boolean.toString(!Boolean.parseBoolean(string));
                routerProps.setProperty(x, inverted);
            } else if (x.equals(context.getString(R.string.PREF_LANGUAGE))) {
                String language[] = TextUtils.split(all.get(x).toString(), "_");

                if (language[0].equals(context.getString(R.string.DEFAULT_LANGUAGE))) {
                    toRemove.setProperty("routerconsole.lang", "");
                    toRemove.setProperty("routerconsole.country", "");
                } else {
                    routerProps.setProperty("routerconsole.lang", language[0].toLowerCase(Locale.US));
                    if (language.length == 2)
                        routerProps.setProperty("routerconsole.country", language[1].toUpperCase(Locale.US));
                    else
                        toRemove.setProperty("routerconsole.country", "");
                }
            } else if (!x.startsWith(ANDROID_PREF_PREFIX)) { // Skip over UI-related I2P Android settings
                String string = all.get(x).toString();
                routerProps.setProperty(x, string);
            }
        }
        if (statSummaries.isEmpty()) {
            // If the graph preferences have not yet been seen, they should be the default
            if (preferences.getBoolean(GRAPH_PREFERENCES_SEEN, false))
                routerProps.setProperty("stat.summaries", "");
            else
                toRemove.setProperty("stat.summaries", "");
        } else {
            Iterator<String> iter = statSummaries.iterator();
            StringBuilder buf = new StringBuilder(iter.next());
            while (iter.hasNext()) {
                buf.append(",").append(iter.next());
            }
            routerProps.setProperty("stat.summaries", buf.toString());
        }

        // See net.i2p.router.web.ConfigNetHandler.saveChanges()
        int udpPort = Integer.parseInt(routerProps.getProperty(UDPTransport.PROP_INTERNAL_PORT, "-1"));
        if (udpPort <= 0)
            routerProps.remove(UDPTransport.PROP_INTERNAL_PORT);
        int ntcpPort = Integer.parseInt(routerProps.getProperty(PROP_I2NP_NTCP_PORT, "-1"));
        boolean ntcpAutoPort = Boolean.parseBoolean(
                routerProps.getProperty(PROP_I2NP_NTCP_AUTO_PORT, "true"));
        if (ntcpPort <= 0 || ntcpAutoPort) {
            routerProps.remove(PROP_I2NP_NTCP_PORT);
            toRemove.setProperty(PROP_I2NP_NTCP_PORT, "");
        }

        pList.add(routerProps);
        pList.add(toRemove);
        pList.add(logSettings);

        return pList;
    }
}
