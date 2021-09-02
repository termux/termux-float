package com.termux.window;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Vibrator;

import com.termux.shared.terminal.TermuxTerminalSessionClientBase;
import com.termux.terminal.TerminalSession;

public class TermuxFloatSessionClient extends TermuxTerminalSessionClientBase {

    private final TermuxFloatService mService;

    private static final String LOG_TAG = "TermuxFloatSessionClient";

    public TermuxFloatSessionClient(TermuxFloatService service) {
        this.mService = service;
    }

    @Override
    public void onTextChanged(TerminalSession changedSession) {
        TermuxFloatView floatingWindow = mService.getFloatingWindow();
        if (floatingWindow != null)
            floatingWindow.mTerminalView.onScreenUpdated();
    }

    @Override
    public void onSessionFinished(TerminalSession finishedSession) {
        mService.requestStopService();
    }

    @Override
    public void onCopyTextToClipboard(TerminalSession pastingSession, String text) {
        ClipboardManager clipboard = (ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(new ClipData(null, new String[]{"text/plain"}, new ClipData.Item(text)));
    }

    @Override
    public void onBell(TerminalSession riningSession) {
        ((Vibrator) mService.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(50);
    }

}
