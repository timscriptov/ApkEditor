package com.gmail.heagoo.common;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import java.lang.ref.WeakReference;

public class ProcessingDialog extends Dialog {

    private Activity activity;
    private ProcessingInterface processor;
    private int successTipResId;

    @SuppressLint("InflateParams")
    public ProcessingDialog(Activity activity, ProcessingInterface processor,
                            int okTipResId) {
        super(activity, R.style.Dialog_No_Border_2);
        this.activity = activity;
        this.processor = processor;
        this.successTipResId = okTipResId;

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        LayoutInflater inflater = activity.getLayoutInflater();
        View layout = inflater.inflate(R.layout.dlg_processing, null);
        super.setContentView(layout);
        super.setCancelable(false);

        // Start processing thread
        ProcessingThread thread = new ProcessingThread(this);
        thread.start();
    }

    // When errMsg == null, means revert succeed
    protected void processCompleted(final String errMsg) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Call back to the task which is mainly for UI change
                processor.afterProcess();

                if (errMsg != null) {
                    showTip("Failed: " + errMsg);
                } else if (successTipResId > 0) {
                    showTip(successTipResId);
                }

                dismiss();
            }
        });
    }

    protected void showTip(String msg) {
        Toast.makeText(activity, msg, Toast.LENGTH_SHORT).show();
    }

    protected void showTip(int resId) {
        Toast.makeText(activity, resId, Toast.LENGTH_SHORT).show();
    }

    public static interface ProcessingInterface {
        public void process() throws Exception;

        public void afterProcess();
    }

    static class ProcessingThread extends Thread {
        private WeakReference<ProcessingDialog> dlgRef;

        public ProcessingThread(ProcessingDialog dlg) {
            this.dlgRef = new WeakReference<ProcessingDialog>(dlg);
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
