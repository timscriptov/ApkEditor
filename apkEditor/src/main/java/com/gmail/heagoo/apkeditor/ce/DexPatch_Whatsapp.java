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

public class DexPatch_Whatsapp implements IApkMaking, Serializable {

    private static final long serialVersionUID = -538351907344661780L;
    private String newPackageName;
    private Context ctx;
    private Map<String, String> extraStrReplaces;

    public DexPatch_Whatsapp(String newPackageName,
                             Map<String, String> extraStrReplaces) {
        this.newPackageName = newPackageName;
        this.extraStrReplaces = extraStrReplaces;

    }

    // thisClz should looks like "Ltest/MainActivity;"
    private String getPatchFunction(String thisClz, String fieldName) {
        return ".method private static _patchz()V\n" + ".registers 3\n"
                + ".prologue\n" + "sget-object v1, "
                + thisClz
                + "->" + fieldName + ":[Ljava/lang/String;\n"
                + "if-eqz v1, :cond_a\n"
                + "const/4 v0, 0x0\n"
                + ".local v0, \"i\":I\n"
                + ":goto_5\n"
                + "sget-object v1, "
                + thisClz
                + "->" + fieldName + ":[Ljava/lang/String;\n"
                + "array-length v1, v1\n"
                + "if-lt v0, v1, :cond_b\n"
                + ":cond_a\n"
                + "return-void\n"
                + ":cond_b\n"
                + "const-string v1, \"com.whatsapp\"\n"
                + "sget-object v2, "
                + thisClz
                + "->" + fieldName + ":[Ljava/lang/String;\n"
                + "aget-object v2, v2, v0\n"
                + "invoke-virtual {v1, v2}, Ljava/lang/String;->equals(Ljava/lang/Object;)Z\n"
                + "move-result v1\n"
                + "if-eqz v1, :cond_1d\n"
                + "sget-object v1, "
                + thisClz
                + "->" + fieldName + ":[Ljava/lang/String;\n"
                + "const-string v2, \""
                + this.newPackageName
                + "\"\n"
                + "aput-object v2, v1, v0\n"
                + ":cond_1d\n"
                + "add-int/lit8 v0, v0, 0x1\n"
                + "goto :goto_5\n"
                + ".end method";
    }

    @Override
    public void prepareReplaces(Context ctx, String apkFilePath,
                                Map<String, String> allReplaces, IDescriptionUpdate updater)
            throws Exception {
        this.ctx = ctx;

        File fileDir = ctx.getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        String smaliRootPath = rootDirectory + "/decoded/smali/";

        // Patch all the strings inside "z" array
        patchStringInPath(smaliRootPath + "com/whatsapp", "Lcom/whatsapp/");
        patchStringInPath(smaliRootPath + "com/whatsapp/util",
                "Lcom/whatsapp/util/");
        patchStringInPath(smaliRootPath + "com/whatsapp/registration",
                "Lcom/whatsapp/registration/");


        //patchAppFile(smaliRootPath + "com/whatsapp/App.smali");

        patchProvider(smaliRootPath + "com/whatsapp/MediaProvider.smali",
                "com.whatsapp.provider.media", "Lcom/whatsapp/MediaProvider");

        patchProvider(smaliRootPath
                        + "com/whatsapp/contact/ContactProvider.smali",
                "com.whatsapp.provider.contact",
                "Lcom/whatsapp/contact/ContactProvider");
    }

    private void patchStringInPath(String folderPath, String clsStartWith) throws IOException {
        File dir = new File(folderPath);
        File[] files = dir.listFiles();
        if (files != null)
            for (File f : files) {
                if (f.isFile()) {
                    patchStringInFile(f, clsStartWith);
                }
            }
    }

    private void patchStringInFile(File file, String clsStartWith) throws IOException {
        String filename = file.getName();
        String smaliClsName = clsStartWith
                + filename.substring(0, filename.length() - 6) + ";";

        String modifiedFile = file.getPath() + ".new";
        BufferedOutputStream fos = new BufferedOutputStream(
                new FileOutputStream(modifiedFile));
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(file)));

        // Copy the original file
        String line;
        String fieldName = null;
        boolean zArrayFound = false;
        boolean insideMethod = false;
        while ((line = br.readLine()) != null) {
            if (line.startsWith(".field private static final ")
                    && line.endsWith(":[Ljava/lang/String;")) {
                zArrayFound = true;
                int pos = line.indexOf(':');
                fieldName = line.substring(28, pos);
            } else if (line.equals(".method static constructor <clinit>()V")) {
                insideMethod = true;
                if (zArrayFound) {
                    line = getPatchFunction(smaliClsName, fieldName) + "\n" + line;
                }
            } else if (line.equals(".end method")) {
                insideMethod = false;
            } else if (zArrayFound && insideMethod) {
                if (line.equals("    return-void")) {
                    line = "    invoke-static {}, " + smaliClsName
                            + "->_patchz()V\n" + line;
                }
            }
            fos.write(line.getBytes());
            fos.write('\n');
        }

        br.close();
        fos.close();

        // At last, remove the original files
        renameFile(file.getPath() + ".new", file.getPath());
    }

    private void patchProvider(String filePath, String authorityName,
                               String clsName) throws Exception {
        String modifiedFile = filePath + ".new";
        BufferedOutputStream fos = new BufferedOutputStream(
                new FileOutputStream(modifiedFile));
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath)));
        BufferedReader assetBr = new BufferedReader(new InputStreamReader(ctx
                .getAssets().open("smali_patch/whatsapp_checkString")));

        // Copy the original file
        String line;
        boolean insideMethod = false;
        while ((line = br.readLine()) != null) {
            if (line.equals(".method private static z([C)Ljava/lang/String;")) {
                insideMethod = true;
            } else if (line.equals(".end method")) {
                insideMethod = false;
            } else if (insideMethod) {
                if (line.equals("    move-result-object v0")) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("    move-result-object v0\n");
                    sb.append("invoke-static {v0}, ");
                    sb.append(clsName);
                    sb.append(";->checkString(Ljava/lang/String;)Ljava/lang/String;");
                    sb.append("    move-result-object v0");
                }
            }
            fos.write(line.getBytes());
            fos.write('\n');
        }

        // add the new function of checkString
        String newName = this.extraStrReplaces.get(authorityName);
        while ((line = assetBr.readLine()) != null) {
            if (line.startsWith("#A")) {
                line = String.format("const-string v1, \"%s\"", authorityName);
            } else if (line.startsWith("#B")) {
                line = String.format("const-string v0, \"%s\"", newName);
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

    private void patchAppFile(String filePath) throws IOException {
        String modifiedFile = filePath + ".new";
        BufferedOutputStream fos = new BufferedOutputStream(
                new FileOutputStream(modifiedFile));
        BufferedReader br = new BufferedReader(new InputStreamReader(
                new FileInputStream(filePath)));
        BufferedReader assetBr = new BufferedReader(new InputStreamReader(ctx
                .getAssets().open("smali_patch/whatsapp_App")));

        // Remove the final keyword
        // And add the patch calling function
        String line;
        int lineIndex = 0;
        while ((line = br.readLine()) != null) {
            if (lineIndex < 1000
                    && line.equals(".field private static final cb:[Ljava/lang/String;")) {
                line = ".field private static cb:[Ljava/lang/String;";
            } else if (line.endsWith("Landroid/app/Application;-><it>()V")) {
                line += "\n    invoke-virtual {p0}, Lcom/whatsapp/App;->patch()V";
            }
            fos.write(line.getBytes());
            fos.write('\n');
            lineIndex++;
        }

        // At last append the patch function
        while ((line = assetBr.readLine()) != null) {
            if (line.contains("com.whatsapp.xy")) {
                line = line.replace("com.whatsapp.xy", newPackageName);
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

    // @Override
    // public String getDescription() {
    // return "Patch Smali Files";
    // }

}
