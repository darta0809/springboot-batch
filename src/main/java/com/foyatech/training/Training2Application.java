package com.foyatech.training;

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
public class Training2Application {

	@Autowired
	JobLauncher jobLauncher;
	@Autowired
	Job job;
	@Value("${batch.runOnce}")
	boolean runOnce;

	public static void main(String[] args) {
		SpringApplication.run(Training2Application.class, args);
	}

	// 每一分鐘執行一次
	@Scheduled(fixedDelayString = "${fixedDelayString.expression}")
	public void perform() throws Exception {
		JobParameters params = new JobParametersBuilder()
				.addString("JobID", String.valueOf(System.currentTimeMillis()))
				.toJobParameters();

		JobExecution execution = jobLauncher.run(job, params);

		System.out.println("STATUS :: " + execution.getStatus());
		System.out.println("耗時 :: " + (execution.getEndTime().getTime() - execution.getStartTime().getTime()) + "ms");
		System.out.println(execution.getJobInstance());
		System.out.println(execution.getStepExecutions());

		if (runOnce) {
			System.exit(0);
		}
	}

}
