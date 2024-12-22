package com.termux.window;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.text.TextUtils;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.settings.properties.TermuxPropertyConstants;
import com.termux.shared.termux.terminal.TermuxTerminalSessionClientBase;
import com.termux.shared.termux.terminal.io.BellHandler;
import com.termux.terminal.TerminalColors;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TextStyle;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class TermuxFloatSessionClient extends TermuxTerminalSessionClientBase {

    private final TermuxFloatService mService;
    private final TermuxFloatView mView;

    private SoundPool mBellSoundPool;

    private int mBellSoundId;

    private static final String LOG_TAG = "TermuxFloatSessionClient";

    public TermuxFloatSessionClient(TermuxFloatService service, TermuxFloatView view) {
        mService = service;
        mView = view;
    }

    /**
     * Should be called when TermuxFloatView.onAttachedToWindow() is called
     */
    public void onAttachedToWindow() {
        // Just initialize the mBellSoundPool and load the sound, otherwise bell might not run
        // the first time bell key is pressed and play() is called, since sound may not be loaded
        // quickly enough before the call to play(). https://stackoverflow.com/questions/35435625
        loadBellSoundPool();
    }

    /**
     * Should be called when TermuxFloatView.onDetachedFromWindow() is called
     */
    public void onDetachedFromWindow() {
        // Release mBellSoundPool resources, specially to prevent exceptions like the following to be thrown
        // java.util.concurrent.TimeoutException: android.media.SoundPool.finalize() timed out after 10 seconds
        // Bell is not played in background anyways
        // Related: https://stackoverflow.com/a/28708351/14686958
        releaseBellSoundPool();
    }

    /**
     * Should be called when TermuxFloatView.onReload() is called
     */
    public void onReload() {
        checkForFontAndColors();
    }



    @Override
    public void onTextChanged(TerminalSession changedSession) {
        if (!mView.isVisible()) return;

        mView.getTerminalView().onScreenUpdated();
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
    public void onPasteTextFromClipboard(TerminalSession session) {
        if (!mView.isVisible()) return;

        ClipboardManager clipboard = (ClipboardManager) mService.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = clipboard.getPrimaryClip();
        if (clipData != null) {
            CharSequence paste = clipData.getItemAt(0).coerceToText(mService);
            if (!TextUtils.isEmpty(paste)) mView.getTerminalView().mEmulator.paste(paste.toString());
        }
    }

    @Override
    public void onBell(TerminalSession session) {
        if (!mView.isVisible()) return;

        int bellBehaviour = mView.getProperties().getBellBehaviour();
        if (bellBehaviour == TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_VIBRATE) {
            BellHandler.getInstance(mService).doBell();
        } else if (bellBehaviour == TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_BEEP) {
            loadBellSoundPool();
            if (mBellSoundPool != null)
                mBellSoundPool.play(mBellSoundId, 1.f, 1.f, 1, 0, 1.f);
        } else if (bellBehaviour == TermuxPropertyConstants.IVALUE_BELL_BEHAVIOUR_IGNORE) {
            // Ignore the bell character.
        }
    }

    @Override
    public void onColorsChanged(TerminalSession changedSession) {
        updateBackgroundColor();
    }


    @Override
    public Integer getTerminalCursorStyle() {
        return mView.getProperties().getTerminalCursorStyle();
    }


    /** Load mBellSoundPool */
    private synchronized void loadBellSoundPool() {
        if (mBellSoundPool == null) {
            mBellSoundPool = new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(
                    new AudioAttributes.Builder().setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).build()).build();

            try {
                mBellSoundId = mBellSoundPool.load(mService, com.termux.shared.R.raw.bell, 1);
            } catch (Exception e){
                // Catch java.lang.RuntimeException: Unable to resume activity {com.termux/com.termux.app.TermuxActivity}: android.content.res.Resources$NotFoundException: File res/raw/bell.ogg from drawable resource ID
                Logger.logStackTraceWithMessage(LOG_TAG, "Failed to load bell sound pool", e);
            }
        }
    }

    /** Release mBellSoundPool resources */
    private synchronized void releaseBellSoundPool() {
        if (mBellSoundPool != null) {
            mBellSoundPool.release();
            mBellSoundPool = null;
        }
    }



    public void checkForFontAndColors() {
        try {
            File colorsFile = TermuxConstants.TERMUX_COLOR_PROPERTIES_FILE;
            File fontFile = TermuxConstants.TERMUX_FONT_FILE;

            final Properties props = new Properties();
            if (colorsFile.isFile()) {
                try (InputStream in = new FileInputStream(colorsFile)) {
                    props.load(in);
                }
            }

            TerminalColors.COLOR_SCHEME.updateWith(props);
            TerminalSession session = mService.getCurrentSession();
            if (session != null && session.getEmulator() != null) {
                session.getEmulator().mColors.reset();
            }

            updateBackgroundColor();

            final Typeface newTypeface = (fontFile.exists() && fontFile.length() > 0) ? Typeface.createFromFile(fontFile) : Typeface.MONOSPACE;
            mView.getTerminalView().setTypeface(newTypeface);
        } catch (Exception e) {
            Logger.logStackTraceWithMessage(LOG_TAG, "Error in checkForFontAndColors()", e);
        }
    }

    public void updateBackgroundColor() {
        //if (!mView.isVisible()) return;

        TerminalSession session = mService.getCurrentSession();
        if (session != null && session.getEmulator() != null) {
            mView.getTerminalView().setBackgroundColor(session.getEmulator().mColors.mCurrentColors[TextStyle.COLOR_INDEX_BACKGROUND]);
        }
    }

}
