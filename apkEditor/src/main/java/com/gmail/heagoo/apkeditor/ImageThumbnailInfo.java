package com.gmail.heagoo.apkeditor;

import android.graphics.Bitmap;

public class ImageThumbnailInfo {

    public Bitmap thumbnail;
    // Detail info may show the resolution
    public String detailInfo;

    public ImageThumbnailInfo(Bitmap thumbnail, String info) {
        this.thumbnail = thumbnail;
        this.detailInfo = info;
    }
}
