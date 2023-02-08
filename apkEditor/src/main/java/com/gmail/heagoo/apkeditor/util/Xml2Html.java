package com.gmail.heagoo.apkeditor.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class Xml2Html {

    public static void transform(List<String> xmlLines, String htmlFilePath)
            throws IOException {
        if (xmlLines.isEmpty()) {
            return;
        }

        File htmlFile = new File(htmlFilePath);

        //BufferedReader reader = new BufferedReader(new FileReader(xmlFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(htmlFile));

        writer.write("<html><head>");
        writer.write("<title>1.xml</title>");
        writer.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"viewsource.css\">");
        writer.write("</head>");
        writer.write("<body id=\"viewsource\" class=\"wrap\" style=\"-moz-tab-size: 4\">");
        writer.write("<pre id=\"line1\">");

        // First line
        //String line = reader.readLine();
        String line = xmlLines.get(0);
        if (line != null) {
            writer.write(line.replaceAll("<", "&lt;").replace(">", "&gt;"));
        }

        int lineIndex = 2;
        int indent = 0;
        //line = reader.readLine();
        //while (line != null) {
        for (int l = 1; l < xmlLines.size(); l++) {
            line = xmlLines.get(l);
            line = line.trim();
            writer.write("\n<span id=\"line" + lineIndex + "\"></span>");

            boolean dealed = false;
            if (line.length() >= 2) {
                if (line.charAt(0) == '<') {
                    char c = line.charAt(1);
                    // Start tag
                    if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
                            || c == '_') {
                        boolean endWithGt = false;
                        boolean selfClosed = false;
                        if (line.endsWith(">")) {
                            endWithGt = true;
                            if (line.endsWith("/>")) {
                                selfClosed = true;
                                line = line.substring(0, line.length() - 2);
                            } else {
                                line = line.substring(0, line.length() - 1);
                            }
                        }

                        String segments[] = line.split(" ");
                        String tagName = segments[0].substring(1);
                        writeStartTag(writer, indent, tagName);
                        for (int i = 1; i < segments.length; i++) {
                            int pos = segments[i].indexOf("=");
                            if (pos != -1) {
                                String name = segments[i].substring(0, pos);
                                String value = segments[i].substring(pos + 1);
                                writeAttribute(writer, indent + 1, name, value);
                            } else {
                                writeRaw(writer, segments[i]);
                            }
                        }

                        if (selfClosed) {
                            writeRaw(writer, "/>");
                        } else if (endWithGt) {
                            writeRaw(writer, ">");
                        }

                        // Self closed
                        if (selfClosed) {

                        } else {
                            indent += 1;
                        }
                        dealed = true;
                    }
                    // End tag
                    else if (c == '/') {
                        indent -= 1;
                        writeEndTag(writer, indent,
                                line.substring(2, line.length() - 1));
                        dealed = true;
                    }
                }
            }

            if (!dealed) {
                for (int i = 0; i < indent; i++) {
                    writer.write("    ");
                }
                writer.write(line.replaceAll("<", "&lt;").replace(">", "&gt;"));
            }

            //line = reader.readLine();
            lineIndex += 1;
        }

        writer.close();
        //reader.close();
    }

    private static void writeEndTag(BufferedWriter writer, int indent,
                                    String name) throws IOException {
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
        String content = "&lt;/<span class=\"end-tag\">" + name + "</span>&gt;";
        writer.write(content);
    }

    private static void writeRaw(BufferedWriter writer, String content)
            throws IOException {
        writer.write(content.replaceAll("<", "&lt;").replace(">", "&gt;"));
    }

    private static void writeAttribute(BufferedWriter writer, int indent,
                                       String name, String value) throws IOException {
        writer.write("\n");
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
        String content = "<span class=\"attribute-name\">" + name
                + "</span>=<a class=\"attribute-value\">" + value + "</a>";
        writer.write(content);
    }

    private static void writeStartTag(BufferedWriter writer, int indent,
                                      String tagName) throws IOException {
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
        writer.write("&lt;<span class=\"start-tag\">" + tagName + "</span>");
    }
}