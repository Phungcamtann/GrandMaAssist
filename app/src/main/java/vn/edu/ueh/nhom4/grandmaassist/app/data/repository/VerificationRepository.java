package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.*;
import com.google.firebase.storage.*;
import java.util.*;
import java.util.function.Supplier;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.CardIdentity;

/** Online-only operations; server Rules are the authority, including every Admin action. */
public final class VerificationRepository {
    /** Status text, not an Admin-authored assessment. Keeps existing request/notice Rules compatible. */
    public static final String APPROVAL_REASON = "Hồ sơ đã được chấp thuận.";
    public static final class Problem extends RuntimeException {
        public final String code;
        public Problem(String code) { super(code); this.code = code; }
    }
    public static final class Record {
        public final String id, collection;
        public final Map<String,Object> data;
        public Record(DocumentSnapshot doc) {
            id = doc.getId(); collection = doc.getReference().getParent().getId(); data = doc.getData();
            if(data==null || !(data.get("identitySnapshot") instanceof Map)
                    || !(data.get("professionalSnapshot") instanceof Map) || !(data.get("imagePaths") instanceof List)
                    || text("userId").isEmpty() || !Arrays.asList("SEEKER","EMPLOYER").contains(text("role")))
                throw new Problem("PROFILE");
            if(!(identity().get("fullName") instanceof String) || !(identity().get("dateOfBirth") instanceof String)
                    || !(identity().get("documentNumber") instanceof String) || paths().isEmpty() || paths().size()>4)
                throw new Problem("PROFILE");
            int index=0;
            for(Object path:paths())if(!(path instanceof String) || !path.equals(path(text("userId"),id,index++)))
                throw new Problem("PROFILE");
        }
        public String text(String key) { Object v = data.get(key); return v instanceof String ? (String)v : ""; }
        public long number(String key) { Object v = data.get(key); return v instanceof Number ? ((Number)v).longValue() : 0; }
        @SuppressWarnings("unchecked") public Map<String,Object> identity() {
            return (Map<String,Object>) data.get("identitySnapshot");
        }
        @SuppressWarnings("unchecked") public List<String> paths() { return (List<String>)data.get("imagePaths"); }
    }
    public static final class Home {
        public final String uid, role, status, currentId;
        public final long userVersion, identityVersion;
        public final boolean active, phone, complete;
        public final String profileRequirement;
        public final Record request;
        Home(String uid, String role, DocumentSnapshot u, DocumentSnapshot p, Record r, boolean phone) {
            this.uid=uid; this.role=role; active="ACTIVE".equals(u.getString("accountStatus"));
            status=text(u, statusField(role)); currentId=text(u, pointer(role));
            userVersion=number(u,"version"); identityVersion=p==null?0:number(p,"identityProfileVersion");
            request=r; this.phone=phone;
            profileRequirement=missingProfileDetail(role,u,p);
            complete=profileRequirement.isEmpty();
        }
    }
    private static String missingProfileDetail(String role,DocumentSnapshot u,DocumentSnapshot p){
        if(p==null)return "Mở Hồ sơ và lưu thông tin trước khi gửi xác minh.";
        if(text(u,"displayName").trim().isEmpty())return "Bổ sung tên hiển thị trong Hồ sơ rồi bấm Lưu.";
        if(text(p,"areaCode").isEmpty())return "Chọn khu vực trong Hồ sơ rồi bấm Lưu.";
        if("SEEKER".equals(role)){
            if(!(p.get("skills") instanceof List) || ((List<?>)p.get("skills")).isEmpty())return "Bổ sung kỹ năng trong Hồ sơ rồi bấm Lưu.";
            if(text(p,"experienceSummary").trim().isEmpty())return "Bổ sung kinh nghiệm trong Hồ sơ rồi bấm Lưu.";
            if(!(p.get("preferredShifts") instanceof List) || ((List<?>)p.get("preferredShifts")).isEmpty())return "Chọn buổi làm phù hợp trong Hồ sơ rồi bấm Lưu.";
        }else if(text(p,"introduction").trim().isEmpty())return "Bổ sung giới thiệu trong Hồ sơ người thuê rồi bấm Lưu.";
        return "";
    }
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseStorage storage = FirebaseStorage.getInstance();
    private final List<UploadTask> uploads=Collections.synchronizedList(new ArrayList<>());
    private long transferGeneration;
    public VerificationRepository(){ storage.setMaxUploadRetryTimeMillis(60_000); storage.setMaxDownloadRetryTimeMillis(60_000); }
    public void cancelUploads(){ ++transferGeneration;synchronized(uploads){for(UploadTask task:new ArrayList<>(uploads))task.cancel();} }
    public boolean same(String uid) { FirebaseUser u=auth.getCurrentUser(); return u!=null && uid.equals(u.getUid()); }
    public static String collection(String role) { return "SEEKER".equals(role)?"retirementRequests":"identityRequests"; }
    public static String pointer(String role) { return "SEEKER".equals(role)?"currentRetirementRequestId":"identityRequestId"; }
    public static String statusField(String role) { return "SEEKER".equals(role)?"retirementStatus":"identityStatus"; }
    private DocumentReference user(String uid) { return db.collection("users").document(uid); }
    private DocumentReference profile(String uid,String role) {
        return db.collection("SEEKER".equals(role)?"jobSeekerProfiles":"employerProfiles").document(uid);
    }
    private <T> Task<T> fresh(String uid, Supplier<Task<T>> action) {
        FirebaseUser u=auth.getCurrentUser();
        if(u==null || !same(uid)) return Tasks.forException(new Problem("SESSION"));
        return u.getIdToken(true).continueWithTask(t->{
            if(!t.isSuccessful()) throw t.getException();
            if(!same(uid)) throw new Problem("SESSION");
            return action.get();
        });
    }
    public Task<Home> home(String uid) {
        return fresh(uid, ()->db.runTransaction(tx->{
            if(!same(uid)) throw new Problem("SESSION");
            DocumentSnapshot u=tx.get(user(uid));
            String role=text(u,"role");
            if("ADMIN".equals(role)) {
                DocumentSnapshot a=tx.get(db.collection("adminAccess").document(uid));
                if(!Boolean.TRUE.equals(a.getBoolean("enabled")) || !"ACTIVE".equals(u.getString("accountStatus")))
                    throw new Problem("ACCESS");
                return new Home(uid,role,u,null,null,false);
            }
            if(!Arrays.asList("SEEKER","EMPLOYER").contains(role)) throw new Problem("PROFILE");
            DocumentSnapshot p=tx.get(profile(uid,role));
            if(!p.exists()) throw new Problem("PROFILE");
            String id=text(u,pointer(role)); Record r=null;
            if(!id.isEmpty()) {
                DocumentSnapshot doc=tx.get(db.collection(collection(role)).document(id));
                if(!doc.exists()) throw new Problem("PROFILE");
                r=new Record(doc);
            }
            FirebaseUser current=auth.getCurrentUser();
            boolean phone=current!=null && current.getPhoneNumber()!=null && !current.getPhoneNumber().isEmpty()
                    && current.getPhoneNumber().equals(text(u,"phoneE164"));
            return new Home(uid,role,u,p,r,phone);
        }));
    }
    public Task<Record> request(String uid,String role,String id) {
        return fresh(uid,()->db.collection(collection(role)).document(id).get(Source.SERVER).continueWith(t->{
            if(!t.isSuccessful()) throw t.getException();
            if(!same(uid)) throw new Problem("SESSION");
            if(!t.getResult().exists()) throw new Problem("MISSING");
            return new Record(t.getResult());
        }));
    }
    /** Stage is immutable. Retry the same ID only for the same frozen form and image bytes. */
    public Task<Void> stage(Home base,String id,CardIdentity identity,String evidence,int imageCount) {
        boolean seeker="SEEKER".equals(base.role),employer="EMPLOYER".equals(base.role);
        if(identity==null || (!seeker && !employer)
                || (seeker && (imageCount<2 || imageCount>4 || !Arrays.asList("RETIREMENT_CERTIFICATE","BHXH_SIMULATED").contains(evidence)))
                || (employer && (imageCount!=1 || !"IDENTITY_SAMPLE".equals(evidence))))
            return Tasks.forException(new Problem("FORM"));
        return fresh(base.uid,()->db.runTransaction(tx->{
            if(!same(base.uid)) throw new Problem("SESSION");
            DocumentReference ref=db.collection(collection(base.role)).document(id);
            DocumentSnapshot existing=tx.get(ref), u=tx.get(user(base.uid));
            DocumentSnapshot p=tx.get(profile(base.uid,base.role));
            if(existing.exists()) return null;
            if(number(u,"version")!=base.userVersion || number(p,"identityProfileVersion")!=base.identityVersion)
                throw new Problem("CONFLICT");
            if(!"ACTIVE".equals(u.getString("accountStatus")) || "SUBMITTED".equals(text(u,statusField(base.role))))
                throw new Problem("STATE");
            Map<String,Object> d=new HashMap<>();
            d.put("id",id); d.put("userId",base.uid); d.put("role",base.role);
            d.put("status","STAGING"); d.put("previousRequestId",base.currentId);
            d.put("identityProfileVersion",base.identityVersion+1); d.put("baseUserVersion",base.userVersion);
            d.put("identitySnapshot",identity.data()); d.put("evidenceType",evidence);
            Map<String,Object> profession=new HashMap<>();
            profession.put("displayName",text(u,"displayName")); profession.put("areaCode",text(p,"areaCode"));
            if("SEEKER".equals(base.role)) {
                profession.put("skills",p.get("skills")); profession.put("experienceSummary",text(p,"experienceSummary"));
                profession.put("preferredShifts",p.get("preferredShifts"));
            } else {
                profession.put("employerType",text(p,"employerType")); profession.put("organizationName",text(p,"organizationName"));
                profession.put("introduction",text(p,"introduction"));
            }
            d.put("professionalSnapshot",profession);
            List<String> paths=new ArrayList<>();
            for(int i=0;i<imageCount;i++) paths.add(path(base.uid,id,i));
            d.put("imagePaths",paths); d.put("lookupMode","SIMULATED"); d.put("lookupResult","");
            d.put("reason",""); d.put("reviewedBy",""); d.put("reviewedAt",null); d.put("decisionVersion",0L);
            d.put("createdAt",FieldValue.serverTimestamp()); d.put("updatedAt",FieldValue.serverTimestamp());
            d.put("consentedAt",FieldValue.serverTimestamp()); d.put("submittedAt",null);
            tx.set(ref,d); return null;
        }));
    }
    public static String path(String uid,String id,int index) {
        return "verification/"+uid+"/"+id+"/"+(index==0?"identity.jpg":"evidence"+index+".jpg");
    }
    public Task<Void> upload(String uid,String path,byte[] bytes) {
        if(!same(uid)) return Tasks.forException(new Problem("SESSION"));
        StorageReference ref=storage.getReference().child(path);
        long generation=transferGeneration;
        // Existing files cannot be overwritten. A resume may reuse exactly the same content.
        return ref.getMetadata().continueWithTask(meta->{
            if(!same(uid) || generation!=transferGeneration) throw new Problem("SESSION");
            if(meta.isSuccessful()) {
                if(!VerificationImages.md5(bytes).equals(meta.getResult().getMd5Hash())) throw new Problem("IMAGE");
                return Tasks.forResult(null);
            }
            Exception e=meta.getException();
            if(!(e instanceof StorageException) || ((StorageException)e).getErrorCode()!=StorageException.ERROR_OBJECT_NOT_FOUND)
                throw e;
            UploadTask upload=ref.putBytes(bytes,new StorageMetadata.Builder().setContentType("image/jpeg").build());
            uploads.add(upload);upload.addOnCompleteListener(t->uploads.remove(upload));
            return upload.continueWith(t->{ if(!t.isSuccessful()) throw t.getException(); return null; });
        });
    }
    public Task<Void> submit(Home base,String id) {
        return fresh(base.uid,()->db.runTransaction(tx->{
            if(!same(base.uid)) throw new Problem("SESSION");
            DocumentReference ref=db.collection(collection(base.role)).document(id);
            DocumentSnapshot r=tx.get(ref),u=tx.get(user(base.uid)),p=tx.get(profile(base.uid,base.role));
            if(!r.exists()) throw new Problem("MISSING");
            if(!"STAGING".equals(text(r,"status"))) {
                if(id.equals(text(u,pointer(base.role)))) return null;
                throw new Problem("CONFLICT");
            }
            if(number(u,"version")!=number(r,"baseUserVersion") || number(p,"identityProfileVersion")+1!=number(r,"identityProfileVersion"))
                throw new Problem("CONFLICT");
            Map<String,Object> ident=(Map<String,Object>)r.get("identitySnapshot");
            Map<String,Object> ud=new HashMap<>();
            ud.put(statusField(base.role),"SUBMITTED"); ud.put(pointer(base.role),id);
            ud.put("approvedIdentityProfileVersion",0L); ud.put("version",number(u,"version")+1);
            ud.put("updatedAt",FieldValue.serverTimestamp());
            Map<String,Object> pd=new HashMap<>();
            pd.put("fullName",ident.get("fullName")); pd.put("dateOfBirth",ident.get("dateOfBirth"));
            pd.put("identityProfileVersion",number(r,"identityProfileVersion")); pd.put("updatedAt",FieldValue.serverTimestamp());
            tx.update(ref,"status","SUBMITTED","submittedAt",FieldValue.serverTimestamp(),"updatedAt",FieldValue.serverTimestamp());
            tx.update(user(base.uid),ud); tx.update(profile(base.uid,base.role),pd); return null;
        }));
    }
    public Task<List<Record>> queue(String uid,String role) {
        return fresh(uid,()->db.collection(collection(role)).whereEqualTo("status","SUBMITTED")
                .limit(50).get(Source.SERVER).continueWith(t->{
                    if(!t.isSuccessful()) throw t.getException();
                    if(!same(uid)) throw new Problem("SESSION");
                    List<Record> result=new ArrayList<>();
                    for(DocumentSnapshot doc:t.getResult()) result.add(new Record(doc));
                    result.sort(Comparator.comparing(r->String.valueOf(r.identity().get("fullName"))));
                    return result;
                }));
    }
    public Task<Record> find(String admin,String role,String key) {
        return fresh(admin,()->db.collection(collection(role)).document(key).get(Source.SERVER).continueWithTask(t->{
            if(!t.isSuccessful())throw t.getException();
            if(!same(admin))throw new Problem("SESSION");
            if(t.getResult().exists())return Tasks.forResult(new Record(t.getResult()));
            return user(key).get(Source.SERVER).continueWithTask(u->{
                if(!u.isSuccessful())throw u.getException();
                if(!same(admin))throw new Problem("SESSION");
                String id=text(u.getResult(),pointer(role));
                if(id.isEmpty() || !role.equals(text(u.getResult(),"role")))throw new Problem("MISSING");
                return request(admin,role,id);
            });
        }));
    }
    public Task<byte[]> image(String uid,String path) {
        if(!same(uid)) return Tasks.forException(new Problem("SESSION"));
        return storage.getReference().child(path).getBytes(VerificationImages.MAX).continueWith(t->{
            if(!t.isSuccessful()) throw t.getException();
            if(!same(uid)) throw new Problem("SESSION"); return t.getResult();
        });
    }
    public Task<Void> decide(String admin,Record expected,String decision,String lookup,String reason) {
        boolean approved=Arrays.asList("APPROVED","VERIFIED").contains(decision);
        String decisionReason=approved?APPROVAL_REASON:reason.trim();
        String role=expected.text("role");
        if(!Arrays.asList("NEEDS_INFO","REJECTED","SEEKER".equals(role)?"APPROVED":"VERIFIED").contains(decision)
                || !Arrays.asList("MATCH","NOT_MATCH","INSUFFICIENT").contains(lookup)
                || (approved && !"MATCH".equals(lookup)) || decisionReason.isEmpty() || decisionReason.length()>1000
                || decisionReason.matches("(?s).*[0-9]{12}.*"))return Tasks.forException(new Problem("FORM"));
        return fresh(admin,()->db.runTransaction(tx->{
            if(!same(admin)) throw new Problem("SESSION");
            DocumentReference ref=db.collection(expected.collection).document(expected.id);
            DocumentSnapshot r=tx.get(ref); String owner=text(r,"userId");
            if(!role.equals(text(r,"role")))throw new Problem("CONFLICT");
            DocumentSnapshot u=tx.get(user(owner)),p=tx.get(profile(owner,role));
            if(!"SUBMITTED".equals(text(r,"status")) || number(r,"decisionVersion")!=expected.number("decisionVersion")
                    || !expected.id.equals(text(u,pointer(role))) || number(p,"identityProfileVersion")!=number(r,"identityProfileVersion"))
                throw new Problem("CONFLICT");
            if(admin.equals(owner)) throw new Problem("FORM");
            Map<String,Object> update=new HashMap<>();
            update.put("status",decision); update.put("lookupResult",lookup); update.put("reason",decisionReason);
            update.put("reviewedBy",admin); update.put("reviewedAt",FieldValue.serverTimestamp());
            update.put("updatedAt",FieldValue.serverTimestamp()); update.put("decisionVersion",1L);
            Map<String,Object> ud=new HashMap<>(); ud.put(statusField(role),decision);
            ud.put("approvedIdentityProfileVersion",Arrays.asList("APPROVED","VERIFIED").contains(decision)?number(r,"identityProfileVersion"):0L);
            ud.put("version",number(u,"version")+1); ud.put("updatedAt",FieldValue.serverTimestamp());
            Map<String,Object> notice=new HashMap<>(); notice.put("requestId",expected.id);
            notice.put("decisionVersion",1L); notice.put("status",decision); notice.put("reason",decisionReason);
            notice.put("createdAt",FieldValue.serverTimestamp());
            tx.update(ref,update); tx.update(user(owner),ud);
            tx.set(user(owner).collection("notifications").document(expected.id+"_1"),notice); return null;
        }));
    }
    public Task<Void> revoke(String admin,Record expected,String reason) {
        return fresh(admin,()->db.runTransaction(tx->{
            if(!same(admin))throw new Problem("SESSION");
            DocumentReference ref=db.collection(expected.collection).document(expected.id);
            DocumentSnapshot r=tx.get(ref);String uid=text(r,"userId"),role=text(r,"role");
            DocumentSnapshot u=tx.get(user(uid)),p=tx.get(profile(uid,role));
            DocumentReference rev=db.collection("verificationRevocations").document(expected.id);
            DocumentSnapshot existing=tx.get(rev);
            if(existing.exists())throw new Problem("CONFLICT");
            if(!expected.id.equals(text(u,pointer(role))) || !Arrays.asList("APPROVED","VERIFIED").contains(text(u,statusField(role)))
                    || !text(u,statusField(role)).equals(text(r,"status")) || number(p,"identityProfileVersion")!=number(r,"identityProfileVersion"))
                throw new Problem("CONFLICT");
            Map<String,Object> d=new HashMap<>();d.put("requestId",expected.id);d.put("userId",uid);d.put("role",role);
            d.put("identityProfileVersion",number(r,"identityProfileVersion"));d.put("reason",reason.trim());
            d.put("reviewedBy",admin);d.put("createdAt",FieldValue.serverTimestamp());
            tx.set(rev,d);
            tx.update(user(uid),statusField(role),"REVERIFY_REQUIRED","approvedIdentityProfileVersion",0L,
                    "version",number(u,"version")+1,"updatedAt",FieldValue.serverTimestamp());
            Map<String,Object> n=new HashMap<>();n.put("requestId",expected.id);n.put("decisionVersion",2L);
            n.put("status","REVERIFY_REQUIRED");n.put("reason",reason.trim());n.put("createdAt",FieldValue.serverTimestamp());
            tx.set(user(uid).collection("notifications").document(expected.id+"_2"),n);return null;
        }));
    }
    public Task<List<Map<String,Object>>> notifications(String uid) {
        return fresh(uid,()->user(uid).collection("notifications").orderBy("createdAt",Query.Direction.DESCENDING)
                .limit(30).get(Source.SERVER).continueWith(t->{
                    if(!t.isSuccessful()) throw t.getException();
                    if(!same(uid)) throw new Problem("SESSION");
                    List<Map<String,Object>> list=new ArrayList<>();
                    for(DocumentSnapshot d:t.getResult()) list.add(d.getData()); return list;
                }));
    }
    static String text(DocumentSnapshot d,String key) { Object v=d.get(key); return v instanceof String?(String)v:""; }
    static long number(DocumentSnapshot d,String key) { Object v=d.get(key); return v instanceof Number?((Number)v).longValue():0; }
}
