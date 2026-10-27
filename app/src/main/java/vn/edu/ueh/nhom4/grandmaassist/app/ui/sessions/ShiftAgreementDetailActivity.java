package vn.edu.ueh.nhom4.grandmaassist.app.ui.sessions;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Locale;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.AgreementSnapshot;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.WorkSession;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.WalletRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.wallet.WalletActivity;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.ShiftAgreementViewModel;

public final class ShiftAgreementDetailActivity extends AppCompatActivity {
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_APPLICATION_ID = "application_id";

    public static void start(android.content.Context context, String sessionId) {
        android.content.Intent intent = new android.content.Intent(context, ShiftAgreementDetailActivity.class);
        intent.putExtra(EXTRA_SESSION_ID, sessionId);
        context.startActivity(intent);
    }

    private ShiftAgreementViewModel vm;
    private String sessionId = "";
    private String applicationId = "";
    private String currentUid = "";

    private TextView tvBadge;
    private TextView tvVersion;
    private TextView tvTasks;
    private TextView tvSchedule;
    private TextView tvAddress;
    private View layoutSeekerPay;
    private TextView tvSeekerPayAmount;
    private View layoutEmployerPay;
    private TextView tvWorkerPay;
    private TextView tvServiceFee;
    private TextView tvTotalDue;
    private View layoutSeekerActions;
    private View layoutEmployerActions;
    private ProgressBar pb;
    private TextView tvMessage;
    private View layoutEmployerFunding;
    private AppCompatButton btnEmployerFund;
    private TextView tvEmployerWalletInfo;
    private View cardSessionReadyNotice;
    private TextView tvSessionReadyNotice;
    private final WalletRepository walletRepo = new WalletRepository();
    private long currentWalletBalance = 0L;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_shift_agreement_detail);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.shiftAgreementDetailRoot), (v, i) -> {
            androidx.core.graphics.Insets safe = i.getInsets(
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

        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        applicationId = getIntent().getStringExtra(EXTRA_APPLICATION_ID);

        tvBadge = findViewById(R.id.shiftDetailBadge);
        tvVersion = findViewById(R.id.shiftDetailVersionText);
        tvTasks = findViewById(R.id.shiftDetailTasks);
        tvSchedule = findViewById(R.id.shiftDetailSchedule);
        tvAddress = findViewById(R.id.shiftDetailAddress);
        layoutSeekerPay = findViewById(R.id.layoutDetailSeekerPay);
        tvSeekerPayAmount = findViewById(R.id.tvDetailSeekerPayAmount);
        layoutEmployerPay = findViewById(R.id.layoutDetailEmployerPay);
        tvWorkerPay = findViewById(R.id.tvDetailWorkerPay);
        tvServiceFee = findViewById(R.id.tvDetailServiceFee);
        tvTotalDue = findViewById(R.id.tvDetailTotalDue);
        layoutSeekerActions = findViewById(R.id.shiftDetailSeekerActions);
        layoutEmployerActions = findViewById(R.id.shiftDetailEmployerActions);
        pb = findViewById(R.id.shiftDetailProgress);
        tvMessage = findViewById(R.id.shiftDetailMessage);
        layoutEmployerFunding = findViewById(R.id.shiftDetailEmployerFunding);
        btnEmployerFund = findViewById(R.id.btnEmployerFund);
        tvEmployerWalletInfo = findViewById(R.id.tvEmployerWalletInfo);
        cardSessionReadyNotice = findViewById(R.id.cardSessionReadyNotice);
        tvSessionReadyNotice = findViewById(R.id.tvSessionReadyNotice);

        findViewById(R.id.shiftDetailBack).setOnClickListener(v -> finish());
        findViewById(R.id.shiftDetailReload).setOnClickListener(v -> reload());

        vm = new ViewModelProvider(this).get(ShiftAgreementViewModel.class);

        AppCompatButton btnAccept = findViewById(R.id.btnSeekerAccept);
        AppCompatButton btnReject = findViewById(R.id.btnSeekerReject);
        btnAccept.setOnClickListener(v -> confirmSeekerAccept());
        btnReject.setOnClickListener(v -> confirmSeekerReject());

        AppCompatButton btnEdit = findViewById(R.id.btnEmployerEdit);
        AppCompatButton btnCancel = findViewById(R.id.btnEmployerCancel);
        btnEdit.setOnClickListener(v -> {
            if (vm.currentSession == null) return;
            Intent intent = new Intent(this, ShiftAgreementEditActivity.class);
            intent.putExtra(ShiftAgreementEditActivity.EXTRA_SESSION_ID, vm.currentSession.id);
            startActivity(intent);
        });
        btnCancel.setOnClickListener(v -> confirmEmployerCancel());

        vm.changes.observe(this, n -> render());

        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void reload() {
        if (!currentUid.isEmpty()) {
            walletRepo.getWallet(currentUid).addOnSuccessListener(doc -> {
                if (doc != null && doc.exists()) {
                    Long b = doc.getLong("balance");
                    currentWalletBalance = b != null ? b : 0L;
                } else {
                    currentWalletBalance = 0L;
                }
                render();
            });
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            vm.loadSession(sessionId);
        } else if (applicationId != null && !applicationId.isEmpty()) {
            vm.loadApplicationForInit(applicationId, () -> {
                if (vm.currentSession != null) {
                    sessionId = vm.currentSession.id;
                    render();
                } else {
                    if (vm.message != null && !vm.message.isEmpty()) {
                        Toast.makeText(this, vm.message, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(this, "Chưa có thỏa thuận ca làm cho đơn này.", Toast.LENGTH_SHORT).show();
                    }
                    finish();
                }
            });
        }
    }

    private void render() {
        pb.setVisibility(vm.busy ? View.VISIBLE : View.GONE);
        if (vm.message != null && !vm.message.isEmpty()) {
            tvMessage.setText(vm.message);
            tvMessage.setVisibility(View.VISIBLE);
        } else {
            tvMessage.setVisibility(View.GONE);
        }

        WorkSession session = vm.currentSession;
        if (session == null || session.agreement == null) return;

        AgreementSnapshot ag = session.agreement;
        renderBadge(tvBadge, session.status);
        tvVersion.setText("Phiên bản: #" + session.agreementVersion);
        tvTasks.setText(ag.tasks != null && !ag.tasks.isEmpty() ? ag.tasks : "Ca làm việc");

        // Format Schedule
        String scheduleStr = "";
        if (ag.startAt != null) {
            DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, new Locale("vi", "VN"));
            String dateStr = df.format(ag.startAt.toDate());
            DateFormat tf = DateFormat.getTimeInstance(DateFormat.SHORT, new Locale("vi", "VN"));
            String startStr = tf.format(ag.startAt.toDate());
            String endStr = ag.endAt != null ? tf.format(ag.endAt.toDate()) : "";
            scheduleStr = dateStr + "\n" + startStr + " - " + endStr;
        }
        tvSchedule.setText(scheduleStr);
                String workAddr = "";
        String area = "";
        if (vm.currentApplication != null && vm.currentApplication.jobSnapshot != null) {
            workAddr = vm.currentApplication.jobSnapshot.workAddress != null ? vm.currentApplication.jobSnapshot.workAddress.trim() : "";
            area = vm.currentApplication.jobSnapshot.areaName != null ? vm.currentApplication.jobSnapshot.areaName.trim() : "";
        }
        if (workAddr.isEmpty() && ag.privateAddress != null) {
            workAddr = ag.privateAddress.trim();
        }
        if (area.isEmpty() && vm.areaName != null) {
            area = vm.areaName.trim();
        }

        if (workAddr.startsWith("Địa chỉ:")) {
            workAddr = workAddr.substring("Địa chỉ:".length()).trim();
        }

        StringBuilder sb = new StringBuilder();
        if (!workAddr.isEmpty()) {
            sb.append(workAddr);
        }
        if (!area.isEmpty() && !sb.toString().contains(area)) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("Khu vực: ").append(area);
        }
        tvAddress.setText(sb.length() > 0 ? sb.toString() : "Địa chỉ làm việc theo tin tuyển dụng");

        boolean isSeeker = currentUid.equals(session.seekerId);
        boolean isEmployer = currentUid.equals(session.employerId);

        if (isSeeker) {
            RoleNavigationBar.topbar(this, "SEEKER");
            layoutSeekerPay.setVisibility(View.VISIBLE);
            layoutEmployerPay.setVisibility(View.GONE);
            tvSeekerPayAmount.setText(formatMoney(ag.laborAmount));

            if (session.isProposed()) {
                layoutSeekerActions.setVisibility(View.VISIBLE);
                cardSessionReadyNotice.setVisibility(View.GONE);
            } else {
                layoutSeekerActions.setVisibility(View.GONE);
                if (session.isReady()) {
                    cardSessionReadyNotice.setVisibility(View.VISIBLE);
                    tvSessionReadyNotice.setText("✓ Người thuê đã nạp đủ " + formatMoney(ag.totalDue) + " tiền cọc bảo chứng trên hệ thống. Cô/chú an tâm chuẩn bị đến ca làm đúng giờ.");
                } else {
                    cardSessionReadyNotice.setVisibility(View.GONE);
                }
            }
            layoutEmployerActions.setVisibility(View.GONE);
            layoutEmployerFunding.setVisibility(View.GONE);
        } else {
            RoleNavigationBar.topbar(this, "EMPLOYER");
            layoutSeekerPay.setVisibility(View.GONE);
            layoutEmployerPay.setVisibility(View.VISIBLE);
            tvWorkerPay.setText(formatMoney(ag.laborAmount));
            tvServiceFee.setText("+" + formatMoney(ag.serviceFee));
            tvTotalDue.setText(formatMoney(ag.totalDue));

            if (session.isProposed()) {
                layoutEmployerActions.setVisibility(View.VISIBLE);
                layoutEmployerFunding.setVisibility(View.GONE);
                cardSessionReadyNotice.setVisibility(View.GONE);
            } else if (session.isAwaitingFunds()) {
                layoutEmployerActions.setVisibility(View.GONE);
                layoutEmployerFunding.setVisibility(View.VISIBLE);
                cardSessionReadyNotice.setVisibility(View.GONE);
                btnEmployerFund.setText("Cấp tiền ca làm (" + formatMoney(ag.totalDue) + ")");
                tvEmployerWalletInfo.setText("Số dư ví hiện tại: " + formatMoney(currentWalletBalance));
                btnEmployerFund.setOnClickListener(v -> handleEmployerFund(ag.totalDue));
            } else if (session.isReady()) {
                layoutEmployerActions.setVisibility(View.GONE);
                layoutEmployerFunding.setVisibility(View.GONE);
                cardSessionReadyNotice.setVisibility(View.VISIBLE);
                tvSessionReadyNotice.setText("✓ Đã tạm giữ cọc " + formatMoney(ag.totalDue) + " an toàn trên hệ thống. Ca làm việc đã sẵn sàng.");
            } else {
                layoutEmployerActions.setVisibility(View.GONE);
                layoutEmployerFunding.setVisibility(View.GONE);
                cardSessionReadyNotice.setVisibility(View.GONE);
            }
            layoutSeekerActions.setVisibility(View.GONE);
        }
    }

    private void renderBadge(TextView badge, String status) {
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
        } else if (WorkSession.STATUS_CANCELLED.equals(status)) {
            badge.setText(R.string.session_status_cancelled);
            badge.setBackgroundColor(Color.parseColor("#EEEEEE"));
            badge.setTextColor(Color.parseColor("#616161"));
        } else {
            badge.setText(WorkSession.statusLabel(status));
            badge.setBackgroundColor(Color.parseColor("#EEEEEE"));
            badge.setTextColor(Color.parseColor("#616161"));
        }
    }

    private void confirmSeekerAccept() {
        if (vm.currentSession == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.session_accept_dialog_title)
                .setMessage(R.string.session_accept_dialog_msg)
                .setPositiveButton(R.string.session_btn_accept, (d, w) -> {
                    vm.seekerAccept(vm.currentSession.id, vm.currentSession.agreementVersion, () -> {
                        Toast.makeText(this, "Đã đồng ý nhận ca làm thành công!", Toast.LENGTH_SHORT).show();
                    }, err -> {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton(R.string.applicant_action_cancel, null)
                .show();
    }

    private void confirmSeekerReject() {
        if (vm.currentSession == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.session_reject_dialog_title)
                .setMessage(R.string.session_reject_dialog_msg)
                .setPositiveButton(R.string.session_btn_reject, (d, w) -> {
                    vm.seekerReject(vm.currentSession.id, () -> {
                        Toast.makeText(this, "Đã từ chối ca làm.", Toast.LENGTH_SHORT).show();
                        finish();
                    }, err -> {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton(R.string.applicant_action_cancel, null)
                .show();
    }

    private void confirmEmployerCancel() {
        if (vm.currentSession == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Hủy đề xuất thỏa thuận?")
                .setMessage("Bạn có chắc muốn hủy đề xuất ca làm này không?")
                .setPositiveButton("Hủy ca", (d, w) -> {
                    vm.seekerReject(vm.currentSession.id, () -> {
                        Toast.makeText(this, "Đã hủy ca làm.", Toast.LENGTH_SHORT).show();
                        finish();
                    }, err -> {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    private String formatMoney(long amount) {
        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(amount) + " đ";
    }

    private void handleEmployerFund(long totalDue) {
        if (vm.currentSession == null) return;
        if (currentWalletBalance < totalDue) {
            long missing = totalDue - currentWalletBalance;
            new AlertDialog.Builder(this)
                    .setTitle("Số dư ví không đủ")
                    .setMessage("Ví của bạn hiện có " + formatMoney(currentWalletBalance) + " (còn thiếu " + formatMoney(missing) + "). Vui lòng nạp thêm tiền vào ví để cấp tiền cho ca làm này.")
                    .setPositiveButton("Nạp tiền ngay", (d, w) -> {
                        WalletActivity.start(this, "EMPLOYER", missing);
                    })
                    .setNegativeButton("Để sau", null)
                    .show();
        } else {
            new AlertDialog.Builder(this)
                    .setTitle("Xác nhận cấp tiền ca làm")
                    .setMessage("Hệ thống sẽ trích " + formatMoney(totalDue) + " từ ví của bạn để tạm giữ cọc an toàn cho ca làm này. Tiền chỉ chuyển cho người làm khi bạn xác nhận hoàn thành.")
                    .setPositiveButton("Xác nhận cấp tiền", (d, w) -> {
                        btnEmployerFund.setEnabled(false);
                        vm.fundSession(vm.currentSession.id, () -> {
                            Toast.makeText(this, "Cấp tiền ca làm thành công! Ca làm đã sẵn sàng.", Toast.LENGTH_LONG).show();
                            reload();
                        }, err -> {
                            btnEmployerFund.setEnabled(true);
                            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                        });
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
        }
    }

}
