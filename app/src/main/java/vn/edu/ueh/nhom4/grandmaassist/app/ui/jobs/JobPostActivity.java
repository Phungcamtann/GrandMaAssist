package vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs;

import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.*;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.*;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.*;
import androidx.lifecycle.ViewModelProvider;
import java.text.*;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.*;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.applications.JobApplicantsActivity;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.sessions.ShiftAgreementDetailActivity;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.JobPostViewModel;

public final class JobPostActivity extends AppCompatActivity {
    public static final String POST_ID="post_id";
    private JobPostViewModel vm;private RoleNavigationBar nav;
    private Object bodyKey;private long lastPage;private boolean rendering;
    private final androidx.activity.result.ActivityResultLauncher<Intent> editor=registerForActivityResult(
        new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),r->{
            if(vm==null||vm.ended)return;
            if(r.getData()!=null&&r.getData().hasExtra(RoleNavigationBar.RETURN_DESTINATION)){setResult(RESULT_OK,r.getData());finish();return;}
            if(r.getData()!=null){String notice=r.getData().getStringExtra("submission_notice");if(notice!=null)vm.pendingNotice=notice;}
            if(vm.selected!=null)vm.open(vm.selected.content.id);else vm.load(false);
        });
    @Override protected void onCreate(Bundle state){super.onCreate(state);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_job_posts);findViewById(R.id.jpContent).setSaveFromParentEnabled(false);
        vm=new ViewModelProvider(this).get(JobPostViewModel.class);lastPage=vm.pageEvent;
        nav=new RoleNavigationBar(this,a->{if(vm.busy)return;
            if(a==RoleNavigationBar.Action.WORK||a==RoleNavigationBar.Action.PROCESS){if(vm.selected!=null)guard(vm::backToList);return;}
            if(a==RoleNavigationBar.Action.OTHER){guard(()->vn.edu.ueh.nhom4.grandmaassist.app.ui.sessions.WorkSessionsActivity.start(this,"EMPLOYER"));return;}
            if(a==RoleNavigationBar.Action.HOME||a==RoleNavigationBar.Action.ACCOUNT||a==RoleNavigationBar.Action.PROFILE)guard(()->leave(a.name()));});
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.jpRoot),(v,i)->{
            androidx.core.graphics.Insets safe=i.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());
            v.setPadding(safe.left,safe.top,safe.right,safe.bottom);render();return i;});
        findViewById(R.id.jpScroll).addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int width=Math.min(v.getWidth(),getResources().getDimensionPixelSize(R.dimen.content_max_width));if(width<=0)return;
            View content=findViewById(R.id.jpContent);FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)content.getLayoutParams();
            if(p.width!=width){p.width=width;p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;content.setLayoutParams(p);}
        });
        click(R.id.jpBack,this::back);
        click(R.id.jpCreate, () -> editor.launch(new Intent(this, JobDraftActivity.class).putExtra(JobDraftActivity.CREATE_NEW, true)));
        click(R.id.jpDrafts,()->guard(()->leave("DRAFTS")));
        click(R.id.jpProfiles,()->guard(()->leave("PROFILES")));
        click(R.id.jpReload,()->guard(()->{if(!vm.ready)vm.authorize(getIntent().getStringExtra(POST_ID)==null?"":getIntent().getStringExtra(POST_ID));else if(vm.selected!=null)vm.open(vm.selected.content.id);else vm.load(false);}));
        click(R.id.jpMore,()->vm.load(true));
        click(R.id.jpApprove,()->{if(vm.selected==null||vm.busy)return;JobPost expected=vm.selected;
            new AlertDialog.Builder(this).setMessage("Chấp thuận và công khai tin cùng địa chỉ làm việc?")
                .setNegativeButton("Quay lại",null).setPositiveButton("Chấp thuận",(d,w)->{if(vm.selected==expected)vm.decide(true);}).show();});
        click(R.id.jpReject, () -> showReasonInput("REJECT", "Lý do từ chối"));
        click(R.id.jpHide, () -> showReasonInput("HIDE", "Lý do ẩn tin"));
        click(R.id.jpConfirmAction, () -> { if ("HIDE".equals(vm.reasonAction)) vm.hidePost(); else vm.decide(false); });
        click(R.id.jpCancelReject,()->{vm.reasonVisible=false;vm.reason="";vm.message="";vm.emit();});
        click(R.id.jpViewSession, () -> {
            if (vm.selected == null || vm.busy) return;
            WorkSession session = vm.jobSessions.get(vm.selected.content.id);
            if (session != null) {
                ShiftAgreementDetailActivity.start(this, session.id);
            }
        });
        click(R.id.jpViewApplicants, () -> {
            if (vm.selected == null || vm.busy) return;
            openApplicants(vm.selected.content.id, vm.selected.content.title);
        });
        click(R.id.jpClose, () -> {
            if (vm.selected == null || vm.busy) return;
            new AlertDialog.Builder(this)
                .setTitle("Đóng tin tuyển dụng")
                .setMessage("Cô/chú có chắc muốn đóng tin này không? Sau khi đóng, người tìm việc sẽ không thể gửi đơn ứng tuyển mới.")
                .setNegativeButton("Giữ lại", null)
                .setPositiveButton("Đóng tin", (d, w) -> vm.closePost())
                .show();
        });
        click(R.id.jpEdit, () -> {
            if (vm.selected == null || vm.busy) return;
            String id = vm.selected.content.id;
            boolean isOpen = "OPEN".equals(vm.selected.status);
            String msg = isOpen 
                ? "Sửa tin đang mở? Sau khi lưu và gửi lại, tin sẽ quay về trạng thái Chờ duyệt để Admin kiểm tra trước khi công khai lại."
                : "Mở nội dung tin bị từ chối để sửa? Nháp chưa gửi có cùng ID trên máy sẽ được thay khi cô/chú lưu.";
            new AlertDialog.Builder(this).setMessage(msg)
                .setNegativeButton("Quay lại", null)
                .setPositiveButton("Sửa tin", (d, w) -> editor.launch(new Intent(this, JobDraftActivity.class).putExtra(JobDraftActivity.EDIT_POST, id)))
                .show();
        });
        ((EditText)findViewById(R.id.jpReason)).addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int a,int c,int f){}public void afterTextChanged(Editable e){}
            public void onTextChanged(CharSequence s,int a,int b,int c){if(!rendering&&!vm.busy)vm.reason=s.toString();}});
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){public void handleOnBackPressed(){back();}});
        vm.changes.observe(this,n->render());String id=getIntent().getStringExtra(POST_ID);vm.initialize(id==null?"":id,getIntent().getStringExtra("submission_notice"));ViewCompat.requestApplyInsets(findViewById(R.id.jpRoot));
        if(state!=null){int y=state.getInt("scroll",0);findViewById(R.id.jpScroll).post(()->findViewById(R.id.jpScroll).scrollTo(0,y));}
    }
    @Override protected void onSaveInstanceState(Bundle b){b.putInt("scroll",findViewById(R.id.jpScroll).getScrollY());super.onSaveInstanceState(b);}
    private void showReasonInput(String action, String title) {
        vm.reasonAction = action; vm.reasonVisible = true; vm.message = ""; vm.emit();
        set(R.id.jpReasonTitle, title);
        set(R.id.jpConfirmAction, "Xác nhận " + ("HIDE".equals(action) ? "ẩn tin" : "từ chối"));
        findViewById(R.id.jpReasonSection).post(() -> findViewById(R.id.jpReasonSection).requestRectangleOnScreen(new android.graphics.Rect(0, 0, 1, findViewById(R.id.jpReasonSection).getHeight()), false));
    }
    private void leave(String route){setResult(RESULT_OK,new Intent().putExtra(RoleNavigationBar.RETURN_DESTINATION,route));finish();}
    private void guard(Runnable action){if(vm.busy)return;if(vm.reasonVisible&&!vm.reason.trim().isEmpty())new AlertDialog.Builder(this).setMessage("Bỏ lý do đang nhập?").setNegativeButton("Tiếp tục nhập",null).setPositiveButton("Bỏ",(d,w)->{vm.reason="";vm.reasonVisible=false;action.run();}).show();else action.run();}
    private void back(){if(vm.busy)return;guard(()->{if(vm.selected!=null)vm.backToList();else finish();});}
    private void render(){if(vm==null)return;if(vm.ended){finish();return;}rendering=true;
        RoleNavigationBar.topbar(this,vm.ready?(vm.admin?"ADMIN":"EMPLOYER"):"");
        WindowInsetsCompat insets=ViewCompat.getRootWindowInsets(findViewById(R.id.jpRoot));boolean keyboard=insets!=null&&insets.isVisible(WindowInsetsCompat.Type.ime());
        nav.render(vm.admin?"ADMIN":"EMPLOYER",vm.admin?RoleNavigationBar.Action.PROCESS:RoleNavigationBar.Action.WORK,vm.ready&&!keyboard,!vm.busy);
        set(R.id.jpTitle, vm.admin ? "Duyệt tin tuyển" : (vm.selected == null ? "Tin của tôi" : "Chi tiết tin tuyển"));
        show(R.id.jpProgress,vm.busy);set(R.id.jpMessage,vm.message);show(R.id.jpMessage,!vm.message.isEmpty());
        show(R.id.jpEmployerActions, vm.ready && !vm.admin && vm.selected == null);
        show(R.id.jpProfiles, vm.ready && vm.admin && vm.selected == null);
        Object key=vm.selected==null?vm.items:vm.selected;
        if(bodyKey!=key){bodyKey=key;LinearLayout body=findViewById(R.id.jpBody);body.removeAllViews();
            if(vm.selected==null){
                for(JobPost p:vm.items){
                    LinearLayout c=card(body);
                    if("CLOSED".equals(p.status))c.setAlpha(0.88f);

                    LinearLayout header=new LinearLayout(this);
                    header.setOrientation(LinearLayout.HORIZONTAL);
                    header.setGravity(Gravity.CENTER_VERTICAL);
                    LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);
                    hp.topMargin=dp(4);

                    TextView title=new TextView(this);
                    title.setTextAppearance(R.style.TextAppearance_GrandMa_Label);
                    title.setText(p.content.title);
                    title.setSaveEnabled(false);
                    ViewCompat.setAccessibilityHeading(title,true);
                    if("CLOSED".equals(p.status))title.setTextColor(Color.parseColor("#616161"));
                    header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));

                    header.addView(statusBadge(p.status));
                    c.addView(header,hp);

                    text(c,p.areaName+"\n"+money(p.content),false);
                    button(c,"Xem tin",()->vm.open(p.content.id));
                    WorkSession itemSession = vm.jobSessions.get(p.content.id);
                    if (!vm.admin && ("OPEN".equals(p.status) || "CLOSED".equals(p.status))) {
                        if (itemSession != null) {
                            button(c, "Xem thỏa thuận ca", () -> ShiftAgreementDetailActivity.start(this, itemSession.id));
                        } else {
                            button(c, "Xem danh sách ứng viên", () -> openApplicants(p.content.id, p.content.title));
                        }
                    }
                }
            }else{
                JobPost p=vm.selected;LocalJobDraft d=p.content;
                int ill = illustration(d.categoryId);
                if (ill != 0) {
                    com.google.android.material.card.MaterialCardView cardImg = new com.google.android.material.card.MaterialCardView(this);
                    cardImg.setRadius(dp(16));
                    cardImg.setCardElevation(dp(2));
                    cardImg.setStrokeWidth(0);
                    ImageView iv = new ImageView(this);
                    iv.setImageResource(ill);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    cardImg.addView(iv, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(180)));
                    LinearLayout.LayoutParams lpImg = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    lpImg.bottomMargin = dp(16);
                    body.addView(cardImg, lpImg);
                }

                LinearLayout c=card(body);
                LinearLayout header=new LinearLayout(this);
                header.setOrientation(LinearLayout.HORIZONTAL);
                header.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams hp=new LinearLayout.LayoutParams(-1,-2);
                hp.topMargin=dp(4);

                TextView title=new TextView(this);
                title.setText(d.title);
                title.setTextSize(22);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
                title.setTextColor(Color.parseColor("#1C6758"));
                title.setSaveEnabled(false);
                ViewCompat.setAccessibilityHeading(title,true);
                header.addView(title,new LinearLayout.LayoutParams(0,-2,1f));

                header.addView(statusBadge(p.status));
                c.addView(header,hp);

                TextView tvSub = labelBody(c, p.employerName + " · " + p.categoryName);
                tvSub.setTextColor(Color.parseColor("#747775"));

                TextView tvPay = labelHeading(c, "Tiền công: " + money(d));
                tvPay.setTextSize(18);
                tvPay.setTextColor(Color.parseColor("#2E7D32"));

                if(!p.reason.isEmpty()){
                    addDetailDivider(c);
                    labelHeading(c, "HIDDEN".equals(p.status) ? "Lý do ẩn tin" : "Lý do từ chối");
                    labelBody(c, p.reason);
                }

                addDetailDivider(c);
                labelHeading(c, "Địa chỉ làm việc");
                String addrText = (d.workAddress != null && !d.workAddress.trim().isEmpty() ? d.workAddress.trim() + "\n" : "") + "Khu vực: " + p.areaName;
                labelBody(c, addrText);

                addDetailDivider(c);
                labelHeading(c, "Thời gian làm việc");
                String schedText = labels(LocalJobDraft.DAYS, getResources().getStringArray(R.array.jd_days), d.days) + "\n" +
                        label(LocalJobDraft.SHIFTS, getResources().getStringArray(R.array.jd_shifts), d.shift) + " · " + d.schedule;
                labelBody(c, schedText);

                addDetailDivider(c);
                labelHeading(c, "Công việc cần làm");
                labelBody(c, d.description);

                addDetailDivider(c);
                labelHeading(c, "Yêu cầu");
                StringBuilder reqSb = new StringBuilder();
                if (!d.requirements.isEmpty()) reqSb.append(d.requirements);
                if (!d.skills().isEmpty()) {
                    if (reqSb.length() > 0) reqSb.append("\n");
                    reqSb.append("Kỹ năng: ").append(String.join(", ", d.skills()));
                }
                if (!d.demands.isEmpty()) {
                    if (reqSb.length() > 0) reqSb.append("\n");
                    reqSb.append("Thể lực: ").append(labels(LocalJobDraft.DEMANDS, new String[]{getString(R.string.jd_lifting), getString(R.string.jd_standing), getString(R.string.jd_moving)}, d.demands));
                }
                labelBody(c, reqSb.length() > 0 ? reqSb.toString() : "Không có yêu cầu đặc biệt");

                addDetailDivider(c);
                labelHeading(c, "Hạn nhận hồ sơ");
                labelBody(c, DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, new Locale("vi", "VN")).format(new Date(d.expiresAt)) + "\nPhiên bản tin: " + p.version);
            }
        }
        boolean isOwner = vm.selected != null && !vm.admin && vm.uid.equals(vm.selected.content.ownerUid);
        boolean isOpen = vm.selected != null && "OPEN".equals(vm.selected.status);
        boolean isClosed = vm.selected != null && "CLOSED".equals(vm.selected.status);
        boolean isRejected = vm.selected != null && "REJECTED".equals(vm.selected.status);
        boolean pending = vm.selected != null && "PENDING_REVIEW".equals(vm.selected.status) && vm.admin && !vm.uid.equals(vm.selected.content.ownerUid);
        boolean canHide = vm.selected != null && vm.admin && (isOpen || isClosed) && !vm.uid.equals(vm.selected.content.ownerUid);

        show(R.id.jpApprove, pending && !vm.reasonVisible);
        show(R.id.jpReject, pending && !vm.reasonVisible);
        show(R.id.jpHide, canHide && !vm.reasonVisible);
        show(R.id.jpReasonSection, vm.reasonVisible);
        WorkSession curSession = vm.selected != null ? vm.jobSessions.get(vm.selected.content.id) : null;
        show(R.id.jpViewSession, isOwner && curSession != null && !vm.reasonVisible);
        show(R.id.jpViewApplicants, isOwner && (isOpen || isClosed) && curSession == null && !vm.reasonVisible);
        show(R.id.jpEdit, isOwner && (isRejected || isOpen) && !vm.reasonVisible);
        show(R.id.jpClose, isOwner && isOpen && !vm.reasonVisible);
        show(R.id.jpMore, vm.ready && vm.selected == null && vm.more);

        if (isOwner && isOpen) {
            set(R.id.jpEdit, "Sửa tin tuyển dụng");
        } else if (isOwner && isRejected) {
            set(R.id.jpEdit, "Sửa và gửi lại");
        }

        set(R.id.jpReason,vm.reason);for(int id:new int[]{R.id.jpReload,R.id.jpMore,R.id.jpApprove,R.id.jpReject,R.id.jpHide,R.id.jpConfirmAction,R.id.jpCancelReject,R.id.jpReason,R.id.jpViewSession,R.id.jpViewApplicants,R.id.jpEdit,R.id.jpClose,R.id.jpDrafts,R.id.jpCreate,R.id.jpProfiles,R.id.jpBack})findViewById(id).setEnabled(!vm.busy);
        enabled(findViewById(R.id.jpBody),!vm.busy);rendering=false;
        if(lastPage!=vm.pageEvent){lastPage=vm.pageEvent;findViewById(R.id.jpScroll).post(()->findViewById(R.id.jpScroll).scrollTo(0,0));}
    }
        private int illustration(String categoryId){
        if (categoryId == null) return 0;
        switch(categoryId){
            case "GARDEN_CARE":return R.drawable.job_illustration_garden;
            case "KITCHEN_HELP":return R.drawable.job_illustration_kitchen;
            case "LIBRARY_HELP":return R.drawable.job_illustration_library;
            case "SHOP_HELP":return R.drawable.job_illustration_shop;
            default:return 0;
        }
    }
    private void addDetailDivider(LinearLayout parent) {
        View v = new View(this);
        v.setBackgroundColor(Color.parseColor("#E0E4E1"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1));
        lp.topMargin = dp(14);
        lp.bottomMargin = dp(4);
        parent.addView(v, lp);
    }
    private TextView labelHeading(LinearLayout parent, String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextSize(16);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        tv.setTextColor(Color.parseColor("#1C6758"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        parent.addView(tv, lp);
        return tv;
    }
    private TextView labelBody(LinearLayout parent, String body) {
        TextView tv = new TextView(this);
        tv.setText(body);
        tv.setTextSize(15);
        tv.setTextColor(Color.parseColor("#2C3332"));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(4);
        parent.addView(tv, lp);
        return tv;
    }
    private String money(LocalJobDraft d){Long amount=d.payAmount();return (amount==null?d.payInput:NumberFormat.getIntegerInstance(new Locale("vi","VN")).format(amount))+" đ · "+label(LocalJobDraft.UNITS,getResources().getStringArray(R.array.jd_units),d.payUnit);}
    private String label(List<String> ids,String[] labels,String value){int i=ids.indexOf(value);return i>=0?labels[i]:value;}
    private String labels(List<String> ids,String[] labels,List<String> selected){List<String> out=new ArrayList<>();for(String s:selected)out.add(label(ids,labels,s));return String.join(", ",out);}
    private LinearLayout card(LinearLayout parent){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(16),dp(16),dp(16));c.setBackgroundResource(R.drawable.verification_section);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(24);parent.addView(c,p);return c;}
    private void text(LinearLayout parent,String value,boolean heading){TextView t=new TextView(this);t.setTextAppearance(heading?R.style.TextAppearance_GrandMa_Label:R.style.TextAppearance_GrandMa_Body);t.setText(value);t.setSaveEnabled(false);ViewCompat.setAccessibilityHeading(t,heading);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(12);parent.addView(t,p);}
    private void button(LinearLayout p,String title,Runnable action){AppCompatButton b=new AppCompatButton(new androidx.appcompat.view.ContextThemeWrapper(this,R.style.Widget_GrandMa_Button_Secondary),null,0);b.setText(title);b.setSaveEnabled(false);b.setOnClickListener(v->{if(!vm.busy)action.run();});LinearLayout.LayoutParams params=new LinearLayout.LayoutParams(-1,-2);params.topMargin=dp(16);p.addView(b,params);}
    private void enabled(View v,boolean b){v.setEnabled(b);if(v instanceof ViewGroup)for(int i=0;i<((ViewGroup)v).getChildCount();i++)enabled(((ViewGroup)v).getChildAt(i),b);}
    private TextView statusBadge(String status) {
        TextView badge = new TextView(this);
        badge.setTextAppearance(R.style.Widget_GrandMa_PublicBadge);
        badge.setText(JobPost.statusLabel(status));
        badge.setIncludeFontPadding(false);
        badge.setGravity(Gravity.CENTER);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(dp(6));

        switch (status) {
            case "OPEN":
                bg.setColor(Color.parseColor("#E8F5E9"));
                badge.setTextColor(Color.parseColor("#1C6758"));
                break;
            case "CLOSED":
                bg.setColor(Color.parseColor("#EEEEEE"));
                badge.setTextColor(Color.parseColor("#616161"));
                break;
            case "PENDING_REVIEW":
                bg.setColor(Color.parseColor("#E3F2FD"));
                badge.setTextColor(Color.parseColor("#1565C0"));
                break;
            case "REJECTED":
                bg.setColor(Color.parseColor("#FFEBEE"));
                badge.setTextColor(Color.parseColor("#C62828"));
                break;
            case "HIDDEN":
                bg.setColor(Color.parseColor("#ECEFF1"));
                badge.setTextColor(Color.parseColor("#546E7A"));
                break;
            default:
                bg.setColor(Color.parseColor("#F5F5F5"));
                badge.setTextColor(Color.parseColor("#757575"));
                break;
        }
        badge.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = dp(8);
        badge.setLayoutParams(lp);
        return badge;
    }
    private void openApplicants(String jobId, String jobTitle) {
        Intent intent = new Intent(this, JobApplicantsActivity.class);
        intent.putExtra(JobApplicantsActivity.EXTRA_JOB_ID, jobId);
        intent.putExtra(JobApplicantsActivity.EXTRA_JOB_TITLE, jobTitle);
        startActivity(intent);
    }
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void click(int id,Runnable r){findViewById(id).setOnClickListener(v->r.run());}
    private void show(int id,boolean b){findViewById(id).setVisibility(b?View.VISIBLE:View.GONE);}
    private void set(int id,String s){TextView t=findViewById(id);if(!t.getText().toString().equals(s))t.setText(s);}
}
