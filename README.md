# 排班系統

員工可提交次月班表，老闆可查看月份總覽與統計。前端使用 Vue 3，後端使用 Spring Boot，資料庫使用 PostgreSQL。

## 專案結構

- `frontend`：Vue 3、TypeScript、Vite
- `backend`：Spring Boot、Spring Security、JPA、Flyway

## 啟動

先確認 PostgreSQL 可用，再於專案根目錄執行：

```powershell
.\start-dev.cmd
```

開啟 `http://localhost:5173`。第一次啟動後端時，Flyway 會自動建立資料表與 Demo 資料。

## Demo 帳號

| 角色 | 帳號 | 密碼 |
| --- | --- | --- |
| 老闆 | `owner@example.com` | `Owner123!` |
| 員工 | `amy@example.com` | `Employee123!` |
| 員工 | `ben@example.com` | `Employee123!` |
| 員工 | `cindy@example.com` | `Employee123!` |

## 功能

- 員工只能編輯下個月班表，一次送出 6 至 15 天。
- 週末與國定假日禁止排班，假日顯示名稱。
- 每天最多兩名員工，後端使用資料庫交易鎖避免超額。
- 老闆可查看指定月份班表、每人月／年排班天數與排名。
- JWT 驗證與角色權限保護員工和老闆 API。

## 測試與建置

```powershell
cd frontend
npm.cmd run build

cd ..\backend
mvn.cmd test
```

資料庫密碼存放在 `C:\Users\necro\AppData\Local\SchedulingDev`，不會提交到專案。
