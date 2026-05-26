const state = {
  apiBaseUrl: "http://localhost:8080",
  selectedId: null,
  testCases: [],
  testRuns: [],
  templates: []
};

const TEMPLATE_STORAGE_KEY = "tms.testCaseTemplates";

const elements = {
  apiBaseUrl: document.getElementById("apiBaseUrl"),
  connectButton: document.getElementById("connectButton"),
  refreshButton: document.getElementById("refreshButton"),
  newButton: document.getElementById("newButton"),
  duplicateButton: document.getElementById("duplicateButton"),
  deleteButton: document.getElementById("deleteButton"),
  resetButton: document.getElementById("resetButton"),
  listState: document.getElementById("listState"),
  list: document.getElementById("testCaseList"),
  templateName: document.getElementById("templateName"),
  templateSelect: document.getElementById("templateSelect"),
  saveTemplateButton: document.getElementById("saveTemplateButton"),
  applyTemplateButton: document.getElementById("applyTemplateButton"),
  deleteTemplateButton: document.getElementById("deleteTemplateButton"),
  templateStatus: document.getElementById("templateStatus"),
  editorPanel: document.getElementById("editorPanel"),
  flowHint: document.getElementById("flowHint"),
  form: document.getElementById("testCaseForm"),
  saveCurrentAsTemplateButton: document.getElementById("saveCurrentAsTemplateButton"),
  testRunForm: document.getElementById("testRunForm"),
  runPanel: document.getElementById("runPanel"),
  runState: document.getElementById("runState"),
  runMode: document.getElementById("runMode"),
  saveRunButton: document.getElementById("saveRunButton"),
  testRunList: document.getElementById("testRunList"),
  editorTitle: document.getElementById("editorTitle"),
  formMode: document.getElementById("formMode"),
  platformPill: document.getElementById("platform-pill"),
  versionPill: document.getElementById("version-pill"),
  testCaseId: document.getElementById("testCaseId"),
  type: document.getElementById("type"),
  priority: document.getElementById("priority"),
  title: document.getElementById("title"),
  description: document.getElementById("description"),
  precondition: document.getElementById("precondition"),
  stepsList: document.getElementById("stepsList"),
  addStepButton: document.getElementById("addStepButton"),
  notes: document.getElementById("notes"),
  runStatus: document.getElementById("runStatus"),
  actualResult: document.getElementById("actualResult"),
  runNotes: document.getElementById("runNotes")
};

const requiredFieldConfigs = [
  { element: elements.type, label: "Type", getValue: () => elements.type.value },
  { element: elements.priority, label: "Priority", getValue: () => elements.priority.value },
  { element: elements.title, label: "Title", getValue: () => elements.title.value.trim() },
  { element: elements.description, label: "Description", getValue: () => elements.description.value.trim() },
  { element: elements.precondition, label: "Precondition", getValue: () => elements.precondition.value.trim() }
];

function getApiBaseUrl() {
  return elements.apiBaseUrl.value.trim().replace(/\/$/, "");
}

function updateStatus(message) {
  elements.listState.textContent = message;
}

function updateTemplateStatus(message) {
  elements.templateStatus.textContent = message;
}

function updateRunStatus(message) {
  elements.runState.textContent = message;
}

function setFlowStage(stage, hint) {
  elements.editorPanel.dataset.flowStage = stage;
  elements.flowHint.textContent = hint;
  elements.runPanel.classList.toggle("run-panel-disabled", stage === "draft");
}

function setSelected(id) {
  state.selectedId = id;
  elements.deleteButton.disabled = id === null;
  elements.duplicateButton.disabled = id === null;
}

function resetForm() {
  elements.form.reset();
  elements.testRunForm.reset();
  elements.testCaseId.value = "";
  elements.type.value = "FUNCTIONAL";
  elements.priority.value = "MEDIUM";
  elements.runStatus.value = "PASSED";
  renderSteps();
  clearValidationErrors();
  setSelected(null);
  state.testRuns = [];
  elements.editorTitle.textContent = "테스트케이스 작성";
  elements.formMode.textContent = "새 테스트케이스를 저장할 준비가 됐습니다.";
  updateTemplateStatus("반복되는 테스트케이스를 템플릿으로 저장해 빠르게 작성할 수 있습니다.");
  elements.saveRunButton.disabled = true;
  elements.runMode.textContent = "실행 결과를 저장하면 해당 테스트케이스의 이력에 추가됩니다.";
  updateRunStatus("저장된 테스트케이스를 선택하면 실행 결과를 기록할 수 있습니다.");
  setFlowStage("draft", "초안을 작성한 뒤 저장하면 바로 실행 결과를 기록할 수 있습니다.");
  renderTestRuns();
  renderList();
  elements.title.focus();
}

async function populateForm(testCase) {
  elements.testCaseId.value = String(testCase.id);
  elements.type.value = testCase.type;
  elements.priority.value = testCase.priority ?? "MEDIUM";
  elements.title.value = testCase.title;
  elements.description.value = testCase.description;
  elements.precondition.value = testCase.precondition;
  renderSteps(parseStepPairs(testCase.steps, testCase.expected));
  elements.notes.value = testCase.notes ?? "";
  setSelected(testCase.id);
  elements.editorTitle.textContent = `테스트케이스 수정 #${testCase.id}`;
  elements.formMode.textContent = "수정 후 저장하면 PUT 요청이 전송됩니다.";
  elements.saveRunButton.disabled = false;
  elements.runMode.textContent = "실행 결과를 저장하면 해당 테스트케이스의 이력에 추가됩니다.";
  setFlowStage("saved", "저장된 테스트케이스입니다. 수정하거나 바로 실행 결과를 기록할 수 있습니다.");
  renderList();
  await loadTestRuns(testCase.id);
}

function duplicateCurrentTestCase() {
  const id = elements.testCaseId.value;
  if (!id) {
    return;
  }

  const testCase = state.testCases.find((item) => String(item.id) === id);
  if (!testCase) {
    updateStatus("복제할 테스트케이스를 찾을 수 없습니다.");
    return;
  }

  elements.testCaseId.value = "";
  elements.type.value = testCase.type;
  elements.priority.value = testCase.priority ?? "MEDIUM";
  elements.title.value = `${testCase.title} 사본`;
  elements.description.value = testCase.description;
  elements.precondition.value = testCase.precondition;
  renderSteps(parseStepPairs(testCase.steps, testCase.expected));
  elements.notes.value = testCase.notes ?? "";
  elements.testRunForm.reset();
  elements.runStatus.value = "PASSED";
  setSelected(null);
  elements.saveRunButton.disabled = true;
  state.testRuns = [];
  elements.editorTitle.textContent = "테스트케이스 복제 작성";
  elements.formMode.textContent = "기존 테스트케이스를 복제했습니다. 필요한 부분만 수정 후 저장하세요.";
  updateStatus("복제본을 만들었습니다. 제목이나 단계만 바꿔서 저장할 수 있습니다.");
  updateRunStatus("복제본은 아직 저장되지 않았습니다. 저장 후 실행 결과를 기록할 수 있습니다.");
  setFlowStage("draft", "복제본 초안입니다. 필요한 부분만 수정한 뒤 저장하세요.");
  renderTestRuns();
  elements.title.focus();
  elements.title.select();
  renderList();
}

function buildTemplatePayload() {
  return {
    type: elements.type.value,
    priority: elements.priority.value,
    title: elements.title.value.trim(),
    description: elements.description.value.trim(),
    precondition: elements.precondition.value.trim(),
    steps: getStepsValue(),
    expected: getExpectedValue(),
    notes: elements.notes.value.trim() || null
  };
}

function loadTemplates() {
  try {
    const raw = localStorage.getItem(TEMPLATE_STORAGE_KEY);
    state.templates = raw ? JSON.parse(raw) : [];
  } catch (_error) {
    state.templates = [];
  }
}

function persistTemplates() {
  localStorage.setItem(TEMPLATE_STORAGE_KEY, JSON.stringify(state.templates));
}

function renderTemplateOptions() {
  const selectedValue = elements.templateSelect.value;
  elements.templateSelect.innerHTML = '<option value="">템플릿을 선택하세요</option>';

  for (const template of state.templates) {
    const option = document.createElement("option");
    option.value = template.id;
    option.textContent = template.name;
    elements.templateSelect.appendChild(option);
  }

  const stillExists = state.templates.some((template) => template.id === selectedValue);
  elements.templateSelect.value = stillExists ? selectedValue : "";
}

function saveTemplate() {
  if (!validateRequiredFields()) {
    updateTemplateStatus("템플릿 저장 전 필수 항목을 모두 입력해야 합니다.");
    return;
  }

  const trimmedName = elements.templateName.value.trim() || elements.title.value.trim() || "새 템플릿";
  if (trimmedName === "") {
    updateTemplateStatus("템플릿 이름은 비워둘 수 없습니다.");
    return;
  }

  const template = {
    id: crypto.randomUUID(),
    name: trimmedName,
    payload: buildTemplatePayload()
  };

  state.templates.unshift(template);
  persistTemplates();
  renderTemplateOptions();
  elements.templateSelect.value = template.id;
  elements.templateName.value = "";
  updateTemplateStatus(`템플릿 "${trimmedName}"을 저장했습니다.`);
}

function applyTemplate() {
  const templateId = elements.templateSelect.value;
  if (!templateId) {
    updateTemplateStatus("불러올 템플릿을 먼저 선택하세요.");
    return;
  }

  const template = state.templates.find((item) => item.id === templateId);
  if (!template) {
    updateTemplateStatus("선택한 템플릿을 찾을 수 없습니다.");
    return;
  }

  const { payload } = template;
  elements.testCaseId.value = "";
  elements.type.value = payload.type;
  elements.priority.value = payload.priority ?? "MEDIUM";
  elements.title.value = payload.title;
  elements.description.value = payload.description;
  elements.precondition.value = payload.precondition;
  renderSteps(parseStepPairs(payload.steps, payload.expected));
  elements.notes.value = payload.notes ?? "";
  elements.testRunForm.reset();
  elements.runStatus.value = "PASSED";
  clearValidationErrors();
  setSelected(null);
  elements.saveRunButton.disabled = true;
  state.testRuns = [];
  elements.editorTitle.textContent = "템플릿으로 테스트케이스 작성";
  elements.formMode.textContent = `템플릿 "${template.name}"을 불러왔습니다. 필요한 부분만 수정 후 저장하세요.`;
  updateTemplateStatus(`템플릿 "${template.name}"을 불러왔습니다.`);
  elements.templateName.value = template.name;
  updateRunStatus("템플릿으로 만든 초안은 저장 후 실행 결과를 기록할 수 있습니다.");
  setFlowStage("draft", `템플릿 "${template.name}"으로 초안을 채웠습니다. 수정 후 저장하세요.`);
  renderTestRuns();
  renderList();
}

function deleteTemplate() {
  const templateId = elements.templateSelect.value;
  if (!templateId) {
    updateTemplateStatus("삭제할 템플릿을 먼저 선택하세요.");
    return;
  }

  const template = state.templates.find((item) => item.id === templateId);
  if (!template) {
    updateTemplateStatus("선택한 템플릿을 찾을 수 없습니다.");
    return;
  }

  const shouldDelete = window.confirm(`템플릿 "${template.name}"을 삭제할까요?`);
  if (!shouldDelete) {
    return;
  }

  state.templates = state.templates.filter((item) => item.id !== templateId);
  persistTemplates();
  renderTemplateOptions();
  elements.templateName.value = "";
  updateTemplateStatus(`템플릿 "${template.name}"을 삭제했습니다.`);
}

function createListItem(testCase) {
  const item = document.createElement("li");
  item.className = "case-item";
  if (testCase.id === state.selectedId) {
    item.classList.add("active");
  }

  const priority = testCase.priority ?? "MEDIUM";
  const stepPairs = parseStepPairs(testCase.steps, testCase.expected);
  const stepSummary = stepPairs.length > 0
    ? stepPairs.map(({ step }, index) => `${index + 1}. ${step}`).join(" ")
    : testCase.description;
  const expectedSummary = stepPairs.length > 0
    ? stepPairs.map(({ expected }, index) => `${index + 1}. ${expected}`).join(" ")
    : testCase.expected;

  item.innerHTML = `
    <div class="case-item-header">
      <div class="case-title">${escapeHtml(testCase.title)}</div>
      <div class="case-badges">
        <span class="case-type">${escapeHtml(testCase.type)}</span>
        <span class="case-priority priority-${escapeHtml(priority.toLowerCase())}">${escapeHtml(priority)}</span>
      </div>
    </div>
    <div class="case-summary-list">
      <div class="case-summary-row">
        <span class="case-summary-label">제목</span>
        <p class="case-summary-text">${escapeHtml(testCase.title)}</p>
      </div>
      <div class="case-summary-row">
        <span class="case-summary-label">테스트케이스 내용</span>
        <p class="case-summary-text">${escapeHtml(stepSummary)}</p>
      </div>
      <div class="case-summary-row">
        <span class="case-summary-label">결과</span>
        <p class="case-summary-text">${escapeHtml(expectedSummary)}</p>
      </div>
    </div>
  `;

  item.addEventListener("click", async () => {
    await populateForm(testCase);
  });
  return item;
}

function renderList() {
  elements.list.innerHTML = "";
  if (state.testCases.length === 0) {
    updateStatus("저장된 테스트케이스가 없습니다.");
    return;
  }

  updateStatus(`총 ${state.testCases.length}개의 테스트케이스가 있습니다.`);
  for (const testCase of state.testCases) {
    elements.list.appendChild(createListItem(testCase));
  }
}

function renderTestRuns() {
  elements.testRunList.innerHTML = "";
  if (state.testRuns.length === 0) {
    return;
  }

  for (const testRun of state.testRuns) {
    const item = document.createElement("li");
    item.className = "run-item";
    item.innerHTML = `
      <div class="run-item-header">
        <div class="run-item-meta">
          <span class="run-status run-status-${escapeHtml(testRun.status.toLowerCase())}">${escapeHtml(testRun.status)}</span>
          <span class="run-date">${escapeHtml(formatDateTime(testRun.executedAt))}</span>
        </div>
        <div class="run-item-actions">
          <button type="button" class="ghost-button run-delete-button" data-run-id="${escapeHtml(String(testRun.id))}">삭제</button>
        </div>
      </div>
      <div class="run-item-section">
        <span class="run-item-label">Actual Result</span>
        <p class="run-item-text">${escapeHtml(testRun.actualResult)}</p>
      </div>
      ${testRun.notes ? `
      <div class="run-item-section">
        <span class="run-item-label">Notes</span>
        <p class="run-item-text">${escapeHtml(testRun.notes)}</p>
      </div>
      ` : ""}
    `;
    item.querySelector(".run-delete-button").addEventListener("click", async () => {
      await deleteTestRun(testRun.id);
    });
    elements.testRunList.appendChild(item);
  }
}

async function loadTestRuns(testCaseId) {
  updateRunStatus("실행 이력을 불러오는 중입니다.");
  try {
    state.testRuns = await request(`/api/testcases/${testCaseId}/runs`, { method: "GET" });
    renderTestRuns();
    updateRunStatus(
      state.testRuns.length === 0
        ? "아직 실행 이력이 없습니다. 첫 실행 결과를 저장해보세요."
        : `총 ${state.testRuns.length}개의 실행 이력이 있습니다.`
    );
  } catch (error) {
    state.testRuns = [];
    renderTestRuns();
    updateRunStatus(`실행 이력 조회 실패: ${error.message}`);
  }
}

async function request(path, options = {}) {
  const response = await window.desktopApi.request({
    url: `${state.apiBaseUrl}${path}`,
    method: options.method ?? "GET",
    headers: {
      "Content-Type": "application/json",
      ...(options.headers ?? {})
    },
    body: options.body ?? null
  });

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = response.data;
      message = body.message ?? message;
    } catch (_error) {
      // ignore invalid JSON
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  return response.data;
}

async function loadTestCases() {
  state.apiBaseUrl = getApiBaseUrl() || state.apiBaseUrl;
  updateStatus("테스트케이스를 불러오는 중입니다.");
  try {
    state.testCases = await request("/api/testcases", { method: "GET" });
    renderList();
  } catch (error) {
    state.testCases = [];
    renderList();
    updateStatus(`연결 실패: ${error.message}`);
  }
}

function getPayload() {
  return {
    type: elements.type.value,
    priority: elements.priority.value,
    title: elements.title.value.trim(),
    description: elements.description.value.trim(),
    precondition: elements.precondition.value.trim(),
    steps: getStepsValue(),
    expected: getExpectedValue(),
    notes: elements.notes.value.trim() || null
  };
}

function createStepRow(stepValue = "", expectedValue = "") {
  const row = document.createElement("div");
  row.className = "step-row";

  const index = document.createElement("span");
  index.className = "step-index";

  const fields = document.createElement("div");
  fields.className = "step-fields";

  const stepField = document.createElement("label");
  stepField.className = "step-field";

  const stepLabel = document.createElement("span");
  stepLabel.className = "step-field-label";
  stepLabel.textContent = "단계";

  const input = document.createElement("textarea");
  input.className = "step-input";
  input.placeholder = "단계를 입력하세요.";
  input.value = stepValue;
  input.addEventListener("input", () => clearValidationErrors());

  const expectedField = document.createElement("label");
  expectedField.className = "step-field";

  const expectedLabel = document.createElement("span");
  expectedLabel.className = "step-field-label";
  expectedLabel.textContent = "기대 결과";

  const expectedInput = document.createElement("textarea");
  expectedInput.className = "step-expected-input";
  expectedInput.placeholder = "이 단계의 기대 결과를 입력하세요.";
  expectedInput.value = expectedValue;
  expectedInput.addEventListener("input", () => clearValidationErrors());

  stepField.append(stepLabel, input);
  expectedField.append(expectedLabel, expectedInput);
  fields.append(stepField, expectedField);

  const removeButton = document.createElement("button");
  removeButton.type = "button";
  removeButton.className = "ghost-button step-remove-button";
  removeButton.textContent = "삭제";
  removeButton.addEventListener("click", () => {
    row.remove();
    if (elements.stepsList.children.length === 0) {
      appendStepRow();
    }
    renumberSteps();
    clearValidationErrors();
  });

  row.append(index, fields, removeButton);
  return row;
}

function appendStepRow(stepValue = "", expectedValue = "") {
  elements.stepsList.appendChild(createStepRow(stepValue, expectedValue));
  renumberSteps();
}

function renderSteps(stepPairs = [{ step: "", expected: "" }]) {
  elements.stepsList.innerHTML = "";
  for (const { step, expected } of stepPairs) {
    appendStepRow(step, expected);
  }
}

function renumberSteps() {
  const rows = elements.stepsList.querySelectorAll(".step-row");
  rows.forEach((row, index) => {
    row.querySelector(".step-index").textContent = `Step ${index + 1}`;
  });
}

function getStepsValue() {
  return getStepPairs()
    .map(({ step }) => step)
    .join("\n");
}

function getExpectedValue() {
  return getStepPairs()
    .map(({ expected }) => expected)
    .join("\n");
}

function getStepPairs() {
  return Array.from(elements.stepsList.querySelectorAll(".step-row"))
    .map((row) => ({
      step: row.querySelector(".step-input").value.trim(),
      expected: row.querySelector(".step-expected-input").value.trim()
    }))
    .filter(({ step, expected }) => step !== "" && expected !== "");
}

function parseStepPairs(stepsValue, expectedValue) {
  const steps = String(stepsValue)
    .split("\n")
    .map((step) => step.trim())
    .filter((step) => step !== "");

  const expecteds = String(expectedValue)
    .split("\n")
    .map((expected) => expected.trim())
    .filter((expected) => expected !== "");

  const rowCount = Math.max(steps.length, expecteds.length, 1);
  const pairs = [];
  for (let index = 0; index < rowCount; index += 1) {
    pairs.push({
      step: steps[index] ?? "",
      expected: expecteds[index] ?? ""
    });
  }

  return pairs;
}

function validateStepPairs() {
  const stepRows = Array.from(elements.stepsList.querySelectorAll(".step-row"));
  const pairs = stepRows.map((row) => ({
    step: row.querySelector(".step-input").value.trim(),
    expected: row.querySelector(".step-expected-input").value.trim(),
    stepInput: row.querySelector(".step-input"),
    expectedInput: row.querySelector(".step-expected-input")
  }));

  if (pairs.every(({ step, expected }) => step === "" && expected === "")) {
    updateStatus("Steps / Expected는 최소 1개 이상 입력해야 합니다.");
    pairs[0]?.stepInput.focus();
    return false;
  }

  const partialPair = pairs.find(({ step, expected }) =>
    (step !== "" || expected !== "") && !(step !== "" && expected !== "")
  );

  if (partialPair) {
    updateStatus("각 단계에는 단계 내용과 기대 결과를 모두 입력해야 합니다.");
    if (partialPair.step === "") {
      partialPair.stepInput.focus();
    } else {
      partialPair.expectedInput.focus();
    }
    return false;
  }

  return true;
}

function clearValidationErrors() {
  for (const { element } of requiredFieldConfigs) {
    if (element) {
      element.setCustomValidity("");
    }
  }
}

function validateRequiredFields() {
  clearValidationErrors();

  for (const { element, label, getValue } of requiredFieldConfigs) {
    if (getValue() === "") {
      element.setCustomValidity(`${label}은(는) 필수 입력입니다.`);
      element.reportValidity();
      return false;
    }
  }

  return validateStepPairs();
}

function validateTestRunFields() {
  elements.actualResult.setCustomValidity("");
  if (elements.actualResult.value.trim() === "") {
    elements.actualResult.setCustomValidity("Actual Result는 필수 입력입니다.");
    elements.actualResult.reportValidity();
    return false;
  }
  return true;
}

async function handleSubmit(event) {
  event.preventDefault();
  state.apiBaseUrl = getApiBaseUrl() || state.apiBaseUrl;

  if (!validateRequiredFields()) {
    updateStatus("필수 항목을 모두 입력해야 저장할 수 있습니다.");
    return;
  }

  const payload = getPayload();
  const id = elements.testCaseId.value;

  try {
    if (id) {
      const updated = await request(`/api/testcases/${id}`, {
        method: "PUT",
        body: JSON.stringify(payload)
      });
      updateStatus(`테스트케이스 #${updated.id}를 수정했습니다.`);
      setFlowStage("saved", "수정이 반영됐습니다. 필요하면 바로 실행 결과를 추가하세요.");
    } else {
      const created = await request("/api/testcases", {
        method: "POST",
        body: JSON.stringify(payload)
      });
      updateStatus(`테스트케이스 #${created.id}를 생성했습니다.`);
      setFlowStage("saved", "저장이 완료됐습니다. 이어서 실행 결과를 기록할 수 있습니다.");
      await loadTestCases();
      const createdItem = state.testCases.find((testCase) => testCase.id === created.id);
      if (createdItem) {
        await populateForm(createdItem);
        elements.actualResult.focus();
      }
      return;
    }

    await loadTestCases();

    if (id) {
      const refreshed = state.testCases.find((testCase) => String(testCase.id) === id);
      if (refreshed) {
        await populateForm(refreshed);
      }
    } else {
      resetForm();
    }
  } catch (error) {
    updateStatus(`저장 실패: ${error.message}`);
  }
}

async function handleTestRunSubmit(event) {
  event.preventDefault();

  const testCaseId = elements.testCaseId.value;
  if (!testCaseId) {
    updateRunStatus("먼저 저장된 테스트케이스를 선택해야 실행 결과를 기록할 수 있습니다.");
    return;
  }

  if (!validateTestRunFields()) {
    updateRunStatus("실행 결과를 입력해야 저장할 수 있습니다.");
    return;
  }

  try {
    const created = await request(`/api/testcases/${testCaseId}/runs`, {
      method: "POST",
      body: JSON.stringify({
        status: elements.runStatus.value,
        actualResult: elements.actualResult.value.trim(),
        notes: elements.runNotes.value.trim() || null
      })
    });

    updateRunStatus(`실행 결과 #${created.id}를 저장했습니다.`);
    setFlowStage("run", "실행 결과가 기록됐습니다. 추가 실행 이력을 계속 남길 수 있습니다.");
    elements.testRunForm.reset();
    elements.runStatus.value = "PASSED";
    await loadTestRuns(testCaseId);
  } catch (error) {
    updateRunStatus(`실행 결과 저장 실패: ${error.message}`);
  }
}

async function deleteTestRun(runId) {
  const testCaseId = elements.testCaseId.value;
  if (!testCaseId) {
    return;
  }

  const shouldDelete = window.confirm(`실행 결과 #${runId}를 삭제할까요?`);
  if (!shouldDelete) {
    return;
  }

  try {
    await request(`/api/testcases/${testCaseId}/runs/${runId}`, {
      method: "DELETE"
    });
    updateRunStatus(`실행 결과 #${runId}를 삭제했습니다.`);
    await loadTestRuns(testCaseId);
  } catch (error) {
    updateRunStatus(`실행 결과 삭제 실패: ${error.message}`);
  }
}

async function handleDelete() {
  const id = elements.testCaseId.value;
  if (!id) {
    return;
  }

  const shouldDelete = window.confirm(`테스트케이스 #${id}를 삭제할까요?`);
  if (!shouldDelete) {
    return;
  }

  try {
    await request(`/api/testcases/${id}`, { method: "DELETE" });
    updateStatus(`테스트케이스 #${id}를 삭제했습니다.`);
    resetForm();
    await loadTestCases();
  } catch (error) {
    updateStatus(`삭제 실패: ${error.message}`);
  }
}

async function verifyConnection() {
  state.apiBaseUrl = getApiBaseUrl() || state.apiBaseUrl;
  try {
    await request("/api/testcases", { method: "GET" });
    updateStatus(`연결 성공: ${state.apiBaseUrl}`);
  } catch (error) {
    updateStatus(`연결 실패: ${error.message}`);
  }
}

function escapeHtml(value) {
  return String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function formatDateTime(value) {
  return new Date(value).toLocaleString("ko-KR", {
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit"
  });
}

async function bootstrap() {
  const config = await window.desktopApi.getConfig();
  elements.platformPill.textContent = config.platform;
  elements.versionPill.textContent = `v${config.version}`;
  state.apiBaseUrl = localStorage.getItem("tms.apiBaseUrl") || config.defaultApiBaseUrl;
  elements.apiBaseUrl.value = state.apiBaseUrl;

  elements.form.addEventListener("submit", handleSubmit);
  elements.saveCurrentAsTemplateButton.addEventListener("click", saveTemplate);
  elements.testRunForm.addEventListener("submit", handleTestRunSubmit);
  elements.saveTemplateButton.addEventListener("click", saveTemplate);
  elements.applyTemplateButton.addEventListener("click", applyTemplate);
  elements.deleteTemplateButton.addEventListener("click", deleteTemplate);
  elements.refreshButton.addEventListener("click", loadTestCases);
  elements.connectButton.addEventListener("click", verifyConnection);
  elements.newButton.addEventListener("click", resetForm);
  elements.duplicateButton.addEventListener("click", duplicateCurrentTestCase);
  elements.resetButton.addEventListener("click", resetForm);
  elements.deleteButton.addEventListener("click", handleDelete);
  elements.addStepButton.addEventListener("click", () => appendStepRow());
  for (const { element } of requiredFieldConfigs) {
    if (!element) {
      continue;
    }
    element.addEventListener("input", () => element.setCustomValidity(""));
    element.addEventListener("change", () => element.setCustomValidity(""));
  }
  elements.apiBaseUrl.addEventListener("change", () => {
    state.apiBaseUrl = getApiBaseUrl() || state.apiBaseUrl;
    localStorage.setItem("tms.apiBaseUrl", state.apiBaseUrl);
  });

  loadTemplates();
  renderTemplateOptions();
  resetForm();
  await loadTestCases();
}

bootstrap();
