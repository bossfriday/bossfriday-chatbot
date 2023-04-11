package cn.bossfriday.chatbot;

import cn.bossfriday.chatbot.core.MessageRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * ApplicationListener
 *
 * @author chenx
 */
@Slf4j
@Component
public class ApplicationListener implements ApplicationRunner, DisposableBean {

    @Autowired
    private MessageRouter router;

    @Override
    public void run(ApplicationArguments args) {
        this.router.start();
        log.info("==================================");
        log.info("********Service Start Done********");
        log.info("==================================");
    }

    @Override
    public void destroy() {
        this.router.stop();
        log.info("=================================");
        log.info("********Service Stop Done********");
        log.info("=================================");
    }
}
