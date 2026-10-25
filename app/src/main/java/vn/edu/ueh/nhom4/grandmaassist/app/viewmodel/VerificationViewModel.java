package vn.edu.ueh.nhom4.grandmaassist.app.viewmodel;

import android.app.Application;
import android.graphics.Bitmap;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.*;
import com.google.android.gms.tasks.*;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.storage.StorageException;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.VerificationRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.VerificationImages;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.CardQrReader;
import vn.edu.ueh.nhom4.grandmaassist.app.BuildConfig;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.CardIdentity;

/** Sensitive draft is memory-only. Reopening reads server; no saved state or durable URI. */
public final class VerificationViewModel extends AndroidViewModel {
    public enum ImageTarget { IDENTITY, EVIDENCE }
    public final MutableLiveData<Long> changes=new MutableLiveData<>(0L);
    public final VerificationRepository repository=new VerificationRepository();
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    public final String uid;
    public VerificationRepository.Home home;
    public List<VerificationRepository.Record> queue=new ArrayList<>();
    public List<Map<String,Object>> notifications=new ArrayList<>();
    public VerificationRepository.Record selected;
    public final Map<String,byte[]> reviewImages=new LinkedHashMap<>();
    public byte[] identityImage,candidate;
    private byte[] candidateQrSource;
    public final List<byte[]> evidence=new ArrayList<>();
    public CardIdentity identity;
    public String message="",evidenceType="RETIREMENT_CERTIFICATE",queueRole="SEEKER",lookup="",reason="";
    public String reasonAction="",reasonMessage="";
    public boolean busy,loaded,consent,reviewed,externalPending,sessionEnded,sending,readRequired;
    public boolean openCurrentOnLoad;
    public ImageTarget pendingImageTarget, processingImageTarget, imageFocus;
    public String identityMessage="", evidenceMessage="";
    public long imageEvent;
    public File capture;
    private volatile long epoch;
    private String requestId;
    private VerificationRepository.Home stagedBase;
    private final FirebaseAuth.AuthStateListener authListener;
    public VerificationViewModel(@NonNull Application app) {
        super(app);
        uid=FirebaseAuth.getInstance().getCurrentUser()==null?"":FirebaseAuth.getInstance().getCurrentUser().getUid();
        authListener=auth->{ if(!repository.same(uid)) { invalidate(); sessionEnded=true; emit(); } };
        FirebaseAuth.getInstance().addAuthStateListener(authListener);
    }
    public void emit() { changes.setValue(changes.getValue()+1); }
    private boolean valid(long e) { return e==epoch && repository.same(uid) && !sessionEnded; }
    public boolean dirty() { return identityImage!=null || candidate!=null || !evidence.isEmpty(); }
    public boolean frozen() { return requestId!=null; }
    public void load() {
        if(busy || sessionEnded) return;
        long e=++epoch; busy=true; selected=null;reviewImages.clear();reviewed=false;reason=lookup=reasonAction=reasonMessage="";
        message="Đang tải hồ sơ…"; emit();
        repository.home(uid).addOnCompleteListener(t->{
            if(!valid(e))return;
            busy=false;
            if(!t.isSuccessful()) { loaded=false; message=error(t.getException()); emit(); return; }
            home=t.getResult(); loaded=true; readRequired=false; message="";
            boolean viewCurrent=openCurrentOnLoad;openCurrentOnLoad=false;
            if(viewCurrent && !"ADMIN".equals(home.role) && home.request!=null){select(home.request);return;}
            emit();
            if("ADMIN".equals(home.role)) loadQueue(); else loadNotices();
        });
    }
    public void loadNotices() {
        long e=epoch;
        repository.notifications(uid).addOnCompleteListener(t->{
            if(!valid(e))return;
            if(t.isSuccessful()) notifications=t.getResult();
            else message="Chưa tải được thông báo. Hãy tải lại.";
            emit();
        });
    }
    public void loadQueue() {
        loadQueue("");
    }
    private void loadQueue(String resultMessage) {
        if(busy || sessionEnded)return;
        long e=++epoch; busy=true; selected=null; reviewImages.clear(); reviewed=false;reason=lookup=reasonAction=reasonMessage="";
        queue=new ArrayList<>(); message=(resultMessage+" Đang tải danh sách…").trim(); emit();
        repository.queue(uid,queueRole).addOnCompleteListener(t->{
            if(!valid(e))return; busy=false;
            if(t.isSuccessful()) { queue=t.getResult(); message=(resultMessage+" "+(queue.isEmpty()?"Chưa có hồ sơ chờ duyệt.":"")).trim(); }
            else message=(resultMessage+" "+error(t.getException())).trim();
            emit();
        });
    }
    public void select(VerificationRepository.Record record) {
        if(busy)return;
        selected=record; lookup=reason=reasonAction=reasonMessage=""; reviewed=false; reviewImages.clear(); loadImages();
    }
    public void closeReview(){
        if(busy)return;
        ++epoch;selected=null;reviewImages.clear();reviewed=false;reason=lookup=reasonAction=reasonMessage=message="";emit();
    }
    public void find(String key) {
        if(busy)return;
        if(key.isEmpty() || key.contains("/") || key.length()>128){message="Nhập UID hoặc mã hồ sơ hợp lệ.";emit();return;}
        long e=++epoch;busy=true;message="Đang tìm hồ sơ…";emit();
        repository.find(uid,queueRole,key).addOnCompleteListener(t->{
            if(!valid(e))return;busy=false;
            if(t.isSuccessful())select(t.getResult());else{message="Chưa tìm được hồ sơ hoặc không có quyền truy cập.";emit();}
        });
    }
    public void openRecord(String role,String id) {
        if(busy || id.isEmpty() || id.contains("/"))return;
        long e=++epoch;busy=true;message="Đang mở hồ sơ…";emit();
        repository.request(uid,role,id).addOnCompleteListener(t->{
            if(!valid(e))return;busy=false;
            if(t.isSuccessful())select(t.getResult());else{message=error(t.getException());emit();}
        });
    }
    public void revoke() {
        if(busy || selected==null || !reviewed || reviewImages.size()!=selected.paths().size())return;
        if(!validateReason())return;
        long e=++epoch;busy=true;message="Đang lưu thu hồi…";emit();
        repository.revoke(uid,selected,reason).addOnCompleteListener(t->{
            if(!valid(e))return;busy=false;selected=null;reviewImages.clear();reviewed=false;
            reason=lookup=reasonAction=reasonMessage="";
            reviewDiagnostic(t.isSuccessful()?"REVOKE_SAVED":"REVOKE_FAILED",t.getException());
            message=t.isSuccessful()?"Đã thu hồi kết quả. Chủ hồ sơ cần gửi xét duyệt lại.":error(t.getException())+" Hãy tải lại hồ sơ để kiểm tra.";
            emit();
        });
    }
    public void loadImages() {
        if(selected==null || busy)return;
        long e=++epoch; VerificationRepository.Record target=selected;
        busy=true; reviewImages.clear(); message="Đang tải ảnh…"; emit();
        Task<Void> chain=Tasks.forResult(null);
        for(String path:target.paths()) chain=chain.continueWithTask(t->{
            if(!t.isSuccessful())throw t.getException();
            if(!valid(e))throw new VerificationRepository.Problem("SESSION");
            return repository.image(uid,path).continueWithTask(image->{
                if(!image.isSuccessful())throw image.getException();
                if(!valid(e))throw new VerificationRepository.Problem("SESSION");
                return Tasks.call(worker,()->{
                    Bitmap decoded=VerificationImages.preview(image.getResult(),1200);
                    if(decoded==null)throw new VerificationRepository.Problem("IMAGE");
                    decoded.recycle(); return image.getResult();
                }).continueWith(checked->{
                    if(!checked.isSuccessful())throw checked.getException();
                    if(valid(e)) reviewImages.put(path,checked.getResult()); return null;
                });
            });
        });
        chain.addOnCompleteListener(t->{
            if(!valid(e))return; busy=false;
            message=t.isSuccessful()?"":"Chưa xem được đủ ảnh. Hãy tải lại ảnh trước khi duyệt."; emit();
        });
    }
    public boolean beginImageRequest(ImageTarget target) {
        if(busy || frozen() || externalPending || sessionEnded || home==null || !home.active)return false;
        if(target==ImageTarget.EVIDENCE && (!"SEEKER".equals(home.role) || evidence.size()>=3))return false;
        pendingImageTarget=target; externalPending=true; imageMessage(target,""); emit(); return true;
    }
    public void cancelImageRequest(ImageTarget target,String detail) {
        if(pendingImageTarget!=null && pendingImageTarget!=target)return;
        pendingImageTarget=null; externalPending=false;
        if(target==ImageTarget.IDENTITY)deleteCapture();
        imageMessage(target,detail); focusImage(target); emit();
    }
    public void completeImageRequest(Uri uri,ImageTarget target) {
        // Keep the request destination separate from the image being previewed.
        // A result after process death has no surviving owner/request and must be picked again.
        if(sessionEnded || !repository.same(uid))return;
        if(pendingImageTarget!=null && pendingImageTarget!=target)return;
        if(!externalPending || pendingImageTarget!=target) {
            imageMessage(target,"Lần chọn ảnh đã kết thúc. Hãy chọn lại ảnh."); focusImage(target); emit(); return;
        }
        pendingImageTarget=null; externalPending=false;
        if(uri==null) { imageMessage(target,"Đã hủy chọn ảnh."); focusImage(target); emit(); return; }
        readImage(uri,target);
    }
    private void imageMessage(ImageTarget target,String value) {
        if(target==ImageTarget.IDENTITY)identityMessage=value; else evidenceMessage=value;
    }
    private void focusImage(ImageTarget target) { imageFocus=target; ++imageEvent; }
    private void readImage(Uri uri,ImageTarget target) {
        if(busy || frozen() || sessionEnded)return;
        long e=++epoch; busy=true; processingImageTarget=target;
        imageMessage(target,"Đang mở ảnh…"); emit();
        Tasks.call(worker,()->VerificationImages.prepare(getApplication(),uri,target==ImageTarget.IDENTITY)).addOnCompleteListener(t->{
            if(!valid(e))return; busy=false; processingImageTarget=null;
            if(target==ImageTarget.IDENTITY)deleteCapture();
            if(!t.isSuccessful()) imageMessage(target,"Không mở được ảnh. Chọn JPEG/PNG tối đa 10 MB.");
            else if(target==ImageTarget.IDENTITY) {
                candidate=t.getResult().image; candidateQrSource=t.getResult().qrSource; identityMessage="";
            } else if(evidence.size()<3) {
                // Gallery evidence is immediately visible and removable in its own section.
                evidence.add(t.getResult().image); evidenceMessage="";
            } else evidenceMessage="Đã đủ 3 ảnh bằng chứng. Bỏ một ảnh để chọn ảnh khác.";
            focusImage(target);
            emit();
        });
    }
    public void acceptCandidate() {
        if(candidate==null || busy || frozen())return;
        final byte[] bytes=candidate;
        long e=++epoch; busy=true; processingImageTarget=ImageTarget.IDENTITY;
        identityMessage="Đang đọc mã QR…"; emit();
        CardQrReader.read(worker,candidateQrSource==null?bytes:candidateQrSource,()->valid(e)).addOnCompleteListener(t->{
            if(!valid(e))return; busy=false; processingImageTarget=null;
            if(t.isSuccessful()) {
                identity=t.getResult(); identityImage=bytes; candidate=candidateQrSource=null; identityMessage="";consent=false;
                qrDiagnostic("OK");
            } else {
                String code=qrFailureCode(t.getException()); qrDiagnostic(code);
                qrStructureDiagnostic(t.getException());
                identityMessage=qrFailureMessage(code);
            }
            focusImage(ImageTarget.IDENTITY);
            emit();
        });
    }
    public void discardCandidate() {
        if(!busy && !externalPending && !frozen()) {
            candidate=candidateQrSource=null; identityMessage=""; focusImage(ImageTarget.IDENTITY); emit();
        }
    }
    public void removeEvidence(byte[] bytes) {
        if(busy || externalPending || frozen() || sessionEnded)return;
        evidence.remove(bytes); evidenceMessage=""; focusImage(ImageTarget.EVIDENCE); emit();
    }
    public void clearDraft() {
        if(busy)return;
        requestId=null; stagedBase=null; identity=null; identityImage=candidate=candidateQrSource=null; evidence.clear();
        consent=false; message=identityMessage=evidenceMessage="";
        pendingImageTarget=processingImageTarget=imageFocus=null; externalPending=false;
        deleteCapture(); emit();
    }
    /** Shared presentation/write preconditions. Employer consent is taken in the send dialog. */
    public String submissionRequirement(boolean requireConsent) {
        if(sessionEnded || !loaded || home==null)return "Tải lại hồ sơ để kiểm tra tài khoản.";
        if(!Arrays.asList("SEEKER","EMPLOYER").contains(home.role))return "Tài khoản không có quyền gửi hồ sơ này.";
        if(!home.active)return "Tài khoản chưa được phép gửi hồ sơ.";
        if(readRequired)return "Tải lại để kiểm tra hồ sơ trước khi gửi tiếp.";
        if("SUBMITTED".equals(home.status))return "Hồ sơ đang chờ xét duyệt.";
        if(!home.phone)return "Thêm và đồng bộ số điện thoại đã xác minh trước khi gửi.";
        if(!home.complete)return home.profileRequirement;
        if(candidate!=null)return "Bấm Dùng ảnh và đọc QR để xác nhận ảnh căn cước.";
        if(identity==null || identityImage==null)return "Chọn hoặc chụp căn cước rồi đọc QR trước khi gửi.";
        if("SEEKER".equals(home.role) && (evidence.isEmpty() || evidence.size()>3))return "Thêm từ 1 đến 3 ảnh bằng chứng nghỉ hưu.";
        if(requireConsent && !consent)return "Đồng ý gửi giấy tờ trước khi gửi hồ sơ.";
        return "";
    }
    /** Called only from the employer's explicit positive confirmation button. */
    public void confirmEmployerAndSend() {
        if(busy || externalPending || home==null || !"EMPLOYER".equals(home.role))return;
        if(!submissionRequirement(false).isEmpty()){message=submissionRequirement(false);emit();return;}
        consent=true;send();
    }
    public void send() {
        if(busy || externalPending)return;
        String requirement=submissionRequirement(true);
        if(!requirement.isEmpty()){message=requirement;emit();return;}
        if(requestId==null) { requestId=UUID.randomUUID().toString(); stagedBase=home; }
        long e=++epoch; String id=requestId; VerificationRepository.Home base=stagedBase;
        List<byte[]> images=new ArrayList<>(); images.add(identityImage);
        if("SEEKER".equals(base.role))images.addAll(evidence);
        busy=sending=true; message="Đang gửi hồ sơ…"; emit();
        Task<Void> chain=repository.stage(base,id,identity,"SEEKER".equals(base.role)?evidenceType:"IDENTITY_SAMPLE",images.size());
        for(int i=0;i<images.size();i++) {
            final int index=i;
            chain=chain.continueWithTask(t->{
                if(!t.isSuccessful())throw t.getException();
                if(!valid(e))throw new VerificationRepository.Problem("SESSION");
                return repository.upload(uid,VerificationRepository.path(uid,id,index),images.get(index));
            });
        }
        chain=chain.continueWithTask(t->{
            if(!t.isSuccessful())throw t.getException();
            if(!valid(e))throw new VerificationRepository.Problem("SESSION");
            return repository.submit(base,id);
        });
        chain.addOnCompleteListener(t->{
            if(!valid(e))return;
            if(t.isSuccessful()) { busy=sending=false; clearDraft(); load(); return; }
            // A lost response may still mean committed. Reconcile before another submission.
            repository.home(uid).addOnCompleteListener(read->{
                if(!valid(e))return; busy=sending=false;
                if(read.isSuccessful() && id.equals(read.getResult().currentId)) {
                    home=read.getResult(); clearDraft(); message="Đã gửi hồ sơ."; loadNotices();
                } else message=error(t.getException())+" Có thể thử gửi lại cùng bản hoặc bỏ bản nháp và tải lại.";
                emit();
            });
        });
    }
    public void cancelSending(){
        if(!sending)return;
        ++epoch;repository.cancelUploads();busy=sending=false;readRequired=true;
        message="Đã dừng chờ. Hãy tải lại để kiểm tra hồ sơ trước khi gửi tiếp.";emit();
    }
    public void requestReason(String action) {
        if(busy || selected==null || sessionEnded)return;
        boolean submitted="SUBMITTED".equals(selected.text("status"));
        if(!(submitted && Arrays.asList("NEEDS_INFO","REJECTED").contains(action))
                && !(Arrays.asList("APPROVED","VERIFIED").contains(selected.text("status")) && "REVOKE".equals(action)))return;
        reasonAction=action;reasonMessage=message="";emit();
    }
    public void cancelReason() { if(!busy){reason=reasonAction=reasonMessage=message="";emit();} }
    private boolean validateReason() {
        String value=reason.trim();reasonMessage="";
        if(value.isEmpty())reasonMessage="Nhập lý do trước khi xác nhận.";
        else if(value.length()>1000)reasonMessage="Lý do tối đa 1.000 ký tự.";
        else if(value.matches("(?s).*[0-9]{12}.*"))reasonMessage="Không ghi số căn cước trong lý do.";
        if(reasonMessage.isEmpty())return true;
        message=reasonMessage;emit();return false;
    }
    public boolean prepareDecision(String decision) {
        if(busy || selected==null || !reviewed || reviewImages.size()!=selected.paths().size())return false;
        if("REVOKE".equals(decision))return validateReason();
        if(lookup.isEmpty()) { message="Chọn kết quả đối chiếu trước khi xác nhận."; emit(); return false; }
        if(Arrays.asList("APPROVED","VERIFIED").contains(decision) && !"MATCH".equals(lookup)) {
            message="Chỉ chấp thuận khi đối chiếu phù hợp."; emit(); return false;
        }
        return Arrays.asList("APPROVED","VERIFIED").contains(decision) || validateReason();
    }
    public void decide(String decision) {
        if(!prepareDecision(decision))return;
        if(Arrays.asList("APPROVED","VERIFIED").contains(decision))reason=reasonAction=reasonMessage="";
        long e=++epoch; busy=true; message="Đang lưu quyết định…"; emit();
        VerificationRepository.Record target=selected;
        reviewDiagnostic("DECISION_START",null);
        repository.decide(uid,target,decision,lookup,reason).addOnCompleteListener(t->{
            if(!valid(e))return; busy=false;
            reviewDiagnostic(t.isSuccessful()?"DECISION_SAVED":"DECISION_FAILED",t.getException());
            // Never replay a decision after an ambiguous response; refresh authoritative queue.
            if(t.isSuccessful())loadQueue(Arrays.asList("APPROVED","VERIFIED").contains(decision)?"Đã chấp thuận hồ sơ.":
                    "NEEDS_INFO".equals(decision)?"Đã yêu cầu bổ sung hồ sơ.":"Đã từ chối hồ sơ.");
            else { selected=null; reviewImages.clear(); reviewed=false;
                reason=lookup=reasonAction=reasonMessage="";
                message=error(t.getException())+" Tải lại danh sách để kiểm tra quyết định."; emit(); }
        });
    }
    public void deleteCapture() {
        if(capture!=null) { capture.delete(); capture=null; }
    }
    private void invalidate() {
        ++epoch;repository.cancelUploads(); busy=sending=false; home=null; selected=null; identity=null; identityImage=candidate=candidateQrSource=null;
        evidence.clear(); queue.clear(); reviewImages.clear(); notifications.clear(); reason=lookup=reasonAction=reasonMessage="";
        identityMessage=evidenceMessage=""; pendingImageTarget=processingImageTarget=imageFocus=null;
        externalPending=false; deleteCapture();
        openCurrentOnLoad=false;
    }
    @Override protected void onCleared() {
        FirebaseAuth.getInstance().removeAuthStateListener(authListener);
        invalidate(); worker.shutdown(); super.onCleared();
    }
    private static String qrFailureCode(Exception exception) {
        Throwable cause=exception;
        for(int depth=0;cause!=null && depth<8;depth++,cause=cause.getCause())
            if(cause instanceof CardQrReader.Failure)return ((CardQrReader.Failure)cause).code;
        return "ENGINE";
    }
    private static void qrDiagnostic(String code) {
        // Only a fixed diagnostic code, never raw QR, URI, identity values, or exception messages.
        if(BuildConfig.DEBUG)android.util.Log.i("GrandMaQR","result="+code);
    }
    private static void reviewDiagnostic(String stage,Exception exception) {
        if(!BuildConfig.DEBUG)return;
        String code=exception==null?"OK":"OTHER";
        Throwable cause=exception;
        for(int depth=0;cause!=null && depth<8;depth++,cause=cause.getCause()) {
            if(cause instanceof FirebaseFirestoreException){code=((FirebaseFirestoreException)cause).getCode().name();break;}
            if(cause instanceof VerificationRepository.Problem){code=((VerificationRepository.Problem)cause).code;break;}
        }
        // Fixed stages/codes only; no identity, UID, paths, reason, token or exception text.
        android.util.Log.i("GrandMaReview","stage="+stage+" code="+code);
    }
    private static void qrStructureDiagnostic(Exception exception) {
        if(!BuildConfig.DEBUG)return;
        Throwable cause=exception;
        for(int depth=0;cause!=null && depth<8;depth++,cause=cause.getCause()) {
            if(cause instanceof CardQrReader.Failure) {
                CardQrReader.Failure failure=(CardQrReader.Failure)cause;
                if("FORMAT".equals(failure.code) && !failure.structure.isEmpty())
                    android.util.Log.i("GrandMaQR","structure "+failure.structure);
                return;
            }
        }
    }
    private static String qrFailureMessage(String code) {
        switch(code) {
            case "NOT_FOUND": return "Chưa thấy QR trong ảnh. Chụp mặt có QR, gần hơn và đủ sáng.";
            case "UNREADABLE": return "Đã thấy vùng QR nhưng chưa đọc được. Giữ máy rõ nét, tránh lóa mã.";
            case "MULTIPLE": return "Ảnh có nhiều mã QR. Hãy chụp riêng căn cước cần đọc.";
            case "FORMAT": return "Đã đọc QR nhưng định dạng căn cước này chưa được hỗ trợ.";
            case "DOCUMENT_NUMBER": return "Đã đọc QR nhưng số định danh không hợp lệ. Hãy kiểm tra đúng QR căn cước.";
            case "NAME": return "Đã đọc QR nhưng thiếu họ tên hợp lệ. Hãy kiểm tra lại căn cước.";
            case "BIRTH_DATE": return "Đã đọc QR nhưng ngày sinh không hợp lệ. Không thể tự điền ngày sinh.";
            case "IMAGE": return "Ảnh đã nhận nhưng chưa xử lý được để đọc QR. Hãy chọn hoặc chụp lại.";
            default: return "Bộ đọc QR chưa xử lý được. Hãy thử lại.";
        }
    }
    public static String error(Exception ex) {
        for(Throwable e=ex;e!=null;e=e.getCause()) {
            if(e instanceof VerificationRepository.Problem) {
                String code=((VerificationRepository.Problem)e).code;
                if("CONFLICT".equals(code))return "Hồ sơ đã thay đổi. Hãy bỏ bản nháp và tải lại.";
                if("SESSION".equals(code))return "Phiên đăng nhập đã thay đổi.";
                if("ACCESS".equals(code))return "Tài khoản chưa có quyền xử lý hồ sơ.";
                if("STATE".equals(code))return "Hồ sơ đang chờ duyệt hoặc tài khoản bị hạn chế.";
            }
            if(e instanceof FirebaseFirestoreException) {
                FirebaseFirestoreException.Code c=((FirebaseFirestoreException)e).getCode();
                if(c==FirebaseFirestoreException.Code.PERMISSION_DENIED)return "Chưa có quyền thực hiện. Nhờ người hỗ trợ kiểm tra.";
                if(c==FirebaseFirestoreException.Code.UNAVAILABLE || c==FirebaseFirestoreException.Code.DEADLINE_EXCEEDED)
                    return "Không kết nối được. Hãy thử lại khi có mạng.";
            }
            if(e instanceof StorageException)return "Chưa tải được ảnh. Hãy kiểm tra mạng và thử lại.";
        }
        return "Chưa hoàn tất thao tác. Hãy tải lại để kiểm tra.";
    }
}
