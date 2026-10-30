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
        click(R.id.homeAdminPostsCard, reviewPosts);
        click(R.id.homeAdminProfilesCard, verify);
        click(R.id.homeAdminReload, vm::loadCounts);
        click(R.id.homeAdminCardEmployers, verify);
        click(R.id.homeAdminCardSeekers, verify);
        click(R.id.homeAdminCardJobs, reviewPosts);
        click(R.id.homeAdminCardFee, () -> activity.startActivity(new Intent(activity, vn.edu.ueh.nhom4.grandmaassist.app.ui.admin.AdminStatsActivity.class)));
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
            for(int id:new int[]{R.id.accountHubName,R.id.accountHubRole,R.id.accountHubArea,R.id.homeAdminPostsCount,R.id.homeAdminProfilesCount,
                    R.id.homeAdminEmployersCount,R.id.homeAdminSeekersCount,R.id.homeAdminJobsCount,R.id.homeAdminFeeCount,R.id.homeAdminUpdatedTime})text(id,"");return;
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

        show(R.id.homeAdminReload, admin);
        if(admin){
            View reloadBtn = activity.findViewById(R.id.homeAdminReload);
            if (reloadBtn != null) reloadBtn.setEnabled(!vm.busy && !vm.countsLoading);

            View postsCard = activity.findViewById(R.id.homeAdminPostsCard);
            if (postsCard != null) postsCard.setEnabled(active && !vm.busy && !auth.isBusy());

            View profilesCard = activity.findViewById(R.id.homeAdminProfilesCard);
            if (profilesCard != null) profilesCard.setEnabled(active && !vm.busy && !auth.isBusy());

            int[] adminCards = new int[]{R.id.homeAdminCardEmployers, R.id.homeAdminCardSeekers,
                    R.id.homeAdminCardJobs, R.id.homeAdminCardFee};
            for (int cid : adminCards) {
                View cv = activity.findViewById(cid);
                if (cv != null) cv.setEnabled(active && !vm.busy && !auth.isBusy());
            }

            if (vm.countsLoading) {
                text(R.id.homeAdminUpdatedTime, "Đang tổng hợp số liệu thực tế...");
                text(R.id.homeAdminEmployersCount, "...");
                text(R.id.homeAdminSeekersCount, "...");
                text(R.id.homeAdminJobsCount, "...");
                text(R.id.homeAdminFeeCount, "...");
                text(R.id.homeAdminPostsCount, "Đang kiểm tra tin chờ duyệt...");
                text(R.id.homeAdminProfilesCount, "Đang kiểm tra hồ sơ...");
            } else if (vm.adminCounts != null) {
                java.text.NumberFormat vnFormat = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
                text(R.id.homeAdminEmployersCount, String.valueOf(vm.adminCounts.totalEmployers));
                text(R.id.homeAdminEmployersVerified, "Đã xác minh: " + vm.adminCounts.verifiedEmployers);

                text(R.id.homeAdminSeekersCount, String.valueOf(vm.adminCounts.totalSeekers));
                text(R.id.homeAdminSeekersVerified, "Đã duyệt CCCD: " + vm.adminCounts.verifiedSeekers);

                text(R.id.homeAdminJobsCount, String.valueOf(vm.adminCounts.totalJobs));
                text(R.id.homeAdminJobsOpen, "Đang mở: " + vm.adminCounts.openJobs);

                text(R.id.homeAdminFeeCount, vnFormat.format(vm.adminCounts.totalFee) + " đ");
                text(R.id.homeAdminFeeCompleted, vm.adminCounts.completedSessions + " ca hoàn tất");

                long pendingJobs = vm.adminCounts.pendingJobs;
                long pendingProfiles = vm.adminCounts.seeker + vm.adminCounts.employer;
                text(R.id.homeAdminPostsCount, pendingJobs > 0 ? ("Hiện có " + pendingJobs + " tin đang chờ duyệt") : "Hiện không có tin chờ duyệt");
                text(R.id.homeAdminProfilesCount, pendingProfiles > 0 ? ("Hiện có " + pendingProfiles + " hồ sơ chờ duyệt") : "Hiện không có hồ sơ chờ duyệt");

                String timeStr = java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT).format(new java.util.Date());
                text(R.id.homeAdminUpdatedTime, "Cập nhật lúc: " + timeStr + " • Tự động tính theo thời gian thực");
            } else if (vm.countsMessage != 0) {
                text(R.id.homeAdminUpdatedTime, activity.getString(vm.countsMessage));
                text(R.id.homeAdminPostsCount, activity.getString(vm.countsMessage));
                text(R.id.homeAdminProfilesCount, activity.getString(vm.countsMessage));
            } else {
                text(R.id.homeAdminUpdatedTime, "Chưa cập nhật được số liệu");
                text(R.id.homeAdminPostsCount, "Chưa cập nhật được số lượng");
                text(R.id.homeAdminProfilesCount, "Chưa cập nhật được số lượng");
            }
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
            String googleText = auth.account.google && auth.account.googleEmail != null && !auth.account.googleEmail.isEmpty()
                    ? "✓ Google: " + auth.account.googleEmail
                    : "• Google: Chưa liên kết";
            String phoneText = auth.account.phone && auth.account.phoneNumber != null && !auth.account.phoneNumber.isEmpty()
                    ? "✓ Số điện thoại: " + auth.account.phoneNumber
                    : "• Số điện thoại: Chưa liên kết";
            authMethod = googleText + "\n" + phoneText;
        }
        if (authMethod.isEmpty()) authMethod = "Đăng nhập an toàn";
        text(R.id.accountHubAuthMethod, authMethod);

        View btnSec = activity.findViewById(R.id.accountHubSecurity);
        if (btnSec instanceof android.widget.TextView && auth != null && auth.account != null) {
            if (auth.account.google && auth.account.phone) {
                ((android.widget.TextView) btnSec).setText("🔗  Quản lý phương thức đăng nhập");
            } else if (!auth.account.google) {
                ((android.widget.TextView) btnSec).setText("🔗  Liên kết thêm tài khoản Google");
            } else if (!auth.account.phone) {
                ((android.widget.TextView) btnSec).setText("🔗  Liên kết thêm Số điện thoại");
            }
        }

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
                                if (s.isCompleted() && s.jobId != null && !s.jobId.isEmpty()) {
                                    if (employer) {
                                        new vn.edu.ueh.nhom4.grandmaassist.app.data.repository.JobPostRepository().closeJobById(auth.account.uid, s.jobId);
                                    }
                                    vn.edu.ueh.nhom4.grandmaassist.app.data.local.PublicJobStore.get(activity).remove(s.jobId);
                                }
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
