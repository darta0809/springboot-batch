package com.foyatech.training.model;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class Fy_tb_file_info {
	
	private BigDecimal fileSeq;
	private String fileType;
	private String fileName;
	private String transactionType;
	private BigDecimal count;
	private BigDecimal amount;
}
