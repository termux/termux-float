package com.termux.window;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.os.Build;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ScaleGestureDetector.OnScaleGestureListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;

import com.termux.shared.logger.Logger;
import com.termux.shared.termux.TermuxConstants;
import com.termux.shared.termux.settings.preferences.TermuxFloatAppSharedPreferences;
import com.termux.shared.view.KeyboardUtils;
import com.termux.terminal.TerminalSession;
import com.termux.terminal.TerminalSessionClient;
import com.termux.view.TerminalView;
import com.termux.view.TerminalViewClient;
import com.termux.window.settings.properties.TermuxFloatAppSharedProperties;

public class TermuxFloatView extends LinearLayout {

    public static final float ALPHA_FOCUS = 0.9f;
    public static final float ALPHA_NOT_FOCUS = 0.7f;
    public static final float ALPHA_MOVING = 0.5f;

    private int DISPLAY_WIDTH, DISPLAY_HEIGHT;

    final WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
    WindowManager mWindowManager;

    private TerminalView mTerminalView;
    ViewGroup mWindowControls;
    FloatingBubbleManager mFloatingBubbleManager;

    /**
     *  The {@link TerminalViewClient} interface implementation to allow for communication between
     *  {@link TerminalView} and {@link TermuxFloatView}.
     */
    TermuxFloatViewClient mTermuxFloatViewClient;

    /**
     *  The {@link TerminalSessionClient} interface implementation to allow for communication between
     *  {@link TerminalSession} and {@link TermuxFloatService}.
     */
    TermuxFloatSessionClient mTermuxFloatSessionClient;

    /**
     * Termux Float app shared preferences manager.
     */
    private TermuxFloatAppSharedPreferences mPreferences;

    /**
     * Termux app shared properties manager, loaded from termux.properties
     */
    private TermuxFloatAppSharedProperties mProperties;

    private boolean withFocus = true;
    int initialX;
    int initialY;
    float initialTouchX;
    float initialTouchY;

    boolean isInLongPressState;

    final int[] location = new int[2];

    final int[] windowControlsLocation = new int[2];

    private static final String LOG_TAG = "TermuxFloatView";

    final ScaleGestureDetector mScaleDetector = new ScaleGestureDetector(getContext(), new OnScaleGestureListener() {
        private static final int MIN_SIZE = 50;

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            int widthChange = (int) (detector.getCurrentSpanX() - detector.getPreviousSpanX());
            int heightChange = (int) (detector.getCurrentSpanY() - detector.getPreviousSpanY());
            layoutParams.width += widthChange;
            layoutParams.height += heightChange;
            layoutParams.width = Math.max(MIN_SIZE, layoutParams.width);
            layoutParams.height = Math.max(MIN_SIZE, layoutParams.height);
            mWindowManager.updateViewLayout(TermuxFloatView.this, layoutParams);
            if (mPreferences != null) {
                mPreferences.setWindowWidth(layoutParams.width);
                mPreferences.setWindowHeight(layoutParams.height);
            }
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            // Do nothing.
        }
    });

    public TermuxFloatView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setAlpha(ALPHA_FOCUS);
    }

    private static int computeLayoutFlags(boolean withFocus) {
        if (withFocus) {
            return 0;
        } else {
            return WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        }
    }

    public void initFloatView(TermuxFloatService service) {
        Logger.logDebug(LOG_TAG, "initFloatView");

        // Load termux shared properties
        mProperties = new TermuxFloatAppSharedProperties(getContext());

        // Load termux float shared preferences
        // This will also fail if TermuxConstants.TERMUX_FLOAT_PACKAGE_NAME does not equal applicationId
        mPreferences = TermuxFloatAppSharedPreferences.build(getContext(), true);
        if (mPreferences == null) {
            return;
        }

        mTermuxFloatSessionClient = new TermuxFloatSessionClient(service, this);

        mTerminalView = findViewById(R.id.terminal_view);
        mTermuxFloatViewClient = new TermuxFloatViewClient(this, mTermuxFloatSessionClient);
        mTerminalView.setTerminalViewClient(mTermuxFloatViewClient);
        mTermuxFloatViewClient.initFloatView();

        mFloatingBubbleManager = new FloatingBubbleManager(this);
        initWindowControls();
    }

    private void initWindowControls() {
        mWindowControls = findViewById(R.id.window_controls);
        mWindowControls.setOnClickListener(v -> changeFocus(true));

        Button minimizeButton = findViewById(R.id.minimize_button);
        minimizeButton.setOnClickListener(v -> mFloatingBubbleManager.toggleBubble());

        Button exitButton = findViewById(R.id.exit_button);
        exitButton.setOnClickListener(v -> exit());
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        Point displaySize = new Point();
        getDisplay().getSize(displaySize);
        DISPLAY_WIDTH = displaySize.x;
        DISPLAY_HEIGHT = displaySize.y;

        if (mTermuxFloatSessionClient != null)
            mTermuxFloatSessionClient.onAttachedToWindow();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        if (mTermuxFloatSessionClient != null)
            mTermuxFloatSessionClient.onDetachedFromWindow();
    }

    @SuppressLint("RtlHardcoded")
    public void launchFloatingWindow() {
        int widthAndHeight = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
        layoutParams.flags = computeLayoutFlags(true);
        layoutParams.width = widthAndHeight;
        layoutParams.height = widthAndHeight;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutParams.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutParams.type = WindowManager.LayoutParams.TYPE_PHONE;
        }
        layoutParams.format = PixelFormat.RGBA_8888;

        layoutParams.gravity = Gravity.TOP | Gravity.LEFT;

        if (mPreferences != null) {
            layoutParams.x = mPreferences.getWindowX();
            layoutParams.y = mPreferences.getWindowY();
            layoutParams.width = mPreferences.getWindowWidth();
            layoutParams.height = mPreferences.getWindowHeight();
        }

        mWindowManager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
        if (getWindowToken() == null)
            mWindowManager.addView(this, layoutParams);
        showTouchKeyboard();
    }

    /**
     * Intercept touch events to obtain and loose focus on touch events.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        if (isInLongPressState) return true;

        getLocationOnScreen(location);
        int x = location[0];
        int y = location[1];
        float touchX = event.getRawX();
        float touchY = event.getRawY();

        if (didClickInsideWindowControls(touchX, touchY)) {
            // avoid unintended focus event if we are tapping on our window controls
            // so that keyboard doesn't possibly show briefly
            return false;
        }

        boolean clickedInside = (touchX >= x) && (touchX <= (x + layoutParams.width)) && (touchY >= y) && (touchY <= (y + layoutParams.height));

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!clickedInside) changeFocus(false);
                break;
            case MotionEvent.ACTION_UP:
                if (clickedInside) {
                    changeFocus(true);
                    showTouchKeyboard();
                }
                break;
        }
        return false;
    }

    private boolean didClickInsideWindowControls(float touchX, float touchY) {
        if (mWindowControls.getVisibility() == View.GONE) {
            return false;
        }
        mWindowControls.getLocationOnScreen(windowControlsLocation);
        int controlsX = windowControlsLocation[0];
        int controlsY = windowControlsLocation[1];

        return (touchX >= controlsX && touchX <= controlsX + mWindowControls.getWidth()) &&
                (touchY >= controlsY && touchY <= controlsY + mWindowControls.getHeight());
    }

    void showTouchKeyboard() {
        mTerminalView.post(() -> KeyboardUtils.showSoftKeyboard(getContext(), mTerminalView));

    }

    void hideTouchKeyboard() {
        mTerminalView.post(() -> KeyboardUtils.hideSoftKeyboard(getContext(), mTerminalView));
    }

    void updateLongPressMode(boolean newValue) {
        isInLongPressState = newValue;
        mFloatingBubbleManager.updateLongPressBackgroundResource(isInLongPressState);
        setAlpha(newValue ? ALPHA_MOVING : (withFocus ? ALPHA_FOCUS : ALPHA_NOT_FOCUS));
        if (newValue && !mFloatingBubbleManager.isMinimized())
            Logger.showToast(getContext(), getContext().getString(R.string.after_long_press), false);
    }

    /**
     * Motion events should only be dispatched here when {@link #onInterceptTouchEvent(MotionEvent)} returns true.
     */
    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isInLongPressState) {
            mScaleDetector.onTouchEvent(event);
            if (mScaleDetector.isInProgress()) return true;
            switch (event.getAction()) {
                case MotionEvent.ACTION_MOVE:
                    layoutParams.x = Math.min(DISPLAY_WIDTH - layoutParams.width, Math.max(0, initialX + (int) (event.getRawX() - initialTouchX)));
                    layoutParams.y = Math.min(DISPLAY_HEIGHT - layoutParams.height, Math.max(0, initialY + (int) (event.getRawY() - initialTouchY)));
                    mWindowManager.updateViewLayout(TermuxFloatView.this, layoutParams);
                    if (mPreferences != null) {
                        mPreferences.setWindowX(layoutParams.x);
                        mPreferences.setWindowY(layoutParams.y);
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    updateLongPressMode(false);
                    break;
            }
            return true;
        }
        return super.onTouchEvent(event);
    }

    /**
     * Visually indicate focus and show the soft input as needed.
     */
    void changeFocus(boolean newFocus) {
        if (newFocus && mFloatingBubbleManager.isMinimized()) {
            mFloatingBubbleManager.displayAsFloatingWindow();
        }
        if (newFocus == withFocus) {
            if (newFocus) showTouchKeyboard();
            return;
        }
        withFocus = newFocus;
        layoutParams.flags = computeLayoutFlags(withFocus);
        if (getWindowToken() != null)
            mWindowManager.updateViewLayout(this, layoutParams);
        setAlpha(newFocus ? ALPHA_FOCUS : ALPHA_NOT_FOCUS);
    }

    public void closeFloatingWindow() {
        if (getWindowToken() != null)
            mWindowManager.removeView(this);

        mFloatingBubbleManager.cleanup();
        mFloatingBubbleManager = null;
    }

    private void exit() {
        Intent exitIntent = new Intent(getContext(), TermuxFloatService.class).setAction(TermuxConstants.TERMUX_FLOAT_APP.TERMUX_FLOAT_SERVICE.ACTION_STOP_SERVICE);
        getContext().startService(exitIntent);
    }



    public boolean isVisible() {
        return isAttachedToWindow() && isShown();
    }

    public TerminalView getTerminalView() {
        return mTerminalView;
    }

    public TermuxFloatViewClient getTermuxFloatViewClient() {
        return mTermuxFloatViewClient;
    }

    public TermuxFloatSessionClient getTermuxFloatSessionClient() {
        return mTermuxFloatSessionClient;
    }

    public TermuxFloatAppSharedPreferences getPreferences() {
        return mPreferences;
    }

    public TermuxFloatAppSharedProperties getProperties() {
        return mProperties;
    }


    public void reloadViewStyling() {
        // Leaving here for future support for termux-reload-settings
        if (mTermuxFloatSessionClient != null)
            mTermuxFloatSessionClient.onReload();
    }
}
