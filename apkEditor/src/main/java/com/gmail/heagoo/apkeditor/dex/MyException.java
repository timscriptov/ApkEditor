package com.gmail.heagoo.apkeditor.dex;

public class MyException extends Exception {

    private static final long serialVersionUID = 5284745021121448343L;

    public MyException(String fmt, Object... args) {
        super(String.format(fmt, args));
    }

}
