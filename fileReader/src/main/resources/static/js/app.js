const BASE_URL = "http://localhost:8080";

$("#uploadCustomerBtn").click(() => {
    uploadAndStream("/upload/customer", "#customerFile");
});

$("#uploadChurnBtn").click(() => {
    uploadAndStream("/upload/churn", "#churnFile");
});

async function uploadAndStream(endpoint, fileInputId) {
    const file = $(fileInputId)[0].files[0];
    if (!file) {
        alert("Select a file first");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    $("#responseBox").empty()
        .append(`<div class="stream-line">Uploading...</div>`);

    const response = await fetch(BASE_URL + endpoint, {
        method: "POST",
        body: formData
    });

    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");

    while (true) {
        const { value, done } = await reader.read();
        if (done) {
            $("#responseBox").append(
                `<div class="stream-line text-success">✔ Completed</div>`
            );
            break;
        }

        const chunk = decoder.decode(value, { stream: true });
        renderSseChunk(chunk);
    }
}

function renderSseChunk(chunk) {
    // Spring SSE sends data: {...}\n\n
    chunk.split("\n\n").forEach(event => {
        if (event.startsWith("data:")) {
            const json = event.replace("data:", "").trim();
            if (json) {
                $("#responseBox").append(
                    `<div class="stream-line">${json}</div>`
                );
            }
        }
    });

    const box = document.getElementById("responseBox");
    box.scrollTop = box.scrollHeight;
}


$("#saveOnlyBtn").click(async function () {
    const file = $("#saveOnlyFile")[0].files[0];

    if (!file) {
        alert("Please select a file first");
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    $("#responseBox").text("Saving file...");

    try {
        const response = await fetch(BASE_URL + "/upload/save-only", {
            method: "POST",
            body: formData
        });

        const text = await response.text();
        $("#responseBox").text(text);

    } catch (e) {
        $("#responseBox").text("Save failed");
    }
});

