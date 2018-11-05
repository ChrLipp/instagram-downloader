package at.corba.tools.instagram_downloader

import org.apache.http.client.fluent.Executor
import org.apache.http.impl.client.HttpClients
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class InstagramDownloaderConfiguration
{
	@Bean
	Executor executor()
	{
		def client = HttpClients.custom()
				.setUserAgent('Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:47.0) Gecko/20100101 Firefox/47.0')
				.build()
		Executor.newInstance(client)
	}
}
