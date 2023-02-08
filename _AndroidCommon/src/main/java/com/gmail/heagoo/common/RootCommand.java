package com.gmail.heagoo.common;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

public class RootCommand implements CommandInterface {

    static String[] SU_LOCATIONS = {"/data/bin/su", "/system/bin/su",
            // This is last because we are afraid a proper su might be in
            // one of those other locations, while this one is secured.
            "/system/xbin/su",};
    // The first one is stdout, second is stderr
    // private String stdout;
    // private String stderr;
    private String[] outputs = new String[2];

    public static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Check whether a process is still alive. We use this as a naive way to
     * implement timeouts.
     */
    public static boolean isProcessAlive(Process p) {
        try {
            p.exitValue();
            return false;
        } catch (IllegalThreadStateException e) {
            return true;
        }
    }

    static String readStream(InputStream stream) throws IOException {
        final char[] buffer = new char[8192];
        StringBuilder out = new StringBuilder();
        Reader in = new InputStreamReader(stream, "UTF-8");
        int read;
        do {
            read = in.read(buffer, 0, buffer.length);
            if (read > 0)
                out.append(buffer, 0, read);
        } while (read >= 0);
        return out.toString();
    }

    static String getSuPath() {
        String path = "su";
        for (String p : SU_LOCATIONS) {
            File su = new File(p);
            if (su.exists()) {
                path = p;
            }
        }
        debug("su path: " + path);
        return path;
    }

    private static void debug(String str) {
        //Log.d("RootCommand", str);
    }

    public static Process runWithEnv(String command, String[] env)
            throws IOException {
        Map<String, String> environment = System.getenv();
        String[] envArray = new String[environment.size()
                + (env != null ? env.length : 0)];
        int idx = 0;
        for (Map.Entry<String, String> entry : environment.entrySet())
            envArray[idx++] = entry.getKey() + "=" + entry.getValue();
        if (env != null)
            for (String entry : env)
                envArray[idx++] = entry;
        Process process = Runtime.getRuntime().exec(command, envArray);
        return process;
    }

    public boolean runRootCommand(String command, String[] env,
                                  Integer timeout) {
        return runRootCommand(command, env, timeout, false);
    }

    public boolean runRootCommand(String command, String[] env, Integer timeout,
                                  boolean readWhileExec) {
        return runRootCommand(command, env, timeout, null, readWhileExec);
    }

    public boolean runRootCommand(String command, String[] env, Integer timeout,
                                  String curDir, boolean readWhileExec) {
        Process process = null;
        DataOutputStream os = null;
        try {
            debug(String.format("Running '%s' as root", command));

            process = runWithEnv(getSuPath(), env);
            os = new DataOutputStream(process.getOutputStream());
            if (curDir != null) {
                os.writeBytes("cd " + curDir + "\n");
            }
            os.writeBytes(command + "\n");
            os.writeBytes("echo \"rc:\" $?\n");
            os.writeBytes("exit\n");
            os.flush();

            // Read stdout and stderr in another 2 threads
            InputStream outStream = process.getInputStream();
            InputStream errStream = process.getErrorStream();
            StreamReadThread thread0 = null;
            StreamReadThread thread1 = null;
            if (readWhileExec) {
                thread0 = new StreamReadThread(outStream, outputs, 0);
                thread0.start();
                thread1 = new StreamReadThread(errStream, outputs, 1);
                thread1.start();
            }

            // Handle a requested timeout, or just use waitFor() otherwise.
            if (timeout != null) {
                long finish = System.currentTimeMillis() + timeout;
                while (true) {
                    // Thread.sleep(300);
                    if (!isProcessAlive(process))
                        break;
                    // TODO: We could use a callback to let the caller
                    // check the success-condition (like the state properly
                    // being changed), and then end early, rather than
                    // waiting for the timeout to occur. However, this
                    // is made more complicated by us not really wanting
                    // to kill a process early that would never have hung,
                    // but which might not actually be completely finished yet
                    // when the callback would register success.
                    // Also, now that the timeout is only used as a last-resort
                    // mechanism anyway, with most cases of a hanging process
                    // being avoided by switching on ADB Debugging, improving
                    // the timeout handling isn't that important anymore.
                    if (System.currentTimeMillis() > finish) {
                        // Usually, this can't be considered a success.
                        // However, in terms of the bug we're trying to
                        // work around here (the call hanging if adb
                        // debugging is disabled), the command would
                        // have successfully run, but just doesn't
                        // return. We report success, just in case, and
                        // the caller will have to check whether the
                        // command actually did do it's job.
                        // TODO: It might be core "correct" to return false
                        // here, or indicate the timeout in some other way,
                        // and let the caller ignore those values on their
                        // own volition.
                        debug("Process doesn't seem "
                                + "to stop on it's own, assuming it's hanging");
                        // Note: 'finally' will call destroy(), but you
                        // might still see zombies.
                        return true;
                    }
                }
            } else
                process.waitFor();

            if (readWhileExec) {
                thread0.close();
                thread1.close();
            } else {
                this.outputs[0] = readStream(outStream);
                this.outputs[1] = readStream(errStream);
                closeQuietly(outStream);
                closeQuietly(errStream);
            }

            debug("Process returned with " + process.exitValue());
            debug("Process stdout was: " + outputs[0] + "; stderr: "
                    + outputs[1]);

            // In order to consider this a success, we require to
            // things: a) a proper exit value, and ...
            if (process.exitValue() != 0)
                return false;

            return true;

        } catch (FileNotFoundException e) {
            debug("Failed to run command: " + e.getMessage());
            return false;
        } catch (IOException e) {
            debug("Failed to run command: " + e.getMessage());
            return false;
        } catch (InterruptedException e) {
            debug("Failed to run command: " + e.getMessage());
            return false;
        } finally {
            if (os != null)
                try {
                    os.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            if (process != null) {
                try {
                    // Yes, this really is the way to check if the process is
                    // still running.
                    process.exitValue();
                } catch (IllegalThreadStateException e) {
                    // Only call destroy() if the process is still running;
                    // Calling it for a terminated process will not crash, but
                    // (starting with at least ICS/4.0) spam the log with INFO
                    // messages ala "Failed to destroy process" and "kill
                    // failed: ESRCH (No such process)".
                    process.destroy();
                }
            }
        }
    }

    @Override
    public boolean runCommand(String command, String[] env, Integer timeout) {
        return runRootCommand(command, env, timeout);
    }

    @Override
    public String getStdOut() {
        return outputs[0];
    }

    @Override
    public String getStdError() {
        return outputs[1];
    }

    private void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public boolean runCommand(String command, String[] env, Integer timeout,
                              boolean readWhileExec) {
        return runRootCommand(command, env, timeout, readWhileExec);
    }

    ////////////////////////////////////////////////////////////////////////////////
    private static class StreamReadThread extends Thread {
        private InputStream input;
        private String[] outputs;
        private int index;

        public StreamReadThread(InputStream input, String[] outputs,
                                int index) {
            this.input = input;
            this.outputs = outputs;
            this.index = index;
        }

        @Override
        public void run() {
            final char[] buffer = new char[128];
            StringBuilder out = new StringBuilder();
            try {
                Reader in = new InputStreamReader(input, "UTF-8");
                int read;
                do {
                    read = in.read(buffer, 0, buffer.length);
                    if (read > 0) {
                        out.append(buffer, 0, read);
                    }
                } while (read >= 0);
            } catch (Exception e) {
            }

            outputs[index] = out.toString();
        }

        public void close() {
            this.interrupt();
            try {
                input.close();
            } catch (IOException e) {
            }
        }
    }
}
