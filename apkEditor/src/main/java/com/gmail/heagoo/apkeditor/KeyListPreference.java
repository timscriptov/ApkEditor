package com.gmail.heagoo.apkeditor;

import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.util.AttributeSet;

import com.gmail.heagoo.apkeditor.base.R;

public class KeyListPreference extends ListPreference implements
        OnPreferenceClickListener {

    private static final int customKeyIndex = 2;
    private Context context;

    /**
     * @param context
     * @param attrs
     */
    public KeyListPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public boolean onPreferenceClick(Preference preference) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void onPrepareDialogBuilder(Builder builder) {
        int checkItem = getValueIndex(getValue());
        builder.setSingleChoiceItems(R.array.signer_key, checkItem,
                new DialogInterface.OnClickListener() {

                    public void onClick(DialogInterface dialog, int which) {
                        if (customKeyIndex == which) {
                            new KeySelectDlgHelper(KeyListPreference.this)
                                    .showDialog(context);
                        } else {
                            setValue(getEntryValues()[which] + "");
                        }
                        dialog.dismiss();
                    }
                });
        builder.setPositiveButton(null, null);
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {

    }

    private int getValueIndex(String value) {
        int len = getEntryValues().length;
        for (int i = 0; i < len; i++) {
            if (value.equals(getEntryValues()[i])) {
                return i;
            }
        }
        return customKeyIndex;
    }

    public void setCustomValue() {
        setValue(getEntryValues()[customKeyIndex] + "");
    }
}