package com.gmail.heagoo.apkeditor.util;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class TimeDumper {
    private boolean bDump;
    private long startTime;
    private long lastTime;

    private Map<String, Long[]> tag2Time = new HashMap<String, Long[]>();

    public TimeDumper(boolean bDump) {
        this.bDump = bDump;
        this.startTime = System.currentTimeMillis();
        this.lastTime = startTime;
    }

    public void lastTime(String str) {
        if (bDump) {
            long curTime = System.currentTimeMillis();
            Log.d("DEBUG", str + ": " + (curTime - lastTime) / 1000.0
                    + " seconds");
            this.lastTime = curTime;
        }
    }

    public void accumulateTime(String tag) {
        long curTime = System.currentTimeMillis();
        Long[] time = tag2Time.get(tag);
        if (time == null) {
            time = new Long[1];
            time[0] = 0L;
        }
        time[0] += curTime - lastTime;
        this.lastTime = curTime;
    }

    public void dumpTime(String tag) {
        Log.d("DEBUG", tag + ": " + tag2Time.get(tag)[0] / 1000.0 + " seconds");
    }

    public void totalTime(String str) {
        if (bDump) {
            long curTime = System.currentTimeMillis();
            Log.d("DEBUG", str + ": " + (curTime - startTime) / 1000.0
                    + " seconds");
        }
    }
}