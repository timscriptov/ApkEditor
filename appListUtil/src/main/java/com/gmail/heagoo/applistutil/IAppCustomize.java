package com.gmail.heagoo.applistutil;

import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.widget.TextView;

public interface IAppCustomize {

    public String getDetail1(AppInfo appInfo);

    public String getDetail2(AppInfo appInfo);

    public void customizeDetail1(AppInfo appInfo, TextView desc1);

    public void appLongClicked(ContextMenu menu, View v,
                               ContextMenuInfo menuInfo, AppInfo appInfo);

    public void appClicked(AppInfo appInfo);

}
