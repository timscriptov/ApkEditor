package com.gmail.heagoo.apkeditor.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class ValuesXml2Html {

    private int indent;

    // flag means whether to write indent
    private static void writeEndTag(BufferedWriter writer, int indent,
                                    String name, boolean flag) throws IOException {
        if (flag) {
            for (int i = 0; i < indent; i++) {
                writer.write("    ");
            }
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

    // flag means whether to write indent
    // only write indent when write to a new line in html
    private static void writeStartTag(BufferedWriter writer, int indent,
                                      String tagName, boolean flag) throws IOException {
        if (flag) {
            for (int i = 0; i < indent; i++) {
                writer.write("    ");
            }
        }
        writer.write("&lt;<span class=\"start-tag\">" + tagName + "</span>");
    }

    public void transform(List<String> xmlLines, String htmlFilePath)
            throws IOException {

        File htmlFile = new File(htmlFilePath);

        // BufferedReader reader = new BufferedReader(new FileReader(xmlFile));
        BufferedWriter writer = new BufferedWriter(new FileWriter(htmlFile));

        writer.write("<html><head>");
        writer.write("<title>1.xml</title>");
        writer.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"viewsource.css\">");
        writer.write("</head>");
        writer.write("<body id=\"viewsource\" class=\"wrap\" style=\"-moz-tab-size: 4\">");
        writer.write("<pre id=\"line1\">");

        // First line
        // String line = reader.readLine();
        String line = xmlLines.get(0);
        if (line != null) {
            writer.write(line.replaceAll("<", "&lt;").replace(">", "&gt;"));
        }

        int lineIndex = 2;
        // line = reader.readLine();
        // while (line != null) {
        for (int l = 1; l < xmlLines.size(); l++) {
            line = xmlLines.get(l);
            line = line.trim();
            writer.write("\n<span id=\"line" + lineIndex + "\"></span>");

            parseLine(writer, line, true);

            // line = reader.readLine();
            lineIndex += 1;
        }

        writer.close();
        // reader.close();
    }

    private void parseStartTag(BufferedWriter writer, String content, boolean flag)
            throws IOException {
        String segments[] = content.split(" ");
        String tagName = segments[0];
        writeStartTag(writer, indent, tagName, flag);

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
    }

    // flag = true only when the html content will be written to a new line
    private void parseLine(BufferedWriter writer, String line, boolean flag)
            throws IOException {

        int ltPos = line.indexOf('<');
        if (ltPos > 0) {
            writeRaw(writer, line.substring(0, ltPos));
        } else if (ltPos == -1) {
            writeRaw(writer, line);
            return;
        }

        char c = line.charAt(ltPos + 1);

        // Start tag
        if (c != '/') {
            int gtPos = line.indexOf('>', ltPos + 2);
            if (gtPos != -1) {
                // Self closed or not
                boolean selfClosed = false;
                int endPos = gtPos;
                if (line.charAt(gtPos - 1) == '/') {
                    selfClosed = true;
                    endPos = gtPos - 1;
                }

                String content = line.substring(ltPos + 1, endPos);
                parseStartTag(writer, content, flag);

                if (!selfClosed) {
                    this.indent += 1;
                    writeRaw(writer, ">");
                } else {
                    writeRaw(writer, "/>");
                }

                // Deal with remaining content
                if (gtPos + 1 < line.length()) {
                    String next = line.substring(gtPos + 1);
                    parseLine(writer, next, false);
                }
            } else {
                writeRaw(writer, line);
            }
        }
        // End tag
        else {
            this.indent -= 1;
            int gtPos = line.indexOf('>', ltPos + 2);
            if (gtPos != -1) {
                writeEndTag(writer, indent, line.substring(ltPos + 2, gtPos), flag);
                // Deal with remaining content
                if (gtPos + 1 < line.length()) {
                    String next = line.substring(gtPos + 1);
                    parseLine(writer, next, false);
                }
            } else {
                writeEndTag(writer, indent, line.substring(ltPos + 2), flag);
            }

        }

    }
}