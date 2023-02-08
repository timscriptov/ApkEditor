package com.gmail.heagoo.apkeditor.patch;

import java.util.List;

public interface IPatchContext {

    // Return decoded root path, like "/data/data/.../files/decoded"
    public String getDecodeRootPath();

    // Return all the smali folders like "smali", "smali_classes2"
    public List<String> getSmaliFolders();

    // Get application name, like "com.gmail.MyApplication"
    public String getApplication();

    // Get all the activities
    List<String> getActivities();

    // Get all the launcher activities
    List<String> getLauncherActivities();

    // Get name list for all the patches
    public List<String> getPatchNames();

    // In general, msg does not contain '\n'
    public void info(int resourceId, boolean bBold, Object... args);

    public void info(String format, boolean bBold, Object... args);

    public void error(int resourceId, Object... args);

    public void patchFinished();

    public String getString(int stringId);

    // Set/Get value for global parameter
    public void setVariableValue(String key, String value);

    public String getVariableValue(String key);

}
