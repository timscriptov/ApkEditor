package com.gmail.heagoo.apkeditor;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import com.gmail.heagoo.apkeditor.base.R;

import java.lang.ref.WeakReference;

public class ProcessingDialog extends Dialog implements
        android.view.View.OnClickListener {

    private WeakReference<Activity> activityRef;
    private ProcessingInterface processor;
    private int successTipResId;

    @SuppressLint("InflateParams")
    public ProcessingDialog(Activity activity, ProcessingInterface processor,
                            int okTipResId) {
        super(activity, R.style.Dialog_No_Border_2);
        this.activityRef = new WeakReference<>(activity);
        this.processor = processor;
        this.successTipResId = okTipResId;

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LayoutInflater inflater = LayoutInflater.from(activity);
        View layout = inflater.inflate(R.layout.dlg_processing, null);
        super.setContentView(layout);
        super.setCancelable(false);

        // Start processing thread
        ProcessingThread thread = new ProcessingThread(this);
        thread.start();
    }

    // When errMsg == null, means revert succeed
    protected void processCompleted(final String errMsg) {
        Activity activity = activityRef.get();
        if (activity != null) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // Call back to the task which is mainly for UI change
                    processor.afterProcess();

                    if (errMsg != null) {
                        showTip("Failed: " + errMsg);
                    } else {
                        showTip(successTipResId);
                    }

                    if (ProcessingDialog.this.isShowing()) {
                        dismissWithoutThrow();
                    }
                }
            });
        }
    }

    // Don't know why occurred, but it appears on google play
    private void dismissWithoutThrow() {
        try {
            this.dismiss();
        } catch (Exception ignored) {
        }
    }

    protected void showTip(String msg) {
        Activity activity = activityRef.get();
        if (activity != null) {
            Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
        }
    }

    protected void showTip(int resId) {
        if (resId != -1) {
            Activity activity = activityRef.get();
            if (activity != null) {
                Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.close_button) {
            dismissWithoutThrow();
        }
    }

    public static interface ProcessingInterface {
        public void process() throws Exception;

        public void afterProcess();
    }

    static class ProcessingThread extends Thread {
        private WeakReference<ProcessingDialog> dlgRef;

        public ProcessingThread(ProcessingDialog dlg) {
            this.dlgRef = new WeakReference<>(dlg);
        }

        @Override
        public void run() {
            String errMsg = null;

            ProcessingDialog dlg = dlgRef.get();
            if (dlg != null) {
                try {
                    dlg.processor.process();
                } catch (Exception e) {
                    errMsg = e.getMessage();
                }
                dlg.processCompleted(errMsg);
            }
        }

    }
}
