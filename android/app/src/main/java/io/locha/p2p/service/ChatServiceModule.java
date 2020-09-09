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

package io.locha.p2p.service;

import io.locha.p2p.util.LibraryLoader;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;

import DeviceInfo.Utils;

import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.Condition;

/**
 * React Native interface to ChatService foreground service.
 */
public class ChatServiceModule extends ReactContextBaseJavaModule {
    static {
        LibraryLoader.load();
    }

    public static final String ENOTCONNECTED = "ENOTCONNECTED";
    public static final String ENOTSTARTED = "ENOTSTARTED";

    private static final String TAG = "LochaP2P";

    public ReactApplicationContext reactContext;

    private IChatService service = null;
    private Intent serviceIntent = null;
    private String peerId = null;
    private boolean isServiceStarted = false;

    private Lock startLock;
    private Condition startDone;

    private Lock stopLock;
    private Condition stopDone;

    private Lock connLock;
    private Condition connDone;

    public ChatServiceModule(ReactApplicationContext reactContext) {
        super(reactContext);

        this.reactContext = reactContext;

        /* The EventsDispatcher class is responsible for sending events from the Chat
         * Service to the React Native JS context, it's a singleto that is shared between
         * our class and ChatService which in turns passes it to Runtime as the events handler
         */
        EventsDispatcher dispatcher = EventsDispatcher.getInstance();
        dispatcher.setApplicationContext(this.reactContext);

        IntentFilter filter = new IntentFilter();
        filter.addAction(ChatService.SERVICE_STARTED);
        filter.addAction(ChatService.SERVICE_NOT_STARTED);
        filter.addAction(ChatService.CLICK_FOREGROUND_NOTIFICATION);
        filter.addAction(ChatService.SERVICE_STOPPED);

        this.reactContext.registerReceiver(broadcastReceiver, filter);

        this.startLock = new ReentrantLock();
        this.startDone = this.startLock.newCondition();

        this.stopLock = new ReentrantLock();
        this.stopDone = this.stopLock.newCondition();

        this.connLock = new ReentrantLock();
        this.connDone = this.connLock.newCondition();
    }

    /**
     * @return Module name.
     */
    @Override
    @NonNull
    public String getName() {
        return "ChatService";
    }

    /**
     * Initialize service for maintaining the app alive in the background
     *
     * Android Version >=8.0 used ForegroundService
     *
     * @param privateKey secp256k1 private key in hex, must be exactly 64
     * characters (32-bytes).
     * @param attemptUpnp Whether to enable UPnP and attempt to use it to discover
     * our external IP address and do port mapping.
     */
    @ReactMethod public void start(String privateKey, boolean attemptUpnp, Promise promise) {
        if (!Utils.isConnected(this.reactContext)) {
            promise.reject(ENOTCONNECTED, "The device is not connected");
            promise = null;
            return;
        }

        byte[] privateKeyBytes = Utils.hexStringToByteArray(privateKey);

        this.serviceIntent = new Intent(this.reactContext, ChatService.class);
        this.serviceIntent.putExtra("privateKey", privateKeyBytes);
        this.serviceIntent.putExtra("attemptUpnp", attemptUpnp);

        this.startLock.lock();
        try {
            // Guard for Android >=8.0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Starting foreground service");
                this.reactContext.startForegroundService(this.serviceIntent);
            } else {
                Log.d(TAG, "Starting normal service");
                this.reactContext.startService(this.serviceIntent);
            }

            this.startDone.await();
        } catch (Exception e) {
            promise.reject(ENOTSTARTED, e);
        } finally {
            this.startLock.unlock();
        }

        if (!isServiceStarted) {
            promise.reject(ENOTSTARTED, "Couldn't start Chat Service");
            return;
        }

        Log.d(TAG, "Binding service");
        this.connLock.lock();
        try {
            this.reactContext.bindService(this.serviceIntent, connection, Context.BIND_AUTO_CREATE);
            this.connDone.await();
        } catch (Exception e) {
            promise.reject(ENOTSTARTED, "Couldn't bind Chat Service");
        } finally {
            this.connLock.unlock();
        }

        Log.d(TAG, "Caching PeerId");
        try {
            this.peerId = this.service.getPeerId();
            Log.d(TAG, String.format("Our PeerId is %s", this.peerId));
            promise.resolve(this.peerId);
        } catch (Exception e) {
            promise.reject(ENOTSTARTED, "Couldn't get PeerId");
        }
    }

    /**
     * Stop the service
     */
    @ReactMethod public void stop(Promise promise) {
        this.stopLock.lock();
        try {
            this.reactContext.stopService(serviceIntent);
            this.stopDone.await();
            promise.resolve(null);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't stop service", e);
            promise.reject(ENOTCONNECTED, e);
        } finally {
            this.stopLock.unlock();
        }
    }

    /**
     * Get our PeerId
     *
     * @param promise Promise that will be resolved when the PeerId is returned.
     */
    @ReactMethod public void getPeerId(Promise promise) {
        if (peerId == null) {
            promise.reject(ENOTSTARTED, "peerId is null");
            return;
        }

        promise.resolve(peerId);
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
    @ReactMethod public void dial(String multiaddr, Promise promise) {
        try {
            this.service.dial(multiaddr);
            promise.resolve(null);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't dial address", e);
            promise.reject(ENOTSTARTED, e);
        }
    }

    /**
     * Send a message
     *
     * @param contents The message contents.
     */
    @ReactMethod public void sendMessage(String contents) {
        try {
            this.service.sendMessage(contents);
        } catch (Exception e) {
            Log.e(TAG, "Couldn't send message", e);
        }
    }

    /**
     * Is service started?
     *
     * @param promise Promise that will resolve to the "is started" value.
     */
    @ReactMethod public void isStarted(Promise promise) {
        promise.resolve(isServiceStarted);
    }

    private void cleanup() {
        this.serviceIntent = null;
        this.peerId = null;
        this.isServiceStarted = false;
    }

    /**
     * BroadcastReceiver is where we will be listening to all the events returned
     * by ChatService
     */
    private BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            Log.d(TAG, String.format("broadcastReceiver, action=\"%s\"", intent.getAction()));

            String action = intent.getAction();
            assert action != null;

            if (action.equals(ChatService.SERVICE_STARTED)) {
                startLock.lock();
                try {
                    isServiceStarted = true;
                    startDone.signal();
                } finally {
                    startLock.unlock();
                }
                return;
            }

            if (action.equals(ChatService.SERVICE_NOT_STARTED)) {
                startLock.lock();
                try {
                    isServiceStarted = false;
                    startDone.signal();
                } finally {
                    startLock.unlock();
                }
                cleanup();
                return;
            }

            if (action.equals(ChatService.CLICK_FOREGROUND_NOTIFICATION)) {
                try {
                    Uri uri = Uri.fromParts("package", reactContext.getPackageName(), null);

                    Intent appDetails = new Intent();
                    appDetails.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    appDetails.setData(uri);
                    appDetails.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    reactContext.getApplicationContext().startActivity(appDetails);
                } catch (Exception e) {
                    Log.e(TAG, "Couldn't start application details settings", e);
                }
                return;
            }

            if (action.equals(ChatService.SERVICE_STOPPED)) {
                Log.d(TAG, "ChatService successfully stopped");
                cleanup();
                stopLock.lock();
                try {
                    isServiceStarted = false;
                    stopDone.signal();
                } finally {
                    stopLock.unlock();
                }
            }
        }
    };

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder boundService) {
            Log.d(TAG, "Service connected");
            service = IChatService.Stub.asInterface(boundService);
            connLock.lock();
            try {
                connDone.signal();
            } finally {
                connLock.unlock();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG, "Service disconnected");
            service = null;
        }
    };
}
