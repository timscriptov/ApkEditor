package com.gmail.heagoo.apkeditor.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.preference.PreferenceManager;

import com.gmail.heagoo.apkeditor.SettingActivity;
import com.gmail.heagoo.apklib.sign.SignApk;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

public class SignHelper {

    public static void sign(Context ctx, String sourceApkPath,
                            String targetApkPath, Map<String, String> replacedFiles,
                            Map<String, String> addedFiles, Set<String> deletedFiles)
            throws IOException {
        AssetManager am = ctx.getAssets();
        int level = SettingActivity.getCompressionLevel(ctx);
        // String keyFile = "testkey";
        String keyName = SettingActivity.getSignKeyName(ctx);

        // Custom Key (keys are from file)
        if (keyName.charAt(0) == 'c' && keyName.charAt(1) == 'u') {
            SharedPreferences sp = PreferenceManager
                    .getDefaultSharedPreferences(ctx);
            String privKeyPath = sp.getString(
                    SettingActivity.STR_PRIVATEKEYPATH, "");
            String pubKeyPath = sp.getString(SettingActivity.STR_PUBLICKEYPATH,
                    "");
            InputStream publicKeyInput = new FileInputStream(pubKeyPath);
            InputStream privateKeyInput = new FileInputStream(privKeyPath);
            SignApk.signAPK(publicKeyInput, privateKeyInput, sourceApkPath,
                    targetApkPath, addedFiles, deletedFiles, replacedFiles,
                    level);
        }
        // Keys are in assets
        else {
            // Проверка подписи SaWSeM
            String keyFile = "testkey";
            //  String keyFile = CheckUtil.getTargetString(ctx, keyName);
            InputStream publicKeyInput = am.open(keyFile + ".x509.pem");
            InputStream privateKeyInput = am.open(keyFile + ".pk8");
            SignApk.signAPK(publicKeyInput, privateKeyInput, sourceApkPath,
                    targetApkPath, addedFiles, deletedFiles, replacedFiles,
                    level);
        }
    }
}
