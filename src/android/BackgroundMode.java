package com.anrip.cordova;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import static android.content.Context.BIND_AUTO_CREATE;

public class BackgroundMode extends CordovaPlugin {

    // Event types for callbacks
    private enum Event {
        ACTIVATE, DEACTIVATE, FAILURE
    }

    // Plugin namespace
    private static final String JS_NAMESPACE = "window.BackgroundService";

    // Flag indicates if the app is in background or foreground
    private boolean inBackground = false;

    // Flag indicates if the plugin is enabled or disabled
    private boolean isDisabled = true;

    // Flag indicates if the service is bind
    private boolean isBind = false;

    // Default settings for the notification
    private static JSONObject defaultSettings = new JSONObject();

    // Service that keeps the app awake
    private ForegroundService service;

    // Used to (un)bind the service to with the activity
    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            ForegroundService.ForegroundBinder binder = (ForegroundService.ForegroundBinder) service;

            BackgroundMode.this.service = binder.getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            fireEvent(Event.FAILURE, "'service disconnected'");
        }
    };

    /**
     * Executes the request.
     *
     * @param action   The action to execute.
     * @param args     The exec() arguments.
     * @param callback The callback context used when calling back into JavaScript.
     *
     * @return Returning false results in a "MethodNotFound" error.
     *
     * @throws JSONException
     */
    @Override
    public boolean execute (String action, JSONArray args, CallbackContext callback) throws JSONException {

        if (action.equalsIgnoreCase("configure")) {
            configure(args.getJSONObject(0), args.getBoolean(1));
            callback.success();
            return true;
        }

        if (action.equalsIgnoreCase("disable")) {
            isDisabled = true;
            stopService();
            callback.success();
            return true;
        }

        if (action.equalsIgnoreCase("enable")) {
            isDisabled = false;
            if (inBackground) {
                startService();
            }
            callback.success();
            return true;
        }

        return false;
    }

    /**
     * Called when the system is about to start resuming a previous activity.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app.
     */
    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        inBackground = true;
        startService();
    }

    /**
     * Called when the activity will start interacting with the user.
     *
     * @param multitasking Flag indicating if multitasking is turned on for app.
     */
    @Override
    public void onResume(boolean multitasking) {
        super.onResume(multitasking);
        inBackground = false;
        stopService();
    }

    /**
     * Called when the activity will be destroyed.
     */
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopService();
    }

    /**
     * Update the default settings and configure the notification.
     *
     * @param settings The settings
     * @param update A truthy value means to update the running service.
     */
    private void configure(JSONObject settings, boolean update) {
        if (update) {
            if (isBind) {
                service.updateNotification(settings);
            }
        } else {
            defaultSettings = settings;
        }
    }

    /**
     * The settings for the new/updated notification.
     *
     * @return updateSettings if set or default settings
     */
    protected static JSONObject getSettings() {
        return defaultSettings;
    }

    /**
     * Bind the activity to a background service and put them into foreground
     * state.
     */
    private void startService() {
        if (isDisabled || isBind)
            return;

        Activity context = cordova.getActivity();
        Intent intent    = new Intent(context, ForegroundService.class);

        try {
            context.bindService(intent, connection, BIND_AUTO_CREATE);
            fireEvent(Event.ACTIVATE, null);
            context.startService(intent);
        } catch (Exception e) {
            fireEvent(Event.FAILURE, String.format("'%s'", e.getMessage()));
        }

        isBind = true;
    }

    /**
     * Bind the activity to a background service and put them into foreground state.
     */
    private void stopService() {
        if (!isBind)
            return;

        Activity context = cordova.getActivity();
        Intent intent    = new Intent(context, ForegroundService.class);

        fireEvent(Event.DEACTIVATE, null);
        context.unbindService(connection);
        context.stopService(intent);

        isBind = false;
    }

    /**
     * Fire vent with some parameters inside the web view.
     *
     * @param event The name of the event
     * @param params Optional arguments for the event
     */
    private void fireEvent (Event event, String params) {
        String status, js;

        switch (event) {
            case ACTIVATE:
                status = "activate"; break;
            case DEACTIVATE:
                status = "deactivate"; break;
            case FAILURE:
                status = "failure"; break;
        }

        js = String.format("%s.fire('%s',%s);", JS_NAMESPACE, status, params);

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl("javascript:" + js);
            }
        });
    }

}
