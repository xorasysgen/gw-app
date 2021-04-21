package com.solum.gwapp.processor;

import com.google.gson.Gson;
import com.solum.gwapp.APIController;
import com.solum.gwapp.dto.GatewayStatus;
import com.solum.gwapp.dto.GatewayStatusReport;
import com.solum.gwapp.dto.GwDTO;
import com.solum.gwapp.dto.GwResponse;
import com.solum.gwapp.repository.GatewayStatusReportRepository;
import com.solum.gwapp.repository.GwRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.Optional;


@Component
@Slf4j
public class ItemProcessorImpl  implements ItemProcessor<GwDTO, GwDTO> {

	@Autowired
	private APIController apiController;

	@Value("${gw.target.protocol}")
	private String gwTargetProtocol;

	@Autowired
	GatewayStatusReportRepository gwstatusRepository;

	@Override
	public GwDTO process(GwDTO item) throws Exception {
		GatewayStatusReport gatewayStatusReport=new GatewayStatusReport();
		GwDTO gwDTO=item;
		log.info("Processing csv gw request...");
		String target = apiController.prepareGWURL(gwDTO.getGwIP());
		log.info("Request Gateway IP : {}, Gateway Period :{}, Gateway Count :{}", gwDTO.getGwIP(), gwDTO.getPeriod(),gwDTO.getCount());
		Map<String,String> response=apiController.executingGateway(gwDTO.getGwIP(),target);
		String successJSON=response.get("SUCCESS");
		String error=response.get("ERROR");
		if(Optional.ofNullable(successJSON).isPresent()) {
			GwResponse gwResponse = new Gson().fromJson(successJSON, GwResponse.class);
			String pollingStatus=gwResponse.isResult()?"Success":"Failed";
			String gwConfigTarget=apiController.prepareGWURLConfig(gwDTO.getGwIP());
			GatewayStatus gatewayStatus=fetchGwPollstatus(gwConfigTarget);
			if(Optional.ofNullable(gatewayStatus).isPresent()) {
				gatewayStatusReport.setGwIP(gwDTO.getGwIP());
				gatewayStatusReport.setPollCount(gatewayStatus.getPollCount());
				gatewayStatusReport.setPollPeriod(gatewayStatus.getPollPeriod());
				gatewayStatusReport.setResponseJson(successJSON);
				gatewayStatusReport.setStatus(pollingStatus);
			}
		}
		else
		{
				String pollingStatus="Failed";
				gatewayStatusReport.setGwIP(gwDTO.getGwIP());
				gatewayStatusReport.setPollCount(-1);
				gatewayStatusReport.setPollPeriod(-1);
				gatewayStatusReport.setResponseJson(error);
				gatewayStatusReport.setStatus(pollingStatus);
		}
		gwstatusRepository.save(gatewayStatusReport);
		return  gwDTO;
	}

	private final GatewayStatus fetchGwPollstatus(String gwConfigTarget){
		{
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			log.info("Gateway config Target {}", gwConfigTarget);
			MultiValueMap<String, String> map = new LinkedMultiValueMap<String, String>();
			HttpEntity<String> request = new HttpEntity<String>(headers);
			try {
				ResponseEntity<GatewayStatus> response = new RestTemplate().getForEntity(gwConfigTarget, GatewayStatus.class);
				log.info("Gateway config response  {} ", response.getBody());
				log.info("PollPeriod  : {}, PollCount  :{}", response.getBody().getPollPeriod(), response.getBody().getPollCount());
				return response.getBody();

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
	
}
