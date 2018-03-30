package at.corba.tools.instagram_downloader.view

import at.corba.tools.instagram_downloader.service.InstagramDownloadService
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXProgressBar
import com.jfoenix.controls.JFXSlider
import com.jfoenix.controls.JFXTextField
import de.felixroske.jfxsupport.FXMLController
import groovy.util.logging.Slf4j
import groovyx.net.http.HttpException
import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.value.ChangeListener
import javafx.concurrent.Task
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.stage.DirectoryChooser
import javafx.util.converter.NumberStringConverter
import org.springframework.beans.factory.annotation.Autowired
/**
 * Dialog controller.
 */
@FXMLController
@Slf4j
class MainController implements Initializable
{
	@Autowired
	InstagramDownloadService downloadService

	@FXML
	private JFXTextField urlField

	@FXML
	private JFXTextField directoryField

	@FXML
	private JFXTextField pagesField

	@FXML
	private JFXSlider pagesSlider

	@FXML
	private Label pagesLabel

	@FXML
	private JFXButton okButton

	@FXML
	private JFXProgressBar progressBar

	/**
	 * Initializes the controller (gets called after Spring and FXML injections are done).
	 * @throws Exception
	 */
	@Override
	void initialize(URL location, ResourceBundle resources)
	{
		ChangeListener cl = { observable, oldValue, newValue ->
			enableControls()
		}
		urlField.textProperty().addListener(cl)
		directoryField.textProperty().addListener(cl)

		Bindings.bindBidirectional(
			pagesField.textProperty(),
			pagesSlider.valueProperty(),
			new NumberStringConverter())
	}

	/**
	 * Downloads a media file or n pages media files of an user.
	 * @param event
	 */
	@FXML
	private void download(final Event event)
	{
		def task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				doDownload(event)
			}
		}
		task.setOnFailed({
			def throwable = task.getException()
			log.error 'Exception while downloading', throwable
			displayException(throwable)
		})

		final Thread thread = new Thread(task, "task-thread")
		thread.setDaemon(true)
		thread.start()
	}

	void doDownload(final Event event)
	{
		if (urlField.text.toUpperCase().startsWith('HTTPS://WWW.INSTAGRAM.COM/P/'))
		{
			downloadService.downloadFile(
				urlField.text, directoryField.text)
		}
		else
		{
			def progress = { int current, int max ->
				Platform.runLater {
					progressBar.setProgress((double) (current / max))
				}
			}
			downloadService.downloadIndexfile(
				urlField.text, directoryField.text,
				pagesSlider.value.toInteger(), progress)
		}
		Platform.runLater {
			progressBar.setProgress(1.0)
			event.source.scene.window.hide()
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
		pagesLabel.disable = isMedia
		pagesSlider.disable = isMedia
	}

	/**
	 * Displays an exception in an user friendly manner.
	 * @param e The exception
	 */
	private void displayException(Throwable throwable)
	{
		def alert = new Alert(Alert.AlertType.ERROR)
		alert.setTitle('An error occurred')
		alert.setHeaderText(setHeaderText(throwable))
		alert.setContentText(throwable.message)
		alert.showAndWait()
	}

	private String setHeaderText(Exception e)
	{
		if (e instanceof HttpException) {
			if (e.statusCode == 404) {
				return 'URL not found or resource is private.'
			}
			else if (e.statusCode == 429) {
				return 'Rate limit applied, please wait some time.'
			}
			else {
				return "Status code ${e.statusCode}"
			}
		}
		else {
			return e.class.canonicalName
		}
	}

	@FXML
	private void linkMedia(final Event event)
	{
		urlField.text = 'https://www.instagram.com/p/'
	}

	@FXML
	private void linkUser(final Event event)
	{
		urlField.text = 'https://www.instagram.com/'
	}
}
