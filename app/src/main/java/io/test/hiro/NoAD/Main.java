package io.test.hiro.NoAD;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Main implements IXposedHookLoadPackage {
    private Set<String> containsSet = new HashSet<>();
    private Set<String> exactMatchSet = new HashSet<>();
    private Set<String> excludeSet = new HashSet<>();
    private static final String FILE_NAME = "log_settings.txt";
    private static final String DIRECTORY_NAME = "NoAd Module";
    private static boolean hasToastShown = false; // トースト表示済みフラグ
    private static final String PREF_NAME = "ad_prefs";  // SharedPreferencesファイル名

    @Override
    public void handleLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        String packageName = loadPackageParam.packageName;
        if ("io.test.hiro.NoAD".equals(packageName)) {
            return;
        }

        XposedHelpers.findAndHookMethod(
                ViewGroup.class,
                "addView",
                View.class,
                ViewGroup.LayoutParams.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.args[0];
                        Context context = view.getContext();
                        String className = view.getClass().getName();
                        if (!hasToastShown) {
                            try {
                                loadAdClassesFromPreferences(context,packageName);
                                hasToastShown = true;

                                XposedBridge.log("Ad detection popup displayed");

                            } catch (Exception e) {
                                XposedBridge.log("Popup display failed: " + e.getMessage());

                                Toast.makeText(context, "Ad detection activated", Toast.LENGTH_SHORT).show();
                            }
                        }
                        if ("android.widget.TextView".equals(className)
                                || "android.widget.ImageView".equals(className)
                                || "android.widget.Space".equals(className)
                                || "androidx.appcompat.widget.AppCompatTextView".equals(className)
                                || "androidx.appcompat.widget.AppCompatImageView".equals(className)
                                || "android.widget.FrameLayout".equals(className)
                                || "androidx.appcompat.view.menu.ActionMenuItemView".equals(className)
                                || "androidx.core.widget.ContentLoadingProgressBar".equals(className)
                                || "androidx.appcompat.widget.AppCompatButton".equals(className)
                        ) {
                            return; // 処理をスキップ
                        }
                        checkAndChangeBackgroundColor(context, view, packageName);

                        if ("com.google.android.webview".equals(packageName) || "com.google.android.gms".equals(packageName)) {
                            return;
                        }


                        boolean shouldHide = false;

                        if (excludeSet.contains(className)) {
                            shouldHide = false;
                        } else {

                            shouldHide = containsSet.stream().anyMatch(className::contains) ||
                                    exactMatchSet.contains(className);
                        }

                        if (shouldHide && view.getVisibility() != View.GONE) {
                            view.setVisibility(View.GONE);
                        }
                    }
                }
        );

        XposedBridge.hookAllMethods(
                View.class,
                "onAttachedToWindow",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        Context context = view.getContext();
                        String className = view.getClass().getName();
                        boolean shouldHide = false;
                        if ("android.widget.TextView".equals(className)
                                || "android.widget.ImageView".equals(className)
                                || "android.widget.Space".equals(className)
                                || "androidx.appcompat.widget.AppCompatTextView".equals(className)
                                || "androidx.appcompat.widget.AppCompatImageView".equals(className)
                                || "android.widget.FrameLayout".equals(className)
                                || "androidx.appcompat.view.menu.ActionMenuItemView".equals(className)
                                || "androidx.core.widget.ContentLoadingProgressBar".equals(className)
                                || "androidx.appcompat.widget.AppCompatButton".equals(className)

                        ) {
                            return; // 処理をスキップ
                        }
                        if (excludeSet.contains(className)) {
                            shouldHide = false;
                        } else {

                            shouldHide = containsSet.stream().anyMatch(className::contains) ||
                                    exactMatchSet.contains(className);
                        }

                        if (shouldHide) {
                            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                            if (layoutParams != null) {
                                layoutParams.height = 0;
                                view.setLayoutParams(layoutParams);
                                XposedBridge.log("Ad view hidden on attach: " + className);
                            }
                        }
                    }
                }
        );


        XposedHelpers.findAndHookMethod("android.view.View", loadPackageParam.classLoader, "onAttachedToWindow",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        Context context = view.getContext();
                        String className = view.getClass().getName();
                        String resourceName = getResourceName(view, context.getResources());

                        boolean shouldHide = false;
                        if ("android.widget.TextView".equals(className)
                                || "android.widget.ImageView".equals(className)
                                || "android.widget.Space".equals(className)
                                || "androidx.appcompat.widget.AppCompatTextView".equals(className)
                                || "androidx.appcompat.widget.AppCompatImageView".equals(className)
                                || "android.widget.FrameLayout".equals(className)
                                || "androidx.appcompat.view.menu.ActionMenuItemView".equals(className)
                                || "androidx.core.widget.ContentLoadingProgressBar".equals(className)
                                || "androidx.appcompat.widget.AppCompatButton".equals(className)

                        ) {
                            return; // 処理をスキップ
                        }
                        checkAndChangeBackgroundColor(context, view, loadPackageParam.packageName);
                        XposedBridge.log("Checking view: Class Name = " + className + ", Resource Name = " + resourceName);
                        XposedBridge.log("Current containsSet: " + containsSet);

                        if (className != null) {

                            boolean isExcluded = excludeSet.stream().anyMatch(className::contains);
                            boolean isExcludedBySuffix = excludeSet.stream().anyMatch(className::endsWith);

                            if (isExcluded || isExcludedBySuffix) {
                                shouldHide = false;
                            } else {
                                // containsSetまたはexactMatchSetに基づいて表示を決定
                                shouldHide = containsSet.stream().anyMatch(className::contains) ||
                                        exactMatchSet.contains(className);
                            }
                        }

                        if (shouldHide) {
                            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                            if (layoutParams != null) {
                                layoutParams.height = 0;
                                view.setLayoutParams(layoutParams);
                                XposedBridge.log("Ad view hidden on attach: " + className);
                            }
                        }
                    }
                }
        );
        for (Object adClass : containsSet) {
            if (adClass instanceof String) {
                String adClassName = (String) adClass;
                try {
                    Class<?> clazz = loadPackageParam.classLoader.loadClass(adClassName);
                    XposedBridge.hookAllConstructors(
                            clazz,
                            new XC_MethodHook() {
                                @Override
                                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                                    if (param.thisObject instanceof View) {
                                        View view = (View) param.thisObject;
                                        Context context = view.getContext();


                                        if (!excludeSet.contains(adClassName)) {

                                            view.setVisibility(View.GONE);

                                            view.getViewTreeObserver().addOnGlobalLayoutListener(
                                                    new ViewTreeObserver.OnGlobalLayoutListener() {
                                                        @Override
                                                        public void onGlobalLayout() {
                                                            if (view.getVisibility() != View.GONE) {
                                                                view.setVisibility(View.GONE);
                                                            }
                                                        }
                                                    }
                                            );
                                        }
                                    }
                                }
                            }
                    );
                } catch (ClassNotFoundException e) {
                    XposedBridge.log("Class not found: " + adClassName);
                }
            }
        }
    }
    private void writeDefaultConfig(File configFile, String packageName) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            String defaultConfig;

            if (packageName.equals("jp.co.airfront.android.a2chMate")) {
                defaultConfig = "C:Ads,C;ads,C:ADs,E:o.onAdsExhausted";
            } else {
                defaultConfig = "C:Ads,C;ads,C:ADs"; // デフォルト値
            }

            writer.write(defaultConfig);
            writer.flush();

        } catch (IOException e) {
            XposedBridge.log("Failed to write default config: " + e.getMessage());

        }
    }
    private void loadAdClassesFromPreferences(Context context, String packageName) {
        // パッケージ名に基づいたファイル名を作成
        File configFile = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "NoAd Module/" + packageName + "_ad_config.txt" // パッケージ名をファイル名に追加
        );

        try {
            File parentDir = configFile.getParentFile();
            if (!parentDir.exists() && !parentDir.mkdirs()) {
                XposedBridge.log("Failed to create directory: " + parentDir.getAbsolutePath());
                return;
            }

            if (!configFile.exists()) {
                if (configFile.createNewFile()) {
                    XposedBridge.log("Created new config file: " + configFile.getAbsolutePath());
                    writeDefaultConfig(configFile,packageName);
                } else {
                    XposedBridge.log("Failed to create config file");
                    return;
                }
            }

            StringBuilder adClassesContent = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(configFile), StandardCharsets.UTF_8))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    processConfigEntry(line.trim());
                    adClassesContent.append(line.trim()).append("\n");
                }
            }
            if (adClassesContent.length() > 0) {
                String adClasses = adClassesContent.toString();
                XposedBridge.log("Loaded ad classes: " + adClasses);
                Toast.makeText(context, "Loaded ad classes:\n" + adClasses, Toast.LENGTH_LONG).show(); // トースト表示
            }

        } catch (IOException e) {
            XposedBridge.log("Config file error: " + e.getMessage());
        }
    }
    private void processConfigEntry(String entry) {
        String[] entries = entry.split(",");
        for (String e : entries) {
            String trimmedEntry = e.trim();
            if (trimmedEntry.startsWith("C:")) {
                containsSet.add(trimmedEntry.substring(2).trim());
            } else if (trimmedEntry.startsWith("M:")) {
                exactMatchSet.add(trimmedEntry.substring(2).trim());
            } else if (trimmedEntry.startsWith("E:")) {
                excludeSet.add(trimmedEntry.substring(2).trim());
            } else {
                containsSet.add(trimmedEntry);
            }
        }
    }

    private String getViewResourceName(View view) {
        try {
            int id = view.getId();
            if (id != View.NO_ID) {
                String resourceName = view.getResources().getResourceName(id);
                return resourceName != null ? resourceName : String.valueOf(id);
            }
        } catch (Resources.NotFoundException e) {
            //XposedBridge.log("Resource name not found for View ID: " + view.getId());
        } catch (Exception e) {
            //XposedBridge.log("Unexpected error while retrieving resource name: " + e.getMessage());
        }
        return null;
    }

    private String getResourceName(View view, Resources resources) {
        try {
            int resourceId = view.getId();
            if (resourceId != View.NO_ID) {
                return resources.getResourceEntryName(resourceId);
            }
        } catch (Resources.NotFoundException e) {
            ////XposedBridge.log("Resource not found: " + e.getMessage());
        }
        return "";
    }
    private Set<String> changedResources = new HashSet<>();
    private boolean isChangingColor = false;

    private void checkAndChangeBackgroundColor(Context context, View view, String packageName) {
        try {
            if ("com.google.android.webview".equals(packageName) || "com.google.android.gms".equals(packageName)) {
                return;
            }
            if (!isLoggingEnabled()) {
                return;
            }
            if (isChangingColor) {
                return;
            }
            isChangingColor = true;
            String resourceName = getViewResourceName(view);
            if (resourceName == null || resourceName.isEmpty()) {
                // リソース名が見つからない場合は、クラス名を使用する
                resourceName = view.getClass().getName();
            }

            // 既に変更済みのリソースかどうかを確認
            if (changedResources.contains(resourceName)) {
                return; // 変更済みリソースの場合は処理を終了
            }

            Drawable background = view.getBackground();
            if (background != null) {
                XposedBridge.log("Background Class Name: " + background.getClass().getName());

                if (background instanceof ColorDrawable) {
                    // 背景が ColorDrawable の場合、色を変更
                    Random random = new Random();
                    int randomColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                    ((ColorDrawable) background).setColor(randomColor);
                    changedResources.add(resourceName);

                    String colorCode = String.format("#%06X", (0xFFFFFF & randomColor));
                    String viewClassName = view.getClass().getName();
                    XposedBridge.log("Changed Background Color of Resource Name: " + resourceName + " (Class: " + viewClassName + ") to " + colorCode);

                    if (isLoggingEnabled()) {
                        writeLogToFile(context, packageName, resourceName, colorCode, viewClassName, null);
                    }
                } else {
                    String backgroundInfo = "Unknown";
                    if (background instanceof BitmapDrawable) {
                        backgroundInfo = "BitmapDrawable";
                    } else if (background instanceof GradientDrawable) {
                        backgroundInfo = "GradientDrawable";
                    }
                    String viewClassName = view.getClass().getName();
                    XposedBridge.log("Background Type: " + backgroundInfo + " for Resource Name: " + resourceName);

                    if (isLoggingEnabled()) {
                        writeLogToFile(context, packageName, resourceName, null, viewClassName, backgroundInfo);
                    }
                }
            } else {
                String viewClassName = view.getClass().getName();
                XposedBridge.log("Background is null for Resource Name: " + resourceName);
                if (isLoggingEnabled()) {
                    writeLogToFile(context, packageName, resourceName, null, viewClassName, "null");
                }

                String finalResourceName = resourceName;
                view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        int width = view.getWidth();
                        int height = view.getHeight();

                        if (width <= 0 || height <= 0) {
                            width = 100;
                            height = 100;
                        }

                        int randomColor = getRandomColor();
                        Bitmap colorBitmap = createColorBitmap(width, height, randomColor);
                        view.setBackground(new BitmapDrawable(context.getResources(), colorBitmap));
                        changedResources.add(finalResourceName);

                        String colorCode = String.format("#%06X", (0xFFFFFF & randomColor));
                        XposedBridge.log("Set random color background for Resource Name: " + finalResourceName + " (Class: " + viewClassName + ") to " + colorCode);

                        // リスナーを削除
                        view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                });
            }
        } catch (Resources.NotFoundException e) {
            XposedBridge.log("Resource name not found for View ID: " + view.getId());
        } finally {
            isChangingColor = false;
        }
    }

    private int getRandomColor() {
        Random random = new Random();
        return Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
    }

    private Bitmap createColorBitmap(int width, int height, int color) {
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(color);
        return bitmap;
    }
    private void writeLogToFile(Context context, String packageName, String resourceName, String colorCode, String className, String backgroundType) {
        File backupDir = null;
        File logFile = null;
        try {
            backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_NAME);
            if (!backupDir.exists() && !backupDir.mkdirs()) {
                return;
            }
            logFile = new File(backupDir, packageName + "_resource_color_log.txt");
            StringBuilder logEntry = new StringBuilder();
            logEntry.append("Resource Name: ").append(resourceName).append(", ");
            if (colorCode != null) {
                logEntry.append("Color Code: ").append(colorCode).append(", ");
            }
            logEntry.append("Class Name: ").append(className).append(", ");
            logEntry.append("Background Type: ").append(backgroundType == null ? "Unknown" : backgroundType).append("\n\n");

            try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
                writer.append(logEntry.toString());
                writer.flush();
            }
        } catch (IOException e) {
            XposedBridge.log("Error writing to log file: " + e.getMessage());
        }
    }

    private boolean isLoggingEnabled() {
        File backupDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), DIRECTORY_NAME);
        if (!backupDir.exists()) {
            Log.w("LoggingCheck", "Directory does not exist: " + backupDir.getAbsolutePath());
            return false;
        }

        File logFile = new File(backupDir, FILE_NAME);
        if (!logFile.exists()) {
            Log.w("LoggingCheck", "Log file does not exist: " + logFile.getAbsolutePath());
            return false;
        }

        if ("log_settings.txt".equals(logFile.getName())) {
            Log.i("LoggingCheck", "Log file is log_settings.txt, returning false.");
            return true;
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
            String line = reader.readLine();
            boolean isEnabled = Boolean.parseBoolean(line);
            Log.i("LoggingCheck", "Read log status: " + isEnabled);
            return isEnabled;
        } catch (IOException e) {
            Log.e("LoggingCheck", "Error reading log file", e);
            return false;
        }
    }

}
