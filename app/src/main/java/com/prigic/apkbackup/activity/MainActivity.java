package com.prigic.apkbackup.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.prigic.apkbackup.R;
import com.prigic.apkbackup.data.Layout;
import com.prigic.apkbackup.data.PermissionUtil;
import com.prigic.apkbackup.data.SharedPref;
import com.prigic.apkbackup.data.Utils;

public class MainActivity extends AppCompatActivity {
    public BackupActivity frag_backup;
    private RestoreActivity frag_restore;

    private ViewPager viewPager;
    private ActionBar actionBar;
    private Toolbar toolbar;

    private SearchView search;
    private FloatingActionButton fab;
    private TabLayout tabLayout;
    private SharedPref sharedPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sharedPref = new SharedPref(this);
        initComponent();
        initToolbar();
        onCreateProcess();
    }

    private void initComponent() {
        viewPager = findViewById(R.id.viewpager);
        fab = findViewById(R.id.fab);
        tabLayout = findViewById(R.id.tabs);

        Layout adapter = new Layout(getSupportFragmentManager());

        if (frag_backup == null) {
            frag_backup = new BackupActivity();
        }
        if (frag_restore == null) {
            frag_restore = new RestoreActivity();
        }
        adapter.addFragment(frag_backup, getString(R.string.tab_title_backup));
        adapter.addFragment(frag_restore, getString(R.string.tab_title_restore));
        viewPager.setAdapter(adapter);
        viewPager.setOffscreenPageLimit(2);
        viewPager.setCurrentItem(0);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (onCreateProcess()) {
                    frag_backup.refresh(true);
                }
            }
        });
        viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // close contextual action mode
                if (frag_backup.getActionMode() != null) {
                    frag_backup.getActionMode().finish();
                }
                if (frag_restore.getActionMode() != null) {
                    frag_restore.getActionMode().finish();
                }

                if (position == 0) {
                    fab.show();
                } else if (position == 1) {
                    frag_restore.refreshList();
                    fab.hide();
                }
                search.onActionViewCollapsed();
                supportInvalidateOptionsMenu();
            }
        });

        tabLayout.setupWithViewPager(viewPager);
    }


    private boolean onCreateProcess() {
        if (Utils.needRequestPermission()) {
            String[] permission = PermissionUtil.getDeniedPermission(this);
            if (permission.length != 0) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(permission, 200);
                }
                if (sharedPref.getNeverAskAgain(PermissionUtil.STORAGE)) {
                    PermissionUtil.showDialogPermission(this);
                }
                return false;
            } else {
                return true;
            }
        } else {
            return true;
        }
    }

    public Toolbar getToolbar() {
        return toolbar;
    }

    private void initToolbar() {
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(false);
        actionBar.setHomeButtonEnabled(false);
        Window window = this.getWindow();
        if (Utils.getAPIVersion() >= 5.0) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                window.setStatusBarColor(this.getResources().getColor(R.color.colorPrimaryDark));
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        search = (SearchView) menu.findItem(R.id.action_search).getActionView();
        search.setIconified(false);
        if (viewPager.getCurrentItem() == 0) {
            search.setQueryHint(getString(R.string.hint_backup_search));
        } else {
            search.setQueryHint(getString(R.string.hint_restore_search));
        }
        search.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                try {
                    if (viewPager.getCurrentItem() == 0) {
                        frag_backup.bAdapter.getFilter().filter(s);
                    } else {
                        frag_restore.rAdapter.getFilter().filter(s);
                    }
                } catch (Exception e) {

                }
                return true;
            }
        });
        search.onActionViewCollapsed();
        return super.onCreateOptionsMenu(menu);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_search: {
                supportInvalidateOptionsMenu();
                return true;
            }
            case R.id.action_github: {
                Uri uri = Uri.parse("https://github.com/prigic/APK-Backup-Android");
                Intent goToGithub = new Intent(Intent.ACTION_VIEW, uri);
                startActivity(goToGithub);
                return true;
            }
            case R.id.action_about: {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("정보");
                builder.setMessage(getString(R.string.about_text));
                builder.setNeutralButton("닫기", null);
                builder.show();
                return true;
            }
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 200) {
            for (String perm : permissions) {
                boolean rationale = false;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                    rationale = shouldShowRequestPermissionRationale(perm);
                }
                sharedPref.setNeverAskAgain(perm, !rationale);
            }
            if (PermissionUtil.isAllPermissionGranted(this)) {
                onCreateProcess();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}
