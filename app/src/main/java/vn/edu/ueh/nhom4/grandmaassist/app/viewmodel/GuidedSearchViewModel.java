package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.PublicJobStore;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.GuidedSearchRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.ProfileRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.SearchPreference;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.service.GuidedJobMatcher;

public final class GuidedSearchViewModel extends AndroidViewModel {
    public int currentStep = 1;
    public final Set<String> selectedCategories = new LinkedHashSet<>();
    public boolean noCategoryPriority = false;
    public final Set<String> selectedAreas = new LinkedHashSet<>();
    public boolean noAreaPriority = false;
    public final Set<String> selectedDays = new LinkedHashSet<>();
    public final Set<String> selectedShifts = new LinkedHashSet<>();
    public boolean isFlexibleShift = false;
    public final Set<String> avoidedDemands = new LinkedHashSet<>();
    public boolean noAvoidPriority = false;

    public PublicJobStore.Catalog catalog = new PublicJobStore.Catalog();
    public List<GuidedJobMatcher.MatchResult> matchedResults = new ArrayList<>();
    public boolean busy = false;
    public String message = "";
    public final MutableLiveData<Long> changes = new MutableLiveData<>(0L);
    public String seekerProfileArea = "";

    private final GuidedSearchRepository repository;
    private final ProfileRepository profileRepo;
    private String ownerKey = "";

    public GuidedSearchViewModel(@NonNull Application app) {
        super(app);
        repository = new GuidedSearchRepository(app);
        profileRepo = new ProfileRepository();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        ownerKey = user != null ? user.getUid() : "";
        loadInitialData();
    }

    private void loadInitialData() {
        busy = true; notifyChanges();
        repository.getCatalog().addOnCompleteListener(t -> {
            if (t.isSuccessful() && t.getResult() != null) catalog = t.getResult();
            loadUserPreferences();
        });
        if (!ownerKey.isEmpty()) {
            profileRepo.load(ownerKey).addOnCompleteListener(t -> {
                if (t.isSuccessful() && t.getResult() != null && t.getResult().draft != null) {
                    seekerProfileArea = t.getResult().draft.areaCode;
                    notifyChanges();
                }
            });
        }
    }

    private void loadUserPreferences() {
        if (ownerKey.isEmpty()) { busy = false; notifyChanges(); return; }
        repository.getPreference(ownerKey).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful() && t.getResult() != null) {
                SearchPreference pref = t.getResult();
                selectedCategories.clear(); selectedCategories.addAll(pref.categoryIds);
                noCategoryPriority = pref.categoryIds.isEmpty();
                selectedAreas.clear(); selectedAreas.addAll(pref.areaCodes);
                noAreaPriority = pref.areaCodes.isEmpty();
                selectedDays.clear(); selectedDays.addAll(pref.days);
                selectedShifts.clear(); selectedShifts.addAll(pref.shifts);
                isFlexibleShift = pref.isShiftFlexible();
                avoidedDemands.clear(); avoidedDemands.addAll(pref.avoidedDemands);
                noAvoidPriority = pref.avoidedDemands.isEmpty();
            }
            notifyChanges();
        });
    }

    public void nextStep() {
        if (currentStep < 5) { currentStep++; notifyChanges(); }
        else if (currentStep == 5) { search(); }
    }

    public void prevStep() {
        if (currentStep > 1) { currentStep--; notifyChanges(); }
    }

    public void jumpToStep(int step) {
        if (step >= 1 && step <= 5) { currentStep = step; notifyChanges(); }
    }

    public void toggleCategory(String id) {
        if (id == null) return;
        noCategoryPriority = false;
        if (selectedCategories.contains(id)) selectedCategories.remove(id);
        else selectedCategories.add(id);
        notifyChanges();
    }

    public void setNoCategoryPriority(boolean noPriority) {
        noCategoryPriority = noPriority;
        if (noPriority) selectedCategories.clear();
        notifyChanges();
    }

    public void toggleArea(String code) {
        if (code == null) return;
        noAreaPriority = false;
        if (selectedAreas.contains(code)) selectedAreas.remove(code);
        else selectedAreas.add(code);
        notifyChanges();
    }

    public void setNoAreaPriority(boolean noPriority) {
        noAreaPriority = noPriority;
        if (noPriority) selectedAreas.clear();
        notifyChanges();
    }

    public void toggleDay(String day) {
        if (day == null) return;
        if (selectedDays.contains(day)) selectedDays.remove(day);
        else selectedDays.add(day);
        notifyChanges();
    }

    public void toggleShift(String shift) {
        if (shift == null) return;
        if ("FLEXIBLE".equals(shift)) {
            isFlexibleShift = !isFlexibleShift;
            if (isFlexibleShift) selectedShifts.clear();
        } else {
            isFlexibleShift = false;
            if (selectedShifts.contains(shift)) selectedShifts.remove(shift);
            else selectedShifts.add(shift);
        }
        notifyChanges();
    }

    public void toggleDemand(String demand) {
        if (demand == null) return;
        noAvoidPriority = false;
        if (avoidedDemands.contains(demand)) avoidedDemands.remove(demand);
        else avoidedDemands.add(demand);
        notifyChanges();
    }

    public void setNoAvoidPriority(boolean noPriority) {
        noAvoidPriority = noPriority;
        if (noPriority) avoidedDemands.clear();
        notifyChanges();
    }

    public void search() {
        List<String> shifts = new ArrayList<>(selectedShifts);
        if (isFlexibleShift && !shifts.contains("FLEXIBLE")) shifts.add("FLEXIBLE");
        SearchPreference pref = new SearchPreference(
                noCategoryPriority ? Collections.emptyList() : new ArrayList<>(selectedCategories),
                noAreaPriority ? Collections.emptyList() : new ArrayList<>(selectedAreas),
                new ArrayList<>(selectedDays),
                shifts,
                noAvoidPriority ? Collections.emptyList() : new ArrayList<>(avoidedDemands),
                System.currentTimeMillis()
        );
        busy = true; notifyChanges();
        if (!ownerKey.isEmpty()) {
            repository.savePreference(ownerKey, pref);
        }
        currentStep = 6;
        repository.matchJobs(pref, seekerProfileArea).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful() && t.getResult() != null) {
                matchedResults = t.getResult();
            } else {
                matchedResults = Collections.emptyList();
                message = "Không thể tải kết quả. Vui lòng thử lại.";
            }
            notifyChanges();
        });
    }

    public void notifyChanges() {
        changes.postValue(System.currentTimeMillis());
    }
}
