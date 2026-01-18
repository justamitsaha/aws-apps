const BASE_URL = "";

$("#searchBtn").click(function () {
  const id = $("#customerId").val();

  if (!id) {
    alert("Please enter a Customer ID");
    return;
  }

  $("#summaryBox").html("<span class='text-muted'>Loading...</span>");
  $("#rawJsonBox").text("");

  $.ajax({
    url: `${BASE_URL}/customerProfile/${id}`,
    method: "GET",
    success: function (data) {
      renderSummary(data);
      $("#rawJsonBox").text(JSON.stringify(data, null, 2));
    },
    error: function (xhr) {
      $("#summaryBox").html(
        `<span class="text-danger">Failed to load profile (HTTP ${xhr.status})</span>`
      );
      $("#rawJsonBox").text(xhr.responseText || "No response body");
    },
  });
});

function renderSummary(p) {
  const churnBadge = p.churn
    ? `<span class="badge bg-danger">CHURN</span>`
    : `<span class="badge bg-success">ACTIVE</span>`;

  const seniorBadge = p.seniorCitizen
    ? `<span class="badge bg-warning text-dark">Senior Citizen</span>`
    : `<span class="badge bg-secondary">Non-Senior</span>`;

  $("#summaryBox").html(`
    <div class="row g-3">
      <div class="col-md-4">
        <div class="summary-item">
          <div class="label">Customer ID</div>
          <div class="value">${p.customerId}</div>
        </div>
      </div>

      <div class="col-md-4">
        <div class="summary-item">
          <div class="label">Age / Gender</div>
          <div class="value">${p.age} / ${p.gender}</div>
        </div>
      </div>

      <div class="col-md-4">
        <div class="summary-item">
          <div class="label">Geography</div>
          <div class="value">${p.geography}</div>
        </div>
      </div>

      <div class="col-md-4">
        <div class="summary-item">
          <div class="label">Status</div>
          <div class="value">${churnBadge} ${seniorBadge}</div>
        </div>
      </div>

      <div class="col-md-4">
        <div class="summary-item">
          <div class="label">Credit Score</div>
          <div class="value">${p.creditScore}</div>
        </div>
      </div>

      <div class="col-md-4">
        <div class="summary-item">
          <div class="label">Products / Card</div>
          <div class="value">${p.numOfProducts} / ${p.hasCrCard ? "Yes" : "No"}</div>
        </div>
      </div>

      <div class="col-md-4">
        <div class="summary-item">
          <div class="label">Monthly Charges</div>
          <div class="value">${formatMoney(p.monthlyCharges)}</div>
        </div>
      </div>

      <div class="col-md-4">
        <div class="summary-item">
          <div class="label">Total Charges</div>
          <div class="value">${formatMoney(p.totalCharges)}</div>
        </div>
      </div>

      <div class="col-md-4">
        <div class="summary-item">
          <div class="label">Balance</div>
          <div class="value">${formatMoney(p.balance)}</div>
        </div>
      </div>

      <div class="col-12">
        <hr/>
        <div class="summary-item">
          <div class="label">Services</div>
          <div class="value">
            Internet: <strong>${p.internetService}</strong>,
            Tech Support: <strong>${p.techSupport}</strong>,
            Online Security: <strong>${p.onlineSecurity}</strong>,
            Streaming TV: <strong>${p.streamingTv}</strong>,
            Streaming Movies: <strong>${p.streamingMovies}</strong>
          </div>
        </div>
      </div>
    </div>
  `);
}

function formatMoney(value) {
  if (value === null || value === undefined) return "-";
  return Number(value).toLocaleString("en-IN", {
    style: "currency",
    currency: "INR",
    maximumFractionDigits: 2,
  });
}
