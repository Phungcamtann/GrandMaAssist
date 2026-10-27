package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.WalletRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.BankAccount;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.WalletTransaction;

public final class WalletViewModel extends ViewModel {
    private final WalletRepository repo = new WalletRepository();

    private final MutableLiveData<Long> _balance = new MutableLiveData<>(0L);
    public final LiveData<Long> balance = _balance;

    private final MutableLiveData<BankAccount> _bankAccount = new MutableLiveData<>(null);
    public final LiveData<BankAccount> bankAccount = _bankAccount;

    private final MutableLiveData<List<WalletTransaction>> _transactions = new MutableLiveData<>(new ArrayList<>());
    public final LiveData<List<WalletTransaction>> transactions = _transactions;

    private final MutableLiveData<Boolean> _busy = new MutableLiveData<>(false);
    public final LiveData<Boolean> busy = _busy;

    private final MutableLiveData<String> _message = new MutableLiveData<>(null);
    public final LiveData<String> message = _message;

    private ListenerRegistration walletListener;
    private String currentUid;

    public void init(String uid) {
        if (uid == null || uid.isEmpty()) return;
        this.currentUid = uid;
        if (walletListener != null) {
            walletListener.remove();
        }

        walletListener = repo.observeWallet(uid, (doc, error) -> {
            if (error != null || doc == null || !doc.exists()) {
                _balance.setValue(0L);
                _bankAccount.setValue(null);
                return;
            }
            Long bal = doc.getLong("balance");
            _balance.setValue(bal != null ? bal : 0L);

            Object bObj = doc.get("bankAccount");
            if (bObj instanceof Map) {
                _bankAccount.setValue(BankAccount.fromMap((Map<?, ?>) bObj));
            } else {
                _bankAccount.setValue(null);
            }
        });

        loadTransactions();
    }

    public void loadTransactions() {
        if (currentUid == null || currentUid.isEmpty()) return;
        _busy.setValue(true);
        repo.getTransactions(currentUid, 30).addOnCompleteListener(t -> {
            _busy.setValue(false);
            if (t.isSuccessful() && t.getResult() != null) {
                _transactions.setValue(t.getResult());
            } else if (t.getException() != null) {
                _message.setValue(WalletRepository.error(t.getException()));
            }
        });
    }

    public void topUp(long amount, String note, Runnable onSuccess, Consumer<String> onError) {
        if (currentUid == null || currentUid.isEmpty()) return;
        _busy.setValue(true);
        repo.topUp(currentUid, amount, note).addOnCompleteListener(t -> {
            _busy.setValue(false);
            if (t.isSuccessful()) {
                loadTransactions();
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WalletRepository.error(t.getException());
                _message.setValue(err);
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void withdraw(long amount, BankAccount bank, Runnable onSuccess, Consumer<String> onError) {
        if (currentUid == null || currentUid.isEmpty()) return;
        _busy.setValue(true);
        repo.withdraw(currentUid, amount, bank).addOnCompleteListener(t -> {
            _busy.setValue(false);
            if (t.isSuccessful()) {
                loadTransactions();
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WalletRepository.error(t.getException());
                _message.setValue(err);
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void linkBankAccount(String bankName, String accountNumber, String accountHolder,
                                Runnable onSuccess, Consumer<String> onError) {
        if (currentUid == null || currentUid.isEmpty()) return;
        BankAccount bank = new BankAccount(bankName, accountNumber, accountHolder, Timestamp.now());
        _busy.setValue(true);
        repo.linkBankAccount(currentUid, bank).addOnCompleteListener(t -> {
            _busy.setValue(false);
            if (t.isSuccessful()) {
                _bankAccount.setValue(bank);
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WalletRepository.error(t.getException());
                _message.setValue(err);
                if (onError != null) onError.accept(err);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (walletListener != null) {
            walletListener.remove();
            walletListener = null;
        }
    }
}
