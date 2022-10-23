package com.example.demo.config;

import com.example.demo.listener.JobListener;
import com.example.demo.tasklet.LinesProcessor;
import com.example.demo.tasklet.LinesReader;
import com.example.demo.tasklet.LinesWriter;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Log4j2
@Configuration
@EnableBatchProcessing
public class TaskletConfig {

  @Autowired
  private JobBuilderFactory jobBuilderFactory;
  @Autowired
  private StepBuilderFactory stepBuilderFactory;

  @Bean
  public Job job() {
    return jobBuilderFactory
        .get("test-job")
        .incrementer(new RunIdIncrementer())
        .listener(new JobListener())
        .start(readLines())
        .next(processLines())
        .next(writeLines())
        .build();
  }

  @Bean
  protected Step readLines() {
    return stepBuilderFactory
        .get("readLines")
        .tasklet(linesReader())
        .build();
  }

  @Bean
  protected Step processLines() {
    return stepBuilderFactory.get("processLines")
        .tasklet(linesProcessor()).build();
  }

  @Bean
  protected Step writeLines() {
    return stepBuilderFactory.get("writeLines")
        .tasklet(linesWriter()).build();
  }

  @Bean
  public LinesReader linesReader() {
    return new LinesReader();
  }

  @Bean
  public LinesProcessor linesProcessor() {
    return new LinesProcessor();
  }

  @Bean
  public LinesWriter linesWriter() {
    return new LinesWriter();
  }
}
