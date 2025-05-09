package io.test.hiro.NoAD;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.Toast;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import dalvik.system.DexFile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Main implements IXposedHookLoadPackage {
    private Set<String> containsSet = new HashSet<>();
    private Set<String> exactMatchSet = new HashSet<>();
    private Set<String> excludeSet = new HashSet<>();
    private Set<String> SkipColorSet = new HashSet<>();
    private static final String FILE_NAME = "log_settings.txt";
    private static final String DIRECTORY_NAME = "NoAd Module";
    private static boolean hasToastShown = false; // トースト表示済みフラグ
    private static final String PREF_NAME = "ad_prefs";  // SharedPreferencesファイル名
    private Class<?> adClass;
    private final Set<Class<?>> adClasses = new HashSet<>();

    @Override
    public void handleLoadPackage( XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
        String packageName = loadPackageParam.packageName;
        if ("io.test.hiro.NoAD".equals(packageName)) {
            return;
        }
        if ("com.google.android.webview".equals(packageName) || "com.google.android.gms".equals(packageName)) {
            return;
        }

 //hookAllClassesInPackage(loadPackageParam.classLoader, loadPackageParam);

        if ("works.jubilee.timetree".equals(packageName)) {


            Class<?> stateClass = XposedHelpers.findClass(
                    "works.jubilee.timetree.features.home.presentation.U$g",
                    loadPackageParam.classLoader
            );
            Class<?> stateClasss = XposedHelpers.findClass(
                    "works.jubilee.timetree.features.home.presentation.v$g",
                    loadPackageParam.classLoader
            );


            XposedHelpers.findAndHookMethod(stateClass,
                "getDisplayHeightInDp",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        param.setResult(0f);
                    }
                }
        );

        Class<?> viewStateEnum = XposedHelpers.findClass(
                "works.jubilee.timetree.components.headline.composable.c$a",
                loadPackageParam.classLoader
        );
        Object hideEnum = XposedHelpers.getStaticObjectField(
                viewStateEnum,
                "Hide"
        );

        XposedHelpers.findAndHookMethod(stateClass,
                "getViewState",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(hideEnum);
                    }
                }
        );


        XposedHelpers.findAndHookMethod(stateClasss,
                "getViewState",
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        param.setResult(hideEnum);
                    }
                }
        );
        XposedBridge.hookAllMethods(
                ViewGroup.class,
                "addView",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        View view = (View) param.args[0];
                        if (view != null && view.getClass().getName().equals("com.google.android.gms.ads.nativead.MediaView")) {
                            XposedBridge.log("Early MediaView detection in addView");
                            view.setVisibility(View.GONE);
                        }
                    }
                }
        );
return;
   }

        XposedBridge.hookAllMethods(
                View.class,
                "onAttachedToWindow",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        View view = (View) param.thisObject;
                        Context context = view.getContext();
                        String className = view.getClass().getName();
                        String resourceName = getResourceName(view, context.getResources());
                        boolean shouldHide = false;

//                        if (Arrays.asList(
//                                "android.widget.TextView",
//                                "android.widget.ImageView",
//                                "android.widget.Space",
//                                "androidx.appcompat.widget.AppCompatTextView",
//                                "androidx.appcompat.widget.AppCompatImageView",
//                                "android.widget.FrameLayout",
//                                "androidx.appcompat.view.menu.ActionMenuItemView",
//                                "androidx.core.widget.ContentLoadingProgressBar",
//                                "androidx.appcompat.widget.AppCompatButton"
//                        ).contains(className)) {
//                            return;
//                        }

                        if (!excludeSet.contains(className)) {
                            shouldHide = containsSet.stream().anyMatch(className::contains) || exactMatchSet.contains(className);
                        }

                        if (!excludeSet.contains(resourceName)) {
                            shouldHide = shouldHide || containsSet.stream().anyMatch(resourceName::contains) || exactMatchSet.contains(resourceName);
                        }

                        if (shouldHide) {
                            ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                            if (layoutParams != null) {
                                layoutParams.height = 0;
                                view.setLayoutParams(layoutParams);
                                if (isLoggingEnabled()) {
                                    XposedBridge.log("Ad view hidden on attach: " + className + " (Resource: " + resourceName + ")");

                                }
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
                            if (isLoggingEnabled()) {
                                checkAndChangeBackgroundColor(context, view, loadPackageParam.packageName);
                                return;
                            }

                            boolean shouldHide = false;
                            if (!hasToastShown) {
                                try {
                                    loadAdClassesFromPreferences(context, packageName);
                                    hasToastShown = true;
                                    if (isLoggingEnabled()) {
                                        XposedBridge.log("Ad detection popup displayed");
                                    }
                                } catch (Exception e) {
                                    if (isLoggingEnabled()) {
                                        Toast.makeText(context, "Ad detection activated", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                            if (Arrays.asList(
                                    "android.widget.TextView",
                                    "android.widget.ImageView",
                                    "android.widget.Space",
                                    "androidx.appcompat.widget.AppCompatTextView",
                                    "androidx.appcompat.widget.AppCompatImageView",
                                    "android.widget.FrameLayout",
                                    "androidx.appcompat.view.menu.ActionMenuItemView",
                                    "androidx.core.widget.ContentLoadingProgressBar",
                                    "androidx.appcompat.widget.AppCompatButton").
                                    contains(className)) {
                                return;
                            }
                            if (isLoggingEnabled()) {
                                XposedBridge.log("Checking view: Class Name = " + className + ", Resource Name = " + resourceName);
                                XposedBridge.log("Current containsSet: " + containsSet);
                            }
                                boolean isExcluded = excludeSet.stream().anyMatch(className::contains);
                                boolean isExcludedBySuffix = excludeSet.stream().anyMatch(className::endsWith);
                                if (isExcluded || isExcludedBySuffix) {
                                } else {
                                    shouldHide = containsSet.stream().anyMatch(className::contains) ||
                                            exactMatchSet.contains(className);
                                }
                            if (!excludeSet.contains(className)) {
                                shouldHide = containsSet.stream().anyMatch(className::contains) || exactMatchSet.contains(className);
                            }
                            if (shouldHide) {
                                ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
                                if (layoutParams != null) {
                                    layoutParams.height = 0;
                                    view.setLayoutParams(layoutParams);
                                    if (isLoggingEnabled()) {
                                        XposedBridge.log("Ad view hidden on attach: " + className + " (Resource: " + resourceName + ")");
                                    }
                                }
                            }
/*
                            // リソース名に基づくチェック
                            if (!excludeSet.contains(resourceName)) {
                                shouldHide = shouldHide || containsSet.stream().anyMatch(resourceName::contains) || exactMatchSet.contains(resourceName);
                            }
                            */
//                            // ビューを非表示にする
//                            if (shouldHide && view.getVisibility() != View.GONE) {
//                                view.setVisibility(View.GONE);
//                                return;
//                            }
                            /*
                            if (resourceName != null) {
                                boolean isExcludedResource = excludeSet.stream().anyMatch(resourceName::contains);
                                boolean isExcludedResourceBySuffix = excludeSet.stream().anyMatch(resourceName::endsWith);

                                if (!(isExcludedResource || isExcludedResourceBySuffix)) {
                                    shouldHide = shouldHide ||
                                            containsSet.stream().anyMatch(resourceName::contains) ||
                                            exactMatchSet.contains(resourceName);
                                }
                            }
                             */

                        }
                    }
            );
    }



    private void writeDefaultConfig(File configFile, String packageName) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))) {
            String defaultConfig;

            if (packageName.equals("jp.co.airfront.android.a2chMate")) {

                defaultConfig = "C:Ads,C;ads,C:ADs,C:AdView,E:o.onAdsExhausted";
            } else {
                defaultConfig = "C:Ads,C;ads,C:ADs,";
            }

            writer.write(defaultConfig);
            writer.flush();

        } catch (IOException e) {
            XposedBridge.log("Failed to write default config: " + e.getMessage());

        }
    }
    private void loadAdClassesFromPreferences(Context context, String packageName) {
        File configFile = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "NoAd Module/" + packageName + "_ad_config.txt"
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

    private void hookAllClassesInPackage(ClassLoader classLoader, XC_LoadPackage.LoadPackageParam loadPackageParam) {
        try {
            String apkPath = loadPackageParam.appInfo.sourceDir;
            if (apkPath == null) {
                XposedBridge.log("Could not get APK path.");
                return;
            }

            DexFile dexFile = new DexFile(new File(apkPath));
            Enumeration<String> classNames = dexFile.entries();
            while (classNames.hasMoreElements()) {
                String className = classNames.nextElement();

                // 指定されたパッケージで始まるクラスのみをフック
                //  if (className.startsWith("com.linecorp.line") || className.startsWith("jp.naver.line.android")) {
                try {
                    Class<?> clazz = Class.forName(className, false, classLoader);
                    hookAllMethods(clazz);
                } catch (ClassNotFoundException e) {
                    XposedBridge.log("Class not found: " + className);
                } catch (Throwable e) {
                    XposedBridge.log("Error loading class " + className + ": " + e.getMessage());
                }
                //  }
            }
        } catch (Throwable e) {
            XposedBridge.log("Error while hooking classes: " + e.getMessage());
        }
    }
    private void hookAllMethods(Class<?> clazz) {
        // クラス内のすべてのメソッドを取得
        Method[] methods = clazz.getDeclaredMethods();

        for (Method method : methods) {
            // 抽象メソッドをスキップ
            if (java.lang.reflect.Modifier.isAbstract(method.getModifiers())) {
                continue;
            }

            // 対象メソッドが特定のビュー関連メソッドであるか確認
            if (

                    !"invokeSuspend".equals(method.getName()) &&
                            !"run".equals(method.getName()) &&
                            !"setOnTouchListener".equals(method.getName()) &&
                            !"setVisibility".equals(method.getName()) &&
                            !"setAlpha".equals(method.getName()) &&
                            !"setEnabled".equals(method.getName()) &&
                            !"onCreate".equals(method.getName()) &&
                            !"setFocusable".equals(method.getName()) &&
                            !"setOnClickListener".equals(method.getName()) &&
                            !"setBackgroundColor".equals(method.getName()) &&
                            !"setPadding".equals(method.getName()) &&
                            !"setLayoutParams".equals(method.getName()) &&
                            !"requestLayout".equals(method.getName()) &&
                            !"invalidate".equals(method.getName()) &&
                            !"setText".equals(method.getName()) &&  // 新しく追加されたメソッド
                            !"setTextColor".equals(method.getName()) &&  // 新しく追加されたメソッド
                            !"setHint".equals(method.getName()) &&  // 新しく追加されたメソッド
                            !"setHintTextColor".equals(method.getName()) &&  // 新しく追加されたメソッド
                            !"onStart".equals(method.getName()) &&
                            !"onViewCreated".equals(method.getName()) &&
//                    !"setCompoundDrawables".equals(method.getName()) &&
                            !"getActivity".equals(method.getName()) &&  // PendingIntent method
                            !"onViewAdded".equals(method.getName()) && // PendingIntent method
                            !"setState".equals(method.getName())) {   // PendingIntent method
                continue;
            }

            method.setAccessible(true); // アクセス可能に設定

            try {
                // メソッドをフックする
                XposedBridge.hookMethod(method, new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                        StringBuilder argsString = new StringBuilder("Args: ");

                        // 引数が複数の場合、すべてを追加
                        for (int i = 0; i < param.args.length; i++) {
                            Object arg = param.args[i];
                            argsString.append("Arg[").append(i).append("]: ")
                                    .append(arg != null ? arg.toString() : "null")
                                    .append(", ");
                        }

// メソッドに応じたログ出力
                        if ("invokeSuspend".equals(method.getName())) {
                       //     XposedBridge.log("Before calling invokeSuspend in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("run".equals(method.getName())) {
                            XposedBridge.log("Before calling run in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("onCreate".equals(method.getName())) {
                            XposedBridge.log("Before calling onCreate in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setAlpha".equals(method.getName())) {
                            XposedBridge.log("Before calling setAlpha in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setEnabled".equals(method.getName())) {
                            XposedBridge.log("Before calling setEnabled in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setFocusable".equals(method.getName())) {
                            XposedBridge.log("Before calling setFocusable in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setOnClickListener".equals(method.getName())) {
                            XposedBridge.log("Before calling setOnClickListener in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setBackgroundColor".equals(method.getName())) {
                            XposedBridge.log("Before calling setBackgroundColor in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setPadding".equals(method.getName())) {
                            XposedBridge.log("Before calling setPadding in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setLayoutParams".equals(method.getName())) {
                            XposedBridge.log("Before calling setLayoutParams in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("requestLayout".equals(method.getName())) {
                            XposedBridge.log("Before calling requestLayout in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("invalidate".equals(method.getName())) {
                            XposedBridge.log("Before calling invalidate in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setText".equals(method.getName())) {
                            XposedBridge.log("Before calling setText in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setTextColor".equals(method.getName())) {
                            XposedBridge.log("Before calling setTextColor in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setHint".equals(method.getName())) {
                            XposedBridge.log("Before calling setHint in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setHintTextColor".equals(method.getName())) {
                            XposedBridge.log("Before calling setHintTextColor in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setCompoundDrawables".equals(method.getName())) {
                            XposedBridge.log("Before calling setCompoundDrawables in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("onStart".equals(method.getName())) {
                            XposedBridge.log("Before calling onStart in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("getActivity".equals(method.getName())) {
                            XposedBridge.log("Before calling getActivity in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("onViewAdded".equals(method.getName())) {
                            // スタックトレースの取得
                            StringBuilder stackTrace = new StringBuilder("\nStack Trace:\n");
                            for (StackTraceElement element : Thread.currentThread().getStackTrace()) {
                                stackTrace.append("  at ")
                                        .append(element.getClassName())
                                        .append(".")
                                        .append(element.getMethodName())
                                        .append("(")
                                        .append(element.getFileName())
                                        .append(":")
                                        .append(element.getLineNumber())
                                        .append(")\n");
                            }

                            // ログ出力（引数 + スタックトレース）
                            XposedBridge.log("Before calling onViewAdded in class: "
                                    + clazz.getName()
                                    + " with args: " + argsString
                                    + stackTrace.toString());

                        } else if ("getService".equals(method.getName())) {
                            XposedBridge.log("Before calling getService in class: " + clazz.getName() + " with args: " + argsString);
                        } else if ("setState".equals(method.getName())) {
                            XposedBridge.log("Before setState invoke in class: " + clazz.getName() + " with args: " + argsString);
                        }
                        }

/*
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Object result = param.getResult();
                        if ("invokeSuspend".equals(method.getName())) {
                            XposedBridge.log("after calling invokeSuspend in class: " + clazz.getName() + (result != null ? result.toString() : "null"));
                        } else if ("run".equals(method.getName())) {
                            XposedBridge.log("After calling run in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setAlpha".equals(method.getName())) {
                            XposedBridge.log("After calling setAlpha in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setEnabled".equals(method.getName())) {
                            XposedBridge.log("After calling setEnabled in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("onCreate".equals(method.getName())) {
                            XposedBridge.log("After calling onCreate in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("onViewCreated".equals(method.getName())) {
                            XposedBridge.log("After calling onViewCreated in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));

                        } else if ("setFocusable".equals(method.getName())) {
                            XposedBridge.log("After calling setFocusable in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setOnClickListener".equals(method.getName())) {
                            XposedBridge.log("After calling setOnClickListener in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setBackgroundColor".equals(method.getName())) {
                            XposedBridge.log("After calling setBackgroundColor in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setPadding".equals(method.getName())) {
                            XposedBridge.log("After calling setPadding in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setLayoutParams".equals(method.getName())) {
                            XposedBridge.log("After calling setLayoutParams in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("requestLayout".equals(method.getName())) {
                            XposedBridge.log("After calling requestLayout in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("invalidate".equals(method.getName())) {
                            XposedBridge.log("After calling invalidate in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setText".equals(method.getName())) {
                            XposedBridge.log("After calling setText in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setTextColor".equals(method.getName())) {
                            XposedBridge.log("After calling setTextColor in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setHint".equals(method.getName())) {
                            XposedBridge.log("After calling setHint in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setHintTextColor".equals(method.getName())) {
                            XposedBridge.log("After calling setHintTextColor in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setCompoundDrawables".equals(method.getName())) {
                            XposedBridge.log("After calling setCompoundDrawables in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("onStart".equals(method.getName())) {
                            XposedBridge.log("Before calling onStart in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("getActivity".equals(method.getName())) {
                            XposedBridge.log("After calling getActivity in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("onViewAdded".equals(method.getName())) {
                            XposedBridge.log("After calling onViewAdded in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("getService".equals(method.getName())) {
                            XposedBridge.log("After calling getService in class: " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        } else if ("setState".equals(method.getName())) {
                            XposedBridge.log("setState " + clazz.getName() + " with result: " + (result != null ? result.toString() : "null"));
                        }
                    }

 */
                });
            } catch (IllegalArgumentException e) {
                XposedBridge.log("Error hooking method " + method.getName() + " in class " + clazz.getName() + " : " + e.getMessage());
            } catch (Throwable e) {
                XposedBridge.log("Unexpected error hooking method " + method.getName() + " in class " + clazz.getName() + " : " + Log.getStackTraceString(e));
            }
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
            } else if (trimmedEntry.startsWith("SK:")) {
                // 小文字に変換して追加（大文字小文字を区別しないため）
                String skipEntry = trimmedEntry.substring(2).trim().toLowerCase();
                SkipColorSet.add(skipEntry);
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
            if (isChangingColor) {
                return;
            }
            isChangingColor = true;
            String resourceName = getViewResourceName(view);
            if (resourceName == null || resourceName.isEmpty()) {
                resourceName = view.getClass().getName();
            }

            String viewClassName = view.getClass().getName();
            String viewClassNameLower = viewClassName.toLowerCase();

// SkipColorSetチェック
            for (String skipPattern : SkipColorSet) {
                if (viewClassNameLower.contains(skipPattern)) {
                    XposedBridge.log("Skipped view by SkipColorSet [Pattern: " + skipPattern + "]: " + viewClassName);
                    return;
                }
            }

// 元のdispatchTouchEventチェック（必要に応じて残す）
            if (resourceName.toLowerCase().contains("dispatchtouchevent") ||
                    viewClassNameLower.contains("dispatchtouchevent")) {
                XposedBridge.log("Skipped dispatchTouchEvent related view: " + resourceName);
                return;
            }

            if (changedResources.contains(resourceName)) {
                return;
            }

            Drawable background = view.getBackground();
            if (background != null) {
                XposedBridge.log("Background Class Name: " + background.getClass().getName());

                if (background instanceof ColorDrawable) {
                    // ColorDrawableの場合の処理
                    Random random = new Random();
                    int randomColor = Color.rgb(random.nextInt(256), random.nextInt(256), random.nextInt(256));
                    ((ColorDrawable) background).setColor(randomColor);
                    changedResources.add(resourceName);

                    String colorCode = String.format("#%06X", (0xFFFFFF & randomColor));
                    XposedBridge.log("Changed Background Color of Resource Name: " + resourceName + " (Class: " + viewClassName + ") to " + colorCode);

                    if (isLoggingEnabled()) {
                        writeLogToFile(context, packageName, resourceName, colorCode, viewClassName, "ColorDrawable");
                    }
                } else {
                    // その他のDrawableタイプの場合の処理
                    String backgroundInfo = "Unknown";
                    if (background instanceof BitmapDrawable) {
                        backgroundInfo = "BitmapDrawable";
                    } else if (background instanceof GradientDrawable) {
                        backgroundInfo = "GradientDrawable";
                    }

                    XposedBridge.log("Modifying Background Type: " + backgroundInfo + " for Resource Name: " + resourceName);

                    int randomColor = getRandomColor();
                    String colorCode = String.format("#%06X", (0xFFFFFF & randomColor));
                    final String finalResourceName = resourceName;
                    final String finalViewClassName = viewClassName;
                    final String finalBackgroundInfo = backgroundInfo;

                    view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            int width = view.getWidth();
                            int height = view.getHeight();

                            if (width <= 0 || height <= 0) {
                                width = 100;
                                height = 100;
                            }

                            Bitmap colorBitmap = createColorBitmap(width, height, randomColor);
                            view.setBackground(new BitmapDrawable(context.getResources(), colorBitmap));
                            changedResources.add(finalResourceName);

                            XposedBridge.log("Set random color background for Resource Name: " + finalResourceName + " (Class: " + finalViewClassName + ") to " + colorCode);

                            if (isLoggingEnabled()) {
                                writeLogToFile(context, packageName, finalResourceName, colorCode, finalViewClassName, finalBackgroundInfo + "_modified");
                            }

                            view.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    });
                }
            } else {
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

                        if (isLoggingEnabled()) {
                            writeLogToFile(context, packageName, finalResourceName, colorCode, viewClassName, "generated_bitmap");
                        }

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


        float size = Math.min(width, height) * 0.2f;

        float margin = Math.min(width, height) * 0.1f;
        float left = width - size - margin;
        float top = margin;
        float right = width - margin;
        float bottom = margin + size;

        Paint paint = new Paint();
        paint.setColor(color);
        paint.setAntiAlias(true);
        paint.setStyle(Paint.Style.FILL);

        canvas.drawRect(left, top, right, bottom, paint);

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