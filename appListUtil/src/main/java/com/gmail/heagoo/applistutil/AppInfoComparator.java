package com.gmail.heagoo.applistutil;

import java.util.Comparator;

@SuppressWarnings("rawtypes")
public class AppInfoComparator implements Comparator {

    private static final String alphaOrder = "ByAlpha";
    private static final String timeOrder = "ByInstallTime";

    private String order;

    public AppInfoComparator() {
        this(alphaOrder);
    }

    public AppInfoComparator(String appOrder) {
        this.order = appOrder;
    }

    @Override
    public int compare(Object obj1, Object obj2) {
        AppInfo info1 = (AppInfo) obj1;
        AppInfo info2 = (AppInfo) obj2;

        if (info1.isSysApp != info2.isSysApp) {
            return info1.isSysApp ? 1 : -1;
        } else {
            // Order by installation time
            if (order.equals(timeOrder)) {
                if (info1.lastUpdateTime == info2.lastUpdateTime) {
                    return 0;
                } else {
                    return (info1.lastUpdateTime < info2.lastUpdateTime ? 1 : -1);
                }
            } else {
                return info1.appName.compareTo(info2.appName);
            }
        }
    }

}