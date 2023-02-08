package brut.util;

import android.util.Pair;

import java.util.ArrayList;

public class NamespaceManager {

    private static final int defaultDepthSize = 4;

    // Each object is a hashmap
    private Object[] namespaces = new Object[defaultDepthSize];
    // How many depth contain namespaces
    private int depthNum = 0;
    // Depth value last saved
    private int lastSavedDepth;

    // Get the name space from an array recrd
    private String getName(ArrayList<Pair<String, String>> l, String uri) {
        for (Pair<String, String> p : l) {
            if (p.second.equals(uri)) {
                return p.first;
            }
        }
        return null;
    }

    public String getNsName(int depth, String uri) {
        String name = null;
        // Special common case: only one depth contain namespace
        if (depthNum == 1) {
            ArrayList<Pair<String, String>> t = (ArrayList<Pair<String, String>>) namespaces[lastSavedDepth];
            if (t != null) {
                // For one element, just return the ONLY namespace, correct???
                if (t.size() == 1) {
                    Pair<String, String> ns = t.get(0);
                    int len1 = ns.second.length();
                    int len2 = uri.length();
                    // uri almost match
                    if (len1 == len2 && ns.second.charAt(len1 - 1) == uri.charAt(len2 - 1)) {
                        name = ns.first;
                    }
                } else {
                    name = getName(t, uri);
                }
            }
        } else {
            int startIndex = (depth >= namespaces.length ? (namespaces.length - 1)
                    : depth);
            for (int i = startIndex; i >= 0; --i) {
                if (namespaces[i] != null) {
                    ArrayList<Pair<String, String>> t = (ArrayList<Pair<String, String>>) namespaces[i];
                    name = getName(t, uri);
                    if (name != null) {
                        break;
                    }
                }
            }
        }
        return name;
    }

    public void putNamespace(int depth, String namespace, String uri) {
        this.lastSavedDepth = depth;

        // Enlarge the array
        if (depth >= namespaces.length) {
            Object[] newArray = new Object[depth + 1];
            for (int i = 0; i < namespaces.length; i++) {
                newArray[i] = namespaces[i];
            }
            namespaces = newArray;
        }

        // Put namespace to related depth name space record
        if (namespaces[depth] == null) {
            ArrayList<Pair<String, String>> t = new ArrayList<Pair<String, String>>();
            t.add(Pair.create(namespace, uri));
            namespaces[depth] = t;
            depthNum += 1;
        } else {
            ArrayList<Pair<String, String>> t = (ArrayList<Pair<String, String>>) namespaces[depth];
            t.add(Pair.create(namespace, uri));
        }
    }

    // Exit a depth
    // Clear the namespace for that depth
    public void depthEnded(int depth) {
        if (namespaces.length > depth && namespaces[depth] != null) {
            namespaces[depth] = null;
        }
    }

}
