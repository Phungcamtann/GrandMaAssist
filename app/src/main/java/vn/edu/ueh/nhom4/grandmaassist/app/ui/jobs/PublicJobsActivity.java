package vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs;

import android.content.Intent;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.*;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;
import androidx.lifecycle.ViewModelProvider;
import java.text.*;
import java.util.*;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.data.local.PublicJobStore;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.*;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.PublicJobsViewModel;

public final class PublicJobsActivity extends AppCompatActivity {
    public static final String ROLE="public_browse_role";
    public static final String NAVIGATION="public_browse_navigation";
    private PublicJobsViewModel vm;
    private RoleNavigationBar nav;
    private boolean rendering;
    private boolean filtersExpanded;
    private Object bodyKey;
    private long scrollEvent;
    private int listScroll;
    private List<String> categoryIds=new ArrayList<>(),areaIds=new ArrayList<>();
    private final Handler timer=new Handler(Looper.getMainLooper());
    private final Runnable tick=new Runnable(){public void run(){
        if(vm!=null){vm.pruneExpired();render();}timer.postDelayed(this,30_000);
    }};

    @Override protected void onCreate(Bundle saved){
        super.onCreate(saved);WindowCompat.setDecorFitsSystemWindows(getWindow(),false);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightStatusBars(true);
        WindowCompat.getInsetsController(getWindow(),getWindow().getDecorView()).setAppearanceLightNavigationBars(true);
        setContentView(R.layout.activity_public_jobs);findViewById(R.id.pbContent).setSaveFromParentEnabled(false);
        vm=new ViewModelProvider(this).get(PublicJobsViewModel.class);scrollEvent=vm.scrollEvent;
        nav=new RoleNavigationBar(this,a->{
            if(a==RoleNavigationBar.Action.OTHER){if(!vm.detailId.isEmpty())back();return;}
            if(a==RoleNavigationBar.Action.HOME || a==RoleNavigationBar.Action.PROFILE || a==RoleNavigationBar.Action.ACCOUNT)leave(a.name());
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pbRoot),(v,i)->{
            androidx.core.graphics.Insets safe=i.getInsets(WindowInsetsCompat.Type.systemBars()|WindowInsetsCompat.Type.displayCutout()|WindowInsetsCompat.Type.ime());
            v.setPadding(safe.left,safe.top,safe.right,safe.bottom);render();return i;
        });
        findViewById(R.id.pbScroll).addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            int width=Math.min(v.getWidth(),getResources().getDimensionPixelSize(R.dimen.content_max_width));if(width<=0)return;
            View content=findViewById(R.id.pbContent);FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)content.getLayoutParams();
            if(p.width!=width){p.width=width;p.gravity=Gravity.TOP|Gravity.CENTER_HORIZONTAL;content.setLayoutParams(p);}
            View toolbar=findViewById(R.id.pbToolbar);ViewGroup.LayoutParams bar=toolbar.getLayoutParams();
            if(bar.width!=width){bar.width=width;toolbar.setLayoutParams(bar);}
        });
        click(R.id.pbBack,this::back);click(R.id.pbReload,vm::reload);click(R.id.pbFilterToggle,this::toggleFilters);
        click(R.id.pbApply,()->{hideKeyboard();vm.apply();});click(R.id.pbReset,vm::clearFilters);
        click(R.id.pbMore,vm::next);click(R.id.pbCatalogReload,vm::loadCatalog);
        bindSpinner(R.id.pbCategory,true);bindSpinner(R.id.pbArea,false);
        getOnBackPressedDispatcher().addCallback(this,new OnBackPressedCallback(true){public void handleOnBackPressed(){back();}});
        vm.changes.observe(this,n->render());
        listScroll=saved==null?0:saved.getInt("list_scroll",0);
        filtersExpanded=saved!=null && saved.getBoolean("filters_expanded",false);
        vm.initialize(getIntent().getStringExtra(ROLE),safe(saved,"category"),safe(saved,"area"),safe(saved,"detail"));
        if(saved!=null){int y=saved.getInt("scroll",0);findViewById(R.id.pbScroll).post(()->findViewById(R.id.pbScroll).scrollTo(0,y));}
        ViewCompat.requestApplyInsets(findViewById(R.id.pbRoot));
    }
    private String safe(Bundle b,String key){String s=b==null?null:b.getString(key);return s==null?"":s;}
    @Override protected void onStart(){super.onStart();timer.post(tick);}
    @Override protected void onStop(){timer.removeCallbacks(tick);super.onStop();}
    @Override protected void onSaveInstanceState(Bundle out){
        out.putInt("scroll",findViewById(R.id.pbScroll).getScrollY());out.putInt("list_scroll",listScroll);
        out.putBoolean("filters_expanded",filtersExpanded);
        out.putString("category",vm.category);out.putString("area",vm.area);out.putString("detail",vm.detailId);
        super.onSaveInstanceState(out);
    }
    private void leave(String route){setResult(RESULT_OK,new Intent().putExtra(RoleNavigationBar.RETURN_DESTINATION,route));finish();}
    private void back(){
        if(vm.detailId.isEmpty()){finish();return;}
        vm.backToList();findViewById(R.id.pbScroll).post(()->findViewById(R.id.pbScroll).scrollTo(0,listScroll));
    }
    private void bindSpinner(int id,boolean category){
        ((Spinner)findViewById(id)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onNothingSelected(AdapterView<?> p){}
            public void onItemSelected(AdapterView<?> p,View v,int position,long itemId){
                List<String> ids=category?categoryIds:areaIds;
                if(rendering || position!=p.getSelectedItemPosition() || position>=ids.size())return;
                if(category)vm.draftCategory=ids.get(position);else vm.draftArea=ids.get(position);
            }
        });
    }
    private void spinner(int id,List<PublicJobStore.Item> items,String selected,boolean category){
        List<String> ids=new ArrayList<>(),names=new ArrayList<>();ids.add("");
        names.add(getString(category?R.string.pb_all_categories:R.string.pb_all_areas));
        for(PublicJobStore.Item i:items){ids.add(i.id);names.add(i.name);}
        if(!ids.contains(selected)){
            ids.add(selected);names.add(getString(category?R.string.pb_selected_category:R.string.pb_selected_area));
        }
        Spinner spinner=findViewById(id);String key=ids.toString()+names.toString();
        if(category)categoryIds=ids;else areaIds=ids;
        if(!key.equals(spinner.getTag())){
            ArrayAdapter<String> adapter=new ArrayAdapter<>(this,R.layout.profile_spinner_item,names);
            adapter.setDropDownViewResource(R.layout.profile_spinner_item);spinner.setAdapter(adapter);spinner.setTag(key);
        }
        int position=ids.indexOf(selected);if(spinner.getSelectedItemPosition()!=position)spinner.setSelection(position);
    }
    private String selectedName(List<PublicJobStore.Item> options,String id,int all,int missing){
        if(id.isEmpty())return getString(all);for(PublicJobStore.Item i:options)if(i.id.equals(id))return i.name;return getString(missing);
    }
    private void render(){
        if(vm==null || nav==null)return;if(vm.ended){finish();return;}rendering=true;
        boolean detail=!vm.detailId.isEmpty();RoleNavigationBar.topbar(this,vm.role);
        WindowInsetsCompat insets=ViewCompat.getRootWindowInsets(findViewById(R.id.pbRoot));
        boolean keyboard=insets!=null && insets.isVisible(WindowInsetsCompat.Type.ime());
        nav.render(vm.role,RoleNavigationBar.Action.OTHER,"SEEKER".equals(vm.role)&&!keyboard&&getIntent().getBooleanExtra(NAVIGATION,false),true);
        text(R.id.pbTitle,getString(detail?R.string.pb_detail:R.string.pb_title));
        show(R.id.pbFilterToggle,!detail);show(R.id.pbFilters,!detail&&filtersExpanded);
        spinner(R.id.pbCategory,vm.catalog.categories,vm.draftCategory,true);spinner(R.id.pbArea,vm.catalog.areas,vm.draftArea,false);
        text(R.id.pbCatalogMessage,vm.catalogMessage);show(R.id.pbCatalogMessage,!vm.catalogMessage.isEmpty());
        show(R.id.pbCatalogReload,!vm.catalogMessage.isEmpty());findViewById(R.id.pbCatalogReload).setEnabled(!vm.catalogBusy);
        String summary=getString(R.string.pb_filter_summary,
                selectedName(vm.catalog.areas,vm.area,R.string.pb_all_areas,R.string.pb_selected_area),
                selectedName(vm.catalog.categories,vm.category,R.string.pb_all_categories,R.string.pb_selected_category));
        text(R.id.pbSummary,summary+"\n"+getString(R.string.pb_order));show(R.id.pbSummary,!detail);
        text(R.id.pbSource,(detail?vm.detailCached:vm.cached)?getString(R.string.pb_old_data):"");
        show(R.id.pbSource,(detail?vm.detailCached:vm.cached));
        text(R.id.pbMessage,vm.message);show(R.id.pbMessage,!vm.message.isEmpty());show(R.id.pbProgress,vm.busy);
        text(R.id.pbCount,getString(R.string.pb_count,vm.items.size()));show(R.id.pbCount,!detail&&!vm.items.isEmpty());
        show(R.id.pbMore,!detail&&vm.more);show(R.id.pbFuture,detail&&vm.selected!=null);
        Object key=detail?vm.selected:vm.items;
        if(bodyKey!=key){
            bodyKey=key;LinearLayout body=findViewById(R.id.pbBody);body.removeAllViews();
            if(detail){if(vm.selected!=null)detail(body,vm.selected);}
            else for(PublicJob job:vm.items)card(body,job);
        }
        // Update deadline labels on foreground/timer without rebuilding focused controls.
        if(vm.selected!=null && detail){
            TextView state=findViewById(R.id.pbDeadlineState);
            if(state!=null)state.setText(vm.selected.expiresAt<=System.currentTimeMillis()?getString(R.string.pb_expired):getString(R.string.pb_deadline,date(vm.selected.expiresAt)));
        }
        for(int id:new int[]{R.id.pbApply,R.id.pbReset,R.id.pbArea,R.id.pbCategory,R.id.pbReload,R.id.pbFilterToggle,R.id.pbMore})findViewById(id).setEnabled(!vm.busy);
        if(!detail){LinearLayout body=findViewById(R.id.pbBody);for(int i=0;i<body.getChildCount();i++)body.getChildAt(i).setEnabled(!vm.busy);}
        rendering=false;
        if(scrollEvent!=vm.scrollEvent){scrollEvent=vm.scrollEvent;findViewById(R.id.pbScroll).post(()->findViewById(R.id.pbScroll).scrollTo(0,0));}
    }
    private void card(LinearLayout body,PublicJob job){
        View card=getLayoutInflater().inflate(R.layout.item_public_job,body,false);
        String badge=getString(vm.cached?R.string.pb_cached_badge:R.string.pb_reviewed);
        cardText(card,R.id.pbCardBadge,badge);cardText(card,R.id.pbCardTitle,job.title);
        cardText(card,R.id.pbCardEmployer,job.employerName+" · "+job.areaName);
        cardText(card,R.id.pbCardCategory,job.categoryName);
        String days=job.days.size()==7?getString(R.string.pb_card_daily):labels(LocalJobDraft.DAYS,getResources().getStringArray(R.array.pb_days_short),job.days);
        cardText(card,R.id.pbCardDays,getString(R.string.pb_card_days,days));
        String schedule=label(LocalJobDraft.SHIFTS,getResources().getStringArray(R.array.jd_shifts),job.shift)+" · "+job.schedule;
        cardText(card,R.id.pbCardSchedule,schedule);cardText(card,R.id.pbCardPay,money(job));
        String deadline=getString(R.string.pb_deadline,date(job.expiresAt));cardText(card,R.id.pbCardDeadline,deadline);
        card.findViewById(R.id.pbCardCachedAt).setVisibility(vm.cached?View.VISIBLE:View.GONE);
        String cachedAt=vm.cached?getString(R.string.pb_offline,date(job.cachedAt)):"";
        cardText(card,R.id.pbCardCachedAt,cachedAt);
        ImageView image=card.findViewById(R.id.pbCardImage);int illustration=illustration(job.categoryId);
        image.setImageResource(illustration==0?R.drawable.ic_role_work:illustration);
        image.setScaleType(illustration==0?ImageView.ScaleType.CENTER_INSIDE:ImageView.ScaleType.CENTER_CROP);
        int padding=illustration==0?dp(48):0;image.setPadding(padding,padding,padding,padding);
        card.findViewById(R.id.pbCardIllustration).setVisibility(illustration==0?View.GONE:View.VISIBLE);
        String fullDays=labels(LocalJobDraft.DAYS,getResources().getStringArray(R.array.jd_days),job.days);
        card.setContentDescription(badge+"\n"+job.title+"\n"+job.employerName+" · "+job.areaName+"\n"+job.categoryName+"\n"+
                fullDays+"\n"+schedule+"\n"+money(job)+"\n"+deadline+(cachedAt.isEmpty()?"":"\n"+cachedAt));
        ViewCompat.setAccessibilityHeading(card,true);
        ViewCompat.setAccessibilityDelegate(card,new AccessibilityDelegateCompat(){
            @Override public void onInitializeAccessibilityNodeInfo(View host,AccessibilityNodeInfoCompat info){
                super.onInitializeAccessibilityNodeInfo(host,info);info.setClassName(Button.class.getName());
            }
        });
        card.setOnClickListener(v->openCard(job.id));
        ViewCompat.replaceAccessibilityAction(card,AccessibilityNodeInfoCompat.AccessibilityActionCompat.ACTION_CLICK,
                getString(R.string.pb_view_details),(v,args)->{if(vm.busy)return false;openCard(job.id);return true;});
        body.addView(card);
    }
    private void openCard(String id){if(vm.busy)return;listScroll=findViewById(R.id.pbScroll).getScrollY();hideKeyboard();vm.open(id,false);}
    private void toggleFilters(){
        if(vm.busy)return;
        filtersExpanded=!filtersExpanded;
        hideKeyboard();
        render();
    }
    private void cardText(View card,int id,String value){((TextView)card.findViewById(id)).setText(value);}
    private int illustration(String categoryId){
        switch(categoryId){
            case "GARDEN_CARE":return R.drawable.job_illustration_garden;
            case "KITCHEN_HELP":return R.drawable.job_illustration_kitchen;
            case "LIBRARY_HELP":return R.drawable.job_illustration_library;
            case "SHOP_HELP":return R.drawable.job_illustration_shop;
            default:return 0;
        }
    }
    private void detail(LinearLayout body,PublicJob j){
        LinearLayout c=section(body);label(c,j.title,true);label(c,j.employerName+"\n"+j.categoryName,false);
        TextView deadline=label(c,"",false);deadline.setId(R.id.pbDeadlineState);
        label(c,getString(vm.detailCached?R.string.pb_offline:R.string.pb_public_time,date(j.cachedAt)),false);
        c=section(body);label(c,getString(R.string.pb_duties),true);label(c,j.description,false);
        c=section(body);label(c,getString(R.string.pb_place),true);label(c,j.areaName+"\n"+j.workAddress,false);
        c=section(body);label(c,getString(R.string.pb_schedule),true);
        label(c,labels(LocalJobDraft.DAYS,getResources().getStringArray(R.array.jd_days),j.days)+"\n"+
                label(LocalJobDraft.SHIFTS,getResources().getStringArray(R.array.jd_shifts),j.shift)+"\n"+j.schedule,false);
        c=section(body);label(c,getString(R.string.pb_requirements),true);
        if(!j.requirements.isEmpty())label(c,j.requirements,false);
        if(!j.skills.isEmpty())label(c,getString(R.string.pb_skills,String.join(", ",j.skills)),false);
        label(c,j.demands.isEmpty()?getString(R.string.pb_no_demands):labels(LocalJobDraft.DEMANDS,
                new String[]{getString(R.string.jd_lifting),getString(R.string.jd_standing),getString(R.string.jd_moving)},j.demands),false);
        c=section(body);label(c,getString(R.string.pb_pay),true);label(c,money(j),false);
    }
    private LinearLayout section(LinearLayout body){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(16),dp(16),dp(16),dp(16));
        c.setBackgroundResource(R.drawable.verification_section);LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(24);body.addView(c,p);return c;
    }
    private TextView label(LinearLayout parent,String value,boolean heading){
        TextView v=new TextView(this);v.setTextAppearance(heading?R.style.TextAppearance_GrandMa_Label:R.style.TextAppearance_GrandMa_Body);
        v.setText(value);v.setSaveEnabled(false);ViewCompat.setAccessibilityHeading(v,heading);
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.topMargin=dp(12);parent.addView(v,p);return v;
    }
    private String date(long millis){return DateFormat.getDateTimeInstance(DateFormat.SHORT,DateFormat.SHORT,new Locale("vi","VN")).format(new Date(millis));}
    private String money(PublicJob j){return NumberFormat.getIntegerInstance(new Locale("vi","VN")).format(j.payAmount)+" đ · "+label(LocalJobDraft.UNITS,getResources().getStringArray(R.array.jd_units),j.payUnit);}
    private String label(List<String> ids,String[] labels,String value){int i=ids.indexOf(value);return i<0?value:labels[i];}
    private String labels(List<String> ids,String[] labels,List<String> selected){List<String> out=new ArrayList<>();for(String v:ids)if(selected.contains(v))out.add(label(ids,labels,v));return String.join(", ",out);}
    private void hideKeyboard(){View v=getCurrentFocus();if(v!=null){((android.view.inputmethod.InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);v.clearFocus();}}
    private void click(int id,Runnable r){findViewById(id).setOnClickListener(v->r.run());}
    private void show(int id,boolean b){findViewById(id).setVisibility(b?View.VISIBLE:View.GONE);}
    private void text(int id,String s){TextView v=findViewById(id);if(!v.getText().toString().equals(s))v.setText(s);}
    private int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
}
