package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import android.content.Context;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.SavedJobStore;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.PublicJob;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.SavedJob;

public final class SavedJobRepository {
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final SavedJobStore store;
    private final PublicJobRepository jobRepo;
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    public SavedJobRepository(Context c) {
        store = SavedJobStore.get(c);
        jobRepo = new PublicJobRepository(c);
    }

    public Task<Void> saveJob(String uid, PublicJob job) {
        if (uid == null || job == null) return Tasks.forException(new IllegalArgumentException("NULL_ARG"));
        Map<String, Object> map = new HashMap<>();
        map.put("seekerId", uid);
        map.put("jobId", job.id);
        map.put("savedAt", FieldValue.serverTimestamp());
        return PublicJobRepository.bounded(db.collection("users").document(uid).collection("savedJobs").document(job.id).set(map))
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) throw t.getException();
                    return Tasks.call(IO, () -> {
                        store.save(uid, new SavedJob(uid, job.id, System.currentTimeMillis(), job, "OPEN"));
                        return null;
                    });
                });
    }

    public Task<Void> unsaveJob(String uid, String jobId) {
        if (uid == null || jobId == null) return Tasks.forException(new IllegalArgumentException("NULL_ARG"));
        return PublicJobRepository.bounded(db.collection("users").document(uid).collection("savedJobs").document(jobId).delete())
                .continueWithTask(t -> {
                    if (!t.isSuccessful()) throw t.getException();
                    return Tasks.call(IO, () -> {
                        store.remove(uid, jobId);
                        return null;
                    });
                });
    }

    public Task<Boolean> isJobSaved(String uid, String jobId) {
        if (uid == null || jobId == null) return Tasks.forResult(false);
        return Tasks.call(IO, () -> store.isSaved(uid, jobId)).continueWithTask(t -> {
            if (t.isSuccessful() && Boolean.TRUE.equals(t.getResult())) {
                return Tasks.forResult(true);
            }
            return PublicJobRepository.bounded(db.collection("users").document(uid).collection("savedJobs").document(jobId).get())
                    .continueWith(docTask -> docTask.isSuccessful() && docTask.getResult() != null && docTask.getResult().exists());
        });
    }

    public Task<List<SavedJob>> getSavedJobs(String uid) {
        if (uid == null || uid.isEmpty()) return Tasks.call(IO, () -> store.list(uid));
        Query q = db.collection("users").document(uid).collection("savedJobs").orderBy("savedAt", Query.Direction.DESCENDING);
        return PublicJobRepository.bounded(q.get(Source.SERVER)).continueWithTask(t -> {
            if (!t.isSuccessful()) {
                return Tasks.call(IO, () -> store.list(uid));
            }
            QuerySnapshot snapshot = t.getResult();
            if (snapshot == null || snapshot.isEmpty()) {
                return Tasks.call(IO, () -> {
                    store.clear(uid);
                    return Collections.emptyList();
                });
            }
            List<Task<SavedJob>> detailTasks = new ArrayList<>();
            for (DocumentSnapshot doc : snapshot) {
                String jobId = doc.getString("jobId");
                if (jobId == null || jobId.isEmpty()) jobId = doc.getId();
                Timestamp ts = doc.getTimestamp("savedAt");
                long savedAt = ts != null ? ts.toDate().getTime() : System.currentTimeMillis();

                String finalJobId = jobId;
                Task<SavedJob> task = jobRepo.detail(finalJobId).continueWithTask(detailTask -> {
                    if (detailTask.isSuccessful() && detailTask.getResult() != null && detailTask.getResult().job != null) {
                        PublicJob job = detailTask.getResult().job;
                        String status = job.expiresAt <= System.currentTimeMillis() ? "EXPIRED" : "OPEN";
                        return Tasks.forResult(new SavedJob(uid, finalJobId, savedAt, job, status));
                    }
                    return jobRepo.cachedDetail(finalJobId).continueWith(cacheTask -> {
                        PublicJob cachedJob = cacheTask.isSuccessful() ? cacheTask.getResult() : null;
                        String status = "CLOSED";
                        if (cachedJob != null) {
                            status = cachedJob.expiresAt <= System.currentTimeMillis() ? "EXPIRED" : "OPEN";
                        }
                        return new SavedJob(uid, finalJobId, savedAt, cachedJob, status);
                    });
                });
                detailTasks.add(task);
            }
            return Tasks.whenAllSuccess(detailTasks).continueWithTask(allTask -> {
                @SuppressWarnings("unchecked")
                List<SavedJob> results = (List<SavedJob>) (List<?>) allTask.getResult();
                return Tasks.call(IO, () -> {
                    for (SavedJob sj : results) {
                        store.save(uid, sj);
                    }
                    return results;
                });
            });
        });
    }
}
