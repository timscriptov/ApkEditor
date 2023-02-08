package com.gmail.heagoo.neweditor;

import java.text.CharacterIterator;

public class Segment implements Cloneable, CharacterIterator, CharSequence {
    public char[] array;
    public int count;
    public int offset;
    private boolean partialReturn;
    private int pos;

    public Segment() {
        this(null, 0, 0);
    }

    public Segment(char[] array, int offset, int count) {
        this.array = array;
        this.offset = offset;
        this.count = count;
        this.partialReturn = false;
    }

    public boolean isPartialReturn() {
        return this.partialReturn;
    }

    public void setPartialReturn(boolean p) {
        this.partialReturn = p;
    }

    public String toString() {
        if (this.array != null) {
            return new String(this.array, this.offset, this.count);
        }
        return new String();
    }

    public char first() {
        this.pos = this.offset;
        if (this.count != 0) {
            return this.array[this.pos];
        }
        return '\uffff';
    }

    public char last() {
        this.pos = this.offset + this.count;
        if (this.count == 0) {
            return '\uffff';
        }
        this.pos--;
        return this.array[this.pos];
    }

    public char current() {
        if (this.count == 0 || this.pos >= this.offset + this.count) {
            return '\uffff';
        }
        return this.array[this.pos];
    }

    public char next() {
        this.pos++;
        int end = this.offset + this.count;
        if (this.pos < end) {
            return current();
        }
        this.pos = end;
        return '\uffff';
    }

    public char previous() {
        if (this.pos == this.offset) {
            return '\uffff';
        }
        this.pos--;
        return current();
    }

    public char setIndex(int position) {
        int end = this.offset + this.count;
        if (position < this.offset || position > end) {
            throw new IllegalArgumentException("bad position: " + position);
        }
        this.pos = position;
        if (this.pos == end || this.count == 0) {
            return '\uffff';
        }
        return this.array[this.pos];
    }

    public int getBeginIndex() {
        return this.offset;
    }

    public int getEndIndex() {
        return this.offset + this.count;
    }

    public int getIndex() {
        return this.pos;
    }

    public char charAt(int index) {
        if (index >= 0 && index < this.count) {
            return this.array[this.offset + index];
        }
        throw new StringIndexOutOfBoundsException(index);
    }

    public int length() {
        return this.count;
    }

    public CharSequence subSequence(int start, int end) {
        if (start < 0) {
            throw new StringIndexOutOfBoundsException(start);
        } else if (end > this.count) {
            throw new StringIndexOutOfBoundsException(end);
        } else if (start > end) {
            throw new StringIndexOutOfBoundsException(end - start);
        } else {
            Segment segment = new Segment();
            segment.array = this.array;
            segment.offset = this.offset + start;
            segment.count = end - start;
            return segment;
        }
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            return null;
        }
    }
}