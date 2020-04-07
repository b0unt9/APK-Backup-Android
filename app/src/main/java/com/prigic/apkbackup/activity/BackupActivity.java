package com.prigic.apkbackup.activity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDialog;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.prigic.apkbackup.R;
import com.prigic.apkbackup.data.BackupList;
import com.prigic.apkbackup.data.Constant;
import com.prigic.apkbackup.data.PermissionUtil;
import com.prigic.apkbackup.data.Utils;
import com.prigic.apkbackup.data.Backup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BackupActivity extends Fragment {

    private ProgressBar progressBar;
    private ListView listView;
    private View view;
    public BackupList bAdapter;
    private PackageManager pm;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        view = inflater.inflate(R.layout.activity_backup, container, false);
        listView = (ListView) view.findViewById(R.id.listView);
        progressBar = (ProgressBar) view.findViewById(R.id.progressBar1);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                dialogAppOption(i);
            }
        });
        pm = (PackageManager) getActivity().getApplicationContext().getPackageManager();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (PermissionUtil.isAllPermissionGranted(getActivity())) {
            refresh(false);
        }
    }

    public void refresh(boolean fab_flag) {
        if (taskRunning) {
            Snackbar.make(view, "작업이 진행중입니다", Snackbar.LENGTH_SHORT).show();
        } else {
            new AppListLoaderTask(fab_flag).execute();
        }
    }

    private boolean taskRunning = false;

    private class AppListLoaderTask extends AsyncTask<String, String, String> {
        private String status = "";
        private List<Backup> app_list = new ArrayList<>();
        private boolean fab_flag = false;


        public AppListLoaderTask(boolean fab_flag) {
            this.fab_flag = fab_flag;
        }

        @Override
        protected void onPreExecute() {
            taskRunning = true;
            app_list.clear();
            progressBar.setVisibility(View.VISIBLE);
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                List<PackageInfo> packs = pm.getInstalledPackages(0);
                for (int i = 0; i < packs.size(); i++) {
                    PackageInfo p = packs.get(i);
                    if ((p.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                        continue;
                    }
                    Backup app = new Backup();
                    app.setApp_name(p.applicationInfo.loadLabel(pm).toString());
                    app.setPackgae_name(p.packageName);
                    app.setVersion_name(p.versionName);
                    app.setVersion_code(p.versionCode);
                    app.setApp_icon(p.applicationInfo.loadIcon(pm));
                    app.setFile(new File(p.applicationInfo.publicSourceDir));
                    app_list.add(app);
                }
                status = "success";
            } catch (Exception e) {
                status = "failed";
            }
            publishProgress();
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            progressBar.setVisibility(View.GONE);
            if (status.equals("success")) {
                Collections.sort(app_list, new Comparator<Backup>() {
                    @Override
                    public int compare(Backup b_1, Backup b_2) {
                        String l1 = b_1.getApp_name().toLowerCase();
                        String l2 = b_2.getApp_name().toLowerCase();
                        return l1.compareTo(l2);
                    }
                });
                app_list = Utils.backupExistChecker(app_list, getActivity());
                bAdapter = new BackupList(getActivity(), app_list);
                listView.setAdapter(bAdapter);
                setMultipleChoice();
            } else {
                Snackbar.make(view, "설치된 어플리케이션을 불러오는데 실패했습니다", Snackbar.LENGTH_SHORT).show();
            }
            taskRunning = false;
            if (fab_flag) {
                Snackbar.make(view, "새로고침 완료", Snackbar.LENGTH_SHORT).show();
            }
            super.onProgressUpdate(values);
        }
    }

    private class FileSaveTask extends AsyncTask<Void, Integer, File> {

        private ProgressDialog progress;
        private List<Backup> selected_app;

        public FileSaveTask(List<Backup> selected_app) {
            this.selected_app = selected_app;
        }

        @Override
        protected void onPreExecute() {
            progress = new ProgressDialog(getActivity());
            progress.setMessage("App Backup");
            progress.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            progress.setMax(selected_app.size());
            progress.setCancelable(false);
            progress.show();
        }

        @Override
        protected File doInBackground(Void... params) {
            int i = 0;
            File outputFile = null;
            while (selected_app.size() > i) {
                String filename = selected_app.get(i).getApp_name() + "_" + selected_app.get(i).getVersion_name() + ".apk";
                outputFile = new File(Constant.BACKUP_FOLDER);
                if (!outputFile.exists()) {
                    outputFile.mkdirs();
                }
                File apk = new File(outputFile.getPath() + "/" + filename);
                try {
                    apk.createNewFile();
                    InputStream in = new FileInputStream(selected_app.get(i).getFile());
                    OutputStream out = new FileOutputStream(apk);
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                    in.close();
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                publishProgress(i);
                i++;
            }
            return outputFile;
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progress.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(File result) {
            if (progress != null) {
                progress.dismiss();
            }

            if (result != null) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                alert.setCancelable(false);
                alert.setTitle("어플리케이션 백업 성공");
                alert.setMessage("APK 파일 위치: " + Constant.BACKUP_FOLDER);
                alert.setPositiveButton("확인", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dg, int arg1) {
                        bAdapter.resetSelected();
                        bAdapter.notifyDataSetChanged();
                        refresh(false);
                        dg.dismiss();
                    }
                });
                alert.show();
            } else {
                Toast.makeText(getActivity(), "어플리케이션 백업 실패", Toast.LENGTH_LONG).show();
            }
        }
    }

    private boolean mode_checkall = false;

    private void toogleCheckAll() {
        mode_checkall = !mode_checkall;
        for (int i = 0; i < bAdapter.getCount(); i++) {
            listView.setItemChecked(i, mode_checkall);
        }
        if (mode_checkall) {
            bAdapter.selectAll();
        } else {
            bAdapter.resetSelected();
        }
    }

    private void setMultipleChoice() {
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(multiChoiceModeListener);
    }

    public ActionMode getActionMode() {
        return act_mode;
    }

    private ActionMode act_mode = null;
    private MultiChoiceModeListener multiChoiceModeListener = new MultiChoiceModeListener() {

        @Override
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            final int checkedCount = listView.getCheckedItemCount();
            mode.setTitle(checkedCount + " 개가 선택되었습니다");
            bAdapter.setSelected(position, checked);
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_check_all:
                    toogleCheckAll();
                    return true;
                case R.id.action_backup:
                    new FileSaveTask(bAdapter.getSelected()).execute();
                    return true;
                case R.id.action_uninstall:
                    uninstallApp(bAdapter.getSelected());
                    return true;

                default:
                    return false;
            }
        }

        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.backup_context_menu, menu);
            mode.setTitle(listView.getCheckedItemCount() + " 개가 선택되었습니다");
            act_mode = mode;
            ((MainActivity) getActivity()).getToolbar().setVisibility(View.GONE);
            return true;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            for (int i = 0; i < bAdapter.getCount(); i++) {
                listView.setItemChecked(i, mode_checkall);
            }
            bAdapter.resetSelected();
            ((MainActivity) getActivity()).getToolbar().setVisibility(View.VISIBLE);
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    };

    private void dialogAppOption(final int position) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        final Backup m = bAdapter.getItem(position);
        builder.setTitle("어플리케이션 옵션");
        ListView listView = new ListView(getActivity());
        listView.setPadding(25, 25, 25, 25);
        String[] stringArray = new String[]{"백업", "제거", "세부사항"};
        listView.setAdapter(new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, stringArray));
        builder.setView(listView);
        final AppCompatDialog dialog = builder.create();
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                dialog.dismiss();
                List<Backup> selected_app = new ArrayList<>();
                selected_app.add(m);
                switch (i) {
                    case 0:
                        new FileSaveTask(selected_app).execute();
                        break;
                    case 1:
                        uninstallApp(selected_app);
                        break;

                    case 2:
                        showInstalledAppDetails(m.getPackgae_name());
                        break;
                }
            }
        });

        dialog.show();
    }

    private void uninstallApp(List<Backup> selected_app) {
        for (Backup b : selected_app) {
            Uri uri = Uri.fromParts("package", b.getPackgae_name(), null);
            Intent it = new Intent(Intent.ACTION_DELETE, uri);
            startActivity(it);
        }
    }

    private void showInstalledAppDetails(String packageName) {
        Intent intent = new Intent();
        intent.setAction(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + packageName));
        startActivity(intent);
    }

}
