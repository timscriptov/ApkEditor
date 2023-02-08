package com.gmail.heagoo.apkeditor.se;

import com.gmail.heagoo.apkeditor.se.ZipFileListAdapter.FileInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipHelper {

    private static final String[] drawablePrefixes = {"res/drawable", "res/mipmap"};
    // Drawable information
    List<String> drawableNameList = new ArrayList<>();
    HashMap<String, DrawableEntry> drawableEntries = new HashMap<>();

    // Directory hierarchical information
    Map<String, List<FileInfo>> dir2Files = new HashMap<>();

    // Audio information
    List<String> audioPathList = new ArrayList<>();
    private String zipFilePath;

    public ZipHelper(String apkPath) {
        this.zipFilePath = apkPath;
    }

    // Check if the file is audio/video
    public static boolean isAudio(String path) {
        String fileExts[] = {".wav", ".mp2", ".mp3", ".ogg", ".aac",
                ".mpg", ".mpeg", ".mid", ".midi", ".smf", ".jet", ".rtttl",
                ".imy", ".xmf", ".mp4", ".m4a", ".m4v", ".3gp", ".3gpp",
                ".3g2", ".3gpp2", ".amr", ".awb", ".wma", ".wmv"};
        for (String ext : fileExts) {
            if (path.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    public void parse() {
        ZipFile zfile = null;

        try {
            zfile = new ZipFile(zipFilePath);
            Enumeration<?> zList = zfile.entries();
            ZipEntry ze;
            while (zList.hasMoreElements()) {
                ze = (ZipEntry) zList.nextElement();
                String path = ze.getName();

                // Nearly not happen, but to make sure, skip it
                if (ze.isDirectory()) {
                    continue;
                }

                // Record file information to the map structure
                {
                    String names[] = path.split("/");
                    String dir = "/";
                    for (int i = 0; i < names.length - 1; i++) {
                        String name = names[i];
                        addFile(dir, name, true);
                        dir += name + "/";
                    }
                    addFile(dir, names[names.length - 1], false);
                }

                // Detect the drawable images
                if (isDrawableResource(path)) {
                    if (!isDrawableImage(path)) {
                        continue;
                    }

                    String filename = getFilename(path);
                    String qualifier = getQualifier(path, filename);

                    DrawableEntry entry = drawableEntries.get(filename);
                    if (entry == null) {
                        drawableNameList.add(filename);
                        entry = new DrawableEntry(filename, qualifier);
                        drawableEntries.put(filename, entry);
                    } else { // Update information
                        entry.addQualifier(qualifier);
                    }
                }

                // Detect audio/video
                else if (isAudio(path)) {
                    audioPathList.add(path);
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            forceClose(zfile);
        }
    }

    private void forceClose(ZipFile c) {
        try {
            c.close();
        } catch (IOException ignored) {
        }
    }

    private String getQualifier(String path, String name) {
        int end = path.length() - name.length() - 1;
        if (end > 0) {
            return path.substring(0, end);
        } else {
            return "";
        }
    }

    private boolean isDrawableResource(String path) {
        for (String prefix : drawablePrefixes) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    // Check if the path like res/drawable-hdpi/icon.png
    private boolean isDrawableImage(String path) {
        int secondSlash = path.indexOf('/', 11);
        if (secondSlash != -1) {
            // There is no third slash
            if (path.indexOf('/', secondSlash + 1) == -1) {
                if (path.endsWith(".png") || path.endsWith(".jpg")
                        || path.endsWith(".jpeg") || path.endsWith("bmp")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String getFilename(String name) {
        int pos = name.lastIndexOf('/');
        if (pos != -1) {
            return name.substring(pos + 1);
        }
        return name;
    }

    // Add a file record
    private void addFile(String dir, String name, boolean isDir) {
        List<FileInfo> fileList = dir2Files.get(dir);
        if (fileList == null) {
            fileList = new ArrayList<>();
            dir2Files.put(dir, fileList);
        }

        // If is file, always add to list, as the file can only enumerate once
        if (!isDir) {
            FileInfo fi = new FileInfo(name, false);
            fileList.add(fi);
        }

        // For the dir, if already exist, skip
        else {
            if (dir2Files.get(dir + name + "/") != null) {
                return;
            }

            FileInfo fi = new FileInfo(name, true);
            fileList.add(fi);
        }
    }

    public String getFilePath() {
        return this.zipFilePath;
    }

    public int getImageNum() {
        return this.drawableNameList.size();
    }

    public int getAudioNum() {
        return this.audioPathList.size();
    }
}
