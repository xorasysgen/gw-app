package com.solum.gwapp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@RestController("/")
public class APIController {
	
	
	@Value("${json.body.key}")
	private String key;
	
	@Value("${json.body.period}")
	private String period;
	
	@Value("${json.body.count}")
	private String count;
	

	@Value("${cs.target.final}")
	private String csTargetFinal;
	@Value("${execution.limit}")
	private String executionLimit;


	@Value("${gw.target.protocol}")
	private String gwTargetProtocol;

	@Value("${gw.target.uri}")
	private String gwTargetURI;

	@Value("${gw.target.port}")
	private String gwTargetPort;


	private final String prepareGWURL(String host) {
		return new StringBuilder()
				.append(gwTargetProtocol)
				.append("://")
				.append(host)
				.append(":")
				.append(gwTargetPort)
				.append("/")
				.append(gwTargetURI)
				.toString();
	}


	@GetMapping("/welcome")
	public ResponseEntity<HashMap<String, String>> welcomeMapping() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date now = new Date();
		String strDate = sdfDate.format(now);
		HashMap<String, String> start = new HashMap<>();
		start.put("msg", "core-gw is running");
		start.put("version", "1.0.0");
		start.put("timestamp", strDate);
		log.info("revision no: " + start);
		return ResponseEntity.ok().body(start);
	}

	@GetMapping("/execute")
	public void serviceCallToCommonService() {
		Integer i=1;
		while(true) {
			if (i > Integer.parseInt(executionLimit))
				break;
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);

			log.info("Common Service Target {}", csTargetFinal);
			MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();

			HttpEntity<String> request = new HttpEntity<String>(headers);

			try {
				ResponseEntity<Root> response = new RestTemplate().getForEntity(csTargetFinal, Root.class);
				log.info("Common Service response {} ", response.getBody());
				Integer gwSize = response.getBody().fullgwlist.size();
				log.info("No of gateway found : {}", gwSize);
				List<Fullgwlist> listFullgwlist = response.getBody().getFullgwlist();
				if (gwSize > 0) {
					for (Fullgwlist gateway : listFullgwlist) {
						String target = prepareGWURL(gateway.getIpAddress());
						log.info("Gateway IP : {}, Gateway Status :{}", gateway.getIpAddress(), gateway.getStatus());
						executingLoopCallToGateway(target);
					}
				}

			} catch (HttpStatusCodeException e) {
				log.error("Common Service failed HttpStatusCodeException reason :{}", e.getMessage().concat(e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isEmpty() ? " | " + e.getResponseBodyAsString() : ""));
			} catch (RestClientException e) {
				log.error("Common Service failed RestClientException reason :{}", e.getMessage());
			} catch (Exception e) {
				log.error("Common Service failed general reason :{}", e.getMessage());
			}
		}

	}

	private final void executingLoopCallToGateway(String gwPreparedTarget) {
		Req req=new Req();
		req.setKey(key);
		req.setPeriod(Integer.parseInt(period));
		req.setCount(Integer.parseInt(count));

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		log.info("Gateway Target : {}",gwPreparedTarget);
		log.info("Gateway post request body : {}",req.toString());
		HttpEntity<Req> request = new HttpEntity<>(req, headers);
		try {
			ResponseEntity<String> response = new RestTemplate().postForEntity(gwPreparedTarget,request, String.class);
			log.info("Gateway response : {} ", response.getBody());
		} catch (HttpStatusCodeException e) {
			log.error("Gateway Service failed HttpStatusCodeException reason :{}", e.getMessage().concat(e.getResponseBodyAsString()!=null && !e.getResponseBodyAsString().isEmpty()? " | " + e.getResponseBodyAsString():""));
		} catch (RestClientException e) {
			log.error("Gateway Service failed RestClientException reason :{}", e.getMessage());
		} catch (Exception e) {
			log.error("Gateway Service failed general reason :{}", e.getMessage());
		}

	}

	@Data
	public static class Fullgwlist{
		public String storeCode;
		public String storeName;
		public String ipAddress;
		public String macAddress;
		public String name;
		public String fwVersion;
		public String status;
		public String lastConnectionTime;
	}

	@Data
	public static class Root{
		public List<Fullgwlist> fullgwlist;
	}

	@Data
	public static class Req{
		public String key;
		public int period;
		public int count;
	}


}
