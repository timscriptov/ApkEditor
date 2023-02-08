package com.gmail.heagoo.folderlist;

import android.graphics.drawable.Drawable;

public interface IListItemProducer {

    public Drawable getFileIcon(String dirPath, FileRecord record);

    public String getDetail1(String dirPath, FileRecord record);
}
