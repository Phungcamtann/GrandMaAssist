package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import java.util.Calendar;
import java.util.function.Consumer;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.ApplicationRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.WorkSessionRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.AgreementSnapshot;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.Application;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.WorkSession;

public final class ShiftAgreementViewModel extends ViewModel {
    public final MutableLiveData<Long> changes = new MutableLiveData<>(0L);

    public boolean busy = false;
    public String message = "";
    public WorkSession currentSession = null;
    public Application currentApplication = null;

    // Form state
    public String tasks = "";
    public String privateAddress = "";
    public String areaName = "";
    public String areaCode = "";
    public Calendar startCalendar = Calendar.getInstance();
    public Calendar endCalendar = Calendar.getInstance();
    public long laborAmount = 0L;
    public long serviceFee = 0L;
    public long totalDue = 0L;
    public boolean dateSet = false;
    public boolean startTimeSet = false;
    public boolean endTimeSet = false;

    private final WorkSessionRepository repository = new WorkSessionRepository();
    private final ApplicationRepository applicationRepository = new ApplicationRepository();

    public ShiftAgreementViewModel() {
        // Mặc định: ngày mai, 08:00 - 11:30
        startCalendar.add(Calendar.DAY_OF_YEAR, 1);
        startCalendar.set(Calendar.HOUR_OF_DAY, 8);
        startCalendar.set(Calendar.MINUTE, 0);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);

        endCalendar.setTimeInMillis(startCalendar.getTimeInMillis());
        endCalendar.set(Calendar.HOUR_OF_DAY, 11);
        endCalendar.set(Calendar.MINUTE, 30);
    }

    public void emit() {
        Long v = changes.getValue();
        changes.setValue(v == null ? 1L : v + 1L);
    }

    public String currentUid() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        return user != null ? user.getUid() : "";
    }

    public void updateLaborAmount(long amount) {
        this.laborAmount = amount;
        this.serviceFee = AgreementSnapshot.computeFee(amount);
        this.totalDue = laborAmount + serviceFee;
        emit();
    }

    public void loadSession(String sessionId) {
        if (sessionId == null || sessionId.trim().isEmpty()) return;
        busy = true;
        message = "";
        emit();

        repository.getSession(sessionId).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                currentSession = t.getResult();
                if (currentSession != null) {
                    if (currentSession.agreement != null) {
                        tasks = currentSession.agreement.tasks;
                        privateAddress = currentSession.agreement.privateAddress;
                        areaCode = currentSession.agreement.areaCode;
                        updateLaborAmount(currentSession.agreement.laborAmount);
                        if (currentSession.agreement.startAt != null) {
                            startCalendar.setTime(currentSession.agreement.startAt.toDate());
                            dateSet = true;
                            startTimeSet = true;
                        }
                        if (currentSession.agreement.endAt != null) {
                            endCalendar.setTime(currentSession.agreement.endAt.toDate());
                            endTimeSet = true;
                        }
                    }
                    if (currentSession.applicationId != null && !currentSession.applicationId.isEmpty()) {
                        String uid = currentUid();
                        applicationRepository.getApplication(uid, currentSession.applicationId).addOnCompleteListener(at -> {
                            if (at.isSuccessful() && at.getResult() != null) {
                                currentApplication = at.getResult();
                                if (currentApplication.jobSnapshot != null) {
                                    areaName = currentApplication.jobSnapshot.areaName;
                                    areaCode = currentApplication.jobSnapshot.areaCode;
                                    privateAddress = currentApplication.jobSnapshot.workAddress;
                                }
                                emit();
                            }
                        });
                    }
                }
            } else {
                message = WorkSessionRepository.error(t.getException());
            }
            emit();
        });
    }

    public void loadApplicationForInit(String applicationId, Runnable onLoaded) {
        if (applicationId == null || applicationId.trim().isEmpty()) return;
        busy = true;
        emit();

        // Query session by applicationId first to see if one already exists
        String uid = currentUid();
        repository.getSessionsForApplication(uid, applicationId).addOnCompleteListener(st -> {
            if (st.isSuccessful() && st.getResult() != null && !st.getResult().isEmpty()) {
                // Already has a session!
                currentSession = st.getResult().get(0);
                if (currentSession.agreement != null) {
                    tasks = currentSession.agreement.tasks;
                    privateAddress = currentSession.agreement.privateAddress;
                    areaCode = currentSession.agreement.areaCode;
                    updateLaborAmount(currentSession.agreement.laborAmount);
                    if (currentSession.agreement.startAt != null) {
                        startCalendar.setTime(currentSession.agreement.startAt.toDate());
                        dateSet = true;
                        startTimeSet = true;
                    }
                    if (currentSession.agreement.endAt != null) {
                        endCalendar.setTime(currentSession.agreement.endAt.toDate());
                        endTimeSet = true;
                    }
                }
            } else {
                // If not existing, prefill from application doc
                applicationRepository.getApplication(uid, applicationId).addOnCompleteListener(at -> {
                    busy = false;
                    if (at.isSuccessful() && at.getResult() != null) {
                        Application app = at.getResult();
                        if (app.jobSnapshot != null) {
                            if (tasks.isEmpty()) tasks = app.jobSnapshot.title;
                            privateAddress = app.jobSnapshot.workAddress;
                            areaName = app.jobSnapshot.areaName;
                            areaCode = app.jobSnapshot.areaCode;
                            if (laborAmount == 0) updateLaborAmount(app.jobSnapshot.payAmount);
                        }
                    }
                    emit();
                    if (onLoaded != null) onLoaded.run();
                });
                return;
            }
            busy = false;
            emit();
            if (onLoaded != null) onLoaded.run();
        });
    }

    public void propose(String applicationId, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        if (tasks.trim().isEmpty()) {
            if (onError != null) onError.accept("Vui lòng nhập nội dung công việc của ca.");
            return;
        }
        if (privateAddress.trim().isEmpty()) {
            if (onError != null) onError.accept("Thiếu thông tin địa chỉ làm việc từ tin tuyển dụng.");
            return;
        }
        if (startCalendar.compareTo(endCalendar) >= 0) {
            if (onError != null) onError.accept("Giờ kết thúc phải sau giờ bắt đầu.");
            return;
        }
        if (startCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
            if (onError != null) onError.accept("Thời gian bắt đầu ca phải ở tương lai.");
            return;
        }
        if (laborAmount <= 0) {
            if (onError != null) onError.accept("Vui lòng nhập số tiền công hợp lệ.");
            return;
        }

        busy = true;
        message = "Đang gửi đề xuất thỏa thuận ca…";
        emit();

        Timestamp startTs = new Timestamp(startCalendar.getTime());
        Timestamp endTs = new Timestamp(endCalendar.getTime());

        repository.proposeSession(uid, applicationId, tasks.trim(), privateAddress.trim(), areaCode,
                startTs, endTs, laborAmount).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                message = "";
                emit();
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WorkSessionRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void edit(String sessionId, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        if (tasks.trim().isEmpty()) {
            if (onError != null) onError.accept("Vui lòng nhập nội dung công việc của ca.");
            return;
        }
        if (privateAddress.trim().isEmpty()) {
            if (onError != null) onError.accept("Thiếu thông tin địa chỉ làm việc từ tin tuyển dụng.");
            return;
        }
        if (startCalendar.compareTo(endCalendar) >= 0) {
            if (onError != null) onError.accept("Giờ kết thúc phải sau giờ bắt đầu.");
            return;
        }
        if (startCalendar.getTimeInMillis() <= System.currentTimeMillis()) {
            if (onError != null) onError.accept("Thời gian bắt đầu ca phải ở tương lai.");
            return;
        }
        if (laborAmount <= 0) {
            if (onError != null) onError.accept("Vui lòng nhập số tiền công hợp lệ.");
            return;
        }

        busy = true;
        message = "Đang cập nhật đề xuất thỏa thuận…";
        emit();

        Timestamp startTs = new Timestamp(startCalendar.getTime());
        Timestamp endTs = new Timestamp(endCalendar.getTime());

        repository.editProposal(uid, sessionId, tasks.trim(), privateAddress.trim(), areaCode,
                startTs, endTs, laborAmount).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                message = "";
                emit();
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WorkSessionRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void seekerAccept(String sessionId, long version, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        message = "Đang kiểm tra lịch và xác nhận ca làm…";
        emit();

        // Check schedule conflict first
        if (currentSession != null && currentSession.agreement != null
                && currentSession.agreement.startAt != null && currentSession.agreement.endAt != null) {
            repository.checkScheduleConflict(uid, currentSession.agreement.startAt, currentSession.agreement.endAt, sessionId)
                    .addOnCompleteListener(ct -> {
                        if (ct.isSuccessful() && ct.getResult() != null) {
                            busy = false;
                            emit();
                            WorkSession conf = ct.getResult();
                            String confTask = conf.agreement != null ? conf.agreement.tasks : "ca làm khác";
                            if (onError != null) {
                                onError.accept("Trùng lịch: Cô/chú đã có ca \"" + confTask + "\" trong khung giờ này.");
                            }
                            return;
                        }

                        // No conflict -> proceed to accept
                        doSeekerAccept(uid, sessionId, version, onSuccess, onError);
                    });
        } else {
            doSeekerAccept(uid, sessionId, version, onSuccess, onError);
        }
    }

    private void doSeekerAccept(String uid, String sessionId, long version, Runnable onSuccess, Consumer<String> onError) {
        repository.seekerAccept(uid, sessionId, version).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                loadSession(sessionId);
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WorkSessionRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void seekerReject(String sessionId, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        emit();

        repository.seekerReject(uid, sessionId).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                loadSession(sessionId);
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WorkSessionRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void fundSession(String sessionId, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        message = "Đang xử lý trích tiền ví và giữ cọc cho ca làm…";
        emit();

        repository.fundSession(uid, sessionId).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                loadSession(sessionId);
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WorkSessionRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void confirmCheckIn(String sessionId, String role, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        message = "Đang xử lý điểm danh ca làm việc...";
        emit();

        repository.confirmCheckIn(uid, sessionId, role).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                currentSession = t.getResult();
                message = "";
                emit();
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WorkSessionRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void reportCompletion(String sessionId, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        message = "Đang gửi báo cáo hoàn thành ca làm...";
        emit();

        repository.reportCompletion(uid, sessionId).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                currentSession = t.getResult();
                message = "";
                emit();
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WorkSessionRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void confirmCompletionAndSettle(String sessionId, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        message = "Đang xác nhận hoàn tất ca và quyết toán tiền công...";
        emit();

        repository.confirmCompletionAndSettle(uid, sessionId).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                currentSession = t.getResult();
                message = "";
                emit();
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WorkSessionRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void syncSettlementToWallet(String seekerId) {
        if (currentSession != null && currentSession.isCompleted()) {
            repository.syncSettlementToWallet(seekerId, currentSession);
        }
    }

    public void requestCancelSession(String sessionId, String reason, String role, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        message = "Đang gửi yêu cầu hủy ca làm...";
        emit();

        repository.requestCancelSession(uid, sessionId, reason, role).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                message = "";
                loadSession(sessionId);
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WorkSessionRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void confirmCancelSession(String sessionId, String role, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        message = "Đang xác nhận hủy ca làm và hoàn trả tiền...";
        emit();

        repository.confirmCancelSession(uid, sessionId, role).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                message = "";
                loadSession(sessionId);
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WorkSessionRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void rejectCancelRequest(String sessionId, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        message = "Đang từ chối yêu cầu hủy ca...";
        emit();

        repository.rejectCancelRequest(uid, sessionId).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                message = "";
                loadSession(sessionId);
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WorkSessionRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void withdrawCancelRequest(String sessionId, Runnable onSuccess, Consumer<String> onError) {
        String uid = currentUid();
        if (uid.isEmpty()) {
            if (onError != null) onError.accept("Chưa đăng nhập.");
            return;
        }

        busy = true;
        message = "Đang rút lại yêu cầu hủy...";
        emit();

        repository.withdrawCancelRequest(uid, sessionId).addOnCompleteListener(t -> {
            busy = false;
            if (t.isSuccessful()) {
                message = "";
                loadSession(sessionId);
                if (onSuccess != null) onSuccess.run();
            } else {
                String err = WorkSessionRepository.error(t.getException());
                message = err;
                emit();
                if (onError != null) onError.accept(err);
            }
        });
    }

    public void syncRefundToEmployerWallet(String employerId) {
        if (currentSession != null && currentSession.isCancelled()) {
            repository.syncRefundToEmployerWallet(employerId, currentSession);
        }
    }
}
