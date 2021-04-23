package com.solum.gwapp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.*;

import com.google.gson.Gson;
import com.solum.gwapp.dto.*;
import com.solum.gwapp.repository.GatewayStatusReportAutoSavedRepository;
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
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
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
	GatewayStatusReportRepository gwStatusRepository;

	@Autowired
	GatewayStatusReportAutoSavedRepository gatewayStatusReportAutoSavedRepository;

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

	@Value("${filename}")
	Resource resource;

	RestTemplate restTemplate;

	@Autowired
	public void setRestTemplate(RestTemplate restTemplate) {
		this.restTemplate = restTemplate;
	}

	private final Map<String,String> apiResponseMap=new LinkedHashMap<>();
	private final Map<String,String>  responseMap=new LinkedHashMap<>();

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

	@GetMapping("/csv/gw/export")
	public void getProcessedCSV(HttpServletResponse httpServletResponse) {
		csvExport.generateCsvResponse(httpServletResponse);
	}

	@GetMapping("/csv/gw/download")
	public void downloadGatewayList(HttpServletResponse httpServletResponse){
			log.info("Common Service Target {}", csTargetFinal);
			try {
				ResponseEntity<Root> response = new RestTemplate().getForEntity(csTargetFinal, Root.class);
				log.info("Common Service response {} ", response.getBody());
				Integer gwSize = response.getBody().fullgwlist!=null?response.getBody().fullgwlist.size():-1;
				log.info("No of gateway found : {}", gwSize);
				List<Fullgwlist> fullGwLSize = response.getBody().getFullgwlist();
				if(Optional.ofNullable(fullGwLSize).isPresent()) {
					long disconnected = fullGwLSize.stream().filter(gws -> gws.getStatus().equalsIgnoreCase("DISCONNECTED")).count();
					log.info("No of gateway, Disconnected : {}", disconnected);
					log.info("No of gateway, Connected : {}", gwSize - disconnected);
				}
				else{
					log.warn("No gateway found!");
				}
				if (gwSize > 0) {
					csvExport.generateCsvResponse(httpServletResponse,fullGwLSize);
				}

			} catch (HttpStatusCodeException e) {
				log.error("Common Service failed HttpStatusCodeException reason :{}", e.getMessage().concat(e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isEmpty() ? " | " + e.getResponseBodyAsString() : ""));
			} catch (RestClientException e) {
				log.error("Common Service failed RestClientException reason :{}", e.getMessage());
			} catch (Exception e) {
				log.error("Common Service failed general reason :{}", e.getMessage());
			}
		log.info("File Download Completed");
		}


	@GetMapping("/csv/gw/run")
	public String runBatch() {
		gwStatusRepository.deleteAll();
		boolean fileExist=resource.exists();
		if(!fileExist) {
			try {
				return  ""
						.concat("prerequisite check failed").concat("<br>Possible Reason : CSV file does not exist in location :"
						.concat(resource.getURL().getPath().toString())
						.concat("<br>Download prepared gateway list [GET /csv/gw/download], file name would be gw_connect.csv, paste [gw_connect.csv] file inside [C:\\env\\csv_data] folder and re-run above service. it can also be filter out by using excel"));
			} catch (IOException e) {
				log.error("Job execution failed{}", e.getMessage());
			}
		}
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

			return "".concat("JOB ").concat(status).concat(status.equals("COMPLETED")?"<br>Downloadable file is ready, GO GET /csv/gw/export":"<br> Possible Reason : CSV file may have Extra Blank line, may have Invalid Character like . or ' etc");
		} catch (JobExecutionAlreadyRunningException | JobRestartException | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
			log.error("Job execution failed{}", e.getMessage());
		}
		return null;


	}

	@GetMapping("/export")
	public void getProcessedData(HttpServletResponse httpServletResponse) {
		csvExport.generateAutoSavedResponse(httpServletResponse);
	}

	@GetMapping(value  = {"/execute","/execute/{autoSaveDecision}"})
	public String serviceCallToCommonService(@PathVariable(required = false) String autoSaveDecision) {
		apiResponseMap.clear();
		gatewayStatusReportAutoSavedRepository.deleteAll();
		gatewayStatusReportAutoSavedRepository.flush();
		int i=1;
		while(true) {
			if (i++ > Integer.parseInt(executionLimit))
				break;
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			log.info("Common Service Target {}", csTargetFinal);
			MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
			HttpEntity<String> request = new HttpEntity<String>(headers);
			try {
				ResponseEntity<Root> response = restTemplate.getForEntity(csTargetFinal, Root.class);
				log.info("Common Service response {} ", response.getBody());
				Integer gwSize = response.getBody().fullgwlist!=null?response.getBody().fullgwlist.size():-1;
				log.info("No of gateway found : {}", gwSize);
				List<Fullgwlist> fullGwList = response!=null ? response.getBody().getFullgwlist(): new ArrayList<>();
				if(Optional.ofNullable(fullGwList).isPresent()) {
					long disconnected = fullGwList.stream().filter(gws -> gws.getStatus().equalsIgnoreCase("DISCONNECTED")).count();
					long connected=gwSize - disconnected;
					log.info("No of gateway, Disconnected : {}", disconnected);
					log.info("No of gateway, Connected : {}", connected);
					if(connected==0) {
						log.info("Execution prerequisite check failed, Process aborted No Active gateway found! ");
						return "Execution prerequisite check failed, Process aborted No Active gateway found! ";
					}
				}
				else{
					log.warn("No gateway found!");
				}
				if (gwSize > 0) {
					for (Fullgwlist gateway : fullGwList) {
						if(gateway.getStatus().equalsIgnoreCase("DISCONNECTED"))
							continue;
						GatewayStatusReportAutoSaved gatewayStatusReportAutoSaved=new GatewayStatusReportAutoSaved();
						String target = prepareGWURL(gateway.getIpAddress());
						log.info("Gateway IP : {}, Gateway Status :{}", gateway.getIpAddress(), gateway.getStatus());
						Map<String,String> apiResponseMap=executingGateway(gateway.getIpAddress(),target);
						//String target = prepareGWURL("202.122.21.74");
						//executingLoopCallToGateway("202.122.21.74",target);

						if(autoSaveDecision!=null && autoSaveDecision.equalsIgnoreCase("save"))
						AutoSaveGatewayStatusReport(gateway, gatewayStatusReportAutoSaved, apiResponseMap);
					}
					return "Execution Completed, Downloadable file is ready, GO GET /export";
				}

			} catch (HttpStatusCodeException e) {
				log.error("Common Service failed HttpStatusCodeException reason :{}", e.getMessage().concat(e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isEmpty() ? " | " + e.getResponseBodyAsString() : ""));
				return "Execution Failed, Reason:".concat( e.getMessage().concat(e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isEmpty() ? " | " + e.getResponseBodyAsString() : ""));
			} catch (RestClientException e) {
				log.error("Common Service failed RestClientException reason :{}", e.getMessage());
				return "Execution Failed, Reason:".concat(e.getMessage());
			} catch (Exception e) {
				log.error("Common Service failed general reason :{}", e.getMessage());
				return "Execution Failed, Reason:".concat(e.getMessage());
			}
		}
	return "Execution Failed Reason: Unknown";
	}

	private void AutoSaveGatewayStatusReport(Fullgwlist gateway, GatewayStatusReportAutoSaved gatewayStatusReportAutoSaved, Map<String, String> apiResponseMap) {
		String successJSON=apiResponseMap.get("SUCCESS");
		String error=apiResponseMap.get("ERROR");
		if(Optional.ofNullable(successJSON).isPresent()) {
			GwResponse gwResponse = new Gson().fromJson(successJSON, GwResponse.class);
			String pollingStatus=gwResponse.isResult()?"Success":"Failed";
			String gwConfigTarget=prepareGWURLConfig(gateway.getIpAddress());
			GatewayStatus gatewayStatus=fetchGwPollstatus(gwConfigTarget);
			if(Optional.ofNullable(gatewayStatus).isPresent()) {
				gatewayStatusReportAutoSaved.setGwIP(gateway.getIpAddress());
				gatewayStatusReportAutoSaved.setPollCount(gatewayStatus.getPollCount());
				gatewayStatusReportAutoSaved.setPollPeriod(gatewayStatus.getPollPeriod());
				gatewayStatusReportAutoSaved.setResponseJson(successJSON);
				gatewayStatusReportAutoSaved.setStatus(pollingStatus);
			}
		}
		else
		{
			String pollingStatus="Failed";
			gatewayStatusReportAutoSaved.setGwIP(gateway.getIpAddress());
			gatewayStatusReportAutoSaved.setPollCount(-1);
			gatewayStatusReportAutoSaved.setPollPeriod(-1);
			gatewayStatusReportAutoSaved.setResponseJson(error);
			gatewayStatusReportAutoSaved.setStatus(pollingStatus);
		}

		log.info("Auto save gatewayStatusReport into database");
		gatewayStatusReportAutoSavedRepository.saveAndFlush(gatewayStatusReportAutoSaved);
		gatewayStatusReportAutoSavedRepository.flush();
		gatewayStatusReportAutoSaved=null;
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
			ResponseEntity<String> response = restTemplate.postForEntity(gwPreparedTarget,requestEntity, String.class);
			log.info("Gateway response : {} ", response.getBody());
			apiResponseMap.put("SUCCESS",response.getBody().toString());
			return apiResponseMap;
		} catch (HttpStatusCodeException e) {
			String msg=e.getMessage().concat(e.getResponseBodyAsString()!=null && !e.getResponseBodyAsString().isEmpty()? " | " + e.getResponseBodyAsString():"");
			log.error("Gateway Service failed HttpStatusCodeException reason :{}", msg);
			apiResponseMap.put("ERROR",msg);
			return apiResponseMap;
		} catch (RestClientException e) {
			log.error("Gateway Service failed RestClientException reason :{}", e.getMessage());
			apiResponseMap.put("ERROR",e.getMessage());
		} catch (Exception e) {
			log.error("Gateway Service failed general reason :{}", e.getMessage());
			apiResponseMap.put("ERROR",e.getMessage());
		}

		return apiResponseMap;
	}


	public final Map<String,String> executingGateway(String Ip,String gwPreparedTarget,String csvPeriod,String csvCount)  {
		responseMap.clear();
		String key=Utils.getMD5Hash(Ip.concat(username));
		Req req=new Req();
		req.setKey(key);
		req.setPeriod(Integer.parseInt(csvPeriod));
		req.setCount(Integer.parseInt(csvCount));

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


	private final GatewayStatus fetchGwPollstatus(String gwConfigTarget){
		{
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.TEXT_PLAIN);
			log.info("Gateway config Target {}", gwConfigTarget);

			HttpEntity<String> request = new HttpEntity<String>(headers);
			try {
				ResponseEntity<String> response = new RestTemplate().getForEntity(gwConfigTarget, String.class);
				String jsonResponse=response.getBody().toString();
				GatewayStatus gatewayStatus=new Gson().fromJson(jsonResponse,GatewayStatus.class);
				log.info("Gateway config response  {} ", response.getBody());
				log.info("PollPeriod  : {}, PollCount  :{}", gatewayStatus.getPollPeriod(), gatewayStatus.getPollCount());
				return gatewayStatus;

			} catch (HttpStatusCodeException e) {
				log.error("Gateway config failed HttpStatusCodeException reason :{}", e.getMessage().concat(e.getResponseBodyAsString() != null && !e.getResponseBodyAsString().isEmpty() ? " | " + e.getResponseBodyAsString() : ""));
				return null;
			} catch (RestClientException e) {
				log.error("Gateway config RestClientException reason :{}", e.getMessage());
				return null;
			} catch (Exception e) {
				log.error("Gateway config general reason :{}", e.getMessage());
				return null;
			}
		}
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