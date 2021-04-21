package com.solum.gwapp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import com.google.gson.Gson;
import com.solum.gwapp.repository.GatewayStatusReportRepository;
import com.solum.gwapp.service.CSVExport;
import com.solum.gwapp.repository.GwRepository;
import lombok.Data;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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

import javax.servlet.http.HttpServletResponse;

@Slf4j
@RestController("/")
public class APIController {

	@Autowired
	JobLauncher jobLauncher;

	@Autowired
	Job gwJob;

	@Autowired
	GwRepository repository;

	@Autowired
	GatewayStatusReportRepository gwstatusRepository;

	@Autowired
	CSVExport csvExport;

	@Value("${json.body.key.username}")
	private String username;
	
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

	@Value("${gw.target.uri.config}")
	private String gwTargetURIConfig;

	@Value("${json.body.filename}")
	private String filename;

	@Value("${json.body.extension}")
	private String fileExtension;

	private Map<String,String> responseMap=new LinkedHashMap<>();

	public final String prepareGWURL(String host) {
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

	public final String prepareGWURLConfig(String host) {
		return new StringBuilder()
				.append(gwTargetProtocol)
				.append("://")
				.append(host)
				.append(":")
				.append(gwTargetPort)
				.append("/")
				.append(gwTargetURIConfig)
				.toString();
	}




	@GetMapping("/welcome")
	public ResponseEntity<HashMap<String, String>> welcomeMapping() {
		SimpleDateFormat sdfDate = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date now = new Date();
		String strDate = sdfDate.format(now);
		HashMap<String, String> start = new HashMap<>();
		start.put("msg", "api-v2-gw is running");
		start.put("version", "1.0.0");
		start.put("timestamp", strDate);
		log.info("revision no: " + start);
		return ResponseEntity.ok().body(start);
	}

	@GetMapping("/csv/gw")
	public void getProcessedCSV(HttpServletResponse httpServletResponse) {
		csvExport.generateCsvResponse(httpServletResponse);
	}

	@GetMapping("/csv/gw/list")
	public void downloadGatewayList(HttpServletResponse httpServletResponse){

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
					csvExport.generateCsvResponse(httpServletResponse,listFullgwlist);
				}

			} catch (HttpStatusCodeException e) {
				log.error("Common Service failed HttpStatusCodeException reason :{}", e.getMessage().concat(e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isEmpty() ? " | " + e.getResponseBodyAsString() : ""));
			} catch (RestClientException e) {
				log.error("Common Service failed RestClientException reason :{}", e.getMessage());
			} catch (Exception e) {
				log.error("Common Service failed general reason :{}", e.getMessage());
			}
		}


	@GetMapping("/csv/gw/run")
	public String runBatch() {
		gwstatusRepository.deleteAll();
		HashMap<String, JobParameter> map=new HashMap<>();
		map.put("Time", new JobParameter(System.currentTimeMillis()));
		JobParameters jobParameters=new JobParameters(map);
		try {
			JobExecution je=jobLauncher.run(gwJob, jobParameters);
			log.info("Job Status:" + je.getStatus());
			while(je.isRunning()) {
				log.info("Job Running");
			}
			String status=je.getStatus().toString();
			return "".concat("<< JOB ").concat(status).concat(" >> Downloadable file is ready, GO GET /csv/gw");
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException
				| JobParametersInvalidException e) {
			log.error("Job execution failed{}", e.getMessage());
		}
		return null;


	}



	@GetMapping("/execute")
	public void serviceCallToCommonService() {
		Integer i=1;
		while(true) {
			if (i++ > Integer.parseInt(executionLimit))
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
						executingGateway(gateway.getIpAddress(),target);
						//String target = prepareGWURL("202.122.21.74");
						//executingLoopCallToGateway("202.122.21.74",target);
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

	public final Map<String,String> executingGateway(String Ip,String gwPreparedTarget)  {
		String key=Utils.getMD5Hash(Ip.concat(username));
		Req req=new Req();
		req.setKey(key);
		req.setPeriod(Integer.parseInt(period));
		req.setCount(Integer.parseInt(count));

		MultiValueMap<String, Object> parts =new LinkedMultiValueMap<String, Object>();
		parts.add("data", createAndUploadFile(req));
		parts.add("filename", "data_ClientPollPram.json");
		log.info("File uploaded location {}",parts.toString());

		HttpHeaders headers = new HttpHeaders();
		//headers.setContentType(MediaType.TEXT_PLAIN);
		log.info("Gateway Target : {}",gwPreparedTarget);
		log.info("Gateway post request body : {}",req.toString());

		HttpEntity<MultiValueMap<String, Object>> requestEntity =	new HttpEntity<MultiValueMap<String, Object>>(parts, headers);

		try {
			ResponseEntity<String> response = new RestTemplate().postForEntity(gwPreparedTarget,requestEntity, String.class);
			log.info("Gateway response : {} ", response.getBody());
			responseMap.put("SUCCESS",response.getBody().toString());
			return responseMap;
		} catch (HttpStatusCodeException e) {
			String msg=e.getMessage().concat(e.getResponseBodyAsString()!=null && !e.getResponseBodyAsString().isEmpty()? " | " + e.getResponseBodyAsString():"");
			log.error("Gateway Service failed HttpStatusCodeException reason :{}", msg);
			responseMap.put("ERROR",msg);
			return responseMap;
		} catch (RestClientException e) {
			log.error("Gateway Service failed RestClientException reason :{}", e.getMessage());
			responseMap.put("ERROR",e.getMessage());
		} catch (Exception e) {
			log.error("Gateway Service failed general reason :{}", e.getMessage());
			responseMap.put("ERROR",e.getMessage());
		}

		return responseMap;
	}

	private final Resource createAndUploadFile(Req req)  {
		Path file = null;
		try {
			file = Files.createTempFile(filename, fileExtension);
		log.info("Uploading data File to : {} " , file);
		Files.write(file, new Gson().toJson(req).getBytes());
		} catch (IOException e) {
			log.error("Unable to create temp file - I/O error: {}", e);
		}
		return new FileSystemResource(file.toFile());
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