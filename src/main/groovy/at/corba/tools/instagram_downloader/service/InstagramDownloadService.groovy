package at.corba.tools.instagram_downloader.service

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
	 * @param url   The browser URL of the resource
	 * @param dir   The destination directory
	 * @param pages How much pages do we load at maximum (one page is 12 media files).
	 *              0 means all pages available.
	 */
	void downloadIndexfile(String url, String dir, int pages)
	{
		def result = []
		def maxId = null
		if (pages == 0) {
			pages = -1
		}

		// collect the files
		while (pages != 0) {
			def indexJson = browser.get {
				request.uri = url
				request.uri.query = maxId ? ['__a': 1, 'max_id': maxId] : ['__a': 1]
			}
			def page = indexJson.graphql.user.edge_owner_to_timeline_media.edges.collect {
				it.node.shortcode
			}
			result.addAll(page)
			maxId = indexJson.graphql.user.edge_owner_to_timeline_media.page_info.end_cursor
			if (!indexJson.graphql.user.edge_owner_to_timeline_media.page_info.has_next_page) {
				break
			}

			--pages
		}

		// download the files
		result.eachWithIndex { item, idx ->
			log.info "Download item ${idx} of ${result.size()}"
			downloadFile("/p/$item", dir)
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
}
