package com.gmail.heagoo.common;

import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Map;

public class CommandRunner implements CommandInterface {

    // The first one is stdout, second is stderr
    // private String stdout;
    // private String stderr;
    private String[] outputs = new String[2];

    public static Process runWithEnv(String command, String[] env,
                                     String directory) throws IOException {
        Map<String, String> environment = System.getenv();
        String[] envArray = new String[environment.size()
                + (env != null ? env.length : 0)];
        int idx = 0;
        for (Map.Entry<String, String> entry : environment.entrySet())
            envArray[idx++] = entry.getKey() + "=" + entry.getValue();
        if (env != null)
            for (String entry : env)
                envArray[idx++] = entry;
        Process process = null;
        if (directory != null) {
            process = Runtime.getRuntime().exec(command, envArray,
                    new File(directory));
        } else {
            process = Runtime.getRuntime().exec(command, envArray);
        }
        return process;
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

    private Process createProcess(String command, String[] env)
            throws IOException {
        return Runtime.getRuntime().exec(command, env);
    }

    private Process createProcess(String[] commands, String[] env)
            throws IOException {
        return Runtime.getRuntime().exec(commands, env);
    }

    public boolean runCommand(String[] commands, String[] env,
                              Integer timeout) {
        return runCommand(commands, env, null, timeout, false);
    }

    @Override
    public boolean runCommand(String command, String[] env, Integer timeout) {
        return runCommand(command, env, null, timeout, false);
    }

    @Override
    public boolean runCommand(String command, String[] env, Integer timeout,
                              boolean readWhileExec) {
        return runCommand(command, env, null, timeout, readWhileExec);
    }

    public boolean runCommand(Object command, String[] env, String currentDir,
                              Integer timeout, boolean readWhileExec) {
        Process process = null;
        try {
            if (command instanceof String) {
                process = createProcess((String) command, env);
            } else {
                process = Runtime.getRuntime().exec((String[]) command);
                // process = createProcess((String[])command, env);
            }

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
                    Thread.sleep(20);
                    if (!isProcessAlive(process))
                        break;
                    if (System.currentTimeMillis() > finish) {
                        Log.w("CommandRunner", "Process doesn't seem "
                                + "to stop on it's own, assuming it's hanging");
                        // Note: 'finally' will call destroy(), but you
                        // might still see zombies.
                        outputs[1] = "Timeout!";
                        return false;
                    }
                }
            } else {
                process.waitFor();
            }

            if (readWhileExec) {
                thread0.close();
                thread1.close();
            } else {
                this.outputs[0] = readStream(outStream);
                this.outputs[1] = readStream(errStream);
                closeQuietly(outStream);
                closeQuietly(errStream);
            }

            // In order to consider this a success, we require to
            // things: a) a proper exit value, and ...
            if (process.exitValue() != 0) {
                return false;
            }

            return true;

        } catch (FileNotFoundException e) {
            Log.e("DEBUG", "Failed to run command", e);
            return false;
        } catch (IOException e) {
            Log.e("DEBUG", "Failed to run command", e);
            return false;
        } catch (InterruptedException e) {
            //Log.e("DEBUG", "Failed to run command", e);
            return false;
        } finally {
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

    private void closeQuietly(InputStream input) {
        if (input != null) {
            try {
                input.close();
            } catch (IOException e) {
            }
        }
    }

    @Override
    public String getStdOut() {
        return outputs[0];
    }

    @Override
    public String getStdError() {
        return outputs[1];
    }

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
