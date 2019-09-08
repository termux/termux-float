package com.termux.window;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import androidx.annotation.RequiresApi;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.termux.terminal.TerminalSession;

import java.io.File;

public class TermuxFloatService extends Service {

    private static final String NOTIFICATION_CHANNEL_ID = "termux_notification_channel";

    public static final String ACTION_HIDE = "com.termux.float.hide";
    public static final String ACTION_SHOW = "com.termux.float.show";

    /**
     * Note that this is a symlink on the Android M preview.
     */
    @SuppressLint("SdCardPath")
    public static final String FILES_PATH = "/data/data/com.termux/files";
    public static final String PREFIX_PATH = FILES_PATH + "/usr";
    public static final String HOME_PATH = FILES_PATH + "/home";

    /**
     * The notification id supplied to {@link #startForeground(int, Notification)}.
     * <p/>
     * Note that the javadoc for that method says it cannot be zero.
     */
    private static final int NOTIFICATION_ID = 0xdead1337;

    private int MIN_FONTSIZE;
    private static final int MAX_FONTSIZE = 256;
    private static final String FONTSIZE_KEY = "fontsize";
    private TermuxFloatView mFloatingWindow;
    private int mFontSize;

    private boolean mVisibleWindow = true;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * If value is not in the range [min, max], set it to either min or max.
     */
    static int clamp(int value, int min, int max) {
        return Math.min(Math.max(value, min), max);
    }

    @SuppressLint({"InflateParams"})
    @Override
    public void onCreate() {
        super.onCreate();

        float dipInPixels = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 1, getApplicationContext().getResources().getDisplayMetrics());

        // This is a bit arbitrary and sub-optimal. We want to give a sensible default for minimum font size
        // to prevent invisible text due to zoom be mistake:
        MIN_FONTSIZE = (int) (4f * dipInPixels);

        // http://www.google.com/design/spec/style/typography.html#typography-line-height
        int defaultFontSize = Math.round(12 * dipInPixels);
        // Make it divisible by 2 since that is the minimal adjustment step:
        if (defaultFontSize % 2 == 1) defaultFontSize--;

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try {
            mFontSize = Integer.parseInt(prefs.getString(FONTSIZE_KEY, Integer.toString(defaultFontSize)));
        } catch (NumberFormatException | ClassCastException e) {
            mFontSize = defaultFontSize;
        }

        mFontSize = clamp(mFontSize, MIN_FONTSIZE, MAX_FONTSIZE);

        TermuxFloatView floatingWindow = (TermuxFloatView) ((LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.activity_main, null);
        floatingWindow.initializeFloatingWindow();
        floatingWindow.mTerminalView.setTextSize(mFontSize);

        TerminalSession session = createTermSession();
        floatingWindow.mTerminalView.attachSession(session);

        try {
            floatingWindow.launchFloatingWindow();
        } catch (Exception e) {
            // Settings.canDrawOverlays() does not work (always returns false, perhaps due to sharedUserId?).
            // So instead we catch the exception and prompt here.
            startActivity(new Intent(this, TermuxFloatPermissionActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            stopSelf();
            return;
        }

        mFloatingWindow = floatingWindow;

        Toast toast = Toast.makeText(this, R.string.initial_instruction_toast, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        TextView v = toast.getView().findViewById(android.R.id.message);
        if (v != null) v.setGravity(Gravity.CENTER);
        toast.show();

        startForeground(NOTIFICATION_ID, buildNotification());
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setupNotificationChannel() {
        String channelName = "Termux";
        String channelDescription = "Notifications from Termux";
        int importance = NotificationManager.IMPORTANCE_LOW;

        NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, channelName, importance);
        channel.setDescription(channelDescription);
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        manager.createNotificationChannel(channel);
    }

    private Notification buildNotification() {
        final Resources res = getResources();
        final String contentTitle = res.getString(R.string.notification_title);
        final String contentText = res.getString(mVisibleWindow ? R.string.notification_message_visible : R.string.notification_message_hidden);

        final String intentAction = mVisibleWindow ? ACTION_HIDE : ACTION_SHOW;
        Intent actionIntent = new Intent(this, TermuxFloatService.class).setAction(intentAction);

        Notification.Builder builder = new Notification.Builder(this).setContentTitle(contentTitle).setContentText(contentText)
            .setPriority(Notification.PRIORITY_MIN).setSmallIcon(R.mipmap.ic_service_notification)
            .setColor(0xFF000000)
            .setContentIntent(PendingIntent.getService(this, 0, actionIntent, 0))
            .setOngoing(true)
            .setShowWhen(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            setupNotificationChannel();
            builder.setChannelId(NOTIFICATION_CHANNEL_ID);
        }

        //final int messageId = mVisibleWindow ? R.string.toggle_hide : R.string.toggle_show;
        //builder.addAction(android.R.drawable.ic_menu_preferences, res.getString(messageId), PendingIntent.getService(this, 0, actionIntent, 0));
        return builder.build();
    }


    @SuppressLint("Wakelock")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        if (ACTION_HIDE.equals(action)) {
            setVisible(false);
        } else if (ACTION_SHOW.equals(action)) {
            setVisible(true);
        } else if (!mVisibleWindow) {
            // Show window if hidden when launched through launcher icon.
            setVisible(true);
        }
        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mFloatingWindow != null) mFloatingWindow.closeFloatingWindow();
    }

    private void setVisible(boolean newVisibility) {
        mVisibleWindow = newVisibility;
        mFloatingWindow.setVisibility(newVisibility ? View.VISIBLE : View.GONE);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID, buildNotification());
    }

    public void changeFontSize(boolean increase) {
        mFontSize += (increase ? 1 : -1) * 2;
        mFontSize = Math.max(MIN_FONTSIZE, mFontSize);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        prefs.edit().putString(FONTSIZE_KEY, Integer.toString(mFontSize)).apply();

        mFloatingWindow.mTerminalView.setTextSize(mFontSize);
    }

    // XXX: Keep in sync with TermuxService.java.
    @SuppressLint("SdCardPath")
    TerminalSession createTermSession() {
        new File(HOME_PATH).mkdirs();

        final String termEnv = "TERM=xterm-256color";
        final String homeEnv = "HOME=" + TermuxFloatService.HOME_PATH;
        final String prefixEnv = "PREFIX=" + TermuxFloatService.PREFIX_PATH;
        final String androidRootEnv = "ANDROID_ROOT=" + System.getenv("ANDROID_ROOT");
        final String androidDataEnv = "ANDROID_DATA=" + System.getenv("ANDROID_DATA");
        // EXTERNAL_STORAGE is needed for /system/bin/am to work on at least
        // Samsung S7 - see https://plus.google.com/110070148244138185604/posts/gp8Lk3aCGp3.
        final String externalStorageEnv = "EXTERNAL_STORAGE=" + System.getenv("EXTERNAL_STORAGE");
        final String ps1Env = "PS1=$ ";
        final String ldEnv = "LD_LIBRARY_PATH=" + TermuxFloatService.PREFIX_PATH + "/lib";
        final String langEnv = "LANG=en_US.UTF-8";
        final String pathEnv = "PATH=" + TermuxFloatService.PREFIX_PATH + "/bin:" + TermuxFloatService.PREFIX_PATH + "/bin/applets";
        String[] env = new String[]{termEnv, homeEnv, prefixEnv, ps1Env, ldEnv, langEnv, pathEnv, androidRootEnv, androidDataEnv, externalStorageEnv};

        String executablePath = null;
        String[] args;
        String shellName = null;

        for (String shellBinary : new String[]{"login", "bash", "zsh"}) {
            File shellFile = new File(PREFIX_PATH + "/bin/" + shellBinary);
            if (shellFile.canExecute()) {
                executablePath = shellFile.getAbsolutePath();
                shellName = "-" + shellBinary;
                break;
            }
        }

        if (executablePath == null) {
            // Fall back to system shell as last resort:
            executablePath = "/system/bin/sh";
            shellName = "-sh";
        }

        args = new String[]{shellName};

        return new TerminalSession(executablePath, HOME_PATH, args, env, new TerminalSession.SessionChangedCallback() {
            @Override
            public void onTitleChanged(TerminalSession changedSession) {
                // Ignore for now.
            }

            @Override
            public void onTextChanged(TerminalSession changedSession) {
                mFloatingWindow.mTerminalView.onScreenUpdated();
            }

            @Override
            public void onSessionFinished(TerminalSession finishedSession) {
                stopSelf();
            }

            @Override
            public void onClipboardText(TerminalSession pastingSession, String text) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(new ClipData(null, new String[]{"text/plain"}, new ClipData.Item(text)));
            }

            @Override
            public void onBell(TerminalSession riningSession) {
                ((Vibrator) getSystemService(Context.VIBRATOR_SERVICE)).vibrate(50);
            }

            @Override
            public void onColorsChanged(TerminalSession session) {
            }
        });
    }

}
