package com.termux.window;

import android.graphics.Outline;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;

import com.termux.shared.view.ViewUtils;
import com.termux.view.TerminalView;

/**
 * Handles displaying our TermuxFloatView as a collapsed bubble and restoring back
 * to its original display.
 */
public class FloatingBubbleManager {
    private static final int DEFAULT_BUBBLE_SIZE_DP = 56;

    private TermuxFloatView mTermuxFloatView;
    private final int BUBBLE_SIZE_PX;

    private boolean mIsMinimized;

    // preserve original layout values so we can restore to normal window
    // from our bubble
    private int mOriginalLayoutWidth;
    private int mOriginalLayoutHeight;
    private boolean mDidCaptureOriginalValues;
    private Drawable mOriginalTerminalViewBackground;
    private Drawable mOriginalFloatViewBackground;

    public FloatingBubbleManager(TermuxFloatView termuxFloatView) {
        mTermuxFloatView = termuxFloatView;
        BUBBLE_SIZE_PX = (int) ViewUtils.dpToPx(mTermuxFloatView.getContext(), DEFAULT_BUBBLE_SIZE_DP);
    }

    public void toggleBubble() {
        if (isMinimized()) {
            displayAsFloatingWindow();
        } else {
            displayAsFloatingBubble();
        }
    }

    public void updateLongPressBackgroundResource(boolean isInLongPressState) {
        if (isMinimized()) {
            return;
        }
        mTermuxFloatView.setBackgroundResource(isInLongPressState ? R.drawable.floating_window_background_resize : R.drawable.floating_window_background);
    }

    public void displayAsFloatingBubble() {
        captureOriginalLayoutValues();

        WindowManager.LayoutParams layoutParams = getLayoutParams();

        layoutParams.width = BUBBLE_SIZE_PX;
        layoutParams.height = BUBBLE_SIZE_PX;

        TerminalView terminalView = getTerminalView();
        final int strokeWidth = (int) terminalView.getResources().getDimension(R.dimen.bubble_outline_stroke_width);
        terminalView.setOutlineProvider(new ViewOutlineProvider() {
            @SuppressWarnings("SuspiciousNameCombination")
            @Override
            public void getOutline(View view, Outline outline) {
                // shrink TerminalView clipping a bit so it doesn't cut off our bubble outline
                outline.setOval(strokeWidth, strokeWidth, view.getWidth() - strokeWidth, view.getHeight() - strokeWidth);
            }
        });
        terminalView.setClipToOutline(true);

        TermuxFloatView termuxFloatView = getTermuxFloatView();
        termuxFloatView.setBackgroundResource(R.drawable.round_button_with_outline);
        termuxFloatView.setClipToOutline(true);
        termuxFloatView.hideTouchKeyboard();
        termuxFloatView.changeFocus(false);

        ViewGroup windowControls = termuxFloatView.findViewById(R.id.window_controls);
        windowControls.setVisibility(View.GONE);

        getWindowManager().updateViewLayout(termuxFloatView, layoutParams);
        mIsMinimized = true;
    }

    public void displayAsFloatingWindow() {
        WindowManager.LayoutParams layoutParams = getLayoutParams();

        // restore back to previous values
        layoutParams.width = mOriginalLayoutWidth;
        layoutParams.height = mOriginalLayoutHeight;

        TerminalView terminalView = getTerminalView();
        terminalView.setBackground(mOriginalTerminalViewBackground);
        terminalView.setOutlineProvider(null);
        terminalView.setClipToOutline(false);

        TermuxFloatView termuxFloatView = getTermuxFloatView();
        termuxFloatView.setBackground(mOriginalFloatViewBackground);
        termuxFloatView.setClipToOutline(false);

        ViewGroup windowControls = termuxFloatView.findViewById(R.id.window_controls);
        windowControls.setVisibility(View.VISIBLE);

        getWindowManager().updateViewLayout(termuxFloatView, layoutParams);
        mIsMinimized = false;

        // clear so we can capture proper values on next minimize
        mDidCaptureOriginalValues = false;
    }

    public boolean isMinimized() {
        return mIsMinimized;
    }

    private void captureOriginalLayoutValues() {
        if (!mDidCaptureOriginalValues) {
            WindowManager.LayoutParams layoutParams = getLayoutParams();
            mOriginalLayoutWidth = layoutParams.width;
            mOriginalLayoutHeight = layoutParams.height;

            mOriginalTerminalViewBackground = getTerminalView().getBackground();
            mOriginalFloatViewBackground = getTermuxFloatView().getBackground();
            mDidCaptureOriginalValues = true;
        }
    }

    public void cleanup() {
        mTermuxFloatView = null;
        mOriginalFloatViewBackground = null;
        mOriginalTerminalViewBackground = null;
    }

    private TermuxFloatView getTermuxFloatView() {
        return mTermuxFloatView;
    }

    private TerminalView getTerminalView() {
        return mTermuxFloatView.getTerminalView();
    }

    private WindowManager getWindowManager() {
        return mTermuxFloatView.mWindowManager;
    }

    private WindowManager.LayoutParams getLayoutParams() {
        return (WindowManager.LayoutParams) mTermuxFloatView.getLayoutParams();
    }
}
