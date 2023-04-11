package cn.bossfriday.chatbot.core.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * PoolingRestTemplate
 *
 * @author chenx
 */
@Slf4j
public abstract class PoolingRestClient<T> {

    protected String name;
    protected RestTemplate restTemplate;
    protected PoolingHttpClientConnectionManager connectionManager;

    protected PoolingRestClient(String name, int restClientMaxTotal, int restClientMaxPerRoute) {
        this.name = name;
        this.connectionManager = new PoolingHttpClientConnectionManager();
        this.connectionManager.setMaxTotal(restClientMaxTotal);
        this.connectionManager.setDefaultMaxPerRoute(restClientMaxPerRoute);
        this.restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory(HttpClientBuilder.create()
                .setConnectionManager(this.connectionManager)
                .build()));
        log.info("PoolingRestClient build done, name=" + this.name + ", maxTotal=" + this.connectionManager.getMaxTotal() + ", maxPerRoute=" + this.connectionManager.getDefaultMaxPerRoute());
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
