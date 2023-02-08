package com.gmail.heagoo.pngeditor;

//import android.support.v7.app.AppCompatActivity;


public class HelpActivity extends WebViewActivity {

    public HelpActivity() {
        super(R.string.help,
                "file:///android_res/raw/pngeditor_help.htm",
                false);
    }
}
