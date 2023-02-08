package com.gmail.heagoo.apkeditor.prj;

import android.os.Bundle;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProjectListActivity2 extends ProjectListActivity {


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    protected List<ProjectListAdapter.ItemInfo> listProjects(String projectFolder) {
        File prjDir = new File(projectFolder);

        List<ProjectListAdapter.ItemInfo> items = new ArrayList<>();

        do {
            File[] files = prjDir.listFiles();
            if (files == null) {
                break;
            }

            for (File f : files) {
                if (f.isFile()) {
                    continue;
                }

                if (likeDecodedProject(f)) {
                    items.add(new ProjectListAdapter.ItemInfo(f.getName(),
                            "", f.getPath(), f.lastModified()));
                }
            }
        } while (false);

        if (!items.isEmpty()) {
            Comparator<ProjectListAdapter.ItemInfo> comparator =
                    (arg0, arg1) -> arg0.lastModified < arg1.lastModified ? 1 : -1;
            Collections.sort(items, comparator);
        }

        return items;
    }

    private boolean likeDecodedProject(File dir) {
        File[] subFiles = dir.listFiles();
        if (subFiles == null) {
            return false;
        }

        boolean containRes = false;
        boolean containManifest = false;
        boolean containDex = false;
        for (File f : subFiles) {
            if (f.isDirectory() && "res".equals(f.getName())) {
                containRes = true;
            }
            if (f.isFile() && f.getName().endsWith(".dex")) {
                containDex = true;
            }
            if (f.isFile() && "AndroidManifest.xml".equals(f.getName())) {
                containManifest = true;
            }
        }

        return containRes && containManifest && containDex;
    }
}
