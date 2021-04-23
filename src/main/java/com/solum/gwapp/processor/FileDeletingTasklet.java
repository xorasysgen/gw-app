package com.solum.gwapp.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class FileDeletingTasklet implements Tasklet, InitializingBean {


   private Resource resource;

   @Override
   public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

      Path filename = Paths.get(resource.getFile().toString());
      log.info("{} file is marked for clean up" , resource.getFile().toString());
      try {
         if(Files.exists(filename)) {
            Files.delete(filename);
            log.info("{} cleaned up", filename);
         }
         else {
         log.warn("{} does not exists", filename);
         }

      } catch (IOException io) {
         log.error("{}",io.getMessage());
      }
      return RepeatStatus.FINISHED;
   }

   public void setResources(Resource resource) {
      this.resource = resource;
   }

   public void afterPropertiesSet() throws Exception {
      Assert.notNull(resource, "directory must be set");
   }
}