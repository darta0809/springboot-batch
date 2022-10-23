package com.example.demo.tasklet;

import com.example.demo.model.Root;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.io.FileUtils;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;

@Log4j2
public class LinesReader implements Tasklet, StepExecutionListener {

  private List<String> lines;
  private List<String[]> data;
  private File file;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    file = new File("C:/Users/vincent.ta/IdeaProjects/demo/input/data.csv");
    try {
      lines = FileUtils.readLines(file, "UTF-8");
    } catch (IOException e) {
      e.printStackTrace();
    }
    log.debug("Lines Reader initialized.");
  }

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
      throws Exception {

    Root root = (Root) chunkContext.getStepContext().getJobExecutionContext().get("root");
    data = new ArrayList<>();
    for (String line : lines) {
      String[] fields = line.split(
          root.getInput().getDelimiter() + "(?=([^\"]*\"[^\"]*\")*[^\"]*$)", -1);
      data.add(fields);
    }

    return RepeatStatus.FINISHED;
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    stepExecution.getJobExecution().getExecutionContext().put("data", data);
    log.debug("Lines Reader ended.");
    return ExitStatus.COMPLETED;
  }


}
