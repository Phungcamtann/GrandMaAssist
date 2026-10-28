package vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import vn.edu.ueh.nhom4.grandmaassist.app.R;

/**
 * Hộp thoại hiển thị thông tin Liên hệ tổng đài hỗ trợ GrandMaAssist.
 */
public final class CallCenterDialog {

    public static void show(Context context) {
        View v = LayoutInflater.from(context).inflate(R.layout.dialog_call_center, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setView(v)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        bindCall(v, R.id.btnCallLong, "0936719323", context);
        bindCall(v, R.id.btnCallTai, "0935272284", context);
        bindCall(v, R.id.btnCallSam, "0775446799", context);
        bindCall(v, R.id.btnCallTan, "0878805819", context);

        v.findViewById(R.id.btnCloseCallCenter).setOnClickListener(view -> dialog.dismiss());
        dialog.show();
    }

    private static void bindCall(View root, int viewId, String phoneNumber, Context context) {
        View btn = root.findViewById(viewId);
        if (btn == null) return;
        btn.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
                context.startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(context, "Không thể mở ứng dụng gọi điện: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
