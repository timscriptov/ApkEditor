/**
 * Copyright 2010 Ryszard Wiśniewski <brut.alll@gmail.com>
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

package brut.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import brut.common.BrutException;
import brut.common.InvalidUnknownFileException;
import brut.common.RootUnknownFileException;
import brut.common.TraversalUnknownFileException;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class BrutIO {

    public static void copyAndClose(InputStream in, OutputStream out)
            throws IOException {
        try {
            byte[] buffer = new byte[1024];
            int readBytes = in.read(buffer);
            while (readBytes > 0) {
                out.write(buffer, 0, readBytes);
                readBytes = in.read(buffer);
            }
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException ex) {
            }
        }
    }

    public static long recursiveModifiedTime(File[] files) {
        long modified = 0;
        for (int i = 0; i < files.length; i++) {
            long submodified = recursiveModifiedTime(files[i]);
            if (submodified > modified) {
                modified = submodified;
            }
        }
        return modified;
    }

    public static String sanitizeUnknownFile(final File directory, final String entry) throws IOException, BrutException {
        if (entry.length() == 0) {
            throw new InvalidUnknownFileException("Invalid Unknown File - " + entry);
        }

        if (new File(entry).isAbsolute()) {
            throw new RootUnknownFileException("Absolute Unknown Files is not allowed - " + entry);
        }

        final String canonicalDirPath = directory.getCanonicalPath() + File.separator;
        final String canonicalEntryPath = new File(directory, entry).getCanonicalPath();

        if (!canonicalEntryPath.startsWith(canonicalDirPath)) {
            throw new TraversalUnknownFileException("Directory Traversal is not allowed - " + entry);
        }

        // https://stackoverflow.com/q/2375903/455008
        return canonicalEntryPath.substring(canonicalDirPath.length());
    }

    public static long recursiveModifiedTime(File file) {
        long modified = file.lastModified();
        if (file.isDirectory()) {
            File[] subfiles = file.listFiles();
            if (subfiles != null) {
                for (int i = 0; i < subfiles.length; i++) {
                    long submodified = recursiveModifiedTime(subfiles[i]);
                    if (submodified > modified) {
                        modified = submodified;
                    }
                }
            }
        }
        return modified;
    }
}
