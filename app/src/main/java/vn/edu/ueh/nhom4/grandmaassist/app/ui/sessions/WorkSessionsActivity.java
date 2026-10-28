package vn.edu.ueh.nhom4.grandmaassist.app.ui.sessions;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.WorkSessionRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.AgreementSnapshot;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.WorkSession;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.JobPostActivity;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;

public final class WorkSessionsActivity extends AppCompatActivity {
    public static final String EXTRA_ROLE = "extra_role";

    public static void start(Context context, String role) {
        Intent intent = new Intent(context, WorkSessionsActivity.class);
        intent.putExtra(EXTRA_ROLE, role);
        context.startActivity(intent);
    }

    private final WorkSessionRepository repository = new WorkSessionRepository();
    private String currentUid = "";
    private String role = "SEEKER";

    private TextView tvTitle;
    private TextView tvSubtitle;
    private AppCompatButton btnTabActive;
    private AppCompatButton btnTabHistory;
    private RecyclerView rvSessions;
    private View layoutEmpty;
    private TextView tvEmptyTitle;
    private TextView tvEmptySubtitle;
    private ProgressBar pbLoading;
    private RoleNavigationBar nav;
    private final SessionsAdapter adapter = new SessionsAdapter();

    private final List<WorkSession> allSessions = new ArrayList<>();
    private boolean showHistory = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_work_sessions);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.workSessionsRoot), (v, i) -> {
            Insets safe = i.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime()
            );
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return i;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        currentUid = user != null ? user.getUid() : "";
        if (currentUid.isEmpty()) {
            Toast.makeText(this, R.string.sj_login_prompt, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        String r = getIntent().getStringExtra(EXTRA_ROLE);
        if (r != null && !r.isEmpty()) role = r;

        tvTitle = findViewById(R.id.wsTitle);
        tvSubtitle = findViewById(R.id.wsSubtitle);
        btnTabActive = findViewById(R.id.btnTabActive);
        btnTabHistory = findViewById(R.id.btnTabHistory);
        rvSessions = findViewById(R.id.rvWorkSessions);
        layoutEmpty = findViewById(R.id.layoutEmptySessions);
        tvEmptyTitle = findViewById(R.id.tvEmptyTitle);
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle);
        pbLoading = findViewById(R.id.pbSessionsLoading);

        boolean isEmployer = "EMPLOYER".equals(role);
        if (isEmployer) {
            tvTitle.setText("Ca làm việc");
            tvSubtitle.setText("Danh sách các ca làm việc đã kết nối của bạn.");
        } else {
            tvTitle.setText("Việc cần xử lý");
            tvSubtitle.setText("Danh sách ca làm việc và thỏa thuận cần theo dõi.");
        }

        RoleNavigationBar.topbar(this, role);

        findViewById(R.id.wsBack).setOnClickListener(v -> finish());
        findViewById(R.id.wsReload).setOnClickListener(v -> loadSessions());

        btnTabActive.setOnClickListener(v -> {
            if (showHistory) {
                showHistory = false;
                updateTabStyles();
                filterAndRender();
            }
        });

        btnTabHistory.setOnClickListener(v -> {
            if (!showHistory) {
                showHistory = true;
                updateTabStyles();
                filterAndRender();
            }
        });

        rvSessions.setLayoutManager(new LinearLayoutManager(this));
        rvSessions.setAdapter(adapter);

        // Bottom Navigation
        nav = new RoleNavigationBar(this, action -> {
            if (action == RoleNavigationBar.Action.HOME) {
                finish();
            } else if (action == RoleNavigationBar.Action.WORK) {
                if (isEmployer) {
                    startActivity(new Intent(this, JobPostActivity.class));
                    finish();
                } else {
                    finish();
                }
            } else if (action == RoleNavigationBar.Action.OTHER) {
                // Already here
                loadSessions();
            } else if (action == RoleNavigationBar.Action.PROFILE) {
                finish();
            }
        });

        nav.render(role, isEmployer ? RoleNavigationBar.Action.OTHER : RoleNavigationBar.Action.HOME, true, true);

        updateTabStyles();
        loadSessions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSessions();
    }

    private void updateTabStyles() {
        if (!showHistory) {
            btnTabActive.setBackgroundResource(R.drawable.button_primary);
            btnTabActive.setTextColor(Color.WHITE);
            btnTabHistory.setBackgroundResource(R.drawable.button_secondary);
            btnTabHistory.setTextColor(Color.parseColor("#444444"));
        } else {
            btnTabHistory.setBackgroundResource(R.drawable.button_primary);
            btnTabHistory.setTextColor(Color.WHITE);
            btnTabActive.setBackgroundResource(R.drawable.button_secondary);
            btnTabActive.setTextColor(Color.parseColor("#444444"));
        }
    }

    private void loadSessions() {
        if (currentUid.isEmpty()) return;
        pbLoading.setVisibility(View.VISIBLE);

        repository.getSessionsForUser(currentUid, role).addOnCompleteListener(t -> {
            pbLoading.setVisibility(View.GONE);
            if (t.isSuccessful() && t.getResult() != null) {
                allSessions.clear();
                allSessions.addAll(t.getResult());

                // Auto-sync completed sessions to seeker's wallet / refunds to employer
                if ("SEEKER".equals(role)) {
                    for (WorkSession s : allSessions) {
                        if (s.isCompleted()) {
                            repository.syncSettlementToWallet(currentUid, s);
                        }
                    }
                } else if ("EMPLOYER".equals(role)) {
                    for (WorkSession s : allSessions) {
                        if (s.isCancelled() && s.totalFunded > 0) {
                            repository.syncRefundToEmployerWallet(currentUid, s);
                        }
                    }
                }

                filterAndRender();
            } else {
                layoutEmpty.setVisibility(View.VISIBLE);
                rvSessions.setVisibility(View.GONE);
                Toast.makeText(this, "Không thể tải danh sách ca làm việc.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void filterAndRender() {
        List<WorkSession> activeList = new ArrayList<>();
        List<WorkSession> historyList = new ArrayList<>();

        for (WorkSession s : allSessions) {
            if (s.isCompleted() || s.isCancelled() || WorkSession.STATUS_DISPUTED.equals(s.status)) {
                historyList.add(s);
            } else {
                activeList.add(s);
            }
        }

        btnTabActive.setText("Đang & Sắp làm (" + activeList.size() + ")");
        btnTabHistory.setText("Lịch sử đã xong (" + historyList.size() + ")");

        List<WorkSession> displayList = showHistory ? historyList : activeList;
        adapter.setItems(displayList, "EMPLOYER".equals(role));

        if (displayList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvSessions.setVisibility(View.GONE);
            if (showHistory) {
                tvEmptyTitle.setText("Chưa có ca làm nào trong lịch sử");
                tvEmptySubtitle.setText("Các ca làm việc đã hoàn thành hoặc đã hủy sẽ được lưu trữ tại đây.");
            } else {
                tvEmptyTitle.setText("Không có ca làm nào đang hoặc sắp diễn ra");
                tvEmptySubtitle.setText("Các ca làm việc đang chờ xác nhận hoặc sắp tới sẽ hiển thị tại đây để bạn tiện theo dõi.");
            }
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvSessions.setVisibility(View.VISIBLE);
        }
    }

    private final class SessionsAdapter extends RecyclerView.Adapter<SessionViewHolder> {
        private final List<WorkSession> items = new ArrayList<>();
        private boolean isEmployer = false;

        public void setItems(List<WorkSession> newItems, boolean isEmployer) {
            this.items.clear();
            if (newItems != null) this.items.addAll(newItems);
            this.isEmployer = isEmployer;
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_work_session, parent, false);
            return new SessionViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
            WorkSession session = items.get(position);
            holder.bind(session, isEmployer);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }
    }

    private final class SessionViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTasks;
        private final TextView tvBadge;
        private final TextView tvSchedule;
        private final TextView tvAddress;
        private final TextView tvPay;
        private final AppCompatButton btnView;

        public SessionViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTasks = itemView.findViewById(R.id.itemSessionTasks);
            tvBadge = itemView.findViewById(R.id.itemSessionBadge);
            tvSchedule = itemView.findViewById(R.id.itemSessionSchedule);
            tvAddress = itemView.findViewById(R.id.itemSessionAddress);
            tvPay = itemView.findViewById(R.id.itemSessionPay);
            btnView = itemView.findViewById(R.id.btnItemViewSession);
        }

        public void bind(WorkSession session, boolean isEmployer) {
            AgreementSnapshot ag = session.agreement;
            tvTasks.setText(ag != null && ag.tasks != null && !ag.tasks.isEmpty() ? ag.tasks : "Ca làm việc");

            renderBadge(tvBadge, session);

            // Schedule
            String scheduleStr = "";
            if (ag != null && ag.startAt != null) {
                DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM, new Locale("vi", "VN"));
                String dateStr = df.format(ag.startAt.toDate());
                DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT, new Locale("vi", "VN"));
                String startStr = tf.format(ag.startAt.toDate());
                String endStr = ag.endAt != null ? tf.format(ag.endAt.toDate()) : "";
                scheduleStr = startStr + " - " + endStr + " (" + dateStr + ")";
            }
            tvSchedule.setText(scheduleStr.isEmpty() ? "Theo thỏa thuận" : scheduleStr);

            // Address
            String addr = ag != null && ag.privateAddress != null ? ag.privateAddress : "Địa chỉ theo tin tuyển dụng";
            if (addr.startsWith("Địa chỉ:")) addr = addr.substring("Địa chỉ:".length()).trim();
            tvAddress.setText(addr);

            // Pay
            if (ag != null) {
                if (isEmployer) {
                    tvPay.setText(formatMoney(ag.laborAmount) + " (Tổng cọc: " + formatMoney(ag.totalDue) + ")");
                } else {
                    tvPay.setText(formatMoney(ag.laborAmount) + " (Nhận trọn vẹn)");
                }
            } else {
                tvPay.setText("0 đ");
            }

            // Click listener
            View.OnClickListener clickListener = v -> ShiftAgreementDetailActivity.start(WorkSessionsActivity.this, session.id);
            itemView.setOnClickListener(clickListener);
            btnView.setOnClickListener(clickListener);
        }

        private void renderBadge(TextView badge, WorkSession session) {
            if (session == null) return;
            if (session.isCancellationRequested()) {
                badge.setText("Chờ hủy ca");
                badge.setBackgroundColor(Color.parseColor("#FFF3E0"));
                badge.setTextColor(Color.parseColor("#E65100"));
                return;
            }
            String status = session.status;
            if (WorkSession.STATUS_PROPOSED.equals(status)) {
                badge.setText(R.string.session_status_proposed);
                badge.setBackgroundColor(Color.parseColor("#FFF3E0"));
                badge.setTextColor(Color.parseColor("#E65100"));
            } else if (WorkSession.STATUS_AWAITING_FUNDS.equals(status)) {
                badge.setText(R.string.session_status_awaiting_funds);
                badge.setBackgroundColor(Color.parseColor("#E8F5E9"));
                badge.setTextColor(Color.parseColor("#1C6758"));
            } else if (WorkSession.STATUS_READY.equals(status)) {
                badge.setText(R.string.session_status_ready);
                badge.setBackgroundColor(Color.parseColor("#E8F5E9"));
                badge.setTextColor(Color.parseColor("#1C6758"));
            } else if (WorkSession.STATUS_IN_PROGRESS.equals(status)) {
                badge.setText("Đang thực hiện");
                badge.setBackgroundColor(Color.parseColor("#E8F5E9"));
                badge.setTextColor(Color.parseColor("#1C6758"));
            } else if (WorkSession.STATUS_COMPLETION_PENDING.equals(status)) {
                badge.setText("Chờ xác nhận xong");
                badge.setBackgroundColor(Color.parseColor("#FFF3E0"));
                badge.setTextColor(Color.parseColor("#E65100"));
            } else if (WorkSession.STATUS_COMPLETED.equals(status)) {
                badge.setText("Đã hoàn thành");
                badge.setBackgroundColor(Color.parseColor("#E8F5E9"));
                badge.setTextColor(Color.parseColor("#1C6758"));
            } else if (WorkSession.STATUS_CANCELLED.equals(status)) {
                badge.setText(R.string.session_status_cancelled);
                badge.setBackgroundColor(Color.parseColor("#FFEBEE"));
                badge.setTextColor(Color.parseColor("#C62828"));
            } else {
                badge.setText(WorkSession.statusLabel(status));
                badge.setBackgroundColor(Color.parseColor("#EEEEEE"));
                badge.setTextColor(Color.parseColor("#616161"));
            }
        }

        private String formatMoney(long amount) {
            return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(amount) + " đ";
        }
    }
}
