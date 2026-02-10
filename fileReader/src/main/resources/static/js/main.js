const BASE_URL = "http://localhost:8081";

let allChunks = [];
let showAll = false;

$(document).ready(function () {
    loadDocuments();
});

function loadDocuments() {
    $.ajax({
        url: `${BASE_URL}/rag/documents`,
        method: "GET",
        success: function (docs) {
            const select = $("#documentSelect");
            select.empty();

            if (!docs || docs.length === 0) {
                select.append(`<option value="">No documents found</option>`);
                return;
            }

            docs.forEach(d => {
                select.append(`
                    <option value="${d.id}">
                        ${d.fileName} (${d.documentType})
                    </option>
                `);
            });
        },
        error: function () {
            $("#documentSelect").html(
                `<option value="">Failed to load documents</option>`
            );
        }
    });
}


/* ---------------- Upload & Stream ---------------- */

$("#ragUploadBtn").click(uploadAndStream);
$("#clearBtn").click(clearAll);

$("#toggleChunksBtn").click(() => {
    showAll = !showAll;
    $("#toggleChunksBtn").text(showAll ? "Show Less" : "Show All");
    renderChunks();
});

$("#chunkSearch").on("input", renderChunks);

async function uploadAndStream() {
    const file = $("#ragFile")[0].files[0];
    if (!file) {
        alert("Select a file first");
        return;
    }

    clearAll();
    $("#summaryBox").text("Uploading and streaming chunks...");

    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${BASE_URL}/rag/upload`, {
        method: "POST",
        body: formData
    });

    if (!response.ok) {
        $("#summaryBox").text(`Failed (HTTP ${response.status})`);
        return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");

    while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        const text = decoder.decode(value, { stream: true });
        parseSse(text);
        renderChunks();
    }

    $("#summaryBox").html("<span class='text-success'>✔ Streaming completed</span>");
}

function parseSse(raw) {
    raw.split("\n\n").forEach(event => {
        if (!event.startsWith("data:")) return;

        const json = event.replace("data:", "").trim();
        if (!json) return;

        try {
            const obj = JSON.parse(json); // { index, text }
            allChunks.push(obj);
            $("#rawJsonBox").text(JSON.stringify(allChunks, null, 2));
        } catch (_) { }
    });
}

function renderChunks() {
    if (!allChunks.length) {
        $("#chunkList").html("<span class='text-muted'>No chunks yet.</span>");
        return;
    }

    const q = ($("#chunkSearch").val() || "").toLowerCase();
    const filtered = allChunks.filter(c =>
        !q || (c.text || "").toLowerCase().includes(q)
    );

    const max = showAll ? filtered.length : Math.min(20, filtered.length);

    $("#summaryBox").html(`
        <div>Total: ${allChunks.length}</div>
        <div>Matched: ${filtered.length}</div>
        <div class="text-muted">Showing: ${max}</div>
    `);

    $("#chunkList").html(filtered.slice(0, max).map(c => `
        <div class="chunk-item">
            <div class="d-flex justify-content-between">
                <strong>Chunk #${c.index}</strong>
                <span class="badge bg-secondary">${c.text.length} chars</span>
            </div>
            <div class="chunk-text">${escapeHtml(trimText(c.text, 700))}</div>
        </div>
    `).join(""));
}

function clearAll() {
    allChunks = [];
    showAll = false;
    $("#toggleChunksBtn").text("Show All");
    $("#chunkSearch").val("");
    $("#chunkList").html("<span class='text-muted'>No chunks yet.</span>");
    $("#summaryBox").text("Cleared.");
    $("#rawJsonBox").text("");
}

/* ---------------- RAG SEARCH ---------------- */

$("#ragSearchBtn").click(runSearch);
$("#ragQuery").on("keypress", e => {
    if (e.which === 13) runSearch();
});

function runSearch() {
    const q = ($("#ragQuery").val() || "").trim();
    const topK = $("#topK").val() || 5;

    if (!q) {
        alert("Enter a search query");
        return;
    }

    $("#searchResults").html("<span class='text-muted'>Searching...</span>");

    $.ajax({
        url: `${BASE_URL}/rag/search?q=${encodeURIComponent(q)}&topK=${topK}`,
        method: "GET",
        success: renderSearchResults,
        error: xhr => {
            $("#searchResults").html(
                `<span class="text-danger">Search failed (HTTP ${xhr.status})</span>`
            );
        }
    });
}

function renderSearchResults(matches) {
    if (!matches || !matches.length) {
        $("#searchResults").html("<div class='text-warning'>No matches found.</div>");
        return;
    }

    $("#searchResults").html(matches.map((m, i) => `
        <div class="chunk-item">
            <div class="d-flex justify-content-between">
                <strong>#${i + 1} | ${escapeHtml(m.fileName)}</strong>
                <span class="badge bg-primary">Score: ${formatScore(m.score)}</span>
            </div>
            <div class="text-muted">
                Doc ${m.documentId} | Chunk ${m.chunkIndex}
            </div>
            <div class="chunk-text mt-2">
                ${escapeHtml(trimText(m.chunkText, 800))}
            </div>
        </div>
    `).join(""));
}

/* ---------------- Utils ---------------- */

function trimText(s, max) {
    return s.length > max ? s.substring(0, max) + "..." : s;
}

function escapeHtml(str) {
    return String(str)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

function formatScore(score) {
    if (score === null || score === undefined) return "-";
    return Number(score).toFixed(4);
}


$("#askDocBtn").click(askDocument);
$("#docQuestion").on("keypress", function (e) {
    if (e.which === 13) askDocument();
});

function askDocument() {
    const documentId = $("#documentSelect").val();
    const question = ($("#docQuestion").val() || "").trim();

    if (!documentId) {
        alert("Please select a document first");
        return;
    }

    if (!question) {
        alert("Please enter a question");
        return;
    }

    $("#docAnswerBox").html("<span class='text-muted'>Thinking...</span>");

    $.ajax({
        url: `${BASE_URL}/rag/${documentId}/ask`,
        method: "POST",
        contentType: "application/json",
        data: JSON.stringify({
            question: question
        }),
        success: function (resp) {
            renderDocumentAnswer(resp);
        },
        error: function (xhr) {
            if (xhr.status === 404) {
                $("#docAnswerBox").html(
                    "<span class='text-warning'>Document not found</span>"
                );
            } else {
                $("#docAnswerBox").html(
                    `<span class="text-danger">Failed (HTTP ${xhr.status})</span>`
                );
            }
        }
    });
}

function renderDocumentAnswer(resp) {
    if (!resp) {
        $("#docAnswerBox").html("<span class='text-warning'>No response</span>");
        return;
    }

    const answerHtml = `
        <div class="mb-3">
            <div class="fw-bold mb-1">Answer</div>
            <div class="border rounded p-3 bg-light">
                ${escapeHtml(resp.answer || "-")}
            </div>
        </div>
    `;

    const sources = resp.sources || [];

    const sourcesHtml = sources.length === 0
        ? `<div class="text-muted">No source chunks returned</div>`
        : `
            <div class="fw-bold mb-2">Sources</div>
            ${sources.map((s, i) => `
                <div class="chunk-item mb-2">
                    <div class="d-flex justify-content-between">
                        <strong>Source #${i + 1}</strong>
                        ${s.score !== undefined
                ? `<span class="badge bg-info text-dark">Score: ${formatScore(s.score)}</span>`
                : ""}
                    </div>

                    <div class="text-muted mt-1">
                        Chunk Index: ${s.chunkIndex ?? "-"}
                    </div>

                    <div class="chunk-text mt-2">
                        ${escapeHtml(trimText(s.text || "", 800))}
                    </div>
                </div>
            `).join("")}
        `;

    $("#docAnswerBox").html(answerHtml + sourcesHtml);
}
