package vn.edu.ueh.nhom4.grandmaassist.app.data.local;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.PublicJob;

/** Public cache only, shared with guests. No UID, credentials, preferences or private documents. */
public final class PublicJobStore extends SQLiteOpenHelper {
    public static final String DATABASE = "public_jobs.db";
    public static final int PAGE_SIZE = 20, MAX_JOBS = 500;
    public static final class Item {
        public final String id,name;
        public Item(String id,String name){this.id=id;this.name=name;}
        @Override public String toString(){return name;}
    }
    public static final class Catalog {
        public final List<Item> categories=new ArrayList<>(),areas=new ArrayList<>();
        public long cachedAt;
    }
    public static final class Page {
        public final List<PublicJob> jobs;
        public final boolean more;
        public Page(List<PublicJob> jobs,boolean more){this.jobs=jobs;this.more=more;}
    }
    private static PublicJobStore instance;
    // All DAO access is serialized on the repository executor. Also block a stale copy if disk invalidation fails.
    private final Map<String,Long> invalidated=new HashMap<>();
    public static synchronized PublicJobStore get(Context c){
        if(instance==null)instance=new PublicJobStore(c.getApplicationContext());return instance;
    }
    private PublicJobStore(Context c){super(c,DATABASE,null,1);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE cached_jobs(job_id TEXT PRIMARY KEY NOT NULL,category_id TEXT NOT NULL,area_code TEXT NOT NULL,expires_at INTEGER NOT NULL,remote_version INTEGER NOT NULL,cached_at INTEGER NOT NULL,payload TEXT NOT NULL)");
        db.execSQL("CREATE INDEX public_job_expiry ON cached_jobs(expires_at,job_id)");
        db.execSQL("CREATE TABLE unavailable_jobs(job_id TEXT PRIMARY KEY NOT NULL,checked_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE cached_categories(id TEXT PRIMARY KEY NOT NULL,name TEXT NOT NULL,cached_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE cached_areas(id TEXT PRIMARY KEY NOT NULL,name TEXT NOT NULL,cached_at INTEGER NOT NULL)");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int from,int to){throw new IllegalStateException("CACHE_MIGRATION");}
    public void save(List<PublicJob> jobs) throws Exception {
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try {
            for(PublicJob j:jobs){
                if(invalidated.containsKey(j.id) && invalidated.get(j.id)>=j.cachedAt)continue;
                try(Cursor c=db.query("unavailable_jobs",new String[]{"checked_at"},"job_id=?",new String[]{j.id},null,null,null)){
                    if(c.moveToFirst() && c.getLong(0)>=j.cachedAt)continue;
                }
                // A late older server response must not overwrite a newer version already cached.
                try(Cursor c=db.query("cached_jobs",new String[]{"remote_version"},"job_id=?",new String[]{j.id},null,null,null)){
                    if(c.moveToFirst() && c.getLong(0)>j.version)continue;
                }
                ContentValues v=new ContentValues();v.put("job_id",j.id);v.put("category_id",j.categoryId);v.put("area_code",j.areaCode);
                v.put("expires_at",j.expiresAt);v.put("remote_version",j.version);v.put("cached_at",j.cachedAt);v.put("payload",j.toJson());
                if(db.insertWithOnConflict("cached_jobs",null,v,SQLiteDatabase.CONFLICT_REPLACE)==-1)throw new SQLiteException("CACHE_WRITE");
                db.delete("unavailable_jobs","job_id=?",new String[]{j.id});
            }
            db.execSQL("DELETE FROM cached_jobs WHERE job_id IN (SELECT job_id FROM cached_jobs ORDER BY cached_at DESC,job_id ASC LIMIT -1 OFFSET "+MAX_JOBS+")");
            db.setTransactionSuccessful();
        } finally {db.endTransaction();}
        for(PublicJob j:jobs){Long time=invalidated.get(j.id);if(time!=null && j.cachedAt>time)invalidated.remove(j.id);}
    }
    public void remove(String id){
        invalidated.put(id,System.currentTimeMillis());
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            db.delete("cached_jobs","job_id=?",new String[]{id});
            ContentValues v=new ContentValues();v.put("job_id",id);v.put("checked_at",System.currentTimeMillis());
            if(db.insertWithOnConflict("unavailable_jobs",null,v,SQLiteDatabase.CONFLICT_REPLACE)==-1)throw new SQLiteException("CACHE_INVALIDATION");
            db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
    public PublicJob read(String id) throws Exception {
        if(invalidated.containsKey(id))return null;
        try(Cursor c=getReadableDatabase().query("cached_jobs",new String[]{"payload","cached_at"},"job_id=?",new String[]{id},null,null,null)){
            return c.moveToFirst()?PublicJob.fromCache(c.getString(0),c.getLong(1)):null;
        }
    }
    public Page list(String category,String area,long now,PublicJob after) throws Exception {
        StringBuilder where=new StringBuilder("expires_at>?");List<String> args=new ArrayList<>();args.add(String.valueOf(now));
        if(!category.isEmpty()){where.append(" AND category_id=?");args.add(category);}
        if(!area.isEmpty()){where.append(" AND area_code=?");args.add(area);}
        if(after!=null){where.append(" AND (expires_at>? OR (expires_at=? AND job_id>?))");
            args.add(String.valueOf(after.expiresAt));args.add(String.valueOf(after.expiresAt));args.add(after.id);}
        List<PublicJob> jobs=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("cached_jobs",new String[]{"payload","cached_at"},where.toString(),args.toArray(new String[0]),null,null,"expires_at ASC,job_id ASC",String.valueOf(PAGE_SIZE+1))){
            while(c.moveToNext()){
                PublicJob j=PublicJob.fromCache(c.getString(0),c.getLong(1));
                if(invalidated.containsKey(j.id))throw new SQLiteException("CACHE_INVALIDATION");
                jobs.add(j);
            }
        }
        boolean more=jobs.size()>PAGE_SIZE;if(more)jobs.remove(jobs.size()-1);return new Page(jobs,more);
    }
    public Catalog catalog(){
        Catalog out=new Catalog();
        for(String table:Arrays.asList("cached_categories","cached_areas")){
            try(Cursor c=getReadableDatabase().query(table,new String[]{"id","name","cached_at"},null,null,null,null,"name COLLATE NOCASE,id ASC")){
                while(c.moveToNext()){
                    (table.equals("cached_categories")?out.categories:out.areas).add(new Item(c.getString(0),c.getString(1)));
                    out.cachedAt=Math.max(out.cachedAt,c.getLong(2));
                }
            }
        }return out;
    }
    public void saveCatalog(Catalog catalog){
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            for(String table:Arrays.asList("cached_categories","cached_areas")){
                db.delete(table,null,null);
                for(Item i:table.equals("cached_categories")?catalog.categories:catalog.areas){
                    ContentValues v=new ContentValues();v.put("id",i.id);v.put("name",i.name);v.put("cached_at",catalog.cachedAt);db.insertOrThrow(table,null,v);
                }
            }db.setTransactionSuccessful();
        }finally{db.endTransaction();}
    }
}
