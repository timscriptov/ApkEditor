package com.gmail.heagoo.apkeditor.util;

import android.app.Activity;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.ApkComposeActivity;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.ProcessingDialog;
import com.gmail.heagoo.common.RandomUtil;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

class ResourceRename {
    String resourceType;
    String resourceName;
    String newResourceName;

    public ResourceRename(String type, String name, String newName) {
        this.resourceType = type;
        this.resourceName = name;
        this.newResourceName = newName;
    }

    public String toString() {
        return "type: " + resourceType
                + ", name: " + resourceName
                + ", newName: " + newResourceName;
    }
}

public class ErrorFixManager {

    public static final int FIXER_INVALID_FILENAME = 0;
    public static final int FIXER_INVALID_TOKEN = 1;
    public static final int FIXER_INVALID_ATTR = 2;
    public static final int FIXER_INVALID_SYMBOL = 3;
    public static final int FIXER_ERROR_EQUIVALENT = 4;
    // Record all the string replaces
    Map<String, String> allReplaces = new HashMap<String, String>();
    // Record replaces for each file
    Map<String, Map<String, String>> fileReplaces = new HashMap<>();
    private String decodeRootPath;
    private String errMessage;
    private FixInvalid fixer;
    private int fixerId = -1;

    public ErrorFixManager(String decodeRootPath) {
        this.decodeRootPath = decodeRootPath;
    }

    // token is an invalid token
    public static String makeValidToken(String token) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (Character.isDigit(c) || (c >= 'a' && c <= 'z')
                    || (c >= 'A' && c <= 'Z') || c == '_' || c == '.') {
                sb.append(c);
            } else {
                String str = RandomUtil.getRandomString(4);
                sb.append(str);
            }
        }
        return sb.toString();
    }

    protected static void closeQuietly(BufferedWriter c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }

    public void setErrMessage(String errMessage) {
        this.errMessage = errMessage;
    }

    public void fixErrors(final Activity activity) {
        if (fixer != null) {
            new ProcessingDialog(activity,
                    new ProcessingDialog.ProcessingInterface() {
                        @Override
                        public void process() throws Exception {
                            fixer.fixErrors();
                            addReplaces(fixer.getAxmlModifications());
                        }

                        @Override
                        public void afterProcess() {
                            if (fixer.succeeded()) {
                                String msg = fixer.getMofifyMessage(activity);
                                Toast.makeText(activity, msg,
                                        Toast.LENGTH_SHORT).show();
                                ((ApkComposeActivity) activity).buildAgain();
                            } else {
                                Toast.makeText(activity,
                                        R.string.str_fix_failed,
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, -1).show();
        }
    }

    protected void addReplaces(Map<String, Map<String, String>> m) {
        if (m == null) {
            return;
        }

        for (Map.Entry<String, Map<String, String>> entry : m.entrySet()) {
            String filePath = entry.getKey();
            Map<String, String> rec = this.fileReplaces.get(filePath);
            if (rec == null) {
                this.fileReplaces.put(filePath, entry.getValue());
            } else {
                rec.putAll(entry.getValue());
            }
        }
    }

    public boolean isErrorFixable() {
        if (errMessage == null) {
            return false;
        }

        fixer = new FixInvalidFileName(decodeRootPath, errMessage);
        if (fixer.isErrorFixable()) {
            fixerId = FIXER_INVALID_FILENAME;
            return true;
        }

        fixer = new FixInvalidToken(decodeRootPath, errMessage);
        if (fixer.isErrorFixable()) {
            fixerId = FIXER_INVALID_TOKEN;
            return true;
        }

        fixer = new FixInvalidAttribute(decodeRootPath, errMessage,
                allReplaces);
        if (fixer.isErrorFixable()) {
            fixerId = FIXER_INVALID_ATTR;
            return true;
        }

        fixer = new FixInvalidSymbol(decodeRootPath, errMessage);
        if (fixer.isErrorFixable()) {
            fixerId = FIXER_INVALID_SYMBOL;
            return true;
        }

        fixer = new FixInvalidEquivalent(decodeRootPath, errMessage);
        if (fixer.isErrorFixable()) {
            fixerId = FIXER_ERROR_EQUIVALENT;
            return true;
        }

        return false;
    }

    public int getFixerId() {
        return fixerId;
    }

    // Get all the modifications
    public Map<String, Map<String, String>> getModifications() {
        return fileReplaces;
    }
}
