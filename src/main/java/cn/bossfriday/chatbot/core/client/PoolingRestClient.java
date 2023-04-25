package cn.bossfriday.chatbot.core.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

/**
 * PoolingRestTemplate
 *
 * @author chenx
 */
@Slf4j
public abstract class PoolingRestClient<T> {

    private static final int CONNECTION_TIME_TO_LIVE = 300 * 1000;
    private static final int CONNECT_TIMEOUT = 30 * 1000;
    private static final int READ_TIMEOUT = 60 * 1000;
    private static final int NO_CONNECTION_WAITING = CONNECT_TIMEOUT + READ_TIMEOUT + 1000;

    protected String name;
    protected RestTemplate restTemplate;
    protected PoolingHttpClientConnectionManager connectionManager;

    protected PoolingRestClient(String name, int restClientMaxTotal, int restClientMaxPerRoute) {
        this.name = name;

        // init connectionManager
        this.connectionManager = new PoolingHttpClientConnectionManager();
        this.connectionManager.setMaxTotal(restClientMaxTotal);
        this.connectionManager.setDefaultMaxPerRoute(restClientMaxPerRoute);

        // init clientHttpRequestFactory
        HttpComponentsClientHttpRequestFactory clientHttpRequestFactory = new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create()
                .setConnectionManager(this.connectionManager)
                .setConnectionTimeToLive(CONNECTION_TIME_TO_LIVE, TimeUnit.MILLISECONDS)
                .build());
        clientHttpRequestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        clientHttpRequestFactory.setReadTimeout(READ_TIMEOUT);
        clientHttpRequestFactory.setConnectionRequestTimeout(NO_CONNECTION_WAITING);

        this.restTemplate = new RestTemplate();
        this.restTemplate.setRequestFactory(clientHttpRequestFactory);
    }

    /**
     * send
     *
     * @param message
     */
    public abstract void send(T message);

    /**
     * shutdown
     */
    public void shutdown() {
        this.connectionManager.shutdown();
        log.info(this.name + " shutdown done.");
    }
}
