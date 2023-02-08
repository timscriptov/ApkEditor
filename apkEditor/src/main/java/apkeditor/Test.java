package apkeditor;

import android.content.ContentResolver;
import android.content.Context;

/**
 * Created by phe3 on 12/10/2016.
 */

public class Test {

    public String test(Context ctx) {
        ContentResolver r = ctx.getContentResolver();
        String id = android.provider.Settings.Secure.getString(r, "android_id");
        return Utils.stringAdd1(id);
    }

    public void testStack() {
        Utils.printCallStack();
    }

}
