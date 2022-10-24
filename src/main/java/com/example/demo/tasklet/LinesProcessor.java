package com.example.demo.tasklet;

import com.example.demo.model.FileInfoCsv;
import com.example.demo.repository.FileInfoEntity;
import com.example.demo.utils.Util;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.repeat.RepeatStatus;

public class LinesProcessor implements Tasklet, StepExecutionListener {

  private List<FileInfoCsv> lines;
  private List<FileInfoEntity> fileInfoEntity;

  @Override
  public void beforeStep(StepExecution stepExecution) {
    this.fileInfoEntity = new ArrayList<>();
    ExecutionContext executionContext = stepExecution
            .getJobExecution()
            .getExecutionContext();
    this.lines = (List<FileInfoCsv>) executionContext.get("data");
  }

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext)
          throws Exception {

    lines.forEach(info -> {
      FileInfoEntity fileInfo = new FileInfoEntity();
      fileInfo.setTransactionType(info.getTransactionType().strip());
      fileInfo.setTransactionSeq(info.getTransactionSeq());
      fileInfo.setTransactionTime(Util.parseDateTime(info.getTransactionTime().strip()));
      fileInfo.setCustId(info.getCustId().strip());
      fileInfo.setStoreId(info.getStoreId().strip());
      fileInfo.setProductId(info.getProductId().strip());
      fileInfo.setPhone(info.getPhone().strip());
      fileInfo.setAddress(info.getAddress().strip());
      fileInfo.setMemo(info.getMemo().strip());
      fileInfo.setPrice(info.getPrice());
      fileInfo.setUnit(info.getUnit());
      fileInfo.setAmount(info.getPrice().multiply(info.getUnit()));
      fileInfoEntity.add(fileInfo);
    });

    return RepeatStatus.FINISHED;
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    stepExecution.getJobExecution().getExecutionContext().put("entity", fileInfoEntity);
    return ExitStatus.COMPLETED;
  }
}
