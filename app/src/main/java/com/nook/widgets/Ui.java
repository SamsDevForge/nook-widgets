package com.nook.widgets;

import android.app.Activity;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;

public final class Ui {
    public static final int BG = 0xff111512, SURFACE = 0xff1c231e, LINE = 0xff303b32;
    public static final int TEXT = 0xfff0f1e9, MUTED = 0xff9ca99e, ACCENT = 0xffc4d7b2;
    private Ui() {}
    public static int dp(Context c, float v) { return Math.round(v * c.getResources().getDisplayMetrics().density); }
    public static GradientDrawable shape(int color, float radius, Context c) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(c, radius)); return d;
    }
    public static void surface(View v, int color, int radius) { v.setBackground(shape(color, radius, v.getContext())); }
    public static void click(View v, Runnable action) {
        Drawable background = v.getBackground();
        v.setBackground(new RippleDrawable(android.content.res.ColorStateList.valueOf(0x22ffffff), background,
            shape(0xffffffff, 20, v.getContext())));
        v.setFocusable(true);
        v.setOnClickListener(w -> { w.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); action.run(); });
    }
    public static LinearLayout column(Context c) {
        LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.VERTICAL); return l;
    }
    public static LinearLayout row(Context c) {
        LinearLayout l = new LinearLayout(c); l.setOrientation(LinearLayout.HORIZONTAL); l.setGravity(Gravity.CENTER_VERTICAL); return l;
    }
    public static void pad(View v, int h, int vertical) { v.setPadding(dp(v.getContext(), h), dp(v.getContext(), vertical), dp(v.getContext(), h), dp(v.getContext(), vertical)); }
    public static TextView text(Context c, String value, int size, int color) {
        TextView t = new TextView(c); t.setText(value); t.setTextSize(size); t.setTextColor(color);
        t.setFontFeatureSettings("kern"); t.setIncludeFontPadding(false); t.setLineSpacing(dp(c, 3), 1f); return t;
    }
    public static TextView bold(Context c, String value, int size, int color) {
        TextView t = text(c, value, size, color); t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL)); return t;
    }
    public static TextView button(Context c, String label, int bg, int color, Runnable action) {
        TextView t = bold(c, label, 14, color); t.setGravity(Gravity.CENTER); t.setMinHeight(dp(c, 50)); pad(t, 18, 12);
        surface(t, bg, 18); click(t, action); return t;
    }
    public static void add(LinearLayout parent, View v, int width, int height) {
        parent.addView(v, new LinearLayout.LayoutParams(width < 0 ? width : dp(parent.getContext(), width), height < 0 ? height : dp(parent.getContext(), height)));
    }
    public static void gap(LinearLayout l, int size) { add(l, new View(l.getContext()), 1, size); }
    public static void weight(LinearLayout l, View v) { l.addView(v, new LinearLayout.LayoutParams(0, -2, 1)); }
    public static void space(LinearLayout l) { l.addView(new View(l.getContext()), new LinearLayout.LayoutParams(0, 1, 1)); }
    public static void margin(View v, int bottom) {
        if (v.getLayoutParams() instanceof LinearLayout.LayoutParams p) { p.bottomMargin = dp(v.getContext(), bottom); v.setLayoutParams(p); }
    }
    public static View divider(Context c) { View v = new View(c); v.setBackgroundColor(LINE); v.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(c, 1))); return v; }
    public static Icon icon(Context c, String name, int color) { return new Icon(c, name, color); }
    public static View iconButton(Context c, String icon, String description, Runnable action) {
        Icon v = icon(c, icon, MUTED); v.setContentDescription(description); v.setLayoutParams(new LinearLayout.LayoutParams(dp(c, 48), dp(c, 48)));
        surface(v, SURFACE, 18); click(v, action); return v;
    }
    public static void insets(Activity a, View root) {
        a.getWindow().setStatusBarColor(BG); a.getWindow().setNavigationBarColor(BG);
        a.getWindow().getDecorView().setSystemUiVisibility(0);
        root.setOnApplyWindowInsetsListener((v, i) -> {
            v.setPadding(i.getSystemWindowInsetLeft(), i.getSystemWindowInsetTop(), i.getSystemWindowInsetRight(), i.getSystemWindowInsetBottom());
            return i.consumeSystemWindowInsets();
        });
        root.requestApplyInsets();
    }
    public static final class Icon extends View {
        private final String name;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        public Icon(Context c, String name, int color) { super(c); this.name = name; p.setColor(color); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.7f); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); }
        private void line(Canvas c, float... a) { for (int i = 0; i < a.length - 2; i += 2) c.drawLine(a[i], a[i+1], a[i+2], a[i+3], p); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            canvas.save(); float size = Math.min(getWidth(), getHeight()) * .48f;
            canvas.translate((getWidth()-size)/2f, (getHeight()-size)/2f); canvas.scale(size/24f, size/24f);
            switch (name) {
                case "plus" -> { line(canvas, 12, 5, 12, 19); line(canvas, 5, 12, 19, 12); }
                case "arrow" -> { line(canvas, 5, 12, 19, 12); line(canvas, 13, 6, 19, 12, 13, 18); }
                case "back" -> { line(canvas, 19, 12, 5, 12); line(canvas, 11, 6, 5, 12, 11, 18); }
                case "check" -> line(canvas, 5, 12, 10, 17, 19, 7);
                case "notes" -> { canvas.drawRoundRect(5, 3, 19, 21, 3, 3, p); line(canvas, 9, 8, 15, 8); line(canvas, 9, 12, 15, 12); line(canvas, 9, 16, 12, 16); }
                case "reminders" -> { canvas.drawArc(6, 4, 18, 18, 180, 180, false, p); line(canvas, 6, 11, 6, 16, 4, 18, 20, 18, 18, 16, 18, 11); canvas.drawArc(10, 17, 14, 22, 0, 180, false, p); }
                case "timers" -> { canvas.drawCircle(12, 14, 8, p); line(canvas, 9, 2, 15, 2); line(canvas, 12, 2, 12, 6); line(canvas, 12, 10, 12, 14, 15, 16); }
                case "alarms" -> { canvas.drawCircle(12, 13, 8, p); line(canvas, 12, 9, 12, 13, 15, 15); line(canvas, 3, 5, 6, 2); line(canvas, 18, 2, 21, 5); line(canvas, 5, 20, 3, 22); line(canvas, 19, 20, 21, 22); }
                case "checklist" -> { canvas.drawRoundRect(4, 3, 20, 21, 3, 3, p); line(canvas, 7, 9, 9, 11, 13, 7); line(canvas, 8, 16, 16, 16); }
                case "habits" -> { line(canvas, 12, 3, 14.5f, 9.5f, 21, 12, 14.5f, 14.5f, 12, 21, 9.5f, 14.5f, 3, 12, 9.5f, 9.5f, 12, 3); }
                case "clock" -> { canvas.drawCircle(12, 12, 9, p); line(canvas, 12, 6, 12, 12, 16, 14); }
                case "settings" -> { line(canvas, 5, 7, 19, 7); line(canvas, 5, 17, 19, 17); canvas.drawCircle(9, 7, 2.5f, p); canvas.drawCircle(15, 17, 2.5f, p); }
                case "sun" -> { canvas.drawCircle(12, 12, 4, p); for (int i=0;i<8;i++) { canvas.save(); canvas.rotate(45*i,12,12); line(canvas,12,2,12,4); canvas.restore(); } }
                default -> { canvas.drawRoundRect(3, 3, 10, 10, 1.5f, 1.5f, p); canvas.drawRoundRect(14, 3, 21, 10, 1.5f, 1.5f, p); canvas.drawRoundRect(3, 14, 10, 21, 1.5f, 1.5f, p); canvas.drawRoundRect(14, 14, 21, 21, 1.5f, 1.5f, p); }
            }
            canvas.restore();
        }
    }
}
