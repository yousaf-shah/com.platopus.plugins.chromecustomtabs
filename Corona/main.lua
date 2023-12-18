local chromeCustomTabs = require "plugin.chromeCustomTabs"

-- This event is dispatched to the global Runtime object
-- by `didLoadMain:` in MyCoronaDelegate.mm
local function delegateListener( event )
	native.showAlert(
		"Event dispatched from `didLoadMain:`",
		"of type: " .. tostring( event.name ),
		{ "OK" } )
end

Runtime:addEventListener( "delegate", delegateListener )

-- This event is dispatched to the following Lua function
-- by PluginLibrary::show() in PluginLibrary.mm

local function listener( event )

	print( "Received event from chromeCustomTabs plugin (" .. event.name .. "): ", event.message )

end


if chromeCustomTabs.supportsCustomTabs() then

	chromeCustomTabs.initCustomTab( "https://platopusretail.com/", listener )

	print( "Init chromeCustomTabs plugin ...")

	timer.performWithDelay( 1000, function()

		chromeCustomTabs.showCustomTab( "https://platopusretail.com/" )

	end )

else

	print( "Custom Tabs not supported on this Device")

end

