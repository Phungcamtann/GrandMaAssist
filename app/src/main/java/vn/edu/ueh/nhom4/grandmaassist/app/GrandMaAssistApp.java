package vn.edu.ueh.nhom4.grandmaassist.app;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;

public class GrandMaAssistApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        FirebaseFirestoreSettings settings =
                new FirebaseFirestoreSettings.Builder()
                        .setLocalCacheSettings(
                                MemoryCacheSettings.newBuilder().build()
                        )
                        .build();

        FirebaseFirestore.getInstance()
                .setFirestoreSettings(settings);
    }
}