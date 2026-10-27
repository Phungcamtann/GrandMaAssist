package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.BankAccount;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.WalletTransaction;

public final class WalletRepository {
    public static final class Failure extends RuntimeException {
        public final String code;
        public Failure(String code) { super(code); this.code = code; }
    }

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public static <T> Task<T> bounded(Task<T> task) {
        return Tasks.withTimeout(task, 45, TimeUnit.SECONDS);
    }

    public boolean same(String uid) {
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        return u != null && u.getUid().equals(uid);
    }

    public DocumentReference getWalletRef(String uid) {
        return db.collection("wallets").document(uid);
    }

    public CollectionReference getTransactionsRef(String uid) {
        return db.collection("wallets").document(uid).collection("transactions");
    }

    public ListenerRegistration observeWallet(String uid, com.google.firebase.firestore.EventListener<DocumentSnapshot> listener) {
        return getWalletRef(uid).addSnapshotListener(listener);
    }

    public Task<DocumentSnapshot> getWallet(String uid) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        return bounded(getWalletRef(uid).get(Source.SERVER));
    }

    public Task<Void> linkBankAccount(String uid, BankAccount bank) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        if (bank == null || bank.bankName.isEmpty() || bank.accountNumber.isEmpty() || bank.accountHolder.isEmpty()) {
            return Tasks.forException(new Failure("INVALID_BANK_INFO"));
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("bankAccount", bank.toMap());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        return bounded(getWalletRef(uid).set(updates, SetOptions.merge()));
    }

    public Task<Long> topUp(String uid, long amount, String note) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        if (amount <= 0) return Tasks.forException(new Failure("INVALID_AMOUNT"));

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference wRef = getWalletRef(uid);
            DocumentSnapshot wSnap = tx.get(wRef);

            long curBalance = 0L;
            if (wSnap.exists()) {
                Long b = wSnap.getLong("balance");
                if (b != null) curBalance = b;
            }

            long newBalance = curBalance + amount;
            Map<String, Object> wData = new HashMap<>();
            wData.put("balance", newBalance);
            wData.put("updatedAt", FieldValue.serverTimestamp());
            if (!wSnap.exists()) {
                wData.put("createdAt", FieldValue.serverTimestamp());
            }
            tx.set(wRef, wData, SetOptions.merge());

            String txId = "tx_topup_" + System.currentTimeMillis();
            DocumentReference txRef = getTransactionsRef(uid).document(txId);
            WalletTransaction txObj = new WalletTransaction(
                    txId,
                    WalletTransaction.TYPE_TOPUP,
                    amount,
                    newBalance,
                    note != null && !note.isEmpty() ? note : "Nạp tiền vào ví qua VietQR",
                    "",
                    Timestamp.now()
            );
            tx.set(txRef, txObj.toMap());

            return newBalance;
        }));
    }

    public Task<Long> withdraw(String uid, long amount, BankAccount bank) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        if (amount <= 0) return Tasks.forException(new Failure("INVALID_AMOUNT"));
        if (bank == null || bank.bankName.isEmpty() || bank.accountNumber.isEmpty()) {
            return Tasks.forException(new Failure("NO_BANK_ACCOUNT"));
        }

        return bounded(db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");

            DocumentReference wRef = getWalletRef(uid);
            DocumentSnapshot wSnap = tx.get(wRef);

            long curBalance = 0L;
            if (wSnap.exists()) {
                Long b = wSnap.getLong("balance");
                if (b != null) curBalance = b;
            }

            if (curBalance < amount) {
                throw new Failure("INSUFFICIENT_BALANCE");
            }

            long newBalance = curBalance - amount;
            Map<String, Object> wData = new HashMap<>();
            wData.put("balance", newBalance);
            wData.put("updatedAt", FieldValue.serverTimestamp());
            tx.set(wRef, wData, SetOptions.merge());

            String txId = "tx_withdraw_" + System.currentTimeMillis();
            DocumentReference txRef = getTransactionsRef(uid).document(txId);
            String desc = "Rút tiền về " + bank.bankName + " (" + bank.getMaskedNumber() + ")";
            WalletTransaction txObj = new WalletTransaction(
                    txId,
                    WalletTransaction.TYPE_WITHDRAW,
                    amount,
                    newBalance,
                    desc,
                    "",
                    Timestamp.now()
            );
            tx.set(txRef, txObj.toMap());

            return newBalance;
        }));
    }

    public Task<List<WalletTransaction>> getTransactions(String uid, int limit) {
        if (!same(uid)) return Tasks.forException(new Failure("SESSION"));
        Query q = getTransactionsRef(uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit > 0 ? limit : 20);

        return bounded(q.get(Source.SERVER).continueWith(t -> {
            if (!t.isSuccessful()) throw t.getException();
            List<WalletTransaction> list = new ArrayList<>();
            QuerySnapshot qs = t.getResult();
            if (qs != null) {
                for (DocumentSnapshot doc : qs) {
                    WalletTransaction item = WalletTransaction.fromSnapshot(doc);
                    if (item != null) list.add(item);
                }
            }
            return list;
        }));
    }

    public static String error(Exception ex) {
        Throwable c = ex;
        for (int i = 0; c != null && i < 10; i++, c = c.getCause()) {
            if (c instanceof Failure) {
                switch (((Failure) c).code) {
                    case "SESSION":
                        return "Phiên đăng nhập đã hết hạn. Hãy đăng nhập lại.";
                    case "INVALID_AMOUNT":
                        return "Số tiền nhập không hợp lệ.";
                    case "INSUFFICIENT_BALANCE":
                        return "Số dư ví hiện tại không đủ để thực hiện rút tiền.";
                    case "NO_BANK_ACCOUNT":
                        return "Vui lòng liên kết tài khoản ngân hàng trước khi rút tiền.";
                    case "INVALID_BANK_INFO":
                        return "Vui lòng nhập đầy đủ thông tin ngân hàng và số tài khoản.";
                    default:
                        return "Thao tác ví chưa hoàn thành. Hãy thử lại.";
                }
            }
            if (c instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException fe = (FirebaseFirestoreException) c;
                if (fe.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                    return "Quy tắc bảo mật từ chối thao tác (PERMISSION_DENIED).";
                }
            }
            if (c instanceof java.util.concurrent.TimeoutException) {
                return "Chưa thể kết nối tới máy chủ sau 45 giây. Hãy kiểm tra kết nối mạng và thử lại.";
            }
        }
        return "Đã xảy ra lỗi kết nối. Hãy thử lại sau giây lát.";
    }
}
