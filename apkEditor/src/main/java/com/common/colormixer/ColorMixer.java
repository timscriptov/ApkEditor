/***
 Copyright (c) 2008-2013 CommonsWare, LLC

 Licensed under the Apache License, Version 2.0 (the "License"); you may
 not use this file except in compliance with the License. You may obtain
 a copy of the License at
 http://www.apache.org/licenses/LICENSE-2.0
 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */

package com.common.colormixer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import com.gmail.heagoo.apkeditor.base.R;

public class ColorMixer extends RelativeLayout implements TextWatcher {
    private static final String SUPERSTATE = "superState";
    private static final String COLOR = "color";
    private View swatch = null;
    private SeekBar red = null;
    private SeekBar blue = null;
    private SeekBar green = null;
    private SeekBar alpha = null;
    private EditText colorEt = null;
    private OnColorChangedListener listener = null;

    // Record last time of value change
    private int valueFromText;
    private int valueFromBar;
    Handler hander = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 0:
                    int color = getColor();

                    // Not set the text if it is from that
                    if (color != valueFromText) {
                        //Log.d("DEBUG", "update color from progress bar: " + color);
                        setTextColorValue(color);
                    }

                    swatch.setBackgroundColor(color);

                    if (listener != null) {
                        listener.onColorChange(color);
                    }
                    break;
            }
        }
    };
    // Make changes from progress bar to text
    private SeekBar.OnSeekBarChangeListener onMix = new SeekBar.OnSeekBarChangeListener() {
        public void onProgressChanged(SeekBar seekBar, int progress,
                                      boolean fromUser) {
            hander.removeMessages(0);
            hander.sendEmptyMessageDelayed(0, 100);
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
            // unused
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            // unused
        }
    };

    public ColorMixer(Context context) {
        super(context);

        initMixer(null);
    }

    public ColorMixer(Context context, AttributeSet attrs) {
        super(context, attrs);

        initMixer(attrs);
    }

    public ColorMixer(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        initMixer(attrs);
    }

    public OnColorChangedListener getOnColorChangedListener() {
        return (listener);
    }

    public void setOnColorChangedListener(OnColorChangedListener listener) {
        this.listener = listener;
    }

    public int getColor() {
        return (Color.argb(alpha.getProgress(), red.getProgress(),
                green.getProgress(), blue.getProgress()));
    }

    public void setProgressBarColor(int color) {
        red.setProgress(Color.red(color));
        green.setProgress(Color.green(color));
        blue.setProgress(Color.blue(color));
        alpha.setProgress(Color.alpha(color));

        // setTextColor(color);
    }

    private void updateColorFromText() {
        String strColor = colorEt.getText().toString();

        Long value = Long.parseLong(strColor, 16);

        // Text is updated by progress bar, do not update progress bar again
        if (value.intValue() == this.valueFromBar) {
            return;
        }

        if (strColor.length() <= 6) {
            value |= 0xff000000;
        }

        int color = value.intValue();

        this.valueFromText = color;

        //Log.d("DEBUG", "update color from text: " + color);
        setProgressBarColor(color);
    }

    private void setTextColorValue(int color) {
        this.valueFromBar = color;

        String text = Integer.toHexString(color);

        colorEt.setText(text);
        colorEt.setSelection(text.length());
    }

    private void initMixer(AttributeSet attrs) {
        if (isInEditMode()) {
            return;
        }

        LayoutInflater inflater = null;

        if (getContext() instanceof Activity) {
            inflater = ((Activity) getContext()).getLayoutInflater();
        } else {
            inflater = LayoutInflater.from(getContext());
        }

        inflater.inflate(R.layout.dlg_colormixer, this, true);

        swatch = findViewById(R.id.swatch);

        // Color EditText
        colorEt = (EditText) findViewById(R.id.color);
        colorEt.addTextChangedListener(this);
        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!isHexChar(source.charAt(i))) {
                        return "";
                    }
                }
                return null;
            }

            private boolean isHexChar(char c) {
                if (c >= '0' && c <= '9') {
                    return true;
                }
                if (c >= 'a' && c <= 'f') {
                    return true;
                }
                if (c >= 'A' && c <= 'F') {
                    return true;
                }
                return false;
            }
        };
        colorEt.setFilters(
                new InputFilter[]{filter, new InputFilter.LengthFilter(8)});

        red = (SeekBar) findViewById(R.id.red);
        red.setMax(0xFF);
        red.setOnSeekBarChangeListener(onMix);

        green = (SeekBar) findViewById(R.id.green);
        green.setMax(0xFF);
        green.setOnSeekBarChangeListener(onMix);

        blue = (SeekBar) findViewById(R.id.blue);
        blue.setMax(0xFF);
        blue.setOnSeekBarChangeListener(onMix);

        alpha = (SeekBar) findViewById(R.id.alpha);
        alpha.setMax(0xFF);
        alpha.setOnSeekBarChangeListener(onMix);

        // if (attrs != null) {
        // int[] styleable = R.styleable.ColorMixer;
        // TypedArray a = getContext().obtainStyledAttributes(attrs,
        // styleable, 0, 0);
        //
        // setColor(a.getInt(R.styleable.ColorMixer_colormixer_color,
        // 0xFFA4C639));
        // a.recycle();
        // }
    }

    @Override
    public Parcelable onSaveInstanceState() {
        Bundle state = new Bundle();

        state.putParcelable(SUPERSTATE, super.onSaveInstanceState());
        state.putInt(COLOR, getColor());

        return (state);
    }

    @Override
    public void onRestoreInstanceState(Parcelable ss) {
        Bundle state = (Bundle) ss;

        super.onRestoreInstanceState(state.getParcelable(SUPERSTATE));

        setProgressBarColor(state.getInt(COLOR));
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count,
                                  int after) {
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before,
                              int count) {
    }

    // Make changes from text to progress bar
    @Override
    public void afterTextChanged(Editable s) {
        try {
            updateColorFromText();
        } catch (Exception e) {
        }
    }

    public interface OnColorChangedListener {
        public void onColorChange(int argb);
    }
}
