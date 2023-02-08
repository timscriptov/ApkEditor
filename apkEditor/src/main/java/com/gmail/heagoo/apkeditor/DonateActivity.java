package com.gmail.heagoo.apkeditor;

import android.app.Activity;
import android.content.pm.ApplicationInfo;
import android.os.Bundle;
import android.webkit.WebView;
import android.widget.ImageView;

import com.gmail.heagoo.apkeditor.base.R;

/**
 * Created by phe3 on 2/9/2018.
 */

public class DonateActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_donate);

        initIcon();
        initWeb();
    }

    private void initIcon() {
        ImageView iv = (ImageView) findViewById(R.id.image_launcher);
        ApplicationInfo ai = getApplicationInfo();
        iv.setImageDrawable(ai.loadIcon(getPackageManager()));
    }

    private void initWeb() {
        WebView v = (WebView) this.findViewById(R.id.donateWeb);
        String account = "ifbhpp";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < account.length(); ++i) {
            char decode = (char) (account.charAt(i) - 1);
            sb.append(decode);
        }
        account = sb.toString();
        String content = "<html>\n" +
                "<body>\n" +
                "\n" +
                "<center><h1>Support the author</h1></center>\n" +
                "<p>\n" +
                "Could you please support a small app developer by paying a little money?<br>\n" +
                "</p>\n" +
                "<p>\n" +
                "With your money support, the author will get motivated to continuously improve APK Editor (Pro).<br>\n" +
                "</p>\n" +
                "<p>\n" +
                "Thanks for your help!<br>\n" +
                "</p>\n" +
                "\n" +
                "\n" +
                "<center>\n" +
                "<a href=\"https://www.paypal.me/" + account + "/1.99\"><button style=\"width:80%\">Basic Support: Donate $1.99</button></a><br><br>\n" +
                "<a href=\"https://www.paypal.me/" + account + "/4.99\"><button style=\"width:80%\">Silver Support: Donate $4.99</button></a><br><br>\n" +
                "<a href=\"https://www.paypal.me/" + account + "/9.99\"><button style=\"width:80%\">Gold Support: Donate $9.99</button></a><br><br>\n" +
                "<a href=\"https://www.paypal.me/" + account + "/\"><button style=\"width:80%\">I'd like to input the money myself</button></a><br><br>\n" +
                "</center>\n" +
                "\n" +
                "</body>\n" +
                "</html>";
        v.loadData(content, "text/html", "UTF-8");
    }
}
