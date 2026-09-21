package com.kuroneko.devoptionslock;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Locale;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class MainActivity extends Activity {
    private static final String PREFS = "devlock";
    private static final String HASH = "password_hash";
    private static final String SALT = "password_salt";
    private static final int UNLOCK_SECONDS = 600;
    private static final String MODULE_CTL = "/data/adb/modules/devoptionslock/ctl.sh";

    private EditText password;
    private EditText password2;
    private TextView status;
    private TextView setupHint;
    private android.content.SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManagerLayoutParams.FLAG_SECURE, WindowManagerLayoutParams.FLAG_SECURE);
        setContentView(R.layout.activity_main);

        password = findViewById(R.id.password);
        password2 = findViewById(R.id.password2);
        status = findViewById(R.id.status);
        setupHint = findViewById(R.id.setupHint);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        Button action = findViewById(R.id.action);
        Button lockNow = findViewById(R.id.lockNow);
        action.setOnClickListener(v -> handlePrimary());
        lockNow.setOnClickListener(v -> {
            if (rootCtl("lock")) {
                toast("已锁定");
                updateUi();
            }
        });
        updateUi();
    }

    private void handlePrimary() {
        String p = password.getText().toString();
        if (!prefs.contains(HASH)) {
            String p2 = password2.getText().toString();
            if (!p.matches("[A-Za-z0-9]{8,64}") || !p.equals(p2)) {
                toast("密码必须相同，且为 8～64 位英文或数字");
                return;
            }
            byte[] salt = new byte[16];
            new SecureRandom().nextBytes(salt);
            String saltHex = hex(salt);
            String hash = hash(p, saltHex);
            prefs.edit().putString(SALT, saltHex).putString(HASH, hash).apply();
            if (rootCtl("lock")) {
                toast("密码已设置");
                password.setText("");
                password2.setText("");
                updateUi();
            }
            return;
        }

        String saved = prefs.getString(HASH, "");
        String salt = prefs.getString(SALT, "");
        String actual = hash(p, salt);
        if (!MessageDigest.isEqual(saved.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8))) {
            toast("密码错误");
            password.selectAll();
            return;
        }

        if (!rootCtl("unlock " + UNLOCK_SECONDS)) {
            return;
        }

        password.setText("");
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
            startActivity(intent);
            toast("已解锁，离开开发者选项页面后会自动重新锁定");
        } catch (Exception e) {
            toast("无法打开开发者选项：" + e.getClass().getSimpleName());
            rootCtl("lock");
        }
    }

    private void updateUi() {
        boolean hasPassword = prefs.contains(HASH);
        password2.setVisibility(hasPassword ? EditText.GONE : EditText.VISIBLE);
        setupHint.setText(hasPassword
                ? "输入密码后打开开发者选项。进入页面期间保持解锁；离开页面后自动重新锁定，最长 10 分钟。"
                : "首次使用请设置密码（8～64 位英文或数字）。");
        ((Button) findViewById(R.id.action)).setText(hasPassword ? "输入密码并打开开发者选项" : "设置密码");
        status.setText("状态：由 root 模块持续保护");
    }

    private boolean rootCtl(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", MODULE_CTL + " " + cmd});
            int code = p.waitFor();
            if (code != 0) {
                toast("root 命令失败，请确认 KSU/Magisk/APatch 已授权本 App");
                return false;
            }
            return true;
        } catch (Exception e) {
            toast("无法获得 root：" + e.getClass().getSimpleName());
            return false;
        }
    }

    private static String hash(String password, String saltHex) {
        try {
            byte[] salt = new byte[saltHex.length() / 2];
            for (int i = 0; i < salt.length; i++) {
                salt[i] = (byte) Integer.parseInt(saltHex.substring(i * 2, i * 2 + 2), 16);
            }
            PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, 120_000, 256);
            byte[] out = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();
            spec.clearPassword();
            return hex(out);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder s = new StringBuilder(bytes.length * 2);
        for (byte x : bytes) {
            s.append(String.format(Locale.US, "%02x", x & 0xff));
        }
        return s.toString();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // Avoid importing WindowManager just for one constant in the minimal project.
    private static final class WindowManagerLayoutParams {
        static final int FLAG_SECURE = 0x00002000;
    }
}
