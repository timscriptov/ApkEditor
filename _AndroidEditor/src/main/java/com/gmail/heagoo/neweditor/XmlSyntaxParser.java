package com.gmail.heagoo.neweditor;

import android.util.Log;

import org.xml.sax.Attributes;
import org.xml.sax.helpers.DefaultHandler;

import java.util.Hashtable;
import java.util.Stack;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class XmlSyntaxParser extends DefaultHandler {

    private TokenMarker marker;

    private KeywordMap keywords;
    private ParserRuleSet rules;

    private String modeName = "xml";
    private String propName;
    private String propValue;
    private Stack<TagDecl> stateStack;
    private Hashtable<String, String> props;

    // private Vector<Mode> reloadModes;

    public XmlSyntaxParser() {
        this.marker = new TokenMarker();
        this.marker.addRuleSet(new ParserRuleSet("xml", "MAIN"));
        this.stateStack = new Stack<TagDecl>();

        // Copied from startDocument
        this.props = new Hashtable<String, String>();
        pushElement(null, null);
        // this.reloadModes = new Vector();
    }

    public void startElement(String uri, String localName, String qName,
                             Attributes attrs) {
        TagDecl tag = pushElement(qName, attrs);
        if (qName.equals("WHITESPACE")) {
            // Logger.log(Logger.WARNING, (Object) this, this.modeName +
            // ": WHITESPACE rule " + "no longer needed");
        } else if (qName.equals("KEYWORDS")) {
            this.keywords = new KeywordMap(this.rules.getIgnoreCase());
        } else if (qName.equals("RULES")) {
            if (tag.lastSetName == null) {
                tag.lastSetName = "MAIN";
            }
            this.rules = this.marker.getRuleSet(tag.lastSetName);
            if (this.rules == null) {
                this.rules = new ParserRuleSet(this.modeName, tag.lastSetName);
                this.marker.addRuleSet(this.rules);
            }
            this.rules.setIgnoreCase(tag.lastIgnoreCase);
            this.rules.setHighlightDigits(tag.lastHighlightDigits);
            if (tag.lastDigitRE != null) {
                try {
                    this.rules.setDigitRegexp(Pattern.compile(tag.lastDigitRE,
                            tag.lastIgnoreCase ? 2 : 0));
                } catch (Exception e) {
                    error("regexp", e);
                }
            }
            if (tag.lastEscape != null) {
                this.rules.setEscapeRule(ParserRule
                        .createEscapeRule(tag.lastEscape));
            }
            this.rules.setDefault(tag.lastDefaultID);
            this.rules.setNoWordSep(tag.lastNoWordSep);
        }
    }

    public void endElement(String uri, String localName, String name) {
        TagDecl tag = popElement();
        if (name.equals(tag.tagName)) {
            // IMPORT omitted
            if (tag.tagName.equals("PROPERTY")) {
                this.props.put(this.propName, this.propValue);
                return;
            } else if (tag.tagName.equals("PROPS")) {
                if (peekElement().tagName.equals("RULES")) {
                    this.rules.setProperties(this.props);
                } else {
                    // this.modeProps = this.props;
                }
                this.props = new Hashtable();
                return;
            } else if (tag.tagName.equals("RULES")) {
                this.rules.setKeywords(this.keywords);
                this.keywords = null;
                this.rules = null;
                return;
            } else if (tag.tagName.equals("IMPORT")) {
                if (!this.rules.equals(tag.lastDelegateSet)) {
                    this.rules.addRuleSet(tag.lastDelegateSet);
                    return;
                }
                return;
            } else if (tag.tagName.equals("TERMINATE")) {
                this.rules.setTerminateChar(tag.termChar);
                return;
            } else if (tag.tagName.equals("SEQ")) {
                if (tag.lastStart == null) {
                    error("empty-tag", "SEQ");
                    return;
                } else {
                    this.rules.addRule(ParserRule.createSequenceRule(
                            tag.lastStartPosMatch, tag.lastStart.toString(),
                            tag.lastDelegateSet, tag.lastTokenID));
                    return;
                }
            } else if (tag.tagName.equals("SEQ_REGEXP")) {
                if (tag.lastStart == null) {
                    error("empty-tag", "SEQ_REGEXP");
                    return;
                }
                try {
                    if (tag.lastHashChars != null) {
                        this.rules.addRule(ParserRule.createRegexpSequenceRule(
                                tag.lastStartPosMatch,
                                tag.lastHashChars.toCharArray(),
                                tag.lastStart.toString(), tag.lastDelegateSet,
                                tag.lastTokenID,
                                findParent("RULES").lastIgnoreCase));
                        return;
                    } else {
                        this.rules.addRule(ParserRule.createRegexpSequenceRule(
                                tag.lastHashChar, tag.lastStartPosMatch,
                                tag.lastStart.toString(), tag.lastDelegateSet,
                                tag.lastTokenID,
                                findParent("RULES").lastIgnoreCase));
                        return;
                    }
                } catch (PatternSyntaxException re) {
                    error("regexp", re);
                    return;
                }
            } else if (tag.tagName.equals("SPAN")) {
                if (tag.lastStart == null) {
                    error("empty-tag", "BEGIN");
                    return;
                } else if (tag.lastEnd == null) {
                    error("empty-tag", "END");
                    return;
                } else {
                    this.rules.addRule(ParserRule.createSpanRule(
                            tag.lastStartPosMatch, tag.lastStart.toString(),
                            tag.lastEndPosMatch, tag.lastEnd.toString(),
                            tag.lastDelegateSet, tag.lastTokenID,
                            tag.lastMatchType, tag.lastNoLineBreak,
                            tag.lastNoWordBreak, tag.lastEscape));
                    return;
                }
            } else if (tag.tagName.equals("SPAN_REGEXP")) {
                if (tag.lastStart == null) {
                    error("empty-tag", "BEGIN");
                    return;
                } else if (tag.lastEnd == null) {
                    error("empty-tag", "END");
                    return;
                } else {
                    try {
                        if (tag.lastHashChars != null) {
                            this.rules.addRule(ParserRule.createRegexpSpanRule(
                                    tag.lastStartPosMatch,
                                    tag.lastHashChars.toCharArray(),
                                    tag.lastStart.toString(),
                                    tag.lastEndPosMatch,
                                    tag.lastEnd.toString(),
                                    tag.lastDelegateSet, tag.lastTokenID,
                                    tag.lastMatchType, tag.lastNoLineBreak,
                                    tag.lastNoWordBreak,
                                    findParent("RULES").lastIgnoreCase,
                                    tag.lastEscape, tag.lastEndRegexp));
                            return;
                        }
                        this.rules.addRule(ParserRule.createRegexpSpanRule(
                                tag.lastHashChar, tag.lastStartPosMatch,
                                tag.lastStart.toString(), tag.lastEndPosMatch,
                                tag.lastEnd.toString(), tag.lastDelegateSet,
                                tag.lastTokenID, tag.lastMatchType,
                                tag.lastNoLineBreak, tag.lastNoWordBreak,
                                findParent("RULES").lastIgnoreCase,
                                tag.lastEscape, tag.lastEndRegexp));
                        return;
                    } catch (PatternSyntaxException re2) {
                        error("regexp", re2);
                        return;
                    }
                }
            } else if (tag.tagName.equals("EOL_SPAN")) {
                if (tag.lastStart == null) {
                    error("empty-tag", "EOL_SPAN");
                    return;
                } else {
                    this.rules.addRule(ParserRule.createEOLSpanRule(
                            tag.lastStartPosMatch, tag.lastStart.toString(),
                            tag.lastDelegateSet, tag.lastTokenID,
                            tag.lastMatchType));
                    return;
                }
            } else if (tag.tagName.equals("EOL_SPAN_REGEXP")) {
                if (tag.lastStart == null) {
                    error("empty-tag", "EOL_SPAN_REGEXP");
                    return;
                }
                try {
                    if (tag.lastHashChars != null) {
                        this.rules.addRule(ParserRule.createRegexpEOLSpanRule(
                                tag.lastStartPosMatch,
                                tag.lastHashChars.toCharArray(),
                                tag.lastStart.toString(), tag.lastDelegateSet,
                                tag.lastTokenID, tag.lastMatchType,
                                findParent("RULES").lastIgnoreCase));
                        return;
                    } else {
                        this.rules.addRule(ParserRule.createRegexpEOLSpanRule(
                                tag.lastHashChar, tag.lastStartPosMatch,
                                tag.lastStart.toString(), tag.lastDelegateSet,
                                tag.lastTokenID, tag.lastMatchType,
                                findParent("RULES").lastIgnoreCase));
                        return;
                    }
                } catch (PatternSyntaxException re22) {
                    error("regexp", re22);
                    return;
                }
            } else if (tag.tagName.equals("MARK_FOLLOWING")) {
                if (tag.lastStart == null) {
                    error("empty-tag", "MARK_FOLLOWING");
                    return;
                } else {
                    this.rules.addRule(ParserRule.createMarkFollowingRule(
                            tag.lastStartPosMatch, tag.lastStart.toString(),
                            tag.lastTokenID, tag.lastMatchType));
                    return;
                }
            } else if (tag.tagName.equals("MARK_PREVIOUS")) {
                if (tag.lastStart == null) {
                    error("empty-tag", "MARK_PREVIOUS");
                    return;
                } else {
                    this.rules.addRule(ParserRule.createMarkPreviousRule(
                            tag.lastStartPosMatch, tag.lastStart.toString(),
                            tag.lastTokenID, tag.lastMatchType));
                    return;
                }
            } else if (!tag.tagName.equals("END")
                    && !tag.tagName.equals("BEGIN")
                    && !tag.tagName.equals("KEYWORDS")
                    && !tag.tagName.equals("MODE")) {
                byte token = Token.stringToToken(tag.tagName);
                if (token == (byte) -1) {
                    return;
                }
                if (tag.lastKeyword == null || tag.lastKeyword.length() == 0) {
                    error("empty-keyword", null);
                    return;
                } else {
                    addKeyword(tag.lastKeyword.toString(), token);
                    return;
                }
            } else {
                return;
            }
        }
        throw new InternalError();
    }

    @Override
    public void characters(char[] c, int off, int len) {
        peekElement().setText(c, off, len);
    }

    private void addKeyword(String k, byte id) {
        if (this.keywords != null) {
            this.keywords.add(k, id);
        }
    }

    private TagDecl popElement() {
        return (TagDecl) this.stateStack.pop();
    }

    private TagDecl pushElement(String name, Attributes attrs) {
        TagDecl tag = null;
        if (name != null) {
            tag = new TagDecl(name, attrs);
        }
        this.stateStack.push(tag);
        return tag;
    }

    private TagDecl peekElement() {
        return (TagDecl) this.stateStack.peek();
    }

    private TagDecl findParent(String tagName) {
        for (int idx = this.stateStack.size() - 1; idx >= 0; idx--) {
            TagDecl tag = (TagDecl) this.stateStack.get(idx);
            if (tag.tagName.equals(tagName)) {
                return tag;
            }
        }
        return null;
    }

    protected void error(String str, Object obj) {
        Log.d("DEBUG", "error occurred: " + str + ", " + obj.toString());
    }

    public TokenMarker getTokenMarker() {
        return this.marker;
    }

    private class TagDecl {
        public boolean lastAtLineStart;
        public boolean lastAtWhitespaceEnd;
        public boolean lastAtWordStart;
        public byte lastDefaultID = (byte) 0;
        public ParserRuleSet lastDelegateSet;
        public String lastDigitRE;
        public StringBuffer lastEnd;
        public int lastEndPosMatch;
        public boolean lastEndRegexp;
        public String lastEscape;
        public String lastHashChar;
        public String lastHashChars;
        public boolean lastHighlightDigits;
        public boolean lastIgnoreCase = true;
        public StringBuffer lastKeyword;
        public byte lastMatchType;
        public boolean lastNoLineBreak;
        public boolean lastNoWordBreak;
        public String lastNoWordSep = "_";
        public boolean lastRegexp;
        public String lastSetName;
        public StringBuffer lastStart;
        public int lastStartPosMatch;
        public byte lastTokenID;
        public ParserRuleSet rules;
        public String tagName;
        public int termChar = -1;

        public TagDecl(String tagName, Attributes attrs) {
            boolean z = true;
            this.tagName = tagName;
            XmlSyntaxParser.this.propName = attrs.getValue("NAME");
            XmlSyntaxParser.this.propValue = attrs.getValue("VALUE");
            String tmp = attrs.getValue("TYPE");
            if (tmp != null) {
                this.lastTokenID = Token.stringToToken(tmp);
                if (this.lastTokenID == (byte) -1) {
                    XmlSyntaxParser.this.error("token-invalid", tmp);
                }
            }
            this.lastMatchType = (byte) -2;
            tmp = attrs.getValue("EXCLUDE_MATCH");
            if (tmp != null && "TRUE".equalsIgnoreCase(tmp)) {
                this.lastMatchType = (byte) -1;
            }
            tmp = attrs.getValue("MATCH_TYPE");
            if (tmp != null) {
                if ("CONTEXT".equals(tmp)) {
                    this.lastMatchType = (byte) -1;
                } else if ("RULE".equals(tmp)) {
                    this.lastMatchType = (byte) -2;
                } else {
                    this.lastMatchType = Token.stringToToken(tmp);
                    if (this.lastMatchType == (byte) -1) {
                        XmlSyntaxParser.this.error("token-invalid", tmp);
                    }
                }
            }
            this.lastAtLineStart = "TRUE".equals(attrs
                    .getValue("AT_LINE_START"));
            this.lastAtWhitespaceEnd = "TRUE".equals(attrs
                    .getValue("AT_WHITESPACE_END"));
            this.lastAtWordStart = "TRUE".equals(attrs
                    .getValue("AT_WORD_START"));
            this.lastNoLineBreak = "TRUE".equals(attrs
                    .getValue("NO_LINE_BREAK"));
            this.lastNoWordBreak = "TRUE".equals(attrs
                    .getValue("NO_WORD_BREAK"));
            if (!(attrs.getValue("IGNORE_CASE") == null || "TRUE".equals(attrs
                    .getValue("IGNORE_CASE")))) {
                z = false;
            }
            this.lastIgnoreCase = z;
            this.lastHighlightDigits = "TRUE".equals(attrs
                    .getValue("HIGHLIGHT_DIGITS"));
            this.lastRegexp = "TRUE".equals(attrs.getValue("REGEXP"));
            this.lastDigitRE = attrs.getValue("DIGIT_RE");
            tmp = attrs.getValue("NO_WORD_SEP");
            if (tmp != null) {
                this.lastNoWordSep = tmp;
            }
            tmp = attrs.getValue("AT_CHAR");
            if (tmp != null) {
                try {
                    this.termChar = Integer.parseInt(tmp);
                } catch (NumberFormatException e) {
                    XmlSyntaxParser.this.error("termchar-invalid", tmp);
                    this.termChar = -1;
                }
            }
            this.lastEscape = attrs.getValue("ESCAPE");
            this.lastSetName = attrs.getValue("SET");
            tmp = attrs.getValue("DELEGATE");
            if (tmp != null) {
                String delegateMode;
                String delegateSetName;
                int index = tmp.indexOf("::");
                if (index != -1) {
                    delegateMode = tmp.substring(0, index);
                    delegateSetName = tmp.substring(index + 2);
                } else {
                    delegateMode = XmlSyntaxParser.this.modeName;
                    delegateSetName = tmp;
                }
                // TokenMarker delegateMarker = XmlTokenMarker.this
                // .getTokenMarker(delegateMode);
                TokenMarker delegateMarker = XmlSyntaxParser.this
                        .getTokenMarker();
                if (delegateMarker == null) {
                    XmlSyntaxParser.this.error("delegate-invalid", tmp);
                } else {
                    this.lastDelegateSet = delegateMarker
                            .getRuleSet(delegateSetName);
                    if (delegateMarker == XmlSyntaxParser.this.marker
                            && this.lastDelegateSet == null) {
                        this.lastDelegateSet = new ParserRuleSet(delegateMode,
                                delegateSetName);
                        this.lastDelegateSet.setDefault((byte) 7);
                        XmlSyntaxParser.this.marker
                                .addRuleSet(this.lastDelegateSet);
                    } else if (this.lastDelegateSet == null) {
                        XmlSyntaxParser.this.error("delegate-invalid", tmp);
                    }
                }
            }
            tmp = attrs.getValue("DEFAULT");
            if (tmp != null) {
                this.lastDefaultID = Token.stringToToken(tmp);
                if (this.lastDefaultID == (byte) -1) {
                    XmlSyntaxParser.this.error("token-invalid", tmp);
                    this.lastDefaultID = (byte) 0;
                }
            }
            this.lastHashChar = attrs.getValue("HASH_CHAR");
            this.lastHashChars = attrs.getValue("HASH_CHARS");
            if (this.lastHashChar != null && this.lastHashChars != null) {
                XmlSyntaxParser.this.error(
                        "hash-char-and-hash-chars-mutually-exclusive", null);
                this.lastHashChars = null;
            }
        }

        public void setText(char[] c, int off, int len) {
            int i = 8;
            int i2 = 4;
            int i3 = 2;
            TagDecl target;
            if (this.tagName.equals("EOL_SPAN")
                    || this.tagName.equals("EOL_SPAN_REGEXP")
                    || this.tagName.equals("MARK_PREVIOUS")
                    || this.tagName.equals("MARK_FOLLOWING")
                    || this.tagName.equals("SEQ")
                    || this.tagName.equals("SEQ_REGEXP")
                    || this.tagName.equals("BEGIN")) {
                target = this;
                if (this.tagName.equals("BEGIN")) {
                    target = (TagDecl) XmlSyntaxParser.this.stateStack
                            .get(XmlSyntaxParser.this.stateStack.size() - 2);
                }
                if (target.lastStart == null) {
                    target.lastStart = new StringBuffer();
                    target.lastStart.append(c, off, len);
                    if (!target.lastAtLineStart) {
                        i3 = 0;
                    }
                    if (!target.lastAtWhitespaceEnd) {
                        i2 = 0;
                    }
                    i2 |= i3;
                    if (target.lastAtWordStart) {
                        i3 = 8;
                    } else {
                        i3 = 0;
                    }
                    target.lastStartPosMatch = i3 | i2;
                    target.lastAtLineStart = false;
                    target.lastAtWordStart = false;
                    target.lastAtWhitespaceEnd = false;
                    return;
                }
                target.lastStart.append(c, off, len);
            } else if (this.tagName.equals("END")) {
                target = (TagDecl) XmlSyntaxParser.this.stateStack
                        .get(XmlSyntaxParser.this.stateStack.size() - 2);
                if (target.lastEnd == null) {
                    target.lastEnd = new StringBuffer();
                    target.lastEnd.append(c, off, len);
                    if (!this.lastAtLineStart) {
                        i3 = 0;
                    }
                    if (!this.lastAtWhitespaceEnd) {
                        i2 = 0;
                    }
                    i3 |= i2;
                    if (!this.lastAtWordStart) {
                        i = 0;
                    }
                    target.lastEndPosMatch = i3 | i;
                    target.lastEndRegexp = this.lastRegexp;
                    target.lastAtLineStart = false;
                    target.lastAtWordStart = false;
                    target.lastAtWhitespaceEnd = false;
                    return;
                }
                target.lastEnd.append(c, off, len);
            } else {
                if (this.lastKeyword == null) {
                    this.lastKeyword = new StringBuffer();
                }
                this.lastKeyword.append(c, off, len);
            }
        }
    }

}
