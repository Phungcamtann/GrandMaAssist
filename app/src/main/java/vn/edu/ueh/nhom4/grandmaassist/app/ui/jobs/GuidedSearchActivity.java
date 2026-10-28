package vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.PublicJobStore;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.PublicJob;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.service.GuidedJobMatcher;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.GuidedSearchViewModel;

public final class GuidedSearchActivity extends AppCompatActivity {
    private GuidedSearchViewModel vm;
    private RoleNavigationBar nav;
    private int renderedStep = -1;
    private vn.edu.ueh.nhom4.grandmaassist.app.ui.accessibility.TtsControllerHelper ttsHelper;

    private static final String[] DAYS_KEYS = {"MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"};
    private static final int[] DAYS_STRINGS = {R.string.gs_d_mon, R.string.gs_d_tue, R.string.gs_d_wed, R.string.gs_d_thu, R.string.gs_d_fri, R.string.gs_d_sat, R.string.gs_d_sun};

    private static final String[] SHIFT_KEYS = {"MORNING", "AFTERNOON", "EVENING"};
    private static final int[] SHIFT_STRINGS = {R.string.gs_s_morning, R.string.gs_s_afternoon, R.string.gs_s_evening};

    private static final String[] DEMAND_KEYS = {"HEAVY_LIFTING", "PROLONGED_STANDING", "FREQUENT_MOVEMENT"};
    private static final int[] DEMAND_STRINGS = {R.string.gs_demand_heavy, R.string.gs_demand_standing, R.string.gs_demand_movement};

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_guided_search);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.gsRoot), (v, insets) -> {
            androidx.core.graphics.Insets safe = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return insets;
        });

        vm = new ViewModelProvider(this).get(GuidedSearchViewModel.class);

        nav = new RoleNavigationBar(this, a -> {
            if (a == RoleNavigationBar.Action.HOME || a == RoleNavigationBar.Action.PROFILE || a == RoleNavigationBar.Action.ACCOUNT) leave(a.name());
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (vm.currentStep > 1) vm.prevStep();
                else finish();
            }
        });

        findViewById(R.id.gsBtnBack).setOnClickListener(v -> {
            if (vm.currentStep > 1) vm.prevStep();
            else finish();
        });

        findViewById(R.id.gsBtnSkip).setOnClickListener(v -> vm.nextStep());
        findViewById(R.id.gsBtnNext).setOnClickListener(v -> vm.nextStep());

        View ttsView = findViewById(R.id.gsTtsController);
        if (ttsView != null) {
            ttsHelper = new vn.edu.ueh.nhom4.grandmaassist.app.ui.accessibility.TtsControllerHelper(this, ttsView, "guided_search");
            ttsHelper.setCustomIdleLabel("Nghe hướng dẫn");
            ttsHelper.setTextSupplier(this::getSpeechTextForCurrentStep);
        }

        vm.changes.observe(this, n -> render());
    }

    private void leave(String route) {
        setResult(RESULT_OK, new Intent().putExtra(RoleNavigationBar.RETURN_DESTINATION, route));
        finish();
    }

    private void render() {
        RoleNavigationBar.topbar(this, "SEEKER");
        nav.render("SEEKER", RoleNavigationBar.Action.HOME, true, !vm.busy);

        int step = vm.currentStep;
        if (renderedStep != step) {
            renderedStep = step;
            findViewById(R.id.gsScroll).post(() -> findViewById(R.id.gsScroll).scrollTo(0, 0));
            if (ttsHelper != null) {
                ttsHelper.reset();
                ttsHelper.autoPlayIfEnabled();
            }
        }

        // Header and Segment Bar
        TextView stepText = findViewById(R.id.gsStepText);
        View seg1 = findViewById(R.id.gsSeg1), seg2 = findViewById(R.id.gsSeg2), seg3 = findViewById(R.id.gsSeg3), seg4 = findViewById(R.id.gsSeg4);
        TextView question = findViewById(R.id.gsQuestion), subQuestion = findViewById(R.id.gsSubQuestion);
        View header = findViewById(R.id.gsHeader);

        int activeColor = Color.parseColor("#1C6758");
        int inactiveColor = Color.parseColor("#D0D5D2");

        if (step <= 4) {
            header.setVisibility(View.VISIBLE);
            stepText.setText(getString(getResources().getIdentifier("gs_step_" + step, "string", getPackageName())));
            seg1.setBackgroundColor(step >= 1 ? activeColor : inactiveColor);
            seg2.setBackgroundColor(step >= 2 ? activeColor : inactiveColor);
            seg3.setBackgroundColor(step >= 3 ? activeColor : inactiveColor);
            seg4.setBackgroundColor(step >= 4 ? activeColor : inactiveColor);

            if (step == 1) {
                question.setText(R.string.gs_q_category);
                subQuestion.setText(R.string.gs_sub_category); subQuestion.setVisibility(View.VISIBLE);
            } else if (step == 2) {
                question.setText(R.string.gs_q_area);
                subQuestion.setVisibility(View.GONE);
            } else if (step == 3) {
                question.setText(R.string.gs_q_schedule);
                subQuestion.setVisibility(View.GONE);
            } else {
                question.setText(R.string.gs_q_demands);
                subQuestion.setVisibility(View.GONE);
            }
        } else if (step == 5) {
            header.setVisibility(View.VISIBLE);
            stepText.setText("");
            seg1.setBackgroundColor(activeColor); seg2.setBackgroundColor(activeColor);
            seg3.setBackgroundColor(activeColor); seg4.setBackgroundColor(activeColor);
            question.setText(R.string.gs_summary_title);
            subQuestion.setVisibility(View.GONE);
        } else {
            header.setVisibility(View.GONE);
        }

        // Footer Buttons State
        Button btnNext = findViewById(R.id.gsBtnNext);
        TextView btnBack = findViewById(R.id.gsBtnBack);
        TextView btnSkip = findViewById(R.id.gsBtnSkip);

        btnBack.setVisibility(step > 1 ? View.VISIBLE : View.GONE);
        btnSkip.setVisibility(step <= 4 ? View.VISIBLE : View.GONE);

        if (step <= 4) btnNext.setText(R.string.gs_action_continue);
        else if (step == 5) btnNext.setText(R.string.gs_action_see_jobs);
        else btnNext.setText(R.string.gs_see_all);

        btnNext.setOnClickListener(v -> {
            if (step <= 4) vm.nextStep();
            else if (step == 5) vm.search();
            else openPublicJobs("");
        });

        LinearLayout container = findViewById(R.id.gsStepContainer);
        container.removeAllViews();

        if (step == 1) renderStep1(container);
        else if (step == 2) renderStep2(container);
        else if (step == 3) renderStep3(container);
        else if (step == 4) renderStep4(container);
        else if (step == 5) renderStep5(container);
        else renderStep6(container);
    }

    private View createCard(String title, boolean selected, View.OnClickListener onClick) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(24, 22, 24, 22);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(20f);
        gd.setColor(selected ? Color.parseColor("#EAF1E8") : Color.WHITE);
        gd.setStroke(selected ? 4 : 2, selected ? Color.parseColor("#1C6758") : Color.parseColor("#E0E4E1"));
        card.setBackground(gd);

        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tv.setText(title); tv.setTextSize(21); tv.setTextColor(Color.parseColor("#1A1C19"));
        card.addView(tv);

        CheckBox cb = new CheckBox(this);
        cb.setChecked(selected);
        cb.setClickable(false);
        card.addView(cb);

        card.setOnClickListener(onClick);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16);
        card.setLayoutParams(lp);
        return card;
    }

    private void renderStep1(LinearLayout container) {
        for (PublicJobStore.Item item : vm.catalog.categories) {
            boolean sel = vm.selectedCategories.contains(item.id);
            container.addView(createCard(item.name, sel, v -> vm.toggleCategory(item.id)));
        }
        container.addView(createCard(getString(R.string.gs_no_priority), vm.noCategoryPriority, v -> vm.setNoCategoryPriority(!vm.noCategoryPriority)));
    }

    private void renderStep2(LinearLayout container) {
        if (!vm.seekerProfileArea.isEmpty()) {
            String suggestName = vm.seekerProfileArea;
            for (PublicJobStore.Item i : vm.catalog.areas) if (i.id.equals(vm.seekerProfileArea)) { suggestName = i.name; break; }
            String title = String.format(getString(R.string.gs_profile_suggest), suggestName);
            boolean sel = vm.selectedAreas.contains(vm.seekerProfileArea);
            container.addView(createCard(title, sel, v -> vm.toggleArea(vm.seekerProfileArea)));
        }
        for (PublicJobStore.Item item : vm.catalog.areas) {
            if (item.id.equals(vm.seekerProfileArea)) continue;
            boolean sel = vm.selectedAreas.contains(item.id);
            container.addView(createCard(item.name, sel, v -> vm.toggleArea(item.id)));
        }
        container.addView(createCard(getString(R.string.gs_no_priority), vm.noAreaPriority, v -> vm.setNoAreaPriority(!vm.noAreaPriority)));
    }

    private void renderStep3(LinearLayout container) {
        // Days pills
        TextView dayLabel = new TextView(this);
        dayLabel.setText("Chọn thứ trong tuần:"); dayLabel.setTextSize(18); dayLabel.setTextColor(Color.parseColor("#5C6360"));
        container.addView(dayLabel);

        GridLayout dayGrid = new GridLayout(this);
        dayGrid.setColumnCount(4);
        dayGrid.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        for (int i = 0; i < DAYS_KEYS.length; i++) {
            String key = DAYS_KEYS[i];
            boolean sel = vm.selectedDays.contains(key);
            Button btn = new Button(this);
            btn.setText(getString(DAYS_STRINGS[i])); btn.setTextSize(18);
            GradientDrawable gd = new GradientDrawable(); gd.setCornerRadius(30f);
            gd.setColor(sel ? Color.parseColor("#1C6758") : Color.WHITE);
            gd.setStroke(2, Color.parseColor("#1C6758"));
            btn.setBackground(gd);
            btn.setTextColor(sel ? Color.WHITE : Color.parseColor("#1C6758"));
            btn.setOnClickListener(v -> vm.toggleDay(key));

            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = 0; params.height = 136;
            params.columnSpec = GridLayout.spec(i % 4, 1f);
            params.setMargins(6, 6, 6, 6);
            btn.setLayoutParams(params);
            dayGrid.addView(btn);
        }
        container.addView(dayGrid);

        // Shifts
        TextView shiftLabel = new TextView(this);
        shiftLabel.setText("Chọn buổi:"); shiftLabel.setTextSize(18); shiftLabel.setTextColor(Color.parseColor("#5C6360"));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        slp.setMargins(0, 20, 0, 8); shiftLabel.setLayoutParams(slp);
        container.addView(shiftLabel);

        LinearLayout shiftRow = new LinearLayout(this);
        shiftRow.setOrientation(LinearLayout.HORIZONTAL);

        for (int i = 0; i < SHIFT_KEYS.length; i++) {
            String key = SHIFT_KEYS[i];
            boolean sel = !vm.isFlexibleShift && vm.selectedShifts.contains(key);
            Button btn = new Button(this);
            btn.setText(getString(SHIFT_STRINGS[i])); btn.setTextSize(18);
            GradientDrawable gd = new GradientDrawable(); gd.setCornerRadius(30f);
            gd.setColor(sel ? Color.parseColor("#1C6758") : Color.WHITE);
            gd.setStroke(2, Color.parseColor("#1C6758"));
            btn.setBackground(gd);
            btn.setTextColor(sel ? Color.WHITE : Color.parseColor("#1C6758"));
            btn.setOnClickListener(v -> vm.toggleShift(key));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 136, 1f);
            lp.setMargins(6, 0, 6, 0); btn.setLayoutParams(lp);
            shiftRow.addView(btn);
        }
        container.addView(shiftRow);

        // Flexible
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        flp.setMargins(0, 20, 0, 0);
        View flexCard = createCard(getString(R.string.gs_flexible), vm.isFlexibleShift, v -> vm.toggleShift("FLEXIBLE"));
        flexCard.setLayoutParams(flp);
        container.addView(flexCard);
    }

    private void renderStep4(LinearLayout container) {
        for (int i = 0; i < DEMAND_KEYS.length; i++) {
            String key = DEMAND_KEYS[i];
            boolean sel = vm.avoidedDemands.contains(key);
            container.addView(createCard(getString(DEMAND_STRINGS[i]), sel, v -> vm.toggleDemand(key)));
        }
        container.addView(createCard(getString(R.string.gs_no_avoid), vm.noAvoidPriority, v -> vm.setNoAvoidPriority(!vm.noAvoidPriority)));
    }

    private void renderStep5(LinearLayout container) {
        container.addView(createSummaryRow(getString(R.string.gs_sum_cat), formatCategories(), 1));
        container.addView(createSummaryRow(getString(R.string.gs_sum_area), formatAreas(), 2));
        container.addView(createSummaryRow(getString(R.string.gs_sum_schedule), formatSchedule(), 3));
        container.addView(createSummaryRow(getString(R.string.gs_sum_demands), formatDemands(), 4));
    }

    private View createSummaryRow(String title, String val, int targetStep) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(20, 20, 20, 20);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(20f); gd.setColor(Color.WHITE);
        gd.setStroke(2, Color.parseColor("#E0E4E1"));
        row.setBackground(gd);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        tvTitle.setText(title); tvTitle.setTextSize(18); tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#1A1C19"));
        top.addView(tvTitle);

        TextView tvEdit = new TextView(this);
        tvEdit.setText(R.string.gs_edit); tvEdit.setTextSize(16); tvEdit.setTextColor(Color.parseColor("#1C6758"));
        tvEdit.setPadding(12, 8, 12, 8);
        tvEdit.setOnClickListener(v -> vm.jumpToStep(targetStep));
        top.addView(tvEdit);

        row.addView(top);

        TextView tvVal = new TextView(this);
        tvVal.setText(val); tvVal.setTextSize(16); tvVal.setTextColor(Color.parseColor("#5C6360"));
        tvVal.setPadding(0, 8, 0, 0);
        row.addView(tvVal);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16); row.setLayoutParams(lp);
        return row;
    }

    private String formatCategories() {
        if (vm.noCategoryPriority || vm.selectedCategories.isEmpty()) return getString(R.string.gs_no_priority);
        List<String> names = new ArrayList<>();
        for (String id : vm.selectedCategories) {
            for (PublicJobStore.Item i : vm.catalog.categories) if (i.id.equals(id)) names.add(i.name);
        }
        return names.isEmpty() ? getString(R.string.gs_no_priority) : String.join(", ", names);
    }

    private String formatAreas() {
        if (vm.noAreaPriority || vm.selectedAreas.isEmpty()) return getString(R.string.gs_no_priority);
        List<String> names = new ArrayList<>();
        for (String code : vm.selectedAreas) {
            for (PublicJobStore.Item i : vm.catalog.areas) if (i.id.equals(code)) names.add(i.name);
        }
        return names.isEmpty() ? getString(R.string.gs_no_priority) : String.join(", ", names);
    }

    private String formatSchedule() {
        if (vm.selectedDays.isEmpty() && vm.selectedShifts.isEmpty() && !vm.isFlexibleShift) return "Tất cả các ngày";
        List<String> days = new ArrayList<>();
        for (int i = 0; i < DAYS_KEYS.length; i++) {
            if (vm.selectedDays.contains(DAYS_KEYS[i])) days.add(getString(DAYS_STRINGS[i]));
        }
        List<String> shifts = new ArrayList<>();
        if (vm.isFlexibleShift) shifts.add(getString(R.string.gs_flexible));
        else {
            for (int i = 0; i < SHIFT_KEYS.length; i++) {
                if (vm.selectedShifts.contains(SHIFT_KEYS[i])) shifts.add(getString(SHIFT_STRINGS[i]));
            }
        }
        String d = days.isEmpty() ? "" : String.join(", ", days);
        String s = shifts.isEmpty() ? "" : String.join(", ", shifts);
        return d + (!d.isEmpty() && !s.isEmpty() ? " • " : "") + s;
    }

    private String formatDemands() {
        if (vm.noAvoidPriority || vm.avoidedDemands.isEmpty()) return getString(R.string.gs_no_avoid);
        List<String> list = new ArrayList<>();
        for (int i = 0; i < DEMAND_KEYS.length; i++) {
            if (vm.avoidedDemands.contains(DEMAND_KEYS[i])) list.add(getString(DEMAND_STRINGS[i]));
        }
        return list.isEmpty() ? getString(R.string.gs_no_avoid) : String.join(", ", list);
    }

    private void renderStep6(LinearLayout container) {
        if (vm.busy) {
            ProgressBar pb = new ProgressBar(this);
            container.addView(pb);
            return;
        }

        if (vm.matchedResults.isEmpty()) {
            TextView emptyText = new TextView(this);
            emptyText.setText(R.string.gs_empty_results);
            emptyText.setTextSize(18); emptyText.setTextColor(Color.parseColor("#1A1C19"));
            emptyText.setPadding(0, 20, 0, 20);
            container.addView(emptyText);
            return;
        }

        TextView countText = new TextView(this);
        countText.setText(String.format(getString(R.string.gs_matched_count), vm.matchedResults.size()));
        countText.setTextSize(20); countText.setTypeface(null, Typeface.BOLD);
        countText.setTextColor(Color.parseColor("#1C6758"));
        countText.setPadding(0, 0, 0, 16);
        container.addView(countText);

        for (GuidedJobMatcher.MatchResult res : vm.matchedResults) {
            container.addView(createJobCard(res));
        }
    }

    private View createJobCard(GuidedJobMatcher.MatchResult res) {
        PublicJob job = res.job;
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(24, 24, 24, 24);

        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(20f); gd.setColor(Color.WHITE);
        gd.setStroke(2, Color.parseColor("#E0E4E1"));
        card.setBackground(gd);

        TextView tvTitle = new TextView(this);
        tvTitle.setText(job.title); tvTitle.setTextSize(20); tvTitle.setTypeface(null, Typeface.BOLD);
        tvTitle.setTextColor(Color.parseColor("#1A1C19"));
        card.addView(tvTitle);

        TextView tvEmp = new TextView(this);
        tvEmp.setText(job.employerName + " • " + job.areaName); tvEmp.setTextSize(16);
        tvEmp.setTextColor(Color.parseColor("#5C6360")); tvEmp.setPadding(0, 4, 0, 8);
        card.addView(tvEmp);

        // Tags Layout
        LinearLayout tagsLayout = new LinearLayout(this);
        tagsLayout.setOrientation(LinearLayout.HORIZONTAL);
        tagsLayout.setPadding(0, 8, 0, 8);

        for (String tag : res.explanationTags) {
            TextView tagView = new TextView(this);
            tagView.setText(tag); tagView.setTextSize(14); tagView.setTextColor(Color.parseColor("#1C6758"));
            GradientDrawable tagBg = new GradientDrawable();
            tagBg.setCornerRadius(12f); tagBg.setColor(Color.parseColor("#EAF1E8"));
            tagView.setBackground(tagBg);
            tagView.setPadding(16, 8, 16, 8);
            LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            tlp.setMargins(0, 0, 8, 8); tagView.setLayoutParams(tlp);
            tagsLayout.addView(tagView);
        }
        card.addView(tagsLayout);

        card.setOnClickListener(v -> openPublicJobs(job.id));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 16); card.setLayoutParams(lp);
        return card;
    }

    private void openPublicJobs(String detailId) {
        Intent intent = new Intent(this, PublicJobsActivity.class);
        intent.putExtra(PublicJobsActivity.ROLE, "SEEKER");
        if (detailId != null && !detailId.isEmpty()) {
            intent.putExtra("detail", detailId);
        }
        startActivity(intent);
    }

    private String getSpeechTextForCurrentStep() {
        int step = vm != null ? vm.currentStep : 1;
        switch (step) {
            case 1:
                return "Bước 1: Bác muốn làm công việc gì? Bác có thể chạm để chọn một hoặc nhiều công việc bên dưới như Nấu ăn gia đình, Dọn dẹp nhà cửa, hoặc Trông coi sân vườn. Sau đó bấm Tiếp tục.";
            case 2:
                return "Bước 2: Bác muốn làm việc ở khu vực nào? Bác có thể chọn các quận huyện gần nhà mình nhất để thuận tiện đi lại.";
            case 3:
                return "Bước 3: Bác thuận tiện làm việc vào những ngày nào trong tuần và buổi nào? Bác có thể chọn làm buổi sáng, buổi chiều hoặc buổi tối vào các ngày phù hợp với bác.";
            case 4:
                return "Bước 4: Bác cần tránh những yêu cầu nào để đảm bảo sức khỏe? Ví dụ tránh việc mang vác nặng, đứng lâu quá hai tiếng, hoặc di chuyển liên tục.";
            default:
                return "Kết quả tìm việc: Hệ thống đã chọn lọc các công việc phù hợp với tiêu chí của bác.";
        }
    }
}
