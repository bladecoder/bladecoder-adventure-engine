package com.bladecoder.engine;

import com.bladecoder.engine.BladeEngine;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.support.v4.app.ActivityCompat;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;

import com.badlogic.gdx.backends.android.APKExpansionSupport;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidFiles;
import com.badlogic.gdx.utils.GdxRuntimeException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import com.badlogic.gdx.Gdx;

public class AndroidLauncher extends AndroidApplication {
	private static final String TAG = "AndroidLauncher";

	private final AndroidEngine bladeEngine = new AndroidEngine();

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AndroidApplicationConfiguration cfg = new AndroidApplicationConfiguration();
		cfg.useAccelerometer = false;
		cfg.useCompass = false;
		// cfg.numSamples = 2;
		cfg.useImmersiveMode = true;

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

		initialize(bladeEngine, cfg);

		if (BuildConfig.EXPANSION_FILE_VERSION > 0) {

			boolean success = expansionFilesExists(BuildConfig.EXPANSION_FILE_VERSION, 0);

			if (!success) {
				if (Build.VERSION.SDK_INT >= 23) {
					if (checkSelfPermission(
							Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
						Log.e(TAG, "Permission is granted but .obb was not found.");
						// TODO Download .obb
						exit();
					} else {
						Log.v(TAG, "Permission is revoked, requesting...");
						bladeEngine.setWaitingForPermissions(true);
						ActivityCompat.requestPermissions(AndroidLauncher.this,
								new String[] { Manifest.permission.READ_EXTERNAL_STORAGE }, 2);
					}
				} else { // permission is automatically granted on sdk<23 upon installation
					Log.e(TAG, "Permission is granted in Manifest (sdk<23) but .obb was not found.");
					// TODO Download .obb
					exit();
				}
			}
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
		case 2:
			Log.d(TAG, "External storage2");
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);

				boolean success = ((AndroidFiles) Gdx.files).setAPKExpansion(BuildConfig.EXPANSION_FILE_VERSION, 0);

				if (!success) {
					Log.e(TAG, "Permission accepted but .obb was not found.");

					// TODO Download .obb
					exit();
				} 
			} else {
				Log.e(TAG, "Permission denied by user.");
				exit();
			}
			break;
		}
	}

	private boolean expansionFilesExists(int mainVersion, int patchVersion) {
		try {
			return APKExpansionSupport.getAPKExpansionZipFile(getContext(), mainVersion, patchVersion) != null;
		} catch (IOException ex) {
			Log.v(TAG, "APK expansion main version " + mainVersion + " or patch version " + patchVersion
					+ " couldn't be opened!");
		}

		return false;
	}

	class AndroidEngine extends BladeEngine {
		private boolean waitingForPermissions = false;

		@Override
		public void create() {
			if(waitingForPermissions)
				return;

			if (BuildConfig.EXPANSION_FILE_VERSION > 0) {
				boolean success = ((AndroidFiles) Gdx.files).setAPKExpansion(BuildConfig.EXPANSION_FILE_VERSION, 0);

				if(!success) {
					Log.e(TAG, "Can not set APK expansion.");
					Gdx.app.exit();
				} else {
					Log.d(TAG, "Expansion file stablish successfully!");
				}
			}

			super.create();
		}

		@Override
		public void resume() {
			if(waitingForPermissions) {
				// returns from querying permissions
				bladeEngine.setWaitingForPermissions(false);
				bladeEngine.create();
			}

			super.resume();
		}

		public void setWaitingForPermissions(boolean v) {
			waitingForPermissions = v;
		}
	}
}