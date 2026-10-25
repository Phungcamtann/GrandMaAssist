package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestoreException;
import java.util.ArrayList;
import java.util.List;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.ProfileRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.ProfileDraft;

/** UI state stays in memory across rotation, never SavedStateHandle/preferences. */
public final class ProfileViewModel extends AndroidViewModel {
    public enum Screen { LOADING, ROLE, PROFILE, ADMIN, ERROR }
    public enum Destination { HOME, PROFILE, ACCOUNT, SECURITY }
    private final ProfileRepository repository = new ProfileRepository();
    private final MutableLiveData<Long> changes = new MutableLiveData<>(0L);
    public Screen screen = Screen.LOADING;
    public Destination destination=Destination.HOME;
    public ProfileRepository.AdminCounts adminCounts;
    public boolean countsLoading;
    public int countsMessage;
    private long countsRequest;
    public String verificationStatus="UNKNOWN";
    public boolean verificationLoading,verificationFailed;
    private long verificationRequest;
    public ProfileRepository.Snapshot snapshot;
    public ProfileDraft draft = new ProfileDraft();
    public List<ProfileRepository.Area> areas = new ArrayList<>();
    public String selectedRole = "", currentPhone = "";
    public boolean dirty, busy, accountPage, syncing, readRequired, areaLoading;
    public int message, areaMessage;
    private String uid, observedPhone;
    private long generation;
    private long areaRequest;

    public ProfileViewModel(@NonNull Application app) { super(app); }
    public LiveData<Long> changes() { return changes; }
    public void notifyChange() { changes.setValue(changes.getValue() + 1); }
    private boolean current(long request, String owner) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return request == generation && owner != null && owner.equals(uid)
                && user != null && owner.equals(user.getUid());
    }
    /** Called only after auth UI has a user, never as evidence of a business permission. */
    public void bind(boolean signedIn, boolean authOperationActive) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (!signedIn || user == null) {
            if (uid != null) {
                ++generation; uid = null; observedPhone = null; snapshot = null;
                draft = new ProfileDraft(); areas = new ArrayList<>(); selectedRole = "";
                dirty = busy = syncing = accountPage = readRequired = areaLoading = false;
                ++areaRequest;
                currentPhone = ""; screen = Screen.LOADING; message = areaMessage = 0;
                destination=Destination.HOME;adminCounts=null;countsLoading=false;countsMessage=0;++countsRequest;
                acceptVerification(null);
            }
            return;
        }
        String phone = user.getPhoneNumber() == null ? "" : user.getPhoneNumber();
        if (!user.getUid().equals(uid)) {
            ++generation; uid = user.getUid(); observedPhone = phone; currentPhone = phone;
            draft = new ProfileDraft(); snapshot = null; dirty = busy = syncing = accountPage = readRequired = areaLoading = false;
            ++areaRequest;
            selectedRole = ""; areas = new ArrayList<>(); message = areaMessage = 0;
            destination=Destination.HOME;adminCounts=null;countsLoading=false;countsMessage=0;++countsRequest;
            acceptVerification(null);
            load(); return;
        }
        currentPhone = phone;
        if (!phone.equals(observedPhone) && !authOperationActive && !busy) {
            observedPhone = phone;
            if (dirty) { message = R.string.p_phone_pending; notifyChange(); }
            else if (snapshot != null && !"ADMIN".equals(snapshot.role)) syncPhone();
            else load();
        }
    }
    public void selectRole(String role) {
        if (screen != Screen.ROLE || busy) return;
        selectedRole = role; notifyChange();
    }
    public void edited() {
        dirty = true; message = 0; notifyChange();
    }
    /** Called only after the user confirms discarding; the following action emits the UI change. */
    public void discardEdits() {
        draft = snapshot == null ? new ProfileDraft() : snapshot.draft.copy();
        dirty = false;
    }
    public void load() {
        if (uid == null || busy) return;
        String owner = uid; long request = ++generation;
        ++countsRequest;countsLoading=false;adminCounts=null;
        acceptVerification(null);
        busy = true; screen = Screen.LOADING; message = 0; notifyChange();
        repository.load(owner).addOnCompleteListener(task -> {
            if (!current(request, owner)) return;
            busy = false;
            if (!task.isSuccessful()) { screen = Screen.ERROR; message = error(task.getException());
                adminCounts=null;countsLoading=false;++countsRequest;notifyChange(); return; }
            snapshot = task.getResult(); acceptVerification(snapshot);readRequired = false; dirty = false;
            if (snapshot == null) { screen = Screen.ROLE; notifyChange(); return; }
            if ("ADMIN".equals(snapshot.role)) { screen = Screen.ADMIN; draft=snapshot.draft.copy();notifyChange();loadCounts();return; }
            screen = Screen.PROFILE; draft = snapshot.draft.copy();
            if (!"ACTIVE".equals(snapshot.status)) message = R.string.p_inactive;
            notifyChange(); loadAreas();
            if (!snapshot.phone.equals(currentPhone) && "ACTIVE".equals(snapshot.status)) syncPhone();
        });
    }
    private void acceptVerification(ProfileRepository.Snapshot value){
        ++verificationRequest;verificationLoading=verificationFailed=false;
        verificationStatus=value==null?"UNKNOWN":value.verificationStatus;
    }
    public void refreshVerification(){
        if(uid==null || busy || verificationLoading || snapshot==null || "ADMIN".equals(snapshot.role))return;
        String owner=uid,role=snapshot.role;long request=generation,attempt=++verificationRequest;
        verificationLoading=true;verificationFailed=false;notifyChange();
        repository.load(owner).addOnCompleteListener(task->{
            if(!current(request,owner) || attempt!=verificationRequest)return;
            verificationLoading=false;
            ProfileRepository.Snapshot latest=task.isSuccessful()?task.getResult():null;
            if(latest==null || !role.equals(latest.role)){
                verificationStatus="UNKNOWN";verificationFailed=true;
            }else{
                verificationStatus=latest.verificationStatus;
                // A status refresh must never replace unsaved professional information.
                if(!dirty && !busy && !readRequired){
                    boolean areaChanged=!snapshot.draft.areaCode.equals(latest.draft.areaCode);
                    snapshot=latest;draft=latest.draft.copy();
                    if(!"ACTIVE".equals(latest.status))message=R.string.p_inactive;
                    else if(message==R.string.p_inactive)message=0;
                    if(areaChanged)loadAreas();
                }
            }
            notifyChange();
        });
    }
    public void loadCounts(){
        if(uid==null || countsLoading || snapshot==null || !"ADMIN".equals(snapshot.role))return;
        if(!"ACTIVE".equals(snapshot.status)){adminCounts=null;countsMessage=R.string.p_inactive;notifyChange();return;}
        String owner=uid;long request=generation,attempt=++countsRequest;
        countsLoading=true;countsMessage=0;adminCounts=null;notifyChange();
        repository.adminCounts(owner).addOnCompleteListener(task->{
            if(!current(request,owner) || attempt!=countsRequest)return;
            countsLoading=false;
            if(task.isSuccessful())adminCounts=task.getResult();else countsMessage=R.string.n_queue_failed;
            notifyChange();
        });
    }
    public void loadAreas() {
        if (uid == null) return;
        String owner = uid; long request = generation;
        long areaAttempt = ++areaRequest;
        areaLoading = true;
        notifyChange();
        repository.areas().addOnCompleteListener(task -> {
            if (!current(request, owner) || areaAttempt != areaRequest) return;
            areaLoading = false;
            if (task.isSuccessful()) {
                areas = task.getResult(); areaMessage = areas.isEmpty() ? R.string.p_areas_empty : 0;
            } else {
                areas = new ArrayList<>(); areaMessage = R.string.p_areas_failed;
            }
            notifyChange();
        });
    }
    public void choose() {
        if (busy || uid == null || screen != Screen.ROLE || selectedRole.isEmpty()) return;
        String owner = uid; long request = generation;
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;
        busy = true; message = R.string.p_saving; notifyChange();
        repository.choose(owner, selectedRole, user.getDisplayName()).addOnCompleteListener(task -> {
            if (!current(request, owner)) return;
            busy = false;
            if (!task.isSuccessful()) {
                readRequired = true; screen = Screen.ERROR; message = R.string.p_unknown_write; notifyChange();
            } else { destination=Destination.HOME;accountPage=false;load(); }
        });
    }
    public void save() {
        if (busy || readRequired || snapshot == null || screen != Screen.PROFILE) return;
        if (!draft.valid(snapshot.role)) { message = R.string.p_validation; notifyChange(); return; }
        String owner = uid; long request = generation;
        busy = true; message = R.string.p_saving; notifyChange();
        repository.save(snapshot, draft.copy()).addOnCompleteListener(task -> {
            if (!current(request, owner)) return;
            busy = false;
            if (!task.isSuccessful()) {
                message = error(task.getException());
                readRequired = message != R.string.p_validation && message != R.string.p_area_invalid;
                notifyChange(); return;
            }
            readRequired = true; // The next base revision must come from the server.
            dirty = false;
            reloadAfterWrite(owner, request, R.string.p_saved);
        });
    }
    public void syncPhone() {
        if (busy || uid == null || snapshot == null || "ADMIN".equals(snapshot.role)) return;
        if (dirty) { message = R.string.p_phone_pending; notifyChange(); return; }
        String owner = uid; long request = generation;
        busy = true; syncing = true; message = R.string.p_syncing; notifyChange();
        repository.syncPhone(owner).addOnCompleteListener(task -> {
            if (!current(request, owner)) return;
            busy = syncing = false; readRequired = true;
            if (!task.isSuccessful()) { message = R.string.p_sync_failed; notifyChange(); return; }
            reloadAfterWrite(owner, request, R.string.p_synced);
        });
    }
    private void reloadAfterWrite(String owner, long request, int successMessage) {
        busy = true; notifyChange();
        repository.load(owner).addOnCompleteListener(loaded -> {
            if (!current(request, owner)) return;
            busy = false;
            if (!loaded.isSuccessful() || loaded.getResult() == null) {
                message = R.string.p_applied_read_failed; readRequired = true;
            } else {
                snapshot = loaded.getResult(); acceptVerification(snapshot);draft = snapshot.draft.copy(); dirty = false;
                readRequired = false; message = successMessage;
                screen = "ADMIN".equals(snapshot.role) ? Screen.ADMIN : Screen.PROFILE;
                loadAreas();
            }
            notifyChange();
        });
    }
    public void showAccount() { destination=Destination.ACCOUNT;accountPage=false;notifyChange(); }
    public void showSecurity() { destination=Destination.SECURITY;accountPage=true;notifyChange(); }
    public void showProfile() { destination=Destination.PROFILE;accountPage=false;notifyChange(); }
    public void showHome() { destination=Destination.HOME;accountPage=false;notifyChange(); }
    @Override protected void onCleared() {
        ++generation; ++areaRequest; ++countsRequest;acceptVerification(null);uid = null; snapshot = null; draft = new ProfileDraft(); areas.clear();adminCounts=null;
        super.onCleared();
    }
    private int error(Exception exception) {
        Throwable cause = exception;
        for (int i = 0; cause != null && i < 8; i++, cause = cause.getCause()) {
            if (cause instanceof ProfileRepository.Failure) {
                switch (((ProfileRepository.Failure) cause).code) {
                    case "VALIDATION": return R.string.p_validation;
                    case "CONFLICT": return R.string.p_conflict;
                    case "AREA": return R.string.p_area_invalid;
                    case "SCHEMA": case "ROLE": return R.string.p_schema;
                    case "ADMIN_ACCESS": return R.string.n_admin_access;
                    case "INACTIVE": return R.string.p_inactive;
                    case "PHONE": return R.string.p_phone_pending;
                    case "SESSION": return R.string.p_session_changed;
                }
            }
            if (cause instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException.Code code = ((FirebaseFirestoreException) cause).getCode();
                if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) return R.string.p_permission;
                if (code == FirebaseFirestoreException.Code.UNAVAILABLE || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)
                    return R.string.p_network;
            }
        }
        return R.string.p_error;
    }
}
