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
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
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
    private vn.edu.ueh.nhom4.grandmaassist.app.ui.accessibility.TtsControllerHelper ttsHelper;
    private TextView tvMessage;
    private View layoutEmployerFunding;
    private AppCompatButton btnEmployerFund;
    private TextView tvEmployerWalletInfo;
    private View cardSessionReadyNotice;
    private TextView tvSessionReadyNotice;

    // Check-in and In-Progress views
    private View layoutSeekerCheckIn;
    private AppCompatButton btnSeekerCheckIn;
    private TextView tvSeekerCheckInNotice;
    private View layoutEmployerCheckIn;
    private AppCompatButton btnEmployerCheckIn;
    private TextView tvEmployerCheckInNotice;
    private View cardSessionLiveStatus;
    private TextView tvLiveStatusHeader;
    private TextView tvLiveStatusDetail;
    private AppCompatButton btnSeekerReportComplete;
    private AppCompatButton btnEmployerConfirmComplete;
    private View cardSessionCompletedNotice;
    private TextView tvSessionCompletedMessage;
    private AppCompatButton btnOpenWalletAfterComplete;

    // Cancellation views (UC23)
    private View cardCancellationPending;
    private TextView tvCancellationPendingTitle;
    private TextView tvCancellationPendingDetail;
    private AppCompatButton btnWithdrawCancelRequest;
    private View layoutRecipientCancelActions;
    private AppCompatButton btnConfirmCancelSession;
    private AppCompatButton btnRejectCancelRequest;
    private AppCompatButton btnRequestCancelWhenReady;
    private View cardSessionCancelledNotice;
    private TextView tvSessionCancelledMessage;
    private AppCompatButton btnOpenWalletAfterCancel;

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

        layoutSeekerCheckIn = findViewById(R.id.layoutSeekerCheckIn);
        btnSeekerCheckIn = findViewById(R.id.btnSeekerCheckIn);
        tvSeekerCheckInNotice = findViewById(R.id.tvSeekerCheckInNotice);
        layoutEmployerCheckIn = findViewById(R.id.layoutEmployerCheckIn);
        btnEmployerCheckIn = findViewById(R.id.btnEmployerCheckIn);
        tvEmployerCheckInNotice = findViewById(R.id.tvEmployerCheckInNotice);
        cardSessionLiveStatus = findViewById(R.id.cardSessionLiveStatus);
        tvLiveStatusHeader = findViewById(R.id.tvLiveStatusHeader);
        tvLiveStatusDetail = findViewById(R.id.tvLiveStatusDetail);
        btnSeekerReportComplete = findViewById(R.id.btnSeekerReportComplete);
        btnEmployerConfirmComplete = findViewById(R.id.btnEmployerConfirmComplete);
        cardSessionCompletedNotice = findViewById(R.id.cardSessionCompletedNotice);
        tvSessionCompletedMessage = findViewById(R.id.tvSessionCompletedMessage);
        btnOpenWalletAfterComplete = findViewById(R.id.btnOpenWalletAfterComplete);

        cardCancellationPending = findViewById(R.id.cardCancellationPending);
        tvCancellationPendingTitle = findViewById(R.id.tvCancellationPendingTitle);
        tvCancellationPendingDetail = findViewById(R.id.tvCancellationPendingDetail);
        btnWithdrawCancelRequest = findViewById(R.id.btnWithdrawCancelRequest);
        layoutRecipientCancelActions = findViewById(R.id.layoutRecipientCancelActions);
        btnConfirmCancelSession = findViewById(R.id.btnConfirmCancelSession);
        btnRejectCancelRequest = findViewById(R.id.btnRejectCancelRequest);
        btnRequestCancelWhenReady = findViewById(R.id.btnRequestCancelWhenReady);
        cardSessionCancelledNotice = findViewById(R.id.cardSessionCancelledNotice);
        tvSessionCancelledMessage = findViewById(R.id.tvSessionCancelledMessage);
        btnOpenWalletAfterCancel = findViewById(R.id.btnOpenWalletAfterCancel);

        View ttsView = findViewById(R.id.shiftDetailTtsController);
        if (ttsView != null) {
            ttsHelper = new vn.edu.ueh.nhom4.grandmaassist.app.ui.accessibility.TtsControllerHelper(this, ttsView, "shift_agreement");
            ttsHelper.setCustomIdleLabel("Nghe thỏa thuận ca làm");
            ttsHelper.setTextSupplier(this::buildAgreementSpeechSummary);
        }

        btnRequestCancelWhenReady.setOnClickListener(v -> showRequestCancelDialog());
        btnWithdrawCancelRequest.setOnClickListener(v -> confirmWithdrawCancelRequest());
        btnConfirmCancelSession.setOnClickListener(v -> confirmAgreeCancelSession());
        btnRejectCancelRequest.setOnClickListener(v -> confirmRejectCancelRequest());
        btnOpenWalletAfterCancel.setOnClickListener(v -> {
            if (vm.currentSession != null) {
                boolean isSeeker = currentUid.equals(vm.currentSession.seekerId);
                WalletActivity.start(this, isSeeker ? "SEEKER" : "EMPLOYER");
            }
        });

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

        btnSeekerCheckIn.setOnClickListener(v -> confirmSeekerCheckIn());
        btnEmployerCheckIn.setOnClickListener(v -> confirmEmployerCheckIn());
        btnSeekerReportComplete.setOnClickListener(v -> confirmSeekerReportComplete());
        btnEmployerConfirmComplete.setOnClickListener(v -> confirmEmployerConfirmComplete());
        btnOpenWalletAfterComplete.setOnClickListener(v -> {
            if (vm.currentSession != null) {
                boolean isSeeker = currentUid.equals(vm.currentSession.seekerId);
                WalletActivity.start(this, isSeeker ? "SEEKER" : "EMPLOYER");
            }
        });

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
        vn.edu.ueh.nhom4.grandmaassist.app.notification.ShiftAlarmScheduler.scheduleReminders(this, session);

        AgreementSnapshot ag = session.agreement;
        renderBadge(tvBadge, session);
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
        View btnDirections = findViewById(R.id.shiftDetailBtnDirections);
        if (btnDirections != null) {
            final String navAddress = workAddr;
            final String navArea = area;
            if (!navAddress.isEmpty() || !navArea.isEmpty()) {
                btnDirections.setVisibility(View.VISIBLE);
                btnDirections.setOnClickListener(v -> vn.edu.ueh.nhom4.grandmaassist.app.util.MapNavigationHelper.openDirections(this, navAddress, navArea));
            } else {
                btnDirections.setVisibility(View.GONE);
            }
        }

        boolean isSeeker = currentUid.equals(session.seekerId);
        boolean isEmployer = currentUid.equals(session.employerId);

        // Role-based Topbar and Pay display
        if (isSeeker) {
            RoleNavigationBar.topbar(this, "SEEKER");
            layoutSeekerPay.setVisibility(View.VISIBLE);
            layoutEmployerPay.setVisibility(View.GONE);
            tvSeekerPayAmount.setText(formatMoney(ag.laborAmount));
        } else {
            RoleNavigationBar.topbar(this, "EMPLOYER");
            layoutSeekerPay.setVisibility(View.GONE);
            layoutEmployerPay.setVisibility(View.VISIBLE);
            tvWorkerPay.setText(formatMoney(ag.laborAmount));
            tvServiceFee.setText("+" + formatMoney(ag.serviceFee));
            tvTotalDue.setText(formatMoney(ag.totalDue));
        }        // State machine UI
        // Reset all dynamic sections first
        layoutSeekerActions.setVisibility(View.GONE);
        layoutEmployerActions.setVisibility(View.GONE);
        layoutEmployerFunding.setVisibility(View.GONE);
        cardSessionReadyNotice.setVisibility(View.GONE);
        layoutSeekerCheckIn.setVisibility(View.GONE);
        layoutEmployerCheckIn.setVisibility(View.GONE);
        cardSessionLiveStatus.setVisibility(View.GONE);
        cardSessionCompletedNotice.setVisibility(View.GONE);
        cardCancellationPending.setVisibility(View.GONE);
        btnRequestCancelWhenReady.setVisibility(View.GONE);
        cardSessionCancelledNotice.setVisibility(View.GONE);

        if (session.isProposed()) {
            layoutSeekerActions.setVisibility(isSeeker ? View.VISIBLE : View.GONE);
            layoutEmployerActions.setVisibility(isEmployer ? View.VISIBLE : View.GONE);
        } else if (session.isAwaitingFunds()) {
            if (isEmployer) {
                layoutEmployerFunding.setVisibility(View.VISIBLE);
                btnEmployerFund.setText("Cấp tiền ca làm (" + formatMoney(ag.totalDue) + ")");
                tvEmployerWalletInfo.setText("Số dư ví hiện tại: " + formatMoney(currentWalletBalance));
                btnEmployerFund.setOnClickListener(v -> handleEmployerFund(ag.totalDue));
            } else {
                cardSessionReadyNotice.setVisibility(View.VISIBLE);
                tvSessionReadyNotice.setText("✓ Cô/chú đã đồng ý nhận ca. Đang chờ Người thuê cấp tiền bảo đảm cho ca làm việc.");
            }
            // In AWAITING_FUNDS, both sides can cancel agreement without penalty
            btnRequestCancelWhenReady.setVisibility(View.VISIBLE);
            btnRequestCancelWhenReady.setText("Hủy thỏa thuận ca làm");
        } else if (session.isReady()) {
            cardSessionReadyNotice.setVisibility(View.VISIBLE);

            if (session.isCancellationRequested()) {
                // There is a pending cancel request
                cardCancellationPending.setVisibility(View.VISIBLE);
                btnRequestCancelWhenReady.setVisibility(View.GONE);
                layoutSeekerCheckIn.setVisibility(View.GONE);
                layoutEmployerCheckIn.setVisibility(View.GONE);

                boolean isRequester = currentUid.equals(session.cancellationRequestedBy);
                String roleRequester = "EMPLOYER".equals(session.cancelInitiatorRole) ? "Người thuê" : "Cô/chú người làm";
                String reason = session.cancellationReason != null && !session.cancellationReason.isEmpty() ? session.cancellationReason : "Bận việc đột xuất";

                if (isRequester) {
                    tvCancellationPendingTitle.setText("⚠️ Đang chờ đối tác đồng ý hủy ca");
                    tvCancellationPendingDetail.setText("Bạn đã gửi yêu cầu hủy ca làm việc này.\nLý do: " + reason + "\nĐang chờ đối tác xác nhận.");
                    btnWithdrawCancelRequest.setVisibility(View.VISIBLE);
                    layoutRecipientCancelActions.setVisibility(View.GONE);
                } else {
                    tvCancellationPendingTitle.setText("⚠️ Yêu cầu hủy ca từ " + roleRequester);
                    tvCancellationPendingDetail.setText(roleRequester + " đã gửi yêu cầu hủy ca làm việc này.\nLý do: " + reason + "\nNếu bạn đồng ý, toàn bộ tiền cọc bảo đảm (" + formatMoney(session.totalFunded) + ") sẽ được hoàn trả về ví Người thuê.");
                    btnWithdrawCancelRequest.setVisibility(View.GONE);
                    layoutRecipientCancelActions.setVisibility(View.VISIBLE);
                }
            } else {
                // Normal READY: show check-in and cancel button
                btnRequestCancelWhenReady.setVisibility(View.VISIBLE);
                btnRequestCancelWhenReady.setText("Yêu cầu hủy ca làm việc");

                if (isSeeker) {
                    tvSessionReadyNotice.setText("✓ Người thuê đã nạp đủ " + formatMoney(ag.totalDue) + " tiền cọc bảo chứng trên hệ thống. Cô/chú an tâm chuẩn bị đến ca làm đúng giờ.");
                    layoutSeekerCheckIn.setVisibility(View.VISIBLE);
                    if (session.isSeekerCheckedIn()) {
                        btnSeekerCheckIn.setVisibility(View.GONE);
                        tvSeekerCheckInNotice.setVisibility(View.VISIBLE);
                        String timeStr = session.seekerCheckedInAt != null ? formatTime(session.seekerCheckedInAt) : "";
                        tvSeekerCheckInNotice.setText("✓ Cô/chú đã điểm danh thành công lúc " + timeStr + ".\nĐang chờ Người thuê bấm xác nhận trên máy của họ để bắt đầu ca làm.");
                    } else {
                        btnSeekerCheckIn.setVisibility(View.VISIBLE);
                        btnSeekerCheckIn.setEnabled(true);
                        tvSeekerCheckInNotice.setVisibility(View.GONE);
                    }
                } else {
                    tvSessionReadyNotice.setText("✓ Đã tạm giữ cọc " + formatMoney(ag.totalDue) + " an toàn trên hệ thống. Ca làm việc đã sẵn sàng.");
                    layoutEmployerCheckIn.setVisibility(View.VISIBLE);
                    if (session.employerCheckedInAt != null) {
                        btnEmployerCheckIn.setVisibility(View.GONE);
                        tvEmployerCheckInNotice.setVisibility(View.VISIBLE);
                        String timeStr = session.employerCheckedInAt != null ? formatTime(session.employerCheckedInAt) : "";
                        tvEmployerCheckInNotice.setText("✓ Bạn đã xác nhận người làm đến lúc " + timeStr + ".\nĐang chờ cô/chú bấm điểm danh trên điện thoại của họ để bắt đầu ca làm.");
                    } else {
                        btnEmployerCheckIn.setVisibility(View.VISIBLE);
                        btnEmployerCheckIn.setEnabled(true);
                        tvEmployerCheckInNotice.setVisibility(View.GONE);
                    }
                }
            }
        } else if (session.isInProgress()) {
            cardSessionLiveStatus.setVisibility(View.VISIBLE);
            tvLiveStatusHeader.setText("🟢 Ca làm việc đang diễn ra");
            String startStr = session.startedAt != null ? formatDateTime(session.startedAt) : "Đã bắt đầu";
            tvLiveStatusDetail.setText("Ca làm đã được cả 2 bên xác nhận bắt đầu lúc " + startStr + ".");

            if (isSeeker) {
                btnSeekerReportComplete.setVisibility(View.VISIBLE);
                btnSeekerReportComplete.setEnabled(true);
                btnEmployerConfirmComplete.setVisibility(View.GONE);
            } else {
                btnSeekerReportComplete.setVisibility(View.GONE);
                btnEmployerConfirmComplete.setVisibility(View.VISIBLE);
                btnEmployerConfirmComplete.setEnabled(true);
            }
        } else if (session.isCompletionPending()) {
            cardSessionLiveStatus.setVisibility(View.VISIBLE);
            tvLiveStatusHeader.setText("⏳ Đang chờ xác nhận hoàn tất ca");
            if (isSeeker) {
                tvLiveStatusDetail.setText("Cô/chú đã báo hoàn thành công việc. Vui lòng chờ Người thuê kiểm tra và xác nhận để nhận tiền vào ví.");
                btnSeekerReportComplete.setVisibility(View.GONE);
                btnEmployerConfirmComplete.setVisibility(View.GONE);
            } else {
                tvLiveStatusDetail.setText("Người làm đã báo hoàn thành công việc. Hãy bấm nút bên dưới để xác nhận hoàn tất ca và giải phóng tiền công vào ví người làm.");
                btnSeekerReportComplete.setVisibility(View.GONE);
                btnEmployerConfirmComplete.setVisibility(View.VISIBLE);
                btnEmployerConfirmComplete.setEnabled(true);
            }
        } else if (session.isCompleted()) {
            cardSessionCompletedNotice.setVisibility(View.VISIBLE);
            if (isSeeker) {
                tvSessionCompletedMessage.setText("Tiền công " + formatMoney(ag.laborAmount) + " đã được cộng vào Ví tiền của cô/chú. Cô/chú có thể mở ví để kiểm tra hoặc rút tiền về tài khoản ngân hàng.");
                vm.syncSettlementToWallet(currentUid);
            } else {
                tvSessionCompletedMessage.setText("Đã hoàn tất thanh toán tiền công " + formatMoney(ag.laborAmount) + " cho người làm và phí nền tảng " + formatMoney(ag.serviceFee) + " thành công.");
            }
        } else if (session.isCancelled()) {
            cardSessionCancelledNotice.setVisibility(View.VISIBLE);
            String reason = session.cancellationReason != null && !session.cancellationReason.isEmpty() ? session.cancellationReason : "Không có lý do cụ thể";
            if (session.totalFunded > 0) {
                if (isEmployer) {
                    tvSessionCancelledMessage.setText("Ca làm việc đã được hủy thành công.\nLý do: " + reason + "\n\n✓ Toàn bộ tiền cọc bảo đảm (" + formatMoney(session.totalFunded) + ") đã được hoàn trả lại vào Ví tiền của bạn.");
                    btnOpenWalletAfterCancel.setVisibility(View.VISIBLE);
                    vm.syncRefundToEmployerWallet(currentUid);
                } else {
                    tvSessionCancelledMessage.setText("Ca làm việc đã được hủy theo thỏa thuận giữa hai bên.\nLý do: " + reason);
                    btnOpenWalletAfterCancel.setVisibility(View.GONE);
                }
            } else {
                tvSessionCancelledMessage.setText("Ca làm việc đã được hủy trước khi cấp tiền bảo chứng.\nLý do: " + reason);
                btnOpenWalletAfterCancel.setVisibility(View.GONE);
            }
        }
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
            badge.setBackgroundColor(Color.parseColor("#EEEEEE"));
            badge.setTextColor(Color.parseColor("#616161"));
        } else {
            badge.setText(WorkSession.statusLabel(status));
            badge.setBackgroundColor(Color.parseColor("#EEEEEE"));
            badge.setTextColor(Color.parseColor("#616161"));
        }
    }

    private void confirmSeekerCheckIn() {
        if (vm.currentSession == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Điểm danh ca làm việc")
                .setMessage("Cô/chú xác nhận đã đến nơi làm việc và sẵn sàng bắt đầu ca?")
                .setPositiveButton("Xác nhận điểm danh", (d, w) -> {
                    btnSeekerCheckIn.setEnabled(false);
                    vm.confirmCheckIn(vm.currentSession.id, "SEEKER", () -> {
                        Toast.makeText(this, "Điểm danh thành công!", Toast.LENGTH_SHORT).show();
                        reload();
                    }, err -> {
                        btnSeekerCheckIn.setEnabled(true);
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmEmployerCheckIn() {
        if (vm.currentSession == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận người làm đã đến")
                .setMessage("Bạn xác nhận người làm đã đến địa điểm làm việc?")
                .setPositiveButton("Xác nhận", (d, w) -> {
                    btnEmployerCheckIn.setEnabled(false);
                    vm.confirmCheckIn(vm.currentSession.id, "EMPLOYER", () -> {
                        Toast.makeText(this, "Đã xác nhận người làm đến nơi!", Toast.LENGTH_SHORT).show();
                        reload();
                    }, err -> {
                        btnEmployerCheckIn.setEnabled(true);
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmSeekerReportComplete() {
        if (vm.currentSession == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Báo hoàn thành ca làm")
                .setMessage("Cô/chú đã hoàn thành các nhiệm vụ được giao trong ca làm này?")
                .setPositiveButton("Báo hoàn thành", (d, w) -> {
                    btnSeekerReportComplete.setEnabled(false);
                    vm.reportCompletion(vm.currentSession.id, () -> {
                        Toast.makeText(this, "Đã gửi báo hoàn thành! Đang chờ Người thuê xác nhận.", Toast.LENGTH_LONG).show();
                        reload();
                    }, err -> {
                        btnSeekerReportComplete.setEnabled(true);
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton("Chưa xong", null)
                .show();
    }

    private void confirmEmployerConfirmComplete() {
        if (vm.currentSession == null || vm.currentSession.agreement == null) return;
        long pay = vm.currentSession.agreement.laborAmount;
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận hoàn tất ca & Quyết toán")
                .setMessage("Hệ thống sẽ chuyển " + formatMoney(pay) + " từ tiền cọc tạm giữ vào Ví tiền của người làm và kết thúc ca. Bạn có chắc chắn muốn xác nhận?")
                .setPositiveButton("Xác nhận & Quyết toán", (d, w) -> {
                    btnEmployerConfirmComplete.setEnabled(false);
                    vm.confirmCompletionAndSettle(vm.currentSession.id, () -> {
                        Toast.makeText(this, "Đã hoàn tất ca và quyết toán tiền công thành công!", Toast.LENGTH_LONG).show();
                        vn.edu.ueh.nhom4.grandmaassist.app.notification.ShiftAlarmScheduler.notifySessionMilestone(
                                this,
                                vm.currentSession.id,
                                "✅ Ca làm đã hoàn tất & quyết toán",
                                "Ca làm việc đã kết thúc tốt đẹp. Tiền công đã được quyết toán chuyển về ví người làm!"
                        );
                        reload();
                    }, err -> {
                        btnEmployerConfirmComplete.setEnabled(true);
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void confirmSeekerAccept() {
        if (vm.currentSession == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.session_accept_dialog_title)
                .setMessage(R.string.session_accept_dialog_msg)
                .setPositiveButton(R.string.session_btn_accept, (d, w) -> {
                    vm.seekerAccept(vm.currentSession.id, vm.currentSession.agreementVersion, () -> {
                        Toast.makeText(this, "Đã đồng ý nhận ca làm thành công!", Toast.LENGTH_SHORT).show();
                        vn.edu.ueh.nhom4.grandmaassist.app.notification.ShiftAlarmScheduler.notifySessionMilestone(
                                this,
                                vm.currentSession.id,
                                "🤝 Đã đồng ý nhận ca làm",
                                "Cô/chú đã đồng ý nhận ca làm việc này. Đang chờ Người thuê ký quỹ cọc bảo đảm."
                        );
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

    private String formatTime(Timestamp ts) {
        if (ts == null) return "";
        return new SimpleDateFormat("HH:mm", new Locale("vi", "VN")).format(ts.toDate());
    }

    private String formatDateTime(Timestamp ts) {
        if (ts == null) return "";
        return new SimpleDateFormat("HH:mm, dd/MM/yyyy", new Locale("vi", "VN")).format(ts.toDate());
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
                            vn.edu.ueh.nhom4.grandmaassist.app.notification.ShiftAlarmScheduler.notifySessionMilestone(
                                    this,
                                    vm.currentSession.id,
                                    "🎉 Ca làm việc đã được ký quỹ!",
                                    "Ca làm việc đã được bảo đảm ký quỹ an toàn. Hệ thống sẽ tự động nhắc nhở trước giờ làm."
                            );
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

    private void showRequestCancelDialog() {
        if (vm.currentSession == null) return;
        boolean isSeeker = currentUid.equals(vm.currentSession.seekerId);
        String status = vm.currentSession.status;

        if (WorkSession.STATUS_PROPOSED.equals(status) || WorkSession.STATUS_AWAITING_FUNDS.equals(status)) {
            new AlertDialog.Builder(this)
                    .setTitle("Hủy thỏa thuận ca làm?")
                    .setMessage("Bạn có chắc muốn hủy ca làm này không? Ca làm sẽ được hủy ngay vì chưa có tiền bảo đảm.")
                    .setPositiveButton("Hủy ca làm", (d, w) -> {
                        vm.requestCancelSession(vm.currentSession.id, "Hủy thỏa thuận trước khi cấp tiền", isSeeker ? "SEEKER" : "EMPLOYER", () -> {
                            Toast.makeText(this, "Đã hủy ca làm việc.", Toast.LENGTH_SHORT).show();
                        }, err -> {
                            Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                        });
                    })
                    .setNegativeButton("Quay lại", null)
                    .show();
            return;
        }

        final android.widget.EditText input = new android.widget.EditText(this);
        input.setHint("Nhập lý do hủy (vd: bận việc đột xuất...)");
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        android.widget.FrameLayout container = new android.widget.FrameLayout(this);
        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT);
        params.leftMargin = pad;
        params.rightMargin = pad;
        input.setLayoutParams(params);
        container.addView(input);

        new AlertDialog.Builder(this)
                .setTitle("Yêu cầu hủy ca làm việc")
                .setMessage("Sau khi gửi, đối tác sẽ nhận được thông báo yêu cầu hủy. Nếu cả hai bên đồng ý, tiền cọc bảo đảm sẽ được hoàn trả về ví người thuê.\n\nNhập lý do hủy ca:")
                .setView(container)
                .setPositiveButton("Gửi yêu cầu", (d, w) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) reason = "Bận việc đột xuất";
                    vm.requestCancelSession(vm.currentSession.id, reason, isSeeker ? "SEEKER" : "EMPLOYER", () -> {
                        Toast.makeText(this, "Đã gửi yêu cầu hủy ca làm việc.", Toast.LENGTH_SHORT).show();
                    }, err -> {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    private void confirmWithdrawCancelRequest() {
        if (vm.currentSession == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Rút lại yêu cầu hủy?")
                .setMessage("Bạn có muốn rút lại yêu cầu hủy ca không? Ca làm việc sẽ tiếp tục trạng thái Sẵn sàng.")
                .setPositiveButton("Rút lại yêu cầu", (d, w) -> {
                    vm.withdrawCancelRequest(vm.currentSession.id, () -> {
                        Toast.makeText(this, "Đã rút lại yêu cầu hủy ca.", Toast.LENGTH_SHORT).show();
                    }, err -> {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton("Giữ yêu cầu", null)
                .show();
    }

    private void confirmAgreeCancelSession() {
        if (vm.currentSession == null) return;
        boolean isSeeker = currentUid.equals(vm.currentSession.seekerId);
        new AlertDialog.Builder(this)
                .setTitle("Đồng ý hủy ca làm việc?")
                .setMessage("Sau khi đồng ý, ca làm việc sẽ bị hủy. Toàn bộ tiền cọc bảo đảm (" + formatMoney(vm.currentSession.totalFunded) + ") sẽ được hoàn trả 100% về ví của Người thuê.")
                .setPositiveButton("Xác nhận hủy & Hoàn cọc", (d, w) -> {
                    vm.confirmCancelSession(vm.currentSession.id, isSeeker ? "SEEKER" : "EMPLOYER", () -> {
                        Toast.makeText(this, "Đã xác nhận hủy ca làm và hoàn cọc.", Toast.LENGTH_LONG).show();
                    }, err -> {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

    private String buildAgreementSpeechSummary() {
        if (vm == null || vm.currentSession == null || vm.currentSession.agreement == null) return "";
        WorkSession session = vm.currentSession;
        AgreementSnapshot ag = session.agreement;

        StringBuilder sb = new StringBuilder();
        sb.append("Chi tiết thỏa thuận ca làm việc. ");
        if (tvBadge != null && tvBadge.getText() != null) {
            sb.append("Trạng thái: ").append(tvBadge.getText()).append(". ");
        }
        if (ag.tasks != null && !ag.tasks.trim().isEmpty()) {
            sb.append("Công việc cần làm: ").append(ag.tasks.trim()).append(". ");
        }
        if (tvSchedule != null && tvSchedule.getText() != null && tvSchedule.getText().length() > 0) {
            sb.append("Thời gian làm việc: ").append(tvSchedule.getText().toString().replace("\n", ", ")).append(". ");
        }
        if (tvAddress != null && tvAddress.getText() != null && tvAddress.getText().length() > 0) {
            sb.append("Địa điểm làm việc: ").append(tvAddress.getText().toString().replace("\n", ", ")).append(". ");
        }
        boolean isSeeker = currentUid.equals(session.seekerId);
        if (isSeeker) {
            sb.append("Tiền công bác thực nhận trọn vẹn là: ").append(formatMoney(ag.laborAmount)).append(". ");
        } else {
            sb.append("Tiền công người làm: ").append(formatMoney(ag.laborAmount))
              .append(". Phí dịch vụ: ").append(formatMoney(ag.serviceFee))
              .append(". Tổng thanh toán: ").append(formatMoney(ag.totalDue)).append(". ");
        }
        return sb.toString();
    }

    private void confirmRejectCancelRequest() {
        if (vm.currentSession == null) return;
        new AlertDialog.Builder(this)
                .setTitle("Từ chối yêu cầu hủy ca?")
                .setMessage("Bạn có chắc muốn từ chối yêu cầu hủy ca không? Ca làm việc sẽ tiếp tục trạng thái Sẵn sàng.")
                .setPositiveButton("Từ chối hủy", (d, w) -> {
                    vm.rejectCancelRequest(vm.currentSession.id, () -> {
                        Toast.makeText(this, "Đã từ chối yêu cầu hủy ca.", Toast.LENGTH_SHORT).show();
                    }, err -> {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton("Quay lại", null)
                .show();
    }

}
