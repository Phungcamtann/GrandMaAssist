package vn.edu.ueh.nhom4.grandmaassist.app.ui.admin;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.JobPostActivity;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.verification.VerificationActivity;

public final class AdminStatsActivity extends AppCompatActivity {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private RoleNavigationBar nav;
    private ProgressBar progress;
    private TextView updatedTime, messageText;
    private TextView employersCount, employersVerified;
    private TextView seekersCount, seekersVerified;
    private TextView jobsCount, jobsOpen;
    private TextView platformFee, completedSessions;
    private TextView totalSessionsCount, completedSessionsCount, completionRate;
    private boolean loading;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_stats);

        RoleNavigationBar.topbar(this, "ADMIN");
        nav = new RoleNavigationBar(this, this::navigate);
        nav.render("ADMIN", RoleNavigationBar.Action.STATS, true, true);

        progress = findViewById(R.id.statsProgress);
        updatedTime = findViewById(R.id.statsUpdatedTime);
        messageText = findViewById(R.id.statsMessage);

        employersCount = findViewById(R.id.statsEmployersCount);
        employersVerified = findViewById(R.id.statsEmployersVerified);
        seekersCount = findViewById(R.id.statsSeekersCount);
        seekersVerified = findViewById(R.id.statsSeekersVerified);
        jobsCount = findViewById(R.id.statsJobsCount);
        jobsOpen = findViewById(R.id.statsJobsOpen);
        platformFee = findViewById(R.id.statsPlatformFee);
        completedSessions = findViewById(R.id.statsCompletedSessions);
        totalSessionsCount = findViewById(R.id.statsTotalSessionsCount);
        completedSessionsCount = findViewById(R.id.statsCompletedSessionsCount);
        completionRate = findViewById(R.id.statsCompletionRate);

        findViewById(R.id.statsBack).setOnClickListener(v -> finish());
        findViewById(R.id.statsReload).setOnClickListener(v -> loadStats());

        findViewById(R.id.statsCardEmployers).setOnClickListener(v -> {
            startActivity(new Intent(this, VerificationActivity.class));
        });
        findViewById(R.id.statsCardSeekers).setOnClickListener(v -> {
            startActivity(new Intent(this, VerificationActivity.class));
        });
        findViewById(R.id.statsCardJobs).setOnClickListener(v -> {
            startActivity(new Intent(this, JobPostActivity.class));
        });

        loadStats();
    }

    private void navigate(RoleNavigationBar.Action action) {
        if (action == RoleNavigationBar.Action.STATS) return;
        finish();
    }

    private void loadStats() {
        if (loading) return;
        loading = true;
        progress.setVisibility(View.VISIBLE);
        messageText.setVisibility(View.GONE);
        updatedTime.setText("Đang tổng hợp số liệu thực tế...");

        Task<AggregateQuerySnapshot> tEmployerTotal = db.collection("users")
                .whereEqualTo("role", "EMPLOYER")
                .count().get(AggregateSource.SERVER);

        Task<AggregateQuerySnapshot> tEmployerVerified = db.collection("users")
                .whereEqualTo("role", "EMPLOYER")
                .whereIn("verificationStatus", Arrays.asList("APPROVED", "VERIFIED"))
                .count().get(AggregateSource.SERVER);

        Task<AggregateQuerySnapshot> tSeekerTotal = db.collection("users")
                .whereEqualTo("role", "SEEKER")
                .count().get(AggregateSource.SERVER);

        Task<AggregateQuerySnapshot> tSeekerVerified = db.collection("users")
                .whereEqualTo("role", "SEEKER")
                .whereIn("verificationStatus", Arrays.asList("APPROVED", "VERIFIED"))
                .count().get(AggregateSource.SERVER);

        Task<AggregateQuerySnapshot> tJobsTotal = db.collection("jobs")
                .count().get(AggregateSource.SERVER);

        Task<AggregateQuerySnapshot> tJobsOpen = db.collection("jobs")
                .whereEqualTo("status", "OPEN")
                .count().get(AggregateSource.SERVER);

        Task<QuerySnapshot> tSessions = db.collection("workSessions")
                .get(Source.SERVER);

        Tasks.whenAllComplete(tEmployerTotal, tEmployerVerified, tSeekerTotal, tSeekerVerified,
                tJobsTotal, tJobsOpen, tSessions).addOnCompleteListener(task -> {
            loading = false;
            progress.setVisibility(View.GONE);
            if (isFinishing() || isDestroyed()) return;

            long employers = safeCount(tEmployerTotal);
            long empVerified = safeCount(tEmployerVerified);
            long seekers = safeCount(tSeekerTotal);
            long sekVerified = safeCount(tSeekerVerified);
            long jobs = safeCount(tJobsTotal);
            long jOpen = safeCount(tJobsOpen);

            employersCount.setText(String.valueOf(employers));
            employersVerified.setText("Đã xác minh: " + empVerified);

            seekersCount.setText(String.valueOf(seekers));
            seekersVerified.setText("Đã duyệt CCCD: " + sekVerified);

            jobsCount.setText(String.valueOf(jobs));
            jobsOpen.setText("Đang mở: " + jOpen);

            long totalFee = 0L;
            long totalSessions = 0L;
            long completedCount = 0L;

            if (tSessions.isSuccessful() && tSessions.getResult() != null) {
                totalSessions = tSessions.getResult().size();
                for (DocumentSnapshot doc : tSessions.getResult()) {
                    String status = doc.getString("status");
                    if ("COMPLETED".equals(status)) {
                        completedCount++;
                        Long fee = doc.getLong("agreement.serviceFee");
                        if (fee != null && fee > 0) {
                            totalFee += fee;
                        } else {
                            Long labor = doc.getLong("agreement.laborAmount");
                            if (labor != null && labor > 0) {
                                totalFee += Math.round(labor * 0.10);
                            }
                        }
                    }
                }
            }

            NumberFormat vnFormat = NumberFormat.getInstance(new Locale("vi", "VN"));
            platformFee.setText(vnFormat.format(totalFee) + " đ");
            completedSessions.setText(completedCount + " ca hoàn tất");

            totalSessionsCount.setText(String.valueOf(totalSessions));
            completedSessionsCount.setText(String.valueOf(completedCount));

            int rate = totalSessions > 0 ? (int) Math.round((double) completedCount / totalSessions * 100) : 100;
            completionRate.setText(rate + "%");

            String timeStr = DateFormat.getTimeInstance(DateFormat.SHORT).format(new Date());
            updatedTime.setText("Cập nhật lúc: " + timeStr + " • Tự động tính theo thời gian thực");
        });
    }

    private long safeCount(Task<AggregateQuerySnapshot> task) {
        if (task != null && task.isSuccessful() && task.getResult() != null) {
            return task.getResult().getCount();
        }
        return 0L;
    }
}
