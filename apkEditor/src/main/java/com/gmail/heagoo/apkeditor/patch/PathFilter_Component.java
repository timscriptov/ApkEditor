package com.gmail.heagoo.apkeditor.patch;

import java.io.File;
import java.util.List;

// Support "[APPLICATION]" "[ACTIVITIES]" "LAUNCHER_ACTIVITIES"
public class PathFilter_Component extends PathFilter {

    private ComponentType compType;
    private String decodeRootPath;
    private String applicationName;
    private List<String> componentList;
    private int cursor = 0;

    public PathFilter_Component(IPatchContext ctx, ComponentType compType) {
        this.compType = compType;
        this.decodeRootPath = ctx.getDecodeRootPath();
        switch (compType) {
            case APPLICATION:
                this.applicationName = ctx.getApplication();
                break;
            case ACTIVITY:
                this.componentList = ctx.getActivities();
                break;
            case LAUNCHER_ACTIVITY:
                this.componentList = ctx.getLauncherActivities();
                break;
        }
    }

    @Override
    public String getNextEntry() {
        switch (compType) {
            case APPLICATION:
                if (cursor == 0) {
                    cursor += 1;
                    return getSmaliPath(applicationName);
                }
                break;
            case ACTIVITY:
            case LAUNCHER_ACTIVITY:
                if (cursor < this.componentList.size()) {
                    return getSmaliPath(componentList.get(cursor++));
                }
                break;
        }

        return null;
    }

    private String getSmaliPath(String clsName) {
        String path = getRelativePath("smali", clsName, true);

        int index = 2;
        while (path == null && index < 8) {
            path = getRelativePath("smali_classes" + index, clsName, true);
            index += 1;
        }

        // When path is still null, return the default one
        if (path == null) {
            path = getRelativePath("smali", clsName, false);
        }

        return path;
    }

    private String getRelativePath(String smaliFolderName, String clsName, boolean notExistRetNull) {
        StringBuilder sb = new StringBuilder();
        sb.append(smaliFolderName);
        sb.append("/");
        sb.append(clsName.replaceAll("\\.", "/"));
        sb.append(".smali");
        String relativePath = sb.toString();
        String absolutionPath = decodeRootPath + "/" + relativePath;
        if (notExistRetNull) {
            return new File(absolutionPath).exists() ? relativePath : null;
        } else {
            return relativePath;
        }
    }

    @Override
    public boolean isTarget(String entryPath) {
        int pos = entryPath.indexOf('/');
        if (pos != -1 && entryPath.endsWith(".smali")) {
            String str = entryPath.substring(pos + 1, entryPath.length() - 6);
            String clsName = str.replaceAll("/", ".");

            switch (compType) {
                case APPLICATION:
                    return clsName.equals(applicationName);
                case ACTIVITY:
                case LAUNCHER_ACTIVITY:
                    return componentList.contains(clsName);
            }
        }

        return false;
    }

    @Override
    public boolean isSmaliNeeded() {
        return true;
    }

    @Override
    public boolean isWildMatch() {
        return false;
    }

    enum ComponentType {
        APPLICATION,
        ACTIVITY,
        LAUNCHER_ACTIVITY
    }
}
