package i2p.Util;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;

import com.lochameshchat.R;

public class Notifications {
    private final Context mCtx;
    private final NotificationManager mNotificationManager;


    public Notifications(Context ctx) {
        mCtx = ctx;
        mNotificationManager = (NotificationManager) ctx.getSystemService(
                Context.NOTIFICATION_SERVICE);
    }

    public void notify(String title, String text) {
        notify(title, text, null);
    }

    public void notify(String title, String text, Class<?> c) {
        NotificationCompat.Builder b =
                new NotificationCompat.Builder(mCtx)
                        .setContentTitle(title)
                        .setContentText(text)
                        .setAutoCancel(true);

        if (c != null) {
            Intent intent = new Intent(mCtx, c);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PendingIntent pi = PendingIntent.getActivity(mCtx, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
            b.setContentIntent(pi);
        }

        mNotificationManager.notify(7175, b.build());
    }
}

