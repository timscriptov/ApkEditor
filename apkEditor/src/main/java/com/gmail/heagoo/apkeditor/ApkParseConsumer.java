package com.gmail.heagoo.apkeditor;

import java.util.Map;

public interface ApkParseConsumer {

    void decodeFailed(String errMessage);

    void resTableDecoded(boolean ret);

    void resourceDecoded(Map<String, String> fileEntry2ZipEntry);

}
