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
        }
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
