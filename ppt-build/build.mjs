import fs from 'node:fs/promises';
import path from 'node:path';
import { pathToFileURL } from 'node:url';
import { Presentation, PresentationFile } from '@oai/artifact-tool';

const workspaceDir = process.cwd();
const SKILL_DIR = 'C:/Users/Admin/.codex/plugins/cache/openai-primary-runtime/presentations/26.909.12148/skills/presentations';
const TMP_DIR = path.join(workspaceDir, 'ppt-build');
const FINAL_PPTX = path.join(workspaceDir, '排班系統_面試簡報_最新版.pptx');
await fs.mkdir(TMP_DIR, { recursive: true });
const { resolvePresentationFont } = await import(pathToFileURL(path.join(SKILL_DIR, 'container_tools/artifact_tool_utils.mjs')).href);
const family = resolvePresentationFont({ availableFonts: ['Aptos', 'Microsoft JhengHei'] });
const p = Presentation.create({ slideSize: { width: 1280, height: 720 } });
const C = { navy:'#102A43', blue:'#1976D2', teal:'#00A6A6', orange:'#F59E0B', ink:'#243B53', muted:'#627D98', bg:'#F5F8FB', white:'#FFFFFF', line:'#D9E2EC' };
function box(slide, x,y,w,h, fill=C.white, radius=0){ const s=slide.shapes.add({geometry: radius?'roundRect':'rect', position:{left:x,top:y,width:w,height:h}, fill, line:{fill:'none',width:0}}); return s; }
function text(slide, str, x,y,w,h, size=22, color=C.ink, bold=false, align='left'){ const s=slide.shapes.add({geometry:'textbox', position:{left:x,top:y,width:w,height:h}, fill:'none', line:{fill:'none',width:0}}); s.text=str; s.text.style={typeface:family,fontSize:size,bold,color,autoFit:'shrink'}; s.text.paragraphFormat={alignment:align}; return s; }
function line(slide,x1,y1,x2,y2,color=C.line,width=2){ slide.shapes.add({geometry:'line',position:{left:x1,top:y1,width:x2-x1,height:y2-y1},line:{fill:color,width}}); }
function base(title, kicker='面試作品解說'){ const s=p.slides.add(); s.background.fill=C.bg; text(s,kicker.toUpperCase(),72,34,500,24,14,C.teal,true); text(s,title,72,64,1100,54,34,C.navy,true); line(s,72,132,1208,132,C.line,2); return s; }
function note(s, t){ s.speakerNotes.textFrame.setText(t); }

let s=p.slides.add(); s.background.fill=C.navy; box(s,760,0,520,720,C.blue); box(s,840,80,260,560,C.teal,1); text(s,'排班系統',84,150,620,86,58,C.white,true); text(s,'技術設計與實作重點',88,248,600,54,34,'#BDE7F2',false); text(s,'面試作品解說',90,334,400,32,20,C.white,false); text(s,'Vue 3 × Spring Boot × PostgreSQL',90,592,650,32,18,'#D9F2F2'); note(s,'簡報目的：用 7 張投影片說清楚系統架構、核心規則與工程取捨。');

s=base('專案目標與使用者流程');
text(s,'把「次月可排班」變成可控、可追蹤的流程',72,158,900,38,24,C.ink,true);
const steps=[['01','員工登入','JWT 驗證身份'],['02','查看可排日期','週末與國定假日不可排'],['03','提交 6–15 天','前端提示、後端再驗證'],['04','老闆檢視總覽','月／年統計與排名']];
steps.forEach((d,i)=>{const x=78+i*282; box(s,x,250,238,190,C.white,1); text(s,d[0],x+18,270,60,30,18,C.teal,true); text(s,d[1],x+18,318,200,34,24,C.navy,true); text(s,d[2],x+18,365,195,48,17,C.muted); if(i<3) line(s,x+238,345,x+278,345,C.blue,3);});
note(s,'面試說法：先從使用者需求切入，再連到技術設計。系統刻意把規則放在後端做最後防線。');

s=base('整體架構');
text(s,'前後端分離，讓畫面、商業規則與資料持久化各自負責',72,158,980,36,23,C.ink,true);
box(s,90,250,280,160,'#E6F0FA',1); text(s,'Vue 3 前端',115,275,230,36,25,C.navy,true); text(s,'TypeScript\nVue Router + Pinia\nAxios API client',115,325,220,70,18,C.ink);
box(s,500,250,280,160,'#E6FFFB',1); text(s,'Spring Boot API',525,275,230,36,25,C.navy,true); text(s,'Controller / Service\nSpring Security\nValidation + Transaction',525,325,240,70,18,C.ink);
box(s,910,250,280,160,'#FFF4D6',1); text(s,'PostgreSQL',935,275,230,36,25,C.navy,true); text(s,'JPA Repository\nFlyway migration\nUnique constraints + index',935,325,235,70,18,C.ink);
line(s,370,330,500,330,C.blue,4); line(s,780,330,910,330,C.blue,4); text(s,'Bearer JWT',396,292,100,26,15,C.blue,true); text(s,'SQL / JPA',810,292,100,26,15,C.blue,true); note(s,'面試說法：前端負責體驗，後端負責可信規則，資料庫負責一致性與查詢效率。');

s=base('核心規則：排班不是單純 CRUD');
text(s,'同一個日期最多兩名員工，競態條件要在交易內解決',72,158,1050,36,23,C.ink,true);
box(s,90,245,470,250,C.white,1); text(s,'提交班表時的後端流程',120,272,380,32,23,C.navy,true); text(s,'1  驗證月份與天數範圍\n2  檢查 calendar_days 是否可排\n3  以交易鎖定並計算當日人數\n4  通過才寫入 schedule_entries',120,330,390,120,19,C.ink);
box(s,665,245,500,250,'#102A43',1); text(s,'資料庫保護',700,272,380,32,23,C.white,true); text(s,'UNIQUE(employee_id, work_date)\n避免同一員工重複排同一天\n\nidx_schedule_work_date\n加速月曆與統計查詢',700,330,410,120,19,'#D9E2EC'); note(s,'面試追問重點：前端的 disabled 只是 UX，不能取代後端交易與資料庫約束。');

 s=base('權限與狀態管理');
text(s,'JWT + Route Guard 形成兩層保護',72,158,800,36,23,C.ink,true);
box(s,100,235,430,265,C.white,1); text(s,'前端：導航層',130,265,300,32,23,C.navy,true); text(s,'記憶體保存 access token\nHttpOnly cookie 保存 refresh token\nrouter.beforeEach 先 initialize\n\nOWNER → /dashboard\nEMPLOYEE → /schedule',130,320,340,140,19,C.ink);
box(s,680,235,430,265,C.white,1); text(s,'後端：API 層',710,265,300,32,23,C.navy,true); text(s,'Spring Security 驗證 JWT\nrefresh token rotation\n角色限制員工與老闆 API\nApiExceptionHandler 統一錯誤格式',710,320,350,140,19,C.ink);
line(s,530,365,680,365,C.teal,4); text(s,'不能只相信前端',540,325,140,32,15,C.teal,true,'center'); note(s,'面試說法：前端 guard 讓使用者體驗清楚，後端 security 才是真正的授權邊界。');

s=base('國定假日與資料生命週期');
text(s,'把外部日曆資料轉成內部可查詢的規則資料',72,158,1050,36,23,C.ink,true);
const life=[['外部來源','NtpcHolidayClient','取得假日資料'],['初始化 / 更新','HolidayCalendarInitializer','寫入 calendar_days'],['業務判斷','ScheduleService','依 schedulable 擋排班'],['畫面呈現','ScheduleView','顯示假日名稱與狀態']];
life.forEach((d,i)=>{const x=80+i*285; box(s,x,275,235,170,i===2?'#E6FFFB':C.white,1); text(s,d[0],x+18,295,190,26,16,C.teal,true); text(s,d[1],x+18,335,200,30,20,C.navy,true); text(s,d[2],x+18,382,190,40,17,C.muted); if(i<3) line(s,x+235,360,x+275,360,C.orange,3);});
note(s,'面試說法：外部 API 只在整合層處理，業務層只依賴 calendar_days，降低耦合並方便測試。');

s=base('測試與可維護性');
text(s,'測試覆蓋最容易出錯的規則，而不是只測 happy path',72,158,1080,36,23,C.ink,true);
box(s,90,245,330,245,'#E6F0FA',1); text(s,'後端單元測試',120,275,260,32,23,C.navy,true); text(s,'ScheduleServiceTest\n涵蓋天數範圍、假日、\n容量與重複排班規則',120,340,260,92,20,C.ink);
box(s,475,245,330,245,'#E6FFFB',1); text(s,'Controller 測試',505,275,260,32,23,C.navy,true); text(s,'OwnerReportControllerTest\n驗證查詢結果與角色邊界',505,340,260,92,20,C.ink);
box(s,860,245,330,245,'#FFF4D6',1); text(s,'建置驗證',890,275,260,32,23,C.navy,true); text(s,'Vue type-check + Vite build\nMaven test\nFlyway migration 可重現',890,340,260,92,20,C.ink);
note(s,'面試說法：把商業規則集中在 service，測試可以直接驗證，不需要每次都透過瀏覽器操作。');

s=base('面試總結：我在這個專案展示的能力');
text(s,'從需求拆解，到一致性、安全性與可維護性的完整思考',72,158,1100,36,23,C.ink,true);
const a=[['架構設計','前後端分離，責任清楚'],['資料一致性','交易鎖、unique constraint、index'],['安全性','JWT、HttpOnly cookie、token rotation'],['工程品質','migration、例外處理、測試']];
a.forEach((d,i)=>{const y=245+i*72; text(s,d[0],100,y,220,32,22,C.teal,true); line(s,330,y+18,390,y+18,C.orange,3); text(s,d[1],420,y,650,32,21,C.ink);});
text(s,'可以延伸的下一步：部署到雲端、加入審核流程、補上 E2E 測試與操作紀錄',100,560,1000,34,19,C.muted);
note(s,'結尾說法：這個作品的重點不是功能很多，而是把容易出錯的規則放在正確的層級並可被驗證。');

const draft=path.join(TMP_DIR,'candidate.pptx'); await (await PresentationFile.exportPptx(p)).save(draft);
const { finalizePresentation } = await import(pathToFileURL(path.join(SKILL_DIR,'container_tools/artifact_tool_utils.mjs')).href);
const outDir=path.join(workspaceDir,'ppt-finalizer'); await fs.mkdir(outDir,{recursive:true});
await finalizePresentation({workspaceDir,candidatePath:draft,finalPath:FINAL_PPTX,pythonExecutable:'C:/Users/Admin/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe',integrityValidatorPath:path.join(SKILL_DIR,'container_tools/inspect_presentation_package_integrity.py'),layoutValidatorPath:path.join(SKILL_DIR,'container_tools/inspect_presentation_layout_geometry.py'),layoutArgs:['--expected-slide-size-emu','12192000,6858000','--validate-heading-fit'],fontPolicy:{basis:'design',families:[family]},verifyArtifactToolImport:true});
console.log(FINAL_PPTX);
