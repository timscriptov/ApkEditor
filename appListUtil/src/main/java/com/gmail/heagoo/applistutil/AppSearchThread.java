package com.gmail.heagoo.applistutil;

import java.util.List;

public class AppSearchThread extends Thread {

    private boolean isRunning = false;

    private IAppSearch iAppSearch;
    private IConsumeSearch consumer;

    public AppSearchThread(IAppSearch iAppSearch, IConsumeSearch consumer) {
        this.iAppSearch = iAppSearch;
        this.consumer = consumer;
    }

    @Override
    public void run() {
        synchronized (this) {
            if (isRunning) {
                return;
            } else {
                isRunning = true;
            }
        }

        List<AppInfo> appList = this.iAppSearch.searchApp();
        consumer.setSearchResult(appList);
        //Log.d("TEST", "Search finished, will get icon!");

//		boolean bNewIcon = false;
//		//int newIconSize = 0;
//		for (AppInfo appInfo : appList) {
//			if (appInfo.icon == null) {
//				iAppSearch.getIcon(appInfo);
//				bNewIcon = true;
//				//newIconSize++;
//			}
//		}
//		//Log.d("TEST", "app size = " + appList.size() + ", new icon size = " + newIconSize);
//		if (bNewIcon) {
//			consumer.setSearchResult(appList);
//		}

        consumer.searchEnded();

        synchronized (this) {
            isRunning = false;
        }
    }
}
