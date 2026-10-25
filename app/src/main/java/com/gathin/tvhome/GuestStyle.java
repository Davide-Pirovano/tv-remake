package com.gathin.tvhome;

import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.Button;

/** Static native surfaces: no bitmap wallpaper, shadows, animation or continuous drawing. */
final class GuestStyle {
    static final int CANVAS=0xff012624, DEEP=0xff011d1c, SURFACE=0xff003734;
    static final int MIST=0xffedfffe, SILVER=0xffbbc7c6, MINT=0xffcbfffc;
    static GradientDrawable surface(float density,int color,int radius) {
        GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(radius*density);return d;
    }
    static void button(Button b, boolean primary) {
        float d=b.getResources().getDisplayMetrics().density;
        StateListDrawable states=new StateListDrawable();
        GradientDrawable focused=surface(d,MINT,6);focused.setStroke(Math.round(2*d),0xffffffff);
        states.addState(new int[]{android.R.attr.state_focused},focused);
        states.addState(new int[]{android.R.attr.state_pressed},surface(d,MINT,6));
        if(primary) {
            GradientDrawable gradient=new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xffcbfffc,0xffedfffe,0xfffffdfa,0xfffad1ff});
            gradient.setCornerRadius(6*d);states.addState(new int[0],gradient);
        } else states.addState(new int[0],surface(d,SURFACE,6));
        b.setBackground(states);b.setStateListAnimator(null);b.setElevation(0);
        b.setTypeface(Typeface.create("sans-serif-medium",Typeface.NORMAL));
        b.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_focused},{android.R.attr.state_pressed},{}},
                new int[]{DEEP,DEEP,primary?DEEP:MIST}));
        b.setAllCaps(false);
    }
}
