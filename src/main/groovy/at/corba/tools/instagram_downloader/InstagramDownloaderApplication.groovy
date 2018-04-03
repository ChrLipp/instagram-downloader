package at.corba.tools.instagram_downloader

import at.corba.tools.instagram_downloader.view.InvisibleSplashScreen
import at.corba.tools.instagram_downloader.view.MainView
import de.felixroske.jfxsupport.AbstractJavaFxApplicationSupport
import javafx.stage.Stage
import javafx.stage.StageStyle
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.ConfigurableApplicationContext

@SpringBootApplication
class InstagramDownloaderApplication  extends AbstractJavaFxApplicationSupport
{
	@Override
	void beforeInitialView(Stage stage, ConfigurableApplicationContext ctx)
	{
		stage.initStyle(StageStyle.TRANSPARENT)
	}

	static void main(String[] args)
	{
		launch(
			InstagramDownloaderApplication.class,
			MainView.class,
			new InvisibleSplashScreen(),
			args)
	}
}
