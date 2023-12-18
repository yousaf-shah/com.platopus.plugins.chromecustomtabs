//
//  LuaLoader.java
//  TemplateApp
//
//  Copyright (c) 2023 Platopus Systems Limited. All rights reserved.
//

// This corresponds to the name of the Lua library,
// e.g. [Lua] require "plugin.chromeCustomTabs"
package plugin.chromeCustomTabs;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static androidx.core.app.ActivityCompat.startActivityForResult;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.ColorInt;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;
import androidx.core.content.ContextCompat;

import com.ansca.corona.CoronaActivity;
import com.ansca.corona.CoronaEnvironment;
import com.ansca.corona.CoronaLua;
import com.ansca.corona.CoronaRuntime;
import com.ansca.corona.CoronaRuntimeListener;
import com.ansca.corona.CoronaRuntimeTask;
import com.naef.jnlua.JavaFunction;
import com.naef.jnlua.LuaState;
import com.naef.jnlua.NamedJavaFunction;

import java.util.Collections;
import java.util.List;

/**
 * Implements the Lua interface for a Corona plugin.
 * <p>
 * Only one instance of this class will be created by Corona for the lifetime of the application.
 * This instance will be re-used for every new Corona activity that gets created.
 */
@SuppressWarnings({"WeakerAccess", "unused"})
public class LuaLoader implements JavaFunction, CoronaRuntimeListener {
	/** Lua registry ID to the Lua function to be called when the ad request finishes. */
	private int fListener;

	private ChromeCustomTab cCT;


	/** This corresponds to the event name, e.g. [Lua] event.name */
	private static final String EVENT_NAME = "pluginLibraryEvent";


	/**
	 * Creates a new Lua interface to this plugin.
	 * <p>
	 * Note that a new LuaLoader instance will not be created for every CoronaActivity instance.
	 * That is, only one instance of this class will be created for the lifetime of the application process.
	 * This gives a plugin the option to do operations in the background while the CoronaActivity is destroyed.
	 */
	@SuppressWarnings("unused")
	public LuaLoader() {
		// Initialize member variables.
		fListener = CoronaLua.REFNIL;

		// Set up this plugin to listen for Corona runtime events to be received by methods
		// onLoaded(), onStarted(), onSuspended(), onResumed(), and onExiting().
		CoronaEnvironment.addRuntimeListener(this);
	}

	/**
	 * Called when this plugin is being loaded via the Lua require() function.
	 * <p>
	 * Note that this method will be called every time a new CoronaActivity has been launched.
	 * This means that you'll need to re-initialize this plugin here.
	 * <p>
	 * Warning! This method is not called on the main UI thread.
	 * @param L Reference to the Lua state that the require() function was called from.
	 * @return Returns the number of values that the require() function will return.
	 *         <p>
	 *         Expected to return 1, the library that the require() function is loading.
	 */
	@Override
	public int invoke(LuaState L) {
		// Register this plugin into Lua with the following functions.
		NamedJavaFunction[] luaFunctions = new NamedJavaFunction[] {
			new InitCustomTabWrapper(),
			new supportsCustomTabs(),
			new showCustomTab(),
		};
		String libName = L.toString( 1 );
		L.register(libName, luaFunctions);

		// Returning 1 indicates that the Lua require() function will return the above Lua library.
		return 1;
	}

	/**
	 * Called after the Corona runtime has been created and just before executing the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been loaded/initialized.
	 *                Provides a LuaState object that allows the application to extend the Lua API.
	 */
	@Override
	public void onLoaded(CoronaRuntime runtime) {
		// Note that this method will not be called the first time a Corona activity has been launched.
		// This is because this listener cannot be added to the CoronaEnvironment until after
		// this plugin has been required-in by Lua, which occurs after the onLoaded() event.
		// However, this method will be called when a 2nd Corona activity has been created.

	}

	/**
	 * Called just after the Corona runtime has executed the "main.lua" file.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been started.
	 */
	@Override
	public void onStarted(CoronaRuntime runtime) {
	}

	/**
	 * Called just after the Corona runtime has been suspended which pauses all rendering, audio, timers,
	 * and other Corona related operations. This can happen when another Android activity (ie: window) has
	 * been displayed, when the screen has been powered off, or when the screen lock is shown.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been suspended.
	 */
	@Override
	public void onSuspended(CoronaRuntime runtime) {
		Log.w("CustomTabs", "onSuspended: App Suspended");
	}

	/**
	 * Called just after the Corona runtime has been resumed after a suspend.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that has just been resumed.
	 */
	@Override
	public void onResumed(CoronaRuntime runtime) {
		Log.w("CustomTabs", "onResume: App Resumed");
	}

	/**
	 * Called just before the Corona runtime terminates.
	 * <p>
	 * This happens when the Corona activity is being destroyed which happens when the user presses the Back button
	 * on the activity, when the native.requestExit() method is called in Lua, or when the activity's finish()
	 * method is called. This does not mean that the application is exiting.
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param runtime Reference to the CoronaRuntime object that is being terminated.
	 */
	@Override
	public void onExiting(CoronaRuntime runtime) {
		// Remove the Lua listener reference.
		CoronaLua.deleteRef( runtime.getLuaState(), fListener );
		fListener = CoronaLua.REFNIL;

		if( null != cCT ) {
			cCT.unbindCustomTabsService();
			Log.w("CustomTabs", "onExiting - Unbind Custom Tabs Service" );
		}

	}

	/**
	 * Simple example on how to dispatch events to Lua. Note that events are dispatched with
	 * Runtime dispatcher. It ensures that Lua is accessed on it's thread to avoid race conditions
	 * @param message simple string to sent to Lua in 'message' field.
	 */
	@SuppressWarnings("unused")
	public void dispatchEvent(final String message) {

		CoronaEnvironment.getCoronaActivity().getRuntimeTaskDispatcher().send( new CoronaRuntimeTask() {
			@Override
			public void executeUsing(CoronaRuntime runtime) {
				LuaState L = runtime.getLuaState();

				CoronaLua.newEvent( L, EVENT_NAME );

				L.pushString(message);
				L.setField(-2, "message");

				try {
					CoronaLua.dispatchEvent( L, fListener, 0 );
				} catch (Exception ignored) {}
			}
		} );
	}

	/**
	 * The following Lua function has been called:  chromeCustomTabs.initCustomTab( listener )
	 * <p>
	 * Warning! This method is not called on the main thread.
	 * @param L Reference to the Lua state that the Lua function was called from.
	 * @return Returns the number of values to be returned by the chromeCustomTabs.initCustomTab() function.
	 */
	@SuppressWarnings({"WeakerAccess", "SameReturnValue"})
	public int initCustomTab(LuaState L) {

		int urlIndex = 1;
		int listenerIndex = 2;

		CoronaActivity activity = CoronaEnvironment.getCoronaActivity();
		if (activity == null) {
			L.pushBoolean(false);
			return 1;
		}

		// Fetch the first argument from the called Lua function.
		String url = L.checkString( urlIndex );
		if ( null == url ) {
			L.pushBoolean(false);
			return 1;
		}

		if ( CoronaLua.isListener( L, listenerIndex, EVENT_NAME ) ) {
			fListener = CoronaLua.newRef( L, listenerIndex );
		}

		cCT = new ChromeCustomTab(activity,url);

		L.pushBoolean(true);
		return 1;
	}

	/** Implements the chromeCustomTabs.initCustomTab() Lua function. */
	private class InitCustomTabWrapper implements NamedJavaFunction {
		/**
		 * Gets the name of the Lua function as it would appear in the Lua script.
		 * @return Returns the name of the custom Lua function.
		 */
		@Override
		public String getName() {
			return "initCustomTab";
		}
		
		/**
		 * This method is called when the Lua function is called.
		 * <p>
		 * Warning! This method is not called on the main UI thread.
		 * @param L Reference to the Lua state.
		 *                 Needed to retrieve the Lua function's parameters and to return values back to Lua.
		 * @return Returns the number of values to be returned by the Lua function.
		 */
		@Override
		public int invoke(LuaState L) {
			return initCustomTab(L);
		}
	}

	/** Implements the chromeCustomTabs.showCustomTab() Lua function. */
	private class showCustomTab implements NamedJavaFunction {
		/**
		 * Gets the name of the Lua function as it would appear in the Lua script.
		 * @return Returns the name of the custom Lua function.
		 */
		@Override
		public String getName() {
			return "showCustomTab";
		}

		/**
		 * This method is called when the Lua function is called.
		 * <p>
		 * Warning! This method is not called on the main UI thread.
		 * @param L Reference to the Lua state.
		 *                 Needed to retrieve the Lua function's parameters and to return values back to Lua.
		 * @return Returns the number of values to be returned by the Lua function.
		 */
		@Override
		public int invoke(LuaState L) {

			if (null == cCT) {
				L.pushBoolean(false);
				return 1;
			}

			dispatchEvent("TAB_SHOW");
			cCT.show();
			L.pushBoolean(true);
			return 1;
		}
	}

	/** Implements the chromeCustomTabs.supportsCustomTabs() Lua function. */
	private class supportsCustomTabs implements NamedJavaFunction {
		/**
		 * Gets the name of the Lua function as it would appear in the Lua script.
		 * @return Returns the name of the custom Lua function.
		 */
		@Override
		public String getName() {
			return "supportsCustomTabs";
		}

		/**
		 * This method is called when the Lua function is called.
		 * <p>
		 * Warning! This method is not called on the main UI thread.
		 * @param L Reference to the Lua state.
		 *                 Needed to retrieve the Lua function's parameters and to return values back to Lua.
		 * @return Returns the number of values to be returned by the Lua function.
		 */
		@Override
		public int invoke(LuaState L) {

			String packageName = CustomTabsClient.getPackageName(
					CoronaEnvironment.getCoronaActivity(),
					Collections.emptyList()
			);

			// True is Custom Tabs are supported by the default browser
			L.pushBoolean(packageName != null);
			return 1;
		}
	}
	public class ChromeCustomTab {

		public static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";

		private Activity activity;
		private String url;

		private CustomTabsSession customTabsSession;
		private CustomTabsClient client;
		private CustomTabsServiceConnection connection;

		private boolean warmupWhenReady = false;
		private boolean mayLaunchWhenReady = false;

		public ChromeCustomTab(Activity activity, String url) {
			this.activity = activity;
			this.url = url;
			bindCustomTabsService();
		}

		public void warmup() {
			if (client != null) {
				warmupWhenReady = false;
				client.warmup(0);
			}
			else {
				warmupWhenReady = true;
			}
		}

		public void mayLaunch() {
			CustomTabsSession session = getSession();
			if (client != null) {
				mayLaunchWhenReady = false;
				session.mayLaunchUrl(Uri.parse(url), null, null);
			}
			else {
				mayLaunchWhenReady = true;
			}
		}

		public void show() {

			@ColorInt int colorPrimaryLight = Color.parseColor("#3c3c3c");
			@ColorInt int colorPrimaryDark = Color.parseColor("#2c2c2c");

			CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(getSession());

			builder.setDefaultColorSchemeParams(new CustomTabColorSchemeParams.Builder()
					.setToolbarColor(colorPrimaryLight)
					.build());

			// set the alternative dark color scheme
			builder.setColorSchemeParams(CustomTabsIntent.COLOR_SCHEME_DARK, new CustomTabColorSchemeParams.Builder()
					.setToolbarColor(colorPrimaryDark)
					.build());

			builder.setStartAnimations(activity, android.R.anim.fade_in, android.R.anim.fade_out);
			builder.setExitAnimations(activity, android.R.anim.fade_in, android.R.anim.fade_out);
			builder.setUrlBarHidingEnabled(true);
			builder.setShowTitle(true);
//			builder.setInitialActivityHeightPx(400);
			builder.setToolbarCornerRadiusDp(10);

			CustomTabsIntent customTabsIntent = builder.build();
			customTabsIntent.launchUrl(activity, Uri.parse(url));

		}

		private CustomTabsSession getSession() {
			if (client == null) {
				customTabsSession = null;
			} else if (customTabsSession == null) {
				customTabsSession = client.newSession(new CustomTabsCallback() {
					@Override
					public void onNavigationEvent(int navigationEvent, Bundle extras) {
						String event;
						switch (navigationEvent) {
							case CustomTabsCallback.NAVIGATION_ABORTED:
								event = "NAVIGATION_ABORTED";
								break;
							case CustomTabsCallback.NAVIGATION_FAILED:
								event = "NAVIGATION_FAILED";
								break;
							case CustomTabsCallback.NAVIGATION_FINISHED:
								event = "NAVIGATION_FINISHED";
								break;
							case CustomTabsCallback.NAVIGATION_STARTED:
								event = "NAVIGATION_STARTED";
								break;
							case CustomTabsCallback.TAB_SHOWN:
								event = "TAB_SHOWN";
								break;
							case CustomTabsCallback.TAB_HIDDEN:
								event = "TAB_HIDDEN";
								break;
							default:
								event = String.valueOf(navigationEvent);
						}
						Log.w("CustomTabs", "onNavigationEvent: [" + navigationEvent + "] " + event);
						dispatchEvent(event);
					}
				});
			}
			return customTabsSession;
		}

		private void bindCustomTabsService() {
			if (client != null) {
				return;
			}

			Log.w("CustomTabs", "Binding Custom Tabs Service ..." );

			connection = new CustomTabsServiceConnection() {
				@Override
				public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
					ChromeCustomTab.this.client = client;

					if (warmupWhenReady) {
						warmup();
					}

					if (mayLaunchWhenReady) {
						mayLaunch();
					}
				}

				@Override
				public void onServiceDisconnected(ComponentName name) {
					client = null;
				}
			};
			boolean ok = CustomTabsClient.bindCustomTabsService(activity, CUSTOM_TAB_PACKAGE_NAME, connection);
			if (!ok) {
				connection = null;
			}
		}

		public void unbindCustomTabsService() {
			if (connection == null) {
				return;
			}
			activity.unbindService(connection);
			client = null;
			customTabsSession = null;
		}
	}
}
