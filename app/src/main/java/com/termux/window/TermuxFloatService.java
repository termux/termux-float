package com.termux.window;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;

import com.termux.shared.logger.Logger;
import com.termux.shared.models.ExecutionCommand;
import com.termux.shared.notification.NotificationUtils;
import com.termux.shared.shell.TermuxSession;
import com.termux.shared.shell.TermuxShellEnvironmentClient;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.TermuxConstants.TERMUX_FLOAT_APP.TERMUX_FLOAT_SERVICE;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;

public class TermuxFloatService extends Service {

    /**
     *  The {@link TerminalSessionClient} interface implementation to allow for communication between
     *  {@link TerminalSession} and {@link TermuxFloatService}.
     */
    TermuxFloatSessionClient mTermuxFloatSessionClient;

    private TermuxFloatView mFloatingWindow;

    private boolean mVisibleWindow = true;

    private static final String LOG_TAG = "TermuxFloatService";

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        runStartForeground();
        TermuxFloatApplication.setLogLevel(this);
        Logger.logVerbose(LOG_TAG, "onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Logger.logDebug(LOG_TAG, "onStartCommand");

        // Run again in case service is already started and onCreate() is not called
        runStartForeground();

        if (!initializeFloatView())
            return Service.START_NOT_STICKY;

        String action = intent.getAction();

        if (action != null) {
            switch (action) {
                case TERMUX_FLOAT_SERVICE.ACTION_STOP_SERVICE:
                    Logger.logDebug(LOG_TAG, "ACTION_STOP_SERVICE intent received");
                    requestStopService();
                    break;
                case TERMUX_FLOAT_SERVICE.ACTION_SHOW:
                    Logger.logDebug(LOG_TAG, "ACTION_SHOW intent received");
                    setVisible(true);
                    break;
                case TERMUX_FLOAT_SERVICE.ACTION_HIDE:
                    Logger.logDebug(LOG_TAG, "ACTION_HIDE intent received");
                    setVisible(false);
                    break;
                default:
                    Logger.logError(LOG_TAG, "Invalid action: \"" + action + "\"");
                    break;
            }
        } else if (!mVisibleWindow) {
            // Show window if hidden when launched through launcher icon.
            setVisible(true);
        }

        return Service.START_NOT_STICKY;

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Logger.logVerbose(LOG_TAG, "onDestroy");

        if (mFloatingWindow != null)
            mFloatingWindow.closeFloatingWindow();

        runStopForeground();
    }
    /** Request to stop service. */
    public void requestStopService() {
        Logger.logDebug(LOG_TAG, "Requesting to stop service");
        runStopForeground();
        stopSelf();
    }

    /** Make service run in foreground mode. */
    private void runStartForeground() {
        setupNotificationChannel();
        startForeground(TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_ID, buildNotification());
    }

    /** Make service leave foreground mode. */
    private void runStopForeground() {
        stopForeground(true);
    }



    private void setupNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationUtils.setupNotificationChannel(this, TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_ID,
                TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_LOW);
    }

    private Notification buildNotification() {
        final Resources res = getResources();

        final String notificationText = res.getString(mVisibleWindow ? R.string.notification_message_visible : R.string.notification_message_hidden);

        final String intentAction = mVisibleWindow ? TERMUX_FLOAT_SERVICE.ACTION_HIDE : TERMUX_FLOAT_SERVICE.ACTION_SHOW;
        Intent notificationIntent = new Intent(this, TermuxFloatService.class).setAction(intentAction);
        PendingIntent contentIntent = PendingIntent.getService(this, 0, notificationIntent, 0);

        // Build the notification
        Notification.Builder builder =  NotificationUtils.geNotificationBuilder(this,
                TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_CHANNEL_ID, Notification.PRIORITY_MIN,
                TermuxConstants.TERMUX_FLOAT_APP_NAME, notificationText, null,
                contentIntent, null, NotificationUtils.NOTIFICATION_MODE_SILENT);
        if (builder == null)  return null;

        // No need to show a timestamp:
        builder.setShowWhen(false);

        // Set notification icon
        builder.setSmallIcon(R.mipmap.ic_service_notification);

        // Set background color for small notification icon
        builder.setColor(0xFF000000);

        // TermuxSessions are always ongoing
        builder.setOngoing(true);

        return builder.build();
    }



    @SuppressLint("InflateParams")
    private boolean initializeFloatView() {
        mTermuxFloatSessionClient = new TermuxFloatSessionClient(this);

        boolean floatWindowWasNull = false;
        if (mFloatingWindow == null) {
            mFloatingWindow = (TermuxFloatView) ((LayoutInflater)
                    getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.activity_main, null);
            floatWindowWasNull = true;
        }

        mFloatingWindow.initFloatView();

        TermuxSession session = createTermuxSession(
                new ExecutionCommand(0, null, null, null, null, false, false), null);
        if (session == null)
            return false;
        mFloatingWindow.mTerminalView.attachSession(session.getTerminalSession());

        try {
            mFloatingWindow.launchFloatingWindow();
        } catch (Exception e) {
            Logger.logStackTrace(LOG_TAG, e);
            // Settings.canDrawOverlays() does not work (always returns false, perhaps due to sharedUserId?).
            // So instead we catch the exception and prompt here.
            startActivity(new Intent(this, TermuxFloatPermissionActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            requestStopService();
            return false;
        }

        if (floatWindowWasNull)
            Logger.showToast(this, getString(R.string.initial_instruction_toast), true);

        return true;
    }

    private void setVisible(boolean newVisibility) {
        mVisibleWindow = newVisibility;
        mFloatingWindow.setVisibility(newVisibility ? View.VISIBLE : View.GONE);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(TermuxConstants.TERMUX_FLOAT_APP_NOTIFICATION_ID, buildNotification());
    }



    /** Create a {@link TermuxSession}. */
    @Nullable
    public synchronized TermuxSession createTermuxSession(ExecutionCommand executionCommand, String sessionName) {
        if (executionCommand == null) return null;

        Logger.logDebug(LOG_TAG, "Creating \"" + executionCommand.getCommandIdAndLabelLogString() + "\" TermuxSession");

        if (executionCommand.inBackground) {
            Logger.logDebug(LOG_TAG, "Ignoring a background execution command passed to createTermuxSession()");
            return null;
        }

        if (Logger.getLogLevel() >= Logger.LOG_LEVEL_VERBOSE)
            Logger.logVerboseExtended(LOG_TAG, executionCommand.toString());

        // If the execution command was started for a plugin, only then will the stdout be set
        // Otherwise if command was manually started by the user like by adding a new terminal session,
        // then no need to set stdout
        TermuxSession newTermuxSession = TermuxSession.execute(this, executionCommand,
                mTermuxFloatSessionClient, null, new TermuxShellEnvironmentClient(),
                sessionName, executionCommand.isPluginExecutionCommand);
        if (newTermuxSession == null) {
            Logger.logError(LOG_TAG, "Failed to execute new TermuxSession command for:\n" + executionCommand.getCommandIdAndLabelLogString());
            return null;
        }

        return newTermuxSession;
    }



    public TermuxFloatView getFloatingWindow() {
        return mFloatingWindow;
    }

}
