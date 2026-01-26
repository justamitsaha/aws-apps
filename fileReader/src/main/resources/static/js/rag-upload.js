const BASE_URL = "http://localhost:8081";

let allChunks = [];
let showAll = false;

$("#ragUploadBtn").click(function () {
    const file = $("#ragFile")[0].files[0];

    if (!file) {
        alert("Please select a file first");
        return;
    }

    $("#ragSummaryBox").html("<span class='text-muted'>Uploading...</span>");
    $("#chunkList").html("<span class='text-muted'>Waiting for chunks...</span>");
    $("#ragRawJsonBox").text("");

    allChunks = [];
    showAll = false;
    $("#toggleChunksBtn").text("Show All");
    $("#chunkSearch").val("");

    const formData = new FormData();
    formData.append("file", file);

    $.ajax({
        url: `${BASE_URL}/rag/upload`,
        method: "POST",
        data: formData,
        processData: false,
        contentType: false,
        success: function (chunks) {
            allChunks = chunks || [];
            $("#ragRawJsonBox").text(JSON.stringify(allChunks, null, 2));
            renderChunks();
        },
        error: function (xhr) {
            $("#ragSummaryBox").html(
                `<span class="text-danger">Upload failed (HTTP ${xhr.status})</span>`
            );
            $("#ragRawJsonBox").text(xhr.responseText || "No response body");
        }
    });
});

// Toggle show all / show less
$("#toggleChunksBtn").click(function () {
    showAll = !showAll;
    $("#toggleChunksBtn").text(showAll ? "Show Less" : "Show All");
    renderChunks();
});

// Search filter
$("#chunkSearch").on("input", function () {
    renderChunks();
});

function renderChunks() {
    if (!allChunks || allChunks.length === 0) {
        $("#ragSummaryBox").html("<span class='text-muted'>No chunks generated yet.</span>");
        $("#chunkList").html("<span class='text-muted'>No data</span>");
        return;
    }

    const query = ($("#chunkSearch").val() || "").trim().toLowerCase();

    const filtered = allChunks.filter(c => {
        if (!query) return true;
        return (c.text || "").toLowerCase().includes(query);
    });

    const maxPreview = showAll ? filtered.length : Math.min(20, filtered.length);

    $("#ragSummaryBox").html(`
        <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
            <div><strong>Total Chunks:</strong> ${allChunks.length}</div>
            <div><strong>Matched:</strong> ${filtered.length}</div>
            <div class="text-muted">Showing: ${maxPreview}</div>
        </div>
    `);

    if (filtered.length === 0) {
        $("#chunkList").html(`<div class="text-warning">No chunks match your search.</div>`);
        return;
    }

    const preview = filtered.slice(0, maxPreview).map((c) => {
        return `
            <div class="chunk-item">
                <div class="d-flex justify-content-between align-items-center">
                    <strong>Chunk #${c.index}</strong>
                    <span class="badge bg-secondary">${c.text.length} chars</span>
                </div>
                <div class="chunk-text">${escapeHtml(trimText(c.text, 700))}</div>
            </div>
        `;
    }).join("");

    $("#chunkList").html(preview);
}

function trimText(s, max) {
    if (!s) return "";
    s = String(s);
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

$("#ragSearchBtn").click(function () {
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
        success: function (matches) {
            renderSearchResults(matches);
        },
        error: function (xhr) {
            $("#searchResults").html(
                `<span class="text-danger">Search failed (HTTP ${xhr.status})</span>`
            );
        }
    });
});

// Enter key triggers search
$("#ragQuery").on("keypress", function (e) {
    if (e.which === 13) $("#ragSearchBtn").click();
});

function renderSearchResults(matches) {
    if (!matches || matches.length === 0) {
        $("#searchResults").html("<div class='text-warning'>No matches found.</div>");
        return;
    }

    const html = matches.map((m, i) => {
        return `
          <div class="chunk-item">
              <div class="d-flex justify-content-between align-items-center">
                  <strong>#${i + 1} | ${escapeHtml(m.fileName || "unknown")}</strong>
                  <span class="badge bg-primary">Score: ${formatScore(m.score)}</span>
              </div>

              <div class="text-muted mt-1">
                  Doc: <strong>${m.documentId}</strong> |
                  ChunkId: <strong>${m.chunkId}</strong> |
                  ChunkIndex: <strong>${m.chunkIndex}</strong>
              </div>

              <div class="chunk-text mt-2">${escapeHtml(trimText(m.chunkText, 800))}</div>
          </div>
        `;
    }).join("");

    $("#searchResults").html(html);
}

function formatScore(score) {
    if (score === null || score === undefined) return "-";
    return Number(score).toFixed(4);
}

