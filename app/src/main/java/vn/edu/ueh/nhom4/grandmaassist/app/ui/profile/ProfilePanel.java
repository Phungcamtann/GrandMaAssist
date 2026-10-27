package vn.edu.ueh.nhom4.grandmaassist.app.ui.profile;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.AccountMethodsFlow;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.AuthRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.ProfileRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.ProfileDraft;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.AuthViewModel;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.ProfileViewModel;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleHomePanel;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation.RoleNavigationBar;

/** Activity-owned bindings only. No Activity escapes into profile state or repositories. */
public final class ProfilePanel {
    private final AppCompatActivity activity;
    private final ProfileViewModel vm;
    private final AuthViewModel auth;
    private boolean rendering;
    private List<ProfileRepository.Area> renderedAreas;
    private List<String> areaCodes = new ArrayList<>();
    private String renderedAreaKey;
    private final OnBackPressedCallback back;
    private final RoleHomePanel homePanel;
    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> verification;
    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> jobDrafts;
    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> publicJobs;
    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> myApplications;

    public ProfilePanel(AppCompatActivity activity, ProfileViewModel vm, AuthViewModel auth) {
        this.activity = activity; this.vm = vm; this.auth = auth;
        verification=activity.registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),result->{
            if(vm.busy || vm.dirty)return;
            android.content.Intent data=result.getData();
            if(data!=null && vm.snapshot!=null && com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser()!=null
                    && vm.snapshot.uid.equals(com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid())){
                String route=data.getStringExtra(RoleNavigationBar.RETURN_DESTINATION);
                if("POSTS".equals(route)){openPosts();return;}
                if("OTHER".equals(route)){openPublicJobs();return;}
                if("WORK".equals(route) && "EMPLOYER".equals(vm.snapshot.role)){openDrafts(false);return;}
                if("HOME".equals(route))vm.showHome();else if("ACCOUNT".equals(route))vm.showAccount();
                else if("PROFILE".equals(route))vm.showProfile();
            }
            vm.load();
        });
        jobDrafts=activity.registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),result->{
            if(vm.dirty || auth.getState().getValue().account==null)return;
            android.content.Intent data=result.getData();
            if(data!=null && result.getResultCode()==android.app.Activity.RESULT_OK){
                String route=data.getStringExtra(RoleNavigationBar.RETURN_DESTINATION);
                if("PROFILES".equals(route)){openVerification();return;}
                if("DRAFTS".equals(route)){openDrafts(false);return;}
                if("HOME".equals(route))vm.showHome();
                else if("ACCOUNT".equals(route)){if(vm.snapshot==null)vm.showSecurity();else vm.showAccount();}
            }
            if(!vm.busy)vm.load();
        });
        publicJobs=activity.registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),result->{
            com.google.firebase.auth.FirebaseUser user=com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if(user==null || vm.snapshot==null || !user.getUid().equals(vm.snapshot.uid))return;
            android.content.Intent data=result.getData();
            String route=data==null?"":data.getStringExtra(RoleNavigationBar.RETURN_DESTINATION);
            if("HOME".equals(route))vm.showHome();
            else if("PROFILE".equals(route)){vm.showProfile();vm.refreshVerification();}
            else if("ACCOUNT".equals(route))vm.showAccount();
            else if("WORK".equals(route))openMyApplications();
        });
        myApplications=activity.registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),result->{
            com.google.firebase.auth.FirebaseUser user=com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if(user==null || vm.snapshot==null || !user.getUid().equals(vm.snapshot.uid))return;
            android.content.Intent data=result.getData();
            String route=data==null?"":data.getStringExtra(RoleNavigationBar.RETURN_DESTINATION);
            if("HOME".equals(route))vm.showHome();
            else if("PROFILE".equals(route)){vm.showProfile();vm.refreshVerification();}
            else if("ACCOUNT".equals(route))vm.showAccount();
            else if("OTHER".equals(route))openPublicJobs();
        });
        view(R.id.profileContent).setSaveFromParentEnabled(false);
        edit(R.id.profileDisplayName, value -> vm.draft.displayName = value);
        edit(R.id.profileSkills, value -> vm.draft.skills = value);
        edit(R.id.profileExperience, value -> vm.draft.experience = value);
        edit(R.id.profileOrganization, value -> vm.draft.organizationName = value);
        edit(R.id.profileIntroduction, value -> vm.draft.introduction = value);
        check(R.id.profile_morning, value -> vm.draft.morning = value);
        check(R.id.profile_afternoon, value -> vm.draft.afternoon = value);
        check(R.id.profile_evening, value -> vm.draft.evening = value);
        ((RadioGroup) view(R.id.profileEmployerType)).setOnCheckedChangeListener((group, id) -> {
            if (rendering) return;
            vm.draft.employerType = id == R.id.profile_organization ? "ORGANIZATION" : "INDIVIDUAL";
            vm.edited(); render(auth.getState().getValue());
        });
        ((Spinner) view(R.id.profileArea)).setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onNothingSelected(AdapterView<?> parent) { }
            @Override public void onItemSelected(AdapterView<?> parent, View child, int position, long id) {
                if (rendering || position >= areaCodes.size() || vm.draft.areaCode.equals(areaCodes.get(position))) return;
                vm.draft.areaCode = areaCodes.get(position); vm.edited();
            }
        });
        click(R.id.btnRoleSeeker, () -> vm.selectRole("SEEKER"));
        click(R.id.btnRoleEmployer, () -> vm.selectRole("EMPLOYER"));
        click(R.id.btnRoleContinue, () -> {
            if (vm.selectedRole.isEmpty() || vm.busy) return;
            String selected = vm.selectedRole;
            new AlertDialog.Builder(activity).setMessage(activity.getString(R.string.p_confirm_role,
                    activity.getString("SEEKER".equals(selected) ? R.string.p_seeker : R.string.p_employer)))
                    .setNegativeButton(R.string.p_cancel, null)
                    .setPositiveButton(R.string.p_confirm, (dialog, which) -> {
                        if (selected.equals(vm.selectedRole)) vm.choose();
                    }).show();
        });
        click(R.id.btnProfileSave, vm::save);
        click(R.id.btnProfileReload, () -> discardThen(vm::load));
        click(R.id.btnOfflineDrafts,()->openDrafts(false));
        click(R.id.btnAreasReload, vm::loadAreas);
        click(R.id.btnProfileSync, () -> discardThen(() -> { vm.dirty = false; vm.syncPhone(); }));
        click(R.id.btnProfileAccount,()->{if(vm.snapshot==null)vm.showSecurity();else vm.showAccount();scrollTop();});
        click(R.id.btnProfileWallet, () -> {
            String role = vm.snapshot != null ? vm.snapshot.role : "SEEKER";
            vn.edu.ueh.nhom4.grandmaassist.app.ui.wallet.WalletActivity.start(activity, role, 0L);
        });
        click(R.id.btnProfileHelp, () -> android.widget.Toast.makeText(activity, "Trung tâm trợ giúp GrandMa Assist", android.widget.Toast.LENGTH_SHORT).show());
        click(R.id.btnProfileSettings, () -> android.widget.Toast.makeText(activity, "Cài đặt ứng dụng", android.widget.Toast.LENGTH_SHORT).show());
        click(R.id.btnProfileVerification,()->{
            if(vm.verificationLoading)return;
            if("UNKNOWN".equals(vm.verificationStatus))vm.refreshVerification();else openVerification();
        });
        click(R.id.btnProfileBackHome,()->navigate(RoleNavigationBar.Action.HOME));
        click(R.id.btnReturnProfile,()->{if(vm.snapshot==null)vm.showProfile();else vm.showAccount();scrollTop();});
        click(R.id.btnProfilePhone, () -> { vm.showSecurity(); auth.beginAccountMethod(false);scrollTop(); });
        click(R.id.btnProfileSignOut, this::signOut);
        click(R.id.btnSignOut, this::signOut);
        back = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() {
                if (vm.busy) {
                    new AlertDialog.Builder(activity).setMessage(R.string.p_busy).setPositiveButton(android.R.string.ok, null).show();
                } else if (vm.accountPage) {if(vm.snapshot==null)vm.showProfile();else vm.showAccount();scrollTop();}
                else if(vm.snapshot!=null && vm.destination!=ProfileViewModel.Destination.HOME)navigate(RoleNavigationBar.Action.HOME);
                else discardThen(() -> { vm.dirty = false; activity.finish(); });
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, back);
        homePanel=new RoleHomePanel(activity,vm,this::navigate,this::openVerification,()->{vm.showSecurity();scrollTop();},this::signOut,()->openDrafts(true),this::openPosts);
    }
    private void scrollTop(){activity.findViewById(R.id.main).scrollTo(0,0);}
    private void navigate(RoleNavigationBar.Action action){
        if(vm.busy || auth.getState().getValue().isBusy())return;
        switch(action){
            case HOME:discardThen(()->{vm.dirty=false;vm.showHome();scrollTop();});break;
            case PROFILE:
                if(vm.snapshot!=null && "ADMIN".equals(vm.snapshot.role))vm.showAccount();
                else{vm.showProfile();vm.refreshVerification();}
                scrollTop();break;
            case ACCOUNT:vm.showAccount();scrollTop();break;
            case PROCESS:new AlertDialog.Builder(activity).setTitle("Xử lý").setItems(new String[]{"Kiểm tra hồ sơ","Duyệt tin tuyển"},(d,w)->{if(w==0)openVerification();else openPosts();}).show();break;
            case WORK:if(vm.snapshot!=null && "EMPLOYER".equals(vm.snapshot.role))openPosts();else if(vm.snapshot!=null && "SEEKER".equals(vm.snapshot.role))openMyApplications();else openDrafts(false);break;
            case OTHER:if(vm.snapshot!=null && "EMPLOYER".equals(vm.snapshot.role))discardThen(()->vn.edu.ueh.nhom4.grandmaassist.app.ui.sessions.WorkSessionsActivity.start(activity,"EMPLOYER"));else openPublicJobs();break;
            default:break;
        }
    }
    private void openPublicJobs(){
        AuthRepository.State state=auth.getState().getValue();
        if(vm.busy || state.account==null || state.isBusy() || auth.methods().active() || vm.snapshot==null || !"SEEKER".equals(vm.snapshot.role))return;
        discardThen(()->publicJobs.launch(new android.content.Intent(activity,
                vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.PublicJobsActivity.class)
                .putExtra(vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.PublicJobsActivity.ROLE,vm.snapshot.role)
                .putExtra(vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.PublicJobsActivity.NAVIGATION,true)));
    }
    private void openMyApplications(){
        AuthRepository.State state=auth.getState().getValue();
        if(vm.busy || state.account==null || state.isBusy() || auth.methods().active() || vm.snapshot==null || !"SEEKER".equals(vm.snapshot.role))return;
        discardThen(()->myApplications.launch(new android.content.Intent(activity,
                vn.edu.ueh.nhom4.grandmaassist.app.ui.applications.MyApplicationsActivity.class)));
    }
    private void openPosts(){
        AuthRepository.State state=auth.getState().getValue();
        if(vm.busy||state.account==null||auth.methods().active()||vm.snapshot==null||!java.util.Arrays.asList("ADMIN","EMPLOYER").contains(vm.snapshot.role)||!"ACTIVE".equals(vm.snapshot.status))return;
        discardThen(()->jobDrafts.launch(new android.content.Intent(activity,vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.JobPostActivity.class)));
    }
    private void openDrafts(boolean create){
        AuthRepository.State state=auth.getState().getValue();
        if(state.phase!=AuthRepository.Phase.ACCOUNT || state.account==null || auth.methods().active())return;
        boolean offline=(vm.screen==ProfileViewModel.Screen.ERROR || vm.screen==ProfileViewModel.Screen.LOADING)
                && vm.offlineDraftAvailable && state.phase==AuthRepository.Phase.ACCOUNT && !auth.methods().active() && !vm.dirty;
        if(!offline && vm.busy)return;
        boolean online=vm.snapshot!=null && "EMPLOYER".equals(vm.snapshot.role) && "ACTIVE".equals(vm.snapshot.status);
        if(!online && !offline){
            if(vm.snapshot!=null && "EMPLOYER".equals(vm.snapshot.role))new AlertDialog.Builder(activity)
                    .setMessage(R.string.p_inactive).setPositiveButton(android.R.string.ok,null).show();
            return;
        }
        Runnable launch=()->jobDrafts.launch(new android.content.Intent(activity,
                vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.JobDraftActivity.class)
                .putExtra(vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.JobDraftActivity.CREATE_NEW,create));
        if(offline)launch.run();else discardThen(launch);
    }
    private void openVerification(){
        if(vm.busy || auth.methods().active() || auth.getState().getValue().phase!=AuthRepository.Phase.ACCOUNT || vm.snapshot==null)return;
        if(vm.dirty){new AlertDialog.Builder(activity).setMessage("Lưu hoặc bỏ các thay đổi hồ sơ trước khi mở xác minh.")
                .setPositiveButton(android.R.string.ok,null).show();return;}
        if("ADMIN".equals(vm.snapshot.role) && !"ACTIVE".equals(vm.snapshot.status)){
            new AlertDialog.Builder(activity).setMessage(R.string.p_inactive).setPositiveButton(android.R.string.ok,null).show();return;
        }
        boolean viewCurrent="SUBMITTED".equals(vm.verificationStatus) || "APPROVED".equals(vm.verificationStatus) || "VERIFIED".equals(vm.verificationStatus);
        verification.launch(new android.content.Intent(activity,
                vn.edu.ueh.nhom4.grandmaassist.app.ui.verification.VerificationActivity.class)
                .putExtra(vn.edu.ueh.nhom4.grandmaassist.app.ui.verification.VerificationActivity.OPEN_CURRENT,viewCurrent));
    }
    public void onForeground(){
        if(vm.destination==ProfileViewModel.Destination.PROFILE && !auth.methods().active()
                && !auth.getState().getValue().isBusy())vm.refreshVerification();
    }
    private View view(int id) { return activity.findViewById(id); }
    private void click(int id, Runnable action) { view(id).setOnClickListener(v -> action.run()); }
    private void signOut() {
        if (vm.busy) return;
        discardThen(() -> { vm.dirty = false; auth.signOut(); });
    }
    private void discardThen(Runnable action) {
        if (vm.busy) return;
        if (!vm.dirty) { action.run(); return; }
        new AlertDialog.Builder(activity).setMessage(R.string.p_unsaved)
                .setNegativeButton(R.string.p_keep, null)
                .setPositiveButton(R.string.p_discard, (dialog, which) -> { vm.discardEdits(); action.run(); }).show();
    }
    private void edit(int id, Consumer<String> update) {
        ((EditText) view(id)).addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) { }
            @Override public void afterTextChanged(Editable value) { }
            @Override public void onTextChanged(CharSequence value, int start, int before, int count) {
                if (rendering) return;
                update.accept(value.toString()); vm.edited();
            }
        });
    }
    private void check(int id, Consumer<Boolean> update) {
        ((CheckBox) view(id)).setOnCheckedChangeListener((button, selected) -> {
            if (!rendering) { update.accept(selected); vm.edited(); }
        });
    }
    private void set(int id, String value) {
        TextView text = (TextView) view(id);
        if (!text.getText().toString().equals(value)) text.setText(value);
    }
    private void text(int id, int res) { set(id, res == 0 ? "" : activity.getString(res)); }
    private void show(int id, boolean visible) { view(id).setVisibility(visible ? View.VISIBLE : View.GONE); }
    private void enable(View root, boolean value) {
        root.setEnabled(value);
        if (root instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) root).getChildCount(); i++)
            enable(((ViewGroup) root).getChildAt(i), value);
    }
    public void render(AuthRepository.State state) {
        if (rendering) return;
        boolean account = state.account != null;
        boolean methods = auth.methods().active();
        vm.bind(account, methods || state.checkingSession);
        rendering = true;
        boolean known=account && vm.snapshot!=null && (vm.screen==ProfileViewModel.Screen.PROFILE || vm.screen==ProfileViewModel.Screen.ADMIN);
        boolean home=known && !methods && vm.destination==ProfileViewModel.Destination.HOME;
        boolean hub=known && !methods && vm.destination==ProfileViewModel.Destination.ACCOUNT;
        boolean security=account && (methods || vm.accountPage);
        boolean profile=account && !home && !hub && !security;
        show(R.id.profileContent, profile);
        show(R.id.accountContent,security);
        show(R.id.methodsContent,security);
        if (!security && !state.error) {
            // Routine Auth diagnostics stay on the account screen, not on the form.
            show(R.id.authStatus, false);
            show(R.id.btnRetry, false);
        }
        show(R.id.btnReturnProfile,security && !methods);
        text(R.id.btnReturnProfile,vm.snapshot==null?R.string.n_back:R.string.n_back_account);
        show(R.id.btnProfileBackHome,known && profile);
        show(R.id.profileRole,false);
        boolean role = vm.screen == ProfileViewModel.Screen.ROLE;
        boolean editor = vm.screen == ProfileViewModel.Screen.PROFILE;
        show(R.id.rolePicker, role);
        show(R.id.profileEditor, editor);
        show(R.id.adminProfile, vm.screen == ProfileViewModel.Screen.ADMIN);
        show(R.id.profileVerificationSection,editor && vm.snapshot!=null);
        int message = vm.busy && vm.message == 0 ? R.string.p_loading : vm.message;
        text(R.id.profileMessage, message);
        show(R.id.profileMessage, message != 0);
        ViewCompat.setAccessibilityLiveRegion(view(R.id.profileMessage), ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        show(R.id.btnProfileReload, vm.screen == ProfileViewModel.Screen.ERROR || vm.screen==ProfileViewModel.Screen.ROLE || vm.readRequired);
        show(R.id.btnOfflineDrafts,account && (vm.screen==ProfileViewModel.Screen.ERROR || vm.screen==ProfileViewModel.Screen.LOADING) && vm.offlineDraftAvailable && !methods);
        view(R.id.btnOfflineDrafts).setEnabled(state.phase==AuthRepository.Phase.ACCOUNT && !methods && !vm.dirty);
        text(R.id.btnProfileReload,vm.screen==ProfileViewModel.Screen.ROLE?R.string.n_check_account:R.string.p_reload);
        enable(view(R.id.rolePicker), !vm.busy);
        for (int id : new int[]{R.id.btnRoleSeeker, R.id.btnRoleEmployer}) {
            boolean selected = vm.selectedRole.equals(id == R.id.btnRoleSeeker ? "SEEKER" : "EMPLOYER");
            view(id).setSelected(selected);
            ViewCompat.setStateDescription(view(id), selected ? activity.getString(R.string.p_selected) : null);
        }
        show(R.id.roleSelection, !vm.selectedRole.isEmpty());
        if (!vm.selectedRole.isEmpty()) set(R.id.roleSelection, activity.getString(R.string.p_selected) + ": "
                + activity.getString("SEEKER".equals(vm.selectedRole) ? R.string.p_seeker : R.string.p_employer));
        view(R.id.btnRoleContinue).setEnabled(!vm.busy && !vm.selectedRole.isEmpty());
        if (editor && vm.snapshot != null) {
            boolean seeker = "SEEKER".equals(vm.snapshot.role);
            boolean editable = !vm.busy && !vm.readRequired && "ACTIVE".equals(vm.snapshot.status);
            enable(view(R.id.profileEditor), editable);
            set(R.id.profileRole, activity.getString(R.string.p_role_fixed,
                    activity.getString(seeker ? R.string.p_seeker : R.string.p_employer)));
            ProfileDraft d = vm.draft;
            set(R.id.profileDisplayName, d.displayName); set(R.id.profileSkills, d.skills);
            set(R.id.profileExperience, d.experience); set(R.id.profileOrganization, d.organizationName);
            set(R.id.profileIntroduction, d.introduction);
            ((CheckBox) view(R.id.profile_morning)).setChecked(d.morning);
            ((CheckBox) view(R.id.profile_afternoon)).setChecked(d.afternoon);
            ((CheckBox) view(R.id.profile_evening)).setChecked(d.evening);
            ((RadioGroup) view(R.id.profileEmployerType)).check("ORGANIZATION".equals(d.employerType)
                    ? R.id.profile_organization : R.id.profile_individual);
            show(R.id.seekerFields, seeker); show(R.id.employerFields, !seeker);
            boolean organization = "ORGANIZATION".equals(d.employerType);
            show(R.id.profileOrganizationLabel, organization);
            show(R.id.profileOrganization, organization);
            view(R.id.profileOrganization).setEnabled(editable && organization);
            renderAreas();
            boolean hasPhone = !vm.currentPhone.isEmpty();
            set(R.id.profileContact, hasPhone ? vm.currentPhone : "Chưa có số điện thoại");
            TextView tvPhoneState = (TextView) view(R.id.profileContactState);
            if (hasPhone) {
                tvPhoneState.setText("✓ Đã xác minh");
                tvPhoneState.setTextColor(android.graphics.Color.parseColor("#2E7D32"));
                show(R.id.profileContactState, true);
                show(R.id.btnProfilePhone, false);
            } else {
                tvPhoneState.setText("Chưa xác minh");
                tvPhoneState.setTextColor(android.graphics.Color.parseColor("#E65100"));
                show(R.id.profileContactState, true);
                show(R.id.btnProfilePhone, true);
                text(R.id.btnProfilePhone, R.string.c03_add_phone);
                view(R.id.btnProfilePhone).setEnabled(!vm.busy && !state.checkingSession);
            }
            show(R.id.btnProfileSync, false);
            // Completeness/authorization explanations belong in help and validation, not
            // as permanent paragraphs between the form and its primary action.
            show(R.id.profileCompleteness, false);
            renderVerification(state);
        }
        for (int id : new int[]{R.id.btnProfileReload, R.id.btnProfileAccount, R.id.btnProfileWallet,
                R.id.btnProfileHelp, R.id.btnProfileSettings,
                R.id.btnProfileSignOut, R.id.btnReturnProfile,R.id.btnProfileBackHome}) {
            View v = view(id);
            if (v != null) v.setEnabled(!vm.busy);
        }
        if (vm.busy) for (int id : new int[]{R.id.btnSignOut, R.id.btnAccountPhone, R.id.btnAccountGoogle}) view(id).setEnabled(false);
        back.setEnabled(account && !methods && (vm.busy || vm.dirty || vm.destination!=ProfileViewModel.Destination.HOME));
        RoleNavigationBar.Action selected=vm.destination==ProfileViewModel.Destination.HOME?RoleNavigationBar.Action.HOME:
                vm.snapshot!=null && !"ADMIN".equals(vm.snapshot.role)?RoleNavigationBar.Action.PROFILE:RoleNavigationBar.Action.ACCOUNT;
        homePanel.render(state,known && !methods,home,hub,selected);
        if (!account) {
            for (int id : new int[]{R.id.profileDisplayName, R.id.profileSkills, R.id.profileExperience,
                    R.id.profileOrganization, R.id.profileIntroduction, R.id.profileContact, R.id.profileContactState,R.id.profileVerificationStatus}) set(id, "");
            renderedAreas = null; renderedAreaKey = null;
        }
        rendering = false;
    }
    private void renderVerification(AuthRepository.State state){
        int label=R.string.p_verification_unknown,button=R.string.p_reload;
        switch(vm.verificationStatus){
            case "NOT_SUBMITTED":label=R.string.p_verification_none;button=R.string.p_verify;break;
            case "SUBMITTED":label=R.string.p_verification_pending;button=R.string.p_verification_submitted;break;
            case "APPROVED":case "VERIFIED":label=R.string.p_verification_approved;button=R.string.p_verification_view;break;
            case "NEEDS_INFO":label=R.string.p_verification_needs_info;button=R.string.p_verification_supplement;break;
            case "REJECTED":label=R.string.p_verification_rejected;button=R.string.p_verification_update;break;
            case "REVERIFY_REQUIRED":label=R.string.p_verification_again;button=R.string.p_verification_retry;break;
        }
        if(vm.verificationFailed)label=R.string.p_verification_failed;
        set(R.id.profileVerificationStatus,vm.verificationLoading?activity.getString(R.string.p_verification_loading):
                activity.getString(R.string.p_verification_label,activity.getString(label)));
        boolean verified="APPROVED".equals(vm.verificationStatus) || "VERIFIED".equals(vm.verificationStatus);
        ((TextView)view(R.id.profileVerificationStatus)).setTextColor(androidx.core.content.ContextCompat.getColor(activity,
                !vm.verificationLoading && verified?R.color.brand_green:R.color.text_charcoal));
        text(R.id.btnProfileVerification,button);
        view(R.id.btnProfileVerification).setEnabled(!vm.busy && !vm.verificationLoading && !state.isBusy());
    }
    private void renderAreas() {
        if (renderedAreas != vm.areas || !vm.draft.areaCode.equals(renderedAreaKey)) {
            renderedAreas = vm.areas; renderedAreaKey = vm.draft.areaCode;
            List<String> names = new ArrayList<>(); areaCodes = new ArrayList<>();
            names.add(activity.getString(R.string.p_area_none)); areaCodes.add("");
            for (ProfileRepository.Area area : vm.areas) { names.add(area.name); areaCodes.add(area.code); }
            if (!areaCodes.contains(vm.draft.areaCode)) {
                names.add(activity.getString(R.string.p_area_inactive)); areaCodes.add(vm.draft.areaCode);
            }
            Spinner spinner = (Spinner) view(R.id.profileArea);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(activity, R.layout.profile_spinner_item, names);
            adapter.setDropDownViewResource(R.layout.profile_spinner_item);
            spinner.setAdapter(adapter); spinner.setSelection(areaCodes.indexOf(vm.draft.areaCode));
        }
        int areaMessage = vm.areaLoading && vm.areas.isEmpty() ? R.string.p_areas_loading : vm.areaMessage;
        text(R.id.profileAreaMessage, areaMessage);
        show(R.id.profileAreaMessage, areaMessage != 0);
        show(R.id.btnAreasReload, vm.areaMessage != 0);
        view(R.id.btnAreasReload).setEnabled(!vm.areaLoading && !vm.busy);
    }
}
