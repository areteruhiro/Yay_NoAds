package io.test.hiro.NoAD;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Main implements IXposedHookLoadPackage {
    private static final String TAG = "NoAdModule";
    private static final String PREF_NAME = "ad_prefs";
    private static final String FILE_NAME = "log_settings.txt";
    private static final String DIRECTORY_NAME = "NoAd Module";
    private static final Set<String> EXCLUDED_PACKAGES = Set.of(
            "com.google.android.webview",
            "com.google.android.gms",
            "io.test.hiro.NoAD"
    );
    private static final Pattern AD_PATTERN = Pattern.compile(
            ".*(?i)(_ad_|ads?|banner|adcontainer|mbridge).*",
            Pattern.CASE_INSENSITIVE
    );

    private final Set<Object> adClassNames = Collections.synchronizedSet(new HashSet<>());
    private final Set<String> changedResources = Collections.newSetFromMap(new WeakHashMap<>());
    private final Set<WeakReference<View>> processedViews = Collections.newSetFromMap(new WeakHashMap<>());
    private final ExecutorService logExecutor = Executors.newSingleThreadExecutor();
    private static boolean hasToastShown = false;

    @Override
    public void handleLoadPackage(@NonNull XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        final String packageName = lpparam.packageName;
        if (EXCLUDED_PACKAGES.contains(packageName)) return;

        loadAdClassesFromPreferences();
        initializeHooks(lpparam);
    }

    private void initializeHooks(XC_LoadPackage.LoadPackageParam lpparam) {
        // View追加フック
        XposedHelpers.findAndHookMethod(
                ViewGroup.class,
                "addView",
                View.class,
                ViewGroup.LayoutParams.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        handleAdView((View) param.args[0], lpparam.packageName);
                    }
                });

        // View表示フック
        XposedBridge.hookAllMethods(
                View.class,
                "onAttachedToWindow",
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        final View view = (View) param.thisObject;
                        handleAdView(view, lpparam.packageName);
                        processBackgroundModification(view, lpparam.packageName);
                    }
                });

        // 動的クラスフック
        hookAdViewConstructors(lpparam.classLoader);
    }

    private synchronized void handleAdView(View view, String packageName) {
        if (view == null || isAlreadyProcessed(view)) return;

        try {
            markViewAsProcessed(view);
            showInitialToastOnce(view.getContext());

            if (isAdView(view)) {
                safelyHideAdView(view);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error handling ad view", e);
        }
    }

    private boolean isAlreadyProcessed(View view) {
        return processedViews.stream()
                .anyMatch(ref -> ref.get() == view);
    }

    private void markViewAsProcessed(View view) {
        processedViews.add(new WeakReference<>(view));
    }

    private void showInitialToastOnce(Context context) {
        if (!hasToastShown) {
            Toast.makeText(context, "Loaded ad filters: " + adClassNames.size(), Toast.LENGTH_SHORT).show();
            hasToastShown = true;
        }
    }

    private boolean isAdView(View view) {
        final String className = view.getClass().getName();
        final int viewId = view.getId();

        // クラス名ベース判定
        if (AD_PATTERN.matcher(className).matches() ||
                className.startsWith("com.five_corp.ad.internal.view.") ||
                className.contains("com.mbridge.msdk.")) {
            return true;
        }

        // リソースIDベース判定
        if (viewId != View.NO_ID) {
            final String resName = getResourceName(view);
            return resName != null && AD_PATTERN.matcher(resName).matches();
        }

        // カスタム設定チェック
        return adClassNames.stream().anyMatch(ad -> matchesAdPattern(ad, view));
    }

    private boolean matchesAdPattern(Object ad, View view) {
        if (ad instanceof String) {
            return view.getClass().getName().contains((String) ad);
        }
        if (ad instanceof Integer) {
            return view.getId() == (Integer) ad;
        }
        return false;
    }

    private void safelyHideAdView(View view) {
        try {
            if (view.getVisibility() != View.GONE) {
                view.setVisibility(View.GONE);
            }

            final ViewGroup.LayoutParams params = view.getLayoutParams();
            if (params != null && params.height != 0) {
                params.height = 0;
                view.setLayoutParams(params);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error hiding ad view", e);
        }
    }

    private void processBackgroundModification(View view, String packageName) {
        if (!isLoggingEnabled() || changedResources.contains(getResourceName(view))) return;

        try {
            modifyBackgroundColor(view, packageName);
        } catch (Exception e) {
            Log.e(TAG, "Background modification error", e);
        }
    }

    private void modifyBackgroundColor(View view, String packageName) {
        final Drawable background = view.getBackground();
        if (background instanceof ColorDrawable) {
            final int newColor = generateRandomColor();
            ((ColorDrawable) background).setColor(newColor);
            logColorModification(view, packageName, newColor);
        }
    }

    private int generateRandomColor() {
        return Color.rgb(
                (int) (Math.random() * 256),
                (int) (Math.random() * 256),
                (int) (Math.random() * 256)
        );
    }

    private void logColorModification(View view, String packageName, int color) {
        final String resourceName = getResourceName(view);
        if (resourceName == null) return;

        changedResources.add(resourceName);
        final String logEntry = String.format(
                "[%s] Resource: %s, Color: #%06X, Class: %s\n",
                new java.util.Date(),
                resourceName,
                color,
                view.getClass().getName()
        );

        logExecutor.execute(() -> writeLogToFile(packageName + "_color.log", logEntry));
    }

    private void writeLogToFile(String fileName, String content) {
        final File logFile = getLogFile(fileName);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(logFile, true))) {
            writer.append(content);
        } catch (IOException e) {
            Log.e(TAG, "Log write error", e);
        }
    }

    private File getLogFile(String fileName) {
        final File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                DIRECTORY_NAME
        );
        if (!dir.exists() && !dir.mkdirs()) {
            Log.e(TAG, "Failed to create log directory");
        }
        return new File(dir, fileName);
    }

    private String getResourceName(View view) {
        try {
            final int id = view.getId();
            return id != View.NO_ID ? view.getResources().getResourceName(id) : null;
        } catch (Resources.NotFoundException e) {
            return null;
        }
    }

    private void hookAdViewConstructors(ClassLoader classLoader) {
        adClassNames.parallelStream().forEach(adClass -> {
            if (adClass instanceof String) {
                try {
                    final Class<?> clazz = classLoader.loadClass((String) adClass);
                    XposedBridge.hookAllConstructors(clazz, new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            if (param.thisObject instanceof View) {
                                safelyHideAdView((View) param.thisObject);
                            }
                        }
                    });
                } catch (ClassNotFoundException e) {
                    Log.d(TAG, "Ad class not found: " + adClass);
                }
            }
        });
    }

    private void loadAdClassesFromPreferences() {
        final XSharedPreferences prefs = new XSharedPreferences("io.test.hiro.NoAD", PREF_NAME);
        prefs.makeWorldReadable();
        prefs.reload();

        final String adClasses = prefs.getString("ad_classes", "");
        if (!adClasses.isEmpty()) {
            Collections.addAll(adClassNames, adClasses.split("\\s*,\\s*"));
        }
    }

    private boolean isLoggingEnabled() {
        final File logFile = getLogFile(FILE_NAME);
        if (!logFile.exists()) return false;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(logFile)))) {
            return Boolean.parseBoolean(reader.readLine());
        } catch (IOException e) {
            Log.e(TAG, "Log status read error", e);
            return false;
        }
    }
}