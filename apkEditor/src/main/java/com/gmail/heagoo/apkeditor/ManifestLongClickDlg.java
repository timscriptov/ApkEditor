package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.view.View;
import android.view.Window;
import android.widget.TextView;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.FileSelectDialog.IFileSelection;
import com.gmail.heagoo.apkeditor.base.BuildConfig;
import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.folderlist.util.OpenFiles;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

class ManifestDesc {
    private static Map<String, String> tag2Desc = new HashMap<>();

    static {
        tag2Desc.put("action", "Adds an action to an intent filter.");
        tag2Desc.put(
                "activity",
                "Declares an activity that implements part of the application's visual user interface.");
        tag2Desc.put("activity-alias", "An alias for an activity.");
        tag2Desc.put(
                "application",
                "The declaration of the application. This element contains subelements that declare each of the application's components and has attributes that can affect all the components.");
        tag2Desc.put("category", "Adds a category name to an intent filter.");
        tag2Desc.put("compatible-screens",
                "Specifies each screen configuration with which the application is compatible.");
        tag2Desc.put("data", "Adds a data specification to an intent filter.");
        tag2Desc.put(
                "grant-uri-permission",
                "Specifies which data subsets of the parent content provider permission can be granted for.");
        tag2Desc.put(
                "instrumentation",
                "Declares an Instrumentation class that enables you to monitor an application's interaction with the system.");
        tag2Desc.put(
                "intent-filter",
                "Specifies the types of intents that an activity, service, or broadcast receiver can respond to. An intent filter declares the capabilities of its parent component -- what an activity or service can do and what types of broadcasts a receiver can handle.");
        tag2Desc.put("manifest",
                "The root element of the AndroidManifest.xml file.");
        tag2Desc.put(
                "meta-data",
                "A name-value pair for an item of additional, arbitrary data that can be supplied to the parent component.");
        tag2Desc.put(
                "path-permission",
                "Defines the path and required permissions for a specific subset of data within a content provider.");
        tag2Desc.put(
                "permission",
                "Declares a security permission that can be used to limit access to specific components or features of this or other applications.");
        tag2Desc.put("permission-group",
                "Declares a name for a logical grouping of related permissions.");
        tag2Desc.put("permission-tree",
                "Declares the base name for a tree of permissions.");
        tag2Desc.put(
                "provider",
                "Declares a content provider component. A content provider supplies structured access to data managed by the application.");
        tag2Desc.put(
                "receiver",
                "Declares a broadcast receiver as one of the application's components. Broadcast receivers enable applications to receive intents that are broadcast by the system or by other applications, even when other components of the application are not running.");
        tag2Desc.put(
                "service",
                "Declares a service as one of the application's components. Unlike activities, services lack a visual user interface. They're used to implement long-running background operations");
        tag2Desc.put(
                "supports-gl-texture",
                "Declares a single GL texture compression format that is supported by the application.");
        tag2Desc.put(
                "supports-screens",
                "Lets you specify the screen sizes your application supports and enable screen compatibility mode for screens larger than what your application supports.");
        tag2Desc.put("uses-configuration",
                "Indicates what hardware and software features the application requires.");
        tag2Desc.put(
                "uses-feature",
                "Declares a single hardware or software feature that is used by the application.");
        tag2Desc.put("uses-library",
                "Specifies a shared library that the application must be linked against.");
        tag2Desc.put(
                "uses-permission",
                "Requests a permission that the application must be granted in order for it to operate correctly.");
        tag2Desc.put(
                "uses-sdk",
                "Lets you express an application's compatibility with one or more versions of the Android platform, by means of an API Level integer.");
    }

    static String getDescription(String tag) {
        return tag2Desc.get(tag);
    }
}

class ManifestLongClickDlg extends Dialog {
    private WeakReference<Activity> activityRef;
    private String xmlPath;
    private LineRecord lineRec;
    private IManifestChangeCallback deleteCallbacker;

    ManifestLongClickDlg(Activity activity, String xmlPath,
                         LineRecord lineRec, IManifestChangeCallback callback) {
        super(activity);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        this.activityRef = new WeakReference<>(activity);
        this.xmlPath = xmlPath;
        this.lineRec = lineRec;
        this.deleteCallbacker = callback;

        init();

        // this.getWindow().setBackgroundDrawableResource(
        // android.R.color.transparent);
    }

    @SuppressLint("InflateParams")
    private void init() {
        //Resources res = activityRef.get().getResources();

        View view = activityRef.get().getLayoutInflater().inflate(
                R.layout.dlg_manifestline, null, false);
        TextView contentTv = (TextView) view.findViewById(R.id.content);
        contentTv.setText(lineRec.lineData);
        TextView descTv = (TextView) view.findViewById(R.id.description);
        String desc = getDescription();
        descTv.setText(desc != null ? desc : "");

        // Delete button
        TextView deleteTv = (TextView) view.findViewById(R.id.delete);
        deleteTv.setClickable(true);
        deleteTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String errMsg = deleteCallbacker.tryToDeleteSection(lineRec);
                if (errMsg == null) {
                    close();
                } else {
                    // Prompt the reason of the fail
                    if (!errMsg.equals("")) {
                        Toast.makeText(activityRef.get(), errMsg, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        });

        // extract button
        TextView extractTv = (TextView) view.findViewById(R.id.extract);
        extractTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Select a target folder to extract
                String dlgTitle = activityRef.get().getString(R.string.select_folder);
                IFileSelection callback = new IFileSelection() {
                    @Override
                    // filePath is the target directory
                    // extraStr is the source file/directory
                    public void fileSelectedInDialog(
                            String filePath, String extraStr, boolean openFile) {
                        String targetFolder = filePath;
                        FileCopyDialog dlg = new FileCopyDialog(
                                activityRef.get(),
                                xmlPath, targetFolder, null, null, null, 0);
                        dlg.show();
                    }

                    @Override
                    public boolean isInterestedFile(String filename, String extraStr) {
                        return true;
                    }

                    @Override
                    public String getConfirmMessage(String filePath, String extraStr) {
                        return null;
                    }
                };

                FileSelectDialog dlg = new FileSelectDialog(
                        activityRef.get(), callback, null, null,
                        dlgTitle, true, false, false, null);
                dlg.show();

                close();
            }
        });

        // Replace button
        TextView replaceTv = (TextView) view.findViewById(R.id.replace);
        replaceTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isProVersion()) {
                    ManifestListAdapter.showPromoteDialog(activityRef.get());
                } else {
                    FileSelectDialog dlg = new FileSelectDialog(
                            activityRef.get(),
                            new IFileSelection() {
                                @Override
                                public void fileSelectedInDialog(
                                        String filePath, String extraStr, boolean openFile) {
                                    if (openFile) {
                                        ((ApkInfoActivity) activityRef.get()).saveParams(
                                                filePath, extraStr, null);
                                        OpenFiles.openFile(activityRef.get(), filePath,
                                                ApkInfoActivity.RC_OPEN_BEFORE_REPLACE);
                                    } else {
                                        ((ApkInfoActivity) activityRef.get()).replaceFile(extraStr, filePath);
                                        ((ApkInfoActivity) activityRef.get()).setManifestModified(true);
                                    }
                                }

                                @Override
                                public boolean isInterestedFile(
                                        String filename, String extraStr) {
                                    return filename.endsWith(".xml");
                                }

                                @Override
                                public String getConfirmMessage(String filePath, String extraStr) {
                                    return null;
                                }
                            }, ".xml", xmlPath, null, false, false, true, null);
                    dlg.show();
                    close();
                }
            }
        });

        // Open in new window
        TextView openTv = (TextView) view.findViewById(R.id.open_in_new_window);
        openTv.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if (!isProVersion()) {
                    ManifestListAdapter.showPromoteDialog(activityRef.get());
                    return;
                }

                Intent intent = TextEditor.getEditorIntent(
                        activityRef.get().getApplicationContext(), xmlPath, null);
                activityRef.get().startActivityForResult(intent, 2);
                close();
            }
        });

        // Close button
        TextView closeTv = (TextView) view.findViewById(R.id.close);
        closeTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                close();
            }
        });

        setContentView(view);
    }

    private boolean isProVersion() {
        return BuildConfig.IS_PRO;
    }

    private void close() {
        this.dismiss();
    }

    // Tag like activity, manifest, uses-permission, etc

    // Get the string specified by android:name
    private String getSearchKeywords() {
        String name = lineRec.getName();
        if (name == null || name.startsWith(".")) {
            name = "AndroidManifest " + lineRec.getSectionTag();
        }

        return name;
    }

    // Get description of section tag
    private String getDescription() {
        String tag = lineRec.getSectionTag();
        if (tag != null) {
            return ManifestDesc.getDescription(tag);
        } else {
            return null;
        }
    }
}
