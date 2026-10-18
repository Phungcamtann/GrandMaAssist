package vn.edu.ueh.nhom4.grandmaassist.app;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Source;

public class MainActivity extends AppCompatActivity {

    private TextView txtFirebaseMessage;
    private Button btnReload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtFirebaseMessage = findViewById(R.id.txtFirebaseMessage);
        btnReload = findViewById(R.id.btnReload);

        btnReload.setOnClickListener(view -> loadMessage());

        loadMessage();
    }

    private void loadMessage() {
        txtFirebaseMessage.setText("Đang đọc dữ liệu từ Firebase...");
        btnReload.setEnabled(false);

        FirebaseFirestore.getInstance()
                .collection("appConfig")
                .document("public")
                .get(Source.SERVER)
                .addOnCompleteListener(this, task -> {
                    btnReload.setEnabled(true);

                    if (!task.isSuccessful()) {
                        Exception error = task.getException();

                        Log.e("GMA_FIREBASE", "Đọc Firestore thất bại", error);

                        String errorCode = "UNKNOWN";

                        if (error instanceof FirebaseFirestoreException) {
                            errorCode =
                                    ((FirebaseFirestoreException) error)
                                            .getCode()
                                            .name();
                        }

                        txtFirebaseMessage.setText(
                                "Không đọc được Firebase.\nMã lỗi: " + errorCode
                        );
                        return;
                    }

                    DocumentSnapshot document = task.getResult();

                    if (document == null || !document.exists()) {
                        txtFirebaseMessage.setText(
                                "Chưa tìm thấy document appConfig/public."
                        );
                        return;
                    }

                    Object message = document.get("message");

                    if (!(message instanceof String)
                            || ((String) message).trim().isEmpty()) {
                        txtFirebaseMessage.setText(
                                "Trường message cần là string và có nội dung."
                        );
                        return;
                    }

                    txtFirebaseMessage.setText((String) message);
                });
    }
}