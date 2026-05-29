package ru.isu.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import ru.isu.backend.service.DemoDataService;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.demo-data.rebuild", havingValue = "true")
public class DemoDataRunner implements ApplicationRunner {

    private final DemoDataService demoDataService;
    private final ConfigurableApplicationContext applicationContext;

    @Value("${app.demo-data.exit:false}")
    private boolean exitAfterRebuild;

    @Override
    public void run(ApplicationArguments args) {
        demoDataService.rebuild();
        log.info("FlashLex demo data has been rebuilt. Demo password for all users: demo12345");
        if (exitAfterRebuild) {
            int exitCode = SpringApplication.exit(applicationContext, () -> 0);
            System.exit(exitCode);
        }
    }
}
