package at.corba.tools.instagram_downloader.view

import at.corba.tools.instagram_downloader.service.InstagramDownloadService
import com.jfoenix.controls.JFXButton
import com.jfoenix.controls.JFXProgressBar
import com.jfoenix.controls.JFXSlider
import com.jfoenix.controls.JFXTextField
import de.felixroske.jfxsupport.FXMLController
import groovy.util.logging.Slf4j
import org.apache.http.HttpException
import org.springframework.beans.factory.annotation.Autowired

import javafx.application.Platform
import javafx.beans.binding.Bindings
import javafx.beans.value.ChangeListener
import javafx.concurrent.Task
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.Cursor
import javafx.scene.control.Alert
import javafx.scene.control.Label
import javafx.scene.control.TextFormatter
import javafx.stage.DirectoryChooser
import javafx.util.converter.IntegerStringConverter
import javafx.util.converter.NumberStringConverter
import java.text.NumberFormat
import java.util.regex.Pattern

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
		// enable ok button and fields only when necessary data is entered
		ChangeListener cl = { observable, oldValue, newValue ->
			enableControls()
		}
		urlField.textProperty().addListener(cl)
		directoryField.textProperty().addListener(cl)

		// allow page entry with slider and field
		Bindings.bindBidirectional(
			pagesField.textProperty(),
			pagesSlider.valueProperty(),
			new NumberStringConverter(NumberFormat.integerInstance))

		// allow numeric input for pagesField only
		TextFormatter<Integer> formatter = new TextFormatter<>(
			new IntegerStringConverter(),
			1,
			{ c -> Pattern.matches("\\d*", c.getText()) ? c : null } )
		pagesField.textFormatter = formatter

		// white label only when cursor is in pagesField
		pagesField.focusedProperty().addListener({ arg0, oldValue, newValue ->
			pagesLabel.disable = !newValue
		} as ChangeListener<Boolean>)
	}

	/**
	 * Downloads a media file or n pages media files of an user.
	 * @param event
	 */
	@FXML
	private void download(final Event event)
	{
		okButton.scene.setCursor(Cursor.WAIT)
		okButton.disable = true

		def task = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				doDownload(event)
			}
		}
		task.setOnFailed({
			def throwable = task.getException()
			log.error 'Exception while downloading', throwable
			okButton.disable = false
			okButton.scene.setCursor(Cursor.DEFAULT)
			displayException(throwable)
		})

		final Thread thread = new Thread(task, "task-thread")
		thread.setDaemon(true)
		thread.start()
	}

	/**
	 * Download task function.
	 * @param event
	 */
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
			downloadService.downloadIndexfiles(
				urlField.text, directoryField.text,
				pagesSlider.value.toInteger(), progress)
		}
		Platform.runLater {
			okButton.scene.setCursor(Cursor.DEFAULT)
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
//		pagesLabel.disable = isMedia
		pagesField.disable = isMedia
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
		def message = setHeaderText(throwable)
		alert.setHeaderText(message.substring(0, Math.min(80, message.length())))
		alert.setContentText(throwable.message)
		alert.showAndWait()
	}

	/**
	 * Transforms an exception to an alert header text.
	 * @param e The exception
	 * @return  The header text
	 */
	private String setHeaderText(Exception e)
	{
		if (e.cause != null) {
			return setHeaderText(e.cause)
		}
		else if (e instanceof HttpException) {
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
		else if (e instanceof ConnectException) {
			return 'Timeout occurred. Please try again with a better connection.'
		}
		else if (e instanceof RuntimeException) {
			return e.message
		}
		else {
			return e.class.canonicalName
		}
	}

	/**
	 * Copies the link text to the url field.
	 * @param event
	 */
	@FXML
	private void linkMedia(final Event event)
	{
		urlField.text = 'https://www.instagram.com/p/'
		urlField.positionCaret(urlField.text.length())

	}

	/**
	 * Copies the link text to the url field.
	 * @param event
	 */
	@FXML
	private void linkUser(final Event event)
	{
		urlField.text = 'https://www.instagram.com/'
		urlField.positionCaret(urlField.text.length())
	}
}
