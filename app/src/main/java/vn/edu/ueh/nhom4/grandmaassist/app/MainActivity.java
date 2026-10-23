package vn.edu.ueh.nhom4.grandmaassist.app;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.GetCredentialInterruptedException;
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException;
import androidx.credentials.exceptions.GetCredentialUnsupportedException;
import androidx.credentials.exceptions.NoCredentialException;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;

import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.AuthRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.AuthViewModel;

public class MainActivity extends AppCompatActivity {
    private AuthViewModel model;
    private CancellationSignal pickerCancellation;
    private long pickerRequest;
    private View loginContent;
    private View accountContent;
    private View progress;
    private TextView accountName;
    private TextView accountEmail;
    private TextView accountProvider;
    private TextView status;
    private AppCompatButton googleButton;
    private AppCompatButton signOutButton;
    private AppCompatButton retryButton;
    private Boolean showingAccount;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.main);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safe = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);

        loginContent = findViewById(R.id.loginContent);
        accountContent = findViewById(R.id.accountContent);
        accountContent.setSaveFromParentEnabled(false);
        accountName = findViewById(R.id.accountName);
        accountEmail = findViewById(R.id.accountEmail);
        accountProvider = findViewById(R.id.accountProvider);
        progress = findViewById(R.id.authProgress);
        status = findViewById(R.id.authStatus);
        googleButton = findViewById(R.id.btnGoogle);
        signOutButton = findViewById(R.id.btnSignOut);
        retryButton = findViewById(R.id.btnRetry);

        Drawable googleIcon = ContextCompat.getDrawable(this, R.drawable.google_g);
        if (googleIcon != null) {
            int size = getResources().getDimensionPixelSize(R.dimen.google_icon_size);
            float scale = (float) size / Math.max(googleIcon.getIntrinsicWidth(), googleIcon.getIntrinsicHeight());
            googleIcon.setBounds(0, 0, Math.round(googleIcon.getIntrinsicWidth() * scale),
                    Math.round(googleIcon.getIntrinsicHeight() * scale));
            googleButton.setCompoundDrawablesRelative(googleIcon, null, null, null);
        }
        model = new ViewModelProvider(this).get(AuthViewModel.class);
        googleButton.setOnClickListener(view -> launchGooglePicker());
        signOutButton.setOnClickListener(view -> model.signOut());
        retryButton.setOnClickListener(view -> model.retry());
        model.getState().observe(this, this::render);
    }

    @Override protected void onStart() {
        super.onStart();
        model.onForeground();
    }

    private void launchGooglePicker() {
        long request = model.beginGooglePicker();
        if (request == 0) return;
        pickerRequest = request;
        pickerCancellation = new CancellationSignal();
        try {
            // The explicit button flow supports existing, new and additional Google accounts.
            // No automatic picker is launched by rendering, onStart, or Activity recreation.
            GetSignInWithGoogleOption option = new GetSignInWithGoogleOption.Builder(
                    getString(R.string.default_web_client_id)).build();
            GetCredentialRequest credentialRequest = new GetCredentialRequest.Builder()
                    .addCredentialOption(option).build();
            CredentialManager.create(this).getCredentialAsync(this, credentialRequest,
                    pickerCancellation, ContextCompat.getMainExecutor(this),
                    new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                        @Override public void onResult(GetCredentialResponse result) {
                            if (!acceptPickerResult(request)) return;
                            releasePicker();
                            model.signIn(request, result.getCredential());
                        }

                        @Override public void onError(@NonNull GetCredentialException error) {
                            if (!acceptPickerResult(request)) return;
                            releasePicker();
                            if (error instanceof GetCredentialCancellationException) {
                                model.pickerFailed(request, R.string.auth_cancelled, false);
                            } else if (error instanceof NoCredentialException) {
                                model.pickerFailed(request, R.string.auth_no_credential, true);
                            } else if (error instanceof GetCredentialInterruptedException) {
                                model.pickerInterrupted(request);
                            } else if (error instanceof GetCredentialProviderConfigurationException
                                    || error instanceof GetCredentialUnsupportedException) {
                                model.pickerFailed(request, R.string.auth_provider_unavailable, true);
                            } else {
                                // Provider errors may not expose a reliable network/configuration cause.
                                model.pickerFailed(request, R.string.auth_picker_error, true);
                            }
                        }
                    });
        } catch (IllegalArgumentException e) {
            releasePicker();
            model.pickerFailed(request, R.string.auth_configuration_error, true);
        } catch (RuntimeException e) {
            releasePicker();
            model.pickerFailed(request, R.string.auth_picker_error, true);
        }
    }

    private boolean acceptPickerResult(long request) {
        return !isDestroyed() && !isFinishing() && pickerRequest == request;
    }

    private void releasePicker() {
        pickerRequest = 0;
        pickerCancellation = null;
    }

    private void render(AuthRepository.State state) {
        boolean hasAccount = state.account != null;
        loginContent.setVisibility(hasAccount ? View.GONE : View.VISIBLE);
        accountContent.setVisibility(hasAccount ? View.VISIBLE : View.GONE);
        // Clear old values even when the account group is hidden.
        accountName.setText(hasAccount ? valueOrFallback(state.account.name, R.string.auth_name_missing) : "");
        accountEmail.setText(hasAccount ? valueOrFallback(state.account.email, R.string.auth_email_missing) : "");
        accountProvider.setText(hasAccount && state.account.google
                ? R.string.auth_provider_google : R.string.auth_provider_unknown);
        googleButton.setEnabled(state.phase == AuthRepository.Phase.READY);
        signOutButton.setEnabled(state.phase == AuthRepository.Phase.ACCOUNT);
        progress.setVisibility(state.isBusy() ? View.VISIBLE : View.GONE);
        status.setVisibility(state.message == 0 ? View.GONE : View.VISIBLE);
        if (state.message != 0) status.setText(state.message);
        else status.setText("");
        status.setTextColor(ContextCompat.getColor(this,
                state.error ? R.color.text_error : R.color.text_charcoal));
        retryButton.setVisibility(state.retry == AuthRepository.Retry.NONE ? View.GONE : View.VISIBLE);
        retryButton.setText(state.retry == AuthRepository.Retry.CLEAR_CREDENTIALS
                ? R.string.auth_retry_clear : R.string.auth_retry_session);
        if (showingAccount != null && showingAccount != hasAccount) {
            findViewById(R.id.main).scrollTo(0, 0);
        }
        showingAccount = hasAccount;
    }

    private String valueOrFallback(String value, int fallback) {
        return value == null || value.trim().isEmpty() ? getString(fallback) : value;
    }

    @Override protected void onDestroy() {
        if (pickerRequest != 0) {
            // Cancel only the Activity-owned picker. A Firebase exchange already accepted by
            // the repository continues across rotation without retaining this Activity.
            long request = pickerRequest;
            CancellationSignal cancellation = pickerCancellation;
            releasePicker();
            model.pickerInterrupted(request);
            if (cancellation != null) cancellation.cancel();
        }
        super.onDestroy();
    }
}
