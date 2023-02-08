package com.gmail.heagoo.apkeditor.ce;

import android.content.Context;

import com.gmail.heagoo.apkeditor.base.R;
import com.gmail.heagoo.apkeditor.smali.AsyncDecodeTask;
import com.gmail.heagoo.common.CommandRunner;

import java.io.File;
import java.io.Serializable;
import java.util.Map;

public class DexDecode implements IApkMaking, Serializable {

    private static final long serialVersionUID = -2847899181541354279L;

    @Override
    public void prepareReplaces(Context ctx, String apkFilePath,
                                Map<String, String> allReplaces, IDescriptionUpdate updater)
            throws Exception {
        if (updater != null) {
            String strDecode = ctx.getString(R.string.decode_dex_file);
            updater.updateDescription(strDecode);
        }

        File fileDir = ctx.getFilesDir();
        String rootDirectory = fileDir.getAbsolutePath();
        String decodeRootPath = rootDirectory + "/decoded";

        // Remove the old directory
        CommandRunner cr = new CommandRunner();
        cr.runCommand("rm -rf " + decodeRootPath + "/smali", null, 10000);

        // Borrow the code from AsyncDecodeTask to do the decoding
        AsyncDecodeTask decoder = new AsyncDecodeTask(ctx, apkFilePath,
                decodeRootPath, null);
        //decoder.doAllJobs();
        // As we only modify the classes.dex
        decoder.decodeMainDex();

        if (updater != null) {
            updater.updateDescription("");
        }
    }

//	@Override
//	public String getDescription() {
//		return "Decode DEX File";
//	}

}
