package vn.edu.ueh.nhom4.grandmaassist.app.ui.wallet;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.WalletTransaction;

public final class WalletTransactionAdapter extends RecyclerView.Adapter<WalletTransactionAdapter.ViewHolder> {
    private final List<WalletTransaction> items = new ArrayList<>();
    private final DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, new Locale("vi", "VN"));

    public void submitList(List<WalletTransaction> list) {
        items.clear();
        if (list != null) items.addAll(list);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_wallet_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        WalletTransaction tx = items.get(position);
        holder.tvTitle.setText(tx.description != null && !tx.description.isEmpty() ? tx.description : tx.getTypeLabel());

        if (tx.createdAt != null) {
            holder.tvTime.setText(df.format(tx.createdAt.toDate()));
        } else {
            holder.tvTime.setText("Vừa xong");
        }

        holder.tvAmount.setText(tx.getFormattedAmount());

        if (tx.isPositive()) {
            holder.tvAmount.setTextColor(Color.parseColor("#2E7D32")); // Green
            holder.ivIcon.setImageResource(R.drawable.ic_arrow_down_green);
        } else if (WalletTransaction.TYPE_HOLD_ESCROW.equals(tx.type)) {
            holder.tvAmount.setTextColor(Color.parseColor("#C62828")); // Red for escrow
            holder.ivIcon.setImageResource(R.drawable.ic_arrow_up_orange);
        } else {
            holder.tvAmount.setTextColor(Color.parseColor("#E65100")); // Orange for withdraw
            holder.ivIcon.setImageResource(R.drawable.ic_arrow_up_orange);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvTime;
        final TextView tvAmount;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.ivTxIcon);
            tvTitle = itemView.findViewById(R.id.tvTxTitle);
            tvTime = itemView.findViewById(R.id.tvTxTime);
            tvAmount = itemView.findViewById(R.id.tvTxAmount);
        }
    }
}
