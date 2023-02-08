package com.gmail.heagoo.apkeditor.ce;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ManifestInfo {

    // Value we concerned
    public int versionCode = -1;
    public int installLocation = -1;
    public String appName;
    public String versionName;
    public String packageName;
    public String applicationCls;
    public int minSdkVersion = -1;
    public int targetSdkVersion = -1;
    public int maxSdkVersion = -1;
    public int appNameIdx = -1;
    public int verNameIdx = -1;
    public int pkgNameIdx = -1;
    // The resource id for apk launcher
    public int launcherId = -1;
    public int appNameId = -1;
    // All the strings in string chunk
    public List<String> strings = new ArrayList<>();
    // Record all the component name indexes
    // Include all 4 classes and name in application
    public List<Integer> compNameIdxs = new ArrayList<>();
    // Name index -> component type
    // 0: activity; 1: service; 2: receiver; 3: provider
    public Map<Integer, Integer> compNameIdx2Type = new HashMap<>();
    // Self defined permission name indexes
    public Map<Integer, String> permissionIdx2Name = new HashMap<>();
    // provider/authorities name indexes
    //public Map<Integer, String> authoritiesIdx2Name = new HashMap<Integer, String>();
    public List<ProviderInfo> providerInfoList = new ArrayList<>();
    // Record all the launcher activity name indexes
    // android:label of activity should also be modified
    public List<Integer> launcherActIdxs = new ArrayList<>();
    // Record all the targetActivity name indexes in activity-alias
    // name index -> targetActivity index
    public Map<Integer, Integer> targetActivityIdxs = new HashMap<>();
    // Record all declared permissions
    public Set<String> permissions = new HashSet<>();

    public static class ProviderInfo {
        public String name;
        public String authority;
        public int nameIdx;
        public int authorityIdx;
    }
}
