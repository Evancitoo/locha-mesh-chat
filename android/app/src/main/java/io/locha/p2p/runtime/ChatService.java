/*
 * Copyright 2020 Locha Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.locha.p2p.runtime;

import DeviceInfo.Utils;
import io.locha.p2p.util.LibraryLoader;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.facebook.react.bridge.Promise;
import com.lochameshchat.MainActivity;
import com.lochameshchat.R;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Chat service. This class manages the chat logic, such as starting and
 * stopping the server.
 *
 * <p> TODO: this should not be a singleton, instead modify the Rust code to
 * work without globals and with various servers. That can help with mocking
 * and other things.
 */
public class ChatService  extends Service {
    static {
        LibraryLoader.load();
    }

    private static String TAG = "LochaP2P";
    private static ChatService INSTANCE = null;

    private ChatServiceEvents eventsHandler;
    private String peerId;

    public ChatService() {
        this.eventsHandler = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onCreate() {

        super.onCreate();

        eventsHandler = EventReceivers.get();


        if (Build.VERSION.SDK_INT >= 26) {
            String CHANNEL_ID = "my_channel_01";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    "Channel human readable title",
                    NotificationManager.IMPORTANCE_DEFAULT);

            Intent notificationIntent = new Intent("com.lochameshchat.CLICK_FOREGRAUND_NOTIFICATION");

            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, 0);

            Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                    .setOngoing(true)
                    .setContentTitle("Locha Mesh  is running in the background")
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentIntent(pendingIntent)
                    .setPriority(NotificationManager.IMPORTANCE_MIN)
                    .setCategory(Notification.CATEGORY_SERVICE)
                    .build();

            startForeground(233, notification);
        }


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.i(TAG, "--------------------- onStartCommand ------------------------ ");

        String privateKey  = intent.getStringExtra("privateKey");

        Log.i(TAG, "privateKey" + privateKey);

        byte[] privateKeyBytes = Utils.hexStringToByteArray(privateKey);
        start(privateKeyBytes);

        return START_STICKY;
    }


    /**
     * Start the server
     *
     * @throws RuntimeException if the server is already started.
     */
    public void start(byte[] privateKey) {
        Log.i(TAG, "Starting ChatService");
        if (isStarted()) {
            Log.i(TAG,"isStarted?");
            Intent broadCastIntent = new Intent("com.lochameshchat.SERVICE_NOT_STARTED");
            sendBroadcast(broadCastIntent);
            return;
        }
        try {
            nativeStart(privateKey);
            this.peerId = nativeGetPeerId();

            Intent broadCastIntent = new Intent("com.lochameshchat.SERVICE_IS_STARTED");
            broadCastIntent.putExtra("peerID", peerId);
            sendBroadcast(broadCastIntent);
        }catch (Exception e){
            Log.e(TAG, "into the catch" + e.toString());
            Intent broadCastIntent = new Intent("com.lochameshchat.SERVICE_NOT_STARTED");
            sendBroadcast(broadCastIntent);
        }
    }

    /**
     * @throws RuntimeException if not started.
     * @throws RuntimeException if an error ocurred while stopping ChatService.
     */
    public void stop() {
        nativeStop();
    }

    /**
     * Has the Chat service already been started?
     */
    public boolean isStarted() {
        return nativeIsStarted();
    }

    /**
     * Return the peer ID 
     */
    public String getPeerId() {
        return this.peerId;
    }

    /**
     * Dial (connect to) a peer.
     * 
     * @param multiaddr The peer address in Multiaddress format.
     *
     * @throws RuntimeException if the address is invalid.
     *
     * @see <a href="https://multiformats.io/multiaddr/">Multiaddr</a>
     */
    public void dial(String multiaddr) {
        Log.i(TAG, String.format("Dialing '%s'", multiaddr));
        nativeDial(multiaddr);
    }

    /**
     * Send a message
     *
     * @param contents The message contents.
     */
    public void sendMessage(String contents) {
        Log.i(TAG, String.format("Sending message '%s'", contents));

        nativeSendMessage(contents);

        Log.i(TAG, String.format("Message sent"));
    }

    public native void nativeStart(byte[] privateKey);
    public native void nativeStop();
    public native boolean nativeIsStarted();
    public native String nativeGetPeerId();
    public native void nativeDial(String multiaddr);
    public native void nativeSendMessage(String contents);
}
