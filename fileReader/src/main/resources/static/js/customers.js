const BASE_URL = "http://localhost:8080";

let currentPage = 0;

document.getElementById("loadCustomersBtn").addEventListener("click", () => {
    currentPage = getPage();
    streamData(currentPage);
});

document.getElementById("nextBtn").addEventListener("click", () => {
    currentPage = getPage() + 1;
    setPage(currentPage);
    streamData(currentPage);
});

document.getElementById("prevBtn").addEventListener("click", () => {
    currentPage = Math.max(0, getPage() - 1);
    setPage(currentPage);
    streamData(currentPage);
});

document.getElementById("cleanupBtn").addEventListener("click", cleanup);

// Reset to page 0 when dataset changes
document.getElementById("datasetSelect").addEventListener("change", () => {
    currentPage = 0;
    setPage(0);
    clearStreamBox();
});

function getPage() {
    return parseInt(document.getElementById("pageInput").value || "0", 10);
}

function setPage(p) {
    document.getElementById("pageInput").value = p;
}

function getSize() {
    return parseInt(document.getElementById("sizeInput").value || "10", 10);
}

function getDataset() {
    return document.getElementById("datasetSelect").value;
}

function clearStreamBox() {
    document.getElementById("customerStream").innerHTML =
        "<div class='stream-line text-muted'>Select dataset and click Load Page</div>";
    document.getElementById("pageInfo").innerHTML = "";
}

async function streamData(page) {
    const size = getSize();
    const dataset = getDataset();

    const box = document.getElementById("customerStream");
    const pageInfo = document.getElementById("pageInfo");

    box.innerHTML = "";
    pageInfo.innerHTML = `Loading <strong>${dataset}</strong> page <strong>${page}</strong>, size <strong>${size}</strong>...`;

    const url = `${BASE_URL}/customerProfile/${dataset}?page=${page}&size=${size}`;

    const response = await fetch(url);

    if (!response.ok) {
        box.innerHTML = `<div class="stream-line text-danger">Failed: HTTP ${response.status}</div>`;
        return;
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");

    let count = 0;

    while (true) {
        const { value, done } = await reader.read();

        if (done) {
            box.innerHTML += `<div class="stream-line text-success">✔ Page completed (${count} records)</div>`;
            pageInfo.innerHTML = `Showing <strong>${dataset}</strong> page <strong>${page}</strong>, size <strong>${size}</strong> (received <strong>${count}</strong>)`;
            break;
        }

        const chunk = decoder.decode(value, { stream: true });
        count += renderSseChunk(chunk, dataset);
    }

    box.scrollTop = 0;
}

function renderSseChunk(chunk, dataset) {
    let added = 0;

    chunk.split("\n\n").forEach(event => {
        if (event.startsWith("data:")) {
            const json = event.replace("data:", "").trim();
            if (json) {
                if (dataset === "customers") {
                    appendCustomer(json);
                } else {
                    appendCustomerChurn(json);
                }
                added++;
            }
        }
    });

    return added;
}

function appendCustomer(json) {
    const c = JSON.parse(json);

    document.getElementById("customerStream").innerHTML += `
        <div class="stream-line">
            <strong>${c.customerId}</strong> |
            Row: ${c.rowNumbers ?? "-"} |
            ${c.surname ?? ""} |
            ${c.geography ?? ""} |
            ${c.gender ?? ""} |
            Age: ${c.age ?? ""} |
            Score: ${c.creditScore ?? ""} |
            Exited: <strong>${c.exited ? "YES" : "NO"}</strong>
        </div>
    `;
}

function appendCustomerChurn(json) {
    const c = JSON.parse(json);

    document.getElementById("customerStream").innerHTML += `
        <div class="stream-line">
            <strong>${c.customerId}</strong> |
            UID: ${c.uniqueId ?? "-"} |
            ${c.gender ?? ""} |
            Tenure: ${c.tenure ?? ""} |
            Internet: ${c.internetService ?? ""} |
            Contract: ${c.contract ?? ""} |
            Monthly: ${c.monthlyCharges ?? ""} |
            Churn: <strong>${c.churn ? "YES" : "NO"}</strong>
        </div>
    `;
}

async function cleanup() {
    if (!confirm("Delete ALL customers + churn data?")) return;

    const res = await fetch(BASE_URL + "/customerProfile/cleanup", {
        method: "DELETE"
    });

    if (res.status === 204) {
        document.getElementById("customerStream").innerHTML =
            "<div class='stream-line text-danger'>All data deleted</div>";
        document.getElementById("pageInfo").innerHTML = "";
    } else {
        alert("Cleanup failed");
    }
}

// init view
clearStreamBox();
