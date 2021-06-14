package com.solum.gwapp.init;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Order(1)
@Slf4j
public class BootLoaderOrderOne implements CommandLineRunner {

    private ApplicationContext appContext;

    @Autowired
    public void setAppContext(ApplicationContext appContext) {
        this.appContext = appContext;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("----------List of beans found----------");
        String[] beans = appContext.getBeanDefinitionNames();
        log.info("Found Beans : {}", beans.length);
        log.debug(Arrays.deepToString(beans));
    }
}
