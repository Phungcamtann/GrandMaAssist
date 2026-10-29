package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value object quản lý cấu hình hỗ trợ tiếp cận (Accessibility) theo UC28.
 */
public final class AccessibilitySettings implements Serializable {
    public static final float RATE_SLOW = 0.75f;
    public static final float RATE_RECOMMENDED = 0.85f; // Tốc độ tối ưu cho người cao tuổi
    public static final float RATE_NORMAL = 1.0f;

    public final boolean voiceEnabled;
    public final boolean autoReadEnabled;
    public final float speechRate;
    public final String fontScaleOption;

    public AccessibilitySettings() {
        this(true, false, RATE_RECOMMENDED, "DEFAULT");
    }

    public AccessibilitySettings(boolean voiceEnabled, boolean autoReadEnabled, float speechRate, String fontScaleOption) {
        this.voiceEnabled = voiceEnabled;
        this.autoReadEnabled = autoReadEnabled;
        this.speechRate = speechRate > 0 ? speechRate : RATE_RECOMMENDED;
        this.fontScaleOption = fontScaleOption != null ? fontScaleOption : "DEFAULT";
    }

    public AccessibilitySettings withVoiceEnabled(boolean voiceEnabled) {
        return new AccessibilitySettings(voiceEnabled, this.autoReadEnabled, this.speechRate, this.fontScaleOption);
    }

    public AccessibilitySettings withAutoReadEnabled(boolean autoReadEnabled) {
        return new AccessibilitySettings(this.voiceEnabled, autoReadEnabled, this.speechRate, this.fontScaleOption);
    }

    public AccessibilitySettings withSpeechRate(float speechRate) {
        return new AccessibilitySettings(this.voiceEnabled, this.autoReadEnabled, speechRate, this.fontScaleOption);
    }

    public AccessibilitySettings withFontScaleOption(String fontScaleOption) {
        return new AccessibilitySettings(this.voiceEnabled, this.autoReadEnabled, this.speechRate, fontScaleOption);
    }

    public float getFontScale() {
        if ("LARGE".equals(fontScaleOption)) return 1.15f;
        if ("EXTRA_LARGE".equals(fontScaleOption)) return 1.30f;
        return 1.0f;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccessibilitySettings that = (AccessibilitySettings) o;
        return voiceEnabled == that.voiceEnabled &&
                autoReadEnabled == that.autoReadEnabled &&
                Float.compare(that.speechRate, speechRate) == 0 &&
                Objects.equals(fontScaleOption, that.fontScaleOption);
    }

    @Override
    public int hashCode() {
        return Objects.hash(voiceEnabled, autoReadEnabled, speechRate, fontScaleOption);
    }
}
