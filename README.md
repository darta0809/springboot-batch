# springboot-batch

採用 tasklet 方式

job 可讀取檢核檔案，長度、條件等等

reader 讀取 csv

processer 處理讀檔後準備塞入 DB (檢核可在此處理)

writer 存入 DB 產出 fix 檔
