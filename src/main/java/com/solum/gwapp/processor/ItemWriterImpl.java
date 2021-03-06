package com.solum.gwapp.processor;

import com.solum.gwapp.dto.GwDTO;
import com.solum.gwapp.repository.GwRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Slf4j
public class ItemWriterImpl implements  ItemWriter<GwDTO> {

	GwRepository gwRepository;

	@Autowired
	public void setGwRepository(GwRepository gwRepository) {
		this.gwRepository = gwRepository;
	}

	@Value("${filename}")
	Resource resource;

	@Override
	public void write(List<? extends GwDTO> gwDTO) throws Exception {
		log.info("Finalising Job...");
		//gwRepository.deleteAll();
		//gwRepository.flush();
		//gwRepository.saveAll(gwDTO);
		gwRepository.flush();

	}

}
