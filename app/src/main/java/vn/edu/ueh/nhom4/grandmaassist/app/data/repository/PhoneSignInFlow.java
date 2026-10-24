package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.FirebaseException;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.FirebaseTooManyRequestsException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseAuthMissingActivityForRecaptchaException;
import com.google.firebase.auth.PhoneAuthCredential;
import com.google.firebase.auth.PhoneAuthProvider;

import vn.edu.ueh.nhom4.grandmaassist.app.BuildConfig;
import vn.edu.ueh.nhom4.grandmaassist.app.R;

/** Transient phone challenge only. No Activity, OTP text, persisted number or independent session. */
@MainThread
public final class PhoneSignInFlow {
    public enum Method { PHONE, GOOGLE }
    public enum Step { ENTRY, REVIEW, SENDING, CODE, VERIFYING }

    interface CredentialConsumer { boolean accept(long generation, PhoneAuthCredential credential); }

    public static final class State {
        public final Method method;
        public final Step step;
        public final String input;
        public final String number;
        public final int message;
        public final boolean error;
        public final boolean canVerify;
        public final boolean codeSent;
        public final long resendAt;
        public final long generation;

        private State(PhoneSignInFlow flow) {
            method = flow.method;
            step = flow.step;
            input = flow.input;
            number = flow.number;
            message = flow.message;
            error = flow.error;
            canVerify = flow.verificationId != null;
            codeSent = flow.codeSent;
            resendAt = flow.resendAt;
            generation = flow.generation;
        }

        public long waitSeconds() {
            return Math.max(0L, (resendAt - SystemClock.elapsedRealtime() + 999L) / 1000L);
        }
    }

    /** One short-lived SDK invocation; never persist/parcel this object or log its fields. */
    public static final class Request {
        public final String number;
        public final PhoneAuthProvider.ForceResendingToken resendToken;
        public final PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks;

        private Request(String number, PhoneAuthProvider.ForceResendingToken token,
                        PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks) {
            this.number = number;
            resendToken = token;
            this.callbacks = callbacks;
        }

        public void failedToStart() {
            callbacks.onVerificationFailed(new FirebaseException("Phone verification could not start"));
        }
    }

    private final CredentialConsumer consumer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private Method method = Method.PHONE;
    private Step step = Step.ENTRY;
    private String input = "";
    private String number = "";
    private String verificationId;
    private PhoneAuthProvider.ForceResendingToken resendToken;
    private long generation;
    private long resendAt;
    private long reattachUntil;
    private boolean autoActive;
    private boolean codeSent;
    private boolean touched;
    private int message;
    private boolean error;
    private Runnable watchdog;

    PhoneSignInFlow(CredentialConsumer consumer) {
        this.consumer = consumer;
        publish();
    }

    public LiveData<State> getState() { return state; }

    public void updateInput(String value) {
        if (step != Step.ENTRY || method != Method.PHONE || input.equals(value)) return;
        touched = true;
        input = value;
        message = 0;
        error = false;
        publish();
    }

    public void review() {
        if (step != Step.ENTRY || method != Method.PHONE) return;
        touched = true;
        String normalized = normalize(input);
        if (normalized == null) {
            show(R.string.phone_invalid_number, true);
            return;
        }
        number = normalized;
        step = Step.REVIEW;
        show(0, false);
    }

    /** Only called by explicit confirm/send-again clicks. Cooldown survives edit/tab changes. */
    public Request send(boolean resend) {
        if (method != Method.PHONE || (resend ? step != Step.CODE : step != Step.REVIEW)) return null;
        if (SystemClock.elapsedRealtime() < resendAt) return null;
        PhoneAuthProvider.ForceResendingToken force = resend ? resendToken : null;
        retire();
        long request = generation;
        verificationId = null;
        step = Step.SENDING;
        codeSent = false;
        autoActive = true;
        long now = SystemClock.elapsedRealtime();
        resendAt = now + 60_000L;
        // SDK uses a 60-second window. Leave a safety margin; never re-enter after expiry.
        reattachUntil = now + 55_000L;
        show(R.string.phone_sending, false);
        watchdog = () -> {
            if (request != generation || step != Step.SENDING) return;
            retire();
            step = Step.REVIEW;
            show(R.string.phone_send_unknown, true);
        };
        handler.postDelayed(watchdog, 90_000L);
        return new Request(number, force, callbacks(request));
    }

    /** Reentrant SDK attachment only within the original active window; never force resend. */
    public Request reattach() {
        if (method != Method.PHONE || !autoActive
                || (step != Step.SENDING && step != Step.CODE)) return null;
        if (SystemClock.elapsedRealtime() >= reattachUntil) {
            autoActive = false;
            if (step == Step.CODE) show(R.string.phone_auto_paused, false);
            return null;
        }
        return new Request(number, null, callbacks(generation));
    }

    public void verify(String code) {
        if (step != Step.CODE || verificationId == null || method != Method.PHONE) return;
        if (!code.matches("[0-9]{6}")) {
            show(R.string.phone_invalid_code_length, true);
            return;
        }
        try {
            consume(generation, PhoneAuthProvider.getCredential(verificationId, code));
        } catch (IllegalArgumentException e) {
            show(R.string.phone_invalid_code, true);
        }
    }

    private void consume(long request, PhoneAuthCredential credential) {
        if (!accept(request) || step == Step.VERIFYING) return;
        clearWatchdog();
        autoActive = false;
        step = Step.VERIFYING;
        show(0, false);
        if (!consumer.accept(request, credential)) {
            step = verificationId == null ? Step.REVIEW : Step.CODE;
            show(R.string.phone_session_busy, true);
        }
    }

    public void signInFailed(long request, Exception exception) {
        if (request != generation || step != Step.VERIFYING) return;
        String code = authCode(exception);
        boolean hadCode = verificationId != null || resendToken != null;
        if ("ERROR_SESSION_EXPIRED".equals(code) || "ERROR_INVALID_VERIFICATION_ID".equals(code)) {
            retire();
            verificationId = null;
        }
        step = hadCode ? Step.CODE : Step.REVIEW;
        show(errorMessage(exception), true);
    }

    public void select(Method selected) {
        if (selected == method) return;
        cancel(false);
        method = selected;
        publish();
    }

    public void cancel(boolean clearInput) {
        retire();
        codeSent = false;
        verificationId = null;
        resendToken = null;
        number = "";
        if (clearInput) input = "";
        step = Step.ENTRY;
        touched = true;
        show(0, false);
    }

    public void processRestored(boolean hadPhoneFlow) {
        if (hadPhoneFlow && !touched) {
            cancel(true);
            show(R.string.phone_process_lost, false);
        }
    }

    private PhoneAuthProvider.OnVerificationStateChangedCallbacks callbacks(long request) {
        return new PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            @Override public void onVerificationCompleted(@NonNull PhoneAuthCredential credential) {
                if (!autoActive) return;
                consume(request, credential);
            }

            @Override public void onVerificationFailed(@NonNull FirebaseException exception) {
                logPhoneFailure("onVerificationFailed", exception);
                if (!accept(request) || step == Step.VERIFYING) return;
                if (step == Step.CODE && !autoActive) return;
                retire();
                verificationId = null;
                step = Step.REVIEW;
                show(errorMessage(exception), true);
            }

            @Override public void onCodeSent(@NonNull String id,
                                            @NonNull PhoneAuthProvider.ForceResendingToken token) {
                if (!accept(request) || step == Step.VERIFYING) return;
                if (id.equals(verificationId) && step == Step.CODE) {
                    resendToken = token;
                    if (!codeSent) {
                        codeSent = true;
                        publish();
                    }
                    return;
                }
                clearWatchdog();
                codeSent = true;
                verificationId = id;
                resendToken = token;
                resendAt = Math.max(resendAt, SystemClock.elapsedRealtime() + 60_000L);
                step = Step.CODE;
                show(R.string.phone_code_sent, false);
            }

            @Override public void onCodeAutoRetrievalTimeOut(@NonNull String id) {
                if (!accept(request) || step == Step.VERIFYING || !autoActive) return;
                clearWatchdog();
                autoActive = false;
                verificationId = id;
                step = Step.CODE;
                show(R.string.phone_auto_timeout, false);
            }
        };
    }

    private boolean accept(long request) {
        return request == generation && method == Method.PHONE
                && (step == Step.SENDING || step == Step.CODE || step == Step.VERIFYING);
    }

    private void retire() {
        ++generation;
        autoActive = false;
        clearWatchdog();
    }

    private void clearWatchdog() {
        if (watchdog != null) handler.removeCallbacks(watchdog);
        watchdog = null;
    }

    private void show(int text, boolean failed) {
        message = text;
        error = failed;
        publish();
    }

    private void publish() { state.setValue(new State(this)); }

    /** Plausible VN mobile format, not a carrier assignment/existence/ownership check. */
    static String normalize(String raw) {
        if (raw == null || !raw.matches("[+0-9\\s\\p{Z}().-]+")) return null;
        String compact = raw.replaceAll("[\\s\\p{Z}().-]", "");
        if (compact.matches("0[35789][0-9]{8}")) return "+84" + compact.substring(1);
        if (compact.matches("\\+84[35789][0-9]{8}")) return compact;
        return null;
    }

    private static String authCode(Exception error) {
        return error instanceof FirebaseAuthException ? ((FirebaseAuthException) error).getErrorCode() : "";
    }

    /** Debug metadata only; never pass the exception itself to Log. */
    static void logPhoneFailure(String step, Exception exception) {
        if (!BuildConfig.DEBUG) return;
        String code = authCode(exception);
        if (code == null || code.isEmpty()) code = "NO_AUTH_CODE";
        String exceptionClass = exception == null ? "NO_EXCEPTION" : exception.getClass().getName();
        Log.w("GrandMaPhoneAuth", step + " " + exceptionClass + " " + code);
    }

    static int errorMessage(Exception error) {
        if (error instanceof FirebaseAuthMissingActivityForRecaptchaException) return R.string.phone_configuration_error;
        if (error instanceof FirebaseNetworkException) return R.string.phone_network_error;
        if (error instanceof FirebaseTooManyRequestsException) return R.string.phone_throttled;
        switch (authCode(error)) {
            case "ERROR_WEB_NETWORK_REQUEST_FAILED": return R.string.phone_network_error;
            case "ERROR_INVALID_PHONE_NUMBER": return R.string.phone_invalid_number;
            case "ERROR_INVALID_VERIFICATION_CODE": return R.string.phone_invalid_code;
            case "ERROR_SESSION_EXPIRED":
            case "ERROR_INVALID_VERIFICATION_ID": return R.string.phone_code_expired;
            case "ERROR_QUOTA_EXCEEDED":
            case "ERROR_TOO_MANY_REQUESTS": return R.string.phone_throttled;
            case "ERROR_WEB_CONTEXT_CANCELED": return R.string.phone_verification_cancelled;
            case "ERROR_USER_DISABLED": return R.string.auth_user_disabled;
            case "ERROR_OPERATION_NOT_ALLOWED":
            case "ERROR_APP_NOT_AUTHORIZED":
            case "ERROR_INVALID_APP_CREDENTIAL":
            case "ERROR_MISSING_CLIENT_IDENTIFIER":
            case "ERROR_MISSING_ACTIVITY_FOR_RECAPTCHA":
            case "ERROR_CAPTCHA_CHECK_FAILED": return R.string.phone_configuration_error;
            default: return R.string.phone_generic_error;
        }
    }
}
