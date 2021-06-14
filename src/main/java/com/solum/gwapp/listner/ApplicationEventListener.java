package com.solum.gwapp.listner;


import io.micrometer.core.instrument.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Locale;

@Slf4j
@Configuration
public class ApplicationEventListener {


	private Environment environment;

	@Autowired
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}


    @EventListener(classes=ApplicationReadyEvent.class)
	public void onApplicationEvent(ApplicationReadyEvent event) throws Exception {
		log.info("-------------- Auth Server Information --------------");
		String protocol = "http";
		if (environment.getProperty("server.ssl.key-store") != null) {
			protocol = "https";
		}
		String serverPort = environment.getProperty("server.port");
		String contextPath = environment.getProperty("server.servlet.context-path");
		if (StringUtils.isBlank(contextPath)) {
			contextPath = "/";
		}
		String hostAddress = "localhost";
		try (Socket socket = new Socket();) {
			socket.connect(new InetSocketAddress("google.com", 80));
			InetAddress localAddress = socket.getLocalAddress();
			hostAddress = localAddress.toString().replace("/", "");
		} catch (Exception e) {
			log.warn("The host name could not be determined, using `localhost` as fallback");
		}
		log.info(
				"\n----------------------------------------------------------\n\t"
						+ "Application '{}' is running! Access URLs:\n\t" + "Local: \t\t{}://localhost:{}{}\n\t"
						+ "External: \t{}://{}:{}{}\n\t"
						+ "Profile(s): \t{}\n----------------------------------------------------------",
				environment.getProperty("spring.application.name"), protocol, serverPort, contextPath, protocol, hostAddress,
				serverPort, contextPath, environment.getActiveProfiles());

	}


}
