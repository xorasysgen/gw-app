package com.solum.gwapp.init;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Order(2)
@Slf4j
public class BootLoaderOrderTwo implements CommandLineRunner {

    private Environment environment;

    @Autowired
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("Active profiles:{} " ,  Arrays.toString(environment.getActiveProfiles()));
    }
}
