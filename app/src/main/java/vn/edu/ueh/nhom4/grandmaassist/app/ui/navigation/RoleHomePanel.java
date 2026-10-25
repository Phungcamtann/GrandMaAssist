package vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation;

import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.function.Consumer;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.AuthRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.ProfileRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.viewmodel.ProfileViewModel;

/** Presentation only. Navigation delegates to the profile owner so dirty forms are protected. */
public final class RoleHomePanel {
    private final AppCompatActivity activity;
    private final ProfileViewModel vm;
    private final RoleNavigationBar navigation;
    public RoleHomePanel(AppCompatActivity activity,ProfileViewModel vm,Consumer<RoleNavigationBar.Action> navigate,
                         Runnable verify,Runnable security,Runnable signOut,Runnable createDraft,Runnable reviewPosts){
        this.activity=activity;this.vm=vm;navigation=new RoleNavigationBar(activity,navigate);
        activity.findViewById(R.id.roleHomeContent).setSaveFromParentEnabled(false);
        activity.findViewById(R.id.roleAccountContent).setSaveFromParentEnabled(false);
        click(R.id.homeOpenProfile,()->navigate.accept(RoleNavigationBar.Action.PROFILE));
        click(R.id.homePostsFuture,reviewPosts);
        click(R.id.homeReviewProfiles,verify);click(R.id.homeRefreshCounts,vm::loadCounts);
        click(R.id.homeMainFuture,()->{if(vm.snapshot!=null && "EMPLOYER".equals(vm.snapshot.role))createDraft.run();});
        click(R.id.homeWorkFuture,()->{if(vm.snapshot!=null && "EMPLOYER".equals(vm.snapshot.role))navigate.accept(RoleNavigationBar.Action.WORK);});
        click(R.id.accountHubBack,()->navigate.accept(RoleNavigationBar.Action.HOME));
        click(R.id.accountHubProfile,()->navigate.accept(RoleNavigationBar.Action.PROFILE));
        click(R.id.accountHubVerification,verify);click(R.id.accountHubSecurity,security);click(R.id.accountHubSignOut,signOut);
    }
    private void click(int id,Runnable action){activity.findViewById(id).setOnClickListener(v->action.run());}
    private void show(int id,boolean value){activity.findViewById(id).setVisibility(value?View.VISIBLE:View.GONE);}
    private void text(int id,String value){TextView view=activity.findViewById(id);if(!view.getText().toString().equals(value))view.setText(value);}
    private void future(int id,int label){text(id,activity.getString(label)+"\n"+activity.getString(R.string.n_soon));}
    public void render(AuthRepository.State auth,boolean known,boolean home,boolean hub,RoleNavigationBar.Action selected){
        String role=auth.account!=null && vm.snapshot!=null?vm.snapshot.role:"";
        RoleNavigationBar.topbar(activity,role);
        show(R.id.roleHomeContent,home);show(R.id.roleAccountContent,hub);
        WindowInsetsCompat insets=ViewCompat.getRootWindowInsets(activity.findViewById(R.id.appRoot));
        boolean keyboard=insets!=null && insets.isVisible(WindowInsetsCompat.Type.ime());
        navigation.render(role,selected,known && !keyboard && !auth.isBusy(),!vm.busy);
        if(!known){
            for(int id:new int[]{R.id.accountHubName,R.id.accountHubRole,R.id.accountHubArea,R.id.homeQueueCounts})text(id,"");return;
        }
        boolean admin="ADMIN".equals(role),seeker="SEEKER".equals(role);
        boolean active="ACTIVE".equals(vm.snapshot.status);
        text(R.id.homeTitle,activity.getString(admin?R.string.n_admin_title:seeker?R.string.n_seeker_title:R.string.n_employer_title));
        text(R.id.homeIntro,admin?"":activity.getString(seeker?R.string.n_seeker_intro:R.string.n_employer_intro));show(R.id.homeIntro,!admin);
        show(R.id.homeMember,!admin);show(R.id.homeAdmin,admin);
        future(R.id.homeMainFuture,seeker?R.string.n_find_jobs:R.string.n_create_post);
        future(R.id.homeAttentionFuture,R.string.n_attention);future(R.id.homeWorkFuture,seeker?R.string.n_my_jobs:R.string.n_my_posts);
        future(R.id.homeOtherFuture,seeker?R.string.n_other_jobs:R.string.n_sessions);future(R.id.homeHelpFuture,R.string.n_help);
        boolean employer="EMPLOYER".equals(role);
        if(employer){text(R.id.homeMainFuture,activity.getString(R.string.jd_create));text(R.id.homeWorkFuture,activity.getString(R.string.n_my_posts));}
        activity.findViewById(R.id.homeMainFuture).setEnabled(employer && active && !vm.busy && !auth.isBusy());
        activity.findViewById(R.id.homeMainFuture).setBackgroundResource(employer?R.drawable.button_primary:R.drawable.verification_section);
        ((TextView)activity.findViewById(R.id.homeMainFuture)).setTextColor(androidx.core.content.ContextCompat.getColor(activity,
                employer?R.color.button_primary_text:R.color.button_disabled_text));
        activity.findViewById(R.id.homeWorkFuture).setEnabled(employer && active && !vm.busy && !auth.isBusy());
        ((TextView)activity.findViewById(R.id.homeWorkFuture)).setTextColor(androidx.core.content.ContextCompat.getColor(activity,
                employer?R.color.brand_green:R.color.button_disabled_text));
        future(R.id.homePostsFuture,R.string.n_admin_posts);future(R.id.homeReportsFuture,R.string.n_admin_reports);
        future(R.id.homeDisputesFuture,R.string.n_admin_disputes);future(R.id.homeOverdueFuture,R.string.n_admin_overdue);
        future(R.id.accountHubSettings,R.string.n_settings);future(R.id.accountHubHelp,R.string.n_help);
        if(admin){
            text(R.id.homePostsFuture,"Duyệt tin tuyển");activity.findViewById(R.id.homePostsFuture).setEnabled(active&&!vm.busy&&!auth.isBusy());
            ((TextView)activity.findViewById(R.id.homePostsFuture)).setTextColor(androidx.core.content.ContextCompat.getColor(activity,R.color.brand_green));
            text(R.id.homeQueueTitle,activity.getString(R.string.n_queue_title));
            if(vm.countsLoading)text(R.id.homeQueueCounts,activity.getString(R.string.n_queue_loading));
            else if(vm.countsMessage!=0)text(R.id.homeQueueCounts,activity.getString(vm.countsMessage));
            else if(vm.adminCounts!=null){
                ProfileRepository.AdminCounts counts=vm.adminCounts;
                String total=String.valueOf(counts.seeker+counts.employer)+((counts.seeker>=50 || counts.employer>=50)?"+":"");
                text(R.id.homeQueueTitle,activity.getString(R.string.n_queue_total,total));
                text(R.id.homeQueueCounts,activity.getString(R.string.n_queue_types,capped(counts.seeker),capped(counts.employer)));
            } else text(R.id.homeQueueCounts,activity.getString(R.string.n_queue_failed));
            activity.findViewById(R.id.homeReviewProfiles).setEnabled(!vm.busy && active);
            activity.findViewById(R.id.homeRefreshCounts).setEnabled(!vm.busy && !vm.countsLoading && active);
        }
        String name=vm.snapshot.draft.displayName;
        if(!seeker && !admin && "ORGANIZATION".equals(vm.snapshot.draft.employerType) && !vm.snapshot.draft.organizationName.isEmpty())name=vm.snapshot.draft.organizationName;
        if(name.isEmpty())name=auth.account.name==null || auth.account.name.isEmpty()?activity.getString(R.string.n_account_title):auth.account.name;
        text(R.id.accountHubName,name);
        text(R.id.accountHubRole,activity.getString(RoleNavigationBar.roleText(role))+("EMPLOYER".equals(role)?" · "+
                activity.getString("ORGANIZATION".equals(vm.snapshot.draft.employerType)?R.string.n_organization:R.string.n_individual):""));
        String area="";for(ProfileRepository.Area item:vm.areas)if(item.code.equals(vm.snapshot.draft.areaCode)){area=item.name;break;}
        text(R.id.accountHubArea,area);show(R.id.accountHubArea,!area.isEmpty());show(R.id.accountHubProfile,!admin);
        show(R.id.accountHubVerification,admin);
        for(int id:new int[]{R.id.homeOpenProfile,R.id.accountHubProfile,R.id.accountHubSecurity,R.id.accountHubBack,R.id.accountHubSignOut})
            activity.findViewById(id).setEnabled(!vm.busy && !auth.isBusy());
        activity.findViewById(R.id.accountHubVerification).setEnabled(!vm.busy && !auth.isBusy() && active);
    }
    private String capped(long count){return count>=50?"50+":String.valueOf(count);}
}
