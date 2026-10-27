package vn.edu.ueh.nhom4.grandmaassist.app.domain.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class WalletTransaction {
    public static final String TYPE_TOPUP = "TOPUP";
    public static final String TYPE_WITHDRAW = "WITHDRAW";
    public static final String TYPE_HOLD_ESCROW = "HOLD_ESCROW";
    public static final String TYPE_RECEIVE_PAY = "RECEIVE_PAY";
    public static final String TYPE_REFUND = "REFUND";

    public final String id;
    public final String type;
    public final long amount;
    public final long balanceAfter;
    public final String description;
    public final String refId;
    public final Timestamp createdAt;

    public WalletTransaction(String id, String type, long amount, long balanceAfter,
                             String description, String refId, Timestamp createdAt) {
        this.id = id;
        this.type = type != null ? type : TYPE_TOPUP;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.description = description != null ? description : "";
        this.refId = refId != null ? refId : "";
        this.createdAt = createdAt;
    }

    public boolean isPositive() {
        return TYPE_TOPUP.equals(type) || TYPE_RECEIVE_PAY.equals(type) || TYPE_REFUND.equals(type);
    }

    public String getTypeLabel() {
        switch (type) {
            case TYPE_TOPUP:
                return "Nạp tiền vào ví";
            case TYPE_WITHDRAW:
                return "Rút tiền về tài khoản";
            case TYPE_HOLD_ESCROW:
                return "Tạm giữ cọc ca làm";
            case TYPE_RECEIVE_PAY:
                return "Nhận tiền công ca làm";
            case TYPE_REFUND:
                return "Hoàn tiền cọc ca làm";
            default:
                return "Giao dịch ví";
        }
    }

    public String getFormattedAmount() {
        NumberFormat nf = NumberFormat.getInstance(new Locale("vi", "VN"));
        String prefix = isPositive() ? "+" : "-";
        return prefix + nf.format(amount) + " đ";
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("type", type);
        map.put("amount", amount);
        map.put("balanceAfter", balanceAfter);
        map.put("description", description);
        map.put("refId", refId);
        map.put("createdAt", createdAt != null ? createdAt : Timestamp.now());
        return map;
    }

    public static WalletTransaction fromSnapshot(DocumentSnapshot doc) {
        if (doc == null || !doc.exists()) return null;
        Long amt = doc.getLong("amount");
        Long bal = doc.getLong("balanceAfter");
        return new WalletTransaction(
                doc.getId(),
                doc.getString("type"),
                amt != null ? amt : 0L,
                bal != null ? bal : 0L,
                doc.getString("description"),
                doc.getString("refId"),
                doc.getTimestamp("createdAt")
        );
    }
}
