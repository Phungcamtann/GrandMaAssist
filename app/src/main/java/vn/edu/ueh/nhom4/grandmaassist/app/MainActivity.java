package vn.edu.ueh.nhom4.grandmaassist.app;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatEditText;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.PhoneAuthOptions;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.TimeUnit;

import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.AuthRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.AccountMethodsFlow;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.PhoneSignInFlow;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.AuthViewModel;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.ProfileViewModel;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.profile.ProfilePanel;

public class MainActivity extends AppCompatActivity {
    private AuthViewModel model;
    private ProfilePanel profilePanel;
    private ProfileViewModel profileModel;
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
    private AppCompatEditText phoneInput;
    private AppCompatEditText otpInput;
    private AppCompatButton phoneTab;
    private AppCompatButton googleTab;
    private TextView phoneStatus;
    private TextView codeStatus;
    private OnBackPressedCallback phoneBack;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    private long renderedPhoneGeneration = -1;
    private boolean renderedOtpStep;
    private final Runnable countdown = new Runnable() {
        @Override public void run() {
            if (model != null) renderPhone();
            uiHandler.postDelayed(this, 1_000L);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_main);
        View root = findViewById(R.id.appRoot);
        ViewCompat.setOnApplyWindowInsetsListener(root, (view, windowInsets) -> {
            Insets safe = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout() | WindowInsetsCompat.Type.ime());
            view.setPadding(safe.left, safe.top, safe.right, safe.bottom);
            if(profilePanel!=null)profilePanel.render(model.getState().getValue());
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(root);

        loginContent = findViewById(R.id.loginContent);
        // Sensitive form values are never restored from the Android view hierarchy.
        loginContent.setSaveFromParentEnabled(false);
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
        bindPhoneViews();
        findViewById(R.id.phoneForms).setSaveFromParentEnabled(false);
        findViewById(R.id.methodsContent).setSaveFromParentEnabled(false);
        findViewById(R.id.btnAccountPhone).setOnClickListener(v -> model.beginAccountMethod(false));
        findViewById(R.id.btnAccountGoogle).setOnClickListener(v -> model.beginAccountMethod(true));
        findViewById(R.id.btnReauthGoogle).setOnClickListener(v -> model.methods().chooseReauth(true));
        findViewById(R.id.btnReauthPhone).setOnClickListener(v -> model.methods().chooseReauth(false));
        findViewById(R.id.btnMethodsGoogle).setOnClickListener(v -> launchGooglePicker());
        findViewById(R.id.btnConfirmGoogle).setOnClickListener(v -> model.methods().confirmGoogle());
        findViewById(R.id.btnRefreshMethods).setOnClickListener(v -> model.methods().refresh());
        findViewById(R.id.btnCancelMethods).setOnClickListener(v -> { hideKeyboard(); model.methods().cancel(); });
        model.methods().processRestored(savedInstanceState != null
                && savedInstanceState.getBoolean("account_flow_was_active", false));
        model.restorePhoneProcess(savedInstanceState != null
                && savedInstanceState.getBoolean("phone_flow_was_active", false));
        if (savedInstanceState == null && model.getState().getValue().phase == AuthRepository.Phase.READY
                && model.getPhoneState().getValue().step == PhoneSignInFlow.Step.ENTRY
                && model.getPhoneState().getValue().method != PhoneSignInFlow.Method.PHONE) {
            model.selectMethod(PhoneSignInFlow.Method.PHONE);
        }
        googleButton.setOnClickListener(view -> launchGooglePicker());
        profileModel = new ViewModelProvider(this).get(ProfileViewModel.class);
        ProfileViewModel profiles = profileModel;
        findViewById(R.id.btnBrowseJobs).setOnClickListener(v -> {
            AuthRepository.State state=model.getState().getValue();
            if(state.isBusy() || model.methods().active())return;
            String role=state.account!=null && profileModel.snapshot!=null?profileModel.snapshot.role:"";
            startActivity(new android.content.Intent(this,vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.PublicJobsActivity.class)
                    .putExtra(vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.PublicJobsActivity.ROLE,role));
        });
        profilePanel = new ProfilePanel(this, profiles, model);
        profiles.changes().observe(this, ignored -> render(model.getState().getValue()));
        retryButton.setOnClickListener(view -> model.retry());
        model.methods().getState().observe(this, ignored -> render(model.getState().getValue()));
        model.methods().getPhoneState().observe(this, ignored -> render(model.getState().getValue()));
        model.getState().observe(this, this::render);
        model.getPhoneState().observe(this, ignored -> render(model.getState().getValue()));
    }

    @Override protected void onStart() {
        super.onStart();
        model.onForeground();
        // SDK callbacks detach onStop. Re-enter only the known original window without a
        // force token; outside that window, never send automatically on foreground/rotation.
        launchPhoneRequest(model.reattachPhone());
        uiHandler.post(countdown);
        if(profilePanel!=null)profilePanel.onForeground();
    }

    @Override protected void onStop() {
        uiHandler.removeCallbacks(countdown);
        super.onStop();
    }

    private void bindPhoneViews() {
        phoneInput = findViewById(R.id.inputPhone);
        otpInput = findViewById(R.id.inputOtp);
        phoneTab = findViewById(R.id.tabPhone);
        googleTab = findViewById(R.id.tabGoogle);
        phoneStatus = findViewById(R.id.phoneStatus);
        codeStatus = findViewById(R.id.codeStatus);
        phoneTab.setOnClickListener(view -> changeMethod(PhoneSignInFlow.Method.PHONE));
        googleTab.setOnClickListener(view -> changeMethod(PhoneSignInFlow.Method.GOOGLE));
        phoneInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                model.updatePhoneInput(s.toString());
            }
            @Override public void afterTextChanged(Editable text) { }
        });
        findViewById(R.id.btnReviewPhone).setOnClickListener(view -> {
            hideKeyboard();
            model.reviewPhone();
        });
        findViewById(R.id.btnSendSms).setOnClickListener(view -> {
            hideKeyboard();
            launchPhoneRequest(model.sendPhone(false));
        });
        findViewById(R.id.btnResendOtp).setOnClickListener(view -> {
            otpInput.setText("");
            launchPhoneRequest(model.sendPhone(true));
        });
        findViewById(R.id.btnVerifyOtp).setOnClickListener(view -> {
            hideKeyboard();
            String code = otpInput.getText() == null ? "" : otpInput.getText().toString();
            otpInput.setText("");
            model.verifyPhone(code);
        });
        View.OnClickListener edit = view -> {
            hideKeyboard();
            otpInput.setText("");
            model.cancelPhone();
        };
        findViewById(R.id.btnEditReviewPhone).setOnClickListener(edit);
        findViewById(R.id.btnEditCodePhone).setOnClickListener(edit);
        phoneBack = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() {
                hideKeyboard();
                otpInput.setText("");
                if (model.methods().active()) model.methods().cancel();
                else model.cancelPhone();
            }
        };
        getOnBackPressedDispatcher().addCallback(this, phoneBack);
    }

    private void changeMethod(PhoneSignInFlow.Method method) {
        hideKeyboard();
        otpInput.setText("");
        model.selectMethod(method);
    }

    private void launchPhoneRequest(PhoneSignInFlow.Request request) {
        if (request == null) return;
        try {
            FirebaseAuth auth = FirebaseAuth.getInstance();
            auth.setLanguageCode("vi");
            PhoneAuthOptions.Builder options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(request.number).setTimeout(60L, TimeUnit.SECONDS)
                    .setActivity(this).setCallbacks(request.callbacks);
            if (request.resendToken != null) options.setForceResendingToken(request.resendToken);
            PhoneAuthProvider.verifyPhoneNumber(options.build());
        } catch (RuntimeException error) {
            request.failedToStart();
        }
    }

    private void hideKeyboard() {
        InputMethodManager keyboard = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.hideSoftInputFromWindow(findViewById(R.id.main).getWindowToken(), 0);
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
        int provider = R.string.auth_provider_unknown;
        if (hasAccount) {
            if (state.account.google && state.account.phone) provider = R.string.phone_and_google_provider;
            else if (state.account.google) provider = R.string.auth_provider_google;
            else if (state.account.phone) provider = R.string.phone_provider;
        }
        accountProvider.setText(provider);
        boolean hasPhone = hasAccount && state.account.phone;
        findViewById(R.id.accountPhoneLabel).setVisibility(hasPhone ? View.VISIBLE : View.GONE);
        TextView accountPhone = findViewById(R.id.accountPhone);
        accountPhone.setVisibility(hasPhone ? View.VISIBLE : View.GONE);
        accountPhone.setText(hasPhone ? valueOrFallback(state.account.phoneNumber, R.string.phone_number_missing) : "");
        googleButton.setEnabled(state.phase == AuthRepository.Phase.READY);
        signOutButton.setEnabled(state.phase == AuthRepository.Phase.ACCOUNT && !model.methods().busy());
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
        renderMethods(state);
        renderPhone();
        if (profilePanel != null) profilePanel.render(state);
        boolean browseGuest=!hasAccount && state.phase==AuthRepository.Phase.READY
                && model.getPhoneState().getValue().step==PhoneSignInFlow.Step.ENTRY;
        boolean browseOffline=hasAccount && profileModel!=null
                && (profileModel.screen==ProfileViewModel.Screen.ERROR || profileModel.screen==ProfileViewModel.Screen.LOADING);
        View browse=findViewById(R.id.btnBrowseJobs);
        browse.setVisibility((browseGuest || browseOffline) && !model.methods().active()?View.VISIBLE:View.GONE);
        browse.setEnabled(!state.isBusy());
        ((TextView)browse).setText(hasAccount?R.string.pb_open:R.string.pb_guest);
    }

    private void renderPhone() {
        if (model == null || phoneInput == null) return;
        AccountMethodsFlow.State methods = model.methods().getState().getValue();
        boolean managing = methods.active();
        PhoneSignInFlow.State phone = managing ? model.methods().getPhoneState().getValue() : model.getPhoneState().getValue();
        AuthRepository.State auth = model.getState().getValue();
        boolean visible = auth.account == null || (managing && methods.step == AccountMethodsFlow.Step.PHONE);
        findViewById(R.id.phoneForms).setVisibility(visible ? View.VISIBLE : View.GONE);
        boolean selected = phone.method == PhoneSignInFlow.Method.PHONE;
        boolean ready = managing ? methods.step == AccountMethodsFlow.Step.PHONE : auth.phase == AuthRepository.Phase.READY;
        phoneTab.setSelected(selected);
        googleTab.setSelected(!selected);
        ViewCompat.setStateDescription(phoneTab, selected ? getString(R.string.phone_selected) : null);
        ViewCompat.setStateDescription(googleTab, !selected ? getString(R.string.phone_selected) : null);
        phoneTab.setEnabled(ready);
        googleTab.setEnabled(ready);
        findViewById(R.id.googleContent).setVisibility(!selected ? View.VISIBLE : View.GONE);
        findViewById(R.id.phoneEntry).setVisibility(selected && phone.step == PhoneSignInFlow.Step.ENTRY ? View.VISIBLE : View.GONE);
        findViewById(R.id.phoneReview).setVisibility(selected && phone.step == PhoneSignInFlow.Step.REVIEW ? View.VISIBLE : View.GONE);
        boolean codeStep = selected && (phone.step == PhoneSignInFlow.Step.SENDING
                || phone.step == PhoneSignInFlow.Step.CODE || phone.step == PhoneSignInFlow.Step.VERIFYING);
        findViewById(R.id.phoneCode).setVisibility(codeStep ? View.VISIBLE : View.GONE);
        if (!phoneInput.getText().toString().equals(phone.input)) phoneInput.setText(phone.input);
        if (renderedPhoneGeneration != phone.generation || renderedOtpStep != codeStep || !visible
                || phone.step == PhoneSignInFlow.Step.VERIFYING) {
            otpInput.setText("");
        }
        renderedPhoneGeneration = phone.generation;
        renderedOtpStep = codeStep;
        ((TextView) findViewById(R.id.reviewPhoneNumber)).setText(phone.number);
        ((TextView) findViewById(R.id.codePhoneNumber)).setText(phone.number);
        ((TextView) findViewById(R.id.codeDestination)).setText(phone.codeSent
                ? R.string.phone_code_destination_sent : R.string.phone_code_destination);
        phoneInput.setEnabled(ready);
        findViewById(R.id.btnReviewPhone).setEnabled(ready);
        long wait = phone.waitSeconds();
        AppCompatButton send = findViewById(R.id.btnSendSms);
        send.setEnabled(ready && wait == 0);
        send.setText(wait == 0 ? getString(R.string.phone_confirm_send) : getString(R.string.phone_wait_send, wait));
        AppCompatButton resend = findViewById(R.id.btnResendOtp);
        resend.setEnabled(ready && phone.step == PhoneSignInFlow.Step.CODE && wait == 0);
        resend.setText(wait == 0 ? getString(R.string.phone_resend) : getString(R.string.phone_wait_resend, wait));
        boolean canVerify = ready && phone.step == PhoneSignInFlow.Step.CODE && phone.canVerify;
        otpInput.setEnabled(canVerify);
        findViewById(R.id.btnVerifyOtp).setEnabled(canVerify);
        boolean edit = ready || (auth.phase == AuthRepository.Phase.SIGNING_IN && selected);
        findViewById(R.id.btnEditReviewPhone).setEnabled(edit);
        findViewById(R.id.btnEditCodePhone).setEnabled(edit);
        phoneBack.setEnabled(managing || (visible && selected && phone.step != PhoneSignInFlow.Step.ENTRY));
        boolean oldPhone = managing && methods.purpose == AccountMethodsFlow.Purpose.REAUTH_PHONE;
        boolean updatePhone = managing && methods.purpose == AccountMethodsFlow.Purpose.UPDATE_PHONE;
        int contextTitle = oldPhone ? R.string.c03_old_phone : updatePhone ? R.string.c03_new_phone : R.string.c03_add_phone;
        ((TextView) findViewById(R.id.phoneEntryTitle)).setText(managing ? contextTitle : R.string.phone_welcome);
        ((TextView) findViewById(R.id.phoneEntryIntro)).setText(managing
                ? (updatePhone ? R.string.c03_new_phone_intro : R.string.c03_phone_intro) : R.string.phone_intro);
        ((TextView) findViewById(R.id.phoneReviewTitle)).setText(managing ? contextTitle : R.string.phone_review_title);
        ((TextView) findViewById(R.id.phoneCodeTitle)).setText(managing ? contextTitle : R.string.phone_code_title);
        ((TextView) findViewById(R.id.btnVerifyOtp)).setText(managing
                ? (oldPhone ? R.string.c03_verify_reauth : updatePhone ? R.string.c03_verify_update : R.string.c03_verify_link)
                : R.string.phone_verify);
        ((TextView) findViewById(R.id.btnEditReviewPhone)).setText(oldPhone ? R.string.c03_cancel : R.string.phone_edit_short);
        ((TextView) findViewById(R.id.btnEditCodePhone)).setText(oldPhone ? R.string.c03_cancel : R.string.phone_edit_short);
        // The normal OTP instructions are separate from transient status/errors.
        int phoneMessage = phone.message == R.string.phone_code_sent ? 0 : phone.message;
        if (managing && phoneMessage == R.string.phone_verification_cancelled) phoneMessage = R.string.c03_verification_cancelled;
        if (managing && phoneMessage == R.string.phone_session_busy) phoneMessage = R.string.c03_applying;
        phoneStatus.setVisibility(selected && !codeStep && phoneMessage != 0 ? View.VISIBLE : View.GONE);
        codeStatus.setVisibility(selected && codeStep && phoneMessage != 0 ? View.VISIBLE : View.GONE);
        String text = phoneMessage == 0 ? "" : getString(phoneMessage);
        TextView activeStatus = codeStep ? codeStatus : phoneStatus;
        TextView inactiveStatus = codeStep ? phoneStatus : codeStatus;
        inactiveStatus.setText("");
        if (!activeStatus.getText().toString().equals(text)) activeStatus.setText(text);
        activeStatus.setTextColor(ContextCompat.getColor(this, phone.error ? R.color.text_error : R.color.text_charcoal));
        progress.setVisibility(auth.isBusy() || methods.busy() || (visible && selected && phone.step == PhoneSignInFlow.Step.SENDING)
                ? View.VISIBLE : View.GONE);
    }

    private void renderMethods(AuthRepository.State auth) {
        AccountMethodsFlow.State methods = model.methods().getState().getValue();
        boolean account = auth.account != null;
        findViewById(R.id.methodsContent).setVisibility(account ? View.VISIBLE : View.GONE);
        TextView summary = findViewById(R.id.methodsSummary);
        summary.setText(account ? getString(R.string.c03_summary,
                auth.account.google ? valueOrFallback(auth.account.googleEmail, R.string.c03_google_linked) : getString(R.string.c03_not_linked),
                auth.account.phone ? valueOrFallback(auth.account.phoneNumber, R.string.phone_number_missing) : getString(R.string.c03_not_linked)) : "");
        boolean idle = !methods.active();
        AppCompatButton phone = findViewById(R.id.btnAccountPhone);
        phone.setVisibility(idle ? View.VISIBLE : View.GONE);
        phone.setText(account && auth.account.phone ? R.string.c03_change_phone : R.string.c03_add_phone);
        phone.setEnabled(!auth.checkingSession && !methods.busy());
        View google = findViewById(R.id.btnAccountGoogle);
        google.setVisibility(idle && account && !auth.account.google ? View.VISIBLE : View.GONE);
        google.setEnabled(!auth.checkingSession && !methods.busy());
        boolean choosing = methods.step == AccountMethodsFlow.Step.CHOOSE_REAUTH;
        findViewById(R.id.btnReauthGoogle).setVisibility(choosing && methods.googleAvailable ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnReauthPhone).setVisibility(choosing && methods.phoneAvailable ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnMethodsGoogle).setVisibility(methods.step == AccountMethodsFlow.Step.GOOGLE_READY ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnConfirmGoogle).setVisibility(methods.step == AccountMethodsFlow.Step.CONFIRM_GOOGLE ? View.VISIBLE : View.GONE);
        findViewById(R.id.btnRefreshMethods).setVisibility(methods.step == AccountMethodsFlow.Step.RECONCILE ? View.VISIBLE : View.GONE);
        View cancel = findViewById(R.id.btnCancelMethods);
        cancel.setVisibility(!idle && methods.step != AccountMethodsFlow.Step.RECONCILE ? View.VISIBLE : View.GONE);
        cancel.setEnabled(!methods.busy());
        TextView instructions = findViewById(R.id.methodsInstructions);
        String text = choosing ? getString(R.string.c03_choose_reauth, methods.currentPhone)
                : methods.step == AccountMethodsFlow.Step.CONFIRM_GOOGLE ? getString(
                        methods.purpose == AccountMethodsFlow.Purpose.LINK_GOOGLE
                                ? R.string.c03_google_consent : R.string.c03_google_reauth_consent, methods.googleIdentity) : "";
        instructions.setText(text);
        instructions.setVisibility(text.isEmpty() ? View.GONE : View.VISIBLE);
        TextView message = findViewById(R.id.methodsStatus);
        message.setText(account && methods.message != 0 ? getString(methods.message) : "");
        message.setVisibility(account && methods.message != 0 ? View.VISIBLE : View.GONE);
        message.setTextColor(ContextCompat.getColor(this, methods.error ? R.color.text_error : R.color.text_charcoal));
    }

    @Override protected void onSaveInstanceState(@NonNull Bundle outState) {
        PhoneSignInFlow.State phone = model.getPhoneState().getValue();
        // Only an opaque non-sensitive recovery flag, never input/OTP/id/resend token.
        outState.putBoolean("phone_flow_was_active", phone.method == PhoneSignInFlow.Method.PHONE
                && (phone.step != PhoneSignInFlow.Step.ENTRY || !phone.input.isEmpty()));
        outState.putBoolean("account_flow_was_active", model.methods().active() || model.methods().busy());
        super.onSaveInstanceState(outState);
    }

    private String valueOrFallback(String value, int fallback) {
        return value == null || value.trim().isEmpty() ? getString(fallback) : value;
    }

    @Override protected void onDestroy() {
        uiHandler.removeCallbacks(countdown);
        if (model != null && isFinishing() && model.methods().active()) model.methods().cancel();
        if (model != null && isFinishing() && !model.methods().active()
                && model.getPhoneState().getValue().step != PhoneSignInFlow.Step.ENTRY) model.cancelPhone();
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
