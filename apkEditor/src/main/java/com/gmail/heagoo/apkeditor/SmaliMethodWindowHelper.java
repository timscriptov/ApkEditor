package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.PopupWindow;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.common.Display;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Popup window helper
public class SmaliMethodWindowHelper {
    private WeakReference<ISmaliMethodClicked> callbackRef;

    private PopupWindow popupWindow;
    private String methodComputedFrom; // Record the method is from which file

    public SmaliMethodWindowHelper(ISmaliMethodClicked callback) {
        callbackRef = new WeakReference<>(callback);
    }

    public String getFile() {
        return methodComputedFrom;
    }

    private void createPopWindow(Activity activity, String smaliFile,
                                 final List<SmaliMethodInfo> methodList) {
        this.methodComputedFrom = smaliFile;

        LayoutInflater layoutInflater = (LayoutInflater)
                activity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = layoutInflater.inflate(R.layout.popup_list, null);
        ListView methodLv = (ListView) view.findViewById(R.id.lvGroup);

        SmaliMethodAdapter adapter = new SmaliMethodAdapter(
                activity.getApplicationContext(), methodList);
        methodLv.setAdapter(adapter);

        // Create a popup window
        int height = Display.getHeight(activity) * 2 / 5;
        int width = Display.getWidth(activity);
        popupWindow = new PopupWindow(view, width, height);

        methodLv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view,
                                    int position, long id) {
                if (position < methodList.size()) {
                    SmaliMethodInfo info = methodList.get(position);
                    if (callbackRef.get() != null) {
                        callbackRef.get().gotoLine(info.lineIndex + 1);
                    }
                }
                if (popupWindow != null) {
                    popupWindow.dismiss();
                }
            }
        });
    }

    public void doPopWindowShow(View parent) {
        if (popupWindow != null) {
            // Focus and allow disappear touch outside
            popupWindow.setFocusable(true);
            popupWindow.setOutsideTouchable(true);

            // To make it can disappear when press return button
            popupWindow.setBackgroundDrawable(new BitmapDrawable());

            // Show it
            popupWindow.showAsDropDown(parent, 0, 0);
        }
    }

    public void asyncShowPopup(Activity activity, String filePath, String text, View parent) {
        new MethodAsyncLoader(activity, filePath, text, parent).execute();
    }

    public interface ISmaliMethodClicked {
        // lineNO starts at 1
        void gotoLine(int lineNO);
    }

    // Use to async load all the methods in smali file
    private class MethodAsyncLoader extends AsyncTask<Void, Integer, Boolean> {
        private final int FILE_TYPE_SMALI = 0;
        private final int FILE_TYPE_JAVA = 1;
        private WeakReference<Activity> activityRef;
        private String smaliFile;
        private String content;
        private View parentView;
        private List<SmaliMethodInfo> methodList;
        private int fileType = -1;

        MethodAsyncLoader(Activity activity, String filePath, String fileContent, View parent) {
            activityRef = new WeakReference<>(activity);
            this.smaliFile = filePath;
            this.content = fileContent;
            this.parentView = parent;
            this.methodList = new ArrayList<>();

            if (filePath.endsWith(".smali")) {
                fileType = FILE_TYPE_SMALI;
            } else if (filePath.endsWith(".java")) {
                fileType = FILE_TYPE_JAVA;
            }
        }

        @Override
        public Boolean doInBackground(Void... params) {
            BufferedReader br = new BufferedReader(new StringReader(content));
            String line;
            try {
                int lineIndex = 0;
                switch (fileType) {
                    case FILE_TYPE_SMALI:
                        while ((line = br.readLine()) != null) {
                            parseSmaliMethod(lineIndex, line);
                            lineIndex += 1;
                        }
                        break;
                    case FILE_TYPE_JAVA:
                        while ((line = br.readLine()) != null) {
                            parseJavaMethod(lineIndex, line);
                            lineIndex += 1;
                        }
                        break;
                }
            } catch (IOException ignored) {
            }
            return null;
        }

        private void parseJavaMethod(int lineIndex, String line) {
            line = line.trim();
            // For 'public' 'private' 'protected'
            if (line.length() > 6 && line.charAt(0) == 'p' && (line.charAt(1) == 'u' || line.charAt(1) == 'r')) {
                Matcher matcher = Pattern.compile("(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *(\\{?|[^;])").matcher(line);
                //Matcher matcher = Pattern.compile("(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *").matcher(line);
                if (matcher.matches()) {
                    String prototype = matcher.group(0);
                    if (prototype.endsWith("{")) {
                        prototype = prototype.substring(0, prototype.length() - 1);
                        prototype = prototype.trim();
                    }
                    methodList.add(new SmaliMethodInfo(lineIndex, prototype));
                }
            }
        }

        private void parseSmaliMethod(int lineIndex, String line) {
            if (line.startsWith(".method ")) {
                String prototype = line.substring(8);
                methodList.add(new SmaliMethodInfo(lineIndex, prototype));
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            createPopWindow(activityRef.get(), smaliFile, methodList);
            doPopWindowShow(parentView);
        }
    }
}
