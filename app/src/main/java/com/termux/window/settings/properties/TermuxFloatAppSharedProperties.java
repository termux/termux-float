package com.termux.window.settings.properties;

import android.content.Context;

import androidx.annotation.NonNull;

import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;
import com.termux.shared.termux.settings.properties.TermuxSharedProperties;

public class TermuxFloatAppSharedProperties extends TermuxSharedProperties {

    private static final String LOG_TAG = "TermuxFloatAppSharedProperties";

    public TermuxFloatAppSharedProperties(@NonNull Context context) {
        super(context, TermuxConstants.TERMUX_FLOAT_APP_NAME, TermuxPropertyConstants.getTermuxFloatPropertiesFile(),
            TermuxPropertyConstants.TERMUX_PROPERTIES_LIST, new SharedPropertiesParserClient());
    }

    /**
     * Load the {@link TermuxPropertyConstants#KEY_TERMINAL_TRANSCRIPT_ROWS} value from termux properties file on disk.
     */
    public static int getTerminalTranscriptRows(Context context) {
        return  (int) TermuxSharedProperties.getInternalPropertyValue(context, TermuxPropertyConstants.getTermuxFloatPropertiesFile(),
            TermuxPropertyConstants.KEY_TERMINAL_TRANSCRIPT_ROWS, new SharedPropertiesParserClient());
    }

}
