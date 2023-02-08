package com.gmail.heagoo.neweditor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class ParserRuleSet {
    private static final ParserRuleSet[] standard = new ParserRuleSet[19];

    static {
        for (byte i = (byte) 0; i < Token.ID_COUNT; i = (byte) (i + 1)) {
            standard[i] = new ParserRuleSet(null, null);
            standard[i].setDefault(i);
            standard[i].builtIn = true;
        }
    }

    private final List<ParserRuleSet> imports;
    private final String modeName;
    private final Map<Character, List<ParserRule>> ruleMap;
    private final String setName;
    private String _noWordSep;
    private boolean builtIn;
    private byte defaultToken;
    private Pattern digitRE;
    private ParserRule escapeRule;
    private boolean highlightDigits;
    private boolean ignoreCase = true;
    private KeywordMap keywords;
    private String noWordSep;
    private Hashtable<String, String> props;
    private int ruleCount;
    private int terminateChar = -1;

    public ParserRuleSet(String modeName, String setName) {
        this.modeName = modeName;
        this.setName = setName;
        this.ruleMap = new HashMap();
        this.imports = new ArrayList();
    }

    public static ParserRuleSet getStandardRuleSet(byte id) {
        return standard[id];
    }

    public String getModeName() {
        return this.modeName;
    }

    public String getSetName() {
        return this.setName;
    }

    public String getName() {
        return this.modeName + "::" + this.setName;
    }

    public Hashtable<String, String> getProperties() {
        return this.props;
    }

    public void setProperties(Hashtable<String, String> props) {
        this.props = props;
        this._noWordSep = null;
    }

    public void resolveImports() {
        for (ParserRuleSet ruleset : this.imports) {
            if (!ruleset.imports.isEmpty()) {
                ruleset.imports.remove(this);
                ruleset.resolveImports();
            }
            for (List<ParserRule> rules : ruleset.ruleMap.values()) {
                for (ParserRule rule : rules) {
                    addRule(rule);
                }
            }
            if (ruleset.keywords != null) {
                if (this.keywords == null) {
                    this.keywords = new KeywordMap(this.ignoreCase);
                }
                this.keywords.add(ruleset.keywords);
            }
        }
        this.imports.clear();
    }

    public void addRuleSet(ParserRuleSet ruleset) {
        this.imports.add(ruleset);
    }

    public void addRule(ParserRule r) {
        Character[] keys;
        int i;
        int i2 = 0;
        this.ruleCount++;
        if (r.upHashChars == null) {
            keys = new Character[1];
            if (r.upHashChar == null || r.upHashChar.length <= 0) {
                keys[0] = null;
            } else {
                keys[0] = Character.valueOf(r.upHashChar[0]);
            }
        } else {
            keys = new Character[r.upHashChars.length];
            char[] cArr = r.upHashChars;
            int length = cArr.length;
            i = 0;
            int i3 = 0;
            while (i < length) {
                int i4 = i3 + 1;
                keys[i3] = Character.valueOf(cArr[i]);
                i++;
                i3 = i4;
            }
        }
        i = keys.length;
        while (i2 < i) {
            Character key = keys[i2];
            List<ParserRule> rules = (List) this.ruleMap.get(key);
            if (rules == null) {
                rules = new ArrayList();
                this.ruleMap.put(key, rules);
            }
            rules.add(r);
            i2++;
        }
    }

    public List<ParserRule> getRules(Character key) {
        boolean emptyForNull;
        boolean emptyForKey = false;
        List<ParserRule> rulesForKey = null;
        List<ParserRule> rulesForNull = (List) this.ruleMap.get(null);
        if (rulesForNull == null || rulesForNull.isEmpty()) {
            emptyForNull = true;
        } else {
            emptyForNull = false;
        }
        Character upperKey = key == null ? null : Character.valueOf(Character.toUpperCase(key.charValue()));
        if (upperKey != null) {
            rulesForKey = (List) this.ruleMap.get(upperKey);
        }
        if (rulesForKey == null || rulesForKey.isEmpty()) {
            emptyForKey = true;
        }
        if (emptyForNull && emptyForKey) {
            return Collections.emptyList();
        }
        if (emptyForKey) {
            return rulesForNull;
        }
        if (emptyForNull) {
            return rulesForKey;
        }
        List<ParserRule> mixed = new ArrayList(rulesForNull.size() + rulesForKey.size());
        mixed.addAll(rulesForKey);
        mixed.addAll(rulesForNull);
        return mixed;
    }

    public int getRuleCount() {
        return this.ruleCount;
    }

    public int getTerminateChar() {
        return this.terminateChar;
    }

    public void setTerminateChar(int atChar) {
        if (atChar < 0) {
            atChar = -1;
        }
        this.terminateChar = atChar;
    }

    public boolean getIgnoreCase() {
        return this.ignoreCase;
    }

    public void setIgnoreCase(boolean b) {
        this.ignoreCase = b;
    }

    public KeywordMap getKeywords() {
        return this.keywords;
    }

    public void setKeywords(KeywordMap km) {
        this.keywords = km;
        this._noWordSep = null;
    }

    public boolean getHighlightDigits() {
        return this.highlightDigits;
    }

    public void setHighlightDigits(boolean highlightDigits) {
        this.highlightDigits = highlightDigits;
    }

    public Pattern getDigitRegexp() {
        return this.digitRE;
    }

    public void setDigitRegexp(Pattern digitRE) {
        this.digitRE = digitRE;
    }

    public ParserRule getEscapeRule() {
        return this.escapeRule;
    }

    public void setEscapeRule(ParserRule escapeRule) {
        this.escapeRule = escapeRule;
    }

    public byte getDefault() {
        return this.defaultToken;
    }

    public void setDefault(byte def) {
        this.defaultToken = def;
    }

    public String getNoWordSep() {
        if (this._noWordSep == null) {
            this._noWordSep = this.noWordSep;
            if (this.noWordSep == null) {
                this.noWordSep = "";
            }
            if (this.keywords != null) {
                this.noWordSep += this.keywords.getNonAlphaNumericChars();
            }
        }
        return this.noWordSep;
    }

    public void setNoWordSep(String noWordSep) {
        this.noWordSep = noWordSep;
        this._noWordSep = null;
    }

    public boolean isBuiltIn() {
        return this.builtIn;
    }

    public String toString() {
        return new StringBuilder(String.valueOf(getClass().getName())).append('[').append(this.modeName).append("::").append(this.setName).append(']').toString();
    }
}