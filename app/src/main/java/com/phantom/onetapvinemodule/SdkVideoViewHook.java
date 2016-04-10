package com.phantom.onetapvinemodule;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SdkVideoViewHook implements IXposedHookLoadPackage {
    private static final String VINE_PACKAGE_NAME = "co.vine.android";
    private static final String CLASS_NAME = "co.vine.android.player.SdkVideoView";

    private static final String ACTION_SAVE_URI = "com.phantom.onetapvideodownload.action.saveurl";
    private static final String ONE_TAP_PACKAGE_NAME = "com.phantom.onetapvideodownload";
    private static final String IPC_SERVICE_CLASS_NAME = ONE_TAP_PACKAGE_NAME + ".IpcService";
    private static final String EXTRA_URL = ONE_TAP_PACKAGE_NAME + ".extra.url";
    private static final String EXTRA_PACKAGE_NAME = ONE_TAP_PACKAGE_NAME + ".extra.package_name";
    private static final String EXTRA_TITLE = ONE_TAP_PACKAGE_NAME + ".extra.title";

    public Context getContext() {
        Class activityThreadClass = XposedHelpers.findClass("android.app.ActivityThread", null);
        Object activityThread = XposedHelpers.callStaticMethod(activityThreadClass, "currentActivityThread");
        return (Context) XposedHelpers.callMethod(activityThread, "getSystemContext");
    }

    public boolean findClass(ClassLoader loader, String className) {
        try {
            loader.loadClass(className);
            return true;
        } catch( ClassNotFoundException e ) {
            return false;
        }
    }

    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals(VINE_PACKAGE_NAME)) {
            return;
        }

        final Context context = getContext();
        final XC_MethodHook methodHook = new XC_MethodHook() {
            protected void afterHookedMethod(MethodHookParam hookParams) throws Throwable {
                try {
                    Uri uri = (Uri)hookParams.args[0];
                    String url = uri.toString();
                    String filename = getFilenameFromUrl(url);
                    XposedBridge.log(uri.toString());

                    Intent intent = new Intent(ACTION_SAVE_URI);
                    intent.setClassName(ONE_TAP_PACKAGE_NAME, IPC_SERVICE_CLASS_NAME);
                    intent.putExtra(EXTRA_URL, uri.toString());
                    intent.putExtra(EXTRA_PACKAGE_NAME, lpparam.packageName);

                    if (filename != null) {
                        filename = filename + ".mp4";
                        intent.putExtra(EXTRA_TITLE, filename);
                    }

                    context.startService(intent);
                } catch (Exception e) {
                    StringWriter sw = new StringWriter();
                    PrintWriter pw = new PrintWriter(sw);
                    e.printStackTrace(pw);
                    XposedBridge.log("One Tap Vine Module : " + sw.toString());
                }
            }
        };

        String packageName = context.getPackageManager().getPackageInfo(VINE_PACKAGE_NAME
                , PackageManager.GET_META_DATA).versionName;

        if (!findClass(lpparam.classLoader, CLASS_NAME)) {
            XposedBridge.log("Class not found. Package name : " + packageName);
            return;
        }

        Class mainClass = XposedHelpers.findClass(CLASS_NAME, lpparam.classLoader);

        Object [] objects = new Object[] {
                Uri.class,
                Map.class,
                methodHook
        };

        // private void setVideoURI(Uri paramUri, Map<String, String> paramMap)
        XposedHelpers.findAndHookMethod(mainClass, "setVideoURI", objects);
        XposedBridge.log("One Tap Vine Module : hooking successful for name "
                + packageName);
    }

    private String getFilenameFromUrl(String url) {
        Uri uri = Uri.parse(url);
        return uri.getLastPathSegment();
    }
}
