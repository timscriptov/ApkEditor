package com.gmail.heagoo.apkeditor.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

public class LinedReader extends BufferedReader {

    private int curLine = 0;

    public LinedReader(Reader in) {
        super(in);
    }

    public String readLine() throws IOException {
        curLine += 1;
        return super.readLine();
    }

    public int getCurrentLine() {
        return curLine;
    }
}
