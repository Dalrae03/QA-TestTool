const http = require("node:http");

let nextTestCaseId = 1;
let nextTagId = 1;
const testCases = [];
const areaTags = [];
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
      type: body.type,
      priority: body.priority,
      status: body.status || "DRAFT",
      title: body.title,
      description: body.description,
      precondition: body.precondition,
      steps: body.steps,
      expected: body.expected,
      notes: body.notes ?? null,
      os: body.os ?? null,
      browser: body.browser ?? null,
      device: body.device ?? null,
      areaTags: resolveAreaTags(body.areaTagIds)
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
      type: body.type,
      priority: body.priority,
      status: body.status,
      title: body.title,
      description: body.description,
      precondition: body.precondition,
      steps: body.steps,
      expected: body.expected,
      notes: body.notes ?? null,
      os: body.os ?? null,
      browser: body.browser ?? null,
      device: body.device ?? null,
      areaTags: resolveAreaTags(body.areaTagIds)
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

  json(res, 404, { message: "Not found" });
});

server.listen(8080, "127.0.0.1", () => {
  console.log("Mock API listening on http://127.0.0.1:8080");
});
