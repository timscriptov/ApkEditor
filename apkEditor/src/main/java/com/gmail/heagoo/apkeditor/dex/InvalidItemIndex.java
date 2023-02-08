package com.gmail.heagoo.apkeditor.dex;

public class InvalidItemIndex extends MyException {

    private static final long serialVersionUID = 5034685574646581891L;

    public InvalidItemIndex(int index, String fmt, Object... args) {
        super(fmt, args);
    }

}
