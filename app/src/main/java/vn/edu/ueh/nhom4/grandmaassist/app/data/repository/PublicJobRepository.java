package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import android.content.Context;
import com.google.android.gms.tasks.*;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.*;
import java.util.*;
import java.util.concurrent.*;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.PublicJobStore;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.PublicJob;

/** Server reads and a strictly public SQLite projection. No sign-in, write or private history query. */
public final class PublicJobRepository {
    private static final ExecutorService IO=Executors.newSingleThreadExecutor();
    private final FirebaseFirestore db=FirebaseFirestore.getInstance();
    private final PublicJobStore store;
    public static final class Failure extends RuntimeException {
        public final String code;public Failure(String code){super(code);this.code=code;}
    }
    public static final class Page {
        public final List<PublicJob> jobs;
        public final DocumentSnapshot cursor;
        public final boolean more,cacheSaved;
        Page(List<PublicJob> jobs,DocumentSnapshot cursor,boolean more,boolean saved){this.jobs=jobs;this.cursor=cursor;this.more=more;cacheSaved=saved;}
    }
    public static final class Detail {
        public final PublicJob job;public final boolean cacheSaved;
        Detail(PublicJob job,boolean saved){this.job=job;cacheSaved=saved;}
    }
    public PublicJobRepository(Context c){store=PublicJobStore.get(c);}
    public static <T> Task<T> bounded(Task<T> t){return Tasks.withTimeout(t,20,TimeUnit.SECONDS);}
    public Task<Page> list(String category,String area,long referenceTime,DocumentSnapshot after){
        long requestedAt=System.currentTimeMillis();
        Query q=db.collection("jobs").whereEqualTo("status","OPEN")
                .whereGreaterThan("expiresAt",new Timestamp(new Date(referenceTime)));
        if(!category.isEmpty())q=q.whereEqualTo("categoryId",category);
        if(!area.isEmpty())q=q.whereEqualTo("areaCode",area);
        q=q.orderBy("expiresAt",Query.Direction.ASCENDING).orderBy(FieldPath.documentId(),Query.Direction.ASCENDING)
                .limit(PublicJobStore.PAGE_SIZE);
        if(after!=null)q=q.startAfter(after);
        return bounded(q.get(Source.SERVER)).continueWithTask(t->{
            if(!t.isSuccessful())throw t.getException();
            QuerySnapshot snapshot=t.getResult();
            return Tasks.call(IO,()->{
                List<PublicJob> jobs=new ArrayList<>();
                for(DocumentSnapshot d:snapshot)jobs.add(PublicJob.fromServer(d,requestedAt));
                boolean saved=true;try{store.save(jobs);}catch(Exception e){saved=false;}
                DocumentSnapshot cursor=snapshot.isEmpty()?null:snapshot.getDocuments().get(snapshot.size()-1);
                return new Page(jobs,cursor,snapshot.size()==PublicJobStore.PAGE_SIZE,saved);
            });
        });
    }
    public Task<Detail> detail(String id){
        long requestedAt=System.currentTimeMillis();
        return bounded(db.collection("jobs").document(id).get(Source.SERVER)).continueWithTask(t->{
            if(!t.isSuccessful()){
                if(permissionDenied(t.getException()))return unavailable(id);
                throw t.getException();
            }
            DocumentSnapshot doc=t.getResult();
            if(!doc.exists() || !"OPEN".equals(doc.getString("status")))return unavailable(id);
            return Tasks.call(IO,()->{
                PublicJob job=PublicJob.fromServer(doc,requestedAt);boolean saved=true;
                try{store.save(Collections.singletonList(job));}catch(Exception e){saved=false;}
                return new Detail(job,saved);
            });
        });
    }
    private Task<Detail> unavailable(String id){
        return Tasks.call(IO,()->{
            // Do not fall back to a copy known to be missing, private or denied by the server.
            try{store.remove(id);}catch(Exception ignored){}
            throw new Failure("UNAVAILABLE");
        });
    }
    public Task<PublicJob> cachedDetail(String id){return Tasks.call(IO,()->store.read(id));}
    public Task<PublicJobStore.Page> cachedList(String category,String area,PublicJob after){
        return Tasks.call(IO,()->store.list(category,area,System.currentTimeMillis(),after));
    }
    public Task<PublicJobStore.Catalog> cachedCatalog(){return Tasks.call(IO,store::catalog);}
    private Task<List<PublicJobStore.Item>> catalogPage(String collection,DocumentSnapshot after,List<PublicJobStore.Item> accumulated){
        Query q=db.collection(collection).whereEqualTo("active",true).orderBy(FieldPath.documentId()).limit(100);
        if(after!=null)q=q.startAfter(after);
        return bounded(q.get(Source.SERVER)).continueWithTask(t->{
            if(!t.isSuccessful())throw t.getException();QuerySnapshot snapshot=t.getResult();
            List<PublicJobStore.Item> out=new ArrayList<>(accumulated);
            for(DocumentSnapshot d:snapshot){
                String name=d.getString("name");
                if(name==null || name.trim().isEmpty() || name.length()>120 || d.getId().length()>80)throw new Failure("CATALOG");
                out.add(new PublicJobStore.Item(d.getId(),name));
            }
            if(snapshot.size()==100){
                if(out.size()>=1000)throw new Failure("CATALOG_LIMIT");
                return catalogPage(collection,snapshot.getDocuments().get(snapshot.size()-1),out);
            }
            out.sort(Comparator.comparing(i->i.name));return Tasks.forResult(out);
        });
    }
    public Task<PublicJobStore.Catalog> catalog(){
        Task<List<PublicJobStore.Item>> categories=catalogPage("jobCategories",null,Collections.emptyList());
        Task<List<PublicJobStore.Item>> areas=catalogPage("areas",null,Collections.emptyList());
        return Tasks.whenAll(categories,areas).continueWithTask(t->{
            if(!t.isSuccessful())throw t.getException();
            PublicJobStore.Catalog result=new PublicJobStore.Catalog();result.cachedAt=System.currentTimeMillis();
            result.categories.addAll(categories.getResult());result.areas.addAll(areas.getResult());
            return Tasks.call(IO,()->{try{store.saveCatalog(result);}catch(Exception ignored){}return result;});
        });
    }
    public static boolean network(Exception e){
        Throwable c=e;for(int i=0;c!=null && i<10;i++,c=c.getCause())if(c instanceof TimeoutException)return true;
        return JobDraftRepository.network(e);
    }
    public static boolean permissionDenied(Exception e){
        Throwable c=e;for(int i=0;c!=null && i<10;i++,c=c.getCause())
            if(c instanceof FirebaseFirestoreException && ((FirebaseFirestoreException)c).getCode()==FirebaseFirestoreException.Code.PERMISSION_DENIED)return true;
        return false;
    }
    public static String code(Exception e){
        Throwable c=e;for(int i=0;c!=null && i<10;i++,c=c.getCause()){
            if(c instanceof Failure)return ((Failure)c).code;
            if(c instanceof org.json.JSONException)return "DATA";
            if(c instanceof android.database.sqlite.SQLiteException)return "CACHE";
            if(c instanceof FirebaseFirestoreException){
                FirebaseFirestoreException.Code code=((FirebaseFirestoreException)c).getCode();
                if(code==FirebaseFirestoreException.Code.FAILED_PRECONDITION)return "INDEX";
                if(code==FirebaseFirestoreException.Code.PERMISSION_DENIED || code==FirebaseFirestoreException.Code.UNAUTHENTICATED)return "ACCESS";
            }
        }return network(e)?"NETWORK":"OTHER";
    }
}
