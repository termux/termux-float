package com.termux.window;

import android.app.Application;
import android.content.Context;

import com.termux.shared.crash.TermuxCrashUtils;
import com.termux.shared.logger.Logger;
import com.termux.shared.settings.preferences.TermuxFloatAppSharedPreferences;

public class TermuxFloatApplication extends Application {

    public void onCreate() {
        super.onCreate();

        // Set crash handler for the app
        TermuxCrashUtils.setCrashHandler(this);

        // Set log level for the app
        setLogLevel(getApplicationContext(), true);

        Logger.logDebug("Starting Application");
    }

    public static void setLogLevel(Context context, boolean commitToFile) {
        // Load the log level from shared preferences and set it to the {@link Logger.CURRENT_LOG_LEVEL}
        TermuxFloatAppSharedPreferences preferences = TermuxFloatAppSharedPreferences.build(context);
        if (preferences == null) return;
        preferences.setLogLevel(null, preferences.getLogLevel(true), commitToFile);
    }

}
