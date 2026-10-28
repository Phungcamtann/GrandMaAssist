package vn.edu.ueh.nhom4.grandmaassist.app.data.local;

import android.content.Context;
import android.content.SharedPreferences;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.AccessibilitySettings;

/**
 * Lưu trữ cục bộ cho cấu hình Accessibility qua SharedPreferences.
 */
public final class AccessibilityStore {
    private static final String PREF_NAME = "grandma_accessibility_prefs";
    private static final String KEY_VOICE_ENABLED = "voice_enabled";
    private static final String KEY_AUTO_READ_ENABLED = "auto_read_enabled";
    private static final String KEY_SPEECH_RATE = "speech_rate";
    private static final String KEY_FONT_SCALE_OPTION = "font_scale_option";

    private final SharedPreferences prefs;

    public AccessibilityStore(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public AccessibilitySettings getSettings() {
        boolean voice = prefs.getBoolean(KEY_VOICE_ENABLED, true);
        boolean autoRead = prefs.getBoolean(KEY_AUTO_READ_ENABLED, false);
        float rate = prefs.getFloat(KEY_SPEECH_RATE, AccessibilitySettings.RATE_RECOMMENDED);
        String fontScale = prefs.getString(KEY_FONT_SCALE_OPTION, "DEFAULT");
        return new AccessibilitySettings(voice, autoRead, rate, fontScale);
    }

    public void saveSettings(AccessibilitySettings settings) {
        if (settings == null) return;
        prefs.edit()
                .putBoolean(KEY_VOICE_ENABLED, settings.voiceEnabled)
                .putBoolean(KEY_AUTO_READ_ENABLED, settings.autoReadEnabled)
                .putFloat(KEY_SPEECH_RATE, settings.speechRate)
                .putString(KEY_FONT_SCALE_OPTION, settings.fontScaleOption)
                .apply();
    }
}
