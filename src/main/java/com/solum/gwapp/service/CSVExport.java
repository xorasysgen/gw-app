package com.solum.gwapp.service;

import com.solum.gwapp.APIController;
import com.solum.gwapp.annotation.ExecutionTimeTracker;
import com.solum.gwapp.dto.GatewayStatusReport;
import com.solum.gwapp.dto.GatewayStatusReportAutoSaved;
import com.solum.gwapp.repository.GatewayStatusReportAutoSavedRepository;
import com.solum.gwapp.repository.GatewayStatusReportRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CSVExport {

   GatewayStatusReportRepository gatewayStatusReportRepository;

   @Value("${json.body.period}")
   private String period;

   @Value("${json.body.count}")
   private String count;

   GatewayStatusReportAutoSavedRepository gatewayStatusReportAutoSavedRepository;

   @Autowired
   public void setGatewayStatusReportRepository(GatewayStatusReportRepository gatewayStatusReportRepository) {
      this.gatewayStatusReportRepository = gatewayStatusReportRepository;
   }

   @Autowired
   public void setGatewayStatusReportAutoSavedRepository(GatewayStatusReportAutoSavedRepository gatewayStatusReportAutoSavedRepository) {
      this.gatewayStatusReportAutoSavedRepository = gatewayStatusReportAutoSavedRepository;
   }

   @ExecutionTimeTracker
   public void generateAutoSavedResponse(HttpServletResponse response) {

      String filename = "gw_connect_status_auto_saved".concat(new Date().toString()).concat(".csv");
      List<GatewayStatusReportAutoSaved> gw= gatewayStatusReportAutoSavedRepository.findAll();
      CSVPrinter csvPrinter=null;
      try {
         response.setContentType("text/csv");
         response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                 "attachment; filename=\"" + filename + "\"");
         csvPrinter = new CSVPrinter(response.getWriter(),
                 CSVFormat.DEFAULT.withHeader("GWProcID", "Gateway IP", "Period", "Count","Status","Gateway Response"));
         if(gw!=null && gw.size()>0){
            for (GatewayStatusReportAutoSaved gatewayStatusReport : gw) {
               csvPrinter.printRecord(Arrays.asList(gatewayStatusReport.getId(),gatewayStatusReport.getGwIP(), gatewayStatusReport.getPollPeriod(),gatewayStatusReport.getPollCount(),gatewayStatusReport.getStatus(),gatewayStatusReport.getResponseJson()));
            }
         }
         else{
            csvPrinter.printRecord(Arrays.asList("NA", "NA", "NA","NA", "NA", "NA","NA"));
         }


      } catch (IOException e) {
         log.error("CSV Export Failed {}",e.getMessage());
      } finally {
         if(csvPrinter != null) {
            try {
               csvPrinter.close();
            } catch (IOException e) {
               log.error("CSV printer Failed to close : {}",e.getMessage());
            }
         }
      }
   }
   @ExecutionTimeTracker
   public void generateCsvResponse(HttpServletResponse response) {

      String filename = "gw_connect_job_status_".concat(new Date().toString()).concat(".csv");
      List<GatewayStatusReport> gw= gatewayStatusReportRepository.findAll();
      CSVPrinter csvPrinter=null;
      try {
         response.setContentType("text/csv");
         response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                 "attachment; filename=\"" + filename + "\"");
         csvPrinter = new CSVPrinter(response.getWriter(),
                 CSVFormat.DEFAULT.withHeader("GWProcID", "Gateway IP", "Period", "Count","Status","Gateway Response"));
         if(gw!=null && gw.size()>0){
            for (GatewayStatusReport gatewayStatusReport : gw) {
               csvPrinter.printRecord(Arrays.asList(gatewayStatusReport.getId(),gatewayStatusReport.getGwIP(), gatewayStatusReport.getPollPeriod(),gatewayStatusReport.getPollCount(),gatewayStatusReport.getStatus(),gatewayStatusReport.getResponseJson()));
            }
         }
         else{
            csvPrinter.printRecord(Arrays.asList("NA", "NA", "NA","NA", "NA", "NA","NA"));
         }


      } catch (IOException e) {
         log.error("CSV Export Failed {}",e.getMessage());
      } finally {
         if(csvPrinter != null) {
            try {
               csvPrinter.close();
            } catch (IOException e) {
               log.error("CSV printer Failed to close : {}",e.getMessage());
            }
         }
      }
   }


   public void generateCsvResponse(HttpServletResponse response, List<APIController.Fullgwlist> listFullgwlist,String statusParam) {

      String filename = "gw_connect.csv";
      List<APIController.Fullgwlist> gwList= listFullgwlist;
      List<APIController.Fullgwlist> gw= null;
      if(statusParam.equalsIgnoreCase("CONNECTED")) {
         gw=gwList.stream().filter(data -> data.getStatus().equalsIgnoreCase("CONNECTED")).collect(Collectors.toList());
      }
      else if(statusParam.equalsIgnoreCase("DISCONNECTED")) {
         gw=gwList.stream().filter(data -> data.getStatus().equalsIgnoreCase("DISCONNECTED")).collect(Collectors.toList());
      }
      else if(statusParam.equalsIgnoreCase("NOT_READY")) {
         gw=gwList.stream().filter(data -> data.getStatus().equalsIgnoreCase("NOT_READY")).collect(Collectors.toList());
      }
      else if(statusParam.equalsIgnoreCase("UNREGISTERED")) {
         gw=gwList.stream().filter(data -> data.getStatus().equalsIgnoreCase("UNREGISTERED")).collect(Collectors.toList());
      }
      else if(statusParam.equalsIgnoreCase("ALL")) {
         gw=gwList;
      }
      CSVPrinter csvPrinter=null;
      try {
         response.setContentType("text/csv");
         response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                 "attachment; filename=\"" + filename + "\"");
         csvPrinter = new CSVPrinter(response.getWriter(),
                 CSVFormat.DEFAULT.withHeader("gwIP","period","count","status","storeCode", "storeName", "GwName","lastConnectionTime","macAddress","fwVersion"));
         if(gw!=null && gw.size()>0){
            for (APIController.Fullgwlist object : gw) {
               csvPrinter.printRecord(Arrays.asList(
                       object.getIpAddress(),
                       period,
                       count,
                       object.getStatus(),
                       object.getStoreCode(),
                       object.getStoreName(),
                       object.getName(),
                       object.getLastConnectionTime(),
                       object.getMacAddress(),
                       object.getFwVersion()
               ));
            }
            log.info("File Download Completed");
         }



      } catch (IOException e) {
         log.error("CSV Export Failed {}",e.getMessage());
      } finally {
         if(csvPrinter != null) {
            try {
               csvPrinter.close();
            } catch (IOException e) {
               log.error("CSV printer Failed to close : {}",e.getMessage());
            }
         }
      }
   }
}

