package name.hagb.getbooxupxkeys;

import static com.onyx.android.onyxotaservice.RsaUtil.nativeGetIvParameter;
import static com.onyx.android.onyxotaservice.RsaUtil.nativeGetSecretKey;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {
    Pattern library_pattern = Pattern.compile("dlopen.failed: library \"([^\"]+)\" not found");
    String lib_path = new File("/system/lib64/").exists() ? "/system/lib64/" : "/system/lib/";
    TextView keysView;

    //  https://stackoverflow.com/a/38569617
    public String getProp(String key) {
        String value = null;

        try {
            value = (String) Class.forName("android.os.SystemProperties")
                    .getMethod("get", String.class).invoke(null, key);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return value;
    }

    public void copy_button_click(View view) {
        ClipboardManager clickboatd = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        clickboatd.setPrimaryClip(ClipData.newPlainText("text", keysView.getText()));
    }


    @SuppressLint({"SetTextI18n", "UnsafeDynamicallyLoadedCode"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        keysView = findViewById(R.id.KeysView);
        System.out.println(getApplicationInfo().nativeLibraryDir);
        if (!forceLoad(new File("/system/priv-app/OnyxOtaService/lib/arm64/libota_jni.so")))
            return;
        keysView.setText("\"MODEL\" = " + getProp("ro.product.model") + "\",\n" +
                "\"STRING_SETTINGS\" = \"" + nativeGetSecretKey() + "\",\n" +
                "\"STRING_UPGRADE\" = \"" + nativeGetIvParameter() + "\"\n" +
                "# fingerprint: " + getProp("ro.vendor.build.fingerprint"));
    }

    boolean forceLoad(File lib) {
        if (!copyFile(lib,
                new File(
                        getApplicationContext().getCacheDir().getAbsolutePath() + "/" + lib.getName()
                )
        )) return false;
        while (true) {
            try {
                System.load(getApplicationContext().getCacheDir().getAbsolutePath() + "/" + lib.getName());
                break;
            } catch (UnsatisfiedLinkError e) {
                Matcher m = library_pattern.matcher(e.getMessage());
                if (!(m.find() && forceLoad(new File(lib_path + m.group(1))))) {
                    return false;
                }
            }
        }
        return true;
    }

    boolean copyFile(File src, File dst_path) {
        FileInputStream fosfrom = null;
        FileOutputStream fosto = null;
        boolean fault = false;
        try {
            fosfrom = new FileInputStream(src);
            fosto = new FileOutputStream(dst_path);
            byte[] buffer = new byte[1024];
            int byteRead;
            while (-1 != (byteRead = fosfrom.read(buffer))) {
                fosto.write(buffer, 0, byteRead);
            }
        } catch (IOException e) {
            keysView.setTextColor(Color.RED);
            keysView.setText(e.getMessage());
            fault = true;
        } finally {
            if (fosfrom != null) {
                try {
                    fosfrom.close();
                } catch (IOException ignored) {
                }
            }
            if (fosto != null) {
                try {
                    fosto.close();
                } catch (IOException ignored) {
                }
            }
        }
        return !fault;
    }
}