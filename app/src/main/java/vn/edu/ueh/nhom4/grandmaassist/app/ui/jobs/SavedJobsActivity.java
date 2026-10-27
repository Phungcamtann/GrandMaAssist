package vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
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
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.PublicJob;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.SavedJob;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.SavedJobsViewModel;

public final class SavedJobsActivity extends AppCompatActivity {
    private SavedJobsViewModel vm;
    private String uid = "";

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_saved_jobs);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.sjRoot), (v, i) -> {
            androidx.core.graphics.Insets safe = i.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return i;
        });

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        uid = user != null ? user.getUid() : "";
        if (uid.isEmpty()) {
            Toast.makeText(this, R.string.sj_login_prompt, Toast.LENGTH_SHORT).show();
            finish(); return;
        }

        RoleNavigationBar.topbar(this, "SEEKER");
        vm = new ViewModelProvider(this).get(SavedJobsViewModel.class);

        findViewById(R.id.sjBack).setOnClickListener(v -> finish());
        findViewById(R.id.sjReload).setOnClickListener(v -> vm.load(uid));
        findViewById(R.id.sjBtnExplore).setOnClickListener(v -> {
            Intent intent = new Intent(this, PublicJobsActivity.class);
            intent.putExtra(PublicJobsActivity.ROLE, "SEEKER");
            startActivity(intent);
        });

        setupTabs();
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { finish(); }
        });

        vm.changes.observe(this, n -> render());
        vm.load(uid);
    }

    @Override protected void onResume() {
        super.onResume();
        if (vm != null && !uid.isEmpty()) vm.load(uid);
    }

    private void setupTabs() {
        findViewById(R.id.sjTabAll).setOnClickListener(v -> vm.setFilter(SavedJobsViewModel.FILTER_ALL));
        findViewById(R.id.sjTabOpen).setOnClickListener(v -> vm.setFilter(SavedJobsViewModel.FILTER_OPEN));
        findViewById(R.id.sjTabClosed).setOnClickListener(v -> vm.setFilter(SavedJobsViewModel.FILTER_CLOSED));
    }

    private void render() {
        ProgressBar pb = findViewById(R.id.sjProgress);
        pb.setVisibility(vm.loading ? View.VISIBLE : View.GONE);

        AppCompatButton tabAll = findViewById(R.id.sjTabAll);
        AppCompatButton tabOpen = findViewById(R.id.sjTabOpen);
        AppCompatButton tabClosed = findViewById(R.id.sjTabClosed);

        tabAll.setText(getString(R.string.sj_tab_all, vm.getCountAll()));
        tabOpen.setText(getString(R.string.sj_tab_open, vm.getCountOpen()));
        tabClosed.setText(getString(R.string.sj_tab_closed, vm.getCountClosed()));

        updateTabStyle(tabAll, vm.currentFilter == SavedJobsViewModel.FILTER_ALL);
        updateTabStyle(tabOpen, vm.currentFilter == SavedJobsViewModel.FILTER_OPEN);
        updateTabStyle(tabClosed, vm.currentFilter == SavedJobsViewModel.FILTER_CLOSED);

        LinearLayout listBody = findViewById(R.id.sjListBody);
        listBody.removeAllViews();
        View emptyView = findViewById(R.id.sjEmptyLayout);

        if (!vm.loading && vm.displayList.isEmpty()) {
            emptyView.setVisibility(View.VISIBLE);
            TextView emptyTitle = findViewById(R.id.sjEmptyTitle);
            TextView emptyDesc = findViewById(R.id.sjEmptyDesc);
            if (vm.getCountAll() == 0) {
                emptyTitle.setText(R.string.sj_empty_title);
                emptyDesc.setText(R.string.sj_empty_desc);
                findViewById(R.id.sjBtnExplore).setVisibility(View.VISIBLE);
            } else {
                emptyTitle.setText(R.string.sj_empty_tab);
                emptyDesc.setText("");
                findViewById(R.id.sjBtnExplore).setVisibility(View.GONE);
            }
        } else {
            emptyView.setVisibility(View.GONE);
            for (SavedJob sj : vm.displayList) {
                listBody.addView(createJobCard(sj));
            }
        }
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

    private View createJobCard(SavedJob sj) {
        LinearLayout listBody = findViewById(R.id.sjListBody);
        View card = getLayoutInflater().inflate(R.layout.item_saved_job, listBody, false);
        TextView title = card.findViewById(R.id.sjItemTitle);
        TextView badge = card.findViewById(R.id.sjItemBadge);
        TextView employer = card.findViewById(R.id.sjItemEmployer);
        TextView category = card.findViewById(R.id.sjItemCategory);
        TextView pay = card.findViewById(R.id.sjItemPay);
        TextView savedDate = card.findViewById(R.id.sjItemSavedDate);
        AppCompatButton btnUnsave = card.findViewById(R.id.sjItemBtnUnsave);

        PublicJob job = sj.job;
        String jobTitle = job != null ? job.title : "Công việc #" + sj.jobId;
        title.setText(jobTitle);

        if (sj.isOpen()) {
            badge.setText(R.string.sj_status_open);
            badge.setBackgroundColor(Color.parseColor("#E8F5E9"));
            badge.setTextColor(Color.parseColor("#1C6758"));
        } else if (sj.isExpired()) {
            badge.setText(R.string.sj_status_expired);
            badge.setBackgroundColor(Color.parseColor("#FFF3E0"));
            badge.setTextColor(Color.parseColor("#E65100"));
        } else {
            badge.setText(R.string.sj_status_closed);
            badge.setBackgroundColor(Color.parseColor("#EEEEEE"));
            badge.setTextColor(Color.parseColor("#616161"));
        }

        if (job != null) {
            employer.setText(job.employerName + " · " + job.areaName);
            category.setText(job.categoryName);
            String formattedPay = NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(job.payAmount) + " đ";
            pay.setText(formattedPay);
        } else {
            employer.setText("Tin tuyển dụng đã đóng hoặc không khả dụng");
            category.setText("");
            pay.setText("");
        }

        String dateStr = DateFormat.getDateInstance(DateFormat.SHORT, new Locale("vi", "VN")).format(new Date(sj.savedAt));
        savedDate.setText(getString(R.string.sj_saved_date, dateStr));

        btnUnsave.setOnClickListener(v -> confirmUnsave(sj, jobTitle));
        card.setOnClickListener(v -> {
            if (job != null) {
                Intent intent = new Intent(this, PublicJobsActivity.class);
                intent.putExtra(PublicJobsActivity.ROLE, "SEEKER");
                intent.putExtra("detail", sj.jobId);
                startActivity(intent);
            } else {
                Toast.makeText(this, "Tin này đã đóng và không thể xem chi tiết", Toast.LENGTH_SHORT).show();
            }
        });

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, dp(16));
        card.setLayoutParams(lp);
        return card;
    }

    private void confirmUnsave(SavedJob sj, String title) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.sj_unsave_confirm_title)
                .setMessage(getString(R.string.sj_unsave_confirm_msg, title))
                .setPositiveButton(R.string.sj_unsave_yes, (d, w) -> {
                    vm.unsave(uid, sj.jobId);
                    Toast.makeText(this, R.string.sj_unsaved_success, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.sj_unsave_no, null)
                .show();
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
