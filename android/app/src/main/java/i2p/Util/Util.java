package i2p.Util;

import net.i2p.router.RouterContext;

import java.text.DecimalFormat;
import java.util.List;

public class Util {


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
}
