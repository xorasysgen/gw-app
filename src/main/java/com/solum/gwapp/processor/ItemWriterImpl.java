package com.solum.gwapp.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import com.solum.gwapp.dto.GwDTO;
import com.solum.gwapp.repository.GwRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class ItemWriterImpl implements  ItemWriter<GwDTO> {

	@Autowired
	GwRepository gwRepository;

	@Value("${filename}")
	Resource resource;

	@Override
	public void write(List<? extends GwDTO> gwDTO) throws Exception {
		log.info("Inserting... records");
		gwRepository.deleteAll();
		gwRepository.flush();
		gwRepository.saveAll(gwDTO);
		gwRepository.flush();

	}

}
