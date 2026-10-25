package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import android.content.Context;
import com.google.android.gms.tasks.*;
import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.*;
import java.util.*;
import java.util.concurrent.*;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.JobDraftStore;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.LocalJobDraft;

/** Only catalog reads use Firestore. Draft content never leaves SQLite in C06. */
public final class JobDraftRepository {
    private static final ExecutorService DB=Executors.newSingleThreadExecutor();
    private final JobDraftStore store;
    public JobDraftRepository(Context context){store=JobDraftStore.get(context);}
    public boolean same(String uid){com.google.firebase.auth.FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();return user!=null && uid.equals(user.getUid());}
    private <T> Task<T> local(String uid,Callable<T> call){return Tasks.call(DB,()->{if(!same(uid))throw new JobDraftStore.Failure("SESSION");return call.call();});}
    public Task<Void> authorize(String uid){
        return new ProfileRepository().load(uid).continueWithTask(t->{
            if(!t.isSuccessful())throw t.getException();
            ProfileRepository.Snapshot s=t.getResult();
            if(s==null || !"EMPLOYER".equals(s.role) || !"ACTIVE".equals(s.status))throw new JobDraftStore.Failure("ACCESS");
            return local(uid,()->{store.rememberOwner(uid);return null;});
        });
    }
    public Task<Boolean> offlineOwner(String uid){return local(uid,()->store.knownOwner(uid));}
    public Task<List<LocalJobDraft>> list(String uid){return local(uid,()->store.list(uid));}
    public Task<LocalJobDraft> read(String uid,String id){return local(uid,()->store.read(uid,id));}
    public Task<LocalJobDraft> save(String uid,LocalJobDraft d){return local(uid,()->store.save(uid,d));}
    public Task<Void> delete(String uid,String id,long revision){return local(uid,()->{store.delete(uid,id,revision);return null;});}
    public Task<JobDraftStore.Catalog> cached(String uid){return local(uid,store::catalog);}
    public Task<JobDraftStore.Catalog> refreshCatalog(String uid){
        if(!same(uid))return Tasks.forException(new JobDraftStore.Failure("SESSION"));
        FirebaseFirestore db=FirebaseFirestore.getInstance();
        Task<QuerySnapshot> categories=db.collection("jobCategories").whereEqualTo("active",true).limit(100).get(Source.SERVER);
        Task<QuerySnapshot> areas=db.collection("areas").whereEqualTo("active",true).limit(100).get(Source.SERVER);
        return Tasks.whenAll(categories,areas).continueWithTask(t->{
            if(!t.isSuccessful())throw t.getException();
            JobDraftStore.Catalog result=new JobDraftStore.Catalog();result.cachedAt=System.currentTimeMillis();
            for(int kind=0;kind<2;kind++)for(DocumentSnapshot d:(kind==0?categories:areas).getResult()){
                String name=d.getString("name"),icon=d.getString("iconKey");
                if(name==null || name.trim().isEmpty() || name.length()>120 || d.getId().length()>80)throw new JobDraftStore.Failure("CATALOG");
                (kind==0?result.categories:result.areas).add(new JobDraftStore.Item(d.getId(),name.trim(),icon==null?"":icon));
            }
            result.categories.sort(Comparator.comparing(i->i.name));result.areas.sort(Comparator.comparing(i->i.name));
            return local(uid,()->store.replaceCatalog(result));
        });
    }
    public static boolean network(Exception exception){
        Throwable c=exception;for(int i=0;c!=null&&i<8;i++,c=c.getCause()){
            if(c instanceof FirebaseNetworkException)return true;
            if(c instanceof com.google.firebase.auth.FirebaseAuthException
                    && "ERROR_NETWORK_REQUEST_FAILED".equals(((com.google.firebase.auth.FirebaseAuthException)c).getErrorCode()))return true;
            if(c instanceof FirebaseFirestoreException){FirebaseFirestoreException.Code code=((FirebaseFirestoreException)c).getCode();
                if(code==FirebaseFirestoreException.Code.UNAVAILABLE || code==FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)return true;
                if(code==FirebaseFirestoreException.Code.PERMISSION_DENIED || code==FirebaseFirestoreException.Code.UNAUTHENTICATED)return false;
            }
        }return false;
    }
    public static String error(Exception exception){
        Throwable c=exception;for(int i=0;c!=null&&i<8;i++,c=c.getCause()){
            if(c instanceof JobDraftStore.Failure){switch(((JobDraftStore.Failure)c).code){
                case "ACCESS":return "Tài khoản chưa có quyền soạn tin tuyển.";
                case "CONFLICT":return "Nháp đã thay đổi. Hãy tải lại trước khi sửa tiếp.";
                case "MISSING":return "Nháp không còn trên máy.";
                case "SESSION":return "Tài khoản đã thay đổi.";
                case "CATALOG":return "Danh mục chưa đúng. Nhờ người hỗ trợ kiểm tra.";
                case "DATA":case "MIGRATION":return "Chưa đọc được dữ liệu nháp. Nhờ người hỗ trợ kiểm tra.";
            }}
            if(c instanceof FirebaseFirestoreException && ((FirebaseFirestoreException)c).getCode()==FirebaseFirestoreException.Code.PERMISSION_DENIED)
                return "Chưa tải được danh mục. Nhờ người hỗ trợ kiểm tra.";
        }
        return network(exception)?"Chưa kết nối được. Có thể dùng nháp đã lưu trên máy.":"Chưa hoàn tất thao tác. Hãy thử lại.";
    }
}
