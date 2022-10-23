create table FILE_INFO
(
    ID               bigint auto_increment,
    TRANSACTION_TYPE nvarchar(255) null,
    TRANSACTION_SEQ  numeric       null,
    TRANSACTION_TIME datetime      null,
    CUST_ID          nvarchar(255) null,
    STORE_ID         nvarchar(255) null,
    PRODUCT_ID       nvarchar(255) null,
    PHONE            nvarchar(255) null,
    ADDRESS          nvarchar(255) null,
    MEMO             nvarchar(255) null,
    PRICE            numeric       null,
    UNIT             numeric       null,
    AMOUNT           numeric       null,
    constraint FILE_CONTROLLER_pk
        primary key (ID)
);

create unique index FILE_INFO_uindex
    on FILE_INFO (ID);


