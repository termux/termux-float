package com.termux.window.settings.properties;

import android.content.Context;

import androidx.annotation.NonNull;

import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;
import com.termux.shared.termux.settings.properties.TermuxSharedProperties;

public class TermuxFloatAppSharedProperties extends TermuxSharedProperties {

    private static final String LOG_TAG = "TermuxFloatAppSharedProperties";

    public TermuxFloatAppSharedProperties(@NonNull Context context) {
        super(context, TermuxConstants.TERMUX_FLOAT_APP_NAME,
                TermuxConstants.TERMUX_FLOAT_PROPERTIES_FILE_PATHS_LIST,
                TermuxPropertyConstants.TERMUX_APP_PROPERTIES_LIST,
                new SharedPropertiesParserClient());
    }

}
