const assert = require("node:assert/strict");
const { _electron: electron } = require("playwright");

async function waitForText(page, selector, expected) {
  await page.waitForFunction(
    ({ selector: targetSelector, expectedText }) => {
      const node = document.querySelector(targetSelector);
      return node && node.textContent && node.textContent.includes(expectedText);
    },
    { selector, expectedText: expected }
  );
}

async function main() {
  const runId = Date.now();
  const uniqueTitle = `E2E testcase ${runId}`;
  const uniqueTag = `E2E_TAG_${runId}`;

  const app = await electron.launch({
    args: ["."],
    env: {
      ...process.env,
      ELECTRON_ENABLE_LOGGING: "1"
    }
  });

  try {
    const page = await app.firstWindow();
    page.on("console", (message) => {
      console.log(`[renderer:${message.type()}] ${message.text()}`);
    });
    page.on("pageerror", (error) => {
      console.error("[renderer:error]", error);
    });

    await page.waitForLoadState("domcontentloaded");

    await page.evaluate(() => localStorage.clear());
    await page.reload();
    await page.waitForLoadState("domcontentloaded");

    await page.waitForSelector("#title");
    await page.waitForSelector("#saveCurrentAsTemplateButton");
    await page.click("#connectButton");
    await waitForText(page, "#listState", "연결 성공");

    await page.fill("#newTagInput", uniqueTag);
    await page.click("#createTagButton");
    await page.waitForFunction(
      (tagName) => Array.from(document.querySelectorAll("#selectedTagChips .tag-chip")).some((node) => node.textContent.includes(tagName)),
      uniqueTag
    );

    await page.click('#statusSelector .status-btn[data-value="READY"]');
    await page.selectOption("#priority", "HIGH");
    await page.fill("#title", uniqueTitle);
    await page.fill("#description", "E2E description");
    await page.fill("#precondition", "E2E precondition");
    await page.fill(".step-row .step-input", "Open the app");
    await page.fill(".step-row .step-expected-input", "App is visible");
    await page.fill("#notes", "E2E note");
    await page.selectOption("#envOs", "MAC");
    await page.selectOption("#envBrowser", "CHROME");
    await page.selectOption("#envDevice", "DESKTOP");

    await page.click("#saveCurrentAsTemplateButton");
    await waitForText(page, "#templateStatus", "저장했습니다");

    await page.click('#testCaseForm button[type="submit"]');
    await page.waitForFunction(
      (title) => Array.from(document.querySelectorAll("#testCaseList .case-item")).some((node) => node.textContent.includes(title)),
      uniqueTitle
    );
    await page.waitForFunction(
      () => {
        const titleInput = document.querySelector("#title");
        const saveRunButton = document.querySelector("#saveRunButton");
        return titleInput?.value && !saveRunButton?.disabled;
      }
    );

    await page.fill("#filterKeyword", uniqueTitle);
    await page.click('#statusFilterPills .filter-pill[data-value="READY"]');
    await page.click('#osFilterPills .filter-pill[data-value="MAC"]');
    await page.waitForFunction(
      (title) => {
        const items = Array.from(document.querySelectorAll("#testCaseList .case-item"));
        return items.length === 1 && items[0].textContent.includes(title);
      },
      uniqueTitle
    );

    await page.click("#newButton");
    await page.selectOption("#templateSelect", { label: uniqueTitle });
    await page.click("#applyTemplateButton");

    await page.waitForFunction(
      ({ title, tag }) => {
        const titleInput = document.querySelector("#title");
        const osSelect = document.querySelector("#envOs");
        const browserSelect = document.querySelector("#envBrowser");
        const deviceSelect = document.querySelector("#envDevice");
        const status = document.querySelector("#tcStatus");
        const chips = Array.from(document.querySelectorAll("#selectedTagChips .tag-chip")).map((node) => node.textContent || "");

        return titleInput?.value === title
          && osSelect?.value === "MAC"
          && browserSelect?.value === "CHROME"
          && deviceSelect?.value === "DESKTOP"
          && status?.value === "READY"
          && chips.some((text) => text.includes(tag));
      },
      { title: uniqueTitle, tag: uniqueTag }
    );

    const snapshot = await page.evaluate(() => ({
      title: document.querySelector("#title")?.value,
      status: document.querySelector("#tcStatus")?.value,
      os: document.querySelector("#envOs")?.value,
      browser: document.querySelector("#envBrowser")?.value,
      device: document.querySelector("#envDevice")?.value,
      tags: Array.from(document.querySelectorAll("#selectedTagChips .tag-chip")).map((node) => node.textContent)
    }));

    assert.equal(snapshot.title, uniqueTitle);
    assert.equal(snapshot.status, "READY");
    assert.equal(snapshot.os, "MAC");
    assert.equal(snapshot.browser, "CHROME");
    assert.equal(snapshot.device, "DESKTOP");
    assert(snapshot.tags.some((text) => text.includes(uniqueTag)));

    console.log(JSON.stringify({
      ok: true,
      createdTestCase: uniqueTitle,
      createdTag: uniqueTag,
      templateRestored: snapshot
    }, null, 2));
  } finally {
    await app.close();
  }
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
