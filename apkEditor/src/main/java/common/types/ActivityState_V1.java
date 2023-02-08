package common.types;

import android.os.Bundle;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

// Used to save the state of ApkInfoActivity
public class ActivityState_V1 implements Serializable {
    private Map<String, String> stringValues = new HashMap<>();
    private Map<String, Serializable> objValues = new HashMap<>();
    private Map<String, Integer> intValues = new HashMap<>();
    private Map<String, Boolean> boolValues = new HashMap<>();

    public void putString(String key, String value) {
        stringValues.put(key, value);
    }

    public String getString(String key) {
        return stringValues.get(key);
    }

    public void putInt(String key, int value) {
        intValues.put(key, value);
    }

    public void putBoolean(String key, boolean value) {
        boolValues.put(key, value);
    }

    public void putSerializable(String key, Serializable value) {
        objValues.put(key, value);
    }

    public void toBundle(Bundle savedInstanceState) {
        for (Map.Entry<String, String> it : stringValues.entrySet()) {
            savedInstanceState.putString(it.getKey(), it.getValue());
        }
        for (Map.Entry<String, Integer> it : intValues.entrySet()) {
            savedInstanceState.putInt(it.getKey(), it.getValue());
        }
        for (Map.Entry<String, Boolean> it : boolValues.entrySet()) {
            savedInstanceState.putBoolean(it.getKey(), it.getValue());
        }
        for (Map.Entry<String, Serializable> it : objValues.entrySet()) {
            savedInstanceState.putSerializable(it.getKey(), it.getValue());
        }
    }

//    public void fromBundle(Bundle savedInstanceState) {
//        Set<String> keyset = savedInstanceState.keySet();
//        for (String key : keyset) {
//            savedInstanceState.getSerializable(key);
//        }
//    }
}
