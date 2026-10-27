package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.google.firebase.auth.*;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.PublicJobStore;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.ApplicationRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.PublicJobRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.SavedJobRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.PublicJob;

/** Public browsing has no role/profile gate. An Auth change invalidates this screen's callbacks. */
public final class PublicJobsViewModel extends AndroidViewModel {
    public final MutableLiveData<Long> changes=new MutableLiveData<>(0L);
    public final String sessionUid;
    public String role="",category="",area="",draftCategory="",draftArea="",detailId="";
    public String message="",catalogMessage="";
    public boolean started,ended,busy,catalogBusy,cached,more,detailCached;
    public PublicJobStore.Catalog catalog=new PublicJobStore.Catalog();
    public List<PublicJob> items=new ArrayList<>();
    public PublicJob selected;
    public long scrollEvent;
    public boolean isSaved;
    public boolean saveBusy;
    public boolean hasApplied;
    public boolean applyCheckBusy;

    private long generation,catalogGeneration,referenceTime;
    private DocumentSnapshot cursor;
    private PublicJob localCursor;
    private final PublicJobRepository repository;
    private final SavedJobRepository savedRepo;
    private final ApplicationRepository appRepo;
    private final FirebaseAuth.AuthStateListener listener;

    public PublicJobsViewModel(@NonNull Application app){
        super(app);repository=new PublicJobRepository(app);savedRepo=new SavedJobRepository(app);appRepo=new ApplicationRepository();sessionUid=currentUid();
        listener=a->{if(!sessionUid.equals(currentUid())){
            ended=true;++generation;++catalogGeneration;busy=catalogBusy=false;
            items=new ArrayList<>();selected=null;message=catalogMessage="";emit();
        }};
        FirebaseAuth.getInstance().addAuthStateListener(listener);
    }
    private static String currentUid(){FirebaseUser u=FirebaseAuth.getInstance().getCurrentUser();return u==null?"":u.getUid();}
    public void emit(){changes.setValue(changes.getValue()+1);}
    private String s(int id){return getApplication().getString(id);}
    private boolean valid(long g){return !ended && generation==g && sessionUid.equals(currentUid());}
    private boolean validCatalog(long g){return !ended && catalogGeneration==g && sessionUid.equals(currentUid());}
    public void initialize(String role,String category,String area,String id){
        if(started)return;started=true;
        this.role=sessionUid.isEmpty()?"":Arrays.asList("SEEKER","EMPLOYER","ADMIN").contains(role)?role:"";
        this.category=category;this.area=area;draftCategory=category;draftArea=area;
        loadCatalog();if(id.isEmpty())load(false,false);else open(id,false);
    }
    public void loadCatalog(){
        if(catalogBusy || ended)return;catalogBusy=true;long g=++catalogGeneration;catalogMessage="";emit();
        repository.cachedCatalog().addOnCompleteListener(local->{
            if(!validCatalog(g))return;
            if(local.isSuccessful()){catalog=local.getResult();emit();}
            PublicJobRepository.bounded(repository.catalog()).addOnCompleteListener(remote->{
                if(!validCatalog(g))return;catalogBusy=false;
                if(remote.isSuccessful()){catalog=remote.getResult();catalogMessage="";}
                else catalogMessage=repository.network(remote.getException())?s(R.string.pb_catalog_cached):s(R.string.pb_catalog_error);
                emit();
            });
        });
    }
    public void apply(){
        if(busy || ended)return;category=draftCategory;area=draftArea;
        items=new ArrayList<>();cursor=null;localCursor=null;more=false;selected=null;detailId="";
        load(false,cached);
    }
    public void clearFilters(){if(busy)return;draftCategory=draftArea="";apply();}
    public void reload(){if(!detailId.isEmpty())open(detailId,false);else load(false,false);}
    public void saved(){if(!busy){detailId="";selected=null;items=new ArrayList<>();load(false,true);}}
    public void next(){if(more && !busy)load(true,cached);}
    private void load(boolean next,boolean useCache){
        if(busy || ended)return;
        if(!next){items=new ArrayList<>();more=false;cursor=null;localCursor=null;cached=useCache;}
        busy=true;selected=null;detailId="";message=s(R.string.pb_loading);long g=++generation;emit();
        if(!next)referenceTime=System.currentTimeMillis();
        if(useCache){loadCache(g,next);return;}
        repository.list(category,area,referenceTime,next?cursor:null).addOnCompleteListener(t->{
            if(!valid(g))return;
            if(t.isSuccessful()){
                PublicJobRepository.Page p=t.getResult();merge(p.jobs,next);cursor=p.cursor;cached=false;more=p.more;busy=false;
                message=items.isEmpty()?s(p.more?R.string.pb_empty_page:R.string.pb_empty):"";
                if(!p.cacheSaved)message=s(R.string.pb_cache_write_error);
                if(!next)++scrollEvent;emit();
            }else if(repository.network(t.getException())){
                loadCache(g,false);
            }else{busy=false;more=next && more;message=error(t.getException());emit();}
        });
    }
    private void loadCache(long g,boolean next){
        repository.cachedList(category,area,next?localCursor:null).addOnCompleteListener(t->{
            if(!valid(g))return;busy=false;
            if(t.isSuccessful()){
                PublicJobStore.Page p=t.getResult();merge(p.jobs,next);cached=true;more=p.more;cursor=null;
                localCursor=p.jobs.isEmpty()?null:p.jobs.get(p.jobs.size()-1);
                message=items.isEmpty()?s(R.string.pb_cache_empty):s(R.string.pb_cache_scope);
                if(!next)++scrollEvent;
            }else{cached=true;more=false;items=new ArrayList<>();message=s(R.string.pb_cache_read_error);}
            emit();
        });
    }
    private void merge(List<PublicJob> incoming,boolean next){
        Map<String,PublicJob> merged=new LinkedHashMap<>();long now=System.currentTimeMillis();
        if(next)for(PublicJob j:items)if(j.expiresAt>now)merged.put(j.id,j);
        for(PublicJob j:incoming)if(j.expiresAt>now)merged.put(j.id,j);
        items=new ArrayList<>(merged.values());
        items.sort(Comparator.comparingLong((PublicJob j)->j.expiresAt).thenComparing(j->j.id));
    }
    public void open(String id,boolean onlyCache){
        if(busy || ended || id==null || id.isEmpty())return;
        detailId=id;selected=null;detailCached=false;busy=true;message=s(R.string.pb_loading);long g=++generation;++scrollEvent;emit();
        if(onlyCache){openCache(g,id);return;}
        repository.detail(id).addOnCompleteListener(t->{
            if(!valid(g))return;
            if(t.isSuccessful()){
                busy=false;selected=t.getResult().job;detailCached=false;
                message=t.getResult().cacheSaved?"":s(R.string.pb_cache_write_error);
                if(selected!=null){checkSaved();checkApplied();}else emit();
            }else if(repository.network(t.getException()))openCache(g,id);
            else{
                busy=false;message=error(t.getException());
                if("UNAVAILABLE".equals(repository.code(t.getException()))) {
                    List<PublicJob> remaining=new ArrayList<>();for(PublicJob j:items)if(!j.id.equals(id))remaining.add(j);items=remaining;
                }
                emit();
            }
        });
    }
    private void openCache(long g,String id){
        repository.cachedDetail(id).addOnCompleteListener(t->{
            if(!valid(g))return;busy=false;detailCached=true;
            if(t.isSuccessful()){
                selected=t.getResult();
                message=selected==null?s(R.string.pb_detail_not_saved):"";
                if(selected!=null){checkSaved();checkApplied();}else emit();
            } else { message=s(R.string.pb_cache_read_error); emit(); }
        });
    }
    public void checkSaved() {
        if (sessionUid.isEmpty() || selected == null) { isSaved = false; emit(); return; }
        savedRepo.isJobSaved(sessionUid, selected.id).addOnCompleteListener(t -> {
            if (!ended && sessionUid.equals(currentUid())) {
                isSaved = t.isSuccessful() && Boolean.TRUE.equals(t.getResult());
                emit();
            }
        });
    }
    public void checkApplied() {
        if (sessionUid.isEmpty() || selected == null) { hasApplied = false; emit(); return; }
        applyCheckBusy = true;
        appRepo.getApplicationForJob(sessionUid, selected.id).addOnCompleteListener(t -> {
            if (!ended && sessionUid.equals(currentUid())) {
                applyCheckBusy = false;
                hasApplied = t.isSuccessful() && t.getResult() != null;
                emit();
            }
        });
    }
    public void toggleSave() {
        if (sessionUid.isEmpty() || selected == null || saveBusy) return;
        saveBusy = true; emit();
        if (isSaved) {
            savedRepo.unsaveJob(sessionUid, selected.id).addOnCompleteListener(t -> {
                if (!ended && sessionUid.equals(currentUid())) {
                    saveBusy = false;
                    if (t.isSuccessful()) isSaved = false;
                    emit();
                }
            });
        } else {
            savedRepo.saveJob(sessionUid, selected).addOnCompleteListener(t -> {
                if (!ended && sessionUid.equals(currentUid())) {
                    saveBusy = false;
                    if (t.isSuccessful()) isSaved = true;
                    emit();
                }
            });
        }
    }
    public void backToList(){
        ++generation;busy=false;selected=null;detailId="";isSaved=false;saveBusy=false;hasApplied=false;applyCheckBusy=false;
        message=items.isEmpty()?s(R.string.pb_choose_load):cached?s(R.string.pb_cache_scope):"";++scrollEvent;emit();
    }
    public void pruneExpired(){
        long now=System.currentTimeMillis();List<PublicJob> remaining=new ArrayList<>();
        for(PublicJob j:items)if(j.expiresAt>now)remaining.add(j);
        if(remaining.size()!=items.size()){items=remaining;emit();}
    }
    private String error(Exception e){
        switch(PublicJobRepository.code(e)){
            case "UNAVAILABLE":return s(R.string.pb_unavailable);
            case "INDEX":return s(R.string.pb_index_error);
            case "ACCESS":return s(R.string.pb_access_error);
            case "DATA":return s(R.string.pb_data_error);
            case "CACHE":return s(R.string.pb_cache_read_error);
            case "NETWORK":return s(R.string.pb_network_error);
            default:return s(R.string.pb_error);
        }
    }
    @Override protected void onCleared(){
        ++generation;++catalogGeneration;FirebaseAuth.getInstance().removeAuthStateListener(listener);super.onCleared();
    }
}
