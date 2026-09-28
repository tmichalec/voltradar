package sk.brutech.voltradar.ingestion.zse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
public class ZseDriveConfiguration {
    @Bean
    ZseDriveClient zseDriveClient(
            @Value("${integrations.zse-drive.base-url:https://zsedrive.sk}") URI baseUrl,
            @Value("${integrations.zse-drive.connect-timeout:5s}") Duration connectTimeout,
            @Value("${integrations.zse-drive.read-timeout:15s}") Duration readTimeout) {
        if (readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalArgumentException("ZSE read timeout must be positive");
        }
        var httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return new ZseDriveClient(RestClient.builder().baseUrl(baseUrl.toString())
                .requestFactory(requestFactory).build());
    }
}
