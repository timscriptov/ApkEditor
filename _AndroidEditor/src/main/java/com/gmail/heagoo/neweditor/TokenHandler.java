package com.gmail.heagoo.neweditor;

import com.gmail.heagoo.neweditor.TokenMarker.LineContext;

public interface TokenHandler {
    void handleToken(Segment segment, byte b, int i, int i2, LineContext lineContext);

    void setLineContext(LineContext lineContext);
}