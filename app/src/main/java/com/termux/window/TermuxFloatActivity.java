package com.termux.window;

import android.app.Activity;
import android.content.Intent;

/**
 * Simple activity which immediately launches {@link TermuxFloatService} and exits.
 */
public class TermuxFloatActivity extends Activity {

    @Override
    protected void onResume() {
        super.onResume();

        // Set log level for the app
        TermuxFloatApplication.setLogConfig(this, false);

        startService(new Intent(this, TermuxFloatService.class));
        finish();
    }
}
