package com.gathin.tvhome;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;

/** Two native text views, animated only during the visible, once-per-stay welcome. */
final class GuestWelcomeView extends FrameLayout {
    private static final String[] GREETINGS = {"Buongiorno", "Good morning", "Bonjour", "Guten Morgen",
            "Buenos días", "Bom dia", "Goedemorgen", "早上好", "おはようございます", "좋은 아침입니다", "صباح الخير"};
    private final TextSwitcher greeting;
    private int index;
    private boolean running;
    private final Runnable advance = new Runnable() {
        @Override public void run() {
            if (!running) return;
            index = (index + 1) % GREETINGS.length;
            greeting.setText(GREETINGS[index]);
            postDelayed(this, 3200);
        }
    };

    GuestWelcomeView(Context context, String guest, Runnable continueAction) {
        super(context);
        setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{GuestStyle.SURFACE, GuestStyle.CANVAS, GuestStyle.DEEP}));
        setPadding(dp(48), dp(32), dp(48), dp(32));
        setFocusable(true);
        setClickable(true);
        setContentDescription("Benvenuti. Premi OK per scegliere la lingua. Welcome. Press OK to choose your language.");
        setOnClickListener(v -> { stop(); setEnabled(false); continueAction.run(); });

        LinearLayout center = new LinearLayout(context);
        center.setOrientation(LinearLayout.VERTICAL);
        center.setGravity(Gravity.CENTER);
        center.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS);
        addView(center, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        TextView welcome = label("BENVENUTI · WELCOME", 15, GuestStyle.SILVER);
        welcome.setLetterSpacing(.18f);
        center.addView(welcome);
        greeting = new TextSwitcher(context);
        greeting.setFactory(() -> {
            TextView text = label("", 88, GuestStyle.MIST);
            text.setTypeface(context.getResources().getFont(R.font.outfit_light));
            text.setSingleLine(true);
            text.setAutoSizeTextTypeUniformWithConfiguration(28, 88, 2, android.util.TypedValue.COMPLEX_UNIT_SP);
            text.setLayoutParams(new FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            return text;
        });
        greeting.setAnimateFirstView(false);
        if (ValueAnimator.areAnimatorsEnabled()) {
            greeting.setInAnimation(transition(true));
            greeting.setOutAnimation(transition(false));
        }
        center.addView(greeting, new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, dp(136)));
        greeting.setCurrentText(GREETINGS[0]);
        if (!guest.isEmpty()) center.addView(label(guest, 24, GuestStyle.MINT));

        TextView hint = label("Premi OK per iniziare · Press OK to start", 17, GuestStyle.SILVER);
        hint.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        addView(hint, new LayoutParams(LayoutParams.MATCH_PARENT, dp(48), Gravity.BOTTOM));
    }
    private TextView label(String text, int size, int color) {
        TextView label = new TextView(getContext());
        label.setText(text);
        label.setTextSize(size);
        label.setTextColor(color);
        label.setGravity(Gravity.CENTER);
        label.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        return label;
    }
    private Animation transition(boolean entering) {
        AnimationSet set = new AnimationSet(true);
        set.addAnimation(new AlphaAnimation(entering ? 0 : 1, entering ? 1 : 0));
        set.addAnimation(new TranslateAnimation(0, 0, entering ? dp(24) : 0, entering ? 0 : -dp(24)));
        set.setDuration(420);
        return set;
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    void start() {
        if (running || !isEnabled() || !isAttachedToWindow() || !isShown() || getWindowVisibility() != VISIBLE) return;
        running = true;
        postDelayed(advance, 3200);
    }
    void stop() {
        running = false;
        removeCallbacks(advance);
        for (int i = 0; i < greeting.getChildCount(); i++) greeting.getChildAt(i).clearAnimation();
    }
    @Override public void onVisibilityAggregated(boolean visible) {
        super.onVisibilityAggregated(visible);
        if (visible) start(); else stop();
    }
    @Override protected void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }
}
