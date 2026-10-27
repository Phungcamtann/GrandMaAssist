package vn.edu.ueh.nhom4.grandmaassist.app.ui.applications;

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
import java.text.NumberFormat;
import java.util.Date;
import java.util.Locale;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.Application;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.JobSnapshot;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.PublicJobsActivity;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.ApplicationViewModel;

public final class MyApplicationsActivity extends AppCompatActivity {
    private ApplicationViewModel vm;
    private RoleNavigationBar nav;
    private String uid = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_my_applications);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.myAppRoot), (v, i) -> {
            androidx.core.graphics.Insets safe = i.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime()
            );
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return i;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : "";
        if (uid.isEmpty()) {
            Toast.makeText(this, R.string.sj_login_prompt, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        RoleNavigationBar.topbar(this, "SEEKER");
        nav = new RoleNavigationBar(this, a -> {
            if (a == RoleNavigationBar.Action.WORK) return;
            if (a == RoleNavigationBar.Action.OTHER) {
                Intent intent = new Intent(this, PublicJobsActivity.class);
                intent.putExtra(PublicJobsActivity.ROLE, "SEEKER");
                startActivity(intent);
                finish();
                return;
            }
            if (a == RoleNavigationBar.Action.HOME || a == RoleNavigationBar.Action.PROFILE || a == RoleNavigationBar.Action.ACCOUNT) {
                leave(a.name());
            }
        });

        vm = new ViewModelProvider(this).get(ApplicationViewModel.class);

        findViewById(R.id.myAppBack).setOnClickListener(v -> finish());
        findViewById(R.id.myAppReload).setOnClickListener(v -> vm.loadApplications(true));
        findViewById(R.id.myAppBtnBrowse).setOnClickListener(v -> {
            Intent intent = new Intent(this, PublicJobsActivity.class);
            intent.putExtra(PublicJobsActivity.ROLE, "SEEKER");
            startActivity(intent);
            finish();
        });

        setupTabs();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                finish();
            }
        });

        vm.changes.observe(this, n -> render());
        vm.loadApplications(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (vm != null && !uid.isEmpty() && !vm.busy) {
            vm.loadApplications(false);
        }
    }

    private void leave(String route) {
        setResult(RESULT_OK, new Intent().putExtra(RoleNavigationBar.RETURN_DESTINATION, route));
        finish();
    }

    private void setupTabs() {
        findViewById(R.id.myAppTabAll).setOnClickListener(v -> vm.setTab(ApplicationViewModel.TAB_ALL));
        findViewById(R.id.myAppTabPending).setOnClickListener(v -> vm.setTab(ApplicationViewModel.TAB_PENDING));
        findViewById(R.id.myAppTabResolved).setOnClickListener(v -> vm.setTab(ApplicationViewModel.TAB_RESOLVED));
    }

    private void render() {
        ProgressBar pb = findViewById(R.id.myAppProgress);
        pb.setVisibility(vm.busy ? View.VISIBLE : View.GONE);

        TextView tvMsg = findViewById(R.id.myAppMessage);
        if (vm.message != null && !vm.message.isEmpty()) {
            tvMsg.setText(vm.message);
            tvMsg.setVisibility(View.VISIBLE);
        } else {
            tvMsg.setVisibility(View.GONE);
        }

        int countAll = vm.items.size();
        int countPending = 0;
        int countResolved = 0;
        for (Application a : vm.items) {
            if (a.isPending()) countPending++;
            else countResolved++;
        }

        AppCompatButton tabAll = findViewById(R.id.myAppTabAll);
        AppCompatButton tabPending = findViewById(R.id.myAppTabPending);
        AppCompatButton tabResolved = findViewById(R.id.myAppTabResolved);

        tabAll.setText(getString(R.string.sj_tab_all, countAll));
        tabPending.setText(getString(R.string.app_status_pending) + " (" + countPending + ")");
        tabResolved.setText("Đã xử lý (" + countResolved + ")");

        updateTabStyle(tabAll, vm.currentTab == ApplicationViewModel.TAB_ALL);
        updateTabStyle(tabPending, vm.currentTab == ApplicationViewModel.TAB_PENDING);
        updateTabStyle(tabResolved, vm.currentTab == ApplicationViewModel.TAB_RESOLVED);

        LinearLayout listBody = findViewById(R.id.myAppListBody);
        listBody.removeAllViews();
        View emptyView = findViewById(R.id.myAppEmptyLayout);

        if (!vm.busy && vm.displayList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
        } else {
            emptyView.setVisibility(View.GONE);
            for (Application app : vm.displayList) {
                listBody.addView(createApplicationCard(app));
            }
        }

        WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(findViewById(R.id.myAppRoot));
        boolean keyboard = insets != null && insets.isVisible(WindowInsetsCompat.Type.ime());
        nav.render("SEEKER", RoleNavigationBar.Action.WORK, !keyboard, !vm.busy);
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

    private View createApplicationCard(Application app) {
        LinearLayout listBody = findViewById(R.id.myAppListBody);
        View card = getLayoutInflater().inflate(R.layout.item_my_application, listBody, false);

        TextView title = card.findViewById(R.id.appItemTitle);
        TextView badge = card.findViewById(R.id.appItemBadge);
        TextView categoryArea = card.findViewById(R.id.appItemCategoryArea);
        TextView pay = card.findViewById(R.id.appItemPay);
        TextView date = card.findViewById(R.id.appItemDate);
        TextView message = card.findViewById(R.id.appItemMessage);
        View divider = card.findViewById(R.id.appItemDivider);
        View actionSection = card.findViewById(R.id.appItemActionSection);
        AppCompatButton btnWithdraw = card.findViewById(R.id.appItemBtnWithdraw);

        JobSnapshot job = app.jobSnapshot;
        String jobTitle = job != null && job.title != null && !job.title.isEmpty() ? job.title : ("Công việc #" + app.jobId);
        title.setText(jobTitle);

        renderBadge(badge, app.status);

        if (job != null) {
            String cat = job.categoryName != null ? job.categoryName : "";
            String ar = job.areaName != null ? job.areaName : "";
            categoryArea.setText(cat.isEmpty() ? ar : (cat + " · " + ar));
            String formattedPay = NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(job.payAmount) + " đ";
            pay.setText(formattedPay);
        } else {
            categoryArea.setText("Tin tuyển dụng #" + app.jobId);
            pay.setText("");
        }

        if (app.createdAt != null) {
            String dateStr = DateFormat.getDateInstance(DateFormat.SHORT, new Locale("vi", "VN")).format(app.createdAt.toDate());
            date.setText("Đã nộp ngày: " + dateStr);
        } else {
            date.setText("");
        }

        if (app.message != null && !app.message.trim().isEmpty()) {
            message.setVisibility(View.VISIBLE);
            message.setText("Lời nhắn: " + app.message.trim());
        } else {
            message.setVisibility(View.GONE);
        }

        View empSection = card.findViewById(R.id.appItemEmployerContactSection);
        TextView empName = card.findViewById(R.id.appItemEmployerName);
        TextView empPhone = card.findViewById(R.id.appItemEmployerPhone);
        AppCompatButton btnCallEmp = card.findViewById(R.id.appItemBtnCallEmployer);

        if (app.isAccepted() && app.employerContactSnapshot != null) {
            empSection.setVisibility(View.VISIBLE);
            String empDisplayName = app.employerContactSnapshot.displayName;
            empName.setText(empDisplayName != null && !empDisplayName.trim().isEmpty() ? empDisplayName : "Người tuyển dụng");
            final String rawPhone = app.employerContactSnapshot.phoneE164;
            if (rawPhone != null && !rawPhone.trim().isEmpty()) {
                empPhone.setText(rawPhone);
                btnCallEmp.setVisibility(View.VISIBLE);
                btnCallEmp.setOnClickListener(v -> dialPhone(rawPhone));
            } else {
                empPhone.setText("Chưa cập nhật số điện thoại");
                btnCallEmp.setVisibility(View.GONE);
            }
        } else {
            empSection.setVisibility(View.GONE);
        }

        if (app.isPending()) {
            divider.setVisibility(View.VISIBLE);
            actionSection.setVisibility(View.VISIBLE);
            btnWithdraw.setOnClickListener(v -> confirmWithdraw(app));
        } else {
            divider.setVisibility(View.GONE);
            actionSection.setVisibility(View.GONE);
        }

        card.setOnClickListener(v -> {
            Intent intent = new Intent(this, PublicJobsActivity.class);
            intent.putExtra(PublicJobsActivity.ROLE, "SEEKER");
            intent.putExtra("detail", app.jobId);
            startActivity(intent);
        });

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

    private void confirmWithdraw(Application app) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.app_withdraw_dialog_title)
                .setMessage(R.string.app_withdraw_dialog_message)
                .setPositiveButton(R.string.app_withdraw_confirm, (d, w) -> {
                    vm.withdraw(app.id, () -> {
                        Toast.makeText(this, R.string.app_withdrawn_success, Toast.LENGTH_SHORT).show();
                    }, err -> {
                        Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                    });
                })
                .setNegativeButton(R.string.app_withdraw_cancel, null)
                .show();
    }

    private void dialPhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) return;
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + phone.trim()));
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, R.string.applicant_call_error, Toast.LENGTH_SHORT).show();
        }
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
