package apkeditor;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.Date;

public class Utils {

    private static final String TAG = "APKEDITOR";

    public static void showToast(Context ctx, String msg) {
        Toast.makeText(ctx, msg, Toast.LENGTH_LONG).show();
    }

    public static void log(String msg) {
        Log.d(TAG, msg);
    }

    public static void dumpValue(Object obj) {
        if (obj != null) {
            log("Values of " + obj + ":");
            getAllValues(obj, 1, 3);
        } else {
            log("null");
        }
    }

    public static String stringAdd1(String str) {
        if (str != null && !str.equals("")) {
            char c = str.charAt(str.length() - 1);
            c += 1;
            return str.substring(0, str.length() - 1) + c;
        }
        return str;
    }

    public static String generateImei(int offset) {
        long base = 35806501910400L;
        long imei = base + offset;
        int checksum = imei_checksum(imei);
        return String.valueOf(imei * 10 + checksum);
    }

    private static int imei_checksum(long imei) {
        int[] sum = new int[15];
        long mod = 10;
        for (int i = 1; i <= 14; i++) {
            sum[i] = (int) (imei % mod);
            if (i % 2 != 0) {
                sum[i] *= 2;
            }
            if (sum[i] >= 10) {
                sum[i] = sum[i] % 10 + (sum[i] / 10);
            }
            imei /= mod;
        }

        int check = 0;
        for (int i = 0; i < sum.length; i++) {
            check += sum[i];
        }
        return (check * 9) % 10;
    }

    private static String getValueString(Object value) {
        if (value instanceof String[]) {
            String[] strArray = (String[]) value;
            StringBuilder sb = new StringBuilder();
            sb.append("String[]={");
            for (int i = 0; i < strArray.length; i++) {
                sb.append("" + i + ":" + strArray[i] + ", ");
            }
            sb.append("}");
            return sb.toString();
        } else if (value instanceof Integer[]) {
            Integer[] intArray = (Integer[]) value;
            StringBuilder sb = new StringBuilder();
            sb.append("Integer[]={");
            for (int i = 0; i < intArray.length; i++) {
                sb.append("" + i + ":" + intArray[i] + ", ");
            }
            sb.append("}");
            return sb.toString();
        }
        return value == null ? "null" : value.toString();
    }

    private static String getPadding(int level) {
        String[] buffers = {"", "  ", "    ", "      ", "        ",
                "          ", "            ", "              ",
                "                ", "                  ",
                "                    "};
        if (level < buffers.length) {
            return buffers[level];
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < level; ++i) {
            sb.append("  ");
        }

        return sb.toString();
    }

    private static void getAllValues(Object obj, int level, int maxlevel) {

        Field[] fields = obj.getClass().getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {

            if (!fields[i].isAccessible()) {
                fields[i].setAccessible(true);
            }

            try {
                Object value = fields[i].get(obj);
                log(getPadding(level) + "Name: " + fields[i].getName()
                        + ", Value: " + getValueString(value));
                if (level < maxlevel && value != null && !isBasicType(value)) {
                    getAllValues(value, level + 1, maxlevel);
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isBasicType(Object param) {
        if (param instanceof Integer) {
            return true;
        } else if (param instanceof String) {
            return true;
        } else if (param instanceof Double) {
            return true;
        } else if (param instanceof Float) {
            return true;
        } else if (param instanceof Long) {
            return true;
        } else if (param instanceof Boolean) {
            return true;
        } else if (param instanceof Date) {
            return true;
        } else if (param instanceof Integer[]) {
            return true;
        } else if (param instanceof String[]) {
            return true;
        } else if (param instanceof Double[]) {
            return true;
        } else if (param instanceof Float[]) {
            return true;
        } else if (param instanceof Long[]) {
            return true;
        } else if (param instanceof Boolean[]) {
            return true;
        } else if (param instanceof Date[]) {
            return true;
        }
        return false;
    }

    public static void printCallStack() {
        printCallStack(null);
    }

    public static void printCallStack(String tag) {
        if (tag != null) {
            log("Stack at " + tag + ": ");
        } else {
            log("Stack:");
        }
        Throwable ex = new Throwable();
        StackTraceElement[] stackElements = ex.getStackTrace();
        if (stackElements != null) {
            for (int i = 0; i < stackElements.length; i++) {
                log("\t" + stackElements[i].toString());
            }
        }
    }
}
