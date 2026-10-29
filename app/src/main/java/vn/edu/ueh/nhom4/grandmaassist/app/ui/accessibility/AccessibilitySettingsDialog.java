package vn.edu.ueh.nhom4.grandmaassist.app.ui.accessibility;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.accessibility.TtsManager;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.AccessibilityStore;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.AccessibilitySettings;

/**
 * Hộp thoại tùy chỉnh hỗ trợ tiếp cận và giọng đọc (UC28 & S23) cho người cao tuổi.
 */
public final class AccessibilitySettingsDialog {

    public interface OnSettingsSavedListener {
        void onSaved(AccessibilitySettings newSettings);
    }

    public static void show(Context context, OnSettingsSavedListener listener) {
        AccessibilityStore store = new AccessibilityStore(context);
        AccessibilitySettings current = store.getSettings();
        TtsManager tts = TtsManager.getInstance(context);

        View v = LayoutInflater.from(context).inflate(R.layout.dialog_accessibility_settings, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(v)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        SwitchMaterial switchVoice = v.findViewById(R.id.switchVoiceEnabled);
        SwitchMaterial switchAutoRead = v.findViewById(R.id.switchAutoReadEnabled);
        RadioGroup rgRate = v.findViewById(R.id.rgSpeechRate);
        RadioButton rbSlow = v.findViewById(R.id.rbRateSlow);
        RadioButton rbRecommended = v.findViewById(R.id.rbRateRecommended);
        RadioButton rbNormal = v.findViewById(R.id.rbRateNormal);
        AppCompatButton btnTest = v.findViewById(R.id.btnTestVoice);

        RadioGroup rgFontScale = v.findViewById(R.id.rgFontScale);
        RadioButton rbFontDefault = v.findViewById(R.id.rbFontDefault);
        RadioButton rbFontLarge = v.findViewById(R.id.rbFontLarge);
        RadioButton rbFontExtraLarge = v.findViewById(R.id.rbFontExtraLarge);
        TextView tvFontPreview = v.findViewById(R.id.tvFontPreview);

        AppCompatButton btnClose = v.findViewById(R.id.btnCloseAccessibility);
        AppCompatButton btnSave = v.findViewById(R.id.btnSaveAccessibility);

        // Bind initial values
        switchVoice.setChecked(current.voiceEnabled);
        switchAutoRead.setChecked(current.autoReadEnabled);

        if (Math.abs(current.speechRate - AccessibilitySettings.RATE_SLOW) < 0.05f) {
            rbSlow.setChecked(true);
        } else if (Math.abs(current.speechRate - AccessibilitySettings.RATE_NORMAL) < 0.05f) {
            rbNormal.setChecked(true);
        } else {
            rbRecommended.setChecked(true);
        }

        if ("LARGE".equals(current.fontScaleOption)) {
            rbFontLarge.setChecked(true);
            tvFontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * 1.15f);
        } else if ("EXTRA_LARGE".equals(current.fontScaleOption)) {
            rbFontExtraLarge.setChecked(true);
            tvFontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * 1.30f);
        } else {
            rbFontDefault.setChecked(true);
            tvFontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
        }

        rgFontScale.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbFontLarge) {
                tvFontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * 1.15f);
            } else if (checkedId == R.id.rbFontExtraLarge) {
                tvFontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f * 1.30f);
            } else {
                tvFontPreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f);
            }
        });

        // Test Voice button
        btnTest.setOnClickListener(view -> {
            float testRate = rbSlow.isChecked() ? AccessibilitySettings.RATE_SLOW :
                    (rbNormal.isChecked() ? AccessibilitySettings.RATE_NORMAL : AccessibilitySettings.RATE_RECOMMENDED);
            
            // Tạm thời áp dụng rate test
            AccessibilitySettings tempSettings = new AccessibilitySettings(true, switchAutoRead.isChecked(), testRate, current.fontScaleOption);
            store.saveSettings(tempSettings);

            tts.speak("GrandMa Assist xin kính chào cô chú. Chúc cô chú một ngày làm việc thật nhiều niềm vui và thuận lợi!", "test_voice", new TtsManager.PlaybackListener() {
                @Override
                public void onStart(String utteranceId) {
                    btnTest.setText("⏹  Đang phát giọng đọc mẫu...");
                }

                @Override
                public void onDone(String utteranceId) {
                    btnTest.setText("🔊  Nghe lại giọng đọc mẫu");
                }

                @Override
                public void onError(String utteranceId, String message) {
                    btnTest.setText("🔊  Nghe thử giọng đọc mẫu");
                    Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                }
            });
        });

        // Save
        btnSave.setOnClickListener(view -> {
            tts.stop();
            boolean voice = switchVoice.isChecked();
            boolean autoRead = switchAutoRead.isChecked();
            float rate = rbSlow.isChecked() ? AccessibilitySettings.RATE_SLOW :
                    (rbNormal.isChecked() ? AccessibilitySettings.RATE_NORMAL : AccessibilitySettings.RATE_RECOMMENDED);

            String selectedScale = "DEFAULT";
            if (rbFontLarge.isChecked()) selectedScale = "LARGE";
            else if (rbFontExtraLarge.isChecked()) selectedScale = "EXTRA_LARGE";

            AccessibilitySettings saved = new AccessibilitySettings(voice, autoRead, rate, selectedScale);
            store.saveSettings(saved);

            Toast.makeText(context, "Đã lưu cài đặt tiếp cận", Toast.LENGTH_SHORT).show();
            if (listener != null) {
                listener.onSaved(saved);
            }
            dialog.dismiss();
        });

        // Close
        btnClose.setOnClickListener(view -> {
            tts.stop();
            dialog.dismiss();
        });

        dialog.setOnDismissListener(d -> tts.stop());
        dialog.show();
    }
}
