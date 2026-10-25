package vn.edu.ueh.nhom4.grandmaassist.app.data.local;

import android.content.*;
import android.database.Cursor;
import android.database.sqlite.*;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.LocalJobDraft;

/** Synchronous DAO; the repository serializes every call on its DB executor. */
public final class JobDraftStore extends SQLiteOpenHelper {
    public static final String DATABASE="job_drafts.db";
    public static final class Failure extends RuntimeException { public final String code;public Failure(String code){super(code);this.code=code;} }
    public static final class Item {
        public final String id,name,iconKey;
        public Item(String id,String name,String icon){this.id=id;this.name=name;this.iconKey=icon;}
        @Override public String toString(){return name;}
    }
    public static final class Catalog {
        public final List<Item> categories=new ArrayList<>(),areas=new ArrayList<>();
        public long cachedAt;
    }
    private static JobDraftStore instance;
    public static synchronized JobDraftStore get(Context context){if(instance==null)instance=new JobDraftStore(context.getApplicationContext());return instance;}
    private JobDraftStore(Context context){super(context,DATABASE,null,1);}
    @Override public void onCreate(SQLiteDatabase db){
        db.execSQL("CREATE TABLE job_drafts(owner_uid TEXT NOT NULL,id TEXT NOT NULL,revision INTEGER NOT NULL,created_at INTEGER NOT NULL,updated_at INTEGER NOT NULL,payload TEXT NOT NULL,PRIMARY KEY(owner_uid,id))");
        db.execSQL("CREATE INDEX draft_owner_updated ON job_drafts(owner_uid,updated_at)");
        db.execSQL("CREATE TABLE draft_owners(owner_uid TEXT PRIMARY KEY NOT NULL,confirmed_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE draft_catalog(kind TEXT NOT NULL,id TEXT NOT NULL,name TEXT NOT NULL,icon_key TEXT NOT NULL,cached_at INTEGER NOT NULL,PRIMARY KEY(kind,id))");
    }
    @Override public void onUpgrade(SQLiteDatabase db,int oldVersion,int newVersion){
        // Add explicit, non-destructive migrations when v2 is introduced; never DROP user drafts.
        throw new Failure("MIGRATION");
    }
    public void rememberOwner(String uid){
        ContentValues v=new ContentValues();v.put("owner_uid",uid);v.put("confirmed_at",System.currentTimeMillis());
        if(getWritableDatabase().insertWithOnConflict("draft_owners",null,v,SQLiteDatabase.CONFLICT_REPLACE)==-1)throw new Failure("DATA");
    }
    public boolean knownOwner(String uid){
        try(Cursor c=getReadableDatabase().query("draft_owners",new String[]{"owner_uid"},"owner_uid=?",new String[]{uid},null,null,null)){return c.moveToFirst();}
    }
    private LocalJobDraft row(Cursor c){
        try{return LocalJobDraft.fromJson(c.getString(c.getColumnIndexOrThrow("payload")),c.getString(c.getColumnIndexOrThrow("owner_uid")),
                c.getString(c.getColumnIndexOrThrow("id")),c.getLong(c.getColumnIndexOrThrow("revision")),
                c.getLong(c.getColumnIndexOrThrow("created_at")),c.getLong(c.getColumnIndexOrThrow("updated_at")));}
        catch(Exception e){throw new Failure("DATA");}
    }
    public List<LocalJobDraft> list(String uid){
        List<LocalJobDraft> result=new ArrayList<>();
        try(Cursor c=getReadableDatabase().query("job_drafts",null,"owner_uid=?",new String[]{uid},null,null,"updated_at DESC,id ASC")){
            while(c.moveToNext())result.add(row(c));
        }return result;
    }
    public LocalJobDraft read(String uid,String id){
        try(Cursor c=getReadableDatabase().query("job_drafts",null,"owner_uid=? AND id=?",new String[]{uid,id},null,null,null)){
            if(!c.moveToFirst())throw new Failure("MISSING");return row(c);
        }
    }
    public LocalJobDraft save(String uid,LocalJobDraft draft){
        if(!uid.equals(draft.ownerUid)||!draft.bounded())throw new Failure("DATA");
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            LocalJobDraft saved=draft.copy();long now=System.currentTimeMillis();
            saved.createdAt=draft.revision==0?now:draft.createdAt;saved.updatedAt=now;saved.revision=draft.revision+1;
            ContentValues v=new ContentValues();v.put("owner_uid",uid);v.put("id",saved.id);v.put("revision",saved.revision);
            v.put("created_at",saved.createdAt);v.put("updated_at",now);
            try{v.put("payload",saved.toJson());}catch(Exception e){throw new Failure("DATA");}
            if(draft.revision==0){
                if(db.insertWithOnConflict("job_drafts",null,v,SQLiteDatabase.CONFLICT_IGNORE)==-1)throw new Failure("CONFLICT");
            }else if(db.update("job_drafts",v,"owner_uid=? AND id=? AND revision=?",new String[]{uid,draft.id,String.valueOf(draft.revision)})!=1)
                throw new Failure("CONFLICT");
            db.setTransactionSuccessful();return saved;
        }finally{db.endTransaction();}
    }
    public void delete(String uid,String id,long revision){
        if(getWritableDatabase().delete("job_drafts","owner_uid=? AND id=? AND revision=?",new String[]{uid,id,String.valueOf(revision)})!=1)
            throw new Failure("CONFLICT");
    }
    public Catalog catalog(){
        Catalog result=new Catalog();
        try(Cursor c=getReadableDatabase().query("draft_catalog",null,null,null,null,null,"name COLLATE NOCASE,id ASC")){
            while(c.moveToNext()){
                Item item=new Item(c.getString(c.getColumnIndexOrThrow("id")),c.getString(c.getColumnIndexOrThrow("name")),c.getString(c.getColumnIndexOrThrow("icon_key")));
                ("CATEGORY".equals(c.getString(c.getColumnIndexOrThrow("kind")))?result.categories:result.areas).add(item);
                result.cachedAt=Math.max(result.cachedAt,c.getLong(c.getColumnIndexOrThrow("cached_at")));
            }
        }return result;
    }
    public Catalog replaceCatalog(Catalog catalog){
        SQLiteDatabase db=getWritableDatabase();db.beginTransaction();
        try{
            db.delete("draft_catalog",null,null);
            for(String kind:Arrays.asList("CATEGORY","AREA"))for(Item item:"CATEGORY".equals(kind)?catalog.categories:catalog.areas){
                ContentValues v=new ContentValues();v.put("kind",kind);v.put("id",item.id);v.put("name",item.name);v.put("icon_key",item.iconKey);v.put("cached_at",catalog.cachedAt);
                db.insertOrThrow("draft_catalog",null,v);
            }
            db.setTransactionSuccessful();return catalog;
        }finally{db.endTransaction();}
    }
}
