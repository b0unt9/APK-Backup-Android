package com.prigic.apkbackup.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class Utils {

    public static float getAPIVersion() {
        Float f = null;
        try {
            StringBuilder strBuild = new StringBuilder();
            strBuild.append(android.os.Build.VERSION.RELEASE.substring(0, 2));
            f = new Float(strBuild.toString());
        } catch (NumberFormatException e) {

        }
        return f.floatValue();
    }

    public static List<Restore> loadBackupAPK(Context ctx) {
        List<Restore> appList = new ArrayList<>();
        File root = new File(Constant.BACKUP_FOLDER);
        if (root.exists() && root.isDirectory()) {
            for (File f : root.listFiles()) {
                if (f.length() > 0 && f.getPath().endsWith(".apk")) {
                    String filePath = f.getPath();
                    PackageInfo pk = ctx.getPackageManager().getPackageArchiveInfo(filePath, PackageManager.GET_ACTIVITIES);
                    if (pk != null) {
                        ApplicationInfo info = pk.applicationInfo;
                        if (Build.VERSION.SDK_INT >= 8) {
                            info.sourceDir = filePath;
                            info.publicSourceDir = filePath;
                        }
                        Drawable icon = info.loadIcon(ctx.getPackageManager());
                        Restore app = new Restore();
                        app.setIcon(icon);
                        app.setFile(f);
                        app.setPath(filePath);
                        app.setName(f.getName());
                        appList.add(app);
                    }
                }
            }
        }
        return appList;
    }

    public static List<Backup> backupExistChecker(List<Backup> backups, Context ctx) {
        File root = new File(Constant.BACKUP_FOLDER);
        if (root.exists() && root.isDirectory()) {
            for (File f : root.listFiles()) {
                if (f.length() > 0 && f.getPath().endsWith(".apk")) {
                    for (int i = 0; i < backups.size(); i++) {
                        String name = backups.get(i).getApp_name() + "_" + backups.get(i).getVersion_name() + ".apk";
                        if (f.getName().equals(name)) {
                            backups.get(i).setExist(true);
                            break;
                        }
                    }
                }
            }
        }
        return backups;
    }

    public static boolean cekConnection(Context context) {
        ConnectionDetector conn = new ConnectionDetector(context);
        if (conn.isConnectingToInternet()) {
            return true;
        } else {
            return false;
        }
    }

    public static boolean needRequestPermission() {
        return (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1);
    }

}
