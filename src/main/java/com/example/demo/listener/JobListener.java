package com.example.demo.listener;

import com.example.demo.model.Root;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import java.io.FileInputStream;
import java.io.InputStream;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.stereotype.Component;

@Log4j2
@Component
public class JobListener extends JobExecutionListenerSupport {

  @Override
  @SneakyThrows
  public void beforeJob(JobExecution jobExecution) {
    if (jobExecution.getStatus() != BatchStatus.STARTED) {
      return;
    }

    log.info("BATCH JOB STARTED SUCCESSFULLY");

    InputStream in = new FileInputStream("C:/Users/vincent.ta/IdeaProjects/demo/conf/csv2fix.xml");
    Root root = new XmlMapper().readValue(in, Root.class);

    jobExecution.getExecutionContext().put("root", root);
  }

  @Override
  public void afterJob(JobExecution jobExecution) {
    if (jobExecution.getStatus() == BatchStatus.COMPLETED
        || jobExecution.getStatus() == BatchStatus.FAILED) {
      log.info("BATCH JOB " + jobExecution.getStatus());
    }
  }

}
