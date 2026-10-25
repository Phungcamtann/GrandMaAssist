package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.CancellationSignal;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.exceptions.ClearCredentialException;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import vn.edu.ueh.nhom4.grandmaassist.app.R;

/** Application-scoped owner of Auth operations. No Activity or persisted app credentials.
 * State transitions run on main; credential-provider calls run on bounded workers.
 * Request versions discard stale results.
 */
@MainThread
public final class AuthRepository {
    private static final long CLEAR_CREDENTIAL_TIMEOUT_MS = 10_000L;
    private static final String LOG_TAG = "GrandMaAuth";
    public enum Phase { READY, PICKING_GOOGLE, SIGNING_IN, CANCELLING_SIGN_IN, ACCOUNT, CLEARING_CREDENTIALS }
    public enum Retry { NONE, CHECK_SESSION, CLEAR_CREDENTIALS }

    public static final class Account {
        private final String uid;
        public final String name;
        public final String email;
        public final String googleEmail;
        public final boolean google;
        public final boolean phone;
        public final String phoneNumber;

        private Account(FirebaseUser user) {
            uid = user.getUid();
            name = user.getDisplayName();
            email = user.getEmail();
            googleEmail = user.getProviderData().stream()
                    .filter(provider -> GoogleAuthProvider.PROVIDER_ID.equals(provider.getProviderId()))
                    .map(provider -> provider.getEmail() == null ? "" : provider.getEmail()).findFirst().orElse("");
            google = user.getProviderData().stream()
                    .anyMatch(provider -> GoogleAuthProvider.PROVIDER_ID.equals(provider.getProviderId()));
            phone = user.getProviderData().stream()
                    .anyMatch(provider -> PhoneAuthProvider.PROVIDER_ID.equals(provider.getProviderId()));
            phoneNumber = user.getPhoneNumber();
        }
    }

    public static final class State {
        public final Phase phase;
        public final Account account;
        public final int message;
        public final boolean error;
        public final boolean checkingSession;
        public final Retry retry;

        private State(Phase phase, Account account, int message, boolean error,
                      boolean checkingSession, Retry retry) {
            this.phase = phase;
            this.account = account;
            this.message = message;
            this.error = error;
            this.checkingSession = checkingSession;
            this.retry = retry;
        }

        public boolean isBusy() {
            return phase == Phase.PICKING_GOOGLE || phase == Phase.SIGNING_IN
                    || phase == Phase.CANCELLING_SIGN_IN || phase == Phase.CLEARING_CREDENTIALS || checkingSession;
        }
    }

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final CredentialManager credentialManager;
    private final Executor mainExecutor;
    private final SharedPreferences recoveryFlags;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    // No queued work: a stuck system/provider call cannot build up cleanup requests that
    // execute later against a newly signed-in session. At most two workers may be blocked.
    private final Executor credentialWorker = new ThreadPoolExecutor(0, 2,
            30L, TimeUnit.SECONDS, new SynchronousQueue<>());
    private final MutableLiveData<State> state;
    private final AccountMethodsFlow methods;
    private long version;
    private boolean initialAuthObserved;
    private boolean exchangeInFlight;
    private boolean phoneExchange;
    private final PhoneSignInFlow phoneFlow = new PhoneSignInFlow(this::signInWithPhone);

    public AuthRepository(Context applicationContext) {
        Context app = applicationContext.getApplicationContext();
        credentialManager = CredentialManager.create(app);
        mainExecutor = ContextCompat.getMainExecutor(app);
        recoveryFlags = app.getSharedPreferences("auth_recovery", Context.MODE_PRIVATE);
        // An opaque cancellation intent only: no phone, OTP, UID, token or credential.
        // If the process died after Firebase persisted a cancelled sign-in but before its
        // completion callback cleaned up, do not restore that unwanted session.
        if (recoveryFlags.getBoolean("cancelled_phone_exchange", false)) {
            auth.signOut();
            recoveryFlags.edit().remove("cancelled_phone_exchange").apply();
        }
        FirebaseUser user = auth.getCurrentUser();
        state = new MutableLiveData<>(user == null
                ? ready(0, false, Retry.NONE)
                : account(user, R.string.auth_session_local, false, false, Retry.CHECK_SESSION));
        methods = new AccountMethodsFlow(auth, mainExecutor, this::publishCurrentAccount);
        // Firebase can invalidate the locally stored session during refresh.
        auth.addAuthStateListener(firebaseAuth -> {
            State current = state.getValue();
            FirebaseUser observed = firebaseAuth.getCurrentUser();
            methods.sessionChanged();
            if (current.account != null && observed != null && !current.account.uid.equals(observed.getUid())) {
                ++version;
                state.setValue(account(observed, R.string.auth_session_local, false, false, Retry.CHECK_SESSION));
                return;
            }
            if (!initialAuthObserved) {
                initialAuthObserved = true;
                if (version == 0 && current.phase == Phase.READY && observed != null) {
                    state.setValue(account(observed, R.string.auth_session_local,
                            false, false, Retry.CHECK_SESSION));
                    checkSession();
                    return;
                }
            }
            if (current.account != null && observed == null && !current.checkingSession) {
                finishSession(R.string.auth_session_expired);
            }
        });
    }

    private void publishCurrentAccount() {
        FirebaseUser user = auth.getCurrentUser();
        if (state.getValue().phase == Phase.ACCOUNT && user != null) {
            state.setValue(account(user, 0, false, false, Retry.NONE));
        }
    }
    public AccountMethodsFlow getMethods() { return methods; }
    public void beginAccountMethod(boolean google) {
        State current = state.getValue();
        if (current.phase == Phase.ACCOUNT && !current.checkingSession && !exchangeInFlight && !methods.busy()) methods.begin(google);
    }
    public LiveData<State> getState() { return state; }
    public LiveData<PhoneSignInFlow.State> getPhoneState() { return phoneFlow.getState(); }

    public void selectLoginMethod(PhoneSignInFlow.Method method) {
        if (state.getValue().phase != Phase.READY || exchangeInFlight) return;
        ++version;
        phoneFlow.select(method);
        state.setValue(ready(0, false, Retry.NONE));
    }

    public void updatePhoneInput(String value) {
        if (methods.active()) methods.updateInput(value);
        else if (state.getValue().phase == Phase.READY) phoneFlow.updateInput(value);
    }

    public void reviewPhone() { if (methods.active()) methods.review(); else if (canUsePhone()) phoneFlow.review(); }
    public PhoneSignInFlow.Request sendPhone(boolean resend) {
        return methods.active() ? methods.send(resend) : canUsePhone() ? phoneFlow.send(resend) : null;
    }
    public PhoneSignInFlow.Request reattachPhone() {
        return methods.active() ? methods.reattach() : canUsePhone() ? phoneFlow.reattach() : null;
    }
    public void verifyPhone(String code) { if (methods.active()) methods.verify(code); else if (canUsePhone()) phoneFlow.verify(code); }
    public void restorePhoneProcess(boolean active) {
        if (state.getValue().account == null) phoneFlow.processRestored(active);
    }

    private boolean canUsePhone() {
        return state.getValue().phase == Phase.READY && !exchangeInFlight
                && auth.getCurrentUser() == null;
    }

    public void cancelPhone() {
        if (methods.active()) { methods.editPhone(); return; }
        if (state.getValue().account != null) return;
        if (exchangeInFlight && !phoneExchange) return;
        if (exchangeInFlight) recoveryFlags.edit().putBoolean("cancelled_phone_exchange", true).apply();
        phoneFlow.cancel(false);
        ++version;
        if (exchangeInFlight) {
            // Firebase Tasks cannot be cancelled. Serialize auth: do not start a new sign-in
            // until this task finishes and its possibly persisted user is signed out.
            state.setValue(new State(Phase.CANCELLING_SIGN_IN, null,
                    R.string.phone_cancelling, false, false, Retry.NONE));
        } else if (state.getValue().phase == Phase.READY) {
            state.setValue(ready(0, false, Retry.NONE));
        }
    }

    private boolean signInWithPhone(long generation, PhoneAuthCredential credential) {
        if (!canUsePhone()) return false;
        long request = ++version;
        exchangeInFlight = true;
        phoneExchange = true;
        state.setValue(new State(Phase.SIGNING_IN, null, R.string.phone_verifying,
                false, false, Retry.NONE));
        auth.signInWithCredential(credential).addOnCompleteListener(mainExecutor, task -> {
            if (!task.isSuccessful()) {
                PhoneSignInFlow.logPhoneFailure("signInWithCredential", task.getException());
            }
            exchangeInFlight = false;
            phoneExchange = false;
            if (!isCurrent(request, Phase.SIGNING_IN)) {
                // No new auth exchange was allowed while this cancelled task was in flight.
                auth.signOut();
                recoveryFlags.edit().remove("cancelled_phone_exchange").apply();
                if (state.getValue().phase == Phase.CANCELLING_SIGN_IN) {
                    state.setValue(ready(R.string.phone_cancelled, false, Retry.NONE));
                }
                return;
            }
            FirebaseUser user = auth.getCurrentUser();
            if (task.isSuccessful() && user != null) {
                phoneFlow.cancel(true);
                state.setValue(account(user, R.string.auth_session_checked, false, false, Retry.NONE));
            } else {
                auth.signOut();
                phoneFlow.signInFailed(generation, task.getException());
                state.setValue(ready(0, false, Retry.NONE));
            }
        });
        return true;
    }

    public long beginGooglePicker() {
        if (methods.active()) return methods.beginPicker();
        if (state.getValue().phase != Phase.READY || exchangeInFlight
                || auth.getCurrentUser() != null
                || phoneFlow.getState().getValue().method != PhoneSignInFlow.Method.GOOGLE) return 0;
        long request = ++version;
        state.setValue(new State(Phase.PICKING_GOOGLE, null, R.string.auth_choosing,
                false, false, Retry.NONE));
        return request;
    }

    public void pickerFailed(long request, int message, boolean error) {
        if (request < 0) { methods.pickerFailed(request, message, error); return; }
        if (!isCurrent(request, Phase.PICKING_GOOGLE)) return;
        ++version;
        state.setValue(ready(message, error, Retry.NONE));
    }

    public void pickerInterrupted(long request) {
        pickerFailed(request, R.string.auth_interrupted, false);
    }

    public void signIn(long request, Credential credential) {
        if (request < 0) { methods.picked(request, credential); return; }
        if (!isCurrent(request, Phase.PICKING_GOOGLE)) return;
        if (!(credential instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            pickerFailed(request, R.string.auth_invalid_credential, true);
            return;
        }
        final GoogleIdTokenCredential google;
        try {
            google = GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
            if (google.getIdToken().trim().isEmpty()) {
                pickerFailed(request, R.string.auth_invalid_credential, true);
                return;
            }
        } catch (IllegalArgumentException | NullPointerException e) {
            pickerFailed(request, R.string.auth_invalid_credential, true);
            return;
        }
        state.setValue(new State(Phase.SIGNING_IN, null, R.string.auth_signing_in,
                false, false, Retry.NONE));
        exchangeInFlight = true;
        // Firebase verifies the Google token. It is never copied to saved state, disk or logs.
        auth.signInWithCredential(GoogleAuthProvider.getCredential(google.getIdToken(), null))
                .addOnCompleteListener(mainExecutor, task -> {
                    exchangeInFlight = false;
                    if (!isCurrent(request, Phase.SIGNING_IN)) {
                        // An obsolete completion must not restore a signed-out session.
                        if (state.getValue().account == null) auth.signOut();
                        return;
                    }
                    FirebaseUser user = auth.getCurrentUser();
                    if (task.isSuccessful() && user != null) {
                        phoneFlow.cancel(true);
                        state.setValue(account(user, R.string.auth_session_checked,
                                false, false, Retry.NONE));
                    } else {
                        auth.signOut();
                        state.setValue(ready(errorMessage(task.getException()), true, Retry.NONE));
                    }
                });
    }

    /** The local Firebase session is enough to show account details, never to grant business rights. */
    public void checkSession() {
        State current = state.getValue();
        if (current.phase != Phase.ACCOUNT || current.checkingSession || methods.active()) return;
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            finishSession(R.string.auth_session_expired);
            return;
        }
        long request = ++version;
        state.setValue(account(user, R.string.auth_session_checking, false, true, Retry.NONE));
        user.reload().addOnCompleteListener(mainExecutor, reload -> {
            if (!isCurrent(request, Phase.ACCOUNT)) return;
            if (!reload.isSuccessful()) {
                completeSessionCheck(request, user, reload.getException());
                return;
            }
            if (!sameSession(request, user)) {
                completeSessionCheck(request, user, null);
                return;
            }
            // Reload profile + force token refresh to detect revoked/invalid sessions when online.
            user.getIdToken(true).addOnCompleteListener(mainExecutor, token -> {
                completeSessionCheck(request, user, token.isSuccessful() ? null : token.getException());
            });
        });
    }

    private void completeSessionCheck(long request, FirebaseUser user, Exception error) {
        if (!isCurrent(request, Phase.ACCOUNT)) return;
        if (invalidSession(error)) {
            finishSession(errorMessage(error));
            return;
        }
        FirebaseUser current = auth.getCurrentUser();
        if (current == null || !current.getUid().equals(user.getUid())) {
            finishSession(R.string.auth_session_expired);
            return;
        }
        if (error == null) {
            state.setValue(account(current, R.string.auth_session_checked, false, false, Retry.NONE));
            return;
        }
        // Network/unknown errors do not prove that a cached session has expired.
        int message = error instanceof FirebaseNetworkException
                ? R.string.auth_session_offline : R.string.auth_session_unchecked;
        state.setValue(account(current, message, true, false, Retry.CHECK_SESSION));
    }

    public void signOut() {
        if (state.getValue().phase != Phase.ACCOUNT) return;
        if (methods.busy()) { methods.cancel(); return; }
        finishSession(R.string.auth_signed_out);
    }

    private void finishSession(int message) {
        State current = state.getValue();
        FirebaseUser currentUser = auth.getCurrentUser();
        boolean clearGoogle = currentUser != null && currentUser.getProviderData().stream()
                .anyMatch(provider -> GoogleAuthProvider.PROVIDER_ID.equals(provider.getProviderId()));
        clearGoogle = clearGoogle || (current.account != null && current.account.google);
        long request = ++version;
        methods.clear();
        phoneFlow.cancel(true);
        // Drop UI account data before Firebase notifies listeners or clearCredentialState returns.
        state.setValue(new State(Phase.CLEARING_CREDENTIALS, null,
                clearGoogle ? R.string.auth_signing_out : R.string.phone_signing_out,
                false, false, Retry.NONE));
        auth.signOut();
        if (clearGoogle) clearCredentialState(request, message);
        else state.setValue(ready(message, false, Retry.NONE));
    }

    public void retry() {
        State current = state.getValue();
        if (current.retry == Retry.CHECK_SESSION) {
            checkSession();
        } else if (current.retry == Retry.CLEAR_CREDENTIALS && current.phase == Phase.READY) {
            long request = ++version;
            state.setValue(new State(Phase.CLEARING_CREDENTIALS, null,
                    R.string.auth_clearing_credentials, false, false, Retry.NONE));
            clearCredentialState(request, R.string.auth_signed_out);
        }
    }

    private void clearCredentialState(long request, int successMessage) {
        CancellationSignal cancellation = new CancellationSignal();
        AtomicBoolean retired = new AtomicBoolean();
        Runnable timeout = () -> {
            if (!isCurrent(request, Phase.CLEARING_CREDENTIALS)) return;
            Log.d(LOG_TAG, "Credential cleanup: timeout");
            retired.set(true);
            // Firebase is already signed out. An unresponsive credential provider must not
            // lock the login screen forever or be reported as successfully cleared.
            clearFailed(request, R.string.auth_clear_timeout);
            cancelCredentialRequest(cancellation);
        };
        mainHandler.postDelayed(timeout, CLEAR_CREDENTIAL_TIMEOUT_MS);
        Runnable failed = () -> {
            retired.set(true);
            mainHandler.removeCallbacks(timeout);
            clearFailed(request, R.string.auth_clear_failed);
        };
        CredentialManagerCallback<Void, ClearCredentialException> callback =
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override public void onResult(Void result) {
                        mainHandler.post(() -> {
                            retired.set(true);
                            mainHandler.removeCallbacks(timeout);
                            if (isCurrent(request, Phase.CLEARING_CREDENTIALS)) {
                                Log.d(LOG_TAG, "Credential cleanup: provider confirmed");
                                state.setValue(ready(successMessage, false, Retry.NONE));
                            }
                        });
                    }

                    @Override public void onError(@NonNull ClearCredentialException error) {
                        mainHandler.post(failed);
                    }
                };
        try {
            credentialWorker.execute(() -> {
                if (retired.get()) return;
                try {
                    // The API's executor controls callbacks, not this invocation. Framework
                    // provider discovery/Binder calls must also run away from the UI thread.
                    Log.d(LOG_TAG, "Credential cleanup: entering provider call on worker");
                    credentialManager.clearCredentialStateAsync(new ClearCredentialStateRequest(),
                            cancellation, mainExecutor, callback);
                    Log.d(LOG_TAG, "Credential cleanup: provider call returned");
                } catch (RuntimeException e) {
                    mainHandler.post(failed);
                }
            });
        } catch (RuntimeException e) {
            // Bounded workers can reject work if earlier provider calls remain blocked.
            failed.run();
        }
    }

    private void cancelCredentialRequest(CancellationSignal cancellation) {
        try {
            credentialWorker.execute(() -> {
                try {
                    cancellation.cancel();
                } catch (RuntimeException ignored) {
                    // Cancellation can involve a provider/Binder call; never wait for it on UI.
                }
            });
        } catch (RuntimeException ignored) {
            // Both workers may be stuck. The timed-out request is already retired and
            // callbacks are guarded; cancellation is best effort and never blocks recovery.
        }
    }

    private void clearFailed(long request, int message) {
        if (isCurrent(request, Phase.CLEARING_CREDENTIALS)) {
            Log.d(LOG_TAG, "Credential cleanup: login screen released; retry available");
            state.setValue(ready(message, true, Retry.CLEAR_CREDENTIALS));
        }
    }

    private boolean isCurrent(long request, Phase phase) {
        return request == version && state.getValue().phase == phase;
    }

    private boolean sameSession(long request, FirebaseUser user) {
        FirebaseUser current = auth.getCurrentUser();
        return isCurrent(request, Phase.ACCOUNT) && current != null
                && current.getUid().equals(user.getUid());
    }

    private static State ready(int message, boolean error, Retry retry) {
        return new State(Phase.READY, null, message, error, false, retry);
    }

    private static State account(FirebaseUser user, int message, boolean error,
                                 boolean checking, Retry retry) {
        return new State(Phase.ACCOUNT, new Account(user), message, error, checking, retry);
    }

    private static boolean invalidSession(Exception error) {
        if (!(error instanceof FirebaseAuthException)) return false;
        String code = ((FirebaseAuthException) error).getErrorCode();
        return "ERROR_USER_DISABLED".equals(code) || "ERROR_USER_NOT_FOUND".equals(code)
                || "ERROR_INVALID_USER_TOKEN".equals(code) || "ERROR_USER_TOKEN_EXPIRED".equals(code);
    }

    private static int errorMessage(Exception error) {
        if (error instanceof FirebaseNetworkException) return R.string.auth_network_error;
        if (error instanceof FirebaseTooManyRequestsException) return R.string.auth_rate_limited;
        if (error instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) error).getErrorCode();
            if ("ERROR_USER_DISABLED".equals(code)) return R.string.auth_user_disabled;
            if (invalidSession(error)) return R.string.auth_session_expired;
            if ("ERROR_OPERATION_NOT_ALLOWED".equals(code)
                    || "ERROR_INVALID_OAUTH_CLIENT_ID".equals(code)
                    || "ERROR_INVALID_API_KEY".equals(code)
                    || "ERROR_APP_NOT_AUTHORIZED".equals(code)
                    || "ERROR_CONFIGURATION_NOT_FOUND".equals(code)) return R.string.auth_configuration_error;
        }
        if (error instanceof FirebaseAuthUserCollisionException) return R.string.auth_account_conflict;
        if (error instanceof FirebaseAuthInvalidCredentialsException) return R.string.auth_invalid_credential;
        return R.string.auth_unknown_error;
    }
}
