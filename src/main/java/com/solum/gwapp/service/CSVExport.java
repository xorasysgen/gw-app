package com.solum.gwapp.service;

import com.solum.gwapp.APIController;
import com.solum.gwapp.dto.GwDTO;
import com.solum.gwapp.repository.GwRepository;
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
import java.util.List;

@Service
@Slf4j
public class CSVExport {

   @Autowired
   GwRepository gwRepository;

   @Value("${json.body.period}")
   private String period;

   @Value("${json.body.count}")
   private String count;

   public void generateCsvResponse(HttpServletResponse response) {

      String filename = "gw_connect_job_status.csv";
      List<GwDTO> gw= gwRepository.findAll();
      CSVPrinter csvPrinter=null;
      try {
         response.setContentType("text/csv");
         response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                 "attachment; filename=\"" + filename + "\"");
         csvPrinter = new CSVPrinter(response.getWriter(),
                 CSVFormat.DEFAULT.withHeader("gwIP", "period", "count"));
         if(gw!=null && gw.size()>0){
            for (GwDTO gwDTO : gw) {
               csvPrinter.printRecord(Arrays.asList(gwDTO.getGwIP(), gwDTO.getPeriod(),gwDTO.getCount()));
            }
         }
         else{
            csvPrinter.printRecord(Arrays.asList("NA", "NA", "NA"));
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


   public void generateCsvResponse(HttpServletResponse response, List<APIController.Fullgwlist> listFullgwlist) {

      String filename = "gw_connect.csv";
      List<APIController.Fullgwlist> gw= listFullgwlist;
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

