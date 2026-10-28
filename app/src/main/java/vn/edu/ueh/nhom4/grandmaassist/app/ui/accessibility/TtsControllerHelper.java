package vn.edu.ueh.nhom4.grandmaassist.app.ui.accessibility;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.accessibility.TtsManager;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.AccessibilityStore;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.AccessibilitySettings;

/**
 * Điều khiển giao diện nút TTS (Pill Button) chuẩn tiếp cận cho người lớn tuổi.
 * Xử lý các trạng thái: Chưa đọc (IDLE), Đang đọc (PLAYING), Đã đọc xong (FINISHED).
 */
public final class TtsControllerHelper {
    public enum State { IDLE, PLAYING, FINISHED }

    public interface TextSupplier {
        String getTextToSpeak();
    }

    private final View container;
    private final ImageView icon;
    private final TextView label;
    private final TtsManager ttsManager;
    private final AccessibilityStore store;
    private final String utteranceId;

    private State currentState = State.IDLE;
    private TextSupplier textSupplier;
    private String customIdleLabel = "Nghe hướng dẫn";

    public TtsControllerHelper(LifecycleOwner owner, View ttsView, String utteranceId) {
        this.container = ttsView.findViewById(R.id.ttsContainer) != null ? ttsView.findViewById(R.id.ttsContainer) : ttsView;
        this.icon = ttsView.findViewById(R.id.ttsIcon);
        this.label = ttsView.findViewById(R.id.ttsLabel);
        this.utteranceId = utteranceId;

        Context ctx = container.getContext();
        this.ttsManager = TtsManager.getInstance(ctx);
        this.store = new AccessibilityStore(ctx);

        if (owner != null) {
            this.ttsManager.bindLifecycle(owner);
        }

        this.container.setOnClickListener(v -> togglePlayback());
        updateUiState();
    }

    public void setCustomIdleLabel(String customIdleLabel) {
        this.customIdleLabel = customIdleLabel;
        if (currentState == State.IDLE) {
            updateUiState();
        }
    }

    public void setTextSupplier(TextSupplier supplier) {
        this.textSupplier = supplier;
    }

    public void reset() {
        ttsManager.stop();
        currentState = State.IDLE;
        updateUiState();
    }

    /**
     * Tự động phát nếu người dùng đã bật cấu hình autoReadEnabled trong cài đặt.
     */
    public void autoPlayIfEnabled() {
        AccessibilitySettings settings = store.getSettings();
        if (settings.voiceEnabled && settings.autoReadEnabled && !ttsManager.isTouchExplorationActive()) {
            container.postDelayed(this::startPlayback, 350);
        }
    }

    private void togglePlayback() {
        if (currentState == State.PLAYING) {
            ttsManager.stop();
            currentState = State.IDLE;
            updateUiState();
        } else {
            startPlayback();
        }
    }

    public void startPlayback() {
        AccessibilitySettings settings = store.getSettings();
        if (!settings.voiceEnabled) {
            Toast.makeText(container.getContext(), "Giọng đọc đang tắt trong Cài đặt hỗ trợ tiếp cận", Toast.LENGTH_SHORT).show();
            return;
        }

        if (textSupplier == null) return;
        String text = textSupplier.getTextToSpeak();
        if (text == null || text.trim().isEmpty()) return;

        currentState = State.PLAYING;
        updateUiState();

        ttsManager.speak(text, utteranceId, new TtsManager.PlaybackListener() {
            @Override
            public void onStart(String id) {
                currentState = State.PLAYING;
                updateUiState();
            }

            @Override
            public void onDone(String id) {
                currentState = State.FINISHED;
                updateUiState();
            }

            @Override
            public void onError(String id, String message) {
                currentState = State.IDLE;
                updateUiState();
                Toast.makeText(container.getContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUiState() {
        AccessibilitySettings settings = store.getSettings();
        if (!settings.voiceEnabled) {
            container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);

        if (currentState == State.PLAYING) {
            container.setBackgroundResource(R.drawable.bg_tts_pill_playing);
            if (icon != null) {
                icon.setImageResource(R.drawable.ic_tts_stop);
            }
            if (label != null) {
                label.setText("Dừng đọc");
                label.setTextColor(Color.parseColor("#C62828"));
            }
            container.setContentDescription("Dừng đọc giọng nói");
        } else if (currentState == State.FINISHED) {
            container.setBackgroundResource(R.drawable.bg_tts_pill);
            if (icon != null) {
                icon.setImageResource(R.drawable.ic_tts_replay);
            }
            if (label != null) {
                label.setText("Nghe lại");
                label.setTextColor(Color.parseColor("#1C6758"));
            }
            container.setContentDescription("Nghe lại giọng nói hướng dẫn");
        } else {
            container.setBackgroundResource(R.drawable.bg_tts_pill);
            if (icon != null) {
                icon.setImageResource(R.drawable.ic_tts_play);
            }
            if (label != null) {
                label.setText(customIdleLabel);
                label.setTextColor(Color.parseColor("#1C6758"));
            }
            container.setContentDescription(customIdleLabel);
        }
    }
}
