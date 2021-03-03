# springboot-batch

### 參數:file_type、.properties

流程:
1. 到 DB 尋找待處理資料，若無資料則 exit(0)
2. 若有資料則根據 filt_type 讀取對應 xml 設定檔；放到 jobExecutionContext
3. 採用 tasklet 方式執行<br>
    3-1. 取出設定檔檢核內容、待處理資料 key 為 fileSeq、fileName<br>
    3-2. 先將資料 update 狀態為 W<br>
    3-3. 讀取對應 fileName.file_type 檔案<br>
    3-4. 檢核讀取的檔案是否符合<br>
4. 若檢核皆正確則計算 AMOUNT 值並 insert DB 及產出正確檔，不正確則不 insert DB，產 err 檔
5. insert DB FILE_INFO
6. insert DB 產出正確檔的 fileName 及 output_file_type
7. update DB 狀態為 S
8. 執行下一筆

若過程中出現 exception 則 rollback 所有資料以及將狀態改為 E 並填寫錯誤原因

待重構..
