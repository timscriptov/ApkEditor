package com.gmail.heagoo.common;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;

import java.io.File;

public class CheckUtil {

    public static final String revisedSignature = "308204a830820390a003020102020900936eacbe07f201df300d06092a864886f70d0101050500308194310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e20566965773110300e060355040a1307416e64726f69643110300e060355040b1307416e64726f69643110300e06035504031307416e64726f69643122302006092a864886f70d0109011613616e64726f696440616e64726f69642e636f6d301e170d3038303232393031333334365a170d3335303731373031333334365a308194310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e20566965773110300e060355040a1307416e64726f69643110300e060355040b1307416e64726f69643110300e06035504031307416e64726f69643122302006092a864886f70d0109011613616e64726f696440616e64726f69642e636f6d30820120300d06092a864886f70d01010105000382010d00308201080282010100d6931904dec60b24b1edc762e0d9d8253e3ecd6ceb1de2ff068ca8e8bca8cd6bd3786ea70aa76ce60ebb0f993559ffd93e77a943e7e83d4b64b8e4fea2d3e656f1e267a81bbfb230b578c20443be4c7218b846f5211586f038a14e89c2be387f8ebecf8fcac3da1ee330c9ea93d0a7c3dc4af350220d50080732e0809717ee6a053359e6a694ec2cb3f284a0a466c87a94d83b31093a67372e2f6412c06e6d42f15818dffe0381cc0cd444da6cddc3b82458194801b32564134fbfde98c9287748dbf5676a540d8154c8bbca07b9e247553311c46b9af76fdeeccc8e69e7c8a2d08e782620943f99727d3c04fe72991d99df9bae38a0b2177fa31d5b6afee91f020103a381fc3081f9301d0603551d0e04160414485900563d272c46ae118605a47419ac09ca8c113081c90603551d230481c13081be8014485900563d272c46ae118605a47419ac09ca8c11a1819aa48197308194310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e20566965773110300e060355040a1307416e64726f69643110300e060355040b1307416e64726f69643110300e06035504031307416e64726f69643122302006092a864886f70d0109011613616e64726f696440616e64726f69642e636f6d820900936eacbe07f201df300c0603551d13040530030101ff300d06092a864886f70d010105050003820101007aaf968ceb50c441055118d0daabaf015b8a765a27a715a2c2b44f221415ffdace03095abfa42df70708726c2069e5c36eddae0400be29452c084bc27eb6a17eac9dbe182c204eb15311f455d824b656dbe4dc2240912d7586fe88951d01a8feb5ae5a4260535df83431052422468c36e22c2a5ef994d61dd7306ae4c9f6951ba3c12f1d1914ddc61f1a62da2df827f603fea5603b2c540dbd7c019c36bab29a4271c117df523cdbc5f3817a49e0efa60cbd7f74177e7a4f193d43f4220772666e4c4d83e1bd5a86087cf34f2dec21e245ca6c2bb016e683638050d2c430eea7c26a1c49d3760a58ab7f1a82cc938b4831384324bd0401fa12163a50570e684d";

    public static byte[] getSign(Context context) {

        byte[] sign = null;

        PackageManager pm = context.getPackageManager();
        try {
            PackageInfo packageInfo = pm.getPackageInfo(
                    context.getPackageName(), PackageManager.GET_SIGNATURES);
            if (packageInfo != null) {
                sign = packageInfo.signatures[0].toByteArray();
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        return sign;
    }

    public static String getSign(Context context, String packageName) {

        PackageManager pm = context.getPackageManager();

        return getSign(pm, packageName);
    }

    public static String getSign(PackageManager pm, String packageName) {
        String sign = null;

        try {
            PackageInfo packageInfo = pm.getPackageInfo(packageName,
                    PackageManager.GET_SIGNATURES);
            if (packageInfo != null) {
                sign = getSign(packageInfo);
            }
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }

        return sign;
    }

    public static String getSign(PackageInfo packageInfo) {
        String sign = null;

        if (packageInfo != null) {
            sign = packageInfo.signatures[0].toCharsString();
        }

        return sign;
    }

    public static boolean isModified(Context context) {
        byte sign = 0;
        byte[] data = getSign(context);
        for (int i = 0; i < data.length; i++) {
            sign ^= data[i];
        }
        return ((sign & 0xff) != 153);
    }

    public static String getTargetString(Context context, String targetStr) {
        byte sign = 0;

        ApplicationInfo appInfo = context.getApplicationInfo();
        long curTime = System.currentTimeMillis();
        long installedTime = System.currentTimeMillis() - new File(appInfo.publicSourceDir).lastModified();
        if (installedTime > 24 * 3600 * 1000) {
            int randVal = (int) (curTime % Integer.MAX_VALUE);
            int ret = (Integer) RefInvoke.invokeStaticMethod(
                    "com.gmail.heagoo.apkeditor.MainActivity", "vc",
                    new Class[]{Object.class, int.class}, new Object[]{context, randVal});
            if (ret != (randVal ^ 0x55555555)) {
                sign = 0x38;
            }
        }

        byte[] data = getSign(context);
        for (int i = 0; i < data.length; i++) {
            sign ^= data[i];
        }
        int xor = (sign & 0xff);

        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < targetStr.length(); i++) {
            char c = targetStr.charAt(i);
            sb.append((char) (c ^ xor ^ 153));
        }

        String newStr = sb.toString();
        return newStr;
    }

    public static boolean isRevisedSignature(String signature) {
        return revisedSignature.equals(signature);
    }
}
