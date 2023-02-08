/**
 * Copyright 2014 Ryszard Wiśniewski <brut.alll@gmail.com>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package brut.androlib.res.data;

import android.content.Context;

import com.gmail.heagoo.apkeditor.CacheManager;
import com.gmail.heagoo.apkeditor.LOGGER;
import com.gmail.heagoo.common.IOUtils;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import brut.androlib.AndrolibException;
import brut.androlib.err.UndefinedResObject;
import brut.androlib.res.data.value.ResValue;
import brut.androlib.res.decoder.ARSCDecoder;
import brut.androlib.res.util.VersionInfo;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
// Modified a lot by Pujiang
public class ResTable {

    private final Map<Integer, ResPackage> mPackagesById = new HashMap<Integer, ResPackage>();
    private final Map<String, ResPackage> mPackagesByName = new HashMap<String, ResPackage>();
    private final Set<ResPackage> mMainPackages = new LinkedHashSet<ResPackage>();
    private final Set<ResPackage> mFramePackages = new LinkedHashSet<ResPackage>();

    private String mPackageRenamed;
    private String mPackageOriginal;
    private int mPackageId;
    private boolean mAnalysisMode = false;
    private boolean mSharedLibrary = false;
    private boolean mSparseResources = false;

    private Map<String, String> mSdkInfo = new LinkedHashMap<>();
    private VersionInfo mVersionInfo = new VersionInfo();

    private String mFrameTag;
    private Context ctx;

    private boolean mApkProtected = false;

    public ResTable() {
    }

    public ResTable(Context ctx, boolean apkProtected) {
        this.ctx = ctx;
        this.mApkProtected = apkProtected;
        // mAndRes = andRes;
    }

    public ResResSpec getResSpec(int resID) throws AndrolibException {
        // The pkgId is 0x00. That means a shared library is using its
        // own resource, so lie to the caller replacing with its own
        // packageId
        if (resID >> 24 == 0) {
            int pkgId = (mPackageId == 0 ? 2 : mPackageId);
            resID = (0xFF000000 & (pkgId << 24)) | resID;
        }
        return getResSpec(new ResID(resID));
    }

    public ResResSpec getResSpec(ResID resID) throws AndrolibException {
        // return getPackage(resID.package_).getResSpec(resID);
        ResPackage pkg = getPackage(resID.package_);
        if (pkg != null) {
            return pkg.getResSpec(resID);
        }

        return null;
    }

    public Set<ResPackage> listMainPackages() {
        return mMainPackages;
    }

    // public Set<ResPackage> listFramePackages() {
    // return mFramePackages;
    // }

    // public ResPackage getPackage(int id) throws AndrolibException {
    // ResPackage pkg = mPackagesById.get(id);
    // if (pkg != null) {
    // return pkg;
    // }
    // throw new UndefinedResObject(String.format("package: id=%d", id));
    // }

    // public ResPackage getPackage(String name) throws AndrolibException {
    // ResPackage pkg = mPackagesByName.get(name);
    // if (pkg == null) {
    // throw new UndefinedResObject("package: name=" + name);
    // }
    // return pkg;
    // }

    public ResPackage getPackage(String name) throws AndrolibException {
        ResPackage pkg = mPackagesByName.get(name);
        if (pkg == null) {
            throw new UndefinedResObject("undefined package: name=" + name);
        }
        return pkg;
    }

    public ResPackage getPackage(int id) throws AndrolibException {
        ResPackage pkg = mPackagesById.get(id);
        if (pkg != null) {
            return pkg;
        }

        // return loadFrameworkPkg(this, id, mFrameTag);
        if (mPackagesById.get(1) == null) {
            try {
                return loadFrameworkPkg(this, id, mFrameTag);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // LOGGER.info("id=" + id + " not found!");
        // StackTraceElement elems[] = Thread.currentThread().getStackTrace();
        // for (StackTraceElement e : elems) {
        // LOGGER.info(e.toString());
        // }

        return null;
    }

    private ResPackage[] getResPackagesFromApk(ResTable resTable,
                                               boolean keepBroken) throws AndrolibException {
        File fileDir = ctx.getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        try {
            File f = new File(rootDirectory + "/bin/resources.arsc");
            FileInputStream fis = new FileInputStream(f);
            ByteArrayInputStream ais = null;
            try {
                int size = (int) f.length();
                byte[] data = new byte[size];
                IOUtils.readFully(fis, data);
                ais = new ByteArrayInputStream(data);
            } catch (Throwable t) {
            }

            // Framework is not protected?
//            ResPackage[] result = ARSCDecoder.decode((ais != null) ? ais : fis,
//                    false, keepBroken, resTable, null, mApkProtected).getPackages();
            ResPackage[] result = ARSCDecoder.decode((ais != null) ? ais : fis,
                    false, keepBroken, resTable, null, false).getPackages();
            if (ais != null) {
                try {
                    ais.close();
                } catch (Exception e) {
                }
            }
            try {
                fis.close();
            } catch (Exception e) {
            }

            return result;
        } catch (IOException ex) {
            throw new AndrolibException("Could not load resources.arsc", ex);
        }
    }

    public ResPackage loadFrameworkPkg(ResTable resTable, int id,
                                       String frameTag) throws AndrolibException {
        LOGGER.info("*************************************id=" + id);
        // Try to get from cache
        if (id == 1) {
            ResPackage pkg = CacheManager.instance().getFrameworkPackage();
            if (pkg != null) {
                resTable.addPackage(pkg, false);
                LOGGER.info("Do not need to parse framework again!");
                return pkg;
            }
        }

        long startTime = System.currentTimeMillis();

        LOGGER.info("Loading resource table from resources.arsc");
        ResPackage[] pkgs = getResPackagesFromApk(resTable, true);

        if (pkgs.length != 1) {
            throw new AndrolibException(
                    "Arsc files with zero or multiple packages");
        }

        ResPackage pkg = pkgs[0];
        if (pkg.getId() != id) {
            throw new AndrolibException("Expected pkg of id: "
                    + String.valueOf(id) + ", got: " + pkg.getId());
        }

        resTable.addPackage(pkg, false);
        long endTime = System.currentTimeMillis();
        LOGGER.info("Loaded. Time=" + (endTime - startTime));

        // Pujiang
        // try {
        // FileOutputStream fos = new FileOutputStream(
        // "/storage/emulated/0/framework.txt");
        // pkg.dumpSpecs(fos);
        // fos.close();
        // } catch (Exception e) {
        // }

        CacheManager.instance().setFrameworkPackage(pkg);
        return pkg;
    }

    public void setFrameTag(String tag) {
        mFrameTag = tag;
    }

    public ResPackage getHighestSpecPackage() throws AndrolibException {
        int id = 0;
        int value = 0;
        for (ResPackage resPackage : mPackagesById.values()) {
            if (resPackage.getResSpecCount() > value && !resPackage.getName().equalsIgnoreCase("android")) {
                value = resPackage.getResSpecCount();
                id = resPackage.getId();
            }
        }
        // if id is still 0, we only have one pkgId which is "android" -> 1
        return (id == 0) ? getPackage(1) : getPackage(id);
    }

    public ResPackage getCurrentResPackage() throws AndrolibException {
        ResPackage pkg = mPackagesById.get(mPackageId);

        if (pkg != null) {
            return pkg;
        } else {
            if (mMainPackages.size() == 1) {
                return mMainPackages.iterator().next();
            }
            return getHighestSpecPackage();
        }
    }

    public boolean hasPackage(int id) {
        return mPackagesById.containsKey(id);
    }

    public boolean hasPackage(String name) {
        return mPackagesByName.containsKey(name);
    }

    public ResValue getValue(String package_, String type, String name)
            throws AndrolibException {
        return getPackage(package_).getType(type).getResSpec(name)
                .getDefaultResource().getValue();
    }

    public void addPackage(ResPackage pkg, boolean main)
            throws AndrolibException {
        Integer id = pkg.getId();
        if (mPackagesById.containsKey(id)) {
            throw new AndrolibException("Multiple packages: id="
                    + id.toString());
        }
        String name = pkg.getName();
        if (mPackagesByName.containsKey(name)) {
            throw new AndrolibException("Multiple packages: name=" + name);
        }

        mPackagesById.put(id, pkg);
        mPackagesByName.put(name, pkg);
        if (main) {
            mMainPackages.add(pkg);
        } else {
            mFramePackages.add(pkg);
        }
    }

    public void clearSdkInfo() {
        mSdkInfo.clear();
    }

    public void addSdkInfo(String key, String value) {
        mSdkInfo.put(key, value);
    }

    public void setVersionName(String versionName) {
        mVersionInfo.versionName = versionName;
    }

    public void setVersionCode(String versionCode) {
        mVersionInfo.versionCode = versionCode;
    }

    public VersionInfo getVersionInfo() {
        return mVersionInfo;
    }

    public Map<String, String> getSdkInfo() {
        return mSdkInfo;
    }

    public boolean getAnalysisMode() {
        return mAnalysisMode;
    }

    public void setAnalysisMode(boolean mode) {
        mAnalysisMode = mode;
    }

    public String getPackageRenamed() {
        return mPackageRenamed;
    }

    public void setPackageRenamed(String pkg) {
        mPackageRenamed = pkg;
    }

    public String getPackageOriginal() {
        return mPackageOriginal;
    }

    public void setPackageOriginal(String pkg) {
        mPackageOriginal = pkg;
    }

    public int getPackageId() {
        return mPackageId;
    }

    public void setPackageId(int id) {
        mPackageId = id;
    }

    public boolean getSharedLibrary() {
        return mSharedLibrary;
    }

    public void setSharedLibrary(boolean flag) {
        mSharedLibrary = flag;
    }

    public boolean getSparseResources() {
        return mSparseResources;
    }

    public void setSparseResources(boolean flag) {
        mSparseResources = flag;
    }
}
