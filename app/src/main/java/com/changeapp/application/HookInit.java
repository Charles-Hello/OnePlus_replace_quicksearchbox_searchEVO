package com.changeapp.application;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class HookInit implements IXposedHookLoadPackage {

	@Override
	public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
		if (BuildConfig.APPLICATION_ID.equals(lpparam.packageName)) {
			XposedHelpers.findAndHookMethod(
				MainActivity.class.getName(),
				lpparam.classLoader,
				"isModuleActivated",
				XC_MethodReplacement.returnConstant(true));
		}
		
		// Hook all apps to intercept startActivity calls
		hookContextWrapper(lpparam);
	}
	
	private void hookContextWrapper(LoadPackageParam lpparam) {
		try {
			// Hook Context.startActivity method
			XposedHelpers.findAndHookMethod("android.content.ContextWrapper", 
				lpparam.classLoader, "startActivity", Intent.class, 
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						Intent intent = (Intent) param.args[0];
						modifyIntent(intent);
					}
				});
			
			// Hook Context.startActivity with Bundle
			XposedHelpers.findAndHookMethod("android.content.ContextWrapper", 
				lpparam.classLoader, "startActivity", Intent.class, "android.os.Bundle",
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						Intent intent = (Intent) param.args[0];
						modifyIntent(intent);
					}
				});
				
			// Hook Activity.startActivity
			XposedHelpers.findAndHookMethod("android.app.Activity", 
				lpparam.classLoader, "startActivity", Intent.class,
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						Intent intent = (Intent) param.args[0];
						modifyIntent(intent);
					}
				});
				
			// Hook Activity.startActivity with Bundle
			XposedHelpers.findAndHookMethod("android.app.Activity", 
				lpparam.classLoader, "startActivity", Intent.class, "android.os.Bundle",
				new XC_MethodHook() {
					@Override
					protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
						Intent intent = (Intent) param.args[0];
						modifyIntent(intent);
					}
				});
				
		} catch (Throwable t) {
			XposedBridge.log("Error hooking startActivity: " + t.getMessage());
		}
	}
	
	private void modifyIntent(Intent intent) {
		if (intent == null) return;
		
		try {
			// Check for explicit component
			if (intent.getComponent() != null) {
				ComponentName component = intent.getComponent();
				String packageName = component.getPackageName();
				
				if ("com.heytap.quicksearchbox".equals(packageName)) {
					ComponentName newComponent = new ComponentName(
						"com.jwg.searchEVO", 
						"com.jwg.searchEVO.MainActivity"
					);
					intent.setComponent(newComponent);
					XposedBridge.log("Redirected quicksearchbox to searchEVO");
				}
			}
			// Check for package name in intent
			else if (intent.getPackage() != null && "com.heytap.quicksearchbox".equals(intent.getPackage())) {
				intent.setComponent(new ComponentName(
					"com.jwg.searchEVO", 
					"com.jwg.searchEVO.MainActivity"
				));
				intent.setPackage("com.jwg.searchEVO");
				XposedBridge.log("Redirected package quicksearchbox to searchEVO");
			}
			// Check for action with package in data
			else if (Intent.ACTION_MAIN.equals(intent.getAction()) && 
					 intent.hasCategory(Intent.CATEGORY_LAUNCHER)) {
				// For launcher intents, we need to check the data or extras
				String targetPackage = intent.getStringExtra("package");
				if ("com.heytap.quicksearchbox".equals(targetPackage)) {
					intent.setComponent(new ComponentName(
						"com.jwg.searchEVO", 
						"com.jwg.searchEVO.MainActivity"
					));
					XposedBridge.log("Redirected launcher intent for quicksearchbox to searchEVO");
				}
			}
		} catch (Throwable t) {
			XposedBridge.log("Error modifying intent: " + t.getMessage());
		}
	}

}
