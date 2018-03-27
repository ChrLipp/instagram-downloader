package at.corba.tools.instagram_downloader

import at.corba.tools.instagram_downloader.view.InvisibleSplashScreen
import at.corba.tools.instagram_downloader.view.MainView
import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport
import org.springframework.boot.autoconfigure.SpringBootApplication

@SpringBootApplication
class InstagramDownloaderApplication  extends AbstractJavaFxApplicationSupport
{
	static void main(String[] args)
	{
		launch(
			InstagramDownloaderApplication.class,
			MainView.class,
			new InvisibleSplashScreen(),
			args)
	}
}
