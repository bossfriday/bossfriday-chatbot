package cn.bossfriday.chatbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * ApplicationRun
 *
 * @author chenx
 */
@EnableSwagger2
@EnableDiscoveryClient
@SpringBootApplication(scanBasePackages = {"cn.bossfriday"})
public class ApplicationRun {

    /**
     * Service startup main function
     *
     * @param args
     */
    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        SpringApplication.run(ApplicationRun.class, args);
    }
}
