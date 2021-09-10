package com.hj.data_proxy.utils;

import android.app.Activity;
import android.content.Context;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.PermissionChecker;

import java.util.List;

/**
 * Created by huangjian at 21-1-4 15:44
 */
public class PermissionUtil {
    public static boolean hasPermissions(Context context, List<String> permsToCheck,
                                         List<String> outDeniedPerms) {
        boolean allGranted = true;
        if (Build.VERSION.SDK_INT >= 23) {
            for (String perm : permsToCheck) {
                if (PermissionChecker.checkSelfPermission(context, perm) != PermissionChecker.PERMISSION_GRANTED) {
                    allGranted = false;
                    outDeniedPerms.add(perm);
                }
            }
        }

        return allGranted;
    }

    public static void requestPermissions(Activity activity, List<String> perms, int requestCode) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!perms.isEmpty()) {
                ActivityCompat.requestPermissions(activity,
                        perms.toArray(new String[0]), requestCode);
            }
        }
    }
}
