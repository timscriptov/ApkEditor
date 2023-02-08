package com.gmail.heagoo.common;

import java.io.File;

public class PathUtil {
    public static String getNameFromPath(String path) {
        if (path != null) {
            int pos = path.lastIndexOf('/');
            return path.substring(pos + 1);
        }
        return null;
    }

    public static String replaceNameWith(String path, String newName) {
        if (path != null) {
            int pos = path.lastIndexOf('/');
            return path.substring(0, pos + 1) + newName;
        }
        return null;
    }

    // Check if path1 is the parent folder of path2
    public static boolean isParentFolderOf(String path1, String path2) {
        if (path1.endsWith("/")) {
            path1 = path1.substring(0, path1.length() - 1);
        }
        if (path2.endsWith("/")) {
            path2 = path2.substring(0, path2.length() - 1);
        }
        String p1[] = path1.split("/");
        String p2[] = path2.split("/");
        if (p1.length < p2.length) {
            for (int i = 0; i < p1.length; ++i) {
                if (!p1[i].equals(p2[i])) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    // Return "0" if parent=/storage/emulated, referPath=/storage/emulated/0
    // Call isParentFolderOf before calling this function
    public static String getSubFolder(String parent, String referPath) {
        if (parent.endsWith("/")) {
            parent = parent.substring(0, parent.length() - 1);
        }
        if (referPath.endsWith("/")) {
            referPath = referPath.substring(0, referPath.length() - 1);
        }
        String p1[] = parent.split("/");
        String p2[] = referPath.split("/");
        if (p1.length < p2.length) {
            return p2[p1.length];
        }
        return null;
    }

    public static String getDirectoryPath(String filePath) {
        File f = new File(filePath);
        if (f.isDirectory()) {
            return f.getPath();
        } else {
            int pos = filePath.lastIndexOf('/');
            if (pos != -1) {
                return filePath.substring(0, pos);
            }
        }
        return filePath;
    }

    // Get parent folder name for a file
    public static String getParentFolder(String filePath) {
        String names[] = filePath.split("/");
        // Maybe the file is in the root directory like "/etc"
        if (names.length < 2) {
            return "";
        } else {
            return names[names.length - 2];
        }
    }

    // Get the target saving file/dir which not exist, by adding (1), (2), etc
    public static File getTargetNonExistFile(String path, boolean isDir) {
        int index = 1;
        String folder = null;
        String name = null;
        String fileType = null;
        if (!isDir) {
            int slashPos = path.lastIndexOf('/');
            folder = path.substring(0, slashPos + 1);
            String filename = path.substring(slashPos + 1);

            name = filename;
            fileType = "";
            int dotPos = filename.lastIndexOf('.');
            if (dotPos != -1) {
                name = filename.substring(0, dotPos);
                fileType = filename.substring(dotPos);
            }
        }

        while (true) {
            String testingPath = isDir ? path + "(" + index + ")" :
                    folder + name + index + fileType;
            File node = new File(testingPath);
            if (!node.exists()) {
                return node;
            }
            index += 1;
        }
    }
}
