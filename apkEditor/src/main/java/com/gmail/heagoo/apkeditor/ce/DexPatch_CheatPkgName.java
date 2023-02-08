package com.gmail.heagoo.apkeditor.ce;

import android.content.Context;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Map;

public class DexPatch_CheatPkgName implements IApkMaking, Serializable {

    private static final long serialVersionUID = -1398699745447046067L;
    private String applicationCls;
    private String originPackageName;
    private String smaliAppCls; // like "Lcom/gmail/ProxyApplication;"

    // applicationClass is null if there is no Application in the target app
    public DexPatch_CheatPkgName(String applicationClass, String oldPkgName) {
        this.applicationCls = applicationClass;
        this.originPackageName = oldPkgName;
        if (applicationClass != null) {
            this.smaliAppCls = "L" + applicationClass.replaceAll("\\.", "/")
                    + ";";
        } else {
            // TODO: create a new application
        }
    }

    @Override
    public void prepareReplaces(Context ctx, String apkFilePath,
                                Map<String, String> allReplaces, IDescriptionUpdate updater)
            throws Exception {
        File fileDir = ctx.getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        String smaliRootPath = rootDirectory + "/decoded/smali/";

        if (this.applicationCls != null) {
            patchApplication(ctx,
                    smaliRootPath + applicationCls.replaceAll("\\.", "/")
                            + ".smali");
        }
    }

    private String getAttachMethod() {
        return "# virtual methods\n"
                + ".method protected attachBaseContext(Landroid/content/Context;)V\n"
                + ".registers 2\n"
                + ".param p1, \"base\"    # Landroid/content/Context;\n"
                + ".prologue\n"
                + "invoke-super {p0, p1}, Landroid/app/Application;->attachBaseContext(Landroid/content/Context;)V\n"
                + "invoke-virtual {p0, p1}, " + this.smaliAppCls
                + "->modifyPackageName(Landroid/content/Context;)V\n"
                + "return-void\n" + ".end method\n";

    }

    private void patchApplication(Context ctx, String filePath)
            throws IOException {
        //Log.d("DEBUG", "filepath=" + filePath);

        String modifiedFile = filePath + ".new";
        BufferedOutputStream fos = new BufferedOutputStream(
                new FileOutputStream(modifiedFile));
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath)));
        BufferedReader assetBr = new BufferedReader(new InputStreamReader(ctx
                .getAssets().open("smali_patch/cheat_package_name")));

        // Detect attachBaseContext method
        String line;
        boolean attachMethodFound = false;
        boolean insideAttachMethod = false;
        while ((line = br.readLine()) != null) {
            if (line.startsWith(".method")
                    && line.contains("attachBaseContext(Landroid/content/Context;)V")) {
                attachMethodFound = true;
                insideAttachMethod = true;
            } else if (line.equals(".end method")) {
                insideAttachMethod = false;
            } else if (insideAttachMethod
                    && line.equals("invoke-super {p0, p1}, Landroid/app/Application;->attachBaseContext(Landroid/content/Context;)V")) {
                line = line + "\n" + "invoke-virtual {p0, p1}, "
                        + this.smaliAppCls
                        + "->modifyPackageName(Landroid/content/Context;)V";
            }

            fos.write(line.getBytes());
            fos.write('\n');
        }

        // Append attachBaseContext
        if (!attachMethodFound) {
            fos.write(getAttachMethod().getBytes());
        }

        // At last append the patch function
        while ((line = assetBr.readLine()) != null) {
            if (line.contains("PACKAGE_NAME")) {
                line = line.replace("PACKAGE_NAME", this.originPackageName);
            }
            fos.write(line.getBytes());
            fos.write('\n');
        }

        assetBr.close();
        br.close();
        fos.close();

        // At last, remove the original files
        renameFile(filePath + ".new", filePath);
    }

    //
    private void renameFile(String fromPath, String toPath) {
        File f = new File(fromPath);
        File f2 = new File(toPath);
        if (!f.renameTo(f2)) {
            // Log.d("DEBUG", "rename failed!");
        } else {
            // Log.d("DEBUG", "rename succeed!");
        }
    }
}
