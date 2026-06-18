const http = require("node:http");

let nextTestCaseId = 1;
let nextTagId = 1;
let nextPlanId = 1;
let nextSuiteId = 1;
let nextServerEnvironmentId = 1;
let nextConfigurationId = 1;
let nextDefectId = 1;
const testCases = [];
const areaTags = [];
const testPlans = [];
const testSuites = [];
const serverEnvironments = [];
const testConfigurations = [];
const defects = [];
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

function normalizeConfiguration(configuration) {
  if (!configuration) return null;
  return {
    ...configuration,
    serverEnvironment: configuration.serverEnvironmentId
      ? serverEnvironments.find((environment) => environment.id === configuration.serverEnvironmentId) || null
      : null
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
      testConfigurationId: body.testConfigurationId ?? null
    };
    testCases.push(testCase);
    json(res, 201, normalizeTestCase(testCase));
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
      testConfigurationId: body.testConfigurationId ?? null
    });
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
    testCases.splice(index, 1);
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
    testCase.status = body.status;
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

  json(res, 404, { message: "Not found" });
});

const port = Number(process.env.MOCK_API_PORT || 8080);
server.listen(port, "127.0.0.1", () => {
  console.log(`Mock API listening on http://127.0.0.1:${port}`);
});
