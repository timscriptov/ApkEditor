package com.gmail.heagoo.apkeditor.ac;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

import com.gmail.heagoo.apkeditor.base.R;

import java.util.ArrayList;
import java.util.List;

public class AutoCompleteAdapter extends BaseAdapter implements Filterable {

    private static final int MAX_RECORDS = 32;
    public List<String> filteredData;
    private Context ctx;
    private String tag;
    private String[] historyWords;
    private ItemFilter filter;

    public AutoCompleteAdapter(Context ctx, String tag) {
        this.ctx = ctx;
        this.tag = tag;
    }

    private void init() {
        this.filter = new ItemFilter();

        // it list data
        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        String history = sp.getString(tag, "");
        // history =
        // "Test\nhello\nandroid:text\nandroid\nhello12\nandroid12\n123\n456\n789\n332";
        if (!history.equals("")) {
            this.historyWords = history.split("\n");
        } else {
            this.historyWords = new String[0];
        }

        this.filteredData = new ArrayList<String>();
        for (String word : historyWords) {
            this.filteredData.add(word);
        }
    }

    @Override
    public int getCount() {
        if (filteredData == null) {
            init();
        }
        return filteredData.size();
    }

    @Override
    public Object getItem(int position) {
        if (filteredData == null) {
            init();
        }
        return filteredData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        String word = filteredData.get(position);
        ViewHolder viewHolder = null;
        // sawsem theme
        if (convertView == null) {
//            if (GlobalConfig.instance(ctx).isDarkTheme()) {
//                convertView = LayoutInflater.from(ctx).inflate(
//                        R.layout.item_autocomplete_dark, null);
//            } else {
            convertView = LayoutInflater.from(ctx).inflate(
                    R.layout.item_autocomplete, null);
            //           }

            viewHolder = new ViewHolder();
            viewHolder.filename = (TextView) convertView
                    .findViewById(R.id.filename);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.filename.setText(word);

        return convertView;
    }

    @Override
    public Filter getFilter() {
        if (this.filter == null) {
            init();
        }
        return this.filter;
    }

    public void addInputHistory(String input) {
        if (filteredData == null) {
            init();
        }

        SharedPreferences sp = PreferenceManager
                .getDefaultSharedPreferences(ctx);
        Editor editor = sp.edit();

        ArrayList<String> updatedHistory = new ArrayList<String>();
        updatedHistory.add(input);
        StringBuilder sb = new StringBuilder();
        sb.append(input);

        for (String word : historyWords) {
            if (!word.equals(input)) {
                updatedHistory.add(word);
                sb.append("\n");
                sb.append(word);
                if (updatedHistory.size() >= MAX_RECORDS) {
                    break;
                }
            }
        }

        this.historyWords = updatedHistory.toArray(new String[updatedHistory
                .size()]);
        editor.putString(this.tag, sb.toString());
        editor.commit();
    }

    static class ViewHolder {
        public TextView filename;
    }

    private class ItemFilter extends Filter {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            if (constraint == null) {
                int count = historyWords.length;
                final ArrayList<String> nlist = new ArrayList<String>(count);
                for (String word : historyWords) {
                    nlist.add(word);
                }
                FilterResults results = new FilterResults();
                results.values = nlist;
                results.count = nlist.size();
                return results;
            }

            String filterString = constraint.toString().toLowerCase();

            FilterResults results = new FilterResults();

            int count = historyWords.length;
            final ArrayList<String> nlist = new ArrayList<String>(count);

            String filterableString;

            for (int i = 0; i < count; i++) {
                filterableString = historyWords[i];
                if (filterableString.toLowerCase().contains(filterString)) {
                    nlist.add(filterableString);
                }
            }

            results.values = nlist;
            results.count = nlist.size();

            return results;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint,
                                      FilterResults results) {
            filteredData = (ArrayList<String>) results.values;
            notifyDataSetChanged();
        }

    }
}
