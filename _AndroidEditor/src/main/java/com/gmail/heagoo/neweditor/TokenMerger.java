package com.gmail.heagoo.neweditor;

public class TokenMerger {

    public static Token merge(Token token) {
        if (token == null) {
            return null;
        }

        Token head = token;
        Token previous = token;
        Token curToken = token.next;

        while (curToken != null) {
            // The same type and continuous, merge it
            if (curToken.id == previous.id
                    && curToken.offset == previous.offset + previous.length) {
                previous.length += curToken.length;
            }
            // Not the same type, add a node to the result
            else {
                previous.next = curToken;
                previous = curToken;
            }

            // iterate next
            curToken = curToken.next;
        }

        previous.next = null;

        return head;
    }

}
