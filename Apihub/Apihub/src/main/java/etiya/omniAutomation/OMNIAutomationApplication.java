package etiya.omniAutomation;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.SSLException;
import java.io.File;
import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
public class OMNIAutomationApplication extends SpringBootServletInitializer {
	protected static final Logger logger = LoggerFactory.getLogger(OMNIAutomationApplication.class);

	// Removed CONFIG_HOME logic - always use embedded application.yml from JAR

	public static void main(String[] args) {
		SpringApplication.run(OMNIAutomationApplication.class, args);
	}

	@Bean("webClient")
	public WebClient webClient() throws SSLException {

		// Tüm SSL sertifikalarına güven
		SslContext sslContext = SslContextBuilder.forClient()
				.trustManager(InsecureTrustManagerFactory.INSTANCE)
				.build();// Sertifika doğrulamasını bypass eder

		// Özel bağlantı havuzu (aynı RestTemplate'deki gibi)
		ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
				.maxConnections(100)
				.maxIdleTime(Duration.ofMinutes(5))
				.build();

		// Reactor Netty HTTP client oluştur
		HttpClient httpClient = HttpClient.create(connectionProvider)
				.secure(ssl -> ssl.sslContext(sslContext))
				.responseTimeout(Duration.ofMinutes(30))  // Okuma süresi (readTimeout)
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30_000); // 30 saniye bağlantı süresi

		ExchangeStrategies strategies = ExchangeStrategies.builder()
				.codecs(config -> config.defaultCodecs().maxInMemorySize(10 * 1024 * 1024)) // 10 MB
				.build();
		// WebClient oluştur
		return WebClient.builder()
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.exchangeStrategies(strategies)
				.build();
	}

	@Bean("virtualThreadExecutor")
	public ExecutorService executorService() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}

}
