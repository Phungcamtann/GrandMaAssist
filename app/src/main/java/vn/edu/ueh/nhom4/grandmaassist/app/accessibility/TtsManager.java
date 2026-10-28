package vn.edu.ueh.nhom4.grandmaassist.app.accessibility;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.accessibility.AccessibilityManager;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleEventObserver;
import androidx.lifecycle.LifecycleOwner;
import java.util.Locale;
import java.util.regex.Pattern;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.AccessibilityStore;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.AccessibilitySettings;

/**
 * Trình quản lý Text-To-Speech (TTS) toàn diện cho GrandMa Assist (UC28).
 * - Lifecycle-aware: tự động dừng khi Activity pause/destroy/xoay màn hình (T11).
 * - Hỗ trợ tiếng Việt (Locale vi-VN), tốc độ đọc tùy chỉnh cho người cao tuổi.
 * - Kiểm tra TalkBack tránh nói chồng.
 * - Bộ lọc an toàn: không đọc mã OTP hoặc thông tin nhạy cảm.
 */
public final class TtsManager {
    private static volatile TtsManager INSTANCE;

    public interface PlaybackListener {
        void onStart(String utteranceId);
        void onDone(String utteranceId);
        void onError(String utteranceId, String message);
    }

    private final Context appContext;
    private final AccessibilityStore store;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private boolean initialized = false;
    private boolean languageSupported = false;
    private boolean isSpeaking = false;
    private String currentUtteranceId = "";
    private PlaybackListener currentListener;

    private static final Pattern PATTERN_CURRENCY = Pattern.compile("(?i)(\\d+)[\\s]*(đ|vnđ|vnd)");
    private static final Pattern PATTERN_SENSITIVE = Pattern.compile("(?i)(mã xác thực|otp|password|mật khẩu|token)");

    public static TtsManager getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (TtsManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new TtsManager(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }

    private TtsManager(Context context) {
        this.appContext = context;
        this.store = new AccessibilityStore(context);
        initTts();
    }

    private void initTts() {
        tts = new TextToSpeech(appContext, status -> {
            if (status == TextToSpeech.SUCCESS) {
                Locale viLocale = new Locale("vi", "VN");
                int result = tts.setLanguage(viLocale);
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    result = tts.setLanguage(new Locale("vi"));
                }
                languageSupported = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED);
                initialized = true;

                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        mainHandler.post(() -> {
                            isSpeaking = true;
                            if (currentListener != null && utteranceId.equals(currentUtteranceId)) {
                                currentListener.onStart(utteranceId);
                            }
                        });
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        mainHandler.post(() -> {
                            isSpeaking = false;
                            if (currentListener != null && utteranceId.equals(currentUtteranceId)) {
                                currentListener.onDone(utteranceId);
                            }
                        });
                    }

                    @Override
                    public void onError(String utteranceId) {
                        mainHandler.post(() -> {
                            isSpeaking = false;
                            if (currentListener != null && utteranceId.equals(currentUtteranceId)) {
                                currentListener.onError(utteranceId, "Lỗi phát âm thanh");
                            }
                        });
                    }

                    @Override
                    public void onError(String utteranceId, int errorCode) {
                        onError(utteranceId);
                    }
                });
            } else {
                initialized = false;
                languageSupported = false;
            }
        });
    }

    /**
     * Gắn vòng đời Activity để tự động ngắt âm thanh khi pause / stop / destroy (Quy tắc T11).
     */
    public void bindLifecycle(LifecycleOwner owner) {
        if (owner == null) return;
        owner.getLifecycle().addObserver(new LifecycleEventObserver() {
            @Override
            public void onStateChanged(@NonNull LifecycleOwner source, @NonNull Lifecycle.Event event) {
                if (event == Lifecycle.Event.ON_PAUSE || event == Lifecycle.Event.ON_STOP || event == Lifecycle.Event.ON_DESTROY) {
                    stop();
                }
            }
        });
    }

    /**
     * Kiểm tra người dùng có đang bật TalkBack hay không để tránh đọc chồng âm thanh.
     */
    public boolean isTouchExplorationActive() {
        try {
            AccessibilityManager am = (AccessibilityManager) appContext.getSystemService(Context.ACCESSIBILITY_SERVICE);
            return am != null && am.isEnabled() && am.isTouchExplorationEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    public boolean isLanguageSupported() {
        return languageSupported;
    }

    public boolean isSpeaking() {
        return isSpeaking;
    }

    /**
     * Đọc văn bản theo cấu hình người dùng.
     */
    public void speak(String rawText, String utteranceId, PlaybackListener listener) {
        if (rawText == null || rawText.trim().isEmpty()) {
            if (listener != null) listener.onDone(utteranceId);
            return;
        }

        AccessibilitySettings settings = store.getSettings();
        if (!settings.voiceEnabled) {
            if (listener != null) listener.onError(utteranceId, "Giọng đọc đang tắt trong Cài đặt");
            return;
        }

        // Lọc nội dung nhạy cảm: không bao giờ đọc OTP hoặc mật khẩu
        if (PATTERN_SENSITIVE.matcher(rawText).find()) {
            if (listener != null) listener.onError(utteranceId, "Nội dung bảo mật không được đọc bằng giọng nói");
            return;
        }

        String cleanedText = formatSpeechText(rawText);
        if (cleanedText.isEmpty()) {
            if (listener != null) listener.onDone(utteranceId);
            return;
        }

        stop();

        this.currentUtteranceId = utteranceId;
        this.currentListener = listener;

        if (!initialized || tts == null) {
            if (listener != null) listener.onError(utteranceId, "Bộ đọc giọng nói chưa sẵn sàng");
            return;
        }

        tts.setSpeechRate(settings.speechRate);
        tts.setPitch(1.0f);

        Bundle params = new Bundle();
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
        int result = tts.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, params, utteranceId);

        if (result != TextToSpeech.SUCCESS) {
            isSpeaking = false;
            if (listener != null) listener.onError(utteranceId, "Không thể phát âm thanh");
        }
    }

    /**
     * Dừng ngay lập tức giọng đọc hiện tại.
     */
    public void stop() {
        if (tts != null) {
            try {
                tts.stop();
            } catch (Exception ignored) {}
        }
        boolean wasSpeaking = isSpeaking;
        isSpeaking = false;
        if (wasSpeaking && currentListener != null) {
            currentListener.onDone(currentUtteranceId);
        }
    }

    /**
     * Chuẩn hóa văn bản tiếng Việt để TTS phát âm tự nhiên, êm dịu, dễ nghe cho người cao tuổi.
     */
    public static String formatSpeechText(String raw) {
        if (raw == null) return "";
        String text = raw;

        // Xóa các ký hiệu markdown thông dụng
        text = text.replaceAll("[#*`_\\[\\]()]", " ");
        text = text.replaceAll("(?m)^\\s*[-+•]\\s*", ". "); // bullet points thành dấu ngắt câu

        // Thay thế ký hiệu tiền tệ sang "đồng" để máy đọc tự nhiên
        text = PATTERN_CURRENCY.matcher(text).replaceAll("$1 đồng");

        // Thay thế các ký tự gạch nối giữa giờ giấc (ví dụ 08:00 - 12:00 -> đến)
        text = text.replaceAll("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})", "từ $1 đến $2");

        // Dấu phân cách khu vực
        text = text.replaceAll("·", ", ");
        text = text.replaceAll("→", " chuyển sang ");

        // Chuẩn hóa khoảng trắng và dấu câu
        text = text.replaceAll("\\s+", " ").trim();
        return text;
    }

    public void shutdown() {
        if (tts != null) {
            try {
                tts.stop();
                tts.shutdown();
            } catch (Exception ignored) {}
            tts = null;
            initialized = false;
        }
    }
}
