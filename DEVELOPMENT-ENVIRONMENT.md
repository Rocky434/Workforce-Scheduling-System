# Vue + Spring Boot + PostgreSQL 開發環境

本次僅設定開發工具，未建立應用程式專案或業務資料表。

## 工具

| 工具 | 版本 |
| --- | --- |
| Eclipse Temurin JDK | 21.0.12.1 |
| Apache Maven | 3.9.16 |
| Node.js | 24.21.0 LTS |
| npm | 11.19.0 |
| PostgreSQL | 18.6 |
| pgAdmin | 9.17 |

JDK、Maven、Node.js 與 pgAdmin 安裝在 `C:\Users\necro\AppData\Local\DevTools`。
PostgreSQL 服務執行檔位於 `C:\Program Files\PostgreSQL\18`，正式資料目錄為 `C:\ProgramData\PostgreSQL\18\data`。
`JAVA_HOME` 與使用者 `PATH` 已設定；安裝後請重新開啟終端，若編輯器仍沿用舊環境，請重新啟動編輯器。

PowerShell 的腳本執行政策可能阻擋 `npm.ps1`，請使用 `npm.cmd` 與 `npx.cmd`。
Vue 與 Spring Boot 由各自專案的相依套件設定管理，不需全域安裝框架。

## 資料庫

連線設定與隨機密碼保存在專案外的私人目錄：

`C:\Users\necro\AppData\Local\SchedulingDev\connection-info.txt`

資料庫名稱為 `scheduling`，應用帳號為 `scheduling_app`。
連線位址為 `localhost:5432`，JDBC URL 為 `jdbc:postgresql://localhost:5432/scheduling`。
請以該檔案中的連接埠與密碼設定 Spring Boot 或 pgAdmin，勿將密碼提交到 Git。

pgAdmin 可從 Windows 開始功能表的「pgAdmin 4」開啟，連線時填入上述設定。

## 目前狀態與啟停

所有工具的版本檢查、npm 連線、資料庫應用帳號登入與 `SELECT 1` 已通過。
資料庫使用 UTF-8、Asia/Taipei，只有本機 IPv4／IPv6 監聽，業務資料表數量為 0。
應用帳號無超級使用者、建立資料庫或建立角色權限。

**Windows 自動服務尚未完成驗證。** `postgresql-x64-18` 已註冊為 Automatic，但使用 NetworkService 啟動仍逾時。
目前資料庫已使用你的帳戶手動啟動。重新開機後請先使用下列啟動指令：

```powershell
& 'C:\Users\necro\AppData\Local\SchedulingDev\start-postgresql.cmd'
```

停止資料庫：

```powershell
& 'C:\Users\necro\AppData\Local\SchedulingDev\stop-postgresql.cmd'
```

自動核准審查拒絕將服務改為 LocalSystem，該帳戶變更未執行。
`DevTools` 中保留官方下載包及原生工具；`SchedulingDev\data` 是初始化時留下的副本，不是目前使用的資料目錄，請勿啟動該副本。

## 常用檢查

```powershell
java -version
javac -version
mvn.cmd -version
node --version
npm.cmd --version
npm.cmd ping
psql --version
```
