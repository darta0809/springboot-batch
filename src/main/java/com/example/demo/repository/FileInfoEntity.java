package com.example.demo.repository;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.Hibernate;

@Getter
@Setter
@ToString
@RequiredArgsConstructor
@Entity
@Table(name = "FILE_INFO")
public class FileInfoEntity implements Serializable {

    @Id
    @Column(name = "ID", nullable = false)
    private Long id;

    @Column(name = "TRANSACTION_TYPE")
    private String transactionType;

    @Column(name = "TRANSACTION_SEQ")
    private Integer transactionSeq;

    @Column(name = "TRANSACTION_TIME")
    private LocalDateTime transactionTime;

    @Column(name = "CUST_ID")
    private String custId;

    @Column(name = "STORE_ID")
    private String storeId;

    @Column(name = "PRODUCT_ID")
    private String productId;

    @Column(name = "PHONE")
    private String phone;

    @Column(name = "ADDRESS")
    private String address;

    @Column(name = "MEMO")
    private String memo;

    @Column(name = "PRICE")
    private BigDecimal price;

    @Column(name = "UNIT")
    private BigDecimal unit;

    @Column(name = "AMOUNT")
    private BigDecimal amount;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) {
            return false;
        }
        FileInfoEntity fileInfo = (FileInfoEntity) o;
        return id != null && Objects.equals(id, fileInfo.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
