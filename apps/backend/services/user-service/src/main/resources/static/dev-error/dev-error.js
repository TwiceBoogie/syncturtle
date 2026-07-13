function showTab(tabName) {
  document.querySelectorAll("[data-tab-panel]").forEach((panel) => {
    panel.classList.toggle("hidden", panel.dataset.tabPanel !== tabName);
  });

  document.querySelectorAll("[data-tab-button]").forEach((button) => {
    button.classList.toggle("active", button.dataset.tabButton === tabName);
  });

  if (tabName !== "flow") {
    closeFlowDrawer();
  }
}

function scrollToFlowNode(nodeId) {
  document.getElementById(nodeId)?.scrollIntoView({
    behavior: "smooth",
    block: "center",
    inline: "center",
  });
}

function setTextContent(id, value) {
  const element = document.getElementById(id);

  if (!element) {
    return;
  }

  element.textContent = value || "--";
}

function setFlowDrawerOpen(isOpen) {
  const drawer = document.getElementById("flow-drawer");
  const backdrop = document.getElementById("flow-drawer-backdrop");

  if (drawer) {
    drawer.hidden = !isOpen;
    drawer.setAttribute("aria-hidden", String(!isOpen));
  }

  if (backdrop) {
    backdrop.hidden = !isOpen;
  }

  document.body.classList.toggle("overflow-hidden", isOpen);
}

function openFlowDrawer() {
  setFlowDrawerOpen(true);
}

function closeFlowDrawer() {
  setFlowDrawerOpen(false);

  document.querySelectorAll("[data-flow-node]").forEach((node) => {
    node.classList.remove("flow-node-selected");
  });
}

function getFlowSelectedNode() {
  return document.querySelector("[data-flow-node].flow-node-selected");
}

function selectFlowNode(node) {
  if (!node) {
    return;
  }

  document.querySelectorAll("[data-flow-node]").forEach((candidate) => {
    candidate.classList.remove("flow-node-selected");
  });

  node.classList.add("flow-node-selected");

  const data = node.dataset;
  const rootCause = data.flowRoot || "";
  const rootCauseWrap = document.getElementById("flow-selected-root-wrap");

  setTextContent("flow-selected-index", data.flowIndex);
  setTextContent("flow-selected-kind", data.flowKind);
  setTextContent("flow-selected-class", data.flowClass);
  setTextContent("flow-selected-method", data.flowMethod);
  setTextContent("flow-selected-caller", data.flowCaller);
  setTextContent("flow-selected-status", data.flowStatus);
  setTextContent("flow-selected-summary", data.flowSummary);
  setTextContent("flow-selected-next", data.flowNext);
  setTextContent("flow-selected-root", rootCause);

  rootCauseWrap?.classList.toggle("hidden", rootCause.length === 0);

  openFlowDrawer();
}

function openFlowNode(nodeId) {
  const node = document.getElementById(nodeId);

  if (!node) {
    return;
  }

  scrollToFlowNode(nodeId);
  selectFlowNode(node);
}

async function copyFlowNodeSummary() {
  const node = getFlowSelectedNode();

  if (!node) {
    return;
  }

  const summary = {
    index: node.dataset.flowIndex || "unknown",
    kind: node.dataset.flowKind || "unknown",
    className: node.dataset.flowClass || "unknown",
    method: node.dataset.flowMethod || "unknown",
    status: node.dataset.flowStatus || "unknown",
    caller: node.dataset.flowCaller || "unknown",
    rootCause: node.dataset.flowRoot || null,
    next: node.dataset.flowNext || null,
  };

  const markdown = [
    "## Request flow selected node",
    "",
    `**Index:** ${summary.index}`,
    `**Kind:** ${summary.kind}`,
    `**Class:** ${summary.className}`,
    `**Method:** ${summary.method}`,
    `**Status:** ${summary.status}`,
    `**Called by:** ${summary.caller}`,
    summary.rootCause ? `**Root cause:** ${summary.rootCause}` : null,
    summary.next ? `**Next:** ${summary.next}` : null,
  ]
    .filter(Boolean)
    .join("\n");

  await navigator.clipboard.writeText(markdown);
}

async function copyDebugMarkdown() {
  const root = document.getElementById("dev-error-root");

  const debug = {
    status: root?.dataset.status || "unknown",
    error: root?.dataset.error || "Error",
    message: root?.dataset.message || "No message",
    exceptionType: root?.dataset.exceptionType || "unknown",
    method: root?.dataset.method || "unknown",
    path: root?.dataset.path || "unknown",
    timestamp: root?.dataset.timestamp || "unknown",
    requestId: root?.dataset.requestId || null,
  };

  const markdown = [
    `# ${debug.status} ${debug.error}`,
    "",
    `**Message:** ${debug.message}`,
    `**Exception:** ${debug.exceptionType}`,
    `**Method:** ${debug.method}`,
    `**Path:** ${debug.path}`,
    `**Timestamp:** ${debug.timestamp}`,
    debug.requestId ? `**Request ID:** ${debug.requestId}` : null,
  ]
    .filter(Boolean)
    .join("\n");

  await navigator.clipboard.writeText(markdown);
}

function shouldIgnoreFlowClick(element) {
  const flowNode = element.closest("[data-flow-node]");
  const button = element.closest("button");

  if (
    element.closest(
      "[data-flow-drawer-close], summary, a, input, textarea, select",
    )
  ) {
    return true;
  }

  return Boolean(button && !flowNode);
}

document.addEventListener("DOMContentLoaded", () => {
  closeFlowDrawer();

  const requestFlowPanel = document.getElementById("request-flow-panel");

  requestFlowPanel?.addEventListener("click", (event) => {
    const target = event.target;

    if (!(target instanceof Element)) {
      return;
    }

    if (shouldIgnoreFlowClick(target)) {
      return;
    }

    const node = target.closest("[data-flow-node]");

    if (!node || !requestFlowPanel.contains(node)) {
      return;
    }

    selectFlowNode(node);
  });

  requestFlowPanel?.addEventListener("keydown", (event) => {
    if (event.key !== "Enter" && event.key !== " ") {
      return;
    }

    const target = event.target;

    if (!(target instanceof Element)) {
      return;
    }

    const node = target.closest("[data-flow-node]");

    if (!node || !requestFlowPanel.contains(node)) {
      return;
    }

    event.preventDefault();
    selectFlowNode(node);
  });

  document.addEventListener("click", (event) => {
    const target = event.target;

    if (!(target instanceof Element)) {
      return;
    }

    if (!target.closest("[data-flow-drawer-close]")) {
      return;
    }

    closeFlowDrawer();
  });

  document.addEventListener("keydown", (event) => {
    if (event.key === "Escape") {
      closeFlowDrawer();
    }
  });
});