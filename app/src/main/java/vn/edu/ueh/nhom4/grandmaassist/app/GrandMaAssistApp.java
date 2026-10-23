package vn.edu.ueh.nhom4.grandmaassist.app;

import android.app.Application;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;
import com.google.firebase.firestore.MemoryCacheSettings;

import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.AuthRepository;

public class GrandMaAssistApp extends Application {
    private AuthRepository authRepository;

    // Application scope serializes Auth operations across Activity recreation.
    public AuthRepository getAuthRepository() {
        if (authRepository == null) authRepository = new AuthRepository(this);
        return authRepository;
    }

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
