package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import com.google.firebase.Timestamp;
import java.util.HashMap;
import java.util.Map;

public final class BankAccount {
    public final String bankName;
    public final String accountNumber;
    public final String accountHolder;
    public final Timestamp linkedAt;

    public BankAccount(String bankName, String accountNumber, String accountHolder, Timestamp linkedAt) {
        this.bankName = bankName != null ? bankName.trim() : "";
        this.accountNumber = accountNumber != null ? accountNumber.trim() : "";
        this.accountHolder = accountHolder != null ? accountHolder.trim().toUpperCase() : "";
        this.linkedAt = linkedAt;
    }

    public String getMaskedNumber() {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return accountNumber != null ? accountNumber : "";
        }
        String last4 = accountNumber.substring(accountNumber.length() - 4);
        return "**** " + last4;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("bankName", bankName);
        map.put("accountNumber", accountNumber);
        map.put("accountHolder", accountHolder);
        map.put("linkedAt", linkedAt != null ? linkedAt : Timestamp.now());
        return map;
    }

    public static BankAccount fromMap(Map<?, ?> map) {
        if (map == null) return null;
        Object bName = map.get("bankName");
        Object aNum = map.get("accountNumber");
        Object aHolder = map.get("accountHolder");
        Object lAt = map.get("linkedAt");

        return new BankAccount(
                bName instanceof String ? (String) bName : "",
                aNum instanceof String ? (String) aNum : "",
                aHolder instanceof String ? (String) aHolder : "",
                lAt instanceof Timestamp ? (Timestamp) lAt : null
        );
    }
}
