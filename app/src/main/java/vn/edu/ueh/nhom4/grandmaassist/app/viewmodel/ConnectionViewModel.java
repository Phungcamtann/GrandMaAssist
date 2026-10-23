package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.AppConfigRepository;

public final class ConnectionViewModel extends ViewModel {
    public static final class State {
        public final boolean loading;
        public final int messageRes;
        public final String serverMessage;

        private State(boolean loading, int messageRes, String serverMessage) {
            this.loading = loading;
            this.messageRes = messageRes;
            this.serverMessage = serverMessage;
        }
    }

    private final AppConfigRepository repository = new AppConfigRepository();
    private final MutableLiveData<State> state = new MutableLiveData<>();
    private boolean cleared;

    public ConnectionViewModel() {
        reload();
    }

    public LiveData<State> getState() {
        return state;
    }

    public void reload() {
        State current = state.getValue();
        if (cleared || (current != null && current.loading)) return;
        state.setValue(new State(true, R.string.connection_loading, null));
        repository.loadPublicConfig().addOnCompleteListener(task -> {
            if (cleared) return;
            if (!task.isSuccessful()) {
                int message = R.string.connection_error;
                Exception error = task.getException();
                if (error instanceof FirebaseFirestoreException) {
                    FirebaseFirestoreException.Code code =
                            ((FirebaseFirestoreException) error).getCode();
                    if (code == FirebaseFirestoreException.Code.UNAVAILABLE
                            || code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED) {
                        message = R.string.connection_network_error;
                    } else if (code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        message = R.string.connection_permission_error;
                    }
                }
                state.setValue(new State(false, message, null));
                return;
            }
            DocumentSnapshot document = task.getResult();
            if (document == null || !document.exists()) {
                state.setValue(new State(false, R.string.connection_missing, null));
                return;
            }
            Object message = document.get("message");
            if (!(message instanceof String) || ((String) message).trim().isEmpty()) {
                state.setValue(new State(false, R.string.connection_invalid, null));
                return;
            }
            state.setValue(new State(false, 0, (String) message));
        });
    }

    @Override
    protected void onCleared() {
        cleared = true;
        super.onCleared();
    }
}
