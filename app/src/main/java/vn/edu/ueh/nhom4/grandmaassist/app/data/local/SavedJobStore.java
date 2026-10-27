package vn.edu.ueh.nhom4.grandmaassist.app.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.PublicJob;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.SavedJob;

public final class SavedJobStore extends SQLiteOpenHelper {
    public static final String DATABASE = "saved_jobs.db";
    private static SavedJobStore instance;

    public static synchronized SavedJobStore get(Context c) {
        if (instance == null) instance = new SavedJobStore(c.getApplicationContext());
        return instance;
    }

    private SavedJobStore(Context c) {
        super(c, DATABASE, null, 1);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE cached_saved_jobs (owner_uid TEXT NOT NULL, job_id TEXT NOT NULL, saved_at INTEGER NOT NULL, last_synced_at INTEGER NOT NULL, payload TEXT NOT NULL, job_status TEXT NOT NULL, PRIMARY KEY (owner_uid, job_id));");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("SAVED_JOB_STORE_MIGRATION");
    }

    public void save(String ownerUid, SavedJob item) {
        if (ownerUid == null || item == null) return;
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            ContentValues v = new ContentValues();
            v.put("owner_uid", ownerUid);
            v.put("job_id", item.jobId);
            v.put("saved_at", item.savedAt);
            v.put("last_synced_at", System.currentTimeMillis());
            String payload = "";
            if (item.job != null) {
                try { payload = item.job.toJson(); } catch (Exception ignored) {}
            }
            v.put("payload", payload);
            v.put("job_status", item.jobStatus != null ? item.jobStatus : "OPEN");
            db.insertWithOnConflict("cached_saved_jobs", null, v, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public void remove(String ownerUid, String jobId) {
        if (ownerUid == null || jobId == null) return;
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            db.delete("cached_saved_jobs", "owner_uid=? AND job_id=?", new String[]{ownerUid, jobId});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public boolean isSaved(String ownerUid, String jobId) {
        if (ownerUid == null || jobId == null) return false;
        try (Cursor c = getReadableDatabase().query("cached_saved_jobs", new String[]{"job_id"},
                "owner_uid=? AND job_id=?", new String[]{ownerUid, jobId}, null, null, null)) {
            return c.moveToFirst();
        } catch (Exception ignored) { return false; }
    }

    public List<SavedJob> list(String ownerUid) {
        if (ownerUid == null) return Collections.emptyList();
        List<SavedJob> out = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("cached_saved_jobs",
                new String[]{"job_id", "saved_at", "payload", "job_status"},
                "owner_uid=?", new String[]{ownerUid}, null, null, "saved_at DESC")) {
            while (c.moveToNext()) {
                String jobId = c.getString(0);
                long savedAt = c.getLong(1);
                String payload = c.getString(2);
                String status = c.getString(3);
                PublicJob job = null;
                if (payload != null && !payload.trim().isEmpty()) {
                    try { job = PublicJob.fromCache(payload, savedAt); } catch (Exception ignored) {}
                }
                out.add(new SavedJob(ownerUid, jobId, savedAt, job, status));
            }
        } catch (Exception ignored) {}
        return out;
    }

    public void clear(String ownerUid) {
        if (ownerUid == null) return;
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            db.delete("cached_saved_jobs", "owner_uid=?", new String[]{ownerUid});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
}
