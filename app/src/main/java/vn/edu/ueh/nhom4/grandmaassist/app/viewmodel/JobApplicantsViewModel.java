package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.ApplicationRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.Application;

public final class JobApplicantsViewModel extends ViewModel {
    public static final int TAB_ALL = 0;
    public static final int TAB_PENDING = 1;
    public static final int TAB_ACCEPTED = 2;
    public static final int TAB_REJECTED = 3;

    public final MutableLiveData<Long> changes = new MutableLiveData<>(0L);
    public final List<Application> items = new ArrayList<>();
    public final List<Application> displayList = new ArrayList<>();

    public boolean busy = false;
    public String message = "";
    public int currentTab = TAB_ALL;
    public String currentJobId = "";

    private final ApplicationRepository repository = new ApplicationRepository();

    public void emit() {
        Long v = changes.getValue();
        changes.setValue(v == null ? 1L : v + 1L);
    }

    public String currentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "";
    }

    public void loadApplicants(String jobId, boolean force) {
        if (jobId == null || jobId.trim().isEmpty()) return;
        this.currentJobId = jobId;

        String uid = currentUid();
        if (uid.isEmpty()) {
            message = "Chưa đăng nhập.";
            emit();
            return;
        }

        busy = true;
        message = "";
        emit();

        repository.getApplicationsForJob(uid, jobId).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                items.clear();
                if (t.getResult() != null) {
                    items.addAll(t.getResult());
                }
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
        for (Application a : items) {
            if (currentTab == TAB_ALL) {
                displayList.add(a);
            } else if (currentTab == TAB_PENDING) {
                if (a.isPending()) displayList.add(a);
            } else if (currentTab == TAB_ACCEPTED) {
                if (a.isAccepted()) displayList.add(a);
            } else if (currentTab == TAB_REJECTED) {
                if (a.isRejected()) displayList.add(a);
            }
        }
    }

    public void accept(String applicationId, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        emit();

        repository.accept(uid, applicationId).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                loadApplicants(currentJobId, true);
                if (onSuccess != null) onSuccess.run();
            } else {
                emit();
                if (onError != null) onError.accept(ApplicationRepository.error(t.getException()));
            }
        });
    }

    public void reject(String applicationId, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        emit();

        repository.reject(uid, applicationId).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                loadApplicants(currentJobId, true);
                if (onSuccess != null) onSuccess.run();
            } else {
                emit();
                if (onError != null) onError.accept(ApplicationRepository.error(t.getException()));
            }
        });
    }
}
