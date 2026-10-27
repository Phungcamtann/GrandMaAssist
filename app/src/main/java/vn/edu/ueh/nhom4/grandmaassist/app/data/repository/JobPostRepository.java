package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import com.google.android.gms.tasks.*;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.*;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.*;
import vn.edu.ueh.nhom4.grandmaassist.app.BuildConfig;

/** Online transactions only. Every retry resolves the same job and submission version. */
public final class JobPostRepository {
    public static final class Failure extends RuntimeException {public final String code;public Failure(String c){super(c);code=c;}}
    private static final class DiagnosticFailure extends RuntimeException {
        final String stage;DiagnosticFailure(String stage,Exception cause){super(stage,cause);this.stage=stage;}
    }
    private static RuntimeException diagnostic(String stage,Exception exception){
        Throwable c=exception;String code="OTHER";
        for(int i=0;c!=null&&i<10;i++,c=c.getCause()){
            if(c instanceof DiagnosticFailure)return (DiagnosticFailure)c;
            if(c instanceof FirebaseFirestoreException)code=((FirebaseFirestoreException)c).getCode().name();
        }
        // No UID, document ID, phone, address, content, exception message or token is logged.
        if(BuildConfig.DEBUG)android.util.Log.w("GrandMaJobs","stage="+stage+" code="+code);
        return new DiagnosticFailure(stage,exception);
    }
    private DocumentSnapshot read(Transaction tx,DocumentReference ref,String stage) throws FirebaseFirestoreException {
        try{return tx.get(ref);}catch(FirebaseFirestoreException e){throw diagnostic(stage,e);}
    }
    private static boolean denied(Exception error){
        Throwable c=error;for(int i=0;c!=null&&i<10;i++,c=c.getCause())
            if(c instanceof FirebaseFirestoreException&&((FirebaseFirestoreException)c).getCode()==FirebaseFirestoreException.Code.PERMISSION_DENIED)return true;
        return false;
    }
    /** On an actual failed submission only: read owner-only rule predicates, without writing probe data. */
    private Task<Void> diagnoseWriteDenial(String uid,long expiresAt){
        Task<Void> chain=Tasks.forResult(null);
        for(String check:Arrays.asList("revision_20261026_3","account","phone","profile")){
            chain=chain.continueWithTask(t->{
                if(!t.isSuccessful())throw t.getException();if(!same(uid))throw new Failure("SESSION");
                return db.collection("jobSubmissionDiagnostics").document(uid).collection("checks").document(check)
                    .get(Source.SERVER).continueWith(read->{
                        if(!read.isSuccessful())throw diagnostic("C07_CHECK_"+(check.startsWith("revision_")?"RULESET":check.toUpperCase(Locale.ROOT)),read.getException());
                        if(!same(uid))throw new Failure("SESSION");return (Void)null;
                    });
            });
        }
        return chain.continueWithTask(t->{
            if(!t.isSuccessful())throw t.getException();if(!same(uid))throw new Failure("SESSION");
            return db.collection("jobSubmissionDiagnostics").document(uid).collection("expiry").document(String.valueOf(expiresAt))
                .get(Source.SERVER).continueWith(read->{
                    if(!read.isSuccessful())throw diagnostic("C07_CHECK_EXPIRY",read.getException());
                    if(!same(uid))throw new Failure("SESSION");return (Void)null;
                });
        });
    }
    public static final class Page {public final List<JobPost> items;public final DocumentSnapshot cursor;public final boolean more;
        Page(QuerySnapshot s){items=new ArrayList<>();for(DocumentSnapshot d:s)items.add(new JobPost(d));cursor=s.isEmpty()?null:s.getDocuments().get(s.size()-1);more=s.size()==20;}}
    private final FirebaseFirestore db=FirebaseFirestore.getInstance();
    public static <T> Task<T> bounded(Task<T> task){return Tasks.withTimeout(task,45,java.util.concurrent.TimeUnit.SECONDS);}
    public boolean same(String uid){FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();return u!=null&&u.getUid().equals(uid);}
    private Task<String> proof(String uid){FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();
        if(u==null||!same(uid))return Tasks.forException(new Failure("SESSION"));
        return u.getIdToken(true).continueWith(t->{if(!t.isSuccessful())throw t.getException();if(!same(uid))throw new Failure("SESSION");Object v=t.getResult().getClaims().get("phone_number");return v instanceof String?(String)v:"";});}
    public Task<Page> list(String uid,boolean admin,DocumentSnapshot cursor){
        if(!same(uid))return Tasks.forException(new Failure("SESSION"));
        Query q=db.collection("jobs");q=admin?q.whereEqualTo("status","PENDING_REVIEW"):q.whereEqualTo("employerId",uid);
        q=q.orderBy("updatedAt",Query.Direction.DESCENDING).orderBy(FieldPath.documentId(),Query.Direction.DESCENDING).limit(20);
        if(cursor!=null)q=q.startAfter(cursor);
        return bounded(q.get(Source.SERVER).continueWith(t->{if(!t.isSuccessful())throw t.getException();if(!same(uid))throw new Failure("SESSION");return new Page(t.getResult());}));
    }
    public Task<JobPost> get(String uid,String id){
        if(!same(uid))return Tasks.forException(new Failure("SESSION"));
        return bounded(db.collection("jobs").document(id).get(Source.SERVER).continueWith(t->{if(!t.isSuccessful())throw t.getException();if(!same(uid))throw new Failure("SESSION");if(!t.getResult().exists())throw new Failure("MISSING");return new JobPost(t.getResult());}));
    }
    private String text(DocumentSnapshot d,String k){String s=d.getString(k);return s==null?"":s;}
    private void boundedText(String value,int max,String code){if(value.trim().isEmpty()||value.length()>max)throw new Failure(code);}
    private void active(DocumentSnapshot user,String role){if(!user.exists()||!role.equals(user.getString("role"))||!"ACTIVE".equals(user.getString("accountStatus")))throw new Failure("ACCESS");}
    public Task<JobPost> submit(String uid,LocalJobDraft original){
        final LocalJobDraft d=original.copy();if(!uid.equals(d.ownerUid))return Tasks.forException(new Failure("SESSION"));
        final String op=d.id+"_s"+(d.serverBaseVersion+1);
        return bounded(proof(uid).continueWithTask(p->{
            if(!p.isSuccessful())throw p.getException();String phone=p.getResult();
            return db.runTransaction(tx->{
                if(!same(uid))throw new Failure("SESSION");DocumentReference ref=db.collection("jobs").document(d.id);
                DocumentSnapshot old=read(tx,ref,"C07_READ_JOB");
                // Recovery must also work after an Admin has already decided this submission.
                if(old.exists()&&uid.equals(old.getString("employerId"))&&op.equals(old.getString("lastSubmissionId")))return null;
                DocumentSnapshot user=read(tx,db.collection("users").document(uid),"C07_READ_USER");
                DocumentSnapshot access=read(tx,db.collection("adminAccess").document(uid),"C07_READ_ACCESS");
                DocumentSnapshot profile=read(tx,db.collection("employerProfiles").document(uid),"C07_READ_PROFILE");
                DocumentSnapshot cat=read(tx,db.collection("jobCategories").document(d.categoryId),"C07_READ_CATEGORY");
                DocumentSnapshot area=read(tx,db.collection("areas").document(d.areaCode),"C07_READ_AREA");
                active(user,"EMPLOYER");if(Boolean.TRUE.equals(access.getBoolean("enabled")))throw new Failure("ACCESS");
                if(phone.isEmpty()||!phone.equals(user.getString("phoneE164"))||!"FIREBASE_AUTH".equals(user.getString("phoneVerificationMode"))
                    ||!(user.get("phoneVerifiedAt") instanceof Timestamp))throw new Failure("PHONE");
                if(!profile.exists()||text(profile,"introduction").trim().isEmpty()||text(profile,"areaCode").isEmpty()
                    ||!uid.equals(profile.getString("userId"))||!Arrays.asList("INDIVIDUAL","ORGANIZATION").contains(profile.getString("employerType"))
                    ||text(user,"displayName").trim().isEmpty()||("ORGANIZATION".equals(profile.getString("employerType"))&&text(profile,"organizationName").trim().isEmpty()))throw new Failure("PROFILE");
                DocumentSnapshot profileArea=read(tx,db.collection("areas").document(text(profile,"areaCode")),"C07_READ_PROFILE_AREA");
                if(!Boolean.TRUE.equals(profileArea.getBoolean("active")))throw new Failure("PROFILE");
                if(!Boolean.TRUE.equals(cat.getBoolean("active"))||!Boolean.TRUE.equals(area.getBoolean("active")))throw new Failure("CATALOG");
                boundedText(text(user,"displayName"),100,"PROFILE_NAME");
                boundedText(text(profile,"introduction"),1000,"PROFILE_INTRODUCTION");
                if("ORGANIZATION".equals(profile.getString("employerType")))boundedText(text(profile,"organizationName"),150,"PROFILE_ORGANIZATION");
                boundedText(text(cat,"name"),120,"CATEGORY_NAME");boundedText(text(area,"name"),120,"AREA_NAME");
                if(!d.validate(Collections.singleton(d.categoryId),Collections.singleton(d.areaCode),System.currentTimeMillis()).isEmpty())throw new Failure("FORM");
                if(old.exists()){
                    if(!uid.equals(old.getString("employerId"))||!Arrays.asList("REJECTED","OPEN").contains(old.getString("status"))||!Objects.equals(old.getLong("version"),d.serverBaseVersion))throw new Failure("CONFLICT");
                }else if(d.serverBaseVersion!=0)throw new Failure("CONFLICT");
                long version=d.serverBaseVersion+1;
                Map<String,Object> m=new HashMap<>();m.put("id",d.id);m.put("employerId",uid);
                String name="ORGANIZATION".equals(profile.getString("employerType"))?text(profile,"organizationName"):text(user,"displayName");
                m.put("employerDisplayName",name);m.put("title",d.title.trim());m.put("categoryId",d.categoryId);m.put("categoryName",text(cat,"name"));
                m.put("areaCode",d.areaCode);m.put("areaName",text(area,"name"));m.put("workAddress",d.workAddress.trim());m.put("description",d.description.trim());
                m.put("requirementsText",d.requirements.trim());m.put("requiredSkillTags",d.skills());m.put("workDays",new ArrayList<>(d.days));
                m.put("workShift",d.shift);m.put("scheduleText",d.schedule.trim());m.put("physicalDemands",new ArrayList<>(d.demands));m.put("demandsDeclared",true);
                m.put("payAmount",d.payAmount());m.put("payUnit",d.payUnit);m.put("expiresAt",new Timestamp(new Date(d.expiresAt)));
                m.put("status","PENDING_REVIEW");m.put("version",version);m.put("lastSubmissionId",op);
                m.put("createdAt",old.exists()?old.get("createdAt"):FieldValue.serverTimestamp());m.put("updatedAt",FieldValue.serverTimestamp());
                m.put("reviewedBy","");m.put("reviewedAt",null);m.put("reviewReason","");
                tx.set(ref,m);tx.set(ref.collection("history").document(String.valueOf(version)),event(m,old,uid,"SUBMITTED"));return null;
            }).continueWithTask(t->{
                if(t.isSuccessful())return get(uid,d.id);
                Exception error=t.getException();
                RuntimeException tagged=diagnostic("C07_SUBMIT_TRANSACTION",error);
                if(!denied(error)||!"C07_SUBMIT_TRANSACTION".equals(((DiagnosticFailure)tagged).stage))throw tagged;
                return diagnoseWriteDenial(uid,d.expiresAt).continueWithTask(check->{
                    if(!check.isSuccessful())throw check.getException();
                    throw diagnostic("C07_WRITE_CONTENT_OR_HISTORY",error);
                });
            });
        }));
    }
    private Map<String,Object> event(Map<String,Object> after,DocumentSnapshot before,String actor,String action){
        Map<String,Object> e=new HashMap<>();e.put("jobId",after.get("id"));e.put("version",after.get("version"));
        e.put("actorId",actor);e.put("action",action);e.put("createdAt",FieldValue.serverTimestamp());
        e.put("before",before.exists()?before.getData():null);e.put("after",new HashMap<>(after));return e;
    }
    public Task<JobPost> decide(String uid,JobPost expected,boolean approve,String reason){
        String note=approve?"":reason.trim();if(!approve&&(note.isEmpty()||note.length()>1000))return Tasks.forException(new Failure("REASON"));
        return bounded(proof(uid).continueWithTask(p->{if(!p.isSuccessful())throw p.getException();return db.runTransaction(tx->{
            if(!same(uid))throw new Failure("SESSION");DocumentReference ref=db.collection("jobs").document(expected.content.id);
            DocumentSnapshot old=tx.get(ref),u=tx.get(db.collection("users").document(uid)),a=tx.get(db.collection("adminAccess").document(uid));
            active(u,"ADMIN");if(!Boolean.TRUE.equals(a.getBoolean("enabled")))throw new Failure("ACCESS");
            if(!old.exists()||uid.equals(old.getString("employerId"))||!"PENDING_REVIEW".equals(old.getString("status"))||!Objects.equals(old.getLong("version"),expected.version))throw new Failure("CONFLICT");
            if(approve){
                DocumentSnapshot category=tx.get(db.collection("jobCategories").document(old.getString("categoryId")));
                DocumentSnapshot area=tx.get(db.collection("areas").document(old.getString("areaCode")));
                if(!Boolean.TRUE.equals(category.getBoolean("active"))||!Boolean.TRUE.equals(area.getBoolean("active")))throw new Failure("CATALOG");
                if(old.getTimestamp("expiresAt").toDate().getTime()<=System.currentTimeMillis())throw new Failure("EXPIRED");
            }
            Map<String,Object> m=new HashMap<>(Objects.requireNonNull(old.getData()));m.put("version",expected.version+1);
            m.put("status",approve?"OPEN":"REJECTED");m.put("reviewReason",note);m.put("reviewedBy",uid);
            m.put("reviewedAt",FieldValue.serverTimestamp());m.put("updatedAt",FieldValue.serverTimestamp());
            tx.set(ref,m);tx.set(ref.collection("history").document(String.valueOf(expected.version+1)),event(m,old,uid,approve?"APPROVED":"REJECTED"));return null;
        }).continueWithTask(t->{if(!t.isSuccessful())throw t.getException();return get(uid,expected.content.id);});}));
    }

    public Task<JobPost> close(String uid, JobPost expected) {
        return bounded(proof(uid).continueWithTask(p -> {
            if (!p.isSuccessful()) throw p.getException();
            return db.runTransaction(tx -> {
                if (!same(uid)) throw new Failure("SESSION");
                DocumentReference ref = db.collection("jobs").document(expected.content.id);
                DocumentSnapshot old = tx.get(ref);
                if (!old.exists() || !uid.equals(old.getString("employerId")) || !"OPEN".equals(old.getString("status")) || !Objects.equals(old.getLong("version"), expected.version))
                    throw new Failure("CONFLICT");
                long nextVersion = expected.version + 1;
                Map<String, Object> m = new HashMap<>(Objects.requireNonNull(old.getData()));
                m.put("version", nextVersion);
                m.put("status", "CLOSED");
                m.put("updatedAt", FieldValue.serverTimestamp());
                tx.set(ref, m);
                tx.set(ref.collection("history").document(String.valueOf(nextVersion)), event(m, old, uid, "CLOSED"));
                return null;
            }).continueWithTask(t -> {
                if (!t.isSuccessful()) throw t.getException();
                return get(uid, expected.content.id);
            });
        }));
    }

    public Task<JobPost> hide(String uid, JobPost expected, String reason) {
        String note = reason == null ? "" : reason.trim();
        if (note.isEmpty() || note.length() > 1000) return Tasks.forException(new Failure("REASON"));
        return bounded(proof(uid).continueWithTask(p -> {
            if (!p.isSuccessful()) throw p.getException();
            return db.runTransaction(tx -> {
                if (!same(uid)) throw new Failure("SESSION");
                DocumentReference ref = db.collection("jobs").document(expected.content.id);
                DocumentSnapshot old = tx.get(ref), u = tx.get(db.collection("users").document(uid)), a = tx.get(db.collection("adminAccess").document(uid));
                active(u, "ADMIN");
                if (!Boolean.TRUE.equals(a.getBoolean("enabled"))) throw new Failure("ACCESS");
                if (!old.exists() || uid.equals(old.getString("employerId")) || !Arrays.asList("OPEN", "CLOSED").contains(old.getString("status")) || !Objects.equals(old.getLong("version"), expected.version))
                    throw new Failure("CONFLICT");
                long nextVersion = expected.version + 1;
                Map<String, Object> m = new HashMap<>(Objects.requireNonNull(old.getData()));
                m.put("version", nextVersion);
                m.put("status", "HIDDEN");
                m.put("reviewReason", note);
                m.put("reviewedBy", uid);
                m.put("reviewedAt", FieldValue.serverTimestamp());
                m.put("updatedAt", FieldValue.serverTimestamp());
                tx.set(ref, m);
                tx.set(ref.collection("history").document(String.valueOf(nextVersion)), event(m, old, uid, "HIDDEN"));
                return null;
            }).continueWithTask(t -> {
                if (!t.isSuccessful()) throw t.getException();
                return get(uid, expected.content.id);
            });
        }));
    }
    public static String error(Exception ex){Throwable c=ex;for(int i=0;c!=null&&i<10;i++,c=c.getCause()){
        if(c instanceof DiagnosticFailure){
            DiagnosticFailure failure=(DiagnosticFailure)c;Throwable nested=failure.getCause();
            for(int j=0;nested!=null&&j<10;j++,nested=nested.getCause())if(nested instanceof FirebaseFirestoreException
                    &&((FirebaseFirestoreException)nested).getCode()==FirebaseFirestoreException.Code.PERMISSION_DENIED){
                switch(failure.stage){
                    case "C07_CHECK_RULESET":return "Chưa áp dụng đúng Rules chẩn đoán (C07_CHECK_RULESET). Publish toàn bộ file Rules mới trên đúng dự án.";
                    case "C07_CHECK_ACCOUNT":return "Rules chặn quyền tài khoản (C07_CHECK_ACCOUNT). Kiểm tra EMPLOYER, ACTIVE và adminAccess của tài khoản.";
                    case "C07_CHECK_PHONE":return "Rules chặn số đã xác minh (C07_CHECK_PHONE). Kiểm tra đồng bộ số và thời điểm xác minh trên máy chủ.";
                    case "C07_CHECK_PROFILE":return "Rules chặn hồ sơ nghề nghiệp (C07_CHECK_PROFILE). Kiểm tra tên hiển thị, userId, khu vực và giới thiệu đã lưu.";
                    case "C07_CHECK_EXPIRY":return "Hạn nhận không còn ở tương lai theo máy chủ (C07_CHECK_EXPIRY). Chọn lại hạn nhận và kiểm tra ngày giờ thiết bị.";
                    case "C07_WRITE_CONTENT_OR_HISTORY":return "Quyền, số và hồ sơ đã qua kiểm tra Rules; transaction còn bị chặn ở nội dung hoặc lịch sử (C07_WRITE_CONTENT_OR_HISTORY).";
                    case "C07_READ_JOB":return "Firestore chưa cho đọc tin (C07_READ_JOB). Kiểm tra đã Publish Rules C07 trên đúng dự án.";
                    case "C07_READ_CATEGORY":return "Danh mục việc bị chặn đọc (C07_READ_CATEGORY). Kiểm tra danh mục còn hoạt động và Rules.";
                    case "C07_READ_AREA":case "C07_READ_PROFILE_AREA":return "Khu vực bị chặn đọc ("+failure.stage+"). Kiểm tra active và Rules.";
                    case "C07_SUBMIT_TRANSACTION":return "Firestore từ chối ghi tin và lịch sử (C07_SUBMIT_TRANSACTION). Cần đối chiếu Rules C07 với dữ liệu.";
                    default:return "Firestore từ chối bước "+failure.stage+". Cần kiểm tra quyền đọc dữ liệu của tài khoản.";
                }
            }
        }
        if(c instanceof java.util.concurrent.TimeoutException)return "Chưa xác định được kết quả sau 45 giây. Hãy tải lại hoặc gửi lại để đối chiếu; không tạo bản khác.";
        if(c instanceof Failure){switch(((Failure)c).code){
            case "PHONE":return "Cần xác minh và đồng bộ số điện thoại trong Tài khoản.";
            case "PROFILE":return "Cần hoàn thiện hồ sơ người thuê và khu vực hoạt động.";
            case "PROFILE_NAME":return "Tên hiển thị đã lưu phải có nội dung và tối đa 100 ký tự.";
            case "PROFILE_INTRODUCTION":return "Giới thiệu đã lưu phải có nội dung và tối đa 1.000 ký tự.";
            case "PROFILE_ORGANIZATION":return "Tên đơn vị đã lưu phải có nội dung và tối đa 150 ký tự.";
            case "CATEGORY_NAME":return "Tên danh mục trên Firebase không hợp lệ: cần nội dung, tối đa 120 ký tự.";
            case "AREA_NAME":return "Tên khu vực trên Firebase không hợp lệ: cần nội dung, tối đa 120 ký tự.";
            case "ACCESS":return "Tài khoản không có quyền thực hiện thao tác này.";
            case "CONFLICT":return "Tin đã thay đổi hoặc đã được xử lý. Hãy tải lại tin.";
            case "EXPIRED":return "Tin đã hết hạn nhận. Không thể chấp thuận.";
            case "REASON":return "Nhập lý do từ chối, tối đa 1.000 ký tự.";
            case "CATALOG":return "Danh mục hoặc khu vực không còn hoạt động. Hãy cập nhật lại.";
            case "FORM":return "Kiểm tra lại nội dung, địa chỉ và hạn nhận trước khi gửi.";
            case "MISSING":return "Tin không còn tồn tại.";
            case "SESSION":return "Tài khoản đã thay đổi. Hãy đăng nhập lại.";
        }}
        if(c instanceof FirebaseFirestoreException){FirebaseFirestoreException.Code code=((FirebaseFirestoreException)c).getCode();
            if(code==FirebaseFirestoreException.Code.PERMISSION_DENIED)return "Chưa đủ quyền hoặc dữ liệu không hợp lệ. Kiểm tra Rules và tải lại.";
            if(code==FirebaseFirestoreException.Code.FAILED_PRECONDITION)return "Chưa hoàn tất. Kiểm tra chỉ mục Firestore hoặc tải lại tin.";}
    }return JobDraftRepository.network(ex)?"Chưa kết nối được. Nếu vừa gửi, thử lại để đối chiếu đúng tin; nháp vẫn được giữ.":"Chưa hoàn tất thao tác. Hãy tải lại và thử lại.";}
}
