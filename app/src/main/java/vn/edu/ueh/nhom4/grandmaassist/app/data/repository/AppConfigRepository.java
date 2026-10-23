package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Source;

/** Reads only public configuration, always from the server. */
public final class AppConfigRepository {
    public Task<DocumentSnapshot> loadPublicConfig() {
        return FirebaseFirestore.getInstance()
                .collection("appConfig").document("public").get(Source.SERVER);
    }
}
