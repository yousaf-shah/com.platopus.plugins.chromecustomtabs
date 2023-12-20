# customChromeTabs-Plugin

Solar2D plugin which presents a Web View using Chrome Custom Tabs (https://developer.chrome.com/docs/android/custom-tabs) for use with OAuth and other secure web browsing needs.

For users who are familiar with the SafariView plugin and its use (https://docs.coronalabs.com/plugin/CoronaProvider_native_popup_safariView/index.html), this is the Android equivalent. It uses a secure tab from the user’s built in Chrome browser which shares cookies and caches with windows the user already has open elsewhere.

API 29 (Android 10) is required for this plugin.

## Status

This is the first release and it supports the basic use case. I will be adding a few features such as colour, link and animation customisation over the coming weeks. Feedback and issue reporting welcome via the issues tab here.

## Overview

To use the plugin, include it in your `build.settings` file as follows:

```
plugins = {
	["plugin.chromeCustomTabs"] =
	  {
	  publisherId = "com.platopus",
	  supportedPlatforms = {
	    android = { url="https://github.com/yousaf-shah/com.platopus.plugins.chromecustomtabs/releases/download/1.0.2/data.tgz" },
	  },
	},
}
```

Basic usage is as follows:

- Check if your device supports Chrome Custom Tabs using **supported()** it will return **true** if they are supported
- Initialise the plugin using **init(listener)**
- If you desire you can ‘warm up’ the tab using **warmup()**.
- If you desire you can ‘pre-load’ the content using **mayLaunch(url)**.
- Show the tab using **show(url)** and listen for results in your listener function.

When you issue **show()**, your listener will receive a `TAB_SHOW` event and then your app will be suspended. The listener will receive all the events in one go when the tab is closed.

If you are using a custom url scheme (for instance to finish off an oAuth sign-in), tour app will resume with the `applicationOpen` event and you should look for the launch URL there, other than this, you will not know where the user has browsed to in the tab beyond the url you opened the tab with.

If the user presses the device’s back icon/button during the custom tab session the tab will close and your app will not receive the back event - only the `TAB_HIDDEN` event in your listener.

A fuller example is included in the code [here](https://github.com/yousaf-shah/com.platopus.plugins.chromecustomtabs/blob/main/Corona/main.lua).

## Functions

bool **supported()**\
Returns true if the device supports Chrome Custom Tabs and false if it does not. If your device does not support them, the OS will open any urls you try to show in the built in browser.

bool **init(listener)**\
Sets up the Chrome Custom Tab. The `listener` function will receive events throughout the lifetime of the bound session. Returns **true** if the initialisation was successful.

bool **warmup()**\
Use of this is optional if you have time to invoke it before the user actions the tab to be shown. It will get the OS preloading chrome in the background to make the pop-up appear faster when the tab is shown, Google quotes up to 700ms faster in some cases. Returns `false` if there was not a session initialised by `init(listener)`.

bool **mayLaunch(url)**\
Use of this is optional if you have time to invoke it before the user actions the tab to be shown. It will tell the Chrome engine that you will be visiting this URL allowing it to pre-load content if it chooses to. Returns `false` if there was not a session initialised by `initCustomTab(listener, url)` or if there is no url supplied.

bool **show(url)**\
Shows the Chrome Custom Tab, suspending your app while the user interacts with the it. The `url` is optional if you supplied one during `init(listener)`. Returns `false` if there was not a session initialised by `initCustomTab( listener, url(optional) )` or if there is no url supplied .

bool **unbind()** (Experimental/In Progress)\
Try to unbind the session you initialised, I’m not convinced this is necessary but it may help you to fix issues where multiple tabs are fired throughout your app. Returns `false` if there was not a session initialised by `init(listener)`, but mostly in my testing it causes an exception if this is the case!


## Listener Events

After you have initialised using `init(listener)`, the `listener` function will receive the following events:

TAB_SHOW\
Immediately after **show(url)** is issued and before your app suspends to show the tab.

TAB_SHOWN*\
The tab was shown to the user.

TAB_HIDDEN\
The tab was closed using the close icon or the device’s back function.

TAB_WARMUP\
The **warmup()** function was called successfully.

TAB_MAYLAUNCH\
The **mayLaunch(url)** function was called successfully.

NAVIGATION_ABORTED*\
The user cancelled the page load before it finished loading.

NAVIGATION_FAILED*\
The url requested failed to load.

NAVIGATION_FINISHED*\
The url requested loaded successfully. If you receive this, you can be fairly sure the user saw the page you requested.

NAVIGATION_STARTED*\
The url requested started to load.
	
\* Generally you will receive all of these events together when the Chrome Tab session is closed and your app resumes.
