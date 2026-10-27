package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.*;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.JobPost;

/** Retains network operation and review form across rotation; never retains an Activity. */
public final class JobPostViewModel extends AndroidViewModel {
    public final MutableLiveData<Long> changes=new MutableLiveData<>(0L);
    public final String uid;
    public boolean started,ready,admin,busy,ended,more,reasonVisible;
    public String message="",reason="",pendingNotice="";
    public String reasonAction = "REJECT";
    public JobPost selected;
    public List<JobPost> items=new ArrayList<>();
    public long pageEvent;
    private long generation;
    private DocumentSnapshot cursor;
    private final JobPostRepository repository=new JobPostRepository();
    private final FirebaseAuth.AuthStateListener listener;
    public JobPostViewModel(@NonNull Application app){super(app);FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();uid=u==null?"":u.getUid();
        listener=a->{if(!repository.same(uid)){++generation;ended=true;busy=ready=false;items.clear();selected=null;reason=message="";emit();}};
        FirebaseAuth.getInstance().addAuthStateListener(listener);
    }
    public void emit(){changes.setValue(changes.getValue()+1);}
    private boolean valid(long g){return g==generation&&!ended&&repository.same(uid);}
    public void initialize(String id,String notice){if(started)return;started=true;pendingNotice=notice==null?"":notice;authorize(id);}
    public void authorize(String id){if(busy)return;busy=true;message="Đang kiểm tra tài khoản…";long g=++generation;emit();
        JobPostRepository.bounded(new ProfileRepository().load(uid)).addOnCompleteListener(t->{if(!valid(g))return;busy=false;
            if(t.isSuccessful()&&t.getResult()!=null&&"ACTIVE".equals(t.getResult().status)
                    &&Arrays.asList("EMPLOYER","ADMIN").contains(t.getResult().role)){
                admin="ADMIN".equals(t.getResult().role);ready=true;if(id.isEmpty())load(false);else open(id);
            }else{ready=false;message=t.isSuccessful()?"Tài khoản chưa có quyền xem mục này.":JobPostRepository.error(t.getException());emit();}
        });
    }
    public void load(boolean next){if(!ready||busy)return;busy=true;message="Đang tải tin…";long g=++generation;emit();
        repository.list(uid,admin,next?cursor:null).addOnCompleteListener(t->{if(!valid(g))return;busy=false;
            if(t.isSuccessful()){
                LinkedHashMap<String,JobPost> merged=new LinkedHashMap<>();if(next)for(JobPost p:items)merged.put(p.content.id,p);
                for(JobPost p:t.getResult().items)merged.put(p.content.id,p);items=new ArrayList<>(merged.values());cursor=t.getResult().cursor;more=t.getResult().more;
                selected=null;reasonVisible=false;reason="";reasonAction="REJECT";message=items.isEmpty()?"Chưa có tin trong danh sách.":"";++pageEvent;
            }else message=JobPostRepository.error(t.getException());emit();
        });
    }
    public void open(String id){if(!ready||busy)return;busy=true;message="Đang tải tin…";long g=++generation;emit();
        repository.get(uid,id).addOnCompleteListener(t->{if(!valid(g))return;busy=false;
            if(t.isSuccessful()){selected=t.getResult();reasonVisible=false;reason="";reasonAction="REJECT";message=pendingNotice;pendingNotice="";++pageEvent;}
            else message=JobPostRepository.error(t.getException());emit();});
    }
    public void backToList(){if(busy)return;selected=null;reasonVisible=false;reason="";reasonAction="REJECT";message=items.isEmpty()?"Bấm Tải lại để xem danh sách tin.":"";++pageEvent;emit();}
    public void decide(boolean approve){if(!ready||busy||!admin||selected==null)return;
        if(!approve&&(reason.trim().isEmpty()||reason.trim().length()>1000)){message="Nhập lý do từ chối, tối đa 1.000 ký tự.";reasonVisible=true;emit();return;}
        JobPost expected=selected;busy=true;message="Đang lưu quyết định…";long g=++generation;emit();
        repository.decide(uid,expected,approve,reason).addOnCompleteListener(t->{if(!valid(g))return;busy=false;
            if(t.isSuccessful()){selected=t.getResult();reasonVisible=false;reason="";message=approve?"Đã chấp thuận tin.":"Đã từ chối tin.";}
            else message=JobPostRepository.error(t.getException());emit();});
    }
    public void closePost() {
        if (!ready || busy || admin || selected == null || !"OPEN".equals(selected.status)) return;
        JobPost expected = selected; busy = true; message = "Đang đóng tin…"; long g = ++generation; emit();
        repository.close(uid, expected).addOnCompleteListener(t -> {
            if (!valid(g)) return; busy = false;
            if (t.isSuccessful()) { selected = t.getResult(); message = "Đã đóng tin tuyển dụng thành công."; }
            else message = JobPostRepository.error(t.getException()); emit();
        });
    }
    public void hidePost() {
        if (!ready || busy || !admin || selected == null || !Arrays.asList("OPEN", "CLOSED").contains(selected.status)) return;
        if (reason.trim().isEmpty() || reason.trim().length() > 1000) { message = "Nhập lý do ẩn tin, tối đa 1.000 ký tự."; reasonVisible = true; emit(); return; }
        JobPost expected = selected; busy = true; message = "Đang ẩn tin…"; long g = ++generation; emit();
        repository.hide(uid, expected, reason).addOnCompleteListener(t -> {
            if (!valid(g)) return; busy = false;
            if (t.isSuccessful()) { selected = t.getResult(); reasonVisible = false; reason = ""; message = "Đã ẩn tin tuyển dụng thành công."; }
            else message = JobPostRepository.error(t.getException()); emit();
        });
    }
    @Override protected void onCleared(){++generation;FirebaseAuth.getInstance().removeAuthStateListener(listener);super.onCleared();}
}
