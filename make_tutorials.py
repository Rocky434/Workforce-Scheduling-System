from reportlab.lib.pagesizes import A4
from reportlab.lib import colors
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.enums import TA_CENTER
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, PageBreak, Table, TableStyle, Preformatted, KeepTogether
from reportlab.lib.units import mm
from reportlab.pdfgen.canvas import Canvas
from pathlib import Path

ROOT=Path.cwd(); OUT=ROOT/'tutorials'; OUT.mkdir(exist_ok=True)
pdfmetrics.registerFont(TTFont('JhengHei','C:/Windows/Fonts/msjh.ttc', subfontIndex=0))
pdfmetrics.registerFont(TTFont('JhengHei-Bold','C:/Windows/Fonts/msjhbd.ttc', subfontIndex=0))
styles=getSampleStyleSheet()
styles.add(ParagraphStyle('TitleCN',parent=styles['Title'],fontName='JhengHei-Bold',fontSize=25,leading=34,textColor=colors.HexColor('#102A43'),alignment=TA_CENTER,spaceAfter=16))
styles.add(ParagraphStyle('H1CN',parent=styles['Heading1'],fontName='JhengHei-Bold',fontSize=18,leading=25,textColor=colors.HexColor('#1976D2'),spaceBefore=12,spaceAfter=8))
styles.add(ParagraphStyle('H2CN',parent=styles['Heading2'],fontName='JhengHei-Bold',fontSize=13,leading=19,textColor=colors.HexColor('#102A43'),spaceBefore=8,spaceAfter=5))
styles.add(ParagraphStyle('BodyCN',parent=styles['BodyText'],fontName='JhengHei',fontSize=10.5,leading=17,textColor=colors.HexColor('#243B53'),spaceAfter=6))
styles.add(ParagraphStyle('SmallCN',parent=styles['BodyText'],fontName='JhengHei',fontSize=8.5,leading=13,textColor=colors.HexColor('#627D98')))
styles.add(ParagraphStyle('CodeCN',parent=styles['Code'],fontName='Courier',fontSize=7.5,leading=10,textColor=colors.HexColor('#102A43'),backColor=colors.HexColor('#F2F6FA'),borderPadding=7,leftIndent=8,rightIndent=8,spaceBefore=4,spaceAfter=7))

def P(x,style='BodyCN'): return Paragraph(x,styles[style])
def header_footer(canv,doc):
    canv.saveState(); canv.setStrokeColor(colors.HexColor('#D9E2EC')); canv.line(18*mm,15*mm,192*mm,15*mm)
    canv.setFont('JhengHei',8); canv.setFillColor(colors.HexColor('#627D98')); canv.drawString(18*mm,9*mm,'排班系統技術教學')
    canv.drawRightString(192*mm,9*mm,f'{doc.page}'); canv.restoreState()
def table(rows,widths):
    t=Table([[P(str(c),'SmallCN') for c in r] for r in rows],colWidths=widths,repeatRows=1)
    t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#E6F0FA')),('TEXTCOLOR',(0,0),(-1,0),colors.HexColor('#102A43')),('GRID',(0,0),(-1,-1),.4,colors.HexColor('#D9E2EC')),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),6),('RIGHTPADDING',(0,0),(-1,-1),6),('TOPPADDING',(0,0),(-1,-1),6),('BOTTOMPADDING',(0,0),(-1,-1),6)])); return t

def backend():
    f=[]; f+=[Spacer(1,25*mm),P('排班系統後端技術教學','TitleCN'),P('從 Spring Boot API 到資料一致性與 JWT 安全設計','H2CN'),Spacer(1,8*mm),P('本教材對應目前專案的 backend 原始碼，目標是讓你能在面試中解釋「為什麼這樣設計」，也能自己修改功能。','BodyCN'),PageBreak()]
    f += [P('1. 後端技術地圖','H1CN'),table([['層次','技術','責任'],['Web API','Spring Boot 4.1.1 / MVC','接收 HTTP、驗證輸入、回傳 JSON'],['商業邏輯','Service + @Transactional','集中排班規則與交易邊界'],['資料存取','Spring Data JPA','Repository 封裝查詢'],['資料庫','PostgreSQL','約束、索引、交易一致性'],['版本管理','Flyway','用 migration 建立可重現 schema'],['安全','Spring Security + OAuth2 Resource Server','驗證 JWT、轉換角色權限']], [38*mm,58*mm,88*mm]),P('建議理解順序：先看 Controller 找到 API，再進 Service 看規則，最後看 Repository 與 migration 如何落地。','BodyCN')]
    f += [P('2. Spring Boot 專案結構','H1CN'),P('啟動類別建立 Spring Application Context。標記為 @RestController 的類別處理 HTTP，@Service 類別放商業邏輯，Repository 由 Spring Data 產生實作。這種分層讓測試可以直接針對 service，不必透過瀏覽器。','BodyCN'),Preformatted('src/main/java/com/example/scheduling\n  auth/        登入、access token、refresh token\n  schedule/    排班 API、Service、Entity、Repository\n  calendar/    工作日與假日同步\n  report/      老闆統計報表\n  config/      Security、CORS、例外處理\n  user/        使用者 Entity 與 Repository',styles['CodeCN'])]
    f += [P('3. API 的完整生命週期','H1CN'),P('以員工提交班表為例：HTTP request 先進入 Controller，@Valid 驗證格式，再呼叫 Service。Service 檢查月份、日期、員工身份與容量，Repository 查詢／寫入 PostgreSQL，最後由 Controller 回傳 MonthView。','BodyCN'),Preformatted('@PostMapping("/api/schedules/month")\npublic MonthView replace(@AuthenticationPrincipal Jwt jwt,\n        @Valid @RequestBody UpdateRequest request) {\n    return service.replaceMonth(jwt.getSubject(),\n        request.month(), request.dates());\n}',styles['CodeCN']),P('Controller 應該薄：它負責 HTTP 邊界。規則集中在 ScheduleService，避免同一規則散落在多個 endpoint。','BodyCN')]
    f += [P('4. 排班規則與交易鎖','H1CN'),P('replaceMonth() 使用 @Transactional，代表刪除舊班表、重新計算容量與寫入新班表會在同一個交易內完成。days.lockMonth(start, end) 先鎖定指定月份的 calendar rows，讓同一時間提交的請求依序取得鎖，避免兩個請求同時看到相同的剩餘容量。','BodyCN'),Preformatted('1. TreeSet 去除重複日期\n2. 限制 6 至 15 天\n3. 限制所有日期屬於指定月份\n4. 鎖定該月 calendar_days\n5. 檢查週末、假日與每日最多 2 人\n6. delete 舊資料後 saveAll 新資料',styles['CodeCN']),P('資料庫的 UNIQUE(employee_id, work_date) 是第二道防線。即使程式未來出現 bug，也不能讓同一員工同一天重複排班。','BodyCN')]
    f += [P('5. JWT、Refresh Token、安全與角色','H1CN'),P('登入成功後，後端回傳短效 access token，並以 HttpOnly、SameSite=Strict cookie 保存 refresh token。前端只把 access token 放在記憶體，過期時呼叫 /api/auth/refresh。RefreshTokenService 會 rotate token；若偵測到已被取代的 token 重用，便撤銷整個 family，降低 token 被竊取後的風險。','BodyCN'),Preformatted('authorizeHttpRequests(a -> a\n  .requestMatchers("/api/auth/login", "/api/auth/refresh",\n      "/api/auth/logout").permitAll()\n  .anyRequest().authenticated())',styles['CodeCN']),P('JwtDecoder 驗證簽章、issuer 與 token_type，JwtAuthenticationConverter 把 role claim 轉成 ROLE_OWNER 或 ROLE_EMPLOYEE。密碼使用 BCrypt，不保存明文；JWT secret 從環境設定取得；CORS 只允許設定的前端 origin。','BodyCN')]
    f += [P('6. 國定假日同步與容錯','H1CN'),P('HolidayCalendarInitializer implements ApplicationRunner，在應用程式啟動時取得新北市政府資料，轉成 calendar_days。若外部 API 失敗，程式保留既有資料並補齊缺少日期，至少讓週末規則仍可工作。這是「外部依賴失敗不應讓核心功能完全不可用」的實作。','BodyCN'),P('面試可說：業務層只依賴內部 CalendarDay，不直接依賴外部 API，降低耦合，也讓測試可以用 fake client 或固定 Clock。','BodyCN')]
    f += [P('7. 測試與面試回答','H1CN'),table([['問題','回答方向'],['為什麼需要 transaction？','避免重新提交班表時，刪除與新增中間留下半套資料，也控制容量檢查的競態。'],['為什麼前端 disabled 不夠？','前端可被繞過，後端與資料庫才是可信邊界。'],['為什麼 Flyway？','schema 變更有版本、有順序，環境能重現。'],['如何擴充審核流程？','新增 schedule status、審核 endpoint，保留 service 交易邊界與角色授權。']], [45*mm,139*mm]),P('練習：新增「員工取消單日班表」API，思考是否仍要限制只能操作次月、是否要檢查身份、以及刪除後如何回傳最新月曆。','BodyCN')]
    return f

def frontend():
    f=[Spacer(1,25*mm),P('排班系統前端技術教學','TitleCN'),P('Vue 3、TypeScript、Router、Pinia 與 Axios 實作導讀','H2CN'),Spacer(1,8*mm),P('這份教材特別把 router、元件、狀態與 API 串接拆開說明，適合前端基礎還不熟時循序閱讀。','BodyCN'),PageBreak()]
    f += [P('1. 前端在做什麼？','H1CN'),P('前端是瀏覽器裡的應用程式。Vue 負責把資料轉成畫面，Router 決定 URL 對應哪個頁面，Pinia 保存跨頁共用的登入狀態，Axios 負責呼叫後端 API。','BodyCN'),table([['檔案','用途'],['main.ts','建立 Vue app、掛上 Pinia 與 router'],['App.vue','最外層版型與 router-view'],['router.ts','URL、頁面與登入導頁規則'],['stores/auth.ts','login、logout、目前 user'],['api.ts','Axios baseURL、Authorization、錯誤轉換'],['views/*.vue','實際畫面：登入、員工班表、老闆總覽']], [48*mm,136*mm])]
    f += [P('2. Vue 3 元件與 reactive data','H1CN'),P('每個 .vue 檔通常由 template、script setup、style 三部分組成。template 描述畫面，script setup 寫資料與事件，style 管外觀。ref() 建立可變 reactive value，讀取時用 .value；在 template 中 Vue 會自動解開。','BodyCN'),Preformatted('<script setup lang="ts">\nimport { ref } from "vue"\nconst count = ref(0)\nfunction add() { count.value++ }\n</script>\n<template><button @click="add">{{ count }}</button></template>',styles['CodeCN']),P('常見語法：v-if 控制是否顯示，v-for 產生清單，:class 綁定 class，@click 綁定事件，v-model 雙向綁定輸入框。','BodyCN')]
    f += [P('3. Router 詳解：URL 如何切換頁面','H1CN'),P('createRouter() 建立 router；createWebHistory() 使用瀏覽器的正常 URL。routes 是路由表，每一筆 route 把 path 對應到 component。App.vue 通常放 <router-view />，匹配到的頁面會渲染在那裡。','BodyCN'),Preformatted('const router = createRouter({\n  history: createWebHistory(),\n  routes: [\n    { path: "/login", component: LoginView },\n    { path: "/schedule", component: ScheduleView,\n      meta: { role: "EMPLOYEE" } },\n    { path: "/dashboard", component: DashboardView,\n      meta: { role: "OWNER" } },\n  ],\n})',styles['CodeCN']),P('path 是網址；component 是頁面；meta 是你自己附加的資料，這裡用來註記該頁需要的角色。路由本身不會自動阻擋使用者，必須搭配 navigation guard。','BodyCN')]
    f += [P('4. Router Guard：登入與角色檢查','H1CN'),P('router.beforeEach() 會在每次導頁前執行。to 是即將前往的 route，讀取 localStorage 的 user 後，依條件回傳字串即可重新導向；沒有回傳值代表允許前往。','BodyCN'),Preformatted('router.beforeEach((to) => {\n  const raw = localStorage.getItem("user")\n  const user = raw ? JSON.parse(raw) : null\n\n  if (to.path !== "/login" && !user) return "/login"\n  if (to.meta.role && user?.role !== to.meta.role) {\n    return user?.role === "OWNER" ? "/dashboard" : "/schedule"\n  }\n})',styles['CodeCN']),P('實際流程：使用者輸入 /dashboard → guard 讀取 user → 沒登入就去 /login → 已登入但不是 OWNER 就回員工頁 → 只有 OWNER 放行。這是前端 UX 保護；後端仍必須再次驗證角色。','BodyCN')]
    f += [P('5. Pinia：跨頁共用登入狀態','H1CN'),P('Pinia store 是集中管理狀態的地方。這個 branch 不把 token 或 user 寫入 localStorage，而是把 session 放在記憶體中。應用程式啟動時 initialize() 呼叫 refreshSession()，由瀏覽器自動帶上 HttpOnly refresh cookie，成功後再把新的 access token 與 user 放入 store。','BodyCN'),Preformatted('const auth = useAuthStore()\nawait auth.login(email.value, password.value)\nif (auth.user?.role === "OWNER") router.push("/dashboard")\n\n// 登出：呼叫後端撤銷 refresh session，清除記憶體狀態\nawait auth.logout()',styles['CodeCN']),P('這樣可以避免把長期 refresh token 暴露給 JavaScript。頁面重新整理後，前端不保留 access token，而是透過 refresh cookie 還原 session。','BodyCN')]
    f += [P('6. Axios：記憶體 access token 與自動 refresh','H1CN'),P('api.ts 建立共用 axios instance。baseURL 由 VITE_API_URL 決定，並設定 withCredentials: true，讓瀏覽器帶上 refresh cookie。accessToken 只存在模組變數；request interceptor 將它放進 Authorization。收到 401 時，response interceptor 呼叫 refreshSession()，取得新 token 後重送原 request。','BodyCN'),Preformatted('let accessToken: string | null = null\n\napi.interceptors.request.use((config) => {\n  if (accessToken) config.headers.Authorization = `Bearer ${accessToken}`\n  return config\n})\n\n// 401 -> refresh cookie 換新 access token -> retry 原請求',styles['CodeCN']),P('pendingRefresh 避免多個 API 同時 401 時重複 refresh；navigator.locks 則協調多個瀏覽器分頁。refresh 失敗會觸發 auth:expired，Pinia 清除 user 並導回登入頁。','BodyCN')]
    f += [P('7. 一次看懂「登入與初始化」流程','H1CN'),table([['步驟','發生什麼事'],['1','LoginView 取得 email、password'],['2','auth.login() 呼叫 POST /api/auth/login'],['3','後端回傳 access token，並設定 HttpOnly refresh cookie'],['4','store 只在記憶體保存 access token 與 user'],['5','頁面啟動時 router guard 先 await auth.initialize()'],['6','access token 過期時 Axios 自動 refresh 並重送原請求']], [25*mm,159*mm]),P('除錯方式：Network 看 login、refresh、原 API 的 status code；不要期待在 Application > Local Storage 找到 token。可檢查 Cookie 是否存在，以及 request 是否帶 Authorization。','BodyCN')]
    f += [P('8. 你可以怎麼修改這個前端？','H1CN'),P('新增頁面：建立 views/ReportView.vue → 在 router.ts 加 route → 若需要登入，在 meta 放 role → 由 App.vue 的 router-view 顯示。新增 API：在 view 或獨立 composable 呼叫 api.get/post → 用 ref 保存 loading、data、error → template 依狀態顯示。','BodyCN'),Preformatted('const loading = ref(false)\nconst error = ref("")\nasync function load() {\n  loading.value = true\n  try { data.value = (await api.get("/reports")).data }\n  catch (e) { error.value = (e as Error).message }\n  finally { loading.value = false }\n}',styles['CodeCN']),P('練習順序：先改文字與樣式，再新增一個 route，接著新增一個 GET API，最後再做表單與錯誤狀態。每次只改一個層次，比較容易定位問題。','BodyCN')]
    return f

def build(name, story):
    doc=SimpleDocTemplate(str(OUT/name),pagesize=A4,rightMargin=18*mm,leftMargin=18*mm,topMargin=16*mm,bottomMargin=20*mm,title=name,author='Codex')
    doc.build(story,onFirstPage=header_footer,onLaterPages=header_footer)
build('後端技術教學.pdf',backend()); build('前端技術教學.pdf',frontend())
print('done')
