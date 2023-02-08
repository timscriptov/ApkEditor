package com.gmail.heagoo.common;

public interface CommandInterface {

    public boolean runCommand(String command, String[] env, Integer timeout);

    public boolean runCommand(String command, String[] env, Integer timeout, boolean readWhileExec);

    public String getStdOut();

    public String getStdError();
}
