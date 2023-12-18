package plugin.chromeCustomTabs;

import android.app.Activity;
import android.content.ComponentName;
import android.net.Uri;
import android.os.Bundle;
import androidx.browser.customtabs.CustomTabsCallback;
import androidx.browser.customtabs.CustomTabsClient;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.browser.customtabs.CustomTabsServiceConnection;
import androidx.browser.customtabs.CustomTabsSession;

import android.util.Log;

/**
 * -----------------------
 * ChromeCustomTab
 * -----------------------
 *
 * @author Tom Redman
 *         15-09-09
 */

public class ChromeCustomTabb {

    public static final String CUSTOM_TAB_PACKAGE_NAME = "com.android.chrome";

    private Activity activity;
    private String url;

    private CustomTabsSession customTabsSession;
    private CustomTabsClient client;
    private CustomTabsServiceConnection connection;

    private boolean warmupWhenReady = false;
    private boolean mayLaunchWhenReady = false;

    public ChromeCustomTabb(Activity activity, String url) {
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
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder(getSession());
//        builder.setToolbarColor(activity.getResources().getColor(android.R.color.buffer_blue)).setShowTitle(true);
//        builder.setStartAnimations(activity, android.R.anim.slide_in_left, android.R.anim.slide_out_right);
//        builder.setExitAnimations(activity, android.R.anim.slide_in_right, android.R.anim.slide_out_left);
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
                    Log.w("CustomTabs", "onNavigationEvent: Code = " + navigationEvent);
                }
            });
        }
        return customTabsSession;
    }

    private void bindCustomTabsService() {
        if (client != null) {
            return;
        }
        connection = new CustomTabsServiceConnection() {
            @Override
            public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                ChromeCustomTabb.this.client = client;

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