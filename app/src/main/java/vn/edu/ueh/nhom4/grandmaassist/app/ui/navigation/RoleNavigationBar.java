package vn.edu.ueh.nhom4.grandmaassist.app.ui.navigation;

import android.app.Activity;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.GridLayout;
import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import java.util.function.Consumer;
import vn.edu.ueh.nhom4.grandmaassist.app.R;

/** Same role-aware navigation on main and verification screens; future routes stay disabled. */
public final class RoleNavigationBar {
    public enum Action { HOME, WORK, OTHER, PROFILE, PROCESS, STATS, ACCOUNT }
    public static final String RETURN_DESTINATION="return_destination";
    private final Activity activity;
    private final GridLayout grid;
    private final View host;
    private final AppCompatButton[] buttons=new AppCompatButton[4];
    private final Action[] actions=new Action[4];
    private final Consumer<Action> onAction;
    private int columns=-1;
    public RoleNavigationBar(Activity activity,Consumer<Action> onAction){
        this.activity=activity;this.onAction=onAction;grid=activity.findViewById(R.id.roleNavGrid);host=activity.findViewById(R.id.roleNavHost);
        host.setSaveFromParentEnabled(false);
        for(int i=0;i<4;i++){
            final int index=i;AppCompatButton b=new AppCompatButton(activity);buttons[i]=b;
            b.setAllCaps(false);b.setTextSize(16);b.setMinWidth(0);b.setMinimumWidth(0);b.setMinHeight(dp(64));
            b.setPadding(dp(4),dp(8),dp(4),dp(8));b.setGravity(Gravity.CENTER);b.setSaveEnabled(false);
            b.setBackgroundResource(R.drawable.navigation_item);b.setSupportBackgroundTintList(null);
            b.setOnClickListener(v->{if(actions[index]!=null)this.onAction.accept(actions[index]);});grid.addView(b);
        }
        grid.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->layoutColumns());
        host.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->{
            if(!(host.getParent() instanceof View))return;
            View parent=(View)host.getParent();
            android.view.ViewGroup.MarginLayoutParams params=(android.view.ViewGroup.MarginLayoutParams)host.getLayoutParams();
            int available=parent.getWidth()-parent.getPaddingLeft()-parent.getPaddingRight()-params.leftMargin-params.rightMargin;
            if(available<=0)return;
            int width=Math.min(available,activity.getResources().getDimensionPixelSize(R.dimen.content_max_width));
            if(params.width!=width){params.width=width;host.setLayoutParams(params);}
        });layoutColumns();
    }
    public static int roleText(String role){
        return "ADMIN".equals(role)?R.string.n_admin:"SEEKER".equals(role)?R.string.n_seeker:
                "EMPLOYER".equals(role)?R.string.n_employer:R.string.n_common;
    }
    public static void topbar(Activity activity,String role){
        TextView label=activity.findViewById(R.id.roleTopbarLabel);label.setText(roleText(role));
        label.setContentDescription("Vai trò: "+activity.getString(roleText(role)));label.setTooltipText(activity.getString(roleText(role)));
        View logo=activity.findViewById(R.id.roleTopbarLogo);
        int width=activity.getResources().getConfiguration().fontScale>=1.5f
                ?Math.round(80*activity.getResources().getDisplayMetrics().density)
                :activity.getResources().getDimensionPixelSize(R.dimen.brand_logo_width);
        if(logo.getLayoutParams().width!=width){logo.getLayoutParams().width=width;logo.setLayoutParams(logo.getLayoutParams());}
    }
    public void render(String role,Action selected,boolean visible,boolean enabled){
        host.setVisibility(visible?View.VISIBLE:View.GONE);if(!visible)return;
        boolean admin="ADMIN".equals(role),seeker="SEEKER".equals(role);
        Action[] routes=admin?new Action[]{Action.HOME,Action.PROCESS,Action.STATS,Action.ACCOUNT}
                :new Action[]{Action.HOME,Action.WORK,Action.OTHER,seeker?Action.PROFILE:Action.ACCOUNT};
        int[] labels=admin?new int[]{R.string.n_overview,R.string.n_process,R.string.n_stats,R.string.n_account}
                :seeker?new int[]{R.string.n_home,R.string.n_my_jobs,R.string.n_other_jobs,R.string.n_profile}
                :new int[]{R.string.n_home,R.string.n_my_posts,R.string.n_sessions,R.string.n_account};
        int[] icons=admin?new int[]{R.drawable.ic_nav_home,R.drawable.ic_nav_queue,R.drawable.ic_nav_stats,R.drawable.ic_nav_account}
                :new int[]{R.drawable.ic_nav_home,R.drawable.ic_role_work,R.drawable.ic_nav_search,R.drawable.ic_nav_account};
        for(int i=0;i<4;i++){
            Action action=routes[i];actions[i]=action;
            boolean live=i==0 || i==3 || (seeker && i==2) || ((admin || "EMPLOYER".equals(role)) && i==1);AppCompatButton b=buttons[i];
            String title=activity.getString(labels[i]);String value=title+(live?"":"\n"+activity.getString(R.string.n_soon));
            if(!b.getText().toString().equals(value))b.setText(value);
            b.setEnabled(enabled && live);b.setSelected(live && selected==action);
            b.setTypeface(ResourcesCompat.getFont(activity,selected==action?R.font.be_vietnam_pro_semibold:R.font.be_vietnam_pro_regular),Typeface.NORMAL);
            b.setTextColor(ContextCompat.getColor(activity,live?R.color.brand_green:R.color.button_disabled_text));
            b.setAlpha(live?1f:.6f);
            Drawable icon=ContextCompat.getDrawable(activity,icons[i]);
            if(icon!=null){icon=icon.mutate();icon.setTint(ContextCompat.getColor(activity,live?R.color.brand_green:R.color.button_disabled_text));
                icon.setBounds(0,0,dp(20),dp(20));}
            b.setCompoundDrawables(null,icon,null,null);b.setCompoundDrawablePadding(dp(4));
            ViewCompat.setStateDescription(b,!live?activity.getString(R.string.n_soon):selected==action?activity.getString(R.string.n_current):null);
        }
        layoutColumns();
    }
    private void layoutColumns(){
        float density=activity.getResources().getDisplayMetrics().density;
        float width=grid.getWidth()>0?grid.getWidth()/density:activity.getResources().getConfiguration().screenWidthDp-24;
        int next=activity.getResources().getConfiguration().fontScale>=1.3f || width<300?2:4;
        if(next==columns)return;columns=next;
        for(AppCompatButton button:buttons){
            GridLayout.LayoutParams old=(GridLayout.LayoutParams)button.getLayoutParams();
            old.columnSpec=GridLayout.spec(GridLayout.UNDEFINED);old.rowSpec=GridLayout.spec(GridLayout.UNDEFINED);button.setLayoutParams(old);
        }
        grid.setColumnCount(columns);
        for(int i=0;i<buttons.length;i++){
            GridLayout.LayoutParams p=new GridLayout.LayoutParams(GridLayout.spec(i/columns),GridLayout.spec(i%columns,1f));
            p.width=0;p.height=GridLayout.LayoutParams.WRAP_CONTENT;p.setGravity(Gravity.FILL_HORIZONTAL);p.setMargins(dp(2),dp(2),dp(2),dp(2));
            buttons[i].setLayoutParams(p);
        }
    }
    private int dp(int value){return Math.round(value*activity.getResources().getDisplayMetrics().density);}
}
