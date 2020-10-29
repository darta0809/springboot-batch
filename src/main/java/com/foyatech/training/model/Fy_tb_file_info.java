package com.foyatech.training.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Fy_tb_file_info {
	
	private BigDecimal fileSeq;
	private String fileType;
	private String fileName;
	private String transactionType;
	private BigDecimal count;
	private BigDecimal amount;
	private String crUser;
	private LocalDateTime crDate;
	private String userStamp;
	private LocalDateTime dateStamp;
	private LocalDateTime minTime;
	private LocalDateTime maxTime;
}
