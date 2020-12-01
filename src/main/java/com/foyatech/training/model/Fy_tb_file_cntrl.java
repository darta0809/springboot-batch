package com.foyatech.training.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Fy_tb_file_cntrl {

	private BigDecimal fileSeq;
	private String fileType;
	private String fileName;
	private String status;
	private LocalDateTime startTime;
	private LocalDateTime endTime;
	private BigDecimal totalCount;
	private BigDecimal totalAmount;
	private BigDecimal errorCount;
	private String dscr;
	private LocalDateTime minTransactionTime;
	private LocalDateTime maxTransactionTime;

}
