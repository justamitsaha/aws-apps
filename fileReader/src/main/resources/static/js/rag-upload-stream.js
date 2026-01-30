const BASE_URL = "http://localhost:8081";

let allChunks = [];
let showAll = false;

document.getElementById("ragUploadBtn").addEventListener("click", uploadAndStream);
document.getElementById("clearBtn").addEventListener("click", clearAll);

document.getElementById("toggleChunksBtn").addEventListener("click", () => {
    showAll = !showAll;
    document.getElementById("toggleChunksBtn").innerText = showAll ? "Show Less" : "Show All";
    renderChunks();
});

document.getElementById("chunkSearch").addEventListener("input", () => {
    renderChunks();
});

function clearAll() {
    allChunks = [];
    showAll = false;

    document.getElementById("toggleChunksBtn").innerText = "Show All";
    document.getElementById("chunkSearch").value = "";

    document.getElementById("summaryBox").innerText = "Cleared.";
    document.getElementById("chunkList").innerHTML = "<span class='text-muted'>No chunks yet.</span>";
    document.getElementById("rawJsonBox").textContent = "";
}

async function uploadAndStream() {
    const file = document.getElementById("ragFile").files[0];
    if (!file) {
        alert("Please select a file first");
        return;
    }

    clearAll();
    document.getElementById("summaryBox").innerText = "Uploading and streaming chunks...";

    const formData = new FormData();
    formData.append("file", file);

    const response = await fetch(`${BASE_URL}/rag/upload/rag`, {
        method: "POST",
        body: formData
    });

    if (!response.ok) {
        document.getElementById("summaryBox").innerHTML =
            `Failed (HTTP ${response.status})`;
        return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");

    while (true) {
        const { value, done } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        parseSseChunk(chunk);
        renderChunks(); // live update
    }

    document.getElementById("summaryBox").innerHTML =
        `<span class="text-success">✔ Streaming completed</span>`;
}

function parseSseChunk(raw) {
    // SSE format: data:{...}\n\n
    raw.split("\n\n").forEach(event => {
        if (!event.startsWith("data:")) return;

        const json = event.replace("data:", "").trim();
        if (!json) return;

        try {
            const obj = JSON.parse(json); // {index, text}
            allChunks.push(obj);
            document.getElementById("rawJsonBox").textContent = JSON.stringify(allChunks, null, 2);
        } catch (e) {
            // ignore partial JSON fragments
        }
    });
}

function renderChunks() {
    const list = document.getElementById("chunkList");

    if (!allChunks.length) {
        list.innerHTML = "<span class='text-muted'>No chunks yet.</span>";
        return;
    }

    const query = (document.getElementById("chunkSearch").value || "").trim().toLowerCase();

    const filtered = allChunks.filter(c => {
        if (!query) return true;
        return (c.text || "").toLowerCase().includes(query);
    });

    const maxPreview = showAll ? filtered.length : Math.min(20, filtered.length);

    document.getElementById("summaryBox").innerHTML = `
        <div><strong>Total streamed:</strong> ${allChunks.length}</div>
        <div><strong>Matched:</strong> ${filtered.length}</div>
        <div class="text-muted">Showing: ${maxPreview}</div>
    `;

    if (!filtered.length) {
        list.innerHTML = `<div class="text-warning">No chunks match your search.</div>`;
        return;
    }

    list.innerHTML = filtered.slice(0, maxPreview).map(c => {
        return `
            <div class="chunk-item">
                <div class="d-flex justify-content-between align-items-center">
                    <strong>Chunk #${c.index}</strong>
                    <span class="badge bg-secondary">${(c.text || "").length} chars</span>
                </div>
                <div class="chunk-text">${escapeHtml(trimText(c.text, 700))}</div>
            </div>
        `;
    }).join("");
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
