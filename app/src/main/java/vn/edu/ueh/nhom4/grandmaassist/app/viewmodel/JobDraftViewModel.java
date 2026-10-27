package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.google.firebase.auth.FirebaseAuth;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.JobDraftStore;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.JobDraftRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.JobPostRepository;
import com.google.android.gms.tasks.Tasks;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.LocalJobDraft;

/** A saved draft is durable SQLite; an unsaved edit is RAM only. No background publication. */
public final class JobDraftViewModel extends AndroidViewModel {
    public enum Page { LIST, EDIT, PREVIEW }
    public final MutableLiveData<Long> changes=new MutableLiveData<>(0L);
    private final JobDraftRepository repository;
    private final FirebaseAuth.AuthStateListener authListener;
    public final String uid;
    public boolean started,ready,busy,accessChecking,offlineAccess,catalogLoading,fromCache,dirty,readRequired,sessionEnded;
    private boolean knownOwner,startNew;
    private String pendingEditId="";
    public String submittedPostId="",submittedNotice="";
    public boolean showPosts,fixAccount,checkSent;
    private final JobPostRepository posts=new JobPostRepository();
    public Page page=Page.LIST;
    public String message="",catalogMessage="",exitRoute="";
    public List<LocalJobDraft> drafts=new ArrayList<>();
    public JobDraftStore.Catalog catalog=new JobDraftStore.Catalog();
    public LocalJobDraft draft;
    public LinkedHashMap<String,String> errors=new LinkedHashMap<>();
    public long validationEvent,pageEvent;
    private long generation,accessRequest,catalogRequest,formRequest;
    public JobDraftViewModel(@NonNull Application app){
        super(app);repository=new JobDraftRepository(app);
        com.google.firebase.auth.FirebaseUser user=FirebaseAuth.getInstance().getCurrentUser();uid=user==null?"":user.getUid();
        authListener=auth->{if(!repository.same(uid)){
            ++generation;sessionEnded=true;ready=busy=catalogLoading=accessChecking=false;
            draft=null;drafts.clear();catalog=new JobDraftStore.Catalog();errors.clear();message=catalogMessage="";emit();
        }};
        FirebaseAuth.getInstance().addAuthStateListener(authListener);
    }
    public void emit(){changes.setValue(changes.getValue()+1);}
    private boolean valid(long e){return e==generation && !sessionEnded && repository.same(uid);}
    public void initialize(boolean create,String editId){
        if(!started)pendingEditId=editId==null?"":editId;
        if(started || sessionEnded)return;started=true;startNew=create;busy=true;message="Đang mở nháp…";emit();
        long e=generation;
        repository.offlineOwner(uid).addOnCompleteListener(t->{
            if(!valid(e))return;busy=false;
            if(!t.isSuccessful()){message=JobDraftRepository.error(t.getException());emit();return;}
            knownOwner=Boolean.TRUE.equals(t.getResult());
            if(knownOwner){ready=true;offlineAccess=true;loadList();refreshCatalog();}
            refreshAccess();
        });
    }
    public void retry(){
        if(busy)return;
        if(!started){initialize(false,"");return;}
        if(!ready){refreshAccess();return;}loadList();
    }
    public void refreshAccess(){
        if(accessChecking || sessionEnded)return;accessChecking=true;long e=generation,a=++accessRequest;
        if(!ready)message="Đang kiểm tra tài khoản…";emit();
        repository.authorize(uid).addOnCompleteListener(t->{
            if(!valid(e)||a!=accessRequest)return;accessChecking=false;
            if(t.isSuccessful()){
                boolean wasReady=ready;ready=knownOwner=true;offlineAccess=false;
                if(!wasReady){loadList();refreshCatalog();}else emit();
            }else if(knownOwner && JobDraftRepository.network(t.getException())){
                offlineAccess=true;emit();
            }else{
                ++generation;ready=busy=catalogLoading=false;
                message=knownOwner?JobDraftRepository.error(t.getException()):
                        JobDraftRepository.network(t.getException())?"Kết nối mạng lần đầu để mở soạn tin.":JobDraftRepository.error(t.getException());
                emit();
            }
        });
    }
    public void refreshCatalog(){
        if(!ready || catalogLoading || sessionEnded)return;catalogLoading=true;long e=generation,a=++catalogRequest;
        catalogMessage="Đang cập nhật danh mục…";emit();
        repository.cached(uid).addOnCompleteListener(cached->{
            if(!valid(e)||a!=catalogRequest)return;
            if(cached.isSuccessful()){catalog=cached.getResult();fromCache=catalog.cachedAt>0;emit();}
            repository.refreshCatalog(uid).addOnCompleteListener(t->{
                if(!valid(e)||a!=catalogRequest)return;catalogLoading=false;
                if(t.isSuccessful()){
                    catalog=t.getResult();fromCache=false;
                    catalogMessage=catalog.categories.isEmpty()||catalog.areas.isEmpty()?"Chưa có danh mục hoặc khu vực để chọn.":"";
                    if(page==Page.PREVIEW && !busy){preview();return;}
                }else{
                    fromCache=catalog.cachedAt>0;
                    catalogMessage=JobDraftRepository.error(t.getException());
                }
                emit();
            });
        });
    }
    public void loadList(){loadList("");}
    private void loadList(String notice){
        if(!ready || busy)return;busy=true;long e=generation,a=++formRequest;
        page=Page.LIST;draft=null;dirty=readRequired=false;errors.clear();message="Đang tải nháp…";++pageEvent;emit();
        repository.list(uid).addOnCompleteListener(t->{
            if(!valid(e)||a!=formRequest)return;busy=false;
            if(t.isSuccessful()){
                drafts=t.getResult();message=notice.isEmpty()?(drafts.isEmpty()?"Chưa có bản nháp.":""):notice;
                if(!pendingEditId.isEmpty()){String id=pendingEditId;pendingEditId="";openRejected(id);return;}
                if(startNew){startNew=false;newDraft();return;}
            }else{drafts=new ArrayList<>();message=JobDraftRepository.error(t.getException());}
            emit();
        });
    }
    public void newDraft(){
        if(!ready||busy)return;draft=new LocalJobDraft();draft.ownerUid=uid;page=Page.EDIT;
        dirty=readRequired=false;message="";errors.clear();++pageEvent;emit();
    }
    public void open(String id){
        if(!ready||busy)return;busy=true;long e=generation,a=++formRequest;message="Đang mở nháp…";emit();
        repository.read(uid,id).addOnCompleteListener(t->{
            if(!valid(e)||a!=formRequest)return;busy=false;
            if(t.isSuccessful()){draft=t.getResult();page=Page.EDIT;dirty=readRequired=false;errors.clear();message="";++pageEvent;}
            else message=JobDraftRepository.error(t.getException());emit();
        });
    }
    public void edited(String field){
        if(!ready||busy||draft==null)return;dirty=true;errors.remove(field);message="";emit();
    }
    public void preview(){
        if(!ready||busy||draft==null)return;
        Set<String> categories=new HashSet<>(),areas=new HashSet<>();
        for(JobDraftStore.Item i:catalog.categories)categories.add(i.id);for(JobDraftStore.Item i:catalog.areas)areas.add(i.id);
        errors=draft.validate(categories,areas,System.currentTimeMillis());
        if(!errors.isEmpty()){if(page!=Page.EDIT)++pageEvent;page=Page.EDIT;message="Kiểm tra các thông tin được đánh dấu.";++validationEvent;emit();return;}
        if(page!=Page.PREVIEW)++pageEvent;page=Page.PREVIEW;message="";emit();
    }
    public void edit(){if(!busy && draft!=null){page=Page.EDIT;++pageEvent;emit();}}
    public void saveThen(String action){
        if(!ready||busy||draft==null)return;
        if(readRequired){message="Tải lại nháp trước khi lưu tiếp.";emit();return;}
        if(!draft.hasContent()){message="Nhập thông tin trước khi lưu nháp.";emit();return;}
        if(!dirty && draft.revision>0){message="Nháp đã được lưu.";emit();if(!action.isEmpty())navigate(action);return;}
        if(!draft.bounded()){message="Thông tin quá dài hoặc chưa đúng. Hãy kiểm tra lại.";emit();return;}
        LocalJobDraft value=draft.copy();long e=generation,a=++formRequest;busy=true;message="Đang lưu nháp…";emit();
        repository.save(uid,value).addOnCompleteListener(t->{
            if(!valid(e)||a!=formRequest)return;busy=false;
            if(t.isSuccessful()){draft=t.getResult();dirty=false;errors.clear();message="Đã lưu nháp trên máy.";emit();if(!action.isEmpty())navigate(action);}
            else{message=JobDraftRepository.error(t.getException());
                Throwable c=t.getException();for(int i=0;c!=null&&i<8;i++,c=c.getCause())
                    if(c instanceof JobDraftStore.Failure && "CONFLICT".equals(((JobDraftStore.Failure)c).code))readRequired=true;
                emit();}
        });
    }
    public void delete(LocalJobDraft target){
        if(!ready||busy||!uid.equals(target.ownerUid))return;busy=true;long e=generation,a=++formRequest;
        message="Đang xóa nháp…";emit();repository.delete(uid,target.id,target.revision).addOnCompleteListener(t->{
            if(!valid(e)||a!=formRequest)return;busy=false;
            if(t.isSuccessful())loadList("Đã xóa nháp.");else{message=JobDraftRepository.error(t.getException());emit();}
        });
    }
    public void openRejected(String id){
        if(!ready||busy)return;busy=true;long e=generation,a=++formRequest;message="Đang mở tin…";emit();
        posts.get(uid,id).addOnCompleteListener(t->{
            if(!valid(e)||a!=formRequest)return;
            if(!t.isSuccessful()){busy=false;message=JobPostRepository.error(t.getException());emit();return;}
            if(!uid.equals(t.getResult().content.ownerUid)||!Arrays.asList("REJECTED","OPEN").contains(t.getResult().status)){
                busy=false;message="Tin đã thay đổi. Hãy tải lại trong Tin đã gửi.";emit();return;}
            LocalJobDraft value=t.getResult().content.copy();
            repository.read(uid,id).addOnCompleteListener(local->{
                if(!valid(e)||a!=formRequest)return;busy=false;
                if(local.isSuccessful()){value.revision=local.getResult().revision;value.createdAt=local.getResult().createdAt;value.updatedAt=local.getResult().updatedAt;}
                else{Throwable c=local.getException();boolean missing=false;for(int i=0;c!=null&&i<8;i++,c=c.getCause())if(c instanceof JobDraftStore.Failure&&"MISSING".equals(((JobDraftStore.Failure)c).code))missing=true;
                    if(!missing){message=JobDraftRepository.error(local.getException());emit();return;}}
                draft=value;page=Page.EDIT;dirty=true;readRequired=false;errors.clear();message="Sửa nội dung rồi xem trước để gửi duyệt.";++pageEvent;emit();
            });
        });
    }
    public void submit(){
        if(!ready||busy||draft==null||readRequired)return;preview();if(!errors.isEmpty())return;
        final LocalJobDraft value=draft.copy();long e=generation,a=++formRequest;busy=true;fixAccount=checkSent=false;message="Đang lưu nháp trước khi gửi…";emit();
        (dirty||value.revision==0?repository.save(uid,value):Tasks.forResult(value)).addOnCompleteListener(saved->{
            if(!valid(e)||a!=formRequest)return;
            if(!saved.isSuccessful()){busy=false;readRequired=true;message=JobDraftRepository.error(saved.getException());emit();return;}
            draft=saved.getResult();dirty=false;message="Đang gửi và đối chiếu tin…";emit();
            posts.submit(uid,draft).addOnCompleteListener(t->{
                if(!valid(e)||a!=formRequest)return;busy=false;
                if(t.isSuccessful()){submittedPostId=t.getResult().content.id;
                    submittedNotice=t.getResult().sameContent(draft)?"":"Tin này đã gửi trước đó. Phần sửa nháp chưa thay đổi tin trên máy chủ. Nếu bị từ chối, chọn Sửa và gửi lại từ tin này.";
                    message="Đã đối chiếu tin trên máy chủ. Nháp vẫn được giữ.";}
                else{checkSent=true;message=JobPostRepository.error(t.getException());Throwable c=t.getException();for(int i=0;c!=null&&i<10;i++,c=c.getCause())
                    if(c instanceof JobPostRepository.Failure&&Arrays.asList("PHONE","PROFILE").contains(((JobPostRepository.Failure)c).code))fixAccount=true;}
                emit();
            });
        });
    }
    /** The Activity asks before calling this if an edit is dirty. No Activity retained by pending work. */
    public void navigate(String action){
        if(busy)return;
        switch(action){
            case "SENT":dirty=false;showPosts=true;emit();break;
            case "LIST":dirty=false;loadList();break;
            case "NEW":dirty=false;newDraft();break;
            case "HOME":case "ACCOUNT":case "CLOSE":draft=null;dirty=false;exitRoute=action;emit();break;
            default:break;
        }
    }
    @Override protected void onCleared(){
        ++generation;FirebaseAuth.getInstance().removeAuthStateListener(authListener);
        draft=null;drafts.clear();errors.clear();super.onCleared();
    }
}
