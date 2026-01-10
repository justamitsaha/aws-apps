const BASE_URL = "";

document.getElementById("loadCustomersBtn")
    .addEventListener("click", streamCustomers);

document.getElementById("cleanupBtn")
    .addEventListener("click", cleanup);

async function streamCustomers() {
    const box = document.getElementById("customerStream");
    box.innerHTML = "<div class='stream-line'>Streaming customers...</div>";

    const response = await fetch(BASE_URL + "/upload/customers");
    const reader = response.body.getReader();
    const decoder = new TextDecoder("utf-8");

    while (true) {
        const { value, done } = await reader.read();
        if (done) {
            box.innerHTML +=
                "<div class='stream-line text-success'>✔ Stream completed</div>";
            break;
        }

        const chunk = decoder.decode(value, { stream: true });
        renderCustomerChunk(chunk);
    }
}

function renderCustomerChunk(chunk) {
    chunk.split("\n\n").forEach(event => {
        if (event.startsWith("data:")) {
            const json = event.replace("data:", "").trim();
            if (json) {
                appendCustomer(json);
            }
        }
    });

    const box = document.getElementById("customerStream");
    box.scrollTop = box.scrollHeight;
}

function appendCustomer(json) {
    const customer = JSON.parse(json);

    document.getElementById("customerStream").innerHTML += `
        <div class="stream-line">
            <strong>${customer.customerId}</strong> |
            ${customer.surname ?? ""} |
            ${customer.geography ?? ""} |
            ${customer.age ?? ""}
        </div>
    `;
}

async function cleanup() {
    if (!confirm("Delete ALL customers?")) return;

    const res = await fetch(BASE_URL + "/upload/cleanup", {
        method: "DELETE"
    });

    if (res.status === 204) {
        document.getElementById("customerStream").innerHTML =
            "<div class='stream-line text-danger'>All customers deleted</div>";
    } else {
        alert("Cleanup failed");
    }
}
