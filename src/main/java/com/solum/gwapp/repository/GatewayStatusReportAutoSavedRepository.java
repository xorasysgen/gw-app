package com.solum.gwapp.repository;

import com.solum.gwapp.dto.GatewayStatusReport;
import com.solum.gwapp.dto.GatewayStatusReportAutoSaved;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
@Transactional
public interface GatewayStatusReportAutoSavedRepository extends JpaRepository<GatewayStatusReportAutoSaved, Integer>{

	
}
