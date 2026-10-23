package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.credentials.Credential;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import vn.edu.ueh.nhom4.grandmaassist.app.GrandMaAssistApp;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.AuthRepository;

/** Retains no screen context, credential, or account data in saved instance state. */
public final class AuthViewModel extends AndroidViewModel {
    private final AuthRepository repository;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        repository = ((GrandMaAssistApp) application).getAuthRepository();
    }

    public LiveData<AuthRepository.State> getState() { return repository.getState(); }
    public long beginGooglePicker() { return repository.beginGooglePicker(); }
    public void pickerFailed(long request, int message, boolean error) {
        repository.pickerFailed(request, message, error);
    }
    public void pickerInterrupted(long request) { repository.pickerInterrupted(request); }
    public void signIn(long request, Credential credential) { repository.signIn(request, credential); }
    public void onForeground() { repository.checkSession(); }
    public void retry() { repository.retry(); }
    public void signOut() { repository.signOut(); }
}
