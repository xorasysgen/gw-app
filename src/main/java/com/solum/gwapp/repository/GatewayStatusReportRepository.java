package com.solum.gwapp.repository;

import com.solum.gwapp.dto.GatewayStatusReport;
import com.solum.gwapp.dto.GwDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface GatewayStatusReportRepository extends JpaRepository<GatewayStatusReport, Integer>{

	
}
