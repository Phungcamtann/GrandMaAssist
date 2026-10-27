package vn.edu.ueh.nhom4.grandmaassist.app.ui.wallet;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.text.NumberFormat;
import java.util.Locale;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.BankAccount;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.WalletViewModel;

public final class WalletActivity extends AppCompatActivity {
    public static final String EXTRA_INITIAL_TOPUP_AMOUNT = "extra_initial_topup_amount";
    public static final String EXTRA_ROLE = "extra_role";

    public static void start(Context context, String role, long prefillTopUpAmount) {
        Intent intent = new Intent(context, WalletActivity.class);
        intent.putExtra(EXTRA_ROLE, role);
        if (prefillTopUpAmount > 0) {
            intent.putExtra(EXTRA_INITIAL_TOPUP_AMOUNT, prefillTopUpAmount);
        }
        context.startActivity(intent);
    }

    private WalletViewModel vm;
    private TextView tvBalance;
    private View layoutBankLinked;
    private View layoutBankNotLinked;
    private TextView tvBankNameAndNum;
    private TextView tvBankAccountHolder;
    private TextView tvEmptyTx;
    private RecyclerView rvTransactions;
    private ProgressBar pbWallet;
    private WalletTransactionAdapter adapter;
    private String currentUid = "";
    private String currentRole = "EMPLOYER";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        currentRole = getIntent().getStringExtra(EXTRA_ROLE);
        if (currentRole == null || currentRole.isEmpty()) currentRole = "EMPLOYER";
        RoleNavigationBar.topbar(this, currentRole);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            finish();
            return;
        }
        currentUid = user.getUid();

        vm = new ViewModelProvider(this).get(WalletViewModel.class);

        findViewById(R.id.btnWalletBack).setOnClickListener(v -> finish());

        tvBalance = findViewById(R.id.tvWalletBalance);
        layoutBankLinked = findViewById(R.id.layoutBankLinked);
        layoutBankNotLinked = findViewById(R.id.layoutBankNotLinked);
        tvBankNameAndNum = findViewById(R.id.tvBankNameAndNum);
        tvBankAccountHolder = findViewById(R.id.tvBankAccountHolder);
        tvEmptyTx = findViewById(R.id.tvEmptyTx);
        rvTransactions = findViewById(R.id.rvTransactions);
        pbWallet = findViewById(R.id.pbWallet);

        adapter = new WalletTransactionAdapter();
        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        rvTransactions.setAdapter(adapter);

        findViewById(R.id.btnWalletTopUp).setOnClickListener(v -> showTopUpDialog(0L));
        findViewById(R.id.btnWalletWithdraw).setOnClickListener(v -> showWithdrawDialog());
        findViewById(R.id.btnLinkBank).setOnClickListener(v -> showLinkBankDialog());
        findViewById(R.id.btnChangeBank).setOnClickListener(v -> showLinkBankDialog());

        vm.balance.observe(this, bal -> {
            tvBalance.setText(formatMoney(bal != null ? bal : 0L));
        });

        vm.bankAccount.observe(this, bank -> {
            if (bank != null && !bank.bankName.isEmpty()) {
                layoutBankLinked.setVisibility(View.VISIBLE);
                layoutBankNotLinked.setVisibility(View.GONE);
                tvBankNameAndNum.setText(bank.bankName + " • " + bank.getMaskedNumber());
                tvBankAccountHolder.setText(bank.accountHolder);
            } else {
                layoutBankLinked.setVisibility(View.GONE);
                layoutBankNotLinked.setVisibility(View.VISIBLE);
            }
        });

        vm.transactions.observe(this, list -> {
            adapter.submitList(list);
            if (list == null || list.isEmpty()) {
                tvEmptyTx.setVisibility(View.VISIBLE);
                rvTransactions.setVisibility(View.GONE);
            } else {
                tvEmptyTx.setVisibility(View.GONE);
                rvTransactions.setVisibility(View.VISIBLE);
            }
        });

        vm.busy.observe(this, b -> pbWallet.setVisibility(b != null && b ? View.VISIBLE : View.GONE));

        vm.message.observe(this, msg -> {
            if (msg != null && !msg.isEmpty()) {
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            }
        });

        vm.init(currentUid);

        long prefill = getIntent().getLongExtra(EXTRA_INITIAL_TOPUP_AMOUNT, 0L);
        if (prefill > 0) {
            showTopUpDialog(prefill);
        }
    }

    private String formatMoney(long amount) {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(amount) + " đ";
    }

    private void showTopUpDialog(long prefillAmount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_topup, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        EditText etAmount = view.findViewById(R.id.etTopUpAmount);
        if (prefillAmount > 0) {
            etAmount.setText(String.valueOf(prefillAmount));
        }

        view.findViewById(R.id.btnChip100k).setOnClickListener(v -> etAmount.setText("100000"));
        view.findViewById(R.id.btnChip200k).setOnClickListener(v -> etAmount.setText("200000"));
        view.findViewById(R.id.btnChip500k).setOnClickListener(v -> etAmount.setText("500000"));
        view.findViewById(R.id.btnChip1m).setOnClickListener(v -> etAmount.setText("1000000"));

        view.findViewById(R.id.btnProceedTopUp).setOnClickListener(v -> {
            String str = etAmount.getText().toString().replaceAll("[^0-9]", "");
            if (str.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tiền muốn nạp", Toast.LENGTH_SHORT).show();
                return;
            }
            long amt = Long.parseLong(str);
            if (amt <= 0) {
                Toast.makeText(this, "Số tiền nạp phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            showTopUpQrDialog(amt);
        });

        dialog.show();
    }

    private void showTopUpQrDialog(long amount) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_topup_qr, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        View layoutScanning = view.findViewById(R.id.layoutQrScanning);
        View layoutSuccess = view.findViewById(R.id.layoutQrSuccess);
        TextView tvQrAmount = view.findViewById(R.id.tvQrAmount);
        TextView tvSuccessAmount = view.findViewById(R.id.tvQrSuccessAmount);
        Button btnManual = view.findViewById(R.id.btnQrConfirmManual);
        Button btnDone = view.findViewById(R.id.btnQrDone);

        tvQrAmount.setText(formatMoney(amount));
        tvSuccessAmount.setText("+" + formatMoney(amount));

        final boolean[] executed = {false};
        Handler handler = new Handler(Looper.getMainLooper());

        Runnable triggerTopUp = () -> {
            if (executed[0]) return;
            executed[0] = true;
            vm.topUp(amount, "Nạp tiền vào ví qua VietQR", () -> {
                layoutScanning.setVisibility(View.GONE);
                layoutSuccess.setVisibility(View.VISIBLE);
            }, err -> {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
                dialog.dismiss();
            });
        };

        // Auto-detect payment after 2 seconds
        handler.postDelayed(triggerTopUp, 2000);

        btnManual.setOnClickListener(v -> {
            handler.removeCallbacks(triggerTopUp);
            triggerTopUp.run();
        });

        btnDone.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showWithdrawDialog() {
        BankAccount bank = vm.bankAccount.getValue();
        if (bank == null || bank.bankName.isEmpty()) {
            Toast.makeText(this, "Vui lòng liên kết tài khoản ngân hàng trước khi rút tiền.", Toast.LENGTH_LONG).show();
            showLinkBankDialog();
            return;
        }

        Long curBal = vm.balance.getValue();
        long balance = curBal != null ? curBal : 0L;
        if (balance <= 0) {
            Toast.makeText(this, "Số dư ví hiện tại bằng 0, không thể rút tiền.", Toast.LENGTH_SHORT).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_withdraw, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        TextView tvBank = view.findViewById(R.id.tvWithdrawBankName);
        TextView tvHolder = view.findViewById(R.id.tvWithdrawAccountHolder);
        TextView tvAvail = view.findViewById(R.id.tvWithdrawAvailable);
        EditText etAmount = view.findViewById(R.id.etWithdrawAmount);

        tvBank.setText(bank.bankName + " • " + bank.getMaskedNumber());
        tvHolder.setText(bank.accountHolder);
        tvAvail.setText("Số dư khả dụng: " + formatMoney(balance));

        view.findViewById(R.id.btnWithdrawAll).setOnClickListener(v -> etAmount.setText(String.valueOf(balance)));

        view.findViewById(R.id.btnConfirmWithdraw).setOnClickListener(v -> {
            String str = etAmount.getText().toString().replaceAll("[^0-9]", "");
            if (str.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tiền cần rút", Toast.LENGTH_SHORT).show();
                return;
            }
            long amt = Long.parseLong(str);
            if (amt <= 0) {
                Toast.makeText(this, "Số tiền rút phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }
            if (amt > balance) {
                Toast.makeText(this, "Số tiền rút vượt quá số dư khả dụng (" + formatMoney(balance) + ")", Toast.LENGTH_SHORT).show();
                return;
            }

            vm.withdraw(amt, bank, () -> {
                dialog.dismiss();
                Toast.makeText(this, "Rút tiền thành công! Số tiền " + formatMoney(amt) + " đã được chuyển về tài khoản của bạn.", Toast.LENGTH_LONG).show();
            }, err -> {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            });
        });

        dialog.show();
    }

    private void showLinkBankDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_link_bank, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        Spinner spBank = view.findViewById(R.id.spBankName);
        EditText etNum = view.findViewById(R.id.etAccountNumber);
        EditText etHolder = view.findViewById(R.id.etAccountHolder);

        String[] banks = new String[]{
                "Vietcombank", "MBBank", "Techcombank", "BIDV",
                "Agribank", "VPBank", "ACB", "VietinBank", "MoMo"
        };
        ArrayAdapter<String> bankAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, banks);
        spBank.setAdapter(bankAdapter);

        BankAccount currentBank = vm.bankAccount.getValue();
        if (currentBank != null) {
            etNum.setText(currentBank.accountNumber);
            etHolder.setText(currentBank.accountHolder);
            for (int i = 0; i < banks.length; i++) {
                if (banks[i].equalsIgnoreCase(currentBank.bankName)) {
                    spBank.setSelection(i);
                    break;
                }
            }
        } else {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u != null && u.getDisplayName() != null && !u.getDisplayName().isEmpty()) {
                etHolder.setText(u.getDisplayName().toUpperCase());
            }
        }

        view.findViewById(R.id.btnSaveBank).setOnClickListener(v -> {
            String selectedBank = (String) spBank.getSelectedItem();
            String accNum = etNum.getText().toString().trim();
            String accHolder = etHolder.getText().toString().trim().toUpperCase();

            if (accNum.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập số tài khoản", Toast.LENGTH_SHORT).show();
                return;
            }
            if (accHolder.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập tên chủ tài khoản", Toast.LENGTH_SHORT).show();
                return;
            }

            vm.linkBankAccount(selectedBank, accNum, accHolder, () -> {
                dialog.dismiss();
                Toast.makeText(this, "Đã liên kết tài khoản ngân hàng thành công!", Toast.LENGTH_SHORT).show();
            }, err -> {
                Toast.makeText(this, err, Toast.LENGTH_LONG).show();
            });
        });

        dialog.show();
    }
}
