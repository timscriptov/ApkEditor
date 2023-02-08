package com.gmail.heagoo.common;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

public class ClipboardUtil {

    public static void copyToClipboard(Context ctx, String str) {
        // Copy to clipboard
        ClipboardManager clipboard = (ClipboardManager) ctx
                .getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("", str);
        clipboard.setPrimaryClip(clip);
    }

    public static String getText(Context ctx) {
        ClipboardManager clipboard = (ClipboardManager) ctx
                .getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) {
            return clip.getItemAt(0).coerceToText(ctx).toString();
        }
        return null;
    }
}
