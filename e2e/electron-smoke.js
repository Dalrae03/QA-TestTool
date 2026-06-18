const assert = require("node:assert/strict");
const { spawn } = require("node:child_process");
const path = require("node:path");
const { _electron: electron } = require("playwright");

const mockPort = 18080;
const mockBaseUrl = `http://127.0.0.1:${mockPort}`;

function startMockApi() {
  const mock = spawn(process.execPath, [path.join(__dirname, "mock-api.js")], {
    env: { ...process.env, MOCK_API_PORT: String(mockPort) },
    stdio: ["ignore", "pipe", "pipe"]
  });

  return new Promise((resolve, reject) => {
    let settled = false;
    const fail = (error) => {
      if (settled) return;
      settled = true;
      reject(error);
    };

    mock.once("error", fail);
    mock.once("exit", (code) => {
      if (!settled) fail(new Error(`Mock API exited before startup. code=${code}`));
    });
    mock.stderr.on("data", (chunk) => process.stderr.write(`[mock-api] ${chunk}`));
    mock.stdout.on("data", (chunk) => {
      const output = chunk.toString();
      process.stdout.write(`[mock-api] ${output}`);
      if (!settled && output.includes("Mock API listening")) {
        settled = true;
        resolve(mock);
      }
    });
  });
}

async function waitForRow(page, title) {
  await page.waitForFunction(
    (expectedTitle) => Array.from(document.querySelectorAll("#testCaseList .tc-ttl"))
      .some((node) => node.textContent === expectedTitle),
    title
  );
}

async function main() {
  const runId = Date.now();
  const uniqueTitle = `E2E testcase ${runId}`;
  const updatedTitle = `${uniqueTitle} updated`;
  const disposableTitle = `E2E disposable ${runId}`;
  const uniqueTag = `E2E_TAG_${runId}`;
  const uniqueFolder = `E2E Folder ${runId}`;
  const uniquePlan = `E2E Plan ${runId}`;
  const uniqueSuite = `E2E Suite ${runId}`;
  const uniqueServerEnvironment = `E2E Staging ${runId}`;
  const uniqueConfiguration = `E2E Config ${runId}`;
  const pageErrors = [];
  const mock = await startMockApi();
  let app;

  try {
    app = await electron.launch({
      args: ["."],
      env: { ...process.env, ELECTRON_ENABLE_LOGGING: "1" }
    });

    const page = await app.firstWindow();
    page.on("console", (message) => console.log(`[renderer:${message.type()}] ${message.text()}`));
    page.on("pageerror", (error) => {
      pageErrors.push(error.message);
      console.error("[renderer:error]", error);
    });

    await page.waitForLoadState("domcontentloaded");
    await page.evaluate((apiBaseUrl) => {
      localStorage.clear();
      localStorage.setItem("tms.apiBaseUrl", apiBaseUrl);
    }, mockBaseUrl);
    await page.reload();
    await page.waitForLoadState("domcontentloaded");
    await page.waitForFunction(() => document.querySelector("#mainSearchCount")?.textContent === "0개");

    await page.click("#newButton");
    await page.waitForSelector("#detailEditor", { state: "visible" });

    await page.fill("#newTagInput", uniqueTag);
    await page.click("#createTagButton");
    await page.waitForFunction(
      (tagName) => Array.from(document.querySelectorAll("#selectedTagChips .tag-chip-item"))
        .some((node) => node.textContent.includes(tagName)),
      uniqueTag
    );

    await page.click('#statusSelector .status-btn[data-value="READY"]');
    await page.selectOption("#priority", "HIGH");
    await page.fill("#title", uniqueTitle);
    await page.fill("#description", "E2E description");
    await page.fill("#precondition", "E2E precondition");
    await page.fill(".step-row .step-input", "Open the app");
    await page.fill("#notes", "E2E note");
    await page.selectOption("#envOs", "MAC");
    await page.selectOption("#envBrowser", "CHROME");
    await page.selectOption("#envDevice", "DESKTOP");
    await page.fill("#newServerEnvName", uniqueServerEnvironment);
    await page.selectOption("#newServerEnvType", "STAGING");
    await page.fill("#newServerEnvUrl", `https://staging-${runId}.example.com`);
    await page.click("#createServerEnvButton");
    await page.waitForFunction(
      (name) => document.querySelector("#envServer option:checked")?.textContent.includes(name),
      uniqueServerEnvironment
    );

    await page.click("#navSet");
    await page.click("#newConfigurationButton");
    await page.fill("#configurationName", uniqueConfiguration);
    const configurationServerValue = await page.locator("#configurationServer option", { hasText: uniqueServerEnvironment }).getAttribute("value");
    await page.selectOption("#configurationServer", configurationServerValue);
    await page.selectOption("#configurationOs", "MAC");
    await page.selectOption("#configurationBrowser", "CHROME");
    await page.selectOption("#configurationDevice", "DESKTOP");
    await page.click('#configurationForm button[type="submit"]');
    await page.waitForFunction(
      (name) => Array.from(document.querySelectorAll("#configurationList .plan-item-name")).some((node) => node.textContent === name),
      uniqueConfiguration
    );

    await page.click("#navTC");
    await page.selectOption("#testConfiguration", { label: uniqueConfiguration });
    assert.equal(await page.locator("#envOs").inputValue(), "MAC");
    assert.equal(await page.locator("#envBrowser").inputValue(), "CHROME");
    assert.equal(await page.locator("#envDevice").inputValue(), "DESKTOP");
    await page.click('#testCaseForm button[type="submit"]');

    await waitForRow(page, uniqueTitle);
    await page.waitForFunction(() => !document.querySelector("#saveRunButton")?.disabled);
    assert.match(await page.locator("#dtCaseId").textContent(), /^TC-\d+$/);
    assert.equal(await page.locator("#tcStatus").inputValue(), "READY");
    assert.match(await page.locator("#envServer option:checked").textContent(), new RegExp(uniqueServerEnvironment));
    assert.equal(await page.locator("#testConfiguration option:checked").textContent(), uniqueConfiguration);

    await page.fill("#title", updatedTitle);
    await page.fill("#description", "E2E updated description");
    await page.click('#testCaseForm button[type="submit"]');
    await waitForRow(page, updatedTitle);
    assert.equal(await page.locator("#title").inputValue(), updatedTitle);
    assert.equal(await page.locator("#description").inputValue(), "E2E updated description");

    await page.click("#tctab-list");
    await page.fill("#filterKeyword", updatedTitle);
    await page.click("#filterToggleBtn");
    await page.click('#statusFilterPills .filter-pill[data-value="READY"]');
    await page.click('#osFilterPills .filter-pill[data-value="MAC"]');
    assert.deepEqual(await page.locator("#testCaseList .tc-ttl").allTextContents(), [updatedTitle]);

    await page.click(".sort-btn");
    await page.getByText("제목 A→Z", { exact: true }).click();
    assert.deepEqual(await page.locator("#testCaseList .tc-ttl").allTextContents(), [updatedTitle]);

    await page.click(".folder-add-btn");
    await page.fill(".folder-name-input", uniqueFolder);
    await page.press(".folder-name-input", "Enter");
    await page.fill("#folderSearchInput", uniqueFolder.toLowerCase());
    assert.deepEqual(await page.locator("#folderTree .folder-label").allTextContents(), [uniqueFolder]);
    await page.fill("#folderSearchInput", "");

    await page.click("#tctab-runs");
    await page.fill("#actualResult", "Initial run result");
    await page.fill("#runNotes", "Initial run note");
    await page.click("#saveRunButton");
    await page.waitForSelector("#testRunList .run-item");
    assert.match(await page.locator("#testRunList .run-item").textContent(), /PASS/);

    await page.click("#testRunList .run-info");
    await page.selectOption("#testRunList .ri-status", "FAILED");
    await page.fill("#testRunList .ri-actual", "Updated run result");
    await page.fill("#testRunList .ri-notes", "Updated run note");
    await page.click("#testRunList .ri-save");
    await page.waitForFunction(() => document.querySelector("#testRunList .run-item")?.textContent.includes("Updated run result"));
    assert.match(await page.locator("#testRunList .run-item").textContent(), /FAIL/);

    await page.selectOption("#runStatus", "BLOCKED");
    await page.fill("#actualResult", "Dependency server is unavailable");
    await page.fill("#runNotes", "Blocked by external dependency");
    await page.click("#saveRunButton");
    await page.waitForFunction(() => document.querySelectorAll("#testRunList .run-item").length === 2);
    assert.match(await page.locator("#testRunList").textContent(), /BLOCKED/);
    assert.equal(await page.locator("#rsFail").textContent(), "1");
    assert.equal(await page.locator("#rsBlock").textContent(), "1");

    await page.click("#navPlans");
    await page.click("#newPlanButton");
    await page.fill("#planName", uniquePlan);
    await page.selectOption("#planStatus", "ACTIVE");
    await page.fill("#planDescription", "E2E release plan");
    await page.click('#planForm button[type="submit"]');
    await page.waitForFunction(
      (planName) => Array.from(document.querySelectorAll("#planList .plan-item-name")).some((node) => node.textContent === planName),
      uniquePlan
    );

    await page.click("#newSuiteButton");
    await page.fill("#suiteName", uniqueSuite);
    await page.fill("#suiteDescription", "E2E authentication suite");
    await page.locator("#suiteCasePicker .suite-case-option", { hasText: updatedTitle }).locator('input[type="checkbox"]').check();
    await page.click('#suiteForm button[type="submit"]');
    await page.waitForFunction(
      (suiteName) => Array.from(document.querySelectorAll("#suiteList .plan-item-name")).some((node) => node.textContent === suiteName),
      uniqueSuite
    );
    assert.match(await page.locator("#suiteList .suite-list-item", { hasText: uniqueSuite }).textContent(), /1 test cases/);

    await page.click("#navTC");
    await page.fill("#filterKeyword", "");
    await page.click('#statusFilterPills .filter-pill[data-value=""]');
    await page.click('#osFilterPills .filter-pill[data-value=""]');
    await page.click("#newButton");
    await page.fill("#title", disposableTitle);
    await page.fill("#description", "Delete flow description");
    await page.fill("#precondition", "Delete flow precondition");
    await page.fill(".step-row .step-input", "Create then delete");
    await page.click('#testCaseForm button[type="submit"]');
    await waitForRow(page, disposableTitle);
    page.once("dialog", (dialog) => dialog.accept());
    await page.click("#deleteButton");
    await page.waitForFunction(
      (title) => !Array.from(document.querySelectorAll("#testCaseList .tc-ttl")).some((node) => node.textContent === title),
      disposableTitle
    );

    assert.deepEqual(pageErrors, []);
    console.log(JSON.stringify({
      ok: true,
      createdTestCase: uniqueTitle,
      updatedTestCase: updatedTitle,
      deletedTestCase: disposableTitle,
      createdTag: uniqueTag,
      folderSearch: uniqueFolder,
      runUpdatedTo: "FAIL",
      blockedRunRecorded: true,
      createdPlan: uniquePlan,
      createdSuite: uniqueSuite,
      createdServerEnvironment: uniqueServerEnvironment,
      createdConfiguration: uniqueConfiguration
    }, null, 2));
  } finally {
    if (app) await app.close();
    mock.kill("SIGTERM");
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
