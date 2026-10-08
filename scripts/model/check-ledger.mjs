#!/usr/bin/env node
// check-ledger.mjs — 账本合法性校验（TASK-MODEL-LOG / DISPATCH-LOG）。
// Usage: node check-ledger.mjs [dir] -> exit 0 合法 / exit 1 列出问题
// 分级：结构/枚举错误 = FAIL（exit 1）；写法不规范 = WARN（不影响 exit，供 supervisor 抽查）。
import { readFileSync, existsSync, readdirSync } from "node:fs";
import { join, basename } from "node:path";

const dir = process.argv[2] || "docs/model";
// --allow-example（2026-10-08）：仅供**母版/分发包自检**——模板包带 `_example` 空壳，
// 直接跑必然 exit 1。真实项目禁用此开关，否则「示例行未删」失去强制力。
const ALLOW_EXAMPLE = process.argv.includes("--allow-example");
const fails = [];
const warns = [];

// model 精确写法白名单
const MODEL_PROVIDERS = [
  "codebuddy/", "codex/", "opencode/", "opencode-go/", "opencode-free/",
  "volcengine-plan/", "radeon-mimo/",
];
// 已知合法模型全串（来自 USER_MODEL_OVERRIDE；不在表内但前缀合法的记 WARN，提醒更新）
const KNOWN_MODELS = [
  "codebuddy/deepseek-v4.1-flash", "codebuddy/glm-5.3-flash",
  "codebuddy/deepseek-v4-pro", // 2026-10-07 产品审查双审链审查 B（约定节，非主表行）
  "codex/gpt-6-sol", "codex/gpt-6.1-sol", "codex/gpt-6-luna", "codex/gpt-5.6-luna",
  "codex/gpt-5.6-terra", "codex/gpt-5.6-sol",
  "opencode/muse-spark-1.3-contributor-free", "opencode/muse-spark-1.3-contributor",
  "opencode/mimo-v2.5-free",
  "opencode-go/muse-spark-1.3-contributor", "opencode-go/deepseek-v4.1-flash",
  "opencode-go/glm-5.3-flash", "opencode-go/space-bunny-free",
  "opencode-free/mimo-v2.5-free", "opencode-free/muse-spark-1.3-contributor-free",
  "volcengine-plan/ark-code-latest",
  "radeon-mimo/MiMo-V2.6-Flash",
];
// 特殊合法写法（非 provider/model，但属规范内的“非模型”记录）
const MODEL_SPECIAL = ["本窗口", "本窗口直驱", "本窗口Agent子代理", "—"];
const MODEL_UNRECORDED = /未派|未记录|未报|unknown/i;

function checkModel(file, ln, d) {
  const m = d.model;
  if (m === null || m === "" || m === undefined) {
    warns.push(`${file}#${ln}: WARN model 为空（须 provider/model 精确写法）`);
    return;
  }
  if (typeof m !== "string") { fails.push(`${file}#${ln}: model 非字符串`); return; }
  if (MODEL_SPECIAL.includes(m)) return;
  if (MODEL_UNRECORDED.test(m)) { warns.push(`${file}#${ln}: WARN unrecorded model="${m}"`); return; }
  if (!MODEL_PROVIDERS.some((p) => m.startsWith(p))) {
    warns.push(`${file}#${ln}: WARN bad model="${m}" (须 provider/model 精确写法，见白名单)`);
    return;
  }
  // 前缀合法：再核是否在已知模型全串内
  if (!KNOWN_MODELS.includes(m)) {
    warns.push(`${file}#${ln}: WARN unknown model="${m}" (前缀合法但不在已知表，请核 USER_MODEL_OVERRIDE)`);
  }
}

const CHAIN_STATUS = ["DELIVERED", "ACCEPTED", "OPEN"];
const ROLES = ["task-manager", "supervisor", "planner", "builder", "code-reviewer", "qa",
  "product-reviewer", "experience-recorder", "neat-freak", "senior-expert", "db-admin",
  "迁移整理工", "orchestrator"];

function check(file, need, enums, evidence) {
  const p = join(dir, file);
  if (!existsSync(p)) { fails.push(`${file}: MISSING`); return; }
  const lines = readFileSync(p, "utf8").split("\n").filter((l) => l.trim());
  let n = 0;
  for (const [i, l] of lines.entries()) {
    let d;
    try { d = JSON.parse(l); } catch { fails.push(`${file}#${i + 1}: BAD_JSON`); continue; }
    if (d === null || typeof d !== "object" || Array.isArray(d)) { fails.push(`${file}#${i + 1}: NOT_OBJECT`); continue; }
    if (d._example === true) continue;
    n++;
    const ln = i + 1;
    for (const k of need) if (!(k in d)) fails.push(`${file}#${ln}: missing ${k}`);
    for (const [k, vs] of Object.entries(enums)) {
      if (k in d && d[k] !== null && !vs.includes(d[k])) fails.push(`${file}#${ln}: bad ${k}=${d[k]}`);
    }
    checkModel(file, ln, d);
    if ("executed_by" in d && d.executed_by !== null && !ROLES.includes(d.executed_by)) {
      fails.push(`${file}#${ln}: bad executed_by=${d.executed_by}`);
    }
    if ("chain_status" in d && d.chain_status !== null && !CHAIN_STATUS.includes(d.chain_status)) {
      fails.push(`${file}#${ln}: bad chain_status=${d.chain_status}`);
    }
    // 证据质量（仅 TASK 账；启发式、advisory）：result=PASS 但备注含未闭环字样且未标 OPEN → WARN
    if (evidence && d.result === "PASS" && typeof d.note === "string"
        && /待评审|待复验|待验证|未验证|产品阻塞|待用户验收|未闭环|未完成/.test(d.note)
        && d.chain_status !== "OPEN") {
      warns.push(`${file}#${ln}: WARN result=PASS 但备注含未闭环字样，建议 chain_status=OPEN`);
    }
  }
  const nex = lines.filter((l) => { try { return JSON.parse(l)._example === true; } catch { return false; } }).length;
  if (n === 0 && nex > 0) {
    const m = `${file}: _example 行未删（首个真实任务前删除示例行）`;
    if (ALLOW_EXAMPLE) warns.push(`[--allow-example] ${m}`); else fails.push(m);
  } else if (n === 0) warns.push(`${file}: 空账本（首个真实任务/派工前正常）`);
  if (n > 0 && nex > 0) {
    const m = `${file}: ${nex} _example rows mixed with ${n} real rows (示例行必须在首个真实任务前删除)`;
    if (ALLOW_EXAMPLE) warns.push(`[--allow-example] ${m}`); else fails.push(m);
  }
}
check("TASK-MODEL-LOG.jsonl",
  ["task", "project", "date", "role", "model", "result", "rework", "escalated", "escalation_reason", "tokens", "cost_cny"],
  { result: ["PASS", "FAIL"], escalated: ["YES", "NO"] }, true);
check("DISPATCH-LOG.jsonl",
  ["date", "task", "role", "model", "used", "runtime", "result"],
  { result: ["PASS", "FAIL"] });

// ── APP 基础能力声明检查（2026-10-08）──────────────────────────────
// 背景：APP 主题/多语言默认要求只写在 PRODUCT_PLAN 模板里，Phase1 收工时无人拦，
// 要到 Design Pipeline 才 BLOCKED（拦得住但白干一轮文档）。这里把校验前移到收工检查。
// 只报 FAIL/WARN，不新增流程与 Gate。规则见 docs/sop/app-theme-i18n.md。
function readIfExists(p) { try { return existsSync(p) ? readFileSync(p, "utf8") : ""; } catch { return ""; } }

// HANDOFF 指名的需求真源（PLAN_VERSION 行里指向的 Plan 文件路径）
function trueSource(root) {
  const h = readIfExists(join(root, "docs/handoff/HANDOFF.md"));
  const m = h.match(/PLAN_VERSION[^\n]*?`([^`]+\.md)`/);
  return m ? m[1] : null;
}

// APP 基础能力硬门（2026-10-08 B1 升级：从 WARN 升 FAIL）
// 前一版三处空转：①模板自带六关键字→原样也命中；②只 WARN 而迁移提示词写「WARN 视为通过」；
// ③只 grep 声明段、从不校验这六类是否真进了「关键 AC 集合」。现改为：
//   · 六类必须出现在**标了 关键：是 的 AC 条目**里（匹配范围限定到关键 AC 行，不再 grep 整篇）；
//   · 命中不到 ⇒ FAIL（真拦，不是提醒）；非 APP 项目与历史版本不受影响。
const APP_AC = [
  ["主题三态", /LIGHT/], ["SYSTEM 默认", /SYSTEM/], ["中英可用", /zh-CN/],
  ["不支持语言回退 zh-CN", /回退|fallback/], ["设置持久化", /持久|persist/i],
  ["切换不丢状态", /不丢|保持|preserv/i],
];

// 抽出「关键：是」的 AC 条目行（关键 AC 集合），匹配只在这里面做
function criticalAcLines(text) {
  const out = [];
  for (const ln of text.split("\n")) {
    if (!/AC-\d+/.test(ln)) continue;
    if (/关键\s*[:：]\s*是|\*\*关键：是\*\*/.test(ln)) out.push(ln);
  }
  return out;
}

function checkAppBaseline(root) {
  const pmDir = join(root, "docs/pm");
  const src = trueSource(root);
  const cands = [];
  if (existsSync(pmDir)) {
    let files = [];
    try { files = readdirSync(pmDir).filter((f) => f.endsWith(".md") && !f.includes("template")); } catch { files = []; }
    for (const f of files) {
      if (!/^PRODUCT[_-]?PLAN/i.test(f) && !/Product\s*Plan/i.test(f)) continue;
      cands.push({ label: `docs/pm/${f}`, isSrc: src ? `docs/pm/${f}`.includes(basename(src)) : files.length === 1 });
    }
  }
  if (src && !cands.some((c) => c.label.includes(basename(src)))) {
    const direct = readIfExists(join(root, src));
    // 只认**产品计划类**文档：技术规格（specs/…spec.md 等）不是 Product Plan，不得当计划判
    if (direct && /product[\s_-]?plan|产品\s*plan|需求/i.test(src)) {
      cands.push({ label: src, isSrc: true });
    }
  }
  for (const c of cands) {
    const text = readIfExists(join(root, c.label));
    if (!text) continue;
    const probe = text.replace(/(无|不含|不做|不涉及|非|不适用)[^\n]{0,12}?(Android|iOS|Flutter|小程序|移动端|APP|客户端)/gi, "");
    const strong = /(Android|iOS|Flutter|React\s*Native|小程序|移动端|APP\s*(项目|应用|端)|面向用户交付|客户端\s*App|APK|IPA)/i.test(probe);
    const basis = /(LIGHT|DARK|SYSTEM|深色|浅色|主题切换|zh-CN|多语言|国际化|本地化)/.test(probe);
    if (!strong && !basis) continue;
    if (!/(APP\s*基础能力声明|app[-_ ]baseline)/.test(text)) {
      const msg = `缺「APP 基础能力声明」（主题三态/语言集/系统跟随/回退/持久化；见 docs/sop/app-theme-i18n.md）`;
      if (c.isSrc) fails.push(`${c.label}: APP-BASELINE-MISSING — ${msg}。不进 Design Pipeline，补齐后再收工。`);
      else warns.push(`${c.label}: WARN 非当前真源的 APP Product Plan ${msg}（历史版本可不补；若将启用则须先补）`);
      continue;
    }
    // 关键 AC 集合必须非空且覆盖六类
    const crit = criticalAcLines(text);
    if (!crit.length) {
      if (c.isSrc) fails.push(`${c.label}: APP-CRITICAL-AC-EMPTY — 已声明 APP 基线，但关键 AC 集合为空或无一条标「关键：是」；`
        + `六类（主题三态／SYSTEM 默认／中英可用／回退 zh-CN／持久化／切换不丢状态）必须进关键 AC 集合，不得进 Human Review。`);
      continue;
    }
    const pool = crit.join("\n");
    const missing = APP_AC.filter(([, re]) => !re.test(pool)).map(([n]) => n);
    if (missing.length) {
      if (c.isSrc) fails.push(`${c.label}: APP-CRITICAL-AC-INCOMPLETE — 关键 AC 集合未覆盖：${missing.join("、")}。`
        + `这六类必须各自有一条标「关键：是」的 AC；补不齐不得进 Human Review。`);
      else warns.push(`${c.label}: WARN 非当前真源的 APP Plan 关键 AC 未覆盖：${missing.join("、")}`);
    }
  }
}

const projectRoot = basename(dir) === "model" ? join(dir, "..", "..") : dir;
if (existsSync(dir)) checkAppBaseline(projectRoot);

for (const w of warns) console.log(w);
if (fails.length) { console.log(fails.join("\n")); process.exit(1); }
console.log(warns.length ? "LEDGER-OK (含 WARN)" : "LEDGER-OK");
