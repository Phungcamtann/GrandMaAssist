package vn.edu.ueh.nhom4.grandmaassist.app.notification;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import java.text.SimpleDateFormat;
import java.util.Locale;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.WorkSession;

/**
 * Đặt lịch báo trước ca làm việc (2 tiếng và 30 phút) thông qua AlarmManager.
 */
public final class ShiftAlarmScheduler {
    private ShiftAlarmScheduler() {}

    public static void scheduleReminders(Context context, WorkSession session) {
        if (context == null || session == null || session.agreement == null || session.agreement.startAt == null) return;

        // Chỉ nhắc nhở các ca hợp lệ chưa hoàn thành và chưa bị hủy
        if (WorkSession.STATUS_COMPLETED.equals(session.status) || WorkSession.STATUS_CANCELLED.equals(session.status)) {
            cancelReminders(context, session.id);
            return;
        }

        long startMillis = session.agreement.startAt.toDate().getTime();
        long now = System.currentTimeMillis();

        if (startMillis <= now) return; // Ca làm đã qua

        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
        String timeText = sdf.format(session.agreement.startAt.toDate());
        String tasks = session.agreement.tasks;
        String address = session.agreement.privateAddress;

        // Nhắc trước 2 tiếng (7200000 ms)
        long twoHoursBefore = startMillis - (2 * 60 * 60 * 1000L);
        if (twoHoursBefore > now) {
            scheduleAlarm(context, session.id, "2H", twoHoursBefore, tasks, address, timeText);
        }

        // Nhắc trước 30 phút (1800000 ms)
        long thirtyMinsBefore = startMillis - (30 * 60 * 1000L);
        if (thirtyMinsBefore > now) {
            scheduleAlarm(context, session.id, "30M", thirtyMinsBefore, tasks, address, timeText);
        } else if (startMillis > now) {
            // Ca làm bắt đầu trong vòng 30 phút tới (rất hay gặp khi test kịch bản) -> Báo ngay sau 3 giây!
            scheduleAlarm(context, session.id, "30M", now + 3000L, tasks, address, timeText);
        }
    }

    /**
     * Gửi ngay thông báo kiểm tra (Test Notification) để người dùng xác nhận thiết bị nhận thông báo bình thường.
     */
    public static void sendTestNotification(Context context) {
        if (context == null) return;
        ShiftReminderReceiver.showNotification(
                context,
                9999,
                "🔔 Kiểm tra thông báo GrandMaAssist",
                "Hệ thống thông báo ca làm việc đang hoạt động bình thường trên thiết bị này. Chúc cô/chú một ngày vui vẻ!",
                null
        );
    }

    /**
     * Phát thông báo tức thời khi trạng thái ca làm việc thay đổi quan trọng (VD: Đã ký quỹ / Đã hoàn tất).
     */
    public static void notifySessionMilestone(Context context, String sessionId, String title, String body) {
        if (context == null || sessionId == null) return;
        int notifId = (sessionId.hashCode() & 0x7FFFFFFF) + 500;
        ShiftReminderReceiver.showNotification(context, notifId, title, body, sessionId);
    }

    private static void scheduleAlarm(Context context, String sessionId, String type, long triggerAtMillis,
                                      String tasks, String address, String timeText) {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        Intent intent = new Intent(context, ShiftReminderReceiver.class);
        intent.putExtra(ShiftReminderReceiver.EXTRA_SESSION_ID, sessionId);
        intent.putExtra(ShiftReminderReceiver.EXTRA_TASKS, tasks);
        intent.putExtra(ShiftReminderReceiver.EXTRA_ADDRESS, address);
        intent.putExtra(ShiftReminderReceiver.EXTRA_TIME_TEXT, timeText);
        intent.putExtra(ShiftReminderReceiver.EXTRA_REMINDER_TYPE, type);

        int requestCode = sessionId.hashCode() + ("2H".equals(type) ? 120 : 30);

        PendingIntent pi = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
            }
        } catch (SecurityException e) {
            // Trường hợp thiếu quyền exact alarm trên Android 12+, dùng set thông thường
            am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi);
        }
    }

    public static void cancelReminders(Context context, String sessionId) {
        if (context == null || sessionId == null) return;
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;

        for (String type : new String[]{"2H", "30M"}) {
            int requestCode = sessionId.hashCode() + ("2H".equals(type) ? 120 : 30);
            Intent intent = new Intent(context, ShiftReminderReceiver.class);
            PendingIntent pi = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE | (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ? PendingIntent.FLAG_IMMUTABLE : 0)
            );
            if (pi != null) {
                am.cancel(pi);
                pi.cancel();
            }
        }
    }
}
