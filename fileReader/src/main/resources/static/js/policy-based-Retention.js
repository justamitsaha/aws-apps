const AI_BASE_URL = "http://localhost:8081";

$("#analyzeBtn").click(function () {
  const id = $("#customerId").val();

  if (!id) {
    alert("Please enter a Customer ID");
    return;
  }

  $("#reportBox").html("<span class='text-muted'>Analyzing...</span>");
  $("#rawJsonBox").text("");

  $.ajax({
    url: `${AI_BASE_URL}/retention/${id}/analyze/rag`,
    method: "GET",
    success: function (data) {
      renderReport(data);
      $("#rawJsonBox").text(JSON.stringify(data, null, 2));
    },
    error: function (xhr) {
      $("#reportBox").html(
        `<span class="text-danger">Failed to fetch report (HTTP ${xhr.status})</span>`
      );
      $("#rawJsonBox").text(xhr.responseText || "No response body");
    },
  });
});

function renderReport(r) {
  const riskBadge = riskBadgeHtml(r.riskLevel);

  const reasoningHtml = (r.reasoning || [])
    .map(x => `<li>${escapeHtml(x)}</li>`)
    .join("");

  const actionsHtml = (r.actions || [])
    .map(a => {
      return `
        <div class="border rounded p-3 mb-2">
          <div class="d-flex justify-content-between align-items-center">
            <div class="fw-bold">${escapeHtml(a.title || "-")}</div>
            <span class="badge ${priorityClass(a.priority)}">${escapeHtml(a.priority || "-")}</span>
          </div>
          <div class="text-muted mt-1">${escapeHtml(a.details || "")}</div>
        </div>
      `;
    })
    .join("");

  const offer = r.offer || {};
  const offerHtml = `
    <div class="border rounded p-3">
      <div><span class="fw-bold">Type:</span> ${escapeHtml(offer.type || "-")}</div>
      <div><span class="fw-bold">Description:</span> ${escapeHtml(offer.description || "-")}</div>
      <div><span class="fw-bold">Discount:</span> ${offer.discountPercent ?? "-"}%</div>
      <div><span class="fw-bold">Duration:</span> ${offer.durationMonths ?? "-"} months</div>
    </div>
  `;

  const citationsHtml = renderCitations(r.citations);

  $("#reportBox").html(`
  <div class="row g-3">
    <div class="col-md-4">
      <div class="summary-item">
        <div class="label">Risk Level</div>
        <div class="value">${riskBadge}</div>
      </div>
    </div>

    <div class="col-12">
      <hr/>
      <div class="summary-item">
        <div class="label">Reasoning</div>
        <div class="value">
          <ul class="mb-0">${reasoningHtml || "<li>-</li>"}</ul>
        </div>
      </div>
    </div>

    <div class="col-12">
      <hr/>
      <div class="summary-item">
        <div class="label">Recommended Actions</div>
        <div class="value">
          ${actionsHtml || "<div class='text-muted'>No actions</div>"}
        </div>
      </div>
    </div>

    <div class="col-12">
      <hr/>
      <div class="summary-item">
        <div class="label">Offer</div>
        <div class="value">
          ${offerHtml}
        </div>
      </div>
    </div>

    <div class="col-12">
      <hr/>
      <div class="summary-item">
        <div class="label">Citations (RAG Evidence)</div>
        <div class="value">
          ${citationsHtml}
        </div>
      </div>
    </div>
  </div>
`);

}

function riskBadgeHtml(level) {
  const l = (level || "").toUpperCase();
  if (l === "HIGH") return `<span class="badge bg-danger">HIGH</span>`;
  if (l === "MEDIUM") return `<span class="badge bg-warning text-dark">MEDIUM</span>`;
  return `<span class="badge bg-success">LOW</span>`;
}

function priorityClass(p) {
  const x = (p || "").toUpperCase();
  if (x === "HIGH") return "bg-danger";
  if (x === "MEDIUM") return "bg-warning text-dark";
  return "bg-secondary";
}

// Prevent HTML injection
function escapeHtml(str) {
  return String(str)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function renderCitations(citations) {
  if (!citations || citations.length === 0) {
    return `<div class="text-muted">No citations available.</div>`;
  }

  return `
    <div class="list-group">
      ${citations.map((c, i) => `
        <div class="list-group-item">
          <div class="d-flex justify-content-between align-items-center">
            <strong>${escapeHtml(c.fileName)}</strong>
            <span class="badge bg-info text-dark">
              Score: ${formatScore(c.score)}
            </span>
          </div>
          <div class="text-muted mt-1">
            Chunk Index: <strong>${c.chunkIndex}</strong>
          </div>
        </div>
      `).join("")}
    </div>
  `;
}

function formatScore(score) {
  if (score === null || score === undefined) return "-";
  return Number(score).toFixed(4);
}
