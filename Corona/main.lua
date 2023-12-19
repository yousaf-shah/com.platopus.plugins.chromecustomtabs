local chromeCustomTabs = require "plugin.chromeCustomTabs"

local myRectangle = display.newRect( display.contentCenterX, display.contentCenterY, display.contentWidth, display.contentHeight )
myRectangle:setFillColor( 0.5 )

local log = display.newText{
    text = "Tap anywhere to present chromeCustomTab.\n",
    x = display.contentCenterX,
    y = display.contentCenterY,
    width = display.contentWidth,
    fontSize = 10,
    align = "center"
}

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

function myTouchListener( event )

    if event.phase == "ended" then

	    if chromeCustomTabs.supportsCustomTabs() then

			local function listener( event )

				log.text = log.text .. "\nAction received: " .. event.message
				print( "Received event from chromeCustomTabs plugin (" .. event.name .. "): ", event.message )

			end

			chromeCustomTabs.initCustomTab( "https://solar2d.com/", listener )

			chromeCustomTabs.warmupCustomTab()

			print( "Init chromeCustomTabs plugin ...")

			timer.performWithDelay( 4000, function()

				chromeCustomTabs.showCustomTab()

			end )

		else


			log.text = log.text .. "\nChrome Custom Tabs are not supported"
			print( "Custom Tabs not supported on this Device")

		end

	end

end
Runtime:addEventListener( "touch", myTouchListener )



