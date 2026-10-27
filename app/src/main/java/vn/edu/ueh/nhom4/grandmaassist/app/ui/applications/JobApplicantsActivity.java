package vn.edu.ueh.nhom4.grandmaassist.app.ui.applications;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.ApplicantSnapshot;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.Application;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.WorkSession;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.sessions.ShiftAgreementDetailActivity;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.sessions.ShiftAgreementEditActivity;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.JobApplicantsViewModel;

public final class JobApplicantsActivity extends AppCompatActivity {
    public static final String EXTRA_JOB_ID = "job_id";
    public static final String EXTRA_JOB_TITLE = "job_title";

    private JobApplicantsViewModel vm;
    private RoleNavigationBar nav;
    private String jobId = "";
    private String jobTitle = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_job_applicants);

        jobId = getIntent().getStringExtra(EXTRA_JOB_ID);
        jobTitle = getIntent().getStringExtra(EXTRA_JOB_TITLE);
        if (jobId == null || jobId.trim().isEmpty()) {
            Toast.makeText(this, "Không tìm thấy mã tin tuyển dụng.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.jobApplicantsRoot), (v, i) -> {
            androidx.core.graphics.Insets safe = i.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime()
            );
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return i;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            Toast.makeText(this, R.string.sj_login_prompt, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        RoleNavigationBar.topbar(this, "EMPLOYER");
        nav = new RoleNavigationBar(this, a -> {
            if (a == RoleNavigationBar.Action.WORK) {
                finish();
                return;
            }
            if (a == RoleNavigationBar.Action.HOME || a == RoleNavigationBar.Action.PROFILE || a == RoleNavigationBar.Action.ACCOUNT) {
                leave(a.name());
            }
        });

        TextView tvSubtitle = findViewById(R.id.applicantsJobSubtitle);
        if (jobTitle != null && !jobTitle.trim().isEmpty()) {
            tvSubtitle.setText(jobTitle.trim());
            tvSubtitle.setVisibility(View.VISIBLE);
        } else {
            tvSubtitle.setVisibility(View.GONE);
        }

        vm = new ViewModelProvider(this).get(JobApplicantsViewModel.class);

        findViewById(R.id.applicantsBack).setOnClickListener(v -> finish());
        findViewById(R.id.applicantsReload).setOnClickListener(v -> vm.loadApplicants(jobId, true));

        setupTabs();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        vm.changes.observe(this, n -> render());
        vm.loadApplicants(jobId, false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vm != null && !jobId.isEmpty() && !vm.busy) {
            vm.loadApplicants(jobId, false);
        }
    }

    private void leave(String route) {
        setResult(RESULT_OK, new Intent().putExtra(RoleNavigationBar.RETURN_DESTINATION, route));
        finish();
    }

    private void setupTabs() {
        findViewById(R.id.applicantsTabAll).setOnClickListener(v -> vm.setTab(JobApplicantsViewModel.TAB_ALL));
        findViewById(R.id.applicantsTabPending).setOnClickListener(v -> vm.setTab(JobApplicantsViewModel.TAB_PENDING));
        findViewById(R.id.applicantsTabAccepted).setOnClickListener(v -> vm.setTab(JobApplicantsViewModel.TAB_ACCEPTED));
        findViewById(R.id.applicantsTabRejected).setOnClickListener(v -> vm.setTab(JobApplicantsViewModel.TAB_REJECTED));
    }

    private void render() {
        ProgressBar pb = findViewById(R.id.applicantsProgress);
        pb.setVisibility(vm.busy ? View.VISIBLE : View.GONE);

        TextView tvMsg = findViewById(R.id.applicantsMessage);
        if (vm.message != null && !vm.message.isEmpty()) {
            tvMsg.setText(vm.message);
            tvMsg.setVisibility(View.VISIBLE);
        } else {
            tvMsg.setVisibility(View.GONE);
        }

        int countAll = vm.items.size();
        int countPending = 0;
        int countAccepted = 0;
        int countRejected = 0;
        for (Application a : vm.items) {
            if (a.isPending()) countPending++;
            else if (a.isAccepted()) countAccepted++;
            else if (a.isRejected()) countRejected++;
        }

        AppCompatButton tabAll = findViewById(R.id.applicantsTabAll);
        AppCompatButton tabPending = findViewById(R.id.applicantsTabPending);
        AppCompatButton tabAccepted = findViewById(R.id.applicantsTabAccepted);
        AppCompatButton tabRejected = findViewById(R.id.applicantsTabRejected);

        tabAll.setText(getString(R.string.applicant_tab_all, countAll));
        tabPending.setText(getString(R.string.applicant_tab_pending, countPending));
        tabAccepted.setText(getString(R.string.applicant_tab_accepted, countAccepted));
        tabRejected.setText(getString(R.string.applicant_tab_rejected, countRejected));

        updateTabStyle(tabAll, vm.currentTab == JobApplicantsViewModel.TAB_ALL);
        updateTabStyle(tabPending, vm.currentTab == JobApplicantsViewModel.TAB_PENDING);
        updateTabStyle(tabAccepted, vm.currentTab == JobApplicantsViewModel.TAB_ACCEPTED);
        updateTabStyle(tabRejected, vm.currentTab == JobApplicantsViewModel.TAB_REJECTED);

        LinearLayout listBody = findViewById(R.id.applicantsListBody);
        listBody.removeAllViews();
        View emptyView = findViewById(R.id.applicantsEmptyLayout);

        if (!vm.busy && vm.displayList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
            for (Application app : vm.displayList) {
                listBody.addView(createApplicantCard(app));
            }
        }

        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(findViewById(R.id.jobApplicantsRoot));
        boolean keyboard = insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
        nav.render("EMPLOYER", RoleNavigationBar.Action.WORK, !keyboard, !vm.busy);
    }

    private void updateTabStyle(AppCompatButton btn, boolean selected) {
        if (selected) {
            btn.setBackgroundResource(R.drawable.button_primary);
            btn.setTextColor(Color.WHITE);
            btn.setTypeface(null, Typeface.BOLD);
        } else {
            btn.setBackgroundResource(R.drawable.button_secondary);
            btn.setTextColor(ContextCompat.getColor(this, R.color.brand_green));
            btn.setTypeface(null, Typeface.NORMAL);
        }
    }

    private View createApplicantCard(Application app) {
        LinearLayout listBody = findViewById(R.id.applicantsListBody);
        View card = getLayoutInflater().inflate(R.layout.item_job_applicant, listBody, false);

        TextView tvName = card.findViewById(R.id.applicantItemName);
        TextView tvBadge = card.findViewById(R.id.applicantItemBadge);
        TextView tvDate = card.findViewById(R.id.applicantItemDate);
        TextView tvPhone = card.findViewById(R.id.applicantItemPhone);
        AppCompatButton btnCall = card.findViewById(R.id.applicantItemBtnCall);
        TextView tvSkills = card.findViewById(R.id.applicantItemSkills);
        TextView tvExp = card.findViewById(R.id.applicantItemExperience);
        View noteSection = card.findViewById(R.id.applicantNoteSection);
        TextView tvNote = card.findViewById(R.id.applicantItemMessage);
        View divider = card.findViewById(R.id.applicantItemDivider);
        View actionSection = card.findViewById(R.id.applicantItemActionSection);
        AppCompatButton btnReject = card.findViewById(R.id.applicantItemBtnReject);
        AppCompatButton btnAccept = card.findViewById(R.id.applicantItemBtnAccept);

        ApplicantSnapshot seeker = app.applicantSnapshot;
        String name = seeker != null && seeker.displayName != null && !seeker.displayName.trim().isEmpty()
                ? seeker.displayName.trim()
                : "Người tìm việc";
        tvName.setText(name);

        renderBadge(tvBadge, app.status);

        if (app.createdAt != null) {
            String dateStr = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, new Locale("vi", "VN"))
                    .format(app.createdAt.toDate());
            tvDate.setText("Nộp đơn lúc: " + dateStr);
        } else {
            tvDate.setText("");
        }

        if (app.sharePhoneConsent && seeker != null && seeker.phoneE164 != null && !seeker.phoneE164.trim().isEmpty()) {
            final String phone = seeker.phoneE164.trim();
            tvPhone.setText(phone);
            btnCall.setVisibility(View.VISIBLE);
            btnCall.setOnClickListener(v -> dialPhone(phone));
        } else {
            tvPhone.setText(R.string.applicant_phone_hidden);
            btnCall.setVisibility(View.GONE);
        }

        if (seeker != null && seeker.skills != null && !seeker.skills.isEmpty()) {
            tvSkills.setText(String.join(", ", seeker.skills));
        } else {
            tvSkills.setText("Chưa cập nhật kỹ năng");
        }

        if (seeker != null && seeker.experienceSummary != null && !seeker.experienceSummary.trim().isEmpty()) {
            tvExp.setText(seeker.experienceSummary.trim());
        } else {
            tvExp.setText("Chưa cập nhật kinh nghiệm");
        }

        if (app.message != null && !app.message.trim().isEmpty()) {
            noteSection.setVisibility(View.VISIBLE);
            tvNote.setText(app.message.trim());
        } else {
            noteSection.setVisibility(View.GONE);
        }

        View acceptedSection = card.findViewById(R.id.applicantItemAcceptedSection);
        AppCompatButton btnShiftAgreement = card.findViewById(R.id.applicantItemBtnShiftAgreement);

        if (app.isPending()) {
            divider.setVisibility(View.VISIBLE);
            actionSection.setVisibility(View.VISIBLE);
            acceptedSection.setVisibility(View.GONE);
            btnReject.setOnClickListener(v -> confirmReject(app));
            btnAccept.setOnClickListener(v -> confirmAccept(app));
        } else if (app.isAccepted()) {
            divider.setVisibility(View.GONE);
            actionSection.setVisibility(View.GONE);
            acceptedSection.setVisibility(View.VISIBLE);
            WorkSession ws = vm.sessionMap.get(app.id);
            if (ws != null) {
                btnShiftAgreement.setText(R.string.session_btn_view_agreement);
                btnShiftAgreement.setOnClickListener(v -> {
                    Intent intent = new Intent(this, ShiftAgreementDetailActivity.class);
                    intent.putExtra(ShiftAgreementDetailActivity.EXTRA_SESSION_ID, ws.id);
                    intent.putExtra(ShiftAgreementDetailActivity.EXTRA_APPLICATION_ID, app.id);
                    startActivity(intent);
                });
            } else {
                btnShiftAgreement.setText(R.string.session_btn_create_agreement);
                btnShiftAgreement.setOnClickListener(v -> openShiftAgreement(app));
            }
        } else {
            divider.setVisibility(View.GONE);
            actionSection.setVisibility(View.GONE);
            acceptedSection.setVisibility(View.GONE);
        }

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
        );
        lp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(lp);
        return card;
    }

    private void renderBadge(TextView badge, String status) {
        if ("PENDING".equals(status)) {
            badge.setText(R.string.app_status_pending);
            badge.setBackgroundColor(Color.parseColor("#FFF3E0"));
            badge.setTextColor(Color.parseColor("#E65100"));
        } else if ("ACCEPTED".equals(status)) {
            badge.setText(R.string.app_status_accepted);
            badge.setBackgroundColor(Color.parseColor("#E8F5E9"));
            badge.setTextColor(Color.parseColor("#1C6758"));
        } else if ("REJECTED".equals(status)) {
            badge.setText(R.string.app_status_rejected);
            badge.setBackgroundColor(Color.parseColor("#FFEBEE"));
            badge.setTextColor(Color.parseColor("#C62828"));
        } else if ("WITHDRAWN".equals(status)) {
            badge.setText(R.string.app_status_withdrawn);
            badge.setBackgroundColor(Color.parseColor("#EEEEEE"));
            badge.setTextColor(Color.parseColor("#616161"));
        } else {
            badge.setText(status != null ? status : "");
            badge.setBackgroundColor(Color.parseColor("#EEEEEE"));
            badge.setTextColor(Color.parseColor("#616161"));
        }
    }

    private void dialPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone.trim()));
            startActivity(intent);
        } catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, R.string.applicant_call_error, Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmAccept(Application app) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.applicant_accept_dialog_title)
                .setMessage(R.string.applicant_accept_dialog_msg)
                .setPositiveButton(R.string.applicant_accept_confirm, (d, w) -> {
                    vm.accept(app.id, () -> {
                        Toast.makeText(this, R.string.applicant_accept_success, Toast.LENGTH_SHORT).show();
                    }, err -> {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton(R.string.applicant_action_cancel, null)
                .show();
    }

    private void confirmReject(Application app) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.applicant_reject_dialog_title)
                .setMessage(R.string.applicant_reject_dialog_msg)
                .setPositiveButton(R.string.applicant_reject_confirm, (d, w) -> {
                    vm.reject(app.id, () -> {
                        Toast.makeText(this, R.string.applicant_reject_success, Toast.LENGTH_SHORT).show();
                    }, err -> {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton(R.string.applicant_action_cancel, null)
                .show();
    }

    private void openShiftAgreement(Application app) {
        Intent intent = new Intent(this, ShiftAgreementEditActivity.class);
        intent.putExtra(ShiftAgreementEditActivity.EXTRA_APPLICATION_ID, app.id);
        if (app.jobSnapshot != null) {
            intent.putExtra(ShiftAgreementEditActivity.EXTRA_INITIAL_TASK, app.jobSnapshot.title);
            intent.putExtra(ShiftAgreementEditActivity.EXTRA_INITIAL_ADDRESS, app.jobSnapshot.workAddress);
            intent.putExtra(ShiftAgreementEditActivity.EXTRA_INITIAL_AREA_NAME, app.jobSnapshot.areaName);
            intent.putExtra(ShiftAgreementEditActivity.EXTRA_INITIAL_AREA_CODE, app.jobSnapshot.areaCode);
            intent.putExtra(ShiftAgreementEditActivity.EXTRA_INITIAL_PAY, app.jobSnapshot.payAmount);
            intent.putExtra(ShiftAgreementEditActivity.EXTRA_INITIAL_SCHEDULE, app.jobSnapshot.scheduleText);
        }
        startActivity(intent);
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
