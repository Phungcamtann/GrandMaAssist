package vn.edu.ueh.nhom4.grandmaassist.app.data.local;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.SearchPreference;

public final class SearchPreferenceStore extends SQLiteOpenHelper {
    public static final String DATABASE = "search_preferences.db";
    private static SearchPreferenceStore instance;

    public static synchronized SearchPreferenceStore get(Context c) {
        if (instance == null) instance = new SearchPreferenceStore(c.getApplicationContext());
        return instance;
    }

    private SearchPreferenceStore(Context c) {
        super(c, DATABASE, null, 1);
    }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE local_search_preferences (owner_key TEXT PRIMARY KEY NOT NULL, selections_json TEXT NOT NULL, updated_at INTEGER NOT NULL);");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("PREF_STORE_MIGRATION");
    }

    public void save(String ownerKey, SearchPreference pref) {
        if (ownerKey == null || pref == null) return;
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            ContentValues cv = new ContentValues();
            cv.put("owner_key", ownerKey);
            cv.put("selections_json", pref.toJson());
            cv.put("updated_at", pref.updatedAt);
            db.insertWithOnConflict("local_search_preferences", null, cv, SQLiteDatabase.CONFLICT_REPLACE);
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }

    public SearchPreference read(String ownerKey) {
        if (ownerKey == null) return null;
        try (Cursor c = getReadableDatabase().query("local_search_preferences", new String[]{"selections_json"}, "owner_key=?", new String[]{ownerKey}, null, null, null)) {
            return c.moveToFirst() ? SearchPreference.fromJson(c.getString(0)) : null;
        } catch (Exception ignored) { return null; }
    }

    public void clear(String ownerKey) {
        if (ownerKey == null) return;
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try {
            db.delete("local_search_preferences", "owner_key=?", new String[]{ownerKey});
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
    }
}
