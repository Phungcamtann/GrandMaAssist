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

    public ProfilePanel(AppCompatActivity activity, ProfileViewModel vm, AuthViewModel auth) {
        this.activity = activity; this.vm = vm; this.auth = auth;
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
        click(R.id.btnAreasReload, vm::loadAreas);
        click(R.id.btnProfileSync, () -> discardThen(() -> { vm.dirty = false; vm.syncPhone(); }));
        click(R.id.btnProfileAccount, vm::showAccount);
        click(R.id.btnReturnProfile, () -> { vm.showProfile(); activity.findViewById(R.id.main).scrollTo(0, 0); });
        click(R.id.btnProfilePhone, () -> { vm.showAccount(); auth.beginAccountMethod(false); });
        click(R.id.btnProfileSignOut, this::signOut);
        click(R.id.btnSignOut, this::signOut);
        back = new OnBackPressedCallback(false) {
            @Override public void handleOnBackPressed() {
                if (vm.busy) {
                    new AlertDialog.Builder(activity).setMessage(R.string.p_busy).setPositiveButton(android.R.string.ok, null).show();
                } else if (vm.accountPage) vm.showProfile();
                else discardThen(() -> { vm.dirty = false; activity.finish(); });
            }
        };
        activity.getOnBackPressedDispatcher().addCallback(activity, back);
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
                .setPositiveButton(R.string.p_discard, (dialog, which) -> action.run()).show();
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
        boolean profile = account && !methods && !vm.accountPage;
        show(R.id.profileContent, profile);
        show(R.id.accountContent, account && !profile);
        show(R.id.methodsContent, account && !profile);
        if (profile && !state.error) {
            // Routine Auth diagnostics stay on the account screen, not on the form.
            show(R.id.authStatus, false);
            show(R.id.btnRetry, false);
        }
        show(R.id.btnReturnProfile, account && !methods);
        boolean role = vm.screen == ProfileViewModel.Screen.ROLE;
        boolean editor = vm.screen == ProfileViewModel.Screen.PROFILE;
        show(R.id.rolePicker, role);
        show(R.id.profileEditor, editor);
        show(R.id.adminProfile, vm.screen == ProfileViewModel.Screen.ADMIN);
        int message = vm.busy && vm.message == 0 ? R.string.p_loading : vm.message;
        text(R.id.profileMessage, message);
        show(R.id.profileMessage, message != 0);
        ViewCompat.setAccessibilityLiveRegion(view(R.id.profileMessage), ViewCompat.ACCESSIBILITY_LIVE_REGION_POLITE);
        show(R.id.btnProfileReload, vm.screen == ProfileViewModel.Screen.ERROR || vm.readRequired);
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
            set(R.id.profileContact, vm.currentPhone.isEmpty() ? activity.getString(R.string.p_no_phone)
                    : activity.getString(R.string.p_phone_value, vm.currentPhone));
            boolean synced = vm.snapshot.phone.equals(vm.currentPhone) && !vm.readRequired;
            text(R.id.profileContactState, synced ? 0 : R.string.p_phone_pending);
            show(R.id.profileContactState, !synced);
            text(R.id.btnProfilePhone, vm.currentPhone.isEmpty() ? R.string.c03_add_phone : R.string.c03_change_phone);
            view(R.id.btnProfilePhone).setEnabled(!vm.busy && !state.checkingSession);
            show(R.id.btnProfileSync, !synced);
            view(R.id.btnProfileSync).setEnabled(!vm.busy && !vm.readRequired);
            // Completeness/authorization explanations belong in help and validation, not
            // as permanent paragraphs between the form and its primary action.
            show(R.id.profileCompleteness, false);
        }
        for (int id : new int[]{R.id.btnProfileReload, R.id.btnProfileAccount,
                R.id.btnProfileSignOut, R.id.btnReturnProfile}) view(id).setEnabled(!vm.busy);
        if (vm.busy) for (int id : new int[]{R.id.btnSignOut, R.id.btnAccountPhone, R.id.btnAccountGoogle}) view(id).setEnabled(false);
        back.setEnabled(account && !methods && (vm.busy || vm.dirty || vm.accountPage));
        if (!account) {
            for (int id : new int[]{R.id.profileDisplayName, R.id.profileSkills, R.id.profileExperience,
                    R.id.profileOrganization, R.id.profileIntroduction, R.id.profileContact, R.id.profileContactState}) set(id, "");
            renderedAreas = null; renderedAreaKey = null;
        }
        rendering = false;
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
