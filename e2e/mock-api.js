const http = require("node:http");

let nextTestCaseId = 1;
let nextTagId = 1;
let nextPlanId = 1;
let nextSuiteId = 1;
let nextServerEnvironmentId = 1;
let nextConfigurationId = 1;
let nextDefectId = 1;
let nextFolderId = 1;
let nextExecutionId = 1;
let nextExecutionItemId = 1;
let nextHistoryId = 1;
let nextUserId = 1;
let nextAuditLogId = 1;
let nextTestCaseVersionId = 1;
const folders = [];
const executions = [];
const testCases = [];
const areaTags = [];
const testPlans = [];
const testSuites = [];
const serverEnvironments = [];
const testConfigurations = [];
const defects = [];
const users = [];
const auditLogs = [];
const testCaseVersions = [];
const testRunsByTestCaseId = new Map();

function json(res, status, body) {
  res.writeHead(status, { "Content-Type": "application/json; charset=utf-8" });
  res.end(body === null ? "" : JSON.stringify(body));
}

function noContent(res) {
  res.writeHead(204);
  res.end();
}

function readBody(req) {
  return new Promise((resolve, reject) => {
    let data = "";
    req.on("data", (chunk) => {
      data += chunk;
    });
    req.on("end", () => {
      if (!data) {
        resolve(null);
        return;
      }
      try {
        resolve(JSON.parse(data));
      } catch (error) {
        reject(error);
      }
    });
    req.on("error", reject);
  });
}

function normalizeTestCase(testCase) {
  return {
    ...testCase,
    serverEnvironment: testCase.serverEnvironmentId
      ? serverEnvironments.find((environment) => environment.id === testCase.serverEnvironmentId) || null
      : null,
    testConfiguration: testCase.testConfigurationId
      ? normalizeConfiguration(testConfigurations.find((configuration) => configuration.id === testCase.testConfigurationId))
      : null,
    areaTags: testCase.areaTags.map((tag) => ({ id: tag.id, name: tag.name }))
  };
}

function recordAudit(entityId, action, fieldName, oldValue, newValue) {
  auditLogs.push({
    id: nextAuditLogId++,
    entityType: "TEST_CASE",
    entityId,
    action,
    fieldName,
    oldValue: oldValue == null ? null : String(oldValue),
    newValue: newValue == null ? null : String(newValue),
    summary: action,
    actor: "system",
    createdAt: new Date().toISOString()
  });
}

function recordAuditIfChanged(entityId, action, fieldName, oldValue, newValue) {
  const oldText = oldValue == null ? null : String(oldValue);
  const newText = newValue == null ? null : String(newValue);
  if (oldText !== newText) recordAudit(entityId, action, fieldName, oldText, newText);
}

function recordVersionSnapshot(testCase, changeSummary) {
  const currentVersions = testCaseVersions.filter((version) => version.testCaseId === testCase.id);
  const versionNumber = currentVersions.length === 0
    ? 1
    : Math.max(...currentVersions.map((version) => version.versionNumber)) + 1;
  const folder = testCase.folderId ? folders.find((item) => item.id === testCase.folderId) : null;
  const serverEnvironment = testCase.serverEnvironmentId
    ? serverEnvironments.find((item) => item.id === testCase.serverEnvironmentId)
    : null;
  const testConfiguration = testCase.testConfigurationId
    ? testConfigurations.find((item) => item.id === testCase.testConfigurationId)
    : null;
  const snapshot = {
    id: nextTestCaseVersionId++,
    testCaseId: testCase.id,
    versionNumber,
    label: testCase.version || `v${versionNumber}`,
    changeSummary,
    type: testCase.type,
    priority: testCase.priority,
    status: testCase.status,
    title: testCase.title,
    version: testCase.version ?? null,
    description: testCase.description,
    precondition: testCase.precondition,
    steps: testCase.steps,
    notes: testCase.notes ?? null,
    os: testCase.os ?? null,
    browser: testCase.browser ?? null,
    device: testCase.device ?? null,
    assignee: testCase.assignee ?? null,
    folderId: testCase.folderId ?? null,
    folderName: folder?.name ?? null,
    serverEnvironmentId: testCase.serverEnvironmentId ?? null,
    serverEnvironmentName: serverEnvironment?.name ?? null,
    testConfigurationId: testCase.testConfigurationId ?? null,
    testConfigurationName: testConfiguration?.name ?? null,
    areaTagIds: testCase.areaTags.map((tag) => tag.id).join(","),
    areaTagNames: testCase.areaTags.map((tag) => tag.name).join(", "),
    createdAt: new Date().toISOString()
  };
  testCaseVersions.push(snapshot);
  recordAudit(testCase.id, "VERSION_CREATED", "version", null, snapshot.label);
  return snapshot;
}

function applyFilters(items, url) {
  const status = url.searchParams.get("status");
  const os = url.searchParams.get("os");
  const type = url.searchParams.get("type");
  const areaTagId = url.searchParams.get("areaTagId");
  const keyword = (url.searchParams.get("keyword") || "").toLowerCase();

  return items.filter((item) => {
    if (status && item.status !== status) return false;
    if (os && item.os !== os) return false;
    if (type && item.type !== type) return false;
    if (areaTagId && !item.areaTags.some((tag) => String(tag.id) === areaTagId)) return false;
    if (keyword) {
      const title = (item.title || "").toLowerCase();
      const description = (item.description || "").toLowerCase();
      if (!title.includes(keyword) && !description.includes(keyword)) return false;
    }
    return true;
  });
}

function resolveAreaTags(ids) {
  if (!Array.isArray(ids) || ids.length === 0) {
    return [];
  }
  return ids
    .map((id) => areaTags.find((tag) => tag.id === id))
    .filter(Boolean);
}

function normalizePlan(plan) {
  const suites = testSuites.filter((suite) => suite.testPlanId === plan.id);
  return {
    ...plan,
    suiteCount: suites.length,
    testCaseCount: new Set(suites.flatMap((suite) => suite.testCaseIds)).size
  };
}

// 컨피그의 서버환경·OS·브라우저·기기·Java·DB 버전을 한 줄 요약으로 — 백엔드 buildEnvironmentDetail()과 같은 규칙.
function configDetailString(config) {
  if (!config) return null;
  const env = config.serverEnvironmentId ? serverEnvironments.find((e) => e.id === config.serverEnvironmentId) : null;
  const parts = [
    env ? env.name : null,
    config.os ? `${config.os}${config.osVersion ? " " + config.osVersion : ""}` : null,
    config.browser ? `${config.browser}${config.browserVersion ? " " + config.browserVersion : ""}` : null,
    config.device || null,
    config.runtimeVersion || null,
    config.dbVersion || null
  ].filter(Boolean);
  return parts.length ? parts.join(" · ") : null;
}

function normalizeConfiguration(configuration) {
  if (!configuration) return null;
  return {
    ...configuration,
    serverEnvironment: configuration.serverEnvironmentId
      ? serverEnvironments.find((environment) => environment.id === configuration.serverEnvironmentId) || null
      : null
  };
}

function normalizeExecution(exec, includeItems) {
  const items = exec.items;
  const total = items.length;
  const countBy = (s) => items.filter((it) => it.status === s).length;
  const untested = countBy("UNTESTED");
  const executed = total - untested;
  return {
    id: exec.id,
    name: exec.name,
    description: exec.description ?? null,
    testPlanId: exec.testPlanId ?? null,
    testSuiteId: exec.testSuiteId ?? null,
    suiteName: exec.suiteName ?? null,
    testConfigurationId: exec.testConfigurationId ?? null,
    configurationName: exec.configurationName ?? null,
    environmentDetail: exec.environmentDetail ?? null,
    status: exec.status,
    assignee: exec.assignee ?? null,
    total,
    untested,
    passed: countBy("PASSED"),
    failed: countBy("FAILED"),
    blocked: countBy("BLOCKED"),
    retest: countBy("RETEST"),
    progressPct: total === 0 ? 0 : Math.round((executed * 100) / total),
    items: includeItems ? items.map((it) => ({ ...it, history: it.history || [] })) : null,
    createdAt: exec.createdAt,
    updatedAt: exec.updatedAt,
    completedAt: exec.completedAt ?? null
  };
}

// 결과 기록 — 재시도 이력을 함께 누적한다. 백엔드 ExecutionItem.record()와 같은 규칙.
function recordItemResult(item, status, comment, failureReason, now) {
  item.history = item.history || [];
  item.status = status;
  item.comment = comment ?? null;
  item.failureReason = failureReason ?? null;
  if (status === "UNTESTED") {
    item.executedAt = null;
    item.history.pop(); // 오클릭 복구 — 직전 시도 되돌림
  } else {
    item.executedAt = now;
    item.history.push({ id: nextHistoryId++, status, comment: comment ?? null, failureReason: failureReason ?? null, recordedAt: now });
  }
}

function normalizeFolder(folder) {
  return {
    id: folder.id,
    name: folder.name,
    parentId: folder.parentId ?? null,
    children: folders.filter((f) => f.parentId === folder.id).map(normalizeFolder),
    createdAt: folder.createdAt,
    updatedAt: folder.updatedAt
  };
}

function normalizeSuite(suite) {
  return {
    id: suite.id,
    testPlanId: suite.testPlanId,
    name: suite.name,
    description: suite.description,
    testCases: suite.testCaseIds.map((id) => testCases.find((testCase) => testCase.id === id)).filter(Boolean).map(normalizeTestCase),
    createdAt: suite.createdAt,
    updatedAt: suite.updatedAt
  };
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url, "http://127.0.0.1:8080");

  if (req.method === "GET" && url.pathname === "/api/area-tags") {
    json(res, 200, areaTags);
    return;
  }

  // 프로젝트 — 목 서버는 단일 테넌트로 동작하므로 빈 목록을 돌려준다(상단 프로젝트 셀렉터는 "프로젝트 없음" 상태가 됨).
  if (req.method === "GET" && url.pathname === "/api/projects") {
    json(res, 200, []);
    return;
  }

  // 테스트케이스 1건의 실행 이력(테스트런을 통해 기록된 결과) — 아직 실행한 적 없으면 빈 배열.
  const tcRunItemsMatch = url.pathname.match(/^\/api\/test-runs\/items\/by-test-case\/(\d+)$/);
  if (req.method === "GET" && tcRunItemsMatch) {
    json(res, 200, []);
    return;
  }

  // 대시보드 통계 — 화면이 깨지지 않을 정도의 최소 형태로 응답한다.
  if (req.method === "GET" && url.pathname === "/api/dashboard/stats") {
    const byStatus = {}, byPriority = {}, byAreaTag = {};
    testCases.forEach((tc) => {
      byStatus[tc.status] = (byStatus[tc.status] || 0) + 1;
      byPriority[tc.priority] = (byPriority[tc.priority] || 0) + 1;
      tc.areaTags.forEach((tag) => { byAreaTag[tag.name] = (byAreaTag[tag.name] || 0) + 1; });
    });
    json(res, 200, {
      totalTestCases: testCases.length,
      testCasesByStatus: byStatus,
      testCasesByPriority: byPriority,
      testCasesByAreaTag: byAreaTag,
      totalExecutions: executions.length,
      activeExecutions: executions.filter((e) => e.status === "IN_PROGRESS").length,
      completedExecutions: executions.filter((e) => e.status === "COMPLETED").length,
      passRate: 0,
      recentExecutions: [],
      totalDefects: defects.length,
      defectsBySeverity: {},
      defectsByStatus: {},
      defectHeatmap: [],
      recentAuditLogs: []
    });
    return;
  }

  if (req.method === "GET" && url.pathname === "/api/users") {
    json(res, 200, [...users].sort((a, b) => Number(b.active) - Number(a.active) || a.name.localeCompare(b.name)));
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/users") {
    const body = await readBody(req);
    const email = body.email ? String(body.email).trim().toLowerCase() : null;
    if (email && users.some((user) => user.email && user.email.toLowerCase() === email)) {
      json(res, 409, { message: `이미 등록된 이메일입니다: ${email}` });
      return;
    }
    const now = new Date().toISOString();
    const user = {
      id: nextUserId++,
      name: String(body.name || "").trim(),
      email,
      role: body.role,
      active: body.active !== false,
      createdAt: now,
      updatedAt: now
    };
    users.push(user);
    json(res, 201, user);
    return;
  }

  if (/^\/api\/users\/\d+$/.test(url.pathname)) {
    const id = Number(url.pathname.split("/").pop());
    const user = users.find((item) => item.id === id);
    if (!user) { json(res, 404, { message: `User not found. id=${id}` }); return; }
    if (req.method === "GET") { json(res, 200, user); return; }
    if (req.method === "PUT") {
      const body = await readBody(req);
      const email = body.email ? String(body.email).trim().toLowerCase() : null;
      if (email && users.some((item) => item.id !== id && item.email && item.email.toLowerCase() === email)) {
        json(res, 409, { message: `이미 등록된 이메일입니다: ${email}` });
        return;
      }
      Object.assign(user, {
        name: String(body.name || "").trim(),
        email,
        role: body.role,
        active: body.active !== false,
        updatedAt: new Date().toISOString()
      });
      json(res, 200, user);
      return;
    }
    if (req.method === "DELETE") {
      user.active = false;
      user.updatedAt = new Date().toISOString();
      noContent(res);
      return;
    }
  }

  if (req.method === "POST" && url.pathname === "/api/area-tags") {
    const body = await readBody(req);
    const normalizedName = String(body?.name || "").trim();
    const existing = areaTags.find((tag) => tag.name.toLowerCase() === normalizedName.toLowerCase());
    if (existing) {
      json(res, 409, { message: `이미 존재하는 태그입니다: ${normalizedName}` });
      return;
    }

    const tag = { id: nextTagId++, name: normalizedName };
    areaTags.push(tag);
    json(res, 201, tag);
    return;
  }

  if (req.method === "DELETE" && url.pathname.startsWith("/api/area-tags/")) {
    const id = Number(url.pathname.split("/").pop());
    const index = areaTags.findIndex((tag) => tag.id === id);
    if (index === -1) {
      json(res, 404, { message: `AreaTag not found. id=${id}` });
      return;
    }
    areaTags.splice(index, 1);
    for (const testCase of testCases) {
      testCase.areaTags = testCase.areaTags.filter((tag) => tag.id !== id);
    }
    noContent(res);
    return;
  }

  if (req.method === "GET" && url.pathname === "/api/testcases") {
    json(res, 200, applyFilters(testCases.map(normalizeTestCase), url));
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/testcases") {
    const body = await readBody(req);
    const testCase = {
      id: nextTestCaseId++,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
      type: body.type,
      priority: body.priority,
      status: body.status || "DRAFT",
      title: body.title,
      description: body.description,
      precondition: body.precondition,
      steps: body.steps,
      notes: body.notes ?? null,
      os: body.os ?? null,
      browser: body.browser ?? null,
      device: body.device ?? null,
      areaTags: resolveAreaTags(body.areaTagIds),
      serverEnvironmentId: body.serverEnvironmentId ?? null,
      testConfigurationId: body.testConfigurationId ?? null,
      assignee: body.assignee ?? null,
      version: body.version ?? null,
      folderId: body.folderId ?? null
    };
    testCases.push(testCase);
    recordAudit(testCase.id, "CREATED", "testCase", null, testCase.title);
    recordVersionSnapshot(testCase, "최초 생성");
    json(res, 201, normalizeTestCase(testCase));
    return;
  }

  const auditMatch = url.pathname.match(/^\/api\/testcases\/(\d+)\/audit-logs$/);
  if (req.method === "GET" && auditMatch) {
    const testCaseId = Number(auditMatch[1]);
    json(res, 200, auditLogs
      .filter((log) => log.entityType === "TEST_CASE" && log.entityId === testCaseId)
      .sort((a, b) => b.id - a.id));
    return;
  }

  const versionMatch = url.pathname.match(/^\/api\/testcases\/(\d+)\/versions$/);
  if (req.method === "GET" && versionMatch) {
    const testCaseId = Number(versionMatch[1]);
    json(res, 200, testCaseVersions
      .filter((version) => version.testCaseId === testCaseId)
      .sort((a, b) => b.versionNumber - a.versionNumber));
    return;
  }

  const restoreVersionMatch = url.pathname.match(/^\/api\/testcases\/(\d+)\/versions\/(\d+)\/restore$/);
  if (req.method === "POST" && restoreVersionMatch) {
    const testCaseId = Number(restoreVersionMatch[1]);
    const versionId = Number(restoreVersionMatch[2]);
    const testCase = testCases.find((item) => item.id === testCaseId);
    if (!testCase) { json(res, 404, { message: `TestCase not found. id=${testCaseId}` }); return; }
    const snapshot = testCaseVersions.find((version) => version.id === versionId && version.testCaseId === testCaseId);
    if (!snapshot) { json(res, 404, { message: `TestCaseVersion not found. id=${versionId}` }); return; }
    const beforeTitle = testCase.title;
    Object.assign(testCase, {
      updatedAt: new Date().toISOString(),
      type: snapshot.type,
      priority: snapshot.priority,
      status: snapshot.status,
      title: snapshot.title,
      version: snapshot.version ?? null,
      description: snapshot.description,
      precondition: snapshot.precondition,
      steps: snapshot.steps,
      notes: snapshot.notes ?? null,
      os: snapshot.os ?? null,
      browser: snapshot.browser ?? null,
      device: snapshot.device ?? null,
      assignee: snapshot.assignee ?? null,
      folderId: snapshot.folderId ?? null,
      serverEnvironmentId: snapshot.serverEnvironmentId ?? null,
      testConfigurationId: snapshot.testConfigurationId ?? null,
      areaTags: snapshot.areaTagIds
        ? snapshot.areaTagIds.split(",").map((id) => areaTags.find((tag) => tag.id === Number(id))).filter(Boolean)
        : []
    });
    recordAuditIfChanged(testCaseId, "UPDATED", "title", beforeTitle, testCase.title);
    recordAudit(testCaseId, "VERSION_RESTORED", "version", null, snapshot.label);
    recordVersionSnapshot(testCase, `버전 복원: ${snapshot.label}`);
    json(res, 200, normalizeTestCase(testCase));
    return;
  }

  if (req.method === "GET" && /^\/api\/testcases\/\d+$/.test(url.pathname)) {
    const id = Number(url.pathname.split("/").pop());
    const testCase = testCases.find((item) => item.id === id);
    if (!testCase) {
      json(res, 404, { message: `TestCase not found. id=${id}` });
      return;
    }
    json(res, 200, normalizeTestCase(testCase));
    return;
  }

  if (req.method === "PUT" && /^\/api\/testcases\/\d+$/.test(url.pathname)) {
    const id = Number(url.pathname.split("/").pop());
    const testCase = testCases.find((item) => item.id === id);
    if (!testCase) {
      json(res, 404, { message: `TestCase not found. id=${id}` });
      return;
    }
    const body = await readBody(req);
    const before = { ...testCase, areaTags: [...testCase.areaTags] };
    Object.assign(testCase, {
      updatedAt: new Date().toISOString(),
      type: body.type,
      priority: body.priority,
      status: body.status,
      title: body.title,
      description: body.description,
      precondition: body.precondition,
      steps: body.steps,
      notes: body.notes ?? null,
      os: body.os ?? null,
      browser: body.browser ?? null,
      device: body.device ?? null,
      areaTags: resolveAreaTags(body.areaTagIds),
      serverEnvironmentId: body.serverEnvironmentId ?? null,
      testConfigurationId: body.testConfigurationId ?? null,
      assignee: body.assignee ?? null,
      version: body.version ?? null,
      folderId: body.folderId ?? null
    });
    recordAuditIfChanged(id, "UPDATED", "title", before.title, testCase.title);
    recordAuditIfChanged(id, "UPDATED", "priority", before.priority, testCase.priority);
    recordAuditIfChanged(id, "STATUS_CHANGED", "status", before.status, testCase.status);
    recordAuditIfChanged(id, "UPDATED", "description", before.description, testCase.description);
    recordAuditIfChanged(id, "UPDATED", "assignee", before.assignee, testCase.assignee);
    recordAuditIfChanged(id, "UPDATED", "version", before.version, testCase.version);
    recordAuditIfChanged(id, "MOVED", "folder", before.folderId, testCase.folderId);
    recordVersionSnapshot(testCase, "테스트케이스 수정");
    json(res, 200, normalizeTestCase(testCase));
    return;
  }

  if (req.method === "DELETE" && /^\/api\/testcases\/\d+$/.test(url.pathname)) {
    const id = Number(url.pathname.split("/").pop());
    const index = testCases.findIndex((item) => item.id === id);
    if (index === -1) {
      json(res, 404, { message: `TestCase not found. id=${id}` });
      return;
    }
    recordAudit(id, "DELETED", "testCase", testCases[index].title, null);
    testCases.splice(index, 1);
    for (let i = testCaseVersions.length - 1; i >= 0; i -= 1) {
      if (testCaseVersions[i].testCaseId === id) testCaseVersions.splice(i, 1);
    }
    testRunsByTestCaseId.delete(id);
    testSuites.forEach((suite) => { suite.testCaseIds = suite.testCaseIds.filter((caseId) => caseId !== id); });
    noContent(res);
    return;
  }

  if (req.method === "PATCH" && /^\/api\/testcases\/\d+\/status$/.test(url.pathname)) {
    const [, , , id] = url.pathname.split("/");
    const testCase = testCases.find((item) => item.id === Number(id));
    if (!testCase) {
      json(res, 404, { message: `TestCase not found. id=${id}` });
      return;
    }
    const body = await readBody(req);
    const before = testCase.status;
    testCase.status = body.status;
    recordAuditIfChanged(testCase.id, "STATUS_CHANGED", "status", before, testCase.status);
    recordVersionSnapshot(testCase, "상태 변경");
    json(res, 200, normalizeTestCase(testCase));
    return;
  }

  if (req.method === "GET" && /^\/api\/testcases\/\d+\/runs$/.test(url.pathname)) {
    const [, , , id] = url.pathname.split("/");
    json(res, 200, testRunsByTestCaseId.get(Number(id)) || []);
    return;
  }

  if (req.method === "POST" && /^\/api\/testcases\/\d+\/runs$/.test(url.pathname)) {
    const [, , , id] = url.pathname.split("/");
    const testCaseId = Number(id);
    const body = await readBody(req);
    const runs = testRunsByTestCaseId.get(testCaseId) || [];
    const run = {
      id: runs.length + 1,
      testCaseId,
      status: body.status,
      actualResult: body.actualResult,
      notes: body.notes ?? null,
      executedAt: new Date().toISOString()
    };
    runs.unshift(run);
    testRunsByTestCaseId.set(testCaseId, runs);
    json(res, 201, run);
    return;
  }

  if (req.method === "PUT" && /^\/api\/testcases\/\d+\/runs\/\d+$/.test(url.pathname)) {
    const [, , , testCaseIdPart, , runIdPart] = url.pathname.split("/");
    const testCaseId = Number(testCaseIdPart);
    const runId = Number(runIdPart);
    const runs = testRunsByTestCaseId.get(testCaseId) || [];
    const run = runs.find((item) => item.id === runId);
    if (!run) {
      json(res, 404, { message: `TestRun not found. id=${runId}` });
      return;
    }
    const body = await readBody(req);
    Object.assign(run, {
      status: body.status,
      actualResult: body.actualResult,
      notes: body.notes ?? null
    });
    json(res, 200, run);
    return;
  }

  if (req.method === "DELETE" && /^\/api\/testcases\/\d+\/runs\/\d+$/.test(url.pathname)) {
    const [, , , testCaseIdPart, , runIdPart] = url.pathname.split("/");
    const testCaseId = Number(testCaseIdPart);
    const runId = Number(runIdPart);
    const runs = testRunsByTestCaseId.get(testCaseId) || [];
    testRunsByTestCaseId.set(testCaseId, runs.filter((run) => run.id !== runId));
    noContent(res);
    return;
  }

  if (req.method === "GET" && url.pathname === "/api/test-plans") {
    json(res, 200, testPlans.map(normalizePlan));
    return;
  }

  if (req.method === "GET" && url.pathname === "/api/server-environments") {
    json(res, 200, [...serverEnvironments].sort((a, b) => a.name.localeCompare(b.name)));
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/server-environments") {
    const body = await readBody(req);
    const existing = serverEnvironments.find((environment) => environment.name.toLowerCase() === String(body.name).toLowerCase());
    if (existing) { json(res, 409, { message: `이미 존재하는 서버 환경입니다: ${body.name}` }); return; }
    const now = new Date().toISOString();
    const environment = { id: nextServerEnvironmentId++, ...body, createdAt: now, updatedAt: now };
    serverEnvironments.push(environment);
    json(res, 201, environment);
    return;
  }

  const serverEnvMatch = url.pathname.match(/^\/api\/server-environments\/(\d+)$/);
  if (serverEnvMatch) {
    const envId = Number(serverEnvMatch[1]);
    const environment = serverEnvironments.find((item) => item.id === envId);
    if (!environment) { json(res, 404, { message: `ServerEnvironment not found. id=${envId}` }); return; }
    if (req.method === "GET") { json(res, 200, environment); return; }
    if (req.method === "PUT") {
      const body = await readBody(req);
      Object.assign(environment, body, { updatedAt: new Date().toISOString() });
      json(res, 200, environment);
      return;
    }
    if (req.method === "DELETE") {
      serverEnvironments.splice(serverEnvironments.indexOf(environment), 1);
      noContent(res);
      return;
    }
  }

  // 결함 (Defect) — 최소 구현
  if (req.method === "GET" && url.pathname === "/api/defects") {
    json(res, 200, defects);
    return;
  }
  if (req.method === "POST" && url.pathname === "/api/defects") {
    const body = await readBody(req);
    const now = new Date().toISOString();
    const defect = { id: nextDefectId++, ...body, status: body.status || "OPEN", jiraKey: null, createdAt: now, updatedAt: now };
    defects.push(defect);
    json(res, 201, defect);
    return;
  }
  const defectMatch = url.pathname.match(/^\/api\/defects\/(\d+)$/);
  if (defectMatch) {
    const defectId = Number(defectMatch[1]);
    const defect = defects.find((item) => item.id === defectId);
    if (!defect) { json(res, 404, { message: `Defect not found. id=${defectId}` }); return; }
    if (req.method === "GET") { json(res, 200, defect); return; }
    if (req.method === "PUT") {
      const body = await readBody(req);
      Object.assign(defect, body, { updatedAt: new Date().toISOString() });
      json(res, 200, defect);
      return;
    }
    if (req.method === "DELETE") {
      defects.splice(defects.indexOf(defect), 1);
      noContent(res);
      return;
    }
  }

  // 첨부파일 목록 — 빈 배열 (업로드는 네이티브 다이얼로그 필요)
  if (req.method === "GET" && /\/attachments$/.test(url.pathname)) {
    json(res, 200, []);
    return;
  }

  if (/^\/api\/server-environments\/\d+$/.test(url.pathname)) {
    const id = Number(url.pathname.split("/").pop());
    const environment = serverEnvironments.find((item) => item.id === id);
    if (!environment) { json(res, 404, { message: `ServerEnvironment not found. id=${id}` }); return; }
    if (req.method === "GET") { json(res, 200, environment); return; }
    if (req.method === "PUT") {
      Object.assign(environment, await readBody(req), { updatedAt: new Date().toISOString() });
      json(res, 200, environment);
      return;
    }
    if (req.method === "DELETE") {
      serverEnvironments.splice(serverEnvironments.indexOf(environment), 1);
      testCases.forEach((testCase) => {
        if (testCase.serverEnvironmentId === id) testCase.serverEnvironmentId = null;
      });
      testConfigurations.forEach((configuration) => {
        if (configuration.serverEnvironmentId === id) configuration.serverEnvironmentId = null;
      });
      noContent(res);
      return;
    }
  }

  if (req.method === "GET" && url.pathname === "/api/test-configurations") {
    json(res, 200, [...testConfigurations].sort((a, b) => a.name.localeCompare(b.name)).map(normalizeConfiguration));
    return;
  }

  if (req.method === "POST" && url.pathname === "/api/test-configurations") {
    const body = await readBody(req);
    const existing = testConfigurations.find((configuration) => configuration.name.toLowerCase() === String(body.name).toLowerCase());
    if (existing) { json(res, 409, { message: `이미 존재하는 configuration입니다: ${body.name}` }); return; }
    const now = new Date().toISOString();
    const configuration = { id: nextConfigurationId++, ...body, createdAt: now, updatedAt: now };
    testConfigurations.push(configuration);
    json(res, 201, normalizeConfiguration(configuration));
    return;
  }

  if (/^\/api\/test-configurations\/\d+$/.test(url.pathname)) {
    const id = Number(url.pathname.split("/").pop());
    const configuration = testConfigurations.find((item) => item.id === id);
    if (!configuration) { json(res, 404, { message: `TestConfiguration not found. id=${id}` }); return; }
    if (req.method === "GET") { json(res, 200, normalizeConfiguration(configuration)); return; }
    if (req.method === "PUT") {
      Object.assign(configuration, await readBody(req), { updatedAt: new Date().toISOString() });
      json(res, 200, normalizeConfiguration(configuration));
      return;
    }
    if (req.method === "DELETE") {
      testConfigurations.splice(testConfigurations.indexOf(configuration), 1);
      testCases.forEach((testCase) => {
        if (testCase.testConfigurationId === id) testCase.testConfigurationId = null;
      });
      noContent(res);
      return;
    }
  }

  if (req.method === "POST" && url.pathname === "/api/test-plans") {
    const body = await readBody(req);
    const now = new Date().toISOString();
    const plan = { id: nextPlanId++, ...body, createdAt: now, updatedAt: now };
    testPlans.unshift(plan);
    json(res, 201, normalizePlan(plan));
    return;
  }

  if (/^\/api\/test-plans\/\d+$/.test(url.pathname)) {
    const planId = Number(url.pathname.split("/").pop());
    const plan = testPlans.find((item) => item.id === planId);
    if (!plan) { json(res, 404, { message: `TestPlan not found. id=${planId}` }); return; }
    if (req.method === "GET") { json(res, 200, normalizePlan(plan)); return; }
    if (req.method === "PUT") {
      Object.assign(plan, await readBody(req), { updatedAt: new Date().toISOString() });
      json(res, 200, normalizePlan(plan));
      return;
    }
    if (req.method === "DELETE") {
      testPlans.splice(testPlans.indexOf(plan), 1);
      for (let index = testSuites.length - 1; index >= 0; index--) {
        if (testSuites[index].testPlanId === planId) testSuites.splice(index, 1);
      }
      noContent(res);
      return;
    }
  }

  // 독립 스위트 — 테스트런에서 직접 생성/관리
  if (req.method === "GET" && url.pathname === "/api/suites") {
    json(res, 200, testSuites.map(normalizeSuite));
    return;
  }
  if (req.method === "POST" && url.pathname === "/api/suites") {
    const body = await readBody(req);
    const now = new Date().toISOString();
    const suite = { id: nextSuiteId++, testPlanId: body.testPlanId ?? null, name: body.name, description: body.description ?? null, testCaseIds: [...new Set(body.testCaseIds || [])], createdAt: now, updatedAt: now };
    testSuites.push(suite);
    json(res, 201, normalizeSuite(suite));
    return;
  }
  const standaloneSuiteMatch = url.pathname.match(/^\/api\/suites\/(\d+)$/);
  if (standaloneSuiteMatch) {
    const suite = testSuites.find((item) => item.id === Number(standaloneSuiteMatch[1]));
    if (!suite) { json(res, 404, { message: `TestSuite not found. id=${standaloneSuiteMatch[1]}` }); return; }
    if (req.method === "GET") { json(res, 200, normalizeSuite(suite)); return; }
    if (req.method === "PUT") {
      const body = await readBody(req);
      Object.assign(suite, { name: body.name, description: body.description ?? null, testCaseIds: [...new Set(body.testCaseIds || [])], updatedAt: new Date().toISOString() });
      json(res, 200, normalizeSuite(suite));
      return;
    }
    if (req.method === "DELETE") {
      testSuites.splice(testSuites.indexOf(suite), 1);
      noContent(res);
      return;
    }
  }

  const suitesMatch = url.pathname.match(/^\/api\/test-plans\/(\d+)\/suites$/);
  if (suitesMatch) {
    const planId = Number(suitesMatch[1]);
    if (req.method === "GET") {
      json(res, 200, testSuites.filter((suite) => suite.testPlanId === planId).map(normalizeSuite));
      return;
    }
    if (req.method === "POST") {
      const body = await readBody(req);
      const now = new Date().toISOString();
      const suite = { id: nextSuiteId++, testPlanId: planId, name: body.name, description: body.description ?? null, testCaseIds: [...new Set(body.testCaseIds || [])], createdAt: now, updatedAt: now };
      testSuites.push(suite);
      json(res, 201, normalizeSuite(suite));
      return;
    }
  }

  const suiteMatch = url.pathname.match(/^\/api\/test-plans\/(\d+)\/suites\/(\d+)$/);
  if (suiteMatch) {
    const planId = Number(suiteMatch[1]);
    const suiteId = Number(suiteMatch[2]);
    const suite = testSuites.find((item) => item.id === suiteId && item.testPlanId === planId);
    if (!suite) { json(res, 404, { message: `TestSuite not found. id=${suiteId}` }); return; }
    if (req.method === "GET") { json(res, 200, normalizeSuite(suite)); return; }
    if (req.method === "PUT") {
      const body = await readBody(req);
      Object.assign(suite, { name: body.name, description: body.description ?? null, testCaseIds: [...new Set(body.testCaseIds || [])], updatedAt: new Date().toISOString() });
      json(res, 200, normalizeSuite(suite));
      return;
    }
    if (req.method === "DELETE") {
      testSuites.splice(testSuites.indexOf(suite), 1);
      noContent(res);
      return;
    }
  }

  if (url.pathname === "/api/folders") {
    if (req.method === "GET") {
      json(res, 200, folders.filter((f) => f.parentId == null).map(normalizeFolder));
      return;
    }
    if (req.method === "POST") {
      const body = await readBody(req);
      const now = new Date().toISOString();
      const folder = { id: nextFolderId++, name: body.name, parentId: body.parentId ?? null, createdAt: now, updatedAt: now };
      folders.push(folder);
      json(res, 201, normalizeFolder(folder));
      return;
    }
  }

  const folderMatch = url.pathname.match(/^\/api\/folders\/(\d+)$/);
  if (folderMatch) {
    const folderId = Number(folderMatch[1]);
    const folder = folders.find((f) => f.id === folderId);
    if (!folder) { json(res, 404, { message: `TestFolder not found. id=${folderId}` }); return; }
    if (req.method === "GET") { json(res, 200, normalizeFolder(folder)); return; }
    if (req.method === "PUT") {
      const body = await readBody(req);
      Object.assign(folder, { name: body.name, parentId: body.parentId ?? null, updatedAt: new Date().toISOString() });
      json(res, 200, normalizeFolder(folder));
      return;
    }
    if (req.method === "DELETE") {
      folders.splice(folders.indexOf(folder), 1);
      for (const testCase of testCases) if (testCase.folderId === folderId) testCase.folderId = null;
      noContent(res);
      return;
    }
  }

  const tcFolderMatch = url.pathname.match(/^\/api\/testcases\/(\d+)\/folder$/);
  if (tcFolderMatch && req.method === "PATCH") {
    const testCase = testCases.find((item) => item.id === Number(tcFolderMatch[1]));
    if (!testCase) { json(res, 404, { message: `TestCase not found. id=${tcFolderMatch[1]}` }); return; }
    const body = await readBody(req);
    const before = testCase.folderId ?? null;
    testCase.folderId = body.folderId ?? null;
    recordAuditIfChanged(testCase.id, "MOVED", "folder", before, testCase.folderId);
    recordVersionSnapshot(testCase, "폴더 변경");
    json(res, 200, normalizeTestCase(testCase));
    return;
  }

  if (url.pathname === "/api/test-runs") {
    if (req.method === "GET") {
      const assignee = url.searchParams.get("assignee");
      const filtered = assignee ? executions.filter((e) => e.assignee === assignee) : executions;
      json(res, 200, [...filtered].sort((a, b) => b.id - a.id).map((exec) => normalizeExecution(exec, false)));
      return;
    }
    if (req.method === "POST") {
      const body = await readBody(req);
      const suite = testSuites.find((s) => s.id === Number(body.suiteId));
      if (!suite) { json(res, 404, { message: `TestSuite not found. id=${body.suiteId}` }); return; }
      if (!suite.testCaseIds || suite.testCaseIds.length === 0) {
        json(res, 400, { message: "테스트케이스가 없는 스위트로는 테스트런을 만들 수 없습니다." });
        return;
      }
      const now = new Date().toISOString();
      const config = body.testConfigurationId ? testConfigurations.find((c) => c.id === Number(body.testConfigurationId)) : null;
      const exec = {
        id: nextExecutionId++,
        name: body.name && body.name.trim() ? body.name.trim() : `${suite.name} — ${now.slice(0, 10)}`,
        description: body.description ?? null,
        testPlanId: suite.testPlanId,
        testSuiteId: suite.id,
        suiteName: suite.name,
        testConfigurationId: config ? config.id : null,
        configurationName: config ? config.name : null,
        environmentDetail: config ? configDetailString(config) : null,
        status: "IN_PROGRESS",
        assignee: body.assignee ?? null,
        items: suite.testCaseIds.map((tcId) => {
          const tc = testCases.find((c) => c.id === tcId);
          return { id: nextExecutionItemId++, testCaseId: tcId, caseTitle: tc ? tc.title : `TC-${tcId}`, status: "UNTESTED", comment: null, executedAt: null };
        }),
        createdAt: now,
        updatedAt: now,
        completedAt: null
      };
      executions.push(exec);
      json(res, 201, normalizeExecution(exec, true));
      return;
    }
  }

  const execItemMatch = url.pathname.match(/^\/api\/test-runs\/(\d+)\/items\/(\d+)$/);
  if (execItemMatch && req.method === "PATCH") {
    const exec = executions.find((e) => e.id === Number(execItemMatch[1]));
    if (!exec) { json(res, 404, { message: `TestRun not found. id=${execItemMatch[1]}` }); return; }
    if (exec.status === "COMPLETED") {
      json(res, 400, { message: "완료된 테스트런은 다시 열기 전까지 수정할 수 없습니다." });
      return;
    }
    const item = exec.items.find((it) => it.id === Number(execItemMatch[2]));
    if (!item) { json(res, 404, { message: `ExecutionItem not found. id=${execItemMatch[2]}` }); return; }
    const body = await readBody(req);
    const now = new Date().toISOString();
    recordItemResult(item, body.status, body.comment, body.failureReason, now);
    exec.updatedAt = now;
    json(res, 200, normalizeExecution(exec, true));
    return;
  }

  const execBulkMatch = url.pathname.match(/^\/api\/test-runs\/(\d+)\/bulk-results$/);
  if (execBulkMatch && req.method === "PATCH") {
    const exec = executions.find((e) => e.id === Number(execBulkMatch[1]));
    if (!exec) { json(res, 404, { message: `TestRun not found. id=${execBulkMatch[1]}` }); return; }
    if (exec.status === "COMPLETED") {
      json(res, 400, { message: "완료된 테스트런은 다시 열기 전까지 수정할 수 없습니다." });
      return;
    }
    const body = await readBody(req);
    const ids = [...new Set(body.itemIds || [])];
    const missing = ids.filter((id) => !exec.items.some((it) => it.id === id));
    if (missing.length > 0) { json(res, 400, { message: `이 테스트런에 속하지 않은 항목이 포함되어 있습니다: ${missing}` }); return; }
    const now = new Date().toISOString();
    exec.items.forEach((it) => {
      if (ids.includes(it.id)) {
        recordItemResult(it, body.status, body.comment, body.failureReason, now);
      }
    });
    exec.updatedAt = now;
    json(res, 200, normalizeExecution(exec, true));
    return;
  }

  const execEnvMatch = url.pathname.match(/^\/api\/test-runs\/(\d+)\/environment$/);
  if (execEnvMatch && req.method === "PATCH") {
    const exec = executions.find((e) => e.id === Number(execEnvMatch[1]));
    if (!exec) { json(res, 404, { message: `TestRun not found. id=${execEnvMatch[1]}` }); return; }
    const body = await readBody(req);
    if (body.testConfigurationId == null) {
      exec.testConfigurationId = null;
      exec.configurationName = null;
      exec.environmentDetail = null;
    } else {
      const config = testConfigurations.find((c) => c.id === Number(body.testConfigurationId));
      if (!config) { json(res, 404, { message: `TestConfiguration not found. id=${body.testConfigurationId}` }); return; }
      exec.testConfigurationId = config.id;
      exec.configurationName = config.name;
      exec.environmentDetail = configDetailString(config);
    }
    exec.updatedAt = new Date().toISOString();
    json(res, 200, normalizeExecution(exec, true));
    return;
  }

  const execCloneMatch = url.pathname.match(/^\/api\/test-runs\/(\d+)\/clone$/);
  if (execCloneMatch && req.method === "POST") {
    const source = executions.find((e) => e.id === Number(execCloneMatch[1]));
    if (!source) { json(res, 404, { message: `TestRun not found. id=${execCloneMatch[1]}` }); return; }
    const now = new Date().toISOString();
    const clone = {
      id: nextExecutionId++,
      name: `${source.name} (복제)`.slice(0, 200),
      description: source.description ?? null,
      testPlanId: source.testPlanId,
      testSuiteId: source.testSuiteId,
      suiteName: source.suiteName,
      testConfigurationId: source.testConfigurationId ?? null,
      configurationName: source.configurationName ?? null,
      environmentDetail: source.environmentDetail ?? null,
      status: "IN_PROGRESS",
      assignee: source.assignee ?? null,
      // 회귀용 — 원본 케이스를 현재 최신 버전으로 재스냅샷하고, 그 사이 삭제된 케이스는 건너뛴다.
      items: source.items.flatMap((it) => {
        const tc = testCases.find((c) => c.id === it.testCaseId);
        if (!tc) return [];
        const latest = testCaseVersions
          .filter((v) => v.testCaseId === tc.id)
          .sort((a, b) => b.versionNumber - a.versionNumber)[0] ?? null;
        return [{
          id: nextExecutionItemId++, testCaseId: tc.id, caseTitle: tc.title,
          versionNumber: latest ? latest.versionNumber : null, versionLabel: latest ? latest.label : null,
          status: "UNTESTED", comment: null, executedAt: null
        }];
      }),
      createdAt: now,
      updatedAt: now,
      completedAt: null
    };
    executions.push(clone);
    json(res, 201, normalizeExecution(clone, true));
    return;
  }

  const execMatch = url.pathname.match(/^\/api\/test-runs\/(\d+)$/);
  if (execMatch) {
    const exec = executions.find((e) => e.id === Number(execMatch[1]));
    if (!exec) { json(res, 404, { message: `TestRun not found. id=${execMatch[1]}` }); return; }
    if (req.method === "GET") { json(res, 200, normalizeExecution(exec, true)); return; }
    if (req.method === "PUT") {
      const body = await readBody(req);
      Object.assign(exec, { name: body.name, description: body.description ?? null, status: body.status, assignee: body.assignee ?? null, updatedAt: new Date().toISOString() });
      exec.completedAt = body.status === "COMPLETED" ? (exec.completedAt || new Date().toISOString()) : null;
      json(res, 200, normalizeExecution(exec, true));
      return;
    }
    if (req.method === "DELETE") {
      executions.splice(executions.indexOf(exec), 1);
      noContent(res);
      return;
    }
  }

  json(res, 404, { message: "Not found" });
});

const port = Number(process.env.MOCK_API_PORT || 8080);
server.listen(port, "127.0.0.1", () => {
  console.log(`Mock API listening on http://127.0.0.1:${port}`);
});
