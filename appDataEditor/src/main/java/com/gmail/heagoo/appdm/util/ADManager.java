package com.gmail.heagoo.appdm.util;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.LinearLayout;

import com.gmail.heagoo.common.RefInvoke;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Hashtable;
import java.util.Map;
import java.util.Random;

//import com.gmail.heagoo.appdm.R;

public class ADManager {

    private static Map<String, Integer> serverDefinedP = new Hashtable<String, Integer>();
    private static Random random = new Random(System.currentTimeMillis());
    private Object adView;

    ;
    private AdSource adSource;

    private WeakReference<Activity> activityRef;

    private Object adHelper;

    private ADManager(Activity ctx, int layoutId) {
        this.activityRef = new WeakReference<Activity>(ctx);
        // boolean paidAppExist = PaidAppsChecker.isPaidAppsExist(ctx);
        // if (!paidAppExist) {
        // SharedPreferences sp = PreferenceManager
        // .getDefaultSharedPreferences(activityRef.get());
        // long lastReqTime = sp.getLong("last_req_time", 0);
        // if (System.currentTimeMillis() > lastReqTime + 24 * 3600 * 1000) {
        // gotControlFromServer();
        // }
        // }

        // String pkgName = ctx.getPackageName();
        // isAdmob = !pkgName.contains("amazon");
        dicideAdSource();

        String clsName = getHelperClass();
        Class<?>[] paramTypes = {Activity.class, int.class};
        Object[] paramValues = {ctx, layoutId};

        this.adHelper = RefInvoke.createInstance(clsName, null, null);
        if (adHelper != null) {
            RefInvoke.invokeMethod(clsName, "init", adHelper, paramTypes,
                    paramValues);
        }
    }

    public static ADManager init(Activity ctx, int layoutId) {
        ADManager mgr = new ADManager(ctx, layoutId);
        return mgr;
    }

    private String getHelperClass() {
        String clsName = null;
        switch (adSource) {
            case admob:
                clsName = "com.gmail.heagoo.appdm.admob.ADHelper";
                break;
            case amazon:
                clsName = "com.gmail.heagoo.appdm.free.AmazonHelper";
                break;
            case startapp:
                clsName = "com.gmail.heagoo.appdm.free.AppstartHelper";
                break;
            case epom:
                clsName = "com.gmail.heagoo.appdm.epom.ADHelper";
                break;
            case inmobi:
                clsName = "com.gmail.heagoo.appdm.inmobi.ADHelper";
                break;
            case facebook:
                clsName = "com.gmail.heagoo.appdm.free.FacebookHelper";
                break;
            default:
                break;
        }
        return clsName;
    }

    private void dicideAdSource() {
        try {
            Class.forName("com.gmail.heagoo.appdm.free.AmazonHelper");
        } catch (ClassNotFoundException e) {
            this.adSource = AdSource.none;
            return;
        }

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(activityRef.get());
        int amazonRatio = 60;

        int randVal = random.nextInt(100) % 10;
        if (randVal < amazonRatio / 10) {
            this.adSource = AdSource.amazon;
//          Log.d("DEBUG", "decided to use amazon, randVal=" + randVal
//          + ", amazonRatio=" + amazonRatio);
        } else if (randVal < 90) {
            this.adSource = AdSource.facebook;
            //Log.d("DEBUG", "decided to use facebook, " + randVal);
        } else {
            this.adSource = AdSource.startapp;
            //Log.d("DEBUG", "decided to use startapp, " + randVal);
        }
    }

    private void gotControlFromServer() {
        new Thread() {
            @Override
            public void run() {
                StringBuffer sb = new StringBuffer();
                try {
                    java.net.URL url = new java.net.URL(
                            "http://hack-app-data.appspot.com/html/control.htm");
                    BufferedReader in = new BufferedReader(
                            new InputStreamReader(url.openStream()));
                    String line;
                    while ((line = in.readLine()) != null) {
                        sb.append(line);
                    }
                    in.close();

                    // Parse the content and put it to map
                    String content = sb.toString();
                    String[] segments = content.split(";");
                    for (String seg : segments) {
                        String[] words = seg.split("=");
                        if (words.length == 2) {
                            try {
                                Integer ratio = Integer.valueOf(words[1]);
                                serverDefinedP.put(words[0], ratio);
                            } catch (Exception e) {
                            }
                        }
                    }

                    if (!serverDefinedP.isEmpty()) {
                        saveRatioInfo2File(serverDefinedP);
                    }
                } catch (Exception e) { // Report any errors that arise
                }
            }
        }.start();
    }

    protected void saveRatioInfo2File(Map<String, Integer> adRatios) {
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(activityRef.get());
        Editor editor = sp.edit();
        for (Map.Entry<String, Integer> entry : adRatios.entrySet()) {
            editor.putInt(entry.getKey() + "_ratio", entry.getValue());
        }
        editor.putLong("last_req_time", System.currentTimeMillis());
        editor.commit();
    }

    // Only for admob
    private void createAdView(Activity ctx, String unitId, int layoutId) {
        try {
            // // Create adView
            // this.adView = new AdView(ctx);
            // adView.setAdUnitId(unitId);
            // adView.setAdSize(AdSize.BANNER);
            //
            // LinearLayout layout = (LinearLayout) ctx
            // .findViewById(R.id.adViewLayout);
            //
            // // Add adView
            // layout.addView(adView);
            //
            // AdRequest adRequest = new AdRequest.Builder().build();
            //
            // adView.loadAd(adRequest);

            // Use reflection
            // adView = new AdView(ctx);
            Class<?>[] paramTypes = {Context.class};
            Object[] paramValues = {ctx};
            this.adView = RefInvoke.createInstance(
                    "com.google.android.gms.ads.AdView", paramTypes,
                    paramValues);

            // adView.setAdUnitId(unitId);
            RefInvoke.invokeMethod("com.google.android.gms.ads.AdView",
                    "setAdUnitId", adView, new Class[]{String.class},
                    new Object[]{unitId});

            // adView.setAdSize(AdSize.BANNER);
            Object banner = RefInvoke.getStaticFieldOjbect(
                    "com.google.android.gms.ads.AdSize", "BANNER");
            RefInvoke.invokeMethod("com.google.android.gms.ads.AdView",
                    "setAdSize", adView, new Class[]{banner.getClass()},
                    new Object[]{banner});

            LinearLayout layout = (LinearLayout) ctx.findViewById(layoutId);
            layout.addView((View) adView);

            // AdRequest adRequest = new AdRequest.Builder().build();
            Object builder = Class
                    .forName("com.google.android.gms.ads.AdRequest$Builder")
                    .newInstance();
            Object adRequest = RefInvoke.invokeMethod(
                    "com.google.android.gms.ads.AdRequest$Builder", "build",
                    builder, new Class[]{}, new Object[]{});

            // adView.loadAd(adRequest);
            RefInvoke.invokeMethod("com.google.android.gms.ads.AdView",
                    "loadAd", adView, new Class[]{adRequest.getClass()},
                    new Object[]{adRequest});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void revokeNoParamMethod(String methodName) {
        Class<?>[] paramTypes = {};
        Object[] paramValues = {};
        String clsName = getHelperClass();
        RefInvoke.invokeMethod(clsName, methodName, adHelper, paramTypes,
                paramValues);
    }

    public void pause() {
        if (adHelper != null) {
            revokeNoParamMethod("pause");
        }
    }

    public void resume() {
        if (adHelper != null) {
            revokeNoParamMethod("resume");
        }
    }

    public void destroy() {
        if (adHelper != null) {
            revokeNoParamMethod("destroy");
        }
    }

    static enum AdSource {
        admob, amazon, startapp, epom, inmobi, facebook, none
    }

}
