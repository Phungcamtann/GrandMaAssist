package vn.edu.ueh.nhom4.grandmaassist.app;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.ConnectionViewModel;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);

        TextView message = findViewById(R.id.txtFirebaseMessage);
        Button reload = findViewById(R.id.btnReload);
        ConnectionViewModel model = new ViewModelProvider(this).get(ConnectionViewModel.class);
        reload.setOnClickListener(view -> model.reload());
        model.getState().observe(this, state -> {
            reload.setEnabled(!state.loading);
            if (state.serverMessage != null) {
                message.setText(state.serverMessage);
            } else {
                message.setText(state.messageRes);
            }
        });
    }
}
