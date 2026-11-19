package com.tsgp.oee;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebChromeClient;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * แอป OEE: เปิดเว็บ https://oee-web.sasatornua.workers.dev/ แบบเต็มจอ
 * เพิ่มเติมจาก Capacitor ปกติ (ไม่ได้แก้เว็บ):
 *  - ปุ่มแนบรูป -> เลือกได้ทั้ง "ถ่ายรูปด้วยกล้อง" และ "เลือกจากคลังรูป"
 *  - การดาวน์โหลดไฟล์ อยู่ใน DownloadPlugin.java
 */
public class MainActivity extends BridgeActivity {

    // ---------- 2) ปุ่มแนบรูป: กล้อง + คลังรูป ----------
    private ValueCallback<Uri[]> pendingFileCallback;
    private WebChromeClient.FileChooserParams pendingParams;
    private Uri cameraOutputUri;

    private final ActivityResultLauncher<String> cameraPermissionLauncher =
        registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> launchChooser(granted));

    private final ActivityResultLauncher<Intent> chooserLauncher =
        registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            ValueCallback<Uri[]> cb = pendingFileCallback;
            pendingFileCallback = null;
            if (cb == null) return;
            Uri[] uris = null;
            Intent data = result.getData();
            if (result.getResultCode() == Activity.RESULT_OK) {
                if (data != null && data.getClipData() != null) {
                    int n = data.getClipData().getItemCount();
                    uris = new Uri[n];
                    for (int k = 0; k < n; k++) uris[k] = data.getClipData().getItemAt(k).getUri();
                } else if (data != null && data.getData() != null) {
                    uris = new Uri[] { data.getData() };
                } else if (cameraOutputUri != null) {
                    uris = new Uri[] { cameraOutputUri };
                }
            }
            cameraOutputUri = null;
            cb.onReceiveValue(uris); // ต้องเรียกเสมอ (null = ยกเลิก) ไม่งั้นปุ่มแนบรูปจะกดไม่ได้อีก
        });

    private class OeeChromeClient extends BridgeWebChromeClient {
        OeeChromeClient() { super(MainActivity.this.getBridge()); }

        @Override
        public boolean onShowFileChooser(WebView webView, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            if (pendingFileCallback != null) pendingFileCallback.onReceiveValue(null);
            pendingFileCallback = filePathCallback;
            pendingParams = fileChooserParams;
            if (wantsImages(fileChooserParams)
                && ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                launchChooser(true);
            }
            return true;
        }
    }

    private static boolean wantsImages(WebChromeClient.FileChooserParams p) {
        if (p == null || p.getAcceptTypes() == null) return true;
        for (String t : p.getAcceptTypes()) {
            if (t == null || t.isEmpty() || t.startsWith("image")) return true;
        }
        return false;
    }

    private void launchChooser(boolean cameraAllowed) {
        if (pendingFileCallback == null) return;
        try {
            WebChromeClient.FileChooserParams p = pendingParams;
            String type = "*/*";
            if (p != null && p.getAcceptTypes() != null && p.getAcceptTypes().length > 0
                && p.getAcceptTypes()[0] != null && !p.getAcceptTypes()[0].isEmpty()) {
                type = p.getAcceptTypes()[0];
            }
            Intent pick = new Intent(Intent.ACTION_GET_CONTENT);
            pick.addCategory(Intent.CATEGORY_OPENABLE);
            pick.setType(type);
            if (p != null && p.getMode() == WebChromeClient.FileChooserParams.MODE_OPEN_MULTIPLE) {
                pick.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            }

            List<Intent> extras = new ArrayList<>();
            cameraOutputUri = null;
            if (cameraAllowed && wantsImages(p)) {
                File photo = new File(getCacheDir(), "cam_" + System.currentTimeMillis() + ".jpg");
                cameraOutputUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", photo);
                Intent cam = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                cam.putExtra(MediaStore.EXTRA_OUTPUT, cameraOutputUri);
                cam.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                extras.add(cam);
            }

            Intent chooser = Intent.createChooser(pick, "แนบรูป");
            if (!extras.isEmpty()) chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, extras.toArray(new Intent[0]));
            chooserLauncher.launch(chooser);
        } catch (Exception e) {
            ValueCallback<Uri[]> cb = pendingFileCallback;
            pendingFileCallback = null;
            if (cb != null) cb.onReceiveValue(null);
            Toast.makeText(this, "เปิดตัวเลือกไฟล์ไม่ได้", Toast.LENGTH_LONG).show();
        }
    }

    // ---------- เริ่มต้น ----------
    @Override
    public void onCreate(Bundle savedInstanceState) {
        // ต้องลงทะเบียนก่อน super.onCreate เพื่อให้ตัวรับดาวน์โหลดพร้อมก่อนหน้าเว็บโหลด
        registerPlugin(DownloadPlugin.class);
        super.onCreate(savedInstanceState);
        getBridge().getWebView().setWebChromeClient(new OeeChromeClient());
    }
}
