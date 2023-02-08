package com.gmail.heagoo.apkeditor.pro;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.gmail.heagoo.apkeditor.GlobalConfig;
import com.gmail.heagoo.appdm.PrefOverallActivity;

public class appdm {

    public static void de(Context ctx, String pkgName) {
        Intent prefIntent = new Intent(ctx, PrefOverallActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString("packagePath", pkgName);
        bundle.putBoolean("backup", false);
        bundle.putInt("themeId", GlobalConfig.instance(ctx).getThemeId());
        prefIntent.putExtras(bundle);
        ctx.startActivity(prefIntent);
    }
}
