package com.prigic.apkbackup.activity;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.prigic.apkbackup.BuildConfig;
import com.prigic.apkbackup.R;
import com.prigic.apkbackup.data.RestoreList;
import com.prigic.apkbackup.data.PermissionUtil;
import com.prigic.apkbackup.data.Utils;
import com.prigic.apkbackup.data.Restore;

import java.util.ArrayList;
import java.util.List;

public class RestoreActivity extends Fragment {
    private ProgressBar progressBar;
    private ListView listView;
    private View view;
    public RestoreList rAdapter;
    private List<Restore> apkList = new ArrayList<>();
    private LinearLayout lyt_not_found;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.activity_restore, container, false);
        listView = (ListView) view.findViewById(R.id.listView);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar2);
        lyt_not_found = (LinearLayout) view.findViewById(R.id.lyt_not_found);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                dialogApkFileOption(i);
            }
        });
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PermissionUtil.isAllPermissionGranted(getActivity())) {
            refreshList();
        }
    }

    public void refreshList() {
        apkList = Utils.loadBackupAPK(getActivity());
        rAdapter = new RestoreList(getActivity(), apkList);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        // Capture ListView item click
        listView.setMultiChoiceModeListener(multiChoiceModeListener);
        listView.setAdapter(rAdapter);
        if (apkList.size() == 0) {
            lyt_not_found.setVisibility(View.VISIBLE);
        } else {
            lyt_not_found.setVisibility(View.GONE);
        }

    }

    private boolean mode_checkall = false;

    private void toogleCheckAll() {
        mode_checkall = !mode_checkall;
        for (int i = 0; i < rAdapter.getCount(); i++) {
            listView.setItemChecked(i, mode_checkall);
        }
        if (mode_checkall) {
            rAdapter.selectAll();
        } else {
            rAdapter.resetSelected();
        }
    }

    public ActionMode getActionMode() {
        return act_mode;
    }

    private ActionMode act_mode = null;
    private AbsListView.MultiChoiceModeListener multiChoiceModeListener = new AbsListView.MultiChoiceModeListener() {

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            final int checkedCount = listView.getCheckedItemCount();
            mode.setTitle(checkedCount + " 개가 선택되었습니다");
            rAdapter.setSelected(position, checked);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_check_all:
                    toogleCheckAll();
                    return true;
                case R.id.action_restore:
                    restoreApkFiles(rAdapter.getSelected());
                    return true;
                case R.id.action_delete:
                    deleteApkFiles(rAdapter.getSelected());
                    refreshList();
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.restore_context_menu, menu);
            mode.setTitle(listView.getCheckedItemCount() + " 개가 선택되었습니다");
            act_mode = mode;
            ((MainActivity) getActivity()).getToolbar().setVisibility(View.GONE);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            ((MainActivity) getActivity()).getToolbar().setVisibility(View.VISIBLE);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    };

    private void dialogApkFileOption(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Restore r = rAdapter.getItem(position);
        builder.setTitle("APK 파일 옵션");
        ListView listView = new ListView(getActivity());
        listView.setPadding(25, 25, 25, 25);
        String[] stringArray = new String[]{"복원", "공유", "파일 삭제"};
        listView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, stringArray));
        builder.setView(listView);
        final AppCompatDialog dialog = builder.create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                dialog.dismiss();
                List<Restore> selected_apk = new ArrayList<>();
                selected_apk.add(r);
                switch (i) {
                    case 0:
                        restoreApkFiles(selected_apk);
                        break;
                    case 1:
                        Intent intent = new Intent(Intent.ACTION_SEND);
                        Uri fileUri = Uri.fromFile(r.getFile());
                        if (Build.VERSION.SDK_INT >= 24) {
                            fileUri = FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID + ".provider", r.getFile());
                        }
                        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
                        intent.setType("*/*");
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        startActivity(intent);
                        break;
                    case 2:
                        deleteApkFiles(selected_apk);
                        refreshList();
                        break;
                }
            }
        });

        dialog.show();
    }

    private void restoreApkFiles(List<Restore> apklist) {
        for (Restore restr : apklist) {
            Uri fileUri = Uri.fromFile(restr.getFile());
            if (Build.VERSION.SDK_INT >= 24) {
                fileUri = FileProvider.getUriForFile(getActivity(), BuildConfig.APPLICATION_ID + ".provider", restr.getFile());
            }
            Intent intent = new Intent(Intent.ACTION_VIEW, fileUri);
            intent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
            intent.setDataAndType(fileUri, "application/vnd.android" + ".package-archive");
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        }
    }

    private void deleteApkFiles(List<Restore> apklist) {
        for (Restore restr : apklist) {
            if (restr.getFile().exists()) {
                restr.getFile().delete();
            }
        }
    }
}
