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

import com.gmail.heagoo.common.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Arrays;

/**
 * @author Ryszard Wiśniewski <brut.alll@gmail.com>
 */
public class OS {
    public static void rmdir(File dir) throws Exception {
        if (!dir.exists()) {
            return;
        }
        File[] files = dir.listFiles();
        if (files != null)
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                if (file.isDirectory()) {
                    rmdir(file);
                } else {
                    file.delete();
                }
            }
        dir.delete();
    }

    public static void rmfile(String file) throws Exception {
        File del = new File(file);
        del.delete();
    }

    public static void rmdir(String dir) throws Exception {
        rmdir(new File(dir));
    }

    public static void cpdir(File src, File dest) throws Exception {
        dest.mkdirs();
        File[] files = src.listFiles();
        if (files != null)
            for (int i = 0; i < files.length; i++) {
                File file = files[i];
                File destFile = new File(dest, file.getName());
//			File destFile = new File(dest.getPath() + File.separatorChar
//					+ file.getName());
                if (file.isDirectory()) {
                    cpdir(file, destFile);
                    continue;
                }
                try {
                    InputStream in = new FileInputStream(file);
                    OutputStream out = new FileOutputStream(destFile);
                    IOUtils.copy(in, out);
                    in.close();
                    out.close();
                } catch (IOException ex) {
                    throw new Exception("Could not copy file: " + file, ex);
                }
            }
    }

    public static void cpdir(String src, String dest) throws Exception {
        cpdir(new File(src), new File(dest));
    }

    public static void copyFile(String srcPath, String dstPath)
            throws Exception {
        try {
            InputStream in = new FileInputStream(srcPath);
            OutputStream out = new FileOutputStream(dstPath);
            IOUtils.copy(in, out);
            in.close();
            out.close();
        } catch (IOException ex) {
            throw new Exception("Could not copy file: " + srcPath, ex);
        }
    }

    public static void exec(String[] cmd) throws Exception {
        Process ps = null;
        try {
            ps = Runtime.getRuntime().exec(cmd);

            new StreamForwarder(ps.getInputStream(), System.err).start();
            new StreamForwarder(ps.getErrorStream(), System.err).start();
            if (ps.waitFor() != 0) {
                throw new Exception("could not exec command: "
                        + Arrays.toString(cmd));
            }
        } catch (IOException ex) {
            throw new Exception("could not exec command: "
                    + Arrays.toString(cmd), ex);
        } catch (InterruptedException ex) {
            throw new Exception("could not exec command: "
                    + Arrays.toString(cmd), ex);
        }
    }

    public static File createTempDirectory() throws Exception {
        try {
            File tmp = File.createTempFile("BRUT", null);
            if (!tmp.delete()) {
                throw new Exception("Could not delete tmp file: "
                        + tmp.getAbsolutePath());
            }
            if (!tmp.mkdir()) {
                throw new Exception("Could not create tmp dir: "
                        + tmp.getAbsolutePath());
            }
            return tmp;
        } catch (IOException ex) {
            throw new Exception("Could not create tmp dir", ex);
        }
    }

    static class StreamForwarder extends Thread {

        private final InputStream mIn;
        private final OutputStream mOut;

        public StreamForwarder(InputStream in, OutputStream out) {
            mIn = in;
            mOut = out;
        }

        @Override
        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(
                        mIn));
                BufferedWriter out = new BufferedWriter(new OutputStreamWriter(
                        mOut));
                String line;
                while ((line = in.readLine()) != null) {
                    out.write(line);
                    out.newLine();
                }
                out.flush();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}
