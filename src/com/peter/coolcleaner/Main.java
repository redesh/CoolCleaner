package com.peter.coolcleaner;

import java.util.ArrayList;
import java.util.List;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;

public class Main extends Activity {

	private Board board;
	
	public boolean isRoot;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);
		board = new Board(this, null);
		((FrameLayout) findViewById(R.id.gameview)).addView(board, 0);
		board.postDelayed(new Runnable() {
			
			@Override
			public void run() {
				isRoot = isRoot();
			}
		}, 200);
		
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (board != null) {
			board.startAnimation();
		}
	}

	@Override
	protected void onPause() {
		super.onPause();
		if (board != null) {
			board.stopAnimation();
		}
	}
	
	public void forceStop(final AppInfo info) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... params) {
				String cmd = "am force-stop " + info.packageName + " \n";
				ProcessUtils.executeCommand(cmd, 1000);
				return null;
			}

		}.execute();
	}

	
	public void showForceStopView(AppInfo info, View v) {
		int version = Build.VERSION.SDK_INT;
		Intent intent = new Intent();
		if (version >= 9) {
			intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
			Uri uri = Uri.fromParts("package", info.packageName, null);
			intent.setData(uri);
		} else {
			final String appPkgName = "pkg";
			intent.setAction(Intent.ACTION_VIEW);
			intent.setClassName("com.android.settings",
					"com.android.settings.InstalledAppDetails");
			intent.putExtra(appPkgName, info.packageName);
		}
		registReceiver(info, v);
		startActivity(intent);
	}
	
	private String forecStopPackageName;
	private BroadcastReceiver forceStopReceiver;
	
	private class MyReceiver extends BroadcastReceiver {

		View mView;
		
		public MyReceiver(View v) {
			mView = v;
		}

		@Override
		public void onReceive(Context context, Intent intent) {
			Uri data = intent.getData();
			if (data != null) {
				String str = data.getSchemeSpecificPart();
				if (!TextUtils.isEmpty(forecStopPackageName)
						&& !TextUtils.isEmpty(str)
						&& str.equals(forecStopPackageName)) {

					String action = intent.getAction();
					if (Intent.ACTION_PACKAGE_RESTARTED.equals(action)) {
						finishSetting();
						unregisterReceiver(forceStopReceiver);
						board.removeView(mView);
					} else if (Intent.ACTION_PACKAGE_REMOVED.equals(action)) {
						
					}
				}
			}
		}

	}
	
	private void finishSetting() {
		Intent in = new Intent();
		in.setClass(Main.this, Main.class);
		in.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);// 确保finish掉setting
		in.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);// 确保MainActivity不被finish掉
		startActivity(in);
	}
	
	private void registReceiver(AppInfo info, View v) {
		forecStopPackageName = info.packageName;
		forceStopReceiver = new MyReceiver(v);
		IntentFilter filter = new IntentFilter();
		filter.addAction(Intent.ACTION_PACKAGE_RESTARTED);
		filter.addAction(Intent.ACTION_PACKAGE_REMOVED);
		filter.addDataScheme("package");
		registerReceiver(forceStopReceiver, filter);
	}
	
	private boolean isRoot() {
		boolean result = true;
		String cmd = "ls /data/";
		int exeCode = ProcessUtils.executeCommand(cmd, 200);
		if (exeCode == 0 || exeCode == -2 || exeCode == -3
				|| exeCode == -4) {
			result = true;
		} else {
			result = false;
		}

		return result;
	}

	static class AppInfo {
		public String appName;
		public String packageName;
		public BitmapDrawable appIcon;
	}

	public List<AppInfo> getRunningAppInfos() {
		ActivityManager mActivityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		PackageManager pm = getPackageManager();
		List<ApplicationInfo> appList = pm
				.getInstalledApplications(PackageManager.GET_UNINSTALLED_PACKAGES);
		// 正在运行的进程
		List<RunningAppProcessInfo> runningAppProcessInfos = mActivityManager
				.getRunningAppProcesses();
		// 正在运行的应用

		List<AppInfo> runningApps = new ArrayList<AppInfo>(
				runningAppProcessInfos.size());
		for (RunningAppProcessInfo runningAppInfo : runningAppProcessInfos) {// 遍历正在运行的程序

			ArrayList<ApplicationInfo> infos = getAppInfo(
					runningAppInfo.pkgList, appList);// 获取正在运行的程序信息

			for (ApplicationInfo applicationInfo : infos) {
				if (applicationInfo != null && !isSystemApp(applicationInfo)) {// 非系统程序

					AppInfo info = new AppInfo();
					info.packageName = applicationInfo.packageName;
					BitmapDrawable bitmapDrawable = (BitmapDrawable) applicationInfo
							.loadIcon(pm);
					info.appIcon = getRightSizeIcon(bitmapDrawable);
					info.appName = applicationInfo.loadLabel(pm).toString();
					if (!containInfo(runningApps, info)) {
						runningApps.add(info);
					}
				}
			}
		}
		return runningApps;
	}

	private boolean containInfo(List<AppInfo> infos, AppInfo info) {
		for (AppInfo af : infos) {
			if (af.packageName.equals(info.packageName)) {
				return true;
			}
		}
		return false;
	}

	private BitmapDrawable getRightSizeIcon(BitmapDrawable drawable) {
		Drawable rightDrawable = getResources().getDrawable(
				R.drawable.ic_launcher);
		int rightSize = rightDrawable.getIntrinsicWidth();
		Bitmap bitmap = drawable.getBitmap();
		int width = bitmap.getWidth();
		float widths = width;
		float scale = rightSize / widths;
		Matrix matrix = new Matrix();
		matrix.setScale(scale, scale);
		Bitmap bm = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
				bitmap.getHeight(), matrix, true);
		return new BitmapDrawable(getResources(), bm);
	}

	private boolean isSystemApp(ApplicationInfo appInfo) {
		if ((appInfo.flags & ApplicationInfo.FLAG_SYSTEM) > 0) {// system apps
			return true;
		} else {
			return false;
		}
	}

	/**
	 * 获取应用信息
	 * 
	 * @param name
	 * @return
	 */
	private ArrayList<ApplicationInfo> getAppInfo(String[] pkgList,
			List<ApplicationInfo> appList) {
		if (pkgList == null) {
			return null;
		}

		ArrayList<ApplicationInfo> infos = new ArrayList<ApplicationInfo>(
				pkgList.length);

		for (String pkg : pkgList) {
			for (ApplicationInfo appinfo : appList) {
				if (pkg.equals(appinfo.packageName)) {
					infos.add(appinfo);
				}
			}
		}
		return infos;
	}

	public static class Programe {
		Drawable icon;
		String name;
	}

}
