package com.gmail.heagoo.appdm;

import android.view.View.OnClickListener;

public class BasicInfoItem {

    public String title;
    public String value;
    public String opName;
    public OnClickListener listener;

    public BasicInfoItem(String title, String value) {
        this.title = title;
        this.value = value;
    }

    public BasicInfoItem(String title, String value, String opName,
                         OnClickListener listener) {
        this.title = title;
        this.value = value;
        this.opName = opName;
        this.listener = listener;
    }
}
