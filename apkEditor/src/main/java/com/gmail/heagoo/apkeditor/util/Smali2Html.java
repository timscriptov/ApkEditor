package com.gmail.heagoo.apkeditor.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Smali2Html {

    private BufferedReader reader;
    private BufferedWriter writer;

    private List<String> lines = new ArrayList<>();

    // Current dealing content is inside a string
    private boolean inString = false;
    ;

    public Smali2Html(String inputFile) {
        try {
            this.reader = new BufferedReader(new FileReader(inputFile));

            String line = reader.readLine();
            while (line != null) {
                lines.add(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

//	public static void main(String[] args) throws IOException {
//		Smali2Html sh = new Smali2Html("D:\\Temp\\backup\\am.smali");
//		sh.transformTo("D:\\Temp\\backup\\am.html");
//	}

    public void transformTo(String outFile) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(outFile));

        writer.write("<html>");
        writer.write("<head>");
        writer.write("<title>1.xml</title>");
        writer.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"viewsource.css\">");
        writer.write("</head>");
        writer.write("<body id=\"viewsource\" class=\"wrap\" style=\"-moz-tab-size: 4\">");
        writer.write("<pre id=\"line1\">" + lines.get(0) + "\n");

        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i);
            // Empty line
            if (line.trim().isEmpty()) {
                writer.write("<span id=\"line" + (i + 1) + "\"></span>\n");
                continue;
            }

            writer.write("<span id=\"line" + (i + 1) + "\">");

            // Deal with the blank head
            int index = 0; // The real content starts at
            String[] words = line.split("\\s+");

            if (words[0].equals("")) {
                index = 1;
                writer.write("    ");
            }

            // Directive, instruction, statement
            String strColor = getInstructionColor(words[index]);
            writer.write("<font color=\"" + strColor + "\">");
            writer.write(words[index]);
            writer.write("</font>");
            index += 1;

            for (int k = index; k < words.length; k++) {
                writer.write(" ");
                String word = words[k];
                String html = generateHtml(word);
                writer.write(html);
            }

            writer.write("</span>\n");
        }

        writer.write("</body>");
        writer.write("</html>");

        writer.close();
    }

    // Get the color string according to the string category
    private String getInstructionColor(String str) {
        if (str.startsWith(".")) {
            return "#FF3399";
        } else if (str.startsWith(":")) {
            return "brown";
        } else {
            return "green";
        }
    }

    private String generateHtml(String str) {
        // const string starts
        if (str.startsWith("\"")) {
            if (!str.endsWith("\"")) {
                this.inString = true;
                return "<font color=\"blue\">" + getRawHtml(str);
            } else { // self closed
                return "<font color=\"blue\">" + getRawHtml(str) + "</font>";
            }
        }
        // const string ends
        else if (str.endsWith("\"")) {
            this.inString = false;
            return getRawHtml(str) + "</font>";
        }
        // inside a string
        if (this.inString) {
            return getRawHtml(str);
        }

        if (str.startsWith("L")) {
            int endPos = str.indexOf(';');
            if (endPos != -1)
                return "L<font color=\"red\">" + str.substring(1, endPos)
                        + "</font>;" + str.substring(endPos + 1);
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '>') {
                sb.append("&gt;");
            } else if (c == '<') {
                sb.append("&lt;");
            }
            // deal with p0, v2, v3, etc
            else if ((c == 'v' || c == 'p') && i + 1 < str.length()
                    && Character.isDigit(str.charAt(i + 1))) {
                sb.append("<font color=\"red\">");
                sb.append(c);
                sb.append(str.charAt(i + 1));
                i++;
                // 2 digits like v10
                if (i + 1 < str.length()
                        && Character.isDigit(str.charAt(i + 1))) {
                    sb.append(str.charAt(i + 1));
                    i++;
                }
                sb.append("</font>");
            } else {
                sb.append(c);
            }
        }

        return sb.toString();
    }

    private String getRawHtml(String content) {
        return content.replaceAll("<", "&lt;").replace(">", "&gt;");
    }

    private void writeRaw(String content) throws IOException {
        writer.write(getRawHtml(content));
    }
}
