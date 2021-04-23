package com.solum.gwapp.configuration;

import com.solum.gwapp.dto.GwDTO;
import com.solum.gwapp.processor.FileDeletingTasklet;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.LineMapper;
import org.springframework.batch.item.file.mapping.BeanWrapperFieldSetMapper;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.DelimitedLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.io.Resource;



@Configuration
@EnableBatchProcessing
@PropertySource(value={"file:${aims.root.path}/env/gw.properties"})
public class BatchConfig {


	@Value("${filename}")
	Resource resource;

	@Bean
	public Job job(JobBuilderFactory builderFactory,StepBuilderFactory stepBuilderFactory,
				   ItemReader<GwDTO> itemReader,
				   ItemProcessor<GwDTO,GwDTO> itemProcessor,
				   ItemWriter<GwDTO> itemwriter) {

		Step step=stepBuilderFactory.get("Core-GW-File-Load")
				.<GwDTO,GwDTO>chunk(100)
				.reader(itemReader)
				.processor(itemProcessor)
				.writer(itemwriter)
				.build();


		FileDeletingTasklet task = new FileDeletingTasklet();
		task.setResources(resource);
		Step delete= stepBuilderFactory.get("delete-gw-connect-csv")
					.tasklet(task)
					.build();


		return builderFactory.get("Core-GW-Load")
				.incrementer(new RunIdIncrementer())
				.start(step)
				.next(delete)
				.build();


	}




	@Bean
	public FlatFileItemReader<GwDTO> itemReader(@Value("${filename}") Resource resource){

		FlatFileItemReader<GwDTO> flatFileItemReader=new FlatFileItemReader<GwDTO>();
		flatFileItemReader.setResource(resource);
		flatFileItemReader.setName("CSV-Reader");
		flatFileItemReader.setLinesToSkip(1);
		flatFileItemReader.setLineMapper(lineMapperImpl());

		return flatFileItemReader;

	}

	@Bean
	public LineMapper<GwDTO> lineMapperImpl() {

		DefaultLineMapper<GwDTO> defaultlineMapper=new DefaultLineMapper<>();

		BeanWrapperFieldSetMapper<GwDTO> beanWrapperFieldSetMapper=new BeanWrapperFieldSetMapper<>();
		beanWrapperFieldSetMapper.setTargetType(GwDTO.class);

		DelimitedLineTokenizer delimitedLineTokenizer=new DelimitedLineTokenizer();
		delimitedLineTokenizer.setDelimiter(",");
		delimitedLineTokenizer.setStrict(false);
		delimitedLineTokenizer.setNames(new String[] {"gwIP","period","count"});

		defaultlineMapper.setLineTokenizer(delimitedLineTokenizer);
		defaultlineMapper.setFieldSetMapper(beanWrapperFieldSetMapper);
		return defaultlineMapper;
	}

}
