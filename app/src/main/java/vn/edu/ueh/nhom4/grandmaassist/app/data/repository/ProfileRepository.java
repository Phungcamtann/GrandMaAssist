package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.AggregateQuerySnapshot;
import com.google.firebase.firestore.AggregateSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.ProfileDraft;

/** All writes use online transactions. No offline write queue and no disk profile cache. */
public final class ProfileRepository {
    public static final class Failure extends RuntimeException {
        public final String code;
        public Failure(String code) { super(code); this.code = code; }
    }
    public static final class Area {
        public final String code, name;
        Area(String code, String name) { this.code = code; this.name = name; }
        @Override public String toString() { return name; }
    }
    public static final class Snapshot {
        public final String uid, role, phone, status, verificationStatus;
        public final long version;
        public final ProfileDraft draft;
        Snapshot(String uid, String role, String phone, String status, long version, ProfileDraft draft, String verificationStatus) {
            this.uid = uid; this.role = role; this.phone = phone; this.status = status;
            this.version = version; this.draft = draft;
            this.verificationStatus=verificationStatus;
        }
    }
    public static final class AdminCounts {
        public final long seeker, employer;
        AdminCounts(long seeker,long employer){this.seeker=seeker;this.employer=employer;}
    }
    private static final class Proof {
        final String uid, phone;
        Proof(String uid, String phone) { this.uid = uid; this.phone = phone; }
    }
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    private boolean same(String uid) {
        return auth.getCurrentUser() != null && uid.equals(auth.getCurrentUser().getUid());
    }
    private <T> Task<T> withProof(String uid, Function<Proof, Task<T>> action) {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null || !same(uid)) return Tasks.forException(new Failure("SESSION"));
        return user.getIdToken(true).continueWithTask(task -> {
            if (!task.isSuccessful()) throw task.getException();
            if (!same(uid)) throw new Failure("SESSION");
            Object value = task.getResult().getClaims().get("phone_number");
            String claim = value instanceof String ? (String) value : "";
            String current = auth.getCurrentUser().getPhoneNumber();
            if (!claim.equals(current == null ? "" : current)) throw new Failure("PHONE");
            return action.apply(new Proof(uid, claim));
        });
    }
    public Task<Snapshot> load(String uid) {
        return withProof(uid, proof -> db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");
            DocumentSnapshot user = tx.get(db.collection("users").document(uid));
            DocumentSnapshot access = tx.get(db.collection("adminAccess").document(uid));
            boolean admin = access.exists() && Boolean.TRUE.equals(access.getBoolean("enabled"));
            if (admin) {
                if (!user.exists() || !"ADMIN".equals(user.getString("role"))) throw new Failure("ADMIN_ACCESS");
                ProfileDraft d=new ProfileDraft();d.displayName=text(user,"displayName");
                return new Snapshot(uid, "ADMIN", proof.phone, text(user, "accountStatus"), 0, d, "UNKNOWN");
            }
            if (!user.exists()) return null;
            String role = user.getString("role");
            if("ADMIN".equals(role))throw new Failure("ADMIN_ACCESS");
            if (!"SEEKER".equals(role) && !"EMPLOYER".equals(role)) throw new Failure("SCHEMA");
            DocumentSnapshot profile = tx.get(profileRef(uid, role));
            Long version = user.getLong("version");
            if (!profile.exists() || version == null) throw new Failure("SCHEMA");
            ProfileDraft d = new ProfileDraft();
            d.displayName = text(user, "displayName"); d.areaCode = text(profile, "areaCode");
            if ("SEEKER".equals(role)) {
                d.skills = String.join(", ", strings(profile, "skills"));
                d.experience = text(profile, "experienceSummary");
                List<String> shifts = strings(profile, "preferredShifts");
                d.morning = shifts.contains("MORNING"); d.afternoon = shifts.contains("AFTERNOON");
                d.evening = shifts.contains("EVENING");
            } else {
                d.employerType = text(profile, "employerType");
                d.organizationName = text(profile, "organizationName");
                d.introduction = text(profile, "introduction");
            }
            return new Snapshot(uid, role, text(user, "phoneE164"),
                    text(user, "accountStatus"), version, d, verificationStatus(user,profile,role));
        }));
    }
    /** Summary only. Business authorization remains at the request/version checks and Rules. */
    private static String verificationStatus(DocumentSnapshot user,DocumentSnapshot profile,String role){
        String field="SEEKER".equals(role)?"retirementStatus":"identityStatus";
        if(!user.contains(field))return "NOT_SUBMITTED"; // Existing C04 users before their first submission.
        String status=text(user,field);
        String approved="SEEKER".equals(role)?"APPROVED":"VERIFIED";
        if(approved.equals(status)){
            Object accepted=user.get("approvedIdentityProfileVersion"),current=profile.get("identityProfileVersion");
            String pointer="SEEKER".equals(role)?"currentRetirementRequestId":"identityRequestId";
            return accepted instanceof Long && current instanceof Long && (Long)accepted>0 && accepted.equals(current) && !text(user,pointer).isEmpty()
                    ?status:"UNKNOWN";
        }
        return java.util.Arrays.asList("NOT_SUBMITTED","SUBMITTED","NEEDS_INFO","REJECTED","REVERIFY_REQUIRED").contains(status)?status:"UNKNOWN";
    }
    public Task<AdminCounts> adminCounts(String uid){
        return withProof(uid,proof->{
            Task<AggregateQuerySnapshot> seeker=db.collection("retirementRequests").whereEqualTo("status","SUBMITTED")
                    .limit(50).count().get(AggregateSource.SERVER);
            Task<AggregateQuerySnapshot> employer=db.collection("identityRequests").whereEqualTo("status","SUBMITTED")
                    .limit(50).count().get(AggregateSource.SERVER);
            return Tasks.whenAll(seeker,employer).continueWith(done->{
                if(!done.isSuccessful())throw done.getException();
                if(!same(uid))throw new Failure("SESSION");
                return new AdminCounts(seeker.getResult().getCount(),employer.getResult().getCount());
            });
        });
    }
    public Task<List<Area>> areas() {
        return db.collection("areas").whereEqualTo("active", true).get(Source.SERVER).continueWith(task -> {
            if (!task.isSuccessful()) throw task.getException();
            List<Area> areas = new ArrayList<>();
            for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                Object value = doc.get("name");
                if (!(value instanceof String) || ((String) value).trim().isEmpty())
                    throw new Failure("AREA_SCHEMA");
                areas.add(new Area(doc.getId(), ((String) value).trim()));
            }
            areas.sort((a, b) -> a.code.compareTo(b.code));
            return areas;
        });
    }
    public Task<Void> choose(String uid, String role, String initialName) {
        if (!"SEEKER".equals(role) && !"EMPLOYER".equals(role)) return Tasks.forException(new Failure("ROLE"));
        return withProof(uid, proof -> db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");
            DocumentReference ref = db.collection("users").document(uid);
            DocumentSnapshot existing = tx.get(ref);
            DocumentSnapshot admin = tx.get(db.collection("adminAccess").document(uid));
            if (admin.exists() && Boolean.TRUE.equals(admin.getBoolean("enabled"))) throw new Failure("ROLE");
            if (existing.exists()) return null; // First server commit owns role; caller reloads it.
            Map<String, Object> user = new HashMap<>();
            user.put("id", uid); user.put("role", role); user.put("accountStatus", "ACTIVE");
            user.put("displayName", initialName == null ? "" : initialName.trim().substring(0, Math.min(100, initialName.trim().length())));
            user.put("phoneE164", proof.phone); user.put("phoneVerificationMode", proof.phone.isEmpty() ? "NONE" : "FIREBASE_AUTH");
            user.put("phoneVerifiedAt", proof.phone.isEmpty() ? null : FieldValue.serverTimestamp());
            user.put("version", 1L); user.put("createdAt", FieldValue.serverTimestamp()); user.put("updatedAt", FieldValue.serverTimestamp());
            user.put("SEEKER".equals(role) ? "retirementStatus" : "identityStatus", "NOT_SUBMITTED");
            ProfileDraft draft = new ProfileDraft();
            Map<String, Object> profile = profileData(uid, role, draft);
            profile.put("createdAt", FieldValue.serverTimestamp());
            tx.set(ref, user); tx.set(profileRef(uid, role), profile);
            return null;
        }));
    }
    public Task<Void> save(Snapshot snapshot, ProfileDraft draft) {
        if (!draft.valid(snapshot.role)) return Tasks.forException(new Failure("VALIDATION"));
        return withProof(snapshot.uid, proof -> db.runTransaction(tx -> {
            if (!same(proof.uid)) throw new Failure("SESSION");
            DocumentReference ref = db.collection("users").document(proof.uid);
            DocumentReference profile = profileRef(proof.uid, snapshot.role);
            DocumentSnapshot user = tx.get(ref);
            DocumentSnapshot oldProfile = tx.get(profile);
            if (!user.exists() || !oldProfile.exists() || !snapshot.role.equals(user.getString("role"))) throw new Failure("SCHEMA");
            if (!Long.valueOf(snapshot.version).equals(user.getLong("version"))) throw new Failure("CONFLICT");
            if (!"ACTIVE".equals(user.getString("accountStatus"))) throw new Failure("INACTIVE");
            if (!draft.areaCode.isEmpty()) {
                DocumentSnapshot area = tx.get(db.collection("areas").document(draft.areaCode));
                if (!area.exists() || !Boolean.TRUE.equals(area.getBoolean("active"))) throw new Failure("AREA");
            }
            Map<String, Object> updates = contact(proof, user);
            updates.put("displayName", draft.displayName.trim());
            updates.put("version", snapshot.version + 1); updates.put("updatedAt", FieldValue.serverTimestamp());
            Map<String, Object> data = profileData(proof.uid, snapshot.role, draft);
            // Preserve identity fields written by the verification transaction.
            tx.update(ref, updates); tx.update(profile, data);
            return null;
        }));
    }
    public Task<Void> syncPhone(String uid) {
        return withProof(uid, proof -> db.runTransaction(tx -> {
            if (!same(uid)) throw new Failure("SESSION");
            DocumentReference ref = db.collection("users").document(uid);
            DocumentSnapshot user = tx.get(ref);
            if (!user.exists() || "ADMIN".equals(user.getString("role"))) return null;
            if (!"ACTIVE".equals(user.getString("accountStatus"))) throw new Failure("INACTIVE");
            if (proof.phone.equals(text(user, "phoneE164"))) return null;
            Long version = user.getLong("version");
            if (version == null) throw new Failure("SCHEMA");
            Map<String, Object> updates = contact(proof, user);
            updates.put("version", version + 1); updates.put("updatedAt", FieldValue.serverTimestamp());
            tx.update(ref, updates);
            return null;
        }));
    }
    private Map<String, Object> contact(Proof proof, DocumentSnapshot old) {
        Map<String, Object> map = new HashMap<>();
        map.put("phoneE164", proof.phone);
        map.put("phoneVerificationMode", proof.phone.isEmpty() ? "NONE" : "FIREBASE_AUTH");
        map.put("phoneVerifiedAt", proof.phone.isEmpty() ? null : proof.phone.equals(text(old, "phoneE164"))
                ? old.get("phoneVerifiedAt") : FieldValue.serverTimestamp());
        return map;
    }
    private DocumentReference profileRef(String uid, String role) {
        return db.collection("SEEKER".equals(role) ? "jobSeekerProfiles" : "employerProfiles").document(uid);
    }
    private Map<String, Object> profileData(String uid, String role, ProfileDraft d) {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", uid); map.put("areaCode", d.areaCode); map.put("updatedAt", FieldValue.serverTimestamp());
        if ("SEEKER".equals(role)) {
            map.put("skills", d.skillList()); map.put("experienceSummary", d.experience.trim()); map.put("preferredShifts", d.shifts());
        } else {
            map.put("employerType", d.employerType);
            map.put("organizationName", "ORGANIZATION".equals(d.employerType) ? d.organizationName.trim() : "");
            map.put("introduction", d.introduction.trim());
        }
        return map;
    }
    private static String text(DocumentSnapshot doc, String field) {
        Object value = doc.get(field); return value instanceof String ? (String) value : "";
    }
    private static List<String> strings(DocumentSnapshot doc, String field) {
        Object value = doc.get(field);
        if (!(value instanceof List<?>)) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (Object item : (List<?>) value) if (item instanceof String) result.add((String) item);
        return result;
    }
}
