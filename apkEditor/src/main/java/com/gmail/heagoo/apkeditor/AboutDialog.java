package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;

import java.lang.ref.WeakReference;

public class AboutDialog extends Dialog implements View.OnClickListener {
    private final WeakReference<Activity> activityRef;

    @SuppressLint("InflateParams")
    public AboutDialog(Activity activity) {
        super(activity, R.style.Dialog_No_Border_2);
        this.activityRef = new WeakReference<>(activity);

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LayoutInflater inflater = LayoutInflater.from(activity);
        View layout = inflater.inflate(R.layout.dlg_about, null);
        super.setContentView(layout);

        Drawable icon = activity.getApplicationInfo().loadIcon(activity.getPackageManager());
        ImageView iv = (ImageView) layout.findViewById(R.id.icon);
        iv.setImageDrawable(icon);

        Button getBtn = (Button) layout.findViewById(R.id.btn_get_pro);
        getBtn.setOnClickListener(this);

        Button tg = (Button) layout.findViewById(R.id.telegram);
        tg.setOnClickListener(v -> {
            final String url = "https://t.me/apkeditorproofficial";
            final Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            activity.startActivity(i);
        });

        layout.findViewById(R.id.btn_close).setOnClickListener(this);

        try {
            PackageInfo pInfo = activity.getPackageManager()
                    .getPackageInfo(activity.getPackageName(), 0);
            TextView tv = (TextView) layout.findViewById(R.id.tv_version);
            tv.setText("Version " + pInfo.versionName);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Versions for depended part
        TextView depVer = (TextView) layout.findViewById(R.id.tv_dep_version);
        boolean upgraded = MainActivity.upgradedFromOldVersion(activity);
        boolean fromPlay = false;
        String pkgName = activity.getPackageName();
        String installer = activity.getPackageManager().getInstallerPackageName(pkgName);
        if (installer != null && installer.endsWith(".vending")) {
            fromPlay = true;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("smali: v2.4.0\nandroid.jar: Android API 28");
        if (!upgraded && fromPlay) { // Cannot use it and installed from google play
            sb.append("\n\nIf you cannot use, please send email to timscriptov@gmail.com with a title of \"VERSION 1.9 ISSUES\"");
        }
        depVer.setText(sb.toString());

        if (BuildConfig.IS_PRO) {
            getBtn.setVisibility(View.GONE);
            layout.findViewById(R.id.textView2).setVisibility(View.GONE);
            layout.findViewById(R.id.textView3).setVisibility(View.GONE);
        }

        if (BuildConfig.PARSER_ONLY) {
            layout.findViewById(R.id.textView2).setVisibility(View.GONE);
            layout.findViewById(R.id.textView3).setVisibility(View.GONE);
        }

        // Set the width, as too small if not
        DisplayMetrics metrics = activity.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        getWindow().setLayout((7 * width) / 8, LayoutParams.WRAP_CONTENT);
    }

    // To view/download the pro version
    protected static void viewProVersion(Context ctx) {
        String pkgName = ctx.getPackageName() + ".pro";
        Uri uri = Uri.parse("market://details?id=" + pkgName);
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        try {
            ctx.startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            ctx.startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id="
                            + pkgName)));
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btn_get_pro) {
            viewProVersion(activityRef.get());
        } else if (id == R.id.btn_close) {
            this.dismiss();
        }
    }
}
