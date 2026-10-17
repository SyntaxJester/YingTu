package com.yingtu.plus.setting;

import com.yingtu.plus.util.Prefers;
import java.util.List;

public class CustomCspSetting {
    public static Result inject(List<Object> sites) { return new Result(); }
    public static void inject(Object object) {}
    public static void inject(List<Object> lives, String spider) {}
    
    public static class Result {
        public boolean success() { return true; }
        public String message() { return ""; }
    }
}
