package com.gmail.heagoo.appdm.util;

import android.content.Context;

import com.gmail.heagoo.common.RefInvoke;

public class InterestAdManager {

    private Object interestAd;

    public InterestAdManager(Context ctx) {
        try {
            this.interestAd = RefInvoke.createInstance(
                    "com.gmail.heagoo.appdm.free.InterestAdManager",
                    new Class<?>[]{Context.class, boolean.class},
                    new Object[]{ctx, false});
        } catch (Exception e) {
        }
    }

    public void onResume() {
        RefInvoke.invokeMethod("com.gmail.heagoo.appdm.free.InterestAdManager",
                "onResume", interestAd, null, null);
    }

    public void onPause() {
        RefInvoke.invokeMethod("com.gmail.heagoo.appdm.free.InterestAdManager",
                "onPause", interestAd, null, null);
    }

    public void show() {
        RefInvoke.invokeMethod("com.gmail.heagoo.appdm.free.InterestAdManager",
                "showInterstitial", interestAd, null, null);

    }
}
