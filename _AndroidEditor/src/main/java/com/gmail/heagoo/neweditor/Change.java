package com.gmail.heagoo.neweditor;

import java.io.Serializable;

public class Change implements Serializable {

    private static final long serialVersionUID = -7699630768546704654L;
    private String newText;
    private String oldText;
    private int start;
    private ChangeType type = ChangeType.NONE;

    public Change(int start, String oldText, String newText) {
        this.start = start;
        this.oldText = oldText;
        this.newText = newText;
        if (oldText.length() == 0 && this.newText.length() > 0) {
            this.type = ChangeType.ADD;
        }
        if (oldText.length() > 0 && this.newText.length() == 0) {
            this.type = ChangeType.REMOVE;
        }
        if (oldText.length() > 0 && this.newText.length() > 0) {
            this.type = ChangeType.REPLACE;
        }
    }

    public int getStart() {
        return this.start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public String getOldText() {
        return this.oldText;
    }

    public void setOldText(String oldText) {
        this.oldText = oldText;
    }

    public String getNewText() {
        return this.newText;
    }

    public void setNewText(String newText) {
        this.newText = newText;
    }

    public ChangeType getType() {
        return this.type;
    }

    public boolean compatible(Change change) {
        if (getType() == ChangeType.ADD && change.getType() == ChangeType.ADD && change.getNewText().length() == 1 && !Character.isWhitespace(change.getNewText().charAt(0)) && getStart() + getNewText().length() == change.getStart()) {
            return true;
        }
        if (getType() == ChangeType.REMOVE && change.getType() == ChangeType.REMOVE && change.getOldText().length() == 1 && !Character.isWhitespace(change.getOldText().charAt(0)) && change.getStart() + change.getOldText().length() == getStart()) {
            return true;
        }
        return false;
    }

    public void add(Change change) {
        if (getType() == ChangeType.ADD && change.getType() == ChangeType.ADD) {
            this.newText += change.getNewText();
        }
        if (getType() == ChangeType.REMOVE && change.getType() == ChangeType.REMOVE) {
            this.oldText = change.getOldText() + this.oldText;
            this.start = change.getStart();
        }
    }

    enum ChangeType {
        NONE,
        ADD,
        REMOVE,
        REPLACE
    }
}