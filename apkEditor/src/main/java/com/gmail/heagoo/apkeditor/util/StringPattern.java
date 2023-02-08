package com.gmail.heagoo.apkeditor.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringPattern {

    private static String getTag(String str, int start, int end) {
        int offset = 1;
        if (str.charAt(start + 1) == '/') {
            offset = 2;
        }

        int position = str.indexOf(' ', start);
        if (position == -1 || position >= end) {
            return str.substring(start + offset, end - 1);
        } else {
            return str.substring(start + offset, position);
        }
    }

    public static List<TagRecord> getHtmlTags(String str) {
//		int position = str.indexOf('<');
//		if (position == -1) {
//			return false;
//		}

        List<TagRecord> allTags = new ArrayList<TagRecord>();

        Pattern pattern1 = Pattern.compile("<[a-zA-Z].*?>");
        Matcher matcher1 = pattern1.matcher(str);
        while (matcher1.find()) {
            int start = matcher1.start();
            int end = matcher1.end();
            String startTag = getTag(str, start, end);
            allTags.add(new TagRecord(startTag, start, end, true));
        }

        // No start tag
        int startTagSize = allTags.size();
        if (startTagSize == 0) {
            return null;
        }

        Pattern pattern2 = Pattern.compile("</[a-zA-Z].*?>");
        Matcher matcher2 = pattern2.matcher(str);
        while (matcher2.find()) {
            int start = matcher2.start();
            int end = matcher2.end();
            String endTag = getTag(str, start, end);
            allTags.add(new TagRecord(endTag, start, end, false));
        }

        // No end ta
        if (allTags.size() == startTagSize) {
            return null;
        }

        Collections.sort(allTags, new MyComparator());
//
//		for (TagRecord rec : allTags) {
//			System.out.println(rec.toString());
//		}
//
        List<TagRecord> matchedHtmlTags = new ArrayList<>();

        // Check the start tag and end tag whether match
        Stack<TagRecord> stackedTags = new Stack<>();
        for (TagRecord curTag : allTags) {
            if (curTag.bStartTag) {
                stackedTags.push(curTag);
            } else {
                if (!stackedTags.isEmpty()) {
                    TagRecord preTag = stackedTags.pop();
                    String preName = preTag.tag;
                    if (preName.equals(curTag.tag)) {
                        matchedHtmlTags.add(preTag);
                        matchedHtmlTags.add(curTag);
                    } else {
                        break;
                    }
                } else {
                    break;
                }
            }
        }

        Collections.sort(matchedHtmlTags, new MyComparator());
        return matchedHtmlTags;
    }

    public static class TagRecord {
        public int startPos;
        public int endPos;
        String tag;
        boolean bStartTag;

        public TagRecord(String tag, int startPos, int endPos, boolean bStartTag) {
            this.tag = tag;
            this.startPos = startPos;
            this.endPos = endPos;
            this.bStartTag = bStartTag;
        }

        public String toString() {
            return tag + ", position=" + startPos + ", bStartTag=" + bStartTag;
        }
    }

    static class MyComparator implements Comparator<TagRecord> {
        @Override
        public int compare(TagRecord arg0, TagRecord arg1) {
            if (arg0.startPos > arg1.startPos) {
                return 1;
            } else if (arg0.startPos < arg1.startPos) {
                return -1;
            } else {
                return 0;
            }
        }
    }

//	public static void main(String[] args) {
//		String str = ">haha</b>";
//		System.out.println(str + ": " + looksLikeHtml(str));
//	}
}
