package com.example.demo.model;

import com.opencsv.bean.CsvBindByName;
import java.math.BigDecimal;
import lombok.Data;

@Data
public class FileInfoCsv {

    @CsvBindByName(column = "TRANSACTION_TYPE")
    private String transactionType;

    @CsvBindByName(column = "TRANSACTION_SEQ")
    private Integer transactionSeq;
    
    @CsvBindByName(column = "TRANSACTION_TIME")
    private String transactionTime;

    @CsvBindByName(column = "CUST_ID")
    private String custId;

    @CsvBindByName(column = "STORE_ID")
    private String storeId;

    @CsvBindByName(column = "PRODUCT_ID")
    private String productId;

    @CsvBindByName(column = "PHONE")
    private String phone;

    @CsvBindByName(column = "ADDRESS")
    private String address;

    @CsvBindByName(column = "MEMO")
    private String memo;

    @CsvBindByName(column = "PRICE")
    private BigDecimal price;

    @CsvBindByName(column = "UNIT")
    private BigDecimal unit;
}
