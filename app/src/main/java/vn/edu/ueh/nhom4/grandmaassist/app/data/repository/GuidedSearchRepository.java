package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import android.content.Context;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.PublicJobStore;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.SearchPreferenceStore;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.PublicJob;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.SearchPreference;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.service.GuidedJobMatcher;

public final class GuidedSearchRepository {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private final SearchPreferenceStore store;
    private final PublicJobRepository jobRepo;

    public GuidedSearchRepository(Context c) {
        store = SearchPreferenceStore.get(c);
        jobRepo = new PublicJobRepository(c);
    }

    public Task<SearchPreference> getPreference(String ownerKey) {
        return Tasks.call(IO, () -> store.read(ownerKey));
    }

    public Task<Void> savePreference(String ownerKey, SearchPreference pref) {
        return Tasks.call(IO, () -> { store.save(ownerKey, pref); return null; });
    }

    public Task<PublicJobStore.Catalog> getCatalog() {
        return jobRepo.catalog().continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null) return t;
            return jobRepo.cachedCatalog();
        });
    }

    public Task<List<GuidedJobMatcher.MatchResult>> matchJobs(SearchPreference pref, String profileArea) {
        return jobRepo.list("", "", System.currentTimeMillis(), null).continueWithTask(t -> {
            if (t.isSuccessful() && t.getResult() != null) {
                List<PublicJob> jobs = t.getResult().jobs;
                return Tasks.call(IO, () -> GuidedJobMatcher.evaluate(jobs, pref, profileArea));
            }
            return jobRepo.cachedList("", "", null).continueWithTask(cachedTask -> {
                List<PublicJob> jobs = (cachedTask.isSuccessful() && cachedTask.getResult() != null)
                        ? cachedTask.getResult().jobs : Collections.emptyList();
                return Tasks.call(IO, () -> GuidedJobMatcher.evaluate(jobs, pref, profileArea));
            });
        });
    }
}
