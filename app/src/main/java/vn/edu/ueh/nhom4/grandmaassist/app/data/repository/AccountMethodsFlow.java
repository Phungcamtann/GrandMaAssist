package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import androidx.annotation.MainThread;
import androidx.credentials.Credential;
import androidx.credentials.CustomCredential;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import java.util.concurrent.Executor;

import vn.edu.ueh.nhom4.grandmaassist.app.R;

/** Main-thread, application-owned C03 coordinator. All sensitive state is memory-only.
 * A submitted Firebase Task is never cancelled/rolled back by navigation.
 */
@MainThread
public final class AccountMethodsFlow {
    public enum Purpose { NONE, LINK_PHONE, LINK_GOOGLE, REAUTH_GOOGLE, REAUTH_PHONE, UPDATE_PHONE }
    public enum Step { IDLE, CHOOSE_REAUTH, PHONE, GOOGLE_READY, PICKING, CONFIRM_GOOGLE, APPLYING, REFRESHING, RECONCILE }

    public static final class State {
        public final Purpose purpose;
        public final Step step;
        public final int message;
        public final boolean error;
        public final String googleIdentity;
        public final String currentPhone;
        public final boolean googleAvailable;
        public final boolean phoneAvailable;
        private final boolean inFlight;

        private State(AccountMethodsFlow flow) {
            purpose = flow.purpose;
            step = flow.step;
            message = flow.message;
            error = flow.error;
            googleIdentity = flow.googleIdentity;
            currentPhone = flow.currentPhone;
            FirebaseUser user = flow.user();
            googleAvailable = has(user, GoogleAuthProvider.PROVIDER_ID);
            phoneAvailable = has(user, PhoneAuthProvider.PROVIDER_ID) && user.getPhoneNumber() != null;
            inFlight = flow.taskInFlight;
        }

        public boolean active() { return step != Step.IDLE; }
        public boolean busy() { return inFlight || step == Step.APPLYING || step == Step.REFRESHING; }
    }

    private final FirebaseAuth auth;
    private final Executor executor;
    private final Runnable accountChanged;
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private final PhoneSignInFlow phone = new PhoneSignInFlow(this::acceptPhone);
    private Purpose purpose = Purpose.NONE;
    private Purpose afterReauth = Purpose.UPDATE_PHONE;
    private Step step = Step.IDLE;
    private String expectedUid;
    private String currentPhone = "";
    private String googleIdentity = "";
    private AuthCredential pendingGoogle;
    private long generation;
    private int message;
    private boolean error;
    private boolean mutationApplied;
    private boolean taskInFlight;

    AccountMethodsFlow(FirebaseAuth auth, Executor executor, Runnable accountChanged) {
        this.auth = auth;
        this.executor = executor;
        this.accountChanged = accountChanged;
        publish();
    }

    public LiveData<State> getState() { return state; }
    public LiveData<PhoneSignInFlow.State> getPhoneState() { return phone.getState(); }
    public boolean active() { return step != Step.IDLE; }
    public boolean busy() { return taskInFlight || step == Step.APPLYING || step == Step.REFRESHING; }
    private FirebaseUser user() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null && (expectedUid == null || expectedUid.equals(user.getUid())) ? user : null;
    }
    private static boolean has(FirebaseUser user, String provider) {
        return user != null && user.getProviderData().stream().anyMatch(p -> provider.equals(p.getProviderId()));
    }
    private boolean valid() {
        if (expectedUid != null && user() != null) return true;
        reset(R.string.c03_session_changed, true);
        return false;
    }
    private boolean accepts(long request) { return request == generation && valid(); }

    public void begin(boolean google) {
        if (active() || busy() || auth.getCurrentUser() == null) return;
        FirebaseUser user = auth.getCurrentUser();
        expectedUid = user.getUid();
        currentPhone = user.getPhoneNumber() == null ? "" : user.getPhoneNumber();
        mutationApplied = false;
        afterReauth = Purpose.UPDATE_PHONE;
        ++generation;
        if (google) {
            if (has(user, GoogleAuthProvider.PROVIDER_ID)) { reset(R.string.c03_already_linked, false); return; }
            purpose = Purpose.LINK_GOOGLE;
            step = Step.GOOGLE_READY;
        } else if (has(user, PhoneAuthProvider.PROVIDER_ID)) {
            purpose = Purpose.NONE;
            step = Step.CHOOSE_REAUTH;
        } else {
            purpose = Purpose.LINK_PHONE;
            phone.cancel(true);
            step = Step.PHONE;
        }
        show(0, false);
    }

    public void chooseReauth(boolean google) {
        if (step != Step.CHOOSE_REAUTH || !valid()) return;
        FirebaseUser user = user();
        if (!has(user, google ? GoogleAuthProvider.PROVIDER_ID : PhoneAuthProvider.PROVIDER_ID)) return;
        ++generation;
        if (google) {
            purpose = Purpose.REAUTH_GOOGLE;
            step = Step.GOOGLE_READY;
        } else {
            if (user.getPhoneNumber() == null) return;
            currentPhone = user.getPhoneNumber();
            purpose = Purpose.REAUTH_PHONE;
            phone.reviewFixed(currentPhone);
            step = Step.PHONE;
        }
        show(0, false);
    }

    public long beginPicker() {
        if (step != Step.GOOGLE_READY || !valid()) return 0;
        step = Step.PICKING;
        show(R.string.auth_choosing, false);
        return -(++generation);
    }
    public void pickerFailed(long request, int text, boolean failed) {
        if (step != Step.PICKING || !accepts(-request)) return;
        ++generation;
        step = Step.GOOGLE_READY;
        if (text == R.string.auth_cancelled) text = R.string.c03_picker_cancelled;
        else if (text == R.string.auth_interrupted) text = R.string.c03_picker_interrupted;
        else if (text == R.string.auth_invalid_credential) text = R.string.c03_google_invalid;
        show(text, failed);
    }
    public void picked(long request, Credential credential) {
        if (step != Step.PICKING || !accepts(-request)) return;
        if (!(credential instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
            pickerFailed(request, R.string.auth_invalid_credential, true); return;
        }
        try {
            GoogleIdTokenCredential google = GoogleIdTokenCredential.createFrom(((CustomCredential) credential).getData());
            if (google.getIdToken().trim().isEmpty()) throw new IllegalArgumentException();
            pendingGoogle = GoogleAuthProvider.getCredential(google.getIdToken(), null);
            googleIdentity = google.getId();
        } catch (IllegalArgumentException | NullPointerException exception) {
            pickerFailed(request, R.string.auth_invalid_credential, true); return;
        }
        step = Step.CONFIRM_GOOGLE;
        show(0, false);
    }
    public void confirmGoogle() {
        if (step != Step.CONFIRM_GOOGLE || pendingGoogle == null || !valid()) return;
        AuthCredential credential = pendingGoogle;
        pendingGoogle = null;
        googleIdentity = "";
        apply(credential, -1);
    }

    public void updateInput(String input) {
        if (step == Step.PHONE && purpose != Purpose.REAUTH_PHONE && valid()) phone.updateInput(input);
    }
    public void review() {
        if (step != Step.PHONE || purpose == Purpose.REAUTH_PHONE || !valid()) return;
        String normalized = PhoneSignInFlow.normalize(phone.getState().getValue().input);
        if (purpose == Purpose.UPDATE_PHONE && currentPhone.equals(normalized)) {
            show(R.string.c03_same_phone, true); return;
        }
        show(0, false);
        phone.review();
    }
    public PhoneSignInFlow.Request send(boolean resend) {
        return step == Step.PHONE && valid() ? phone.send(resend) : null;
    }
    public PhoneSignInFlow.Request reattach() {
        return step == Step.PHONE && valid() ? phone.reattach() : null;
    }
    public void verify(String code) { if (step == Step.PHONE && valid()) phone.verify(code); }
    public void editPhone() {
        if (step != Step.PHONE || !valid()) return;
        if (purpose == Purpose.REAUTH_PHONE) { cancel(); return; }
        phone.cancel(false);
        show(0, false);
    }
    private boolean acceptPhone(long phoneGeneration, PhoneAuthCredential credential) {
        if (step != Step.PHONE || !valid()) return false;
        if (purpose != Purpose.LINK_PHONE && purpose != Purpose.REAUTH_PHONE && purpose != Purpose.UPDATE_PHONE) return false;
        apply(credential, phoneGeneration);
        return true;
    }

    private void apply(AuthCredential credential, long phoneGeneration) {
        if (!valid() || busy()) return;
        FirebaseUser user = user();
        Purpose operation = purpose;
        if ((operation == Purpose.LINK_GOOGLE && has(user, GoogleAuthProvider.PROVIDER_ID))
                || (operation == Purpose.LINK_PHONE && has(user, PhoneAuthProvider.PROVIDER_ID))) {
            reconcile(R.string.c03_already_linked, false); return;
        }
        long request = ++generation;
        step = Step.APPLYING;
        taskInFlight = true;
        show(R.string.c03_applying, false);
        Task<?> task;
        if (operation == Purpose.LINK_GOOGLE || operation == Purpose.LINK_PHONE) task = user.linkWithCredential(credential);
        else if (operation == Purpose.UPDATE_PHONE) task = user.updatePhoneNumber((PhoneAuthCredential) credential);
        else task = user.reauthenticate(credential);
        task.addOnCompleteListener(executor, result -> {
            taskInFlight = false;
            if (!accepts(request)) { publish(); return; }
            if (!result.isSuccessful()) {
                Exception failure = result.getException();
                String code = failure instanceof FirebaseAuthException ? ((FirebaseAuthException) failure).getErrorCode() : "";
                if ("ERROR_REQUIRES_RECENT_LOGIN".equals(code)) {
                    if (operation != Purpose.REAUTH_GOOGLE && operation != Purpose.REAUTH_PHONE) afterReauth = operation;
                    phone.cancel(true);
                    purpose = Purpose.NONE;
                    step = Step.CHOOSE_REAUTH;
                    show(R.string.c03_recent_login, true);
                } else if (!(failure instanceof FirebaseAuthException)
                        || failure instanceof FirebaseNetworkException || "ERROR_INTERNAL_ERROR".equals(code)) {
                    // An interrupted response does not prove the mutation was rejected.
                    reconcile(R.string.c03_unknown_result, true);
                } else if ("ERROR_PROVIDER_ALREADY_LINKED".equals(code)) {
                    reconcile(R.string.c03_already_linked, false);
                } else {
                    step = phoneGeneration >= 0 ? Step.PHONE : Step.GOOGLE_READY;
                    if (phoneGeneration >= 0) phone.signInFailed(phoneGeneration, failure);
                    int text = failure instanceof FirebaseAuthUserCollisionException
                            || "ERROR_CREDENTIAL_ALREADY_IN_USE".equals(code)
                            || "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL".equals(code)
                            ? R.string.c03_conflict : (operation == Purpose.REAUTH_GOOGLE ? R.string.c03_wrong_google
                            : operation == Purpose.LINK_GOOGLE ? R.string.c03_google_error : PhoneSignInFlow.errorMessage(failure));
                    show(text, true);
                }
                return;
            }
            phone.cancel(true); // Retires old-number callbacks before opening new-number entry.
            if (operation == Purpose.REAUTH_GOOGLE || operation == Purpose.REAUTH_PHONE) {
                purpose = afterReauth;
                ++generation;
                step = purpose == Purpose.LINK_GOOGLE ? Step.GOOGLE_READY : Step.PHONE;
                show(R.string.c03_reauthenticated, false);
            } else {
                mutationApplied = true;
                step = Step.RECONCILE;
                refresh();
            }
        });
    }

    private void reconcile(int text, boolean failed) {
        phone.cancel(true);
        pendingGoogle = null;
        googleIdentity = "";
        step = Step.RECONCILE;
        show(text, failed);
        accountChanged.run();
    }
    /** Read-only retry. Never repeats a mutation or starts SMS. */
    public void refresh() {
        if (!valid() || busy()) return;
        FirebaseUser user = user();
        long request = ++generation;
        step = Step.REFRESHING;
        show(R.string.c03_refreshing, false);
        user.reload().addOnCompleteListener(executor, reload -> {
            if (!accepts(request)) return;
            if (!reload.isSuccessful()) { refreshFailed(); return; }
            FirebaseUser refreshed = user();
            refreshed.getIdToken(true).addOnCompleteListener(executor, token -> {
                if (!accepts(request)) return;
                if (!token.isSuccessful()) { refreshFailed(); return; }
                boolean applied = mutationApplied;
                reset(applied ? R.string.c03_success : R.string.c03_reconciled, false);
                accountChanged.run();
            });
        });
    }
    private void refreshFailed() {
        reconcile(mutationApplied ? R.string.c03_applied_refresh_failed : R.string.c03_unknown_result, true);
    }
    public void cancel() {
        if (busy()) { show(R.string.c03_finishing, false); return; }
        if (step == Step.RECONCILE) { refresh(); return; }
        reset(R.string.c03_cancelled, false);
    }
    public void sessionChanged() {
        if (expectedUid != null && user() == null) reset(R.string.c03_session_changed, true);
    }
    public void clear() { reset(0, false); }
    public void processRestored(boolean wasActive) {
        if (wasActive && !active() && !busy() && message == 0) show(R.string.c03_restored, false);
    }
    private void reset(int text, boolean failed) {
        ++generation;
        purpose = Purpose.NONE;
        step = Step.IDLE;
        expectedUid = null;
        currentPhone = "";
        pendingGoogle = null;
        googleIdentity = "";
        mutationApplied = false;
        phone.cancel(true);
        show(text, failed);
    }
    private void show(int text, boolean failed) { message = text; error = failed; publish(); }
    private void publish() { state.setValue(new State(this)); }
}
