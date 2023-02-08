package com.gmail.heagoo.apkeditor.smali;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import brut.androlib.res.decoder.ResourceIdProvider;

// Manage the resource id read from smali files
public class ResSmaliIdProvider implements ResourceIdProvider {

    Map<Integer, String> id2Name = new HashMap<Integer, String>();

    // workDir is the root path contains the R.smali
    public ResSmaliIdProvider(String workDir, String pkgName) {
        String[] names = pkgName.split("\\.");
        StringBuilder sb = new StringBuilder();
        sb.append(workDir);
        for (String name : names) {
            sb.append(name);
            sb.append("/");
        }

        File dir = new File(sb.toString());
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }

        for (File f : files) {
            String filename = f.getName();
            if (filename.startsWith("R$")) {
                readFields(f);
            }
        }
    }

    private void readFields(File f) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(
                    new InputStreamReader(new FileInputStream(f)));
            // Read like: .field public static final DialogTitleStyle:I =
            // 0x7f080013
            String line = br.readLine();
            while (line != null) {
                if (line.startsWith(".field ")) {
                    int nameEndPos = line.indexOf(":I");
                    if (nameEndPos != -1) {
                        int nameStartPos = line.lastIndexOf(" ", nameEndPos)
                                + 1;
                        parseNameAndId(line, nameStartPos, nameEndPos);
                    }
                }
                line = br.readLine();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
    }

    private void parseNameAndId(String line, int nameStartPos, int nameEndPos) {
        String name = line.substring(nameStartPos, nameEndPos);
        int idStartPos = nameEndPos + 7;

        try {
            String hexStr = line.substring(idStartPos);
            int id = Integer.parseInt(hexStr, 16);
            id2Name.put(id, name);
        } catch (Exception e) {
        }
    }

    public String getNameById(int id) {
        return id2Name.get(id);
    }
}
