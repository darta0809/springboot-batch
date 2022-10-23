package com.example.demo;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@EnableScheduling
@SpringBootApplication
public class DemoApplication {

  @Autowired
  JobLauncher jobLauncher;
  @Autowired
  Job job;
  @Value("${batch.runOnce:true}")
  boolean runOnce;

  public static void main(String[] args) {
    SpringApplication.run(DemoApplication.class, args);
  }

  @Scheduled(fixedDelayString = "${fixedDelayString.expression:600000}")
  public void perform() throws Exception {
    JobParameters params = new JobParametersBuilder()
        .addString("JobID", String.valueOf(System.currentTimeMillis()))
        .toJobParameters();

    JobExecution execution = jobLauncher.run(job, params);

    System.out.println("STATUS :: " + execution.getStatus());
    System.out.println(
        "耗時 :: " + (execution.getEndTime().getTime() - execution.getStartTime().getTime()) + "ms");
    System.out.println(execution.getJobInstance());
    System.out.println(execution.getStepExecutions());

    if (runOnce) {
      System.exit(0);
    }
  }
}
