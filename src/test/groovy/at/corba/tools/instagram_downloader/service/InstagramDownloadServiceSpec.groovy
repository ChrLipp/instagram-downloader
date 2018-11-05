package at.corba.tools.instagram_downloader.service

import spock.lang.Specification

class InstagramDownloadServiceSpec extends Specification
{
//	"id" -> "460563723"
//	"first" -> "12"
//	"after" -> "AQAHAYg1Yu0o7suVxz8BF2vdmxOYon_ebDpiENeJYVO-tfSlyEA4KHuzxZX5elUsxAnmoEFJgaMZjzx_9QM5LACxXhLK_jBdIYlBhoBuPCL2yA"
//	"csrf" -> "F1c110tD0HOz82RMDfZZU6CuM9n0Ejs1"
//	"rhx_gis" -> "6a43d87d4be1fe3bccadb55385b07ece"
//	https://www.instagram.com/graphql/query/?
// query_hash=42323d64886122307be10013ad2dcc44 &
// variables=%7B%22id%22%3A%22460563723%22%2C%22first%22%3A12%2C%22after%22%3A%22AQBLnWHOr8EMDnTpYxY4VqmIavCLtUA-0aBEo9X4seDYCMbtVnhzuViywbgbrjLaUhGM796dCmUN28rTTro8VY8stjYoF0-tgiA6F-MUtlbfXQ%22%7D
//	query_hash: 42323d64886122307be10013ad2dcc44
//	variables: {"id":"460563723","first":12,"after":"AQBLnWHOr8EMDnTpYxY4VqmIavCLtUA-0aBEo9X4seDYCMbtVnhzuViywbgbrjLaUhGM796dCmUN28rTTro8VY8stjYoF0-tgiA6F-MUtlbfXQ"}

	def 'test generateQueryParams'()
	{
		given:
		def params = [:]
		params['query_id'] = '42323d64886122307be10013ad2dcc44'
		params['id'] = '460563723'
		params['first'] = 12
		params['after'] = 'AQAHAYg1Yu0o7suVxz8BF2vdmxOYon_ebDpiENeJYVO-tfSlyEA4KHuzxZX5elUsxAnmoEFJgaMZjzx_9QM5LACxXhLK_jBdIYlBhoBuPCL2yA'
		params['csrf'] = 'F1c110tD0HOz82RMDfZZU6CuM9n0Ejs1'
		params['rhx_gis'] = '6a43d87d4be1fe3bccadb55385b07ece'

		when:
		def result = InstagramDownloadService.generateQueryParams(params)

		then:
		result.variables == '{"id":"460563723","first":12,"after":"AQAHAYg1Yu0o7suVxz8BF2vdmxOYon_ebDpiENeJYVO-tfSlyEA4KHuzxZX5elUsxAnmoEFJgaMZjzx_9QM5LACxXhLK_jBdIYlBhoBuPCL2yA"}'
		result.query_hash == '42323d64886122307be10013ad2dcc44'
	}

	def 'test buildChecksum'()
	{
		given:
		def params = [:]
		params['query_id'] = '42323d64886122307be10013ad2dcc44'
		params['id'] = '460563723'
		params['first'] = 12
		params['after'] = 'AQAHAYg1Yu0o7suVxz8BF2vdmxOYon_ebDpiENeJYVO-tfSlyEA4KHuzxZX5elUsxAnmoEFJgaMZjzx_9QM5LACxXhLK_jBdIYlBhoBuPCL2yA'
		params['csrf'] = 'F1c110tD0HOz82RMDfZZU6CuM9n0Ejs1'
		params['rhx_gis'] = '6a43d87d4be1fe3bccadb55385b07ece'

		when:
		def queryParams = InstagramDownloadService.generateQueryParams(params)
		def result = InstagramDownloadService.buildChecksum(params.rhx_gis, queryParams.variables)

		then:
		result == 'a25dd08f43bd64b30186fc965019edae'
	}
}
