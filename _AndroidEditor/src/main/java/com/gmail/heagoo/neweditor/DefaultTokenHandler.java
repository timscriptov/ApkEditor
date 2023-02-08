package com.gmail.heagoo.neweditor;

import com.gmail.heagoo.neweditor.TokenMarker.LineContext;

public class DefaultTokenHandler implements TokenHandler {
    protected Token firstToken;
    protected Token lastToken;
    protected LineContext lineContext;

    public void init() {
        this.firstToken = null;
        this.lastToken = null;
    }

    public Token getTokens() {
        return this.firstToken;
    }

    public void handleToken(Segment seg, byte id, int offset, int length, LineContext context) {
        Token token = createToken(id, offset, length, context);
        if (token != null) {
            addToken(token, context);
        }
    }

    public LineContext getLineContext() {
        return this.lineContext;
    }

    public void setLineContext(LineContext lineContext) {
        this.lineContext = lineContext;
    }

    protected ParserRuleSet getParserRuleSet(LineContext context) {
        while (context != null) {
            if (!context.rules.isBuiltIn()) {
                return context.rules;
            }
            context = context.parent;
        }
        return null;
    }

    protected Token createToken(byte id, int offset, int length, LineContext context) {
        return new Token(id, offset, length, getParserRuleSet(context));
    }

    protected void addToken(Token token, LineContext context) {
        if (this.firstToken == null) {
            this.lastToken = token;
            this.firstToken = token;
            return;
        }
        this.lastToken.next = token;
        this.lastToken = this.lastToken.next;
    }
}
