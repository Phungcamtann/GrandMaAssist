package vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs;

import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.*;
import android.view.*;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.*;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.view.*;
import androidx.lifecycle.ViewModelProvider;
import java.text.*;
import java.util.*;
import java.util.function.Consumer;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.JobDraftStore;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.LocalJobDraft;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.JobDraftViewModel;

/** Local composition and explicit online submission; no writes from rendering. */
public final class JobDraftActivity extends AppCompatActivity {
    public static final String CREATE_NEW="create_new_draft",EDIT_POST="edit_rejected_post";
    private JobDraftViewModel vm;
    private final androidx.activity.result.ActivityResultLauncher<Intent> posts=registerForActivityResult(
        new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),r->{
            if(vm==null||vm.sessionEnded)return;
            if(r.getData()!=null){String route=r.getData().getStringExtra(RoleNavigationBar.RETURN_DESTINATION);
                if(route!=null&&!"DRAFTS".equals(route)){setResult(RESULT_OK,r.getData());finish();return;}}
            vm.loadList();
        });
    private RoleNavigationBar navigation;
    private boolean rendering;
    private Object renderedList;
    private long lastPageEvent,lastValidationEvent;
    private final Map<Integer,List<String>> optionIds=new HashMap<>(),optionLabels=new HashMap<>();
    private final LinkedHashMap<String,Integer> fields=new LinkedHashMap<>();
    private final Map<String,Integer> errorViews=new HashMap<>();
    private final int[] days={R.id.jdMonday,R.id.jdTuesday,R.id.jdWednesday,R.id.jdThursday,R.id.jdFriday,R.id.jdSaturday,R.id.jdSunday};
    private final int[] demands={R.id.jdLifting,R.id.jdStanding,R.id.jdMoving};
    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_job_drafts);
        findViewById(R.id.jdContent).setSaveFromParentEnabled(false);
        vm=new ViewModelProvider(this).get(JobDraftViewModel.class);
        navigation=new RoleNavigationBar(this,a->{
            if(a==RoleNavigationBar.Action.WORK)guard("LIST");
            else if(a==RoleNavigationBar.Action.HOME)guard("HOME");else if(a==RoleNavigationBar.Action.ACCOUNT)guard("ACCOUNT");
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.jdRoot),(v,insets)->{
            androidx.core.graphics.Insets safe=insets.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());
            v.setPadding(safe.left,safe.top,safe.right,safe.bottom);if(vm!=null)render();return insets;
        });
        ViewCompat.requestApplyInsets(findViewById(R.id.jdRoot));
        findViewById(R.id.jdScroll).addOnLayoutChangeListener((scroll,l,t,r,b,ol,ot,or,ob)->{
            int available=scroll.getWidth()-scroll.getPaddingLeft()-scroll.getPaddingRight();if(available<=0)return;
            View content=findViewById(R.id.jdContent);FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)content.getLayoutParams();
            int width=Math.min(available,getResources().getDimensionPixelSize(R.dimen.content_max_width));
            if(p.width!=width){p.width=width;p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;content.setLayoutParams(p);}
        });
        bindText("workAddress",R.id.jdWorkAddress,v->vm.draft.workAddress=v);
        bindText("title",R.id.jdName,v->vm.draft.title=v);bindText("description",R.id.jdDescription,v->vm.draft.description=v);
        bindText("requirements",R.id.jdRequirements,v->vm.draft.requirements=v);bindText("skillsInput",R.id.jdSkills,v->vm.draft.skillsInput=v);
        bindText("schedule",R.id.jdScheduleText,v->vm.draft.schedule=v);bindText("payInput",R.id.jdPay,v->vm.draft.payInput=v);
        bindSpinner("categoryId",R.id.jdCategory,v->vm.draft.categoryId=v);bindSpinner("areaCode",R.id.jdArea,v->vm.draft.areaCode=v);
        bindSpinner("shift",R.id.jdShift,v->vm.draft.shift=v);bindSpinner("payUnit",R.id.jdUnit,v->vm.draft.payUnit=v);
        fields.put("days",R.id.jdMonday);fields.put("demands",R.id.jdDemandMode);fields.put("expiresAt",R.id.jdExpiry);
        errorViews.put("categoryId",R.id.jdCategoryError);errorViews.put("areaCode",R.id.jdAreaError);errorViews.put("days",R.id.jdDaysError);
        errorViews.put("shift",R.id.jdShiftError);errorViews.put("demands",R.id.jdDemandsError);errorViews.put("payUnit",R.id.jdUnitError);errorViews.put("expiresAt",R.id.jdExpiryError);
        for(int i=0;i<days.length;i++){final String key=LocalJobDraft.DAYS.get(i);
            ((CheckBox)findViewById(days[i])).setOnCheckedChangeListener((b,on)->{if(canEdit()){toggle(vm.draft.days,key,on);vm.edited("days");}});}
        for(int i=0;i<demands.length;i++){final String key=LocalJobDraft.DEMANDS.get(i);
            ((CheckBox)findViewById(demands[i])).setOnCheckedChangeListener((b,on)->{if(canEdit()){toggle(vm.draft.demands,key,on);vm.edited("demands");}});}
        ((RadioGroup)findViewById(R.id.jdDemandMode)).setOnCheckedChangeListener((g,id)->{
            if(!canEdit())return;vm.draft.demandMode=id==R.id.jdNoDemands?"NONE":id==R.id.jdSomeDemands?"SELECT":"";
            if("NONE".equals(vm.draft.demandMode))vm.draft.demands.clear();vm.edited("demands");
        });
        click(R.id.jdSent,()->guard("SENT"));
        click(R.id.jdCheckSent,()->guard("SENT"));
        click(R.id.jdFixAccount,()->guard("ACCOUNT"));
        click(R.id.jdSubmit,()->{if(vm.busy||vm.draft==null)return;String id=vm.draft.id;
            new AlertDialog.Builder(this).setMessage("Gửi tin để Admin duyệt? Khi được duyệt, nội dung và địa chỉ làm việc sẽ hiển thị công khai.")
                .setNegativeButton("Quay lại",null).setPositiveButton("Gửi duyệt",(d,w)->{if(vm.draft!=null&&id.equals(vm.draft.id))vm.submit();}).show();});
        click(R.id.jdBack,this::back);click(R.id.jdCreate,()->guard("NEW"));click(R.id.jdListReload,vm::loadList);
        click(R.id.jdRetryAccess,vm::retry);click(R.id.jdCatalogRefresh,vm::refreshCatalog);
        click(R.id.jdSave,()->{hideKeyboard();vm.saveThen("");});click(R.id.jdPreview,()->{hideKeyboard();vm.preview();});
        click(R.id.jdEdit,vm::edit);click(R.id.jdExpiry,this::pickExpiry);
        click(R.id.jdFormReload,()->{
            if(vm.draft==null)return;String id=vm.draft.id;
            new AlertDialog.Builder(this).setMessage("Bỏ các thay đổi và đọc lại nháp đã lưu?").setNegativeButton("Tiếp tục sửa",null)
                    .setPositiveButton("Tải lại",(d,w)->{if(vm.draft!=null && id.equals(vm.draft.id))vm.open(id);}).show();
        });
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){@Override public void handleOnBackPressed(){back();}});
        boolean retained=vm.started;lastPageEvent=vm.pageEvent;lastValidationEvent=vm.validationEvent;
        vm.changes.observe(this,n->render());vm.initialize(saved==null && getIntent().getBooleanExtra(CREATE_NEW,false),getIntent().getStringExtra(EDIT_POST));
        if(saved!=null && retained){int y=saved.getInt("draft_scroll",0);findViewById(R.id.jdScroll).post(()->findViewById(R.id.jdScroll).scrollTo(0,y));}
    }
    @Override protected void onStart(){super.onStart();if(vm.ready && !vm.busy)vm.refreshAccess();}
    @Override protected void onSaveInstanceState(Bundle state){state.putInt("draft_scroll",findViewById(R.id.jdScroll).getScrollY());super.onSaveInstanceState(state);}
    private boolean canEdit(){return !rendering && vm.ready && !vm.busy && vm.draft!=null;}
    private void bindText(String key,int id,Consumer<String> update){
        fields.put(key,id);((EditText)findViewById(id)).addTextChangedListener(new TextWatcher(){
            public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void afterTextChanged(Editable e){}
            public void onTextChanged(CharSequence s,int st,int before,int count){if(canEdit()){update.accept(s.toString());vm.edited(key);}}
        });
    }
    private void bindSpinner(String key,int id,Consumer<String> update){
        fields.put(key,id);((Spinner)findViewById(id)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> parent){}
            public void onItemSelected(AdapterView<?> parent,View child,int position,long row){
                if(!canEdit() || position!=((Spinner)parent).getSelectedItemPosition())return;
                List<String> ids=optionIds.get(id);if(ids==null||position>=ids.size())return;
                String value=ids.get(position);if(value.equals(current(key)))return;update.accept(value);vm.edited(key);
            }
        });
    }
    private String current(String key){switch(key){case "categoryId":return vm.draft.categoryId;case "areaCode":return vm.draft.areaCode;case "shift":return vm.draft.shift;default:return vm.draft.payUnit;}}
    private static void toggle(List<String> list,String value,boolean selected){if(selected){if(!list.contains(value))list.add(value);}else list.remove(value);}
    private void options(int id,List<String> values,List<String> labels,String selected){
        List<String> ids=new ArrayList<>(),names=new ArrayList<>();ids.add("");names.add(getString(R.string.jd_choose));ids.addAll(values);names.addAll(labels);
        if(!ids.contains(selected)){ids.add(selected);names.add(getString(R.string.jd_unavailable));}
        Spinner spinner=findViewById(id);
        if(!ids.equals(optionIds.get(id)) || !names.equals(optionLabels.get(id))){
            optionIds.put(id,ids);optionLabels.put(id,names);
            ArrayAdapter<String> adapter=new ArrayAdapter<>(this,R.layout.profile_spinner_item,names);adapter.setDropDownViewResource(R.layout.profile_spinner_item);spinner.setAdapter(adapter);
        }
        int index=ids.indexOf(selected);if(spinner.getSelectedItemPosition()!=index)spinner.setSelection(index);
    }
    private void catalogOptions(int id,List<JobDraftStore.Item> items,String selected){
        List<String> ids=new ArrayList<>(),names=new ArrayList<>();for(JobDraftStore.Item item:items){ids.add(item.id);names.add(item.name);}options(id,ids,names,selected);
    }
    private void back(){if(!vm.ready){guard("CLOSE");return;}if(vm.page==JobDraftViewModel.Page.PREVIEW && !vm.busy){vm.edit();return;}guard(vm.page==JobDraftViewModel.Page.LIST?"CLOSE":"LIST");}
    private void guard(String action){
        if(vm.busy){vm.message="Đang xử lý. Cô/chú vui lòng chờ.";vm.emit();return;}
        hideKeyboard();if(!vm.ready || !vm.dirty || vm.draft==null){vm.navigate(action);return;}
        String id=vm.draft.id;
        AlertDialog.Builder dialog=new AlertDialog.Builder(this).setMessage("Lưu thay đổi trước khi rời nháp?")
                .setNegativeButton("Tiếp tục sửa",null).setNeutralButton("Bỏ thay đổi",(d,w)->{if(vm.draft!=null&&id.equals(vm.draft.id))vm.navigate(action);});
        if(!vm.readRequired)dialog.setPositiveButton("Lưu nháp",(d,w)->{if(vm.draft!=null&&id.equals(vm.draft.id))vm.saveThen(action);});
        dialog.show();
    }
    private void pickExpiry(){
        if(!canEdit())return;String id=vm.draft.id;Calendar date=Calendar.getInstance();
        if(vm.draft.expiresAt>0)date.setTimeInMillis(vm.draft.expiresAt);else{date.add(Calendar.DAY_OF_MONTH,7);date.set(Calendar.HOUR_OF_DAY,18);date.set(Calendar.MINUTE,0);}
        new DatePickerDialog(this,(picker,year,month,day)->{
            date.set(year,month,day);
            new TimePickerDialog(this,(time,hour,minute)->{
                if(!canEdit()||!id.equals(vm.draft.id))return;
                date.set(Calendar.HOUR_OF_DAY,hour);date.set(Calendar.MINUTE,minute);date.set(Calendar.SECOND,0);date.set(Calendar.MILLISECOND,0);
                vm.draft.expiresAt=date.getTimeInMillis();vm.edited("expiresAt");
            },date.get(Calendar.HOUR_OF_DAY),date.get(Calendar.MINUTE),true).show();
        },date.get(Calendar.YEAR),date.get(Calendar.MONTH),date.get(Calendar.DAY_OF_MONTH)).show();
    }
    private void render(){
        if(vm.sessionEnded){finish();return;}
        if(vm.showPosts||!vm.submittedPostId.isEmpty()){
            String id=vm.submittedPostId,notice=vm.submittedNotice;vm.submittedPostId="";vm.submittedNotice="";vm.showPosts=false;
            if(!id.isEmpty()&&getIntent().hasExtra(EDIT_POST)){setResult(RESULT_OK,new Intent().putExtra(JobPostActivity.POST_ID,id).putExtra("submission_notice",notice));finish();return;}
            Intent intent=new Intent(this,JobPostActivity.class).putExtra("submission_notice",notice);if(!id.isEmpty())intent.putExtra(JobPostActivity.POST_ID,id);posts.launch(intent);return;
        }
        if(!vm.exitRoute.isEmpty()){
            if(!"CLOSE".equals(vm.exitRoute))setResult(RESULT_OK,new Intent().putExtra(RoleNavigationBar.RETURN_DESTINATION,vm.exitRoute));
            finish();return;
        }
        rendering=true;
        RoleNavigationBar.topbar(this,vm.ready?"EMPLOYER":"");
        WindowInsetsCompat insets=ViewCompat.getRootWindowInsets(findViewById(R.id.jdRoot));
        boolean keyboard=insets!=null && insets.isVisible(WindowInsetsCompat.Type.ime());
        navigation.render("EMPLOYER",RoleNavigationBar.Action.WORK,vm.ready&&!keyboard,!vm.busy);
        boolean list=vm.ready&&vm.page==JobDraftViewModel.Page.LIST,form=vm.ready&&vm.draft!=null&&!list;
        boolean edit=form&&vm.page==JobDraftViewModel.Page.EDIT,preview=form&&!edit;
        show(R.id.jdListSection,list);show(R.id.jdEditorSection,edit);show(R.id.jdPreviewSection,preview);show(R.id.jdActions,form);
        set(R.id.jdTitle,getString(list||!vm.ready?R.string.jd_title:edit?R.string.jd_edit_title:R.string.jd_preview_title));
        set(R.id.jdMessage,vm.message);show(R.id.jdMessage,!form&&!vm.message.isEmpty());
        set(R.id.jdActionMessage,vm.message);show(R.id.jdActionMessage,form&&!vm.message.isEmpty());
        show(R.id.jdProgress,!form&&(vm.busy||vm.accessChecking));show(R.id.jdActionProgress,form&&vm.busy);show(R.id.jdCheckSent,form&&vm.checkSent);
        show(R.id.jdAccessNote,vm.ready&&vm.offlineAccess);show(R.id.jdRetryAccess,!vm.ready&&!vm.busy&&!vm.accessChecking);
        enabled(findViewById(R.id.jdListSection),!vm.busy);enabled(findViewById(R.id.jdEditorSection),!vm.busy);
        enabled(findViewById(R.id.jdActions),!vm.busy);findViewById(R.id.jdEdit).setEnabled(!vm.busy);
        if(list && renderedList!=vm.drafts){
            renderedList=vm.drafts;LinearLayout box=findViewById(R.id.jdList);box.removeAllViews();
            for(LocalJobDraft item:vm.drafts){
                LinearLayout card=new LinearLayout(this);card.setOrientation(LinearLayout.VERTICAL);card.setPadding(dp(16),dp(16),dp(16),dp(16));card.setBackgroundResource(R.drawable.verification_section);
                LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(24);box.addView(card,p);
                String title=item.title.trim().isEmpty()?getString(R.string.jd_empty_title):item.title;
                text(card,title,true);text(card,getString(R.string.jd_saved_time,date(item.updatedAt)),false);
                button(card,getString(R.string.jd_open),()->vm.open(item.id),false).setContentDescription(getString(R.string.jd_open)+": "+title);
                button(card,getString(R.string.jd_delete),()->new AlertDialog.Builder(this).setMessage("Xóa nháp “"+title+"” trên máy?")
                        .setNegativeButton("Giữ lại",null).setPositiveButton("Xóa nháp",(d,w)->vm.delete(item)).show(),true)
                        .setContentDescription(getString(R.string.jd_delete)+": "+title);
            }
        }
        if(form){
            LocalJobDraft d=vm.draft;
            set(R.id.jdWorkAddress,d.workAddress);set(R.id.jdName,d.title);set(R.id.jdDescription,d.description);set(R.id.jdRequirements,d.requirements);set(R.id.jdSkills,d.skillsInput);set(R.id.jdScheduleText,d.schedule);set(R.id.jdPay,d.payInput);
            catalogOptions(R.id.jdCategory,vm.catalog.categories,d.categoryId);catalogOptions(R.id.jdArea,vm.catalog.areas,d.areaCode);
            options(R.id.jdShift,LocalJobDraft.SHIFTS,Arrays.asList(getResources().getStringArray(R.array.jd_shifts)),d.shift);
            options(R.id.jdUnit,LocalJobDraft.UNITS,Arrays.asList(getResources().getStringArray(R.array.jd_units)),d.payUnit);
            for(int i=0;i<days.length;i++)((CheckBox)findViewById(days[i])).setChecked(d.days.contains(LocalJobDraft.DAYS.get(i)));
            ((RadioGroup)findViewById(R.id.jdDemandMode)).check("NONE".equals(d.demandMode)?R.id.jdNoDemands:"SELECT".equals(d.demandMode)?R.id.jdSomeDemands:View.NO_ID);
            show(R.id.jdDemandChoices,"SELECT".equals(d.demandMode));
            for(int i=0;i<demands.length;i++)((CheckBox)findViewById(demands[i])).setChecked(d.demands.contains(LocalJobDraft.DEMANDS.get(i)));
            set(R.id.jdExpiry,d.expiresAt>0?date(d.expiresAt):getString(R.string.jd_pick_expiry));
            findViewById(R.id.jdExpiry).setContentDescription("Hạn nhận hồ sơ: "+(d.expiresAt>0?date(d.expiresAt):getString(R.string.jd_pick_expiry)));
            String catalog=(vm.fromCache?getString(R.string.jd_cache)+"\n":"")+vm.catalogMessage;
            set(R.id.jdCatalogStatus,catalog.trim());show(R.id.jdCatalogStatus,!catalog.trim().isEmpty());
            findViewById(R.id.jdCatalogRefresh).setEnabled(!vm.busy&&!vm.catalogLoading);
            for(Map.Entry<String,Integer> field:fields.entrySet()){
                View view=findViewById(field.getValue());String error=vm.errors.get(field.getKey());
                if(view instanceof EditText)((EditText)view).setError(error);
                Integer errorId=errorViews.get(field.getKey());if(errorId!=null){set(errorId,error==null?"":error);show(errorId,error!=null);}
            }
            show(R.id.jdSubmit,preview);findViewById(R.id.jdSubmit).setEnabled(!vm.busy&&!vm.readRequired);show(R.id.jdFixAccount,vm.fixAccount);show(R.id.jdPreview,edit);show(R.id.jdFormReload,vm.readRequired);findViewById(R.id.jdSave).setEnabled(!vm.busy&&!vm.readRequired);
            if(preview){
                set(R.id.jdPreviewWork,d.title.trim()+"\n\n"+name(vm.catalog.categories,d.categoryId)+"\n\n"+d.description.trim()
                        +(d.requirements.trim().isEmpty()?"":"\n\nYêu cầu: "+d.requirements.trim())
                        +"\n\nKỹ năng: "+(d.skills().isEmpty()?"Không yêu cầu kỹ năng riêng":String.join(", ",d.skills())));
                set(R.id.jdPreviewSchedule,"Khu vực: "+name(vm.catalog.areas,d.areaCode)+"\nĐịa chỉ: "+d.workAddress.trim()+"\n\n"+labels(LocalJobDraft.DAYS,Arrays.asList(getResources().getStringArray(R.array.jd_days)),d.days)
                        +"\n"+choice(LocalJobDraft.SHIFTS,R.array.jd_shifts,d.shift)+"\n"+d.schedule.trim()+"\n\nYêu cầu thể lực: "
                        +("NONE".equals(d.demandMode)?"Không có các yêu cầu trong danh sách":labels(LocalJobDraft.DEMANDS,
                            Arrays.asList(getString(R.string.jd_lifting),getString(R.string.jd_standing),getString(R.string.jd_moving)),d.demands)));
                Long amount=d.payAmount();set(R.id.jdPreviewPay,"Tiền công: "+(amount==null?d.payInput:NumberFormat.getIntegerInstance(new Locale("vi","VN")).format(amount))+" đ · "
                        +choice(LocalJobDraft.UNITS,R.array.jd_units,d.payUnit)+"\n\nHạn nhận: "+date(d.expiresAt));
            }
        }
        rendering=false;
        if(vm.pageEvent!=lastPageEvent){lastPageEvent=vm.pageEvent;findViewById(R.id.jdScroll).post(()->findViewById(R.id.jdScroll).scrollTo(0,0));}
        if(vm.validationEvent!=lastValidationEvent){
            lastValidationEvent=vm.validationEvent;
            if(!vm.errors.isEmpty()){Integer id=fields.get(vm.errors.keySet().iterator().next());if(id!=null)findViewById(id).post(()->scrollTo(id));}
        }
    }
    private void scrollTo(int id){
        ScrollView scroll=findViewById(R.id.jdScroll);View view=findViewById(id);if(!view.isShown())return;
        int y=view.getTop();android.view.ViewParent parent=view.getParent();while(parent instanceof View&&parent!=scroll){y+=((View)parent).getTop();parent=parent.getParent();}
        scroll.smoothScrollTo(0,Math.max(0,y-dp(16)));view.requestFocus();
    }
    private String date(long millis){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,new Locale("vi","VN")).format(new Date(millis));}
    private String name(List<JobDraftStore.Item> items,String id){for(JobDraftStore.Item i:items)if(i.id.equals(id))return i.name;return getString(R.string.jd_unavailable);}
    private String choice(List<String> ids,int labels,String value){int i=ids.indexOf(value);return i<0?"":getResources().getStringArray(labels)[i];}
    private String labels(List<String> ids,List<String> names,List<String> selected){List<String> result=new ArrayList<>();for(int i=0;i<ids.size();i++)if(selected.contains(ids.get(i)))result.add(names.get(i));return String.join(", ",result);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
    private void show(int id,boolean visible){findViewById(id).setVisibility(visible?View.VISIBLE:View.GONE);}
    private void set(int id,String value){TextView view=findViewById(id);if(!view.getText().toString().equals(value))view.setText(value);}
    private void click(int id,Runnable action){findViewById(id).setOnClickListener(v->action.run());}
    private void enabled(View view,boolean enabled){view.setEnabled(enabled);if(view instanceof ViewGroup)for(int i=0;i<((ViewGroup)view).getChildCount();i++)enabled(((ViewGroup)view).getChildAt(i),enabled);}
    private void text(LinearLayout parent,String value,boolean heading){TextView t=new TextView(this);t.setTextAppearance(heading?R.style.TextAppearance_GrandMa_Label:R.style.TextAppearance_GrandMa_Body);t.setText(value);t.setSaveEnabled(false);
        ViewCompat.setAccessibilityHeading(t,heading);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(12);parent.addView(t,p);}
    private AppCompatButton button(LinearLayout parent,String title,Runnable action,boolean danger){
        AppCompatButton b=new AppCompatButton(new androidx.appcompat.view.ContextThemeWrapper(this,R.style.Widget_GrandMa_Button_Secondary),null,0);
        b.setText(title);b.setSaveEnabled(false);b.setOnClickListener(v->action.run());if(danger)b.setTextColor(androidx.core.content.ContextCompat.getColor(this,R.color.text_error));
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(12);parent.addView(b,p);return b;
    }
    private void hideKeyboard(){View focus=getCurrentFocus();if(focus==null)return;android.view.inputmethod.InputMethodManager input=(android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);if(input!=null)input.hideSoftInputFromWindow(focus.getWindowToken(),0);focus.clearFocus();}
}
