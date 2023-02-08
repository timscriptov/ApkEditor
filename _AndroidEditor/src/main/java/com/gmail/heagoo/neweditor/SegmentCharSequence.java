package com.gmail.heagoo.neweditor;

import java.io.Serializable;

public class SegmentCharSequence implements CharSequence, Serializable {
    private static final long serialVersionUID = -8718409144053615735L;
    private int length;
    private int offset;
    private Segment seg;

    public SegmentCharSequence(Segment seg) {
        this(seg, 0, seg.count);
    }

    public SegmentCharSequence(Segment seg, int off, int len) {
        this.offset = off;
        this.length = len;
        this.seg = seg;
    }

    public char charAt(int index) {
        return this.seg.array[(this.seg.offset + this.offset) + index];
    }

    public int length() {
        return this.length;
    }

    public CharSequence subSequence(int start, int end) {
        return new SegmentCharSequence(this.seg, this.offset + start, end - start);
    }

    public String toString() {
        return new String(this.seg.array, this.offset + this.seg.offset, this.length);
    }
}
