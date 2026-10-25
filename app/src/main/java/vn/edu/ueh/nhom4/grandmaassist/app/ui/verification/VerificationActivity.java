package vn.edu.ueh.nhom4.grandmaassist.app.ui.verification;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.*;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.view.*;
import androidx.lifecycle.ViewModelProvider;
import java.io.File;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.VerificationRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.VerificationViewModel;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.VerificationViewModel.ImageTarget;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;

/** A private document screen. Neither QR payloads nor document paths go into Intents/state. */
public final class VerificationActivity extends AppCompatActivity {
    public static final String OPEN_CURRENT="open_current_request";
    private VerificationViewModel vm;
    private boolean rendering;
    private byte[] renderedCandidate;
    private Object renderedIdentityKey,renderedEvidenceKey,renderedReviewKey,renderedQueueKey,renderedNoticeKey;
    private long lastImageEvent;
    private com.google.firebase.firestore.ListenerRegistration userChanges;
    private RoleNavigationBar navigation;
    private final ActivityResultLauncher<String> identityPicker=registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri->vm.completeImageRequest(uri,ImageTarget.IDENTITY));
    private final ActivityResultLauncher<String> evidencePicker=registerForActivityResult(new ActivityResultContracts.GetContent(),
            uri->vm.completeImageRequest(uri,ImageTarget.EVIDENCE));
    private final ActivityResultLauncher<Uri> camera=registerForActivityResult(new ActivityResultContracts.TakePicture(),ok->{
        if(Boolean.TRUE.equals(ok) && vm.capture!=null) {
            vm.completeImageRequest(FileProvider.getUriForFile(this,getPackageName()+".verification-files",vm.capture),ImageTarget.IDENTITY);
        } else vm.cancelImageRequest(ImageTarget.IDENTITY,
                Boolean.TRUE.equals(ok)?"Ảnh chưa được giữ lại. Hãy chụp lại.":"Đã hủy chụp ảnh.");
    });
    private final ActivityResultLauncher<String> permission=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted->{
        if(granted)launchCamera();
        else {
            vm.cancelImageRequest(ImageTarget.IDENTITY,"Chưa có quyền camera. Có thể chọn ảnh có sẵn.");
            if(!shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) new AlertDialog.Builder(this)
                    .setMessage("Cho phép camera trong cài đặt ứng dụng để chụp căn cước.")
                    .setNegativeButton("Đóng",null).setPositiveButton("Mở cài đặt",(d,w)->
                            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+getPackageName())))).show();
        }
    });
    @Override protected void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_verification);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.verificationScreenRoot),(v,insets)->{
            androidx.core.graphics.Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    |WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());
            v.setPadding(safe.left,safe.top,safe.right,safe.bottom);
            if(vm!=null && navigation!=null)render();return insets;
        });
        ViewCompat.requestApplyInsets(findViewById(R.id.verificationScreenRoot));
        findViewById(R.id.verificationScroll).addOnLayoutChangeListener((scroll,l,t,r,b,ol,ot,or,ob)->{
            int available=scroll.getWidth()-scroll.getPaddingLeft()-scroll.getPaddingRight();
            if(available<=0)return;
            View root=findViewById(R.id.verificationRoot);
            android.widget.FrameLayout.LayoutParams params=(android.widget.FrameLayout.LayoutParams)root.getLayoutParams();
            int width=Math.min(available,getResources().getDimensionPixelSize(R.dimen.content_max_width));
            if(params.width!=width){params.width=width;params.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;root.setLayoutParams(params);}
        });
        vm=new ViewModelProvider(this).get(VerificationViewModel.class);
        if(saved==null || !vm.loaded)vm.openCurrentOnLoad=getIntent().getBooleanExtra(OPEN_CURRENT,false);
        navigation=new RoleNavigationBar(this,this::navigate);
        lastImageEvent=saved==null?vm.imageEvent:saved.getLong("verification_image_event",vm.imageEvent);
        if(vm.imageEvent==0)lastImageEvent=0;
        findViewById(R.id.verificationRoot).setSaveFromParentEnabled(false);
        File dir=new File(getCacheDir(),"verification-capture");
        File[] stale=dir.listFiles();
        if(stale!=null)for(File f:stale)if(!f.equals(vm.capture))f.delete();
        click(R.id.vBack,this::leave);
        click(R.id.vCancelUpload,vm::cancelSending);
        click(R.id.vReload,()->discardThen(()->{ vm.clearDraft(); vm.load(); }));
        click(R.id.vProcess,()->new AlertDialog.Builder(this).setTitle(R.string.v_process)
                .setMessage(R.string.v_explanation).setPositiveButton("Đã hiểu",null).show());
        click(R.id.vCapture,()->{
            if(!vm.beginImageRequest(ImageTarget.IDENTITY))return;
            if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)launchCamera();
            else permission.launch(Manifest.permission.CAMERA);
        });
        click(R.id.vPickCard,()->pick(true)); click(R.id.vAddEvidence,()->pick(false));
        click(R.id.vUseImage,vm::acceptCandidate); click(R.id.vDiscardImage,vm::discardCandidate);
        click(R.id.vClear,()->discardThen(()->{vm.clearDraft();vm.load();}));
        click(R.id.vSend,()->new AlertDialog.Builder(this).setMessage("Gửi bản hồ sơ này để xét duyệt?")
                .setNegativeButton("Quay lại",null).setPositiveButton("Gửi hồ sơ",(d,w)->vm.send()).show());
        ((CheckBox)findViewById(R.id.vConsent)).setOnCheckedChangeListener((b,checked)->{
            if(!rendering){vm.consent=checked;vm.emit();}
        });
        ((RadioGroup)findViewById(R.id.vEvidenceType)).setOnCheckedChangeListener((g,id)->{
            if(!rendering)vm.evidenceType=id==R.id.vBhxh?"BHXH_SIMULATED":"RETIREMENT_CERTIFICATE";
        });
        ((RadioGroup)findViewById(R.id.vQueueType)).setOnCheckedChangeListener((g,id)->{
            if(!rendering && !vm.busy){String role=id==R.id.vQueueEmployer?"EMPLOYER":"SEEKER";
                discardThen(()->{vm.queueRole=role;vm.loadQueue();});render();}
        });
        ((RadioGroup)findViewById(R.id.vLookup)).setOnCheckedChangeListener((g,id)->{
            if(!rendering){vm.lookup=id==R.id.vMatch?"MATCH":id==R.id.vNotMatch?"NOT_MATCH":"INSUFFICIENT";vm.emit();}
        });
        ((EditText)findViewById(R.id.vReason)).addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after) { }
            public void onTextChanged(CharSequence s,int start,int before,int count){if(!rendering){
                vm.reason=s.toString();
                if(!vm.reasonMessage.isEmpty()){vm.reasonMessage=vm.message="";show(R.id.vDecisionMessage,false);}
                ((EditText)findViewById(R.id.vReason)).setError(null);
            }}
            public void afterTextChanged(Editable e) { }
        });
        ((CheckBox)findViewById(R.id.vReviewed)).setOnCheckedChangeListener((b,c)->{if(!rendering){vm.reviewed=c;vm.emit();}});
        click(R.id.vRetryImages,vm::loadImages);
        click(R.id.vFind,()->discardThen(()->vm.find(((EditText)findViewById(R.id.vSearch)).getText().toString().trim())));
        click(R.id.vPrevious,()->{if(vm.selected!=null)discardThen(()->vm.openRecord(vm.selected.text("role"),vm.selected.text("previousRequestId")));});
        click(R.id.vRevoke,()->requestReason("REVOKE"));
        click(R.id.vViewCurrent,()->{if(vm.home!=null && vm.home.request!=null)vm.select(vm.home.request);});
        click(R.id.vUpdateIdentity,()->{
            if(vm.busy || vm.selected==null || vm.home==null)return;
            VerificationRepository.Record target=vm.selected;
            new AlertDialog.Builder(this).setMessage("Hồ sơ mới sẽ được xét duyệt lại sau khi gửi. Tiếp tục?")
                    .setNegativeButton("Quay lại",null).setPositiveButton("Tiếp tục",(d,w)->{
                        if(vm.selected==target && !vm.busy)vm.closeReview();
                    }).show();
        });
        click(R.id.vApprove,()->{if(vm.selected!=null)decision("SEEKER".equals(vm.selected.text("role"))?"APPROVED":"VERIFIED");});
        click(R.id.vNeedsInfo,()->requestReason("NEEDS_INFO"));click(R.id.vReject,()->requestReason("REJECTED"));
        click(R.id.vReasonConfirm,()->{if(!vm.reasonAction.isEmpty())decision(vm.reasonAction);});
        click(R.id.vReasonCancel,()->{hideKeyboard();vm.cancelReason();});
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){
            @Override public void handleOnBackPressed(){leave();}
        });
        vm.changes.observe(this,n->render());
        if(saved!=null){
            int y=saved.getInt("verification_scroll_y",0);
            long savedEvent=saved.getLong("verification_image_event",vm.imageEvent);
            findViewById(R.id.verificationScroll).post(()->{
                if(!vm.externalPending && savedEvent==vm.imageEvent)
                    ((ScrollView)findViewById(R.id.verificationScroll)).scrollTo(0,y);
            });
        }
        if(!vm.loaded && !vm.busy)vm.load();
    }
    private void pick(boolean identity) {
        ImageTarget target=identity?ImageTarget.IDENTITY:ImageTarget.EVIDENCE;
        if(!vm.beginImageRequest(target))return;
        try{(identity?identityPicker:evidencePicker).launch("image/*");}
        catch(RuntimeException ex){vm.cancelImageRequest(target,"Không mở được trình chọn ảnh. Hãy thử lại.");}
    }
    private void launchCamera() {
        if(!vm.externalPending || vm.pendingImageTarget!=ImageTarget.IDENTITY)return;
        try{
            File dir=new File(getCacheDir(),"verification-capture");
            if(!dir.exists() && !dir.mkdirs())throw new IllegalStateException();
            vm.deleteCapture(); vm.capture=File.createTempFile("card-",".jpg",dir);
            camera.launch(FileProvider.getUriForFile(this,getPackageName()+".verification-files",vm.capture));
        }catch(Exception ex){vm.cancelImageRequest(ImageTarget.IDENTITY,"Không mở được camera. Có thể chọn ảnh có sẵn.");}
    }
    @Override protected void onSaveInstanceState(Bundle state){
        state.putInt("verification_scroll_y",findViewById(R.id.verificationScroll).getScrollY());
        state.putLong("verification_image_event",lastImageEvent);
        super.onSaveInstanceState(state);
    }
    private void decision(String status) {
        if(!vm.prepareDecision(status))return;
        VerificationRepository.Record target=vm.selected;
        boolean approve="APPROVED".equals(status) || "VERIFIED".equals(status);
        String prompt=approve?"Chấp thuận hồ sơ này?":"REVOKE".equals(status)?"Thu hồi kết quả và gửi lý do cho chủ hồ sơ?":
                "NEEDS_INFO".equals(status)?"Yêu cầu bổ sung và gửi lý do cho chủ hồ sơ?":"Từ chối hồ sơ và gửi lý do cho chủ hồ sơ?";
        new AlertDialog.Builder(this).setMessage(prompt)
                .setNegativeButton("Quay lại",null).setPositiveButton("Xác nhận",(d,w)->{
                    if(vm.selected!=target || vm.busy)return;
                    hideKeyboard();if("REVOKE".equals(status))vm.revoke();else vm.decide(status);
                }).show();
    }
    private void requestReason(String action) {
        vm.requestReason(action);
        findViewById(R.id.vReasonSection).post(()->{if(!isFinishing() && !isDestroyed() && action.equals(vm.reasonAction))scrollToSection(R.id.vReasonSection);});
    }
    private void hideKeyboard(){
        View focused=getCurrentFocus();if(focused==null)return;
        android.view.inputmethod.InputMethodManager keyboard=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        if(keyboard!=null)keyboard.hideSoftInputFromWindow(focused.getWindowToken(),0);focused.clearFocus();
    }
    @Override protected void onStart(){
        super.onStart();
        if(vm.uid.isEmpty())return;
        userChanges=com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("users").document(vm.uid)
                .addSnapshotListener((doc,error)->{
                    if(error!=null || doc==null || doc.getMetadata().isFromCache() || !doc.exists()
                            || vm.home==null || vm.busy || !vm.loaded || vm.sessionEnded)return;
                    Long version=doc.getLong("version");
                    if(version!=null && version!=vm.home.userVersion){
                        if(vm.dirty() || vm.externalPending){vm.message="Hồ sơ trên máy chủ đã thay đổi. Hãy tải lại trước khi gửi.";vm.emit();}
                        else vm.load();
                    }
                });
    }
    @Override protected void onStop(){
        if(userChanges!=null){userChanges.remove();userChanges=null;}super.onStop();
    }
    private void discardThen(Runnable action) {
        if(vm.busy || vm.externalPending)return;
        boolean reviewDraft=vm.home!=null && "ADMIN".equals(vm.home.role) && vm.selected!=null
                && (!vm.reason.trim().isEmpty() || !vm.lookup.isEmpty() || vm.reviewed);
        if(!vm.dirty() && !reviewDraft){action.run();return;}
        new AlertDialog.Builder(this).setMessage(reviewDraft?"Bỏ lựa chọn xét duyệt chưa lưu?":"Bỏ bản nháp chưa gửi?").setNegativeButton("Tiếp tục sửa",null)
                .setPositiveButton(reviewDraft?"Bỏ lựa chọn":"Bỏ bản nháp",(d,w)->action.run()).show();
    }
    private void leave(){
        if(vm.busy || vm.externalPending){vm.message="Đang xử lý. Cô/chú vui lòng chờ.";vm.emit();return;}
        if(vm.selected!=null){
            if(vm.home!=null && "ADMIN".equals(vm.home.role))discardThen(vm::loadQueue);
            else if(getIntent().getBooleanExtra(OPEN_CURRENT,false))discardThen(this::finish);
            else vm.closeReview();
            return;
        }
        discardThen(this::finish);
    }
    private void navigate(RoleNavigationBar.Action action){
        if(vm.busy || vm.externalPending){vm.message="Đang xử lý. Cô/chú vui lòng chờ.";vm.emit();return;}
        if(action==RoleNavigationBar.Action.PROCESS){
            if(vm.selected!=null)discardThen(vm::loadQueue);else findViewById(R.id.verificationScroll).scrollTo(0,0);
            return;
        }
        String route=action==RoleNavigationBar.Action.HOME?"HOME":action==RoleNavigationBar.Action.ACCOUNT?"ACCOUNT":
                action==RoleNavigationBar.Action.PROFILE?"PROFILE":null;
        if(route==null)return;
        discardThen(()->{setResult(RESULT_OK,new Intent().putExtra(RoleNavigationBar.RETURN_DESTINATION,route));finish();});
    }
    private void click(int id,Runnable action){findViewById(id).setOnClickListener(v->action.run());}
    private void show(int id,boolean visible){findViewById(id).setVisibility(visible?View.VISIBLE:View.GONE);}
    private void set(int id,String value){TextView t=findViewById(id);if(!t.getText().toString().equals(value))t.setText(value);}
    private void enabled(View v,boolean enabled){v.setEnabled(enabled);if(v instanceof ViewGroup)
        for(int i=0;i<((ViewGroup)v).getChildCount();i++)enabled(((ViewGroup)v).getChildAt(i),enabled);}
    private void render() {
        if(vm.sessionEnded){finish();return;}
        rendering=true;
        boolean ready=vm.loaded && vm.home!=null;
        boolean admin=ready && "ADMIN".equals(vm.home.role);
        RoleNavigationBar.topbar(this,ready?vm.home.role:"");
        set(R.id.vPageTitle,getString(admin?(vm.selected!=null?R.string.n_review_title:R.string.n_processing_title):
                vm.selected!=null?R.string.v_review_section:R.string.v_title));
        WindowInsetsCompat insets=ViewCompat.getRootWindowInsets(findViewById(R.id.verificationScreenRoot));
        boolean keyboard=insets!=null && insets.isVisible(WindowInsetsCompat.Type.ime());
        navigation.render(ready?vm.home.role:"",admin?RoleNavigationBar.Action.PROCESS:
                ready && "SEEKER".equals(vm.home.role)?RoleNavigationBar.Action.PROFILE:RoleNavigationBar.Action.ACCOUNT,
                ready && !keyboard,!vm.busy && !vm.externalPending);
        boolean decisionScreen=admin && vm.selected!=null && Arrays.asList("SUBMITTED","APPROVED","VERIFIED").contains(vm.selected.text("status"));
        boolean showReload=!ready || vm.readRequired || !vm.message.isEmpty() || (admin && vm.selected==null);
        set(R.id.vMessage,vm.message); show(R.id.vMessage,!vm.message.isEmpty());
        show(R.id.vCancelUpload,vm.sending);
        show(R.id.vReload,showReload && !vm.sending);
        show(R.id.vFeedbackSection,!decisionScreen && (!vm.message.isEmpty() || showReload || vm.sending));
        findViewById(R.id.vReload).setEnabled(!vm.busy && !vm.externalPending);
        show(R.id.vOwner,ready && !admin && vm.selected==null); show(R.id.vAdmin,admin && vm.selected==null);
        if(ready && !admin){
            set(R.id.vCurrent,statusLabel(vm.home.status)+(vm.home.request==null || !vm.home.status.equals(vm.home.request.text("status"))?"":
                    reasonSuffix(vm.home.status,vm.home.request.text("reason"))));
            show(R.id.vViewCurrent,vm.home.request!=null);
            boolean submitted="SUBMITTED".equals(vm.home.status);
            show(R.id.vDraft,!submitted);
            boolean editable=!vm.busy && !vm.externalPending && vm.home.active;
            enabled(findViewById(R.id.vDraft),editable);
            if(vm.frozen()){
                for(int id:new int[]{R.id.vCapture,R.id.vPickCard,R.id.vAddEvidence,R.id.vEvidenceType,R.id.vConsent})enabled(findViewById(id),false);
            }
            boolean seeker="SEEKER".equals(vm.home.role);
            show(R.id.vEvidenceSection,seeker);
            set(R.id.vSubmitHeading,getString(seeker?R.string.v_submit_section:R.string.v_submit_employer_section));
            ((RadioGroup)findViewById(R.id.vEvidenceType)).check("BHXH_SIMULATED".equals(vm.evidenceType)?R.id.vBhxh:R.id.vRetirement);
            ((CheckBox)findViewById(R.id.vConsent)).setChecked(vm.consent);
            set(R.id.vIdentity,vm.identity==null?"":vm.identity.fullName+"\nNgày sinh: "+vm.identity.dateOfBirth
                    +"\nSố căn cước: ••••••••"+vm.identity.documentNumber.substring(8));
            show(R.id.vIdentity,vm.identity!=null && vm.candidate==null);
            set(R.id.vIdentityMessage,vm.identityMessage);show(R.id.vIdentityMessage,!vm.identityMessage.isEmpty());
            set(R.id.vEvidenceMessage,vm.evidenceMessage);show(R.id.vEvidenceMessage,!vm.evidenceMessage.isEmpty());
            set(R.id.vEvidenceCount,getString(R.string.v_evidence_count,vm.evidence.size()));
            findViewById(R.id.vSend).setEnabled(editable && !vm.readRequired && vm.home.phone
                    && vm.home.complete && vm.identity!=null && vm.consent && vm.candidate==null
                    && (!seeker||!vm.evidence.isEmpty()));
            String requirement=!vm.home.phone?"Thêm số điện thoại đã xác minh trước khi gửi.":
                    !vm.home.complete?"Hoàn thiện và lưu thông tin hồ sơ trước khi gửi.":"";
            set(R.id.vSubmitMessage,requirement);show(R.id.vSubmitMessage,!requirement.isEmpty());
            show(R.id.vClear,vm.dirty() || vm.frozen());
            findViewById(R.id.vAddEvidence).setEnabled(editable && !vm.frozen() && vm.evidence.size()<3);
            show(R.id.vCandidate,vm.candidate!=null);
            if(renderedCandidate!=vm.candidate){renderedCandidate=vm.candidate;image((ImageView)findViewById(R.id.vCandidateImage),vm.candidate);}
            if(vm.candidate!=null)findViewById(R.id.vCandidateImage).setOnClickListener(v->zoom(vm.candidate));
            show(R.id.vIdentityImages,vm.candidate==null && vm.identityImage!=null);
            Object identityKey=Arrays.asList(vm.identityImage);
            if(!identityKey.equals(renderedIdentityKey)){
                renderedIdentityKey=identityKey;LinearLayout box=findViewById(R.id.vIdentityImages);box.removeAllViews();
                if(vm.identityImage!=null)addImage(box,vm.identityImage,"Ảnh căn cước",null);
            }
            Object evidenceKey=Arrays.asList(new ArrayList<>(vm.evidence),vm.frozen());
            if(!evidenceKey.equals(renderedEvidenceKey)){
                renderedEvidenceKey=evidenceKey;LinearLayout box=findViewById(R.id.vEvidenceImages);box.removeAllViews();int number=0;
                for(byte[] bytes:vm.evidence){
                    addImage(box,bytes,"Bằng chứng "+(++number),vm.frozen()?null:()->vm.removeEvidence(bytes));
                }
            }
            enabled(findViewById(R.id.vIdentityImages),editable);
            enabled(findViewById(R.id.vEvidenceImages),editable);
            show(R.id.vNoticeSection,!vm.notifications.isEmpty());
            if(renderedNoticeKey!=vm.notifications){
                renderedNoticeKey=vm.notifications; LinearLayout box=findViewById(R.id.vNotices);box.removeAllViews();
                if(vm.notifications.isEmpty())label(box,"Chưa có kết quả.");
                for(Map<String,Object> n:vm.notifications){
                    label(box,statusLabel(String.valueOf(n.get("status")))+reasonSuffix(String.valueOf(n.get("status")),String.valueOf(n.get("reason"))));
                    button(box,"Xem hồ sơ",()->{
                        if(vm.busy)return;
                        vm.openRecord(vm.home.role,String.valueOf(n.get("requestId")));
                    });
                }
            }
        }
        if(admin){
            findViewById(R.id.vFind).setEnabled(!vm.busy);
            enabled(findViewById(R.id.vQueueType),!vm.busy);
            ((RadioGroup)findViewById(R.id.vQueueType)).check("SEEKER".equals(vm.queueRole)?R.id.vQueueSeeker:R.id.vQueueEmployer);
            if(renderedQueueKey!=vm.queue){
                renderedQueueKey=vm.queue;LinearLayout box=findViewById(R.id.vQueue);box.removeAllViews();
                for(VerificationRepository.Record r:vm.queue)button(box,String.valueOf(r.identity().get("fullName"))+" · "+r.id.substring(0,8),()->discardThen(()->vm.select(r)));
                if(vm.queue.size()==50)label(box,"Hiển thị tối đa 50 hồ sơ. Duyệt xong rồi tải lại để xem tiếp.");
            }
        }
        show(R.id.vReview,vm.selected!=null);
        if(vm.selected!=null){
            VerificationRepository.Record r=vm.selected;
            Map<String,Object> id=r.identity();
            set(R.id.vReviewIdentity,String.valueOf(id.get("fullName"))+"\nNgày sinh: "+id.get("dateOfBirth")
                    +"\nSố căn cước: "+id.get("documentNumber")+"\n"+statusLabel(r.text("status"))
                    +reasonSuffix(r.text("status"),r.text("reason"))+"\n"+evidenceLabel(r.text("evidenceType"))+professionalSummary(r)+"\nMã hồ sơ: "+r.id);
            show(R.id.vPrevious,!r.text("previousRequestId").isEmpty());
            findViewById(R.id.vPrevious).setEnabled(!vm.busy);
            Object key=Arrays.asList(r.id,vm.reviewImages.size());
            if(!key.equals(renderedReviewKey)){
                renderedReviewKey=key;LinearLayout box=findViewById(R.id.vReviewImages);box.removeAllViews();int i=0;
                for(byte[] bytes:vm.reviewImages.values())addImage(box,bytes,i++==0?"Ảnh căn cước":"Ảnh bằng chứng",null);
            }
            show(R.id.vRetryImages,vm.reviewImages.size()!=r.paths().size());
            findViewById(R.id.vRetryImages).setEnabled(!vm.busy);
            show(R.id.vUpdateIdentity,!admin && vm.home.active && getIntent().getBooleanExtra(OPEN_CURRENT,false)
                    && r.id.equals(vm.home.currentId) && Arrays.asList("APPROVED","VERIFIED").contains(vm.home.status)
                    && vm.home.status.equals(r.text("status")));
            findViewById(R.id.vUpdateIdentity).setEnabled(!vm.busy);
            boolean canDecide=admin && "SUBMITTED".equals(r.text("status"));
            boolean canRevoke=admin && Arrays.asList("APPROVED","VERIFIED").contains(r.text("status"));
            show(R.id.vDecision,canDecide || canRevoke);show(R.id.vLookup,canDecide);
            show(R.id.vRevoke,canRevoke);
            enabled(findViewById(R.id.vDecision),!vm.busy && vm.reviewImages.size()==r.paths().size());
            set(R.id.vReason,vm.reason);
            ((EditText)findViewById(R.id.vReason)).setError(vm.reasonMessage.isEmpty()?null:vm.reasonMessage);
            show(R.id.vReasonSection,!vm.reasonAction.isEmpty() && (canDecide || canRevoke));
            set(R.id.vReasonConfirm,getString("REVOKE".equals(vm.reasonAction)?R.string.v_revoke:
                    "REJECTED".equals(vm.reasonAction)?R.string.v_reject:R.string.v_needs_info));
            set(R.id.vDecisionMessage,vm.message);show(R.id.vDecisionMessage,!vm.message.isEmpty());show(R.id.vDecisionProgress,vm.busy);
            ((RadioGroup)findViewById(R.id.vLookup)).check("MATCH".equals(vm.lookup)?R.id.vMatch:"NOT_MATCH".equals(vm.lookup)?R.id.vNotMatch:"INSUFFICIENT".equals(vm.lookup)?R.id.vInsufficient:View.NO_ID);
            ((CheckBox)findViewById(R.id.vReviewed)).setChecked(vm.reviewed);
            for(int button:new int[]{R.id.vApprove,R.id.vNeedsInfo,R.id.vReject}){
                show(button,canDecide);findViewById(button).setEnabled(!vm.busy && vm.reviewed && vm.reviewImages.size()==r.paths().size()
                        && !vm.lookup.isEmpty() && (button!=R.id.vApprove || "MATCH".equals(vm.lookup)));
            }
            findViewById(R.id.vRevoke).setEnabled(!vm.busy && vm.reviewed && vm.reviewImages.size()==r.paths().size());
            findViewById(R.id.vReasonConfirm).setEnabled(!vm.busy && vm.reviewed && vm.reviewImages.size()==r.paths().size()
                    && ("REVOKE".equals(vm.reasonAction) || !vm.lookup.isEmpty()));
        }
        rendering=false;
        if(vm.imageEvent!=lastImageEvent && vm.imageFocus!=null){
            lastImageEvent=vm.imageEvent;long event=lastImageEvent;
            int anchor=vm.imageFocus==ImageTarget.IDENTITY?R.id.vIdentitySection:R.id.vEvidenceSection;
            findViewById(anchor).post(()->{
                if(isFinishing() || isDestroyed() || vm.imageEvent!=event)return;
                scrollToSection(anchor);
            });
        }
    }
    private void scrollToSection(int id){
        ScrollView scroll=findViewById(R.id.verificationScroll);View section=findViewById(id);
        if(!section.isShown())return;
        int y=section.getTop();android.view.ViewParent parent=section.getParent();
        while(parent instanceof View && parent!=scroll){y+=((View)parent).getTop();parent=parent.getParent();}
        scroll.smoothScrollTo(0,Math.max(0,y-dp(8)));
    }
    private String evidenceLabel(String type){return "BHXH_SIMULATED".equals(type)?"BHXH mô phỏng":
            "RETIREMENT_CERTIFICATE".equals(type)?"Giấy chứng nhận nghỉ hưu":"Hồ sơ danh tính mẫu";}
    private static String reasonSuffix(String status,String reason){
        if(reason==null || reason.trim().isEmpty() || (Arrays.asList("APPROVED","VERIFIED").contains(status)
                && VerificationRepository.APPROVAL_REASON.equals(reason)))return "";
        return "\n"+reason;
    }
    public static String statusLabel(String status){
        switch(status){case "SUBMITTED":return "Đang chờ duyệt";case "APPROVED":return "Hồ sơ nghỉ hưu đã được duyệt";
            case "VERIFIED":return "Hồ sơ danh tính đã được duyệt";case "NEEDS_INFO":return "Hồ sơ cần bổ sung";
            case "REJECTED":return "Hồ sơ chưa được chấp thuận";case "REVERIFY_REQUIRED":return "Cần xét duyệt lại";
            default:return "Chưa gửi hồ sơ";}
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void label(LinearLayout box,String value){TextView t=new TextView(this);t.setTextAppearance(R.style.TextAppearance_GrandMa_Body);t.setText(value);t.setPadding(0,0,0,0);t.setSaveEnabled(false);
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(12);box.addView(t,params);}
    private void button(LinearLayout box,String text,Runnable action){
        AppCompatButton b=new AppCompatButton(new androidx.appcompat.view.ContextThemeWrapper(this,R.style.Widget_GrandMa_Button_Secondary),null,0);
        b.setText(text);b.setSaveEnabled(false);b.setMinHeight(dp(56));b.setOnClickListener(v->action.run());
        LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(12);box.addView(b,params);
    }
    private void addImage(LinearLayout box,byte[] bytes,String description,Runnable remove){
        LinearLayout group=new LinearLayout(this);group.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams groupParams=new LinearLayout.LayoutParams(-1,-2);groupParams.topMargin=dp(16);box.addView(group,groupParams);
        label(group,description); ImageView v=new ImageView(this);v.setScaleType(ImageView.ScaleType.FIT_CENTER);
        v.setContentDescription(description+". Chạm để phóng to.");v.setSaveEnabled(false);image(v,bytes);
        LinearLayout.LayoutParams imageParams=new LinearLayout.LayoutParams(-1,dp(200));imageParams.topMargin=dp(12);group.addView(v,imageParams);
        v.setOnClickListener(w->zoom(bytes));if(remove!=null)button(group,"Bỏ ảnh này",remove);
    }
    private void image(ImageView view,byte[] bytes){view.setImageBitmap(vn.edu.ueh.nhom4.grandmaassist.app.data.repository.VerificationImages.preview(bytes,1200));}
    private String professionalSummary(VerificationRepository.Record r){
        Object value=r.data.get("professionalSnapshot");if(!(value instanceof Map))return "";
        Map<?,?> p=(Map<?,?>)value;
        if("SEEKER".equals(r.text("role")))return "\nKhu vực: "+p.get("areaCode")+"\nKỹ năng: "+p.get("skills")
                +"\nKinh nghiệm: "+p.get("experienceSummary")+"\nCa: "+String.valueOf(p.get("preferredShifts"))
                    .replace("MORNING","Sáng").replace("AFTERNOON","Chiều").replace("EVENING","Tối");
        return "\nKhu vực: "+p.get("areaCode")+("ORGANIZATION".equals(p.get("employerType"))?"\nTổ chức: "+p.get("organizationName"):"\nNgười thuê cá nhân")
                +"\nGiới thiệu: "+p.get("introduction");
    }
    private void zoom(byte[] bytes){
        if(bytes==null)return;
        ZoomDocumentView image=new ZoomDocumentView(this,bytes);
        AlertDialog dialog=new AlertDialog.Builder(this).setView(image).setPositiveButton("Đóng",null).create();
        dialog.setOnShowListener(d->{if(dialog.getWindow()!=null){dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            dialog.getWindow().setLayout(-1,(int)(getResources().getDisplayMetrics().heightPixels*.85f));}});dialog.show();
    }
}
