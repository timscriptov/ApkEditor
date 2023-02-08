package com.gmail.heagoo.sqliteutil.util;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

public class MySimpleAdapter extends SimpleAdapter {
    private int color1;
    private int color2;

    public MySimpleAdapter(Context context, List<Map<String, String>> items,
                           int resource, String[] from, int[] to, boolean isDark) {
        super(context, items, resource, from, to);
        if (isDark) {
            color1 = 0xffcccccc;
            color2 = 0xffaaaaaa;
        } else {
            color1 = 0xff333333;
            color2 = 0xff555555;
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = super.getView(position, convertView, parent);
        TextView tv1 = (TextView) view.findViewById(android.R.id.text1);
        tv1.setTextColor(color1);
        TextView tv2 = (TextView) view.findViewById(android.R.id.text2);
        tv2.setTextColor(color2);
        return view;
    }
}