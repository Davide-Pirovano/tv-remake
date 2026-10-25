package com.gathin.tvhome;

import android.content.Context;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.function.Consumer;

/** On-screen keypad; never opens the Android IME. Digits are visible at the owner’s request. */
final class PinPad extends LinearLayout {
    static final int WIDTH_DP=300;
    private final StringBuilder digits=new StringBuilder();
    private final TextView display;
    private final Button[] keys=new Button[12];
    private final Consumer<String> submit;
    private boolean busy;
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    PinPad(Context context,Consumer<String> submit) {
        super(context);this.submit=submit;setOrientation(VERTICAL);setGravity(Gravity.CENTER_HORIZONTAL);
        display=new TextView(context);display.setTextColor(GuestStyle.MIST);display.setTextSize(24);
        display.setGravity(Gravity.CENTER);display.setLetterSpacing(.16f);
        addView(display,new LayoutParams(dp(WIDTH_DP),dp(42)));
        String[] labels={"1","2","3","4","5","6","7","8","9","⌫","0","OK"};
        for(int row=0;row<4;row++) {
            LinearLayout line=new LinearLayout(context);line.setGravity(Gravity.CENTER);
            addView(line,new LayoutParams(LayoutParams.WRAP_CONTENT,dp(46)));
            for(int col=0;col<3;col++) {
                int index=row*3+col;Button b=new Button(context);keys[index]=b;b.setId(View.generateViewId());
                b.setText(labels[index]);b.setTextSize(21);GuestStyle.button(b,index==11);
                b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(0);b.setMinimumHeight(0);b.setPadding(0,0,0,0);
                LayoutParams lp=new LayoutParams(dp(96),dp(40));lp.setMargins(dp(3),dp(3),dp(3),dp(3));line.addView(b,lp);
                if(index==9)b.setContentDescription("Cancella ultima cifra");
                b.setOnClickListener(v->{if(index==9)erase();else if(index==11)send();else digit(labels[index].charAt(0));});
            }
        }
        for(int i=0;i<12;i++) {
            keys[i].setNextFocusLeftId(keys[i%3==0?i:i-1].getId());
            keys[i].setNextFocusRightId(keys[i%3==2?i:i+1].getId());
            keys[i].setNextFocusUpId(keys[i<3?i:i-3].getId());
            if(i<9)keys[i].setNextFocusDownId(keys[i+3].getId());
        }
        render();keys[0].requestFocus();
    }
    void digit(char digit){if(!busy&&digits.length()<8){digits.append(digit);render();}}
    void erase(){if(!busy&&digits.length()>0){digits.deleteCharAt(digits.length()-1);render();}}
    void send(){if(!busy)submit.accept(digits.toString());}
    void clear(){digits.setLength(0);render();}
    void busy(boolean value){busy=value;for(Button b:keys)b.setEnabled(!value);if(!value)keys[0].requestFocus();}
    private void render(){display.setText(digits.length()==0?"— — — — — —":digits.toString());
        display.setContentDescription(digits.length()+" cifre inserite");}
}
