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

local function listener( event )

	log.text = log.text .. "\nAction received: " .. event.message
	print( "Received event from chromeCustomTabs plugin (" .. event.name .. "): ", event.message )

end

if chromeCustomTabs.supported() then
	
	chromeCustomTabs.init(listener)

end

function myTouchListener( event )

    if event.phase == "ended" then

	    if chromeCustomTabs.supported() then

			chromeCustomTabs.warmup()
			chromeCustomTabs.mayLaunch("https://solar2d.com/")

			print( "Init chromeCustomTabs plugin ...")

			timer.performWithDelay( 4000, function()

				chromeCustomTabs.show("https://solar2d.com/")

			end )

		else

			log.text = log.text .. "\nChrome Custom Tabs are not supported"
			print( "Custom Tabs not supported on this Device")

		end

	end

end
Runtime:addEventListener( "touch", myTouchListener )



