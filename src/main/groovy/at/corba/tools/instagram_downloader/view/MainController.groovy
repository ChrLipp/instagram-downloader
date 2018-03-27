package at.corba.tools.instagram_downloader.view

import at.corba.tools.instagram_downloader.service.InstagramDownloadService
import de.felixroske.jfxsupport.FXMLController
import groovy.util.logging.Slf4j
import groovyx.net.http.HttpException
import javafx.beans.value.ChangeListener
import javafx.event.Event
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.stage.DirectoryChooser
import org.springframework.beans.factory.annotation.Autowired

/**
 * Dialog controller.
 */
@FXMLController
@Slf4j
class MainController
{
	@Autowired
	InstagramDownloadService downloadService

	@FXML
	private TextField urlField

	@FXML
	private TextField directoryField

	@FXML
	private Spinner<Integer> pagesField

	@FXML
	private Label pagesLabel

	@FXML
	private Button okButton

	@FXML
	private ProgressBar progressBar

	/**
	 * Initializes the controller (gets called after Spring and FXML injections are done).
	 * @throws Exception
	 */
	@FXML
	void initialize() throws Exception
	{
		ChangeListener cl = { observable, oldValue, newValue ->
			enableControls()
		}
		urlField.textProperty().addListener(cl)
		directoryField.textProperty().addListener(cl)
	}

	/**
	 * Downloads a media file or n pages media files of an user.
	 * @param event
	 */
	@FXML
	private void download(final Event event)
	{
		try {
			progressBar.setProgress(0)
			if (urlField.text.toUpperCase().startsWith('HTTPS://WWW.INSTAGRAM.COM/P/')) {
				downloadService.downloadFile(
					urlField.text, directoryField.text)
			}
			else {
				downloadService.downloadIndexfile(
					urlField.text, directoryField.text, pagesField.value)
			}
			progressBar.setProgress(1)
			event.source.scene.window.hide()
		}
		catch (Exception e) {
			displayException(e)
		}
	}

	/**
	 * Closes the dialog.
	 * @param event
	 */
	@FXML
	private void cancel(final Event event)
	{
		event.source.scene.window.hide()
	}

	/**
	 * Displays a directory chooser.
	 * @param event
	 */
	@FXML
	private void selectDirectory(final Event event)
	{
		def directoryChooser = new DirectoryChooser()
		directoryChooser.title = 'Select destination directory'
		def file = directoryChooser.showDialog(null)
		if (file) {
			directoryField.text = file.absolutePath
		}
	}

	/**
	 * Change listener function for enabling / disabling controls.
	 */
	private void enableControls()
	{
		def url = urlField.text.toUpperCase()

		boolean isInValidInput =
			!url.startsWith('HTTPS://WWW.INSTAGRAM.COM/') ||
			directoryField.text.length() == 0
		okButton.disable = isInValidInput

		boolean isMedia = url.startsWith('HTTPS://WWW.INSTAGRAM.COM/P/')
		pagesLabel.disable = isInValidInput || isMedia
		pagesField.disable = isInValidInput || isMedia
	}

	/**
	 * Displays an exception in an user friendly manner.
	 * @param e The exception
	 */
	private void displayException(Exception e)
	{
		def alert = new Alert(Alert.AlertType.ERROR)
		alert.setTitle('An error occurred')
		alert.setHeaderText(setHeaderText(e))
		alert.setContentText(e.message)
		alert.showAndWait()
	}

	private String setHeaderText(Exception e)
	{
		if (e instanceof HttpException) {
			if (e.statusCode == 404) {
				return 'URL not found or resource is private.'
			}
			else {
				return "Status code ${e.statusCode}"
			}
		}
		else {
			return e.class.canonicalName
		}
	}
}
