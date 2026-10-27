package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import androidx.lifecycle.*;
import com.google.firebase.auth.*;
import java.util.*;
import java.util.function.Consumer;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.ApplicationRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.Application;

public final class ApplicationViewModel extends ViewModel {
    public static final int TAB_ALL = 0;
    public static final int TAB_PENDING = 1;
    public static final int TAB_RESOLVED = 2;

    public final MutableLiveData<Long> changes = new MutableLiveData<>(0L);
    public final List<Application> items = new ArrayList<>();
    public final List<Application> displayList = new ArrayList<>();

    public boolean busy = false;
    public String message = "";
    public int currentTab = TAB_ALL;

    private final ApplicationRepository repository = new ApplicationRepository();

    public void emit() {
        Long v = changes.getValue();
        changes.setValue(v == null ? 1L : v + 1L);
    }

    public String currentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "";
    }

    public void loadApplications(boolean forceRefresh) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            message = "Chưa đăng nhập.";
            emit();
            return;
        }

        busy = true;
        message = "";
        emit();

        repository.getMyApplications(uid, null).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                items.clear();
                items.addAll(t.getResult().items);
                applyFilter();
            } else {
                message = ApplicationRepository.error(t.getException());
            }
            emit();
        });
    }

    public void setTab(int tab) {
        if (currentTab == tab) return;
        currentTab = tab;
        applyFilter();
        emit();
    }

    private void applyFilter() {
        displayList.clear();
        for (Application app : items) {
            if (currentTab == TAB_ALL) {
                displayList.add(app);
            } else if (currentTab == TAB_PENDING) {
                if (app.isPending()) displayList.add(app);
            } else if (currentTab == TAB_RESOLVED) {
                if (!app.isPending()) displayList.add(app);
            }
        }
    }

    public void apply(String jobId, String messageText, boolean consent, Consumer<String> onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        message = "Đang gửi đơn ứng tuyển…";
        emit();

        repository.apply(uid, jobId, messageText, consent).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                message = "";
                emit();
                if (onSuccess != null) onSuccess.accept(t.getResult());
            } else {
                String err = ApplicationRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void withdraw(String applicationId, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        emit();

        repository.withdraw(uid, applicationId).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                loadApplications(true);
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = ApplicationRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void checkApplication(String jobId, Consumer<Application> callback) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (callback != null) callback.accept(null);
            return;
        }

        repository.getApplicationForJob(uid, jobId).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                if (callback != null) callback.accept(t.getResult());
            } else {
                if (callback != null) callback.accept(null);
            }
        });
    }
}
