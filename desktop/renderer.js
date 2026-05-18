const state = {
  apiBaseUrl: "http://localhost:8080",
  selectedId: null,
  testCases: []
};

const elements = {
  apiBaseUrl: document.getElementById("apiBaseUrl"),
  connectButton: document.getElementById("connectButton"),
  refreshButton: document.getElementById("refreshButton"),
  newButton: document.getElementById("newButton"),
  deleteButton: document.getElementById("deleteButton"),
  resetButton: document.getElementById("resetButton"),
  listState: document.getElementById("listState"),
  list: document.getElementById("testCaseList"),
  form: document.getElementById("testCaseForm"),
  editorTitle: document.getElementById("editorTitle"),
  formMode: document.getElementById("formMode"),
  platformPill: document.getElementById("platform-pill"),
  versionPill: document.getElementById("version-pill"),
  testCaseId: document.getElementById("testCaseId"),
  type: document.getElementById("type"),
  title: document.getElementById("title"),
  description: document.getElementById("description"),
  precondition: document.getElementById("precondition"),
  steps: document.getElementById("steps"),
  expected: document.getElementById("expected"),
  notes: document.getElementById("notes")
};

function getApiBaseUrl() {
  return elements.apiBaseUrl.value.trim().replace(/\/$/, "");
}

function updateStatus(message) {
  elements.listState.textContent = message;
}

function setSelected(id) {
  state.selectedId = id;
  elements.deleteButton.disabled = id === null;
}

function resetForm() {
  elements.form.reset();
  elements.testCaseId.value = "";
  elements.type.value = "FUNCTIONAL";
  setSelected(null);
  elements.editorTitle.textContent = "테스트케이스 작성";
  elements.formMode.textContent = "새 테스트케이스를 저장할 준비가 됐습니다.";
  renderList();
}

function populateForm(testCase) {
  elements.testCaseId.value = String(testCase.id);
  elements.type.value = testCase.type;
  elements.title.value = testCase.title;
  elements.description.value = testCase.description;
  elements.precondition.value = testCase.precondition;
  elements.steps.value = testCase.steps;
  elements.expected.value = testCase.expected;
  elements.notes.value = testCase.notes ?? "";
  setSelected(testCase.id);
  elements.editorTitle.textContent = `테스트케이스 수정 #${testCase.id}`;
  elements.formMode.textContent = "수정 후 저장하면 PUT 요청이 전송됩니다.";
  renderList();
}

function createListItem(testCase) {
  const item = document.createElement("li");
  item.className = "case-item";
  if (testCase.id === state.selectedId) {
    item.classList.add("active");
  }

  item.innerHTML = `
    <div class="case-item-header">
      <div class="case-title">${escapeHtml(testCase.title)}</div>
      <span class="case-type">${escapeHtml(testCase.type)}</span>
    </div>
    <p class="case-snippet">${escapeHtml(testCase.description)}</p>
  `;

  item.addEventListener("click", () => populateForm(testCase));
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

async function request(path, options = {}) {
  const response = await fetch(`${state.apiBaseUrl}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers ?? {})
    },
    ...options
  });

  if (!response.ok) {
    let message = `Request failed (${response.status})`;
    try {
      const body = await response.json();
      message = body.message ?? message;
    } catch (_error) {
      // ignore invalid JSON
    }
    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
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
    title: elements.title.value.trim(),
    description: elements.description.value.trim(),
    precondition: elements.precondition.value.trim(),
    steps: elements.steps.value.trim(),
    expected: elements.expected.value.trim(),
    notes: elements.notes.value.trim() || null
  };
}

async function handleSubmit(event) {
  event.preventDefault();
  state.apiBaseUrl = getApiBaseUrl() || state.apiBaseUrl;
  const payload = getPayload();
  const id = elements.testCaseId.value;

  try {
    if (id) {
      const updated = await request(`/api/testcases/${id}`, {
        method: "PUT",
        body: JSON.stringify(payload)
      });
      updateStatus(`테스트케이스 #${updated.id}를 수정했습니다.`);
    } else {
      const created = await request("/api/testcases", {
        method: "POST",
        body: JSON.stringify(payload)
      });
      updateStatus(`테스트케이스 #${created.id}를 생성했습니다.`);
    }

    await loadTestCases();

    if (id) {
      const refreshed = state.testCases.find((testCase) => String(testCase.id) === id);
      if (refreshed) {
        populateForm(refreshed);
      }
    } else {
      resetForm();
    }
  } catch (error) {
    updateStatus(`저장 실패: ${error.message}`);
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

async function bootstrap() {
  const config = await window.desktopApi.getConfig();
  elements.platformPill.textContent = config.platform;
  elements.versionPill.textContent = `v${config.version}`;
  state.apiBaseUrl = localStorage.getItem("tms.apiBaseUrl") || config.defaultApiBaseUrl;
  elements.apiBaseUrl.value = state.apiBaseUrl;

  elements.form.addEventListener("submit", handleSubmit);
  elements.refreshButton.addEventListener("click", loadTestCases);
  elements.connectButton.addEventListener("click", verifyConnection);
  elements.newButton.addEventListener("click", resetForm);
  elements.resetButton.addEventListener("click", resetForm);
  elements.deleteButton.addEventListener("click", handleDelete);
  elements.apiBaseUrl.addEventListener("change", () => {
    state.apiBaseUrl = getApiBaseUrl() || state.apiBaseUrl;
    localStorage.setItem("tms.apiBaseUrl", state.apiBaseUrl);
  });

  resetForm();
  await loadTestCases();
}

bootstrap();
