package vn.edu.ueh.nhom4.grandmaassist.app.ui.sessions;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Locale;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.ShiftAgreementViewModel;

public final class ShiftAgreementEditActivity extends AppCompatActivity {
    public static final String EXTRA_APPLICATION_ID = "application_id";
    public static final String EXTRA_SESSION_ID = "session_id";
    public static final String EXTRA_INITIAL_TASK = "initial_task";
    public static final String EXTRA_INITIAL_ADDRESS = "initial_address";
    public static final String EXTRA_INITIAL_AREA_NAME = "initial_area_name";
    public static final String EXTRA_INITIAL_AREA_CODE = "initial_area_code";
    public static final String EXTRA_INITIAL_PAY = "initial_pay";
    public static final String EXTRA_INITIAL_SCHEDULE = "initial_schedule";

    private ShiftAgreementViewModel vm;
    private String applicationId = "";
    private String sessionId = "";

    private EditText etTask;
    private TextView tvWorkAddress;
    private TextView tvAreaName;
    private EditText etPay;
    private AppCompatButton btnDate;
    private AppCompatButton btnStartTime;
    private AppCompatButton btnEndTime;
    private TextView tvWorkerPay;
    private TextView tvServiceFee;
    private TextView tvTotalDue;
    private ProgressBar pb;
    private TextView tvMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_shift_agreement_edit);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.shiftAgreementEditRoot), (v, i) -> {
            androidx.core.graphics.Insets safe = i.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime()
            );
            v.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return i;
        });

        RoleNavigationBar.topbar(this, "EMPLOYER");

        applicationId = getIntent().getStringExtra(EXTRA_APPLICATION_ID);
        sessionId = getIntent().getStringExtra(EXTRA_SESSION_ID);
        String initTask = getIntent().getStringExtra(EXTRA_INITIAL_TASK);
        String initAddress = getIntent().getStringExtra(EXTRA_INITIAL_ADDRESS);
        String initAreaName = getIntent().getStringExtra(EXTRA_INITIAL_AREA_NAME);
        String initAreaCode = getIntent().getStringExtra(EXTRA_INITIAL_AREA_CODE);
        long initPay = getIntent().getLongExtra(EXTRA_INITIAL_PAY, 0L);
        String initSchedule = getIntent().getStringExtra(EXTRA_INITIAL_SCHEDULE);

        etTask = findViewById(R.id.etShiftTask);
        tvWorkAddress = findViewById(R.id.tvShiftWorkAddress);
        tvAreaName = findViewById(R.id.tvShiftAreaName);
        etPay = findViewById(R.id.etShiftPay);
        btnDate = findViewById(R.id.btnPickShiftDate);
        btnStartTime = findViewById(R.id.btnPickStartTime);
        btnEndTime = findViewById(R.id.btnPickEndTime);
        tvWorkerPay = findViewById(R.id.tvFeeWorkerPay);
        tvServiceFee = findViewById(R.id.tvFeeService);
        tvTotalDue = findViewById(R.id.tvFeeTotalDue);
        pb = findViewById(R.id.shiftEditProgress);
        tvMessage = findViewById(R.id.shiftEditMessage);

        findViewById(R.id.shiftEditBack).setOnClickListener(v -> finish());

        vm = new ViewModelProvider(this).get(ShiftAgreementViewModel.class);

        // Pre-fill initial values if provided
        if (initTask != null && !initTask.isEmpty() && vm.tasks.isEmpty()) {
            vm.tasks = initTask;
            etTask.setText(initTask);
        }
        if (initAddress != null && !initAddress.isEmpty() && vm.privateAddress.isEmpty()) {
            vm.privateAddress = initAddress;
        }
        if (initAreaName != null && !initAreaName.isEmpty() && vm.areaName.isEmpty()) {
            vm.areaName = initAreaName;
        }
        if (initAreaCode != null && !initAreaCode.isEmpty()) {
            vm.areaCode = initAreaCode;
        }
        if (initPay > 0 && vm.laborAmount == 0) {
            vm.updateLaborAmount(initPay);
            etPay.setText(String.valueOf(initPay));
        }
        if (initSchedule != null && !initSchedule.isEmpty() && !vm.dateSet) {
            java.util.regex.Matcher dm = java.util.regex.Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})").matcher(initSchedule);
            if (dm.find()) {
                int d = Integer.parseInt(dm.group(1));
                int m = Integer.parseInt(dm.group(2)) - 1;
                int y = Integer.parseInt(dm.group(3));
                vm.startCalendar.set(Calendar.YEAR, y);
                vm.startCalendar.set(Calendar.MONTH, m);
                vm.startCalendar.set(Calendar.DAY_OF_MONTH, d);
                vm.endCalendar.set(Calendar.YEAR, y);
                vm.endCalendar.set(Calendar.MONTH, m);
                vm.endCalendar.set(Calendar.DAY_OF_MONTH, d);
                vm.dateSet = true;
            }
            java.util.regex.Matcher tm = java.util.regex.Pattern.compile("(\\d{1,2}):(\\d{2})\\s*-\\s*(\\d{1,2}):(\\d{2})").matcher(initSchedule);
            if (tm.find()) {
                int sh = Integer.parseInt(tm.group(1));
                int sm = Integer.parseInt(tm.group(2));
                int eh = Integer.parseInt(tm.group(3));
                int em = Integer.parseInt(tm.group(4));
                vm.startCalendar.set(Calendar.HOUR_OF_DAY, sh);
                vm.startCalendar.set(Calendar.MINUTE, sm);
                vm.endCalendar.set(Calendar.HOUR_OF_DAY, eh);
                vm.endCalendar.set(Calendar.MINUTE, em);
            }
        }

        btnDate.setOnClickListener(v -> showDatePicker());
        btnStartTime.setOnClickListener(v -> showStartTimePicker());
        btnEndTime.setOnClickListener(v -> showEndTimePicker());

        etPay.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    String str = s.toString().replaceAll("[^0-9]", "");
                    long val = str.isEmpty() ? 0L : Long.parseLong(str);
                    vm.updateLaborAmount(val);
                } catch (NumberFormatException ignored) {}
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        AppCompatButton btnSubmit = findViewById(R.id.btnSubmitShiftAgreement);
        if (sessionId != null && !sessionId.isEmpty()) {
            btnSubmit.setText(R.string.session_btn_edit_proposal);
        }

        btnSubmit.setOnClickListener(v -> {
            String rawPay = etPay.getText().toString().replaceAll("[^0-9]", "");
            try {
                long pay = rawPay.isEmpty() ? 0L : Long.parseLong(rawPay);
                vm.updateLaborAmount(pay);
            } catch (Exception ignored) {}

            vm.tasks = etTask.getText().toString().trim();

            btnSubmit.setEnabled(false);
            if (pb != null) pb.setVisibility(View.VISIBLE);

            if (sessionId != null && !sessionId.isEmpty()) {
                vm.edit(sessionId, () -> {
                    Toast.makeText(this, "Đã cập nhật thỏa thuận ca làm!", Toast.LENGTH_SHORT).show();
                    finish();
                }, err -> {
                    btnSubmit.setEnabled(true);
                    if (pb != null) pb.setVisibility(View.GONE);
                    Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                });
            } else {
                vm.propose(applicationId, () -> {
                    Toast.makeText(this, R.string.session_propose_success, Toast.LENGTH_SHORT).show();
                    finish();
                }, err -> {
                    btnSubmit.setEnabled(true);
                    if (pb != null) pb.setVisibility(View.GONE);
                    Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                });
            }
        });

        vm.changes.observe(this, n -> render());

        if (sessionId != null && !sessionId.isEmpty()) {
            vm.loadSession(sessionId);
        } else if (applicationId != null && !applicationId.isEmpty()) {
            vm.loadApplicationForInit(applicationId, () -> {
                if (vm.currentSession != null) {
                    sessionId = vm.currentSession.id;
                    btnSubmit.setText(R.string.session_btn_edit_proposal);
                    etTask.setText(vm.tasks);
                    etPay.setText(String.valueOf(vm.laborAmount));
                }
            });
        }

        updateDateButtons();
    }

    private void render() {
        pb.setVisibility(vm.busy ? View.VISIBLE : View.GONE);
        if (vm.message != null && !vm.message.isEmpty()) {
            tvMessage.setText(vm.message);
            tvMessage.setVisibility(View.VISIBLE);
        } else {
            tvMessage.setVisibility(View.GONE);
        }

        tvWorkerPay.setText(formatMoney(vm.laborAmount));
        tvServiceFee.setText("+" + formatMoney(vm.serviceFee));
        tvTotalDue.setText(formatMoney(vm.totalDue));

        String addr = vm.privateAddress != null && !vm.privateAddress.trim().isEmpty()
                ? vm.privateAddress.trim() : "Địa chỉ theo tin tuyển dụng";
        if (addr.startsWith("Địa chỉ:")) {
            addr = addr.substring("Địa chỉ:".length()).trim();
        }
        tvWorkAddress.setText(addr);

        if (vm.areaName != null && !vm.areaName.trim().isEmpty()) {
            tvAreaName.setText("Khu vực: " + vm.areaName.trim());
            tvAreaName.setVisibility(View.VISIBLE);
        } else {
            tvAreaName.setVisibility(View.GONE);
        }

        updateDateButtons();
    }

    private void updateDateButtons() {
        DateFormat df = DateFormat.getDateInstance(DateFormat.FULL, new Locale("vi", "VN"));
        btnDate.setText(df.format(vm.startCalendar.getTime()));

        btnStartTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                vm.startCalendar.get(Calendar.HOUR_OF_DAY), vm.startCalendar.get(Calendar.MINUTE)));
        btnEndTime.setText(String.format(Locale.getDefault(), "%02d:%02d",
                vm.endCalendar.get(Calendar.HOUR_OF_DAY), vm.endCalendar.get(Calendar.MINUTE)));
    }

    private void showDatePicker() {
        Calendar cal = vm.startCalendar;
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            vm.startCalendar.set(Calendar.YEAR, year);
            vm.startCalendar.set(Calendar.MONTH, month);
            vm.startCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            vm.endCalendar.set(Calendar.YEAR, year);
            vm.endCalendar.set(Calendar.MONTH, month);
            vm.endCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            vm.dateSet = true;
            updateDateButtons();
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showStartTimePicker() {
        Calendar cal = vm.startCalendar;
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            vm.startCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            vm.startCalendar.set(Calendar.MINUTE, minute);
            vm.startTimeSet = true;
            updateDateButtons();
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }

    private void showEndTimePicker() {
        Calendar cal = vm.endCalendar;
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            vm.endCalendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            vm.endCalendar.set(Calendar.MINUTE, minute);
            vm.endTimeSet = true;
            updateDateButtons();
        }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
    }

    private String formatMoney(long amount) {
        return NumberFormat.getIntegerInstance(new Locale("vi", "VN")).format(amount) + " đ";
    }
}
