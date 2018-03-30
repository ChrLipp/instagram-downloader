package at.corba.tools.instagram_downloader.service

import at.corba.tools.instagram_downloader.view.IProgressbar
import groovy.util.logging.Slf4j
import groovyx.net.http.optional.Download
import org.springframework.stereotype.Component

import static groovyx.net.http.ContentTypes.JSON
import static groovyx.net.http.HttpBuilder.configure

@Slf4j
@Component
class InstagramDownloadService
{
	/** the instagram browser */
	private browser = configure {
		request.headers['User-Agent'] = 'Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0'
		request.uri = 'https://www.instagram.com/'
	}

	/**
	 * Downloads <pages> pages media files of an user.
	 * @param url       The browser URL of the resource
	 * @param dir       The destination directory
	 * @param pages     How much pages do we load at maximum (one page is 12 media files).
	 *                  0 means all pages available.
	 * @param progress  Optional progress bar interface
	 */
	void downloadIndexfile(String url, String dir, int pages, IProgressbar progress = null)
	{
		def result = []
		def params = null
		if (pages == 0) {
			pages = -1
		}

		// collect the files
		while (pages != 0) {
			if (params == null) {
				params = downloadFirstIndexPage(url, result)
				if (params == null) {
					break
				}
			}
			else {
				if (!downloadNextIndexPage(params, result)) {
					break
				}
			}

			--pages
		}

		// defining values for progress bar
		def max = result.size() + 1
		progress?.setProgress(1, max)

		// download the files
		result.eachWithIndex { item, idx ->
			log.info "Download item ${idx} of ${result.size()}"
			downloadFile("/p/$item", dir)
			progress?.setProgress(idx + 2, max)
		}
	}

	/**
	 * Downloads an Instagram resource.
	 * @param url   The browser URL of the resource
	 * @param dir   The destination directory
	 */
	void downloadFile(String url, String dir)
	{
		def metaData = retrieveMetaData(url)
//		println JsonOutput.prettyPrint(JsonOutput.toJson(metaData))

		String linkToDownload

		if (metaData.graphql.shortcode_media.is_video) {
			linkToDownload = metaData.graphql.shortcode_media.video_url
		}
		else {
			def imagesArray = metaData.graphql.shortcode_media.display_resources
			def maxResolution = imagesArray.max { it.config_height }
			linkToDownload = maxResolution.src
		}

		def fileName = linkToDownload.split('/').last()
		def destination = new File(dir, fileName)
		if (!destination.exists()) {
			browser.get {
				request.uri = linkToDownload
				Download.toFile(delegate, destination)
			}
		}
	}

	/**
	 * Retrieves the meta data of a media file or an user.
	 * @param url   Browser URL of the media file or the user
	 * @return      JSON meta data
	 */
	private Object retrieveMetaData(String url)
	{
		return browser.get {
			request.uri = url
			request.uri.query = ['__a': 1]
			request.contentType = JSON[0]
		}
	}

	private HashMap<String, String> downloadFirstIndexPage(
		String url, List<String> result)
	{
		def indexJson = browser.get {
			request.uri = url
			request.uri.query = ['__a': 1]
		}
		def page = indexJson.graphql.user.edge_owner_to_timeline_media.edges.collect {
			it.node.shortcode
		}
		result.addAll(page)

		if (!indexJson.graphql.user.edge_owner_to_timeline_media.page_info.has_next_page) {
			return null
		}

		def params = [:]
		params['query_id'] = '17888483320059182'
		params['id'] = indexJson.graphql.user.id
		params['first'] = '12'
		params['after'] = indexJson.graphql
			.user.edge_owner_to_timeline_media.page_info.end_cursor

		params
	}

	private boolean downloadNextIndexPage(HashMap<String, String> params, List<String> result)
	{
		def indexJson = browser.get {
			request.uri = '/graphql/query'
			request.uri.query = params
		}
		def page = indexJson.data.user.edge_owner_to_timeline_media.edges.collect {
			it.node.shortcode
		}
		result.addAll(page)

		if (!indexJson.data.user.edge_owner_to_timeline_media.page_info.has_next_page) {
			return false
		}

		params['after'] = indexJson.data
			.user.edge_owner_to_timeline_media.page_info.end_cursor
		true
	}
}
