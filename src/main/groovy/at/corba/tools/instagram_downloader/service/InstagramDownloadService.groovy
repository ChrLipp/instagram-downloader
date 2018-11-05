package at.corba.tools.instagram_downloader.service

import org.apache.http.client.fluent.Executor
import org.apache.http.client.fluent.Request
import org.apache.http.client.utils.URIBuilder
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Autowired

import java.security.MessageDigest
import java.util.regex.Pattern

import at.corba.tools.instagram_downloader.view.IProgressbar
import groovy.json.JsonBuilder
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j
import org.springframework.stereotype.Component

/**
 * Sevice which implements the Instagram Download functionality.
 */
@Slf4j
@Component
class InstagramDownloadService
{
	/** Instagram base url */
	private static String BASE_URL = 'https://www.instagram.com/'

	/** Apache HttpClient Executor */
	@Autowired
	private Executor executor

	/**
	 * Downloads <pages> pages media files and the linked content of an user.
	 * @param url       The browser URL of the resource
	 * @param dir       The destination directory
	 * @param pages     How much pages do we load at maximum (one page is 12 media files).
	 *                  0 means all pages available.
	 * @param progress  Optional progress bar interface
	 */
	void downloadIndexfiles(String url, String dir, int pages, IProgressbar progress = null)
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
			log.info "Download item ${idx + 1} of ${result.size()}"
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
		// get the describing meta data (a json)
		def metaData = retrieveMetaData(url)
//		println JsonOutput.prettyPrint(JsonOutput.toJson(metaData))

		// parse json to get a link for a video or a picture in the highest available resolution
		String linkToDownload

		if (metaData.graphql.shortcode_media.is_video) {
			linkToDownload = metaData.graphql.shortcode_media.video_url
		}
		else {
			def imagesArray = metaData.graphql.shortcode_media.display_resources
			def maxResolution = imagesArray.max { it.config_height }
			linkToDownload = maxResolution.src
		}

		// download and store it
		def fileName = linkToDownload.split('/').last()
		def destination = new File(dir, fileName)
		if (!destination.exists()) {
			executor.execute(Request.Get(linkToDownload))
					.saveContent(destination)
		}
	}

	/**
	 * Retrieves the meta data (the describing json) of a media file.
	 * @param url   Browser URL of the media file or the user
	 * @return      JSON meta data
	 */
	private Object retrieveMetaData(String url)
	{
		def uri = buildUri("$BASE_URL/$url", ['__a': '1'])
		def metaData = executor
				.execute(Request.Get(uri))
				.returnContent().asString()
		new JsonSlurper().parseText(metaData)
	}

	/**
	 * Downloads the first index page. This API was originally
	 * capable of pagination, but Instagram disabled this
	 * functionality. Therefor it is only useful for the first call.
	 * @param url       URL of an instagram profil
	 * @param result    Media urls
	 * @return Settings for the following calls
	 */
	private HashMap<String, String> downloadFirstIndexPage(
		String url, List<String> result)
	{
		// get html instead of json because Instagram restricted API again
		def indexHtml = executor
				.execute(Request.Get(url))
				.returnContent().asString()
		def indexDocument = Jsoup.parse(indexHtml)

		// strip embedded json from html with jsoup and regex
		def javaScript = indexDocument.select('body script').first()
		def p = Pattern.compile('window._sharedData = (.+);')
		def m = p.matcher(javaScript.html())
		if (!m.find()) {
			throw new IllegalStateException('Instagram changed result structure')
		}
		def jsonAsString = m.group(1).toString()

		// parse json
		def json = new JsonSlurper().parseText(jsonAsString)
		def profiles = json.entry_data.ProfilePage
		def user = profiles[0].graphql.user
		def page = user.edge_owner_to_timeline_media.edges.collect {
			it.node.shortcode
		}
		result.addAll(page)

		// return end of pagination
		if (!user.edge_owner_to_timeline_media.page_info.has_next_page) {
			return null
		}

		// return the necessary parameters for the next page call
		def params = [:]
		params['query_id'] = loadQueryId(indexDocument)
		params['id'] = user.id
		params['first'] = '12'
		params['after'] = user.edge_owner_to_timeline_media.page_info.end_cursor
		params['csrf'] = json.config.csrf_token
		params['rhx_gis'] = json.rhx_gis

		params
	}

	/**
	 * Gets the necessary query id from a linked JS ressource.
	 * @param indexDocument root document which contains the link to the JS
	 * @return the query id
	 */
	String loadQueryId(def indexDocument)
	{
		// download JS
		def url = indexDocument.select('link[href*=ProfilePageContainer.js]').first().attr('href')
		def queryIdJs = executor
				.execute(Request.Get("$BASE_URL$url"))
				.returnContent().asString()

		// extract queryId with regex
		def p = Pattern.compile('r\\.pagination\\},queryId\\:\\"([^&]+)\\"')
		def m = p.matcher(queryIdJs)
		if (m.find()) {
			def queryId = m.group(1).toString()
			return queryId
		}
		else {
			throw new IllegalStateException('Could not find Session ID')
		}
	}

	/**
	 * Downloads the subsequent index pages.
	 * @param params Settings for the following calls
	 * @param result Media urls
	 * @return true when there are new pages
	 */
	private boolean downloadNextIndexPage(HashMap<String, String> params, List<String> result)
	{
		// download json
		def queryParams = generateQueryParams(params)
		def uri = buildUri("$BASE_URL/graphql/query/", queryParams)
		def checksum = buildChecksum(params.rhx_gis, queryParams.variables)
		def json = executor
				.execute(Request
					.Get(uri)
					.addHeader('x-instagram-gis', checksum))
				.returnContent().asString()

		// parse json
		def indexJson = new JsonSlurper().parseText(json)
		def page = indexJson.data.user.edge_owner_to_timeline_media.edges.collect {
			it.node.shortcode
		}
		result.addAll(page)

		// return end of pagination
		if (!indexJson.data.user.edge_owner_to_timeline_media.page_info.has_next_page) {
			return false
		}

		// return the cursor for the next page call
		params['after'] = indexJson.data
			.user.edge_owner_to_timeline_media.page_info.end_cursor
		true
	}

	/**
	 * Generates the query parameters from the parameters of the first index page.
	 * @param input the given parameters
	 * @return the query parameters
	 */
	private static HashMap<String, String> generateQueryParams(Map<String, String> input)
	{
		// build the json structure (the second parameter called 'variables')
		def builder = new JsonBuilder()
		def json = builder {
			id input.id
			first input.first.toInteger()
			after input.after
		}
		def text = builder.toString()

		// fill the structure
		def result = [:]
		result.query_hash = input.query_id
		result.variables = text
		result
	}

	/**
	 * The calls to the subsequent index pages are secured with an MD5 checksum header.
	 * This function calculates this checksum.
	 * @param rhxgis    rhx_gis parameter
	 * @param text      the json structure (query parameter variables
	 * @return the MD5 checksum
	 */
	private static String buildChecksum(String rhxgis, String text)
	{
		MessageDigest
			.getInstance('MD5')
			.digest("$rhxgis:$text".bytes)
			.encodeHex()
			.toString()
	}

	/**
	 * Build an uri with a given base uri and a map of query parameters.
	 * @param url   base uri
	 * @param query query parameter
	 * @return full uri
	 */
	private static URI buildUri(String url, Map<String,String> query)
	{
		def uriBuilder = new URIBuilder(url)
		query.each { key, value ->
			uriBuilder.addParameter(key, value)
		}
		uriBuilder.build()
	}
}
