package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.SavedJobRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.SavedJob;

public final class SavedJobsViewModel extends AndroidViewModel {
    public static final int FILTER_ALL = 0;
    public static final int FILTER_OPEN = 1;
    public static final int FILTER_CLOSED = 2;

    public final MutableLiveData<Long> changes = new MutableLiveData<>(0L);
    public final String sessionUid;
    public List<SavedJob> rawList = new ArrayList<>();
    public List<SavedJob> displayList = new ArrayList<>();
    public int currentFilter = FILTER_ALL;
    public boolean loading = false;
    public String errorMessage = "";

    private final SavedJobRepository repository;
    private final FirebaseAuth.AuthStateListener authListener;

    public SavedJobsViewModel(@NonNull Application app) {
        super(app);
        repository = new SavedJobRepository(app);
        FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
        sessionUid = u != null ? u.getUid() : "";
        authListener = a -> {
            FirebaseUser curr = FirebaseAuth.getInstance().getCurrentUser();
            String currUid = curr != null ? curr.getUid() : "";
            if (!sessionUid.equals(currUid)) {
                rawList.clear(); displayList.clear(); emit();
            }
        };
        FirebaseAuth.getInstance().addAuthStateListener(authListener);
    }

    public void emit() { changes.setValue(changes.getValue() + 1); }

    public void load(String uid) {
        if (uid == null || uid.isEmpty()) return;
        loading = true; errorMessage = ""; emit();
        repository.getSavedJobs(uid).addOnCompleteListener(t -> {
            loading = false;
            if (t.isSuccessful() && t.getResult() != null) {
                rawList = new ArrayList<>(t.getResult());
                applyFilter();
            } else {
                errorMessage = "Không thể tải danh sách việc đã lưu";
            }
            emit();
        });
    }

    public void setFilter(int filter) {
        currentFilter = filter;
        applyFilter();
        emit();
    }

    public void unsave(String uid, String jobId) {
        if (uid == null || jobId == null) return;
        repository.unsaveJob(uid, jobId).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                List<SavedJob> updated = new ArrayList<>();
                for (SavedJob sj : rawList) {
                    if (!sj.jobId.equals(jobId)) updated.add(sj);
                }
                rawList = updated;
                applyFilter();
                emit();
            }
        });
    }

    private void applyFilter() {
        displayList.clear();
        for (SavedJob sj : rawList) {
            if (currentFilter == FILTER_ALL) {
                displayList.add(sj);
            } else if (currentFilter == FILTER_OPEN && sj.isOpen()) {
                displayList.add(sj);
            } else if (currentFilter == FILTER_CLOSED && (sj.isClosed() || sj.isExpired())) {
                displayList.add(sj);
            }
        }
    }

    public int getCountAll() { return rawList.size(); }
    public int getCountOpen() {
        int c = 0; for (SavedJob sj : rawList) if (sj.isOpen()) c++; return c;
    }
    public int getCountClosed() {
        int c = 0; for (SavedJob sj : rawList) if (sj.isClosed() || sj.isExpired()) c++; return c;
    }

    @Override protected void onCleared() {
        FirebaseAuth.getInstance().removeAuthStateListener(authListener);
        super.onCleared();
    }
}
