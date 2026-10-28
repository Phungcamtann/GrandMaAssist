package vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation;

import android.content.Intent;
import android.graphics.Typeface;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.WorkSessionRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.domain.model.WorkSession;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.function.Consumer;
import vn.edu.ueh.nhom4.grandmaassist.app.R;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.AuthRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.data.repository.ProfileRepository;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.GuidedSearchActivity;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.PublicJobsActivity;
import vn.edu.ueh.nhom4.grandmaassist.app.ui.jobs.SavedJobsActivity;
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
        click(R.id.homeMainFuture,()->{
            if(vm.snapshot!=null && "EMPLOYER".equals(vm.snapshot.role)) createDraft.run();
            else if(vm.snapshot!=null && "SEEKER".equals(vm.snapshot.role)) {
                activity.startActivity(new Intent(activity, GuidedSearchActivity.class));
            }
        });
        click(R.id.homeAttentionFuture, () -> {
            if (vm.snapshot != null) {
                vn.edu.ueh.nhom4.grandmaassist.app.ui.sessions.WorkSessionsActivity.start(activity, vm.snapshot.role);
            }
        });
        click(R.id.homeWorkFuture,()->{if(vm.snapshot!=null && ("EMPLOYER".equals(vm.snapshot.role) || "SEEKER".equals(vm.snapshot.role)))navigate.accept(RoleNavigationBar.Action.WORK);});
        click(R.id.homeOtherFuture,()->{
            if(vm.snapshot!=null && "SEEKER".equals(vm.snapshot.role)) {
                activity.startActivity(new Intent(activity, PublicJobsActivity.class).putExtra(PublicJobsActivity.ROLE, "SEEKER"));
            } else if (vm.snapshot!=null && "EMPLOYER".equals(vm.snapshot.role)) {
                vn.edu.ueh.nhom4.grandmaassist.app.ui.sessions.WorkSessionsActivity.start(activity, "EMPLOYER");
            }
        });
        click(R.id.homeAudioGuide, () -> Toast.makeText(activity, "Đang phát hướng dẫn bằng giọng nói...", Toast.LENGTH_SHORT).show());
        click(R.id.homeHelpFuture, () -> CallCenterDialog.show(activity));
        click(R.id.accountHubBack,()->navigate.accept(RoleNavigationBar.Action.HOME));
        click(R.id.accountHubProfile,()->navigate.accept(RoleNavigationBar.Action.PROFILE));
        click(R.id.accountHubWallet, () -> {
            String role = vm.snapshot != null ? vm.snapshot.role : "EMPLOYER";
            vn.edu.ueh.nhom4.grandmaassist.app.ui.wallet.WalletActivity.start(activity, role, 0L);
        });
        click(R.id.accountHubSavedJobs, () -> activity.startActivity(new Intent(activity, SavedJobsActivity.class)));
        click(R.id.accountHubVerification,verify);click(R.id.accountHubSecurity,security);click(R.id.accountHubSignOut,signOut);
    }
    private void click(int id,Runnable action){
        View v = activity.findViewById(id);
        if (v != null) v.setOnClickListener(view -> action.run());
    }
    private void show(int id,boolean value){
        View v = activity.findViewById(id);
        if (v != null) v.setVisibility(value?View.VISIBLE:View.GONE);
    }
    private void text(int id,String value){
        TextView view=activity.findViewById(id);
        if(view != null && !view.getText().toString().equals(value))view.setText(value);
    }
    private void future(int id,int label){text(id,activity.getString(label));}

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
        boolean admin="ADMIN".equals(role),seeker="SEEKER".equals(role),employer="EMPLOYER".equals(role);
        boolean active="ACTIVE".equals(vm.snapshot.status);

        if (seeker) {
            text(R.id.homeTitle, "Chào cô chú");
            text(R.id.homeIntro, activity.getString(R.string.gs_seeker_home_intro));
            show(R.id.homeIntro, true);
            show(R.id.homeAudioGuide, false);

            text(R.id.homeMainFuture, activity.getString(R.string.gs_seeker_help_me));
            activity.findViewById(R.id.homeMainFuture).setEnabled(!vm.busy && !auth.isBusy());
            activity.findViewById(R.id.homeMainFuture).setBackgroundResource(R.drawable.button_primary);
            ((TextView)activity.findViewById(R.id.homeMainFuture)).setTextColor(androidx.core.content.ContextCompat.getColor(activity, R.color.white));
            ((androidx.appcompat.widget.AppCompatButton)activity.findViewById(R.id.homeMainFuture)).setCompoundDrawableTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));

            text(R.id.homeAttentionTitle, "Việc làm sắp tới");
            text(R.id.homeAttentionCount, "Chưa có việc sắp tới");
            text(R.id.homeWorkFuture, activity.getString(R.string.gs_seeker_my_jobs));
            text(R.id.homeOtherFuture, "Xem các việc khác");
            text(R.id.homeHelpFuture, activity.getString(R.string.gs_seeker_support));

            activity.findViewById(R.id.homeAttentionFuture).setEnabled(!vm.busy && !auth.isBusy());
            activity.findViewById(R.id.homeWorkFuture).setEnabled(!vm.busy && !auth.isBusy());
            ((TextView)activity.findViewById(R.id.homeWorkFuture)).setTextColor(androidx.core.content.ContextCompat.getColor(activity, R.color.brand_green));
            activity.findViewById(R.id.homeOtherFuture).setEnabled(!vm.busy && !auth.isBusy());
            ((TextView)activity.findViewById(R.id.homeOtherFuture)).setTextColor(androidx.core.content.ContextCompat.getColor(activity, R.color.brand_green));
            show(R.id.homeOtherFuture, true);
            int[] seekerBigButtons = new int[]{R.id.homeMainFuture, R.id.homeWorkFuture, R.id.homeOtherFuture};
            for (int bid : seekerBigButtons) {
                View b = activity.findViewById(bid);
                if (b instanceof TextView) {
                    ((TextView) b).setTextSize(20f);
                    ((TextView) b).setTypeface(null, Typeface.BOLD);
                    b.setMinimumHeight(Math.round(80 * activity.getResources().getDisplayMetrics().density));
                }
            }
        } else {
            show(R.id.homeAudioGuide, false);
            text(R.id.homeTitle,activity.getString(admin?R.string.n_admin_title:R.string.n_employer_title));
            text(R.id.homeIntro,admin?"":activity.getString(R.string.n_employer_intro));
            show(R.id.homeIntro,!admin);
            future(R.id.homeMainFuture,R.string.n_create_post);
            text(R.id.homeAttentionTitle, activity.getString(R.string.n_attention));
            text(R.id.homeAttentionCount, "Chưa có việc sắp tới");
            future(R.id.homeWorkFuture,R.string.n_my_posts);
            future(R.id.homeOtherFuture,R.string.n_sessions);
            future(R.id.homeHelpFuture,R.string.n_help);
        }

        show(R.id.homeMember,!admin);show(R.id.homeAdmin,admin);

        if(employer){
            text(R.id.homeTitle, "Bạn đang muốn tìm người giúp việc ?");
            boolean employerVerified = (vm.snapshot != null && "VERIFIED".equals(vm.snapshot.verificationStatus)) || "VERIFIED".equals(vm.verificationStatus);
            show(R.id.homeIntro, !employerVerified);
            text(R.id.homeMainFuture,activity.getString(R.string.jd_create));
            text(R.id.homeWorkFuture,activity.getString(R.string.n_my_posts));
            text(R.id.homeAttentionTitle, "Việc làm sắp tới");
            text(R.id.homeAttentionCount, "Chưa có việc sắp tới");
            show(R.id.homeOtherFuture, false);
            activity.findViewById(R.id.homeMainFuture).setEnabled(active && !vm.busy && !auth.isBusy());
            activity.findViewById(R.id.homeMainFuture).setBackgroundResource(R.drawable.button_primary);
            ((TextView)activity.findViewById(R.id.homeMainFuture)).setTextColor(androidx.core.content.ContextCompat.getColor(activity, R.color.button_primary_text));
            activity.findViewById(R.id.homeAttentionFuture).setEnabled(active && !vm.busy && !auth.isBusy());
            activity.findViewById(R.id.homeWorkFuture).setEnabled(active && !vm.busy && !auth.isBusy());
            ((TextView)activity.findViewById(R.id.homeWorkFuture)).setTextColor(androidx.core.content.ContextCompat.getColor(activity, R.color.brand_green));
        }

        future(R.id.homePostsFuture,R.string.n_admin_posts);future(R.id.homeReportsFuture,R.string.n_admin_reports);
        future(R.id.homeDisputesFuture,R.string.n_admin_disputes);future(R.id.homeOverdueFuture,R.string.n_admin_overdue);
        

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
        text(R.id.accountHubArea,area);show(R.id.accountHubArea,!area.isEmpty());show(R.id.accountHubProfile,false);

        String authMethod = "";
        if (auth != null && auth.account != null) {
            if (auth.account.google && auth.account.googleEmail != null && !auth.account.googleEmail.isEmpty()) {
                authMethod += "Google: " + auth.account.googleEmail;
            }
            if (auth.account.phone && auth.account.phoneNumber != null && !auth.account.phoneNumber.isEmpty()) {
                if (!authMethod.isEmpty()) authMethod += "\n";
                authMethod += "Số điện thoại: " + auth.account.phoneNumber;
            }
            if (authMethod.isEmpty() && auth.account.email != null && !auth.account.email.isEmpty()) {
                authMethod = "Email: " + auth.account.email;
            }
        }
        if (authMethod.isEmpty()) authMethod = "Đăng nhập an toàn";
        text(R.id.accountHubAuthMethod, authMethod);

        show(R.id.accountHubSavedJobs,false);
        show(R.id.accountHubVerification,false);
        for(int id:new int[]{R.id.homeOpenProfile,R.id.accountHubProfile,R.id.accountHubWallet,R.id.accountHubSavedJobs,R.id.accountHubSecurity,R.id.accountHubBack,R.id.accountHubSignOut}) {
            View v = activity.findViewById(id);
            if (v != null) v.setEnabled(!vm.busy && !auth.isBusy());
        }
        View ver = activity.findViewById(R.id.accountHubVerification);
        if (ver != null) ver.setEnabled(!vm.busy && !auth.isBusy() && active);
        View helpBtn = activity.findViewById(R.id.homeHelpFuture);
        if (helpBtn != null) {
            helpBtn.setEnabled(!vm.busy && !auth.isBusy());
            if (helpBtn instanceof TextView) {
                ((TextView) helpBtn).setTextColor(androidx.core.content.ContextCompat.getColor(activity, R.color.brand_green));
            }
        }

        if (auth.account != null && auth.account.uid != null && !auth.account.uid.isEmpty() && (seeker || employer)) {
            new WorkSessionRepository()
                    .getSessionsForUser(auth.account.uid, role)
                    .addOnSuccessListener(sessions -> {
                        if (activity.isFinishing() || activity.isDestroyed()) return;
                        int count = 0;
                        if (sessions != null) {
                            for (WorkSession s : sessions) {
                                if (!s.isCompleted() && !s.isCancelled() && !WorkSession.STATUS_DISPUTED.equals(s.status)) {
                                    boolean bothConfirmed = s.isReady() || s.isInProgress() || s.isCompletionPending()
                                            || (s.employerAcceptedVersion != null && s.seekerAcceptedVersion != null
                                                && s.employerAcceptedVersion.equals(s.agreementVersion) && s.seekerAcceptedVersion.equals(s.agreementVersion));
                                    if (bothConfirmed) {
                                        count++;
                                    }
                                }
                            }
                        }
                        String countText = count > 0 ? ("Có " + count + " việc sắp tới") : "Chưa có việc sắp tới";
                        text(R.id.homeAttentionTitle, "Việc làm sắp tới");
                        text(R.id.homeAttentionCount, countText);
                    });
        }
    }
    private String capped(long count){return count>=50?"50+":String.valueOf(count);}
}
