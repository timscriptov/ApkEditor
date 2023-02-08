package com.gmail.heagoo.pngeditor;

import android.graphics.Bitmap;

public interface ImageEditor {
    void setParam(String name, Object value);

    Bitmap edit(Bitmap bitmap);

    boolean isModified();
}
