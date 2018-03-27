package at.corba.tools.instagram_downloader.view

import de.felixroske.jfxsupport.SplashScreen

/**
 * Blocks the splash screen.
 */
class InvisibleSplashScreen extends SplashScreen
{
	@Override
	boolean visible()
	{
		false
	}
}
