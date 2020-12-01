package com.foyatech.training.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Data;

@Data
public class Fy_tb_transaction {

	private BigDecimal fileSeq;
	private String fileType;
	private String fileName;
	private String transactionType;
	private BigDecimal transactionSeq;
	private LocalDateTime transactionTime;
	private String custId;
	private String storeId;
	private String productId;
	private String phone;
	private String address;
	private String memo;
	private BigDecimal price;
	private BigDecimal unit;
	private BigDecimal amount;

	private Boolean flag;

}
