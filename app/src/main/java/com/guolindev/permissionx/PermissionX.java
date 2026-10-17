package com.guolindev.permissionx;

import android.app.Activity;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PermissionX {
    
    private Activity activity;
    private List<String> permissions = new ArrayList<>();
    private Map<String, Boolean> granted = new HashMap<>();
    
    public static PermissionX init(Activity activity) {
        return new PermissionX(activity);
    }
    
    private PermissionX(Activity activity) {
        this.activity = activity;
    }
    
    public PermissionX permissions(List<String> perms) {
        if (perms != null) {
            this.permissions = new ArrayList<>(perms);
        }
        return this;
    }
    
    public PermissionX permissions(String... perms) {
        this.permissions = new ArrayList<>();
        for (String p : perms) {
            this.permissions.add(p);
        }
        return this;
    }
    
    public void request(PermissionCallback callback) {
        // Stub: always grant permissions
        Map<String, Boolean> result = new HashMap<>();
        for (String perm : permissions) {
            result.put(perm, true);
        }
        callback.onResult(permissions, result);
    }
    
    public interface PermissionCallback {
        void onResult(List<String> permissions, Map<String, Boolean> allGranted);
    }
}
