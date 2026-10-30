package vn.edu.ueh.nhom4.grandmaassist.app.notification;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import vn.edu.ueh.nhom4.grandmaassist.app.MainActivity;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.sessions.ShiftAgreementDetailActivity;

/**
 * Nhận cảnh báo từ AlarmManager để phát thông báo hệ thống nhắc nhở ca làm (trước 2 tiếng và trước 30 phút).
 */
public final class ShiftReminderReceiver extends BroadcastReceiver {
    public static final String CHANNEL_ID = "grandma_shift_channel";
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_TASKS = "tasks";
    public static final String EXTRA_ADDRESS = "address";
    public static final String EXTRA_TIME_TEXT = "time_text";
    public static final String EXTRA_REMINDER_TYPE = "reminder_type"; // "2H" hoặc "30M"

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null) return;
        String sessionId = intent.getStringExtra(EXTRA_SESSION_ID);
        String tasks = intent.getStringExtra(EXTRA_TASKS);
        String address = intent.getStringExtra(EXTRA_ADDRESS);
        String timeText = intent.getStringExtra(EXTRA_TIME_TEXT);
        String type = intent.getStringExtra(EXTRA_REMINDER_TYPE);

        if (tasks == null) tasks = "Ca làm việc sắp tới";
        if (timeText == null) timeText = "";
        if (address == null) address = "";

        ensureNotificationChannel(context);

        String title;
        String body;
        int notifId;

        if ("30M".equals(type)) {
            title = "🚗 Chuẩn bị xuất phát (còn 30 phút)";
            body = "Ca làm: " + tasks + " lúc " + timeText + " tại " + address + ". Chúc cô/chú di chuyển an toàn!";
            notifId = (sessionId != null ? sessionId.hashCode() : 100) + 30;
        } else {
            title = "⏰ Sắp đến giờ làm việc (còn 2 tiếng)";
            body = "Ca làm: " + tasks + " bắt đầu lúc " + timeText + ". Cô/chú xem lại thỏa thuận và chuẩn bị nhé!";
            notifId = (sessionId != null ? sessionId.hashCode() : 100) + 120;
        }

        showNotification(context, notifId, title, body, sessionId);
    }

    public static void showNotification(Context context, int notifId, String title, String body, String sessionId) {
        if (context == null) return;
        ensureNotificationChannel(context);

        Intent openIntent;
        if (sessionId != null && !sessionId.isEmpty()) {
            openIntent = new Intent(context, ShiftAgreementDetailActivity.class);
            openIntent.putExtra(ShiftAgreementDetailActivity.EXTRA_SESSION_ID, sessionId);
        } else {
            openIntent = new Intent(context, MainActivity.class);
        }
        openIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                notifId,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification_bell)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        try {
            android.graphics.Bitmap largeIcon = android.graphics.BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher);
            if (largeIcon != null) builder.setLargeIcon(largeIcon);
        } catch (Exception ignored) {}

        try {
            NotificationManagerCompat.from(context).notify(notifId, builder.build());
        } catch (SecurityException ignored) {
            // Android 13+ khi người dùng chưa cấp quyền POST_NOTIFICATIONS
        }
    }

    public static void ensureNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Nhắc nhở ca làm việc",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Thông báo nhắc nhở trước ca làm việc (2 tiếng và 30 phút) cho cô chú");
            channel.enableVibration(true);
            channel.setLockscreenVisibility(android.app.Notification.VISIBILITY_PUBLIC);
            NotificationManager nm = context.getSystemService(NotificationManager.class);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
