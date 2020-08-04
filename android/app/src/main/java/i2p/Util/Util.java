package i2p.Util;

import net.i2p.router.RouterContext;

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
}
