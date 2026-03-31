// ── Configuration ──────────────────────────────────────────────────────────
// Change this to your Render.com backend URL before deploying to GitHub Pages.
const API_BASE = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
  ? 'http://localhost:8080'
  : 'https://YOUR-RENDER-URL.onrender.com';   // ← paste your Render URL here

// ── Tab navigation ─────────────────────────────────────────────────────────
document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
    if (btn.dataset.tab === 'audit') fetchAudit();
  });
});

// ── Queue polling ──────────────────────────────────────────────────────────
async function fetchQueue() {
  try {
    const res = await fetch(`${API_BASE}/api/orders`);
    if (!res.ok) return;
    const orders = await res.json();
    renderQueue(orders);
  } catch (_) { /* backend not yet reachable */ }
}

function renderQueue(orders) {
  const tbody = document.getElementById('queue-body');
  const empty = document.getElementById('queue-empty');
  tbody.innerHTML = '';

  if (!orders.length) {
    empty.classList.remove('hidden');
    return;
  }
  empty.classList.add('hidden');

  orders.forEach(o => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td><code>${o.orderId}</code></td>
      <td>${o.type}</td>
      <td>${o.patientName}</td>
      <td>${o.clinician}</td>
      <td>${o.description}</td>
      <td><span class="badge badge-${o.priority}">${o.priority}</span></td>
      <td><span class="status status-${o.status}">${o.status.replace('_', ' ')}</span></td>
      <td>${o.claimedBy ?? '—'}</td>
      <td>${actionsFor(o)}</td>`;
    tbody.appendChild(tr);
  });

  // attach action listeners
  tbody.querySelectorAll('[data-action]').forEach(btn => {
    btn.addEventListener('click', handleAction);
  });
}

function actionsFor(o) {
  if (o.status === 'PENDING') {
    return `<button class="btn btn-claim"   data-action="claim"  data-id="${o.orderId}">Claim</button>
            <button class="btn btn-cancel"  data-action="cancel" data-id="${o.orderId}" data-clinician="${o.clinician}">Cancel</button>`;
  }
  if (o.status === 'IN_PROGRESS') {
    return `<button class="btn btn-complete" data-action="complete" data-id="${o.orderId}">Complete</button>`;
  }
  return '—';
}

async function handleAction(e) {
  const btn    = e.currentTarget;
  const action = btn.dataset.action;
  const id     = btn.dataset.id;
  const staff  = document.getElementById('staff-id').value.trim();

  if ((action === 'claim' || action === 'complete') && !staff) {
    alert('Please enter your Staff ID in the box below the table before claiming or completing an order.');
    return;
  }

  const body = action === 'cancel'
    ? { actor: btn.dataset.clinician || staff || 'clinician' }
    : { actor: staff };

  try {
    const res = await fetch(`${API_BASE}/api/orders/${id}/${action}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(body)
    });
    const data = await res.json();
    if (!res.ok) { alert('Error: ' + (data.error || 'Unknown error')); return; }
    fetchQueue();
    fetchAudit();
  } catch (err) {
    alert('Request failed: ' + err.message);
  }
}

// Poll every 3 seconds
fetchQueue();
setInterval(fetchQueue, 3000);

// ── Submit form ────────────────────────────────────────────────────────────
document.getElementById('submit-form').addEventListener('submit', async e => {
  e.preventDefault();
  const msg = document.getElementById('submit-message');

  const payload = {
    type:        document.getElementById('order-type').value,
    patientName: document.getElementById('patient-name').value.trim(),
    clinician:   document.getElementById('clinician').value.trim(),
    description: document.getElementById('description').value.trim(),
    priority:    document.getElementById('priority').value
  };

  try {
    const res = await fetch(`${API_BASE}/api/orders`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();

    if (!res.ok) {
      msg.textContent = 'Error: ' + (data.error || 'Submission failed.');
      msg.className = 'message error';
    } else {
      msg.textContent = `Order ${data.orderId} submitted successfully.`;
      msg.className = 'message success';
      e.target.reset();
    }
    msg.classList.remove('hidden');
    setTimeout(() => msg.classList.add('hidden'), 4000);
  } catch (err) {
    msg.textContent = 'Network error: ' + err.message;
    msg.className = 'message error';
    msg.classList.remove('hidden');
  }
});

// ── Audit trail ────────────────────────────────────────────────────────────
async function fetchAudit() {
  try {
    const res = await fetch(`${API_BASE}/api/audit`);
    if (!res.ok) return;
    const entries = await res.json();
    renderAudit(entries);
  } catch (_) {}
}

function renderAudit(entries) {
  const tbody = document.getElementById('audit-body');
  const empty = document.getElementById('audit-empty');
  tbody.innerHTML = '';

  if (!entries.length) {
    empty.classList.remove('hidden');
    return;
  }
  empty.classList.add('hidden');

  // newest first
  [...entries].reverse().forEach(e => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${new Date(e.timestamp).toLocaleString()}</td>
      <td><strong>${e.commandType}</strong></td>
      <td><code>${e.orderId}</code></td>
      <td>${e.actor}</td>`;
    tbody.appendChild(tr);
  });
}
