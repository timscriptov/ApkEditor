package com.gmail.heagoo.httpserver;

import com.gmail.heagoo.common.IOUtils;
import com.gmail.heagoo.common.TextFileReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;

public class HttpServer extends NanoHTTPD {
    public static final int DEFAULT_PORT = 8000;
    private static final String TAG = "HttpServer";
    private static final String[] customizedUris = new String[]{
            "/listFiles",
            "/readFile",
            "/readImage",
            "/saveFile"
    };
    private String httpDirectory;
    private String projectDirectory;
    private String editorTemplate = null;

    public HttpServer(String httpDirectory, String projectDirectory) {
        super(DEFAULT_PORT);
        this.httpDirectory = httpDirectory;
        this.projectDirectory = projectDirectory;
    }

    private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // 遍历所有的网络接口
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements(); ) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // 在所有的接口下再遍历IP
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements(); ) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {// 排除loopback类型地址
                        if (inetAddr.isSiteLocalAddress()) {
                            // 如果是site-local地址，就是它了
                            return inetAddr;
                        } else if (candidateAddress == null) {
                            // site-local类型的地址未被发现，先记录候选地址
                            candidateAddress = inetAddr;
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                return candidateAddress;
            }
            // 如果没有发现 non-loopback地址.只能用最次选的方案
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        } catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException(
                    "Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

    public void tryStart(int portDelta) throws IOException {
        Class<?> clz = NanoHTTPD.class;
        try {
            Field nameField = clz.getDeclaredField("myPort");
            nameField.setAccessible(true);
            nameField.set(this, DEFAULT_PORT + portDelta);
        } catch (Exception e) {
            e.printStackTrace();
        }

        super.start();
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();
        Map<String, String> headers = session.getHeaders();

        //接收不到post参数的问题，              http://blog.csdn.net/obguy/article/details/53841559
        try {
            session.parseBody(new HashMap<String, String>());
        } catch (Exception ignored) {
        }
        Map<String, String> parms = session.getParms();
        try {
            LogUtil.d(TAG, uri);

            if (isDynamicUri(uri)) {
                switch (uri) {
                    case "/listFiles":
                        return listFiles(parms);
                    case "/readFile":
                        return readFile(parms);
                    case "/readImage":
                        return readImage(parms);
                    case "/saveFile":
                        return saveTextFile(parms);
                }
            } else {
                // Static resources
                String filePath = getFilePath(uri); // 根据url获取文件路径

                if (filePath == null) {
                    return super.serve(session);
                }

                File file = new File(filePath);
                if (file != null && file.exists()) {
                    LogUtil.d(TAG, "file path = " + file.getAbsolutePath());

                    //Mine Type: image/jpg, video/mp4, etc
                    String mimeType = getMimeType(filePath);

                    Response res = null;
                    InputStream is = new FileInputStream(file);
                    res = newFixedLengthResponse(Response.Status.OK, mimeType, is, is.available());
                    return res;
                } else {
                    LogUtil.d(TAG, "Cannot find" + file.getAbsolutePath());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return notFound();
    }

    private Response saveTextFile(Map<String, String> params) {
        String relativePath = params.get("path");
        String content = params.get("content");

        if (relativePath != null && content != null) {
            File file = new File(projectDirectory + "/" + relativePath);
            if (file.isFile() && file.exists()) {
                FileOutputStream fos = null;
                try {
                    fos = new FileOutputStream(file);
                    fos.write(content.getBytes());
                    fos.close();
                    return newFixedLengthResponse(Response.Status.OK, "text/html", "OK");
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(fos);
                }
            }
        }

        // TODO
        return notFound();
    }

    private Response readImage(Map<String, String> params) {
        String relativePath = params.get("path");

        File file = new File(projectDirectory + "/" + relativePath);
        if (!file.exists() || !file.isFile()) {
            return notFound();
        }

        try {
            FileInputStream fis = new FileInputStream(file);
            int size = fis.available();
            return newFixedLengthResponse(Response.Status.OK, getMimeType(relativePath), fis, size);
        } catch (IOException e) {
            return notFound();
        }
    }

    private Response readFile(Map<String, String> params) {
        String relativePath = params.get("path");

        File file = new File(projectDirectory + "/" + relativePath);
        if (!file.exists() || !file.isFile()) {
            return notFound();
        }

        // Detect edit mode
        String editMode = getEditMode(relativePath);
        if (editMode == null) {
            if (isImage(relativePath)) {
                return showImage(relativePath);
            } else {
                return cannotOpen(relativePath);
            }
        } else { // Show editor
            if (editorTemplate == null) {
                initEditorTemplate();
            }
            try {
                TextFileReader reader = new TextFileReader(file.getPath());
                String content = reader.getContents();
                content = content.replace("<", "&lt;");
                String html = editorTemplate.replace("__MODE__", editMode);
                html = html.replace("__PATH__", relativePath);
                html = html.replace("__CONTENT__", content);
                return newFixedLengthResponse(Response.Status.OK, "text/html", html);
            } catch (IOException e) {
                return notFound();
            }
        }
    }

    private Response cannotOpen(String relativePath) {
        String html = "<html>\n" +
                "<head>\n" +
                "<style type=\"text/css\">\n" +
                "#tip {\n" +
                "    position: absolute;\n" +
                "    margin: auto;\n" +
                "    top: 50%;\n" +
                "    width: 100%;\n" +
                "    height: 100px;\n" +
                "    margin-top: -50px;\n" +
                "}\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<div id=\"tip\"><center>Cannot open " + relativePath +
                "</center></div>\n" +
                "</body>\n" +
                "</html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private Response showImage(String relativePath) {
        String html = "<html>\n" +
                "<head>\n" +
                "<style type=\"text/css\">\n" +
                "img {\n" +
                "    position: absolute;\n" +
                "    margin: auto;\n" +
                "    top: 0;\n" +
                "    left: 0;\n" +
                "    right: 0;\n" +
                "    bottom: 0;\n" +
                "}\n" +
                "</style>\n" +
                "</head>\n" +
                "<body>\n" +
                "<img src=\"readImage?path=" + relativePath + "\">\n" +
                "</body>\n" +
                "</html>";
        return newFixedLengthResponse(Response.Status.OK, "text/html", html);
    }

    private boolean isImage(String relativePath) {
        return relativePath.endsWith(".png") || relativePath.endsWith(".jpg")
                || relativePath.endsWith(".jpeg")
                || relativePath.endsWith(".gif")
                || relativePath.endsWith(".bmp");
    }

    // ACE editor template
    private void initEditorTemplate() {
        try {
            TextFileReader reader = new TextFileReader(httpDirectory + "/editor.htm");
            editorTemplate = reader.getContents();
        } catch (IOException ignored) {
            editorTemplate =
                    "<html lang=\"en\">\n" +
                            "<head>\n" +
                            "  <meta charset=\"UTF-8\">\n" +
                            "  <meta http-equiv=\"X-UA-Compatible\" content=\"IE=edge,chrome=1\">\n" +
                            "  <title>Editor</title>\n" +
                            "  <style type=\"text/css\" media=\"screen\">\n" +
                            "    body {\n" +
                            "        overflow: hidden;\n" +
                            "    }\n" +
                            "    #editor {\n" +
                            "        margin: 0;\n" +
                            "        position: absolute;\n" +
                            "        top: 0;\n" +
                            "        bottom: 0;\n" +
                            "        left: 0;\n" +
                            "        right: 0;\n" +
                            "    }\n" +
                            "  </style>\n" +
                            "</head>\n" +
                            "<body>\n" +
                            "<pre id=\"editor\">__CONTENT__</pre>\n" +
                            "<script src=\"src-min-noconflict/ace.js\" type=\"text/javascript\" charset=\"utf-8\"></script>\n" +
                            "<script>\n" +
                            "    var editor = ace.edit(\"editor\");\n" +
                            "    editor.setTheme(\"ace/theme/dreamweaver\");\n" +
                            "    editor.session.setMode(\"ace/mode/__MODE__\");\n" +
                            "</script>\n" +
                            "</body>\n" +
                            "</html>";
        }
    }

    // Mode in ACE editor, like java, xml, etc.
    private String getEditMode(String relativePath) {
        if (relativePath.endsWith(".xml")) {
            return "xml";
        }
        if (relativePath.endsWith(".java")) {
            return "java";
        }
        if (relativePath.endsWith(".kt")) {
            return "kotlin";
        }
        if (relativePath.endsWith(".css")) {
            return "css";
        }
        if (relativePath.endsWith(".js")) {
            return "javascript";
        }
        if (relativePath.endsWith(".htm") || relativePath.endsWith(".html")) {
            return "html";
        }
        if (relativePath.endsWith(".txt") || relativePath.endsWith(".md")
                || relativePath.endsWith(".project") || relativePath.endsWith(".gradle")
                || relativePath.endsWith(".smali")) {
            return "text";
        }
        if (relativePath.endsWith(".c") || relativePath.endsWith(".cpp")) {
            return "c_cpp";
        }
        if (relativePath.endsWith(".py")) {
            return "python";
        }
        return null;
    }

    private Response notFound() {
        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/html", "Not Found!");
    }

    private Response listFiles(Map<String, String> params) {
        String subPath = params.get("node");
        if (subPath == null) {
            subPath = "";
        } else {
            subPath += "/";
        }

        File dir = new File(projectDirectory + "/" + subPath);
        if (!dir.exists() || !dir.isDirectory()) {
            return notFound();
        }

        File[] files = dir.listFiles();
        List<File> fileList = Arrays.asList(files);
        Collections.sort(fileList, new FileComparator());
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < fileList.size(); ++i) {
            File f = fileList.get(i);
            appendFileNode(sb, f, subPath);
            if (i != fileList.size() - 1) {
                sb.append(",");
            }
        }
        sb.append("]");

        String mimeType = "application/json";
        return newFixedLengthResponse(Response.Status.OK, mimeType, sb.toString());
    }

    private void appendFileNode(StringBuilder sb, File f, String subPath) {
        sb.append("{");
        sb.append("\"name\": \"" + f.getName() + "\",");
        // Use the relative path as the id
        sb.append("\"id\": \"" + subPath + f.getName() + "\",");
        if (f.isDirectory()) {
            sb.append("\"load_on_demand\": true");
        } else {
            sb.append("\"load_on_demand\": false");
        }
        sb.append("}");
    }

    private String getFilePath(String uri) {
        if ("/".equals(uri)) {
            return httpDirectory + "/index.htm";
        }
        return httpDirectory + uri;
    }

    private String getMimeType(String filePath) {
        if (filePath.endsWith(".jpg") || filePath.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (filePath.endsWith(".png")) {
            return "image/png";
        }
        if (filePath.endsWith(".gif")) {
            return "image/gif";
        }
        if (filePath.endsWith(".bmp")) {
            return "image/bmp";
        }
        if (filePath.endsWith(".htm") || filePath.endsWith(".html")) {
            return "text/html";
        }
        if (filePath.endsWith(".css")) {
            return "text/css";
        }
        if (filePath.endsWith(".js")) {
            return "text/javascript";
        }
        return "text/plain";
    }

    private boolean isDynamicUri(String uri) {
        for (String str : customizedUris) {
            if (str.equals(uri)) {
                return true;
            }
        }
        return false;
    }

    public String getServiceURL() {
        final String[] hostAddress = {""};
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    // Network must be on another thread
                    InetAddress address = getLocalHostLANAddress();
                    hostAddress[0] = address.getHostAddress();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
                synchronized (this) {
                    this.notify();
                }
            }
        };
        try {
            synchronized (thread) {
                thread.start();
                thread.wait();
            }
        } catch (InterruptedException ignored) {
        }

        int port = getListeningPort();
        return "http://" + hostAddress[0] + ":" + String.valueOf(port);
    }

    public void setProjectDirectory(String projectDirectory) {
        this.projectDirectory = projectDirectory;
    }

    public void setHttpDirectory(String httpDirectory) {
        this.httpDirectory = httpDirectory;
    }
}