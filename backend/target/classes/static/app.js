// ── Configuration ──────────────────────────────────────────────────────────
// Week 2: update the production URL below after deploying to Render.
const API_BASE = (window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1')
  ? 'http://localhost:8080'
  : 'https://project2-week2.onrender.com';

// Tracks the active triage strategy so the queue UI can adapt
let currentStrategy = 'PRIORITY_FIRST';

// ── Tab navigation ─────────────────────────────────────────────────────────
document.querySelectorAll('.tab-btn').forEach(btn => {
  btn.addEventListener('click', () => {
    document.querySelectorAll('.tab-btn').forEach(b => b.classList.remove('active'));
    document.querySelectorAll('.tab-panel').forEach(p => p.classList.remove('active'));
    btn.classList.add('active');
    document.getElementById('tab-' + btn.dataset.tab).classList.add('active');
    if (btn.dataset.tab === 'audit')    fetchAudit();
    if (btn.dataset.tab === 'settings') fetchPreferences();
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

  tbody.querySelectorAll('[data-action]').forEach(btn => {
    btn.addEventListener('click', handleAction);
  });
}

function actionsFor(o) {
  if (o.status === 'PENDING') {
    const cancelBtn = `<button class="btn btn-cancel" data-action="cancel" data-id="${o.orderId}" data-clinician="${o.clinician}">Cancel</button>`;
    if (currentStrategy === 'LOAD_BALANCING') {
      // No manual claim in load-balancing mode — assignments are automatic
      return `<span class="auto-assign-label">Pending auto-assign</span>${cancelBtn}`;
    }
    return `<button class="btn btn-claim" data-action="claim" data-id="${o.orderId}">Claim</button>${cancelBtn}`;
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

  if (action === 'complete' && !staff) {
    alert('Please enter your Staff ID before completing an order.');
    return;
  }
  if (action === 'claim' && !staff) {
    alert('Please enter your Staff ID before claiming an order.');
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
    fetchBadge();
  } catch (err) {
    alert('Request failed: ' + err.message);
  }
}

// Poll every 3 seconds
fetchQueue();
setInterval(fetchQueue, 3000);
setInterval(fetchBadge, 5000);

// ── Change 1: Triage strategy selector ────────────────────────────────────
function updateQueueUIForStrategy() {
  const lbBtn = document.getElementById('lb-auto-assign-btn');
  if (currentStrategy === 'LOAD_BALANCING') {
    lbBtn.classList.remove('hidden');
  } else {
    lbBtn.classList.add('hidden');
  }
}

async function loadCurrentStrategy() {
  try {
    const res = await fetch(`${API_BASE}/api/triage/strategy`);
    if (!res.ok) return;
    const data = await res.json();
    currentStrategy = data.strategy || 'PRIORITY_FIRST';
    document.getElementById('triage-strategy').value = currentStrategy;
    updateQueueUIForStrategy();
  } catch (_) {}
}

document.getElementById('apply-strategy').addEventListener('click', async () => {
  const strategy = document.getElementById('triage-strategy').value;
  try {
    const res = await fetch(`${API_BASE}/api/triage/strategy`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ strategy })
    });
    const data = await res.json();
    if (!res.ok) { alert('Error: ' + (data.error || 'Unknown')); return; }
    currentStrategy = strategy;
    updateQueueUIForStrategy();
    // In load-balancing mode, immediately assign any already-pending orders
    if (strategy === 'LOAD_BALANCING') await runAutoAssign();
    fetchQueue();
  } catch (err) {
    alert('Request failed: ' + err.message);
  }
});

loadCurrentStrategy();

// Auto-assign button in queue panel (load-balancing mode only)
document.getElementById('lb-auto-assign-btn').addEventListener('click', async () => {
  await runAutoAssign();
  fetchQueue();
  fetchAudit();
});

async function runAutoAssign() {
  try {
    const res = await fetch(`${API_BASE}/api/staff/auto-assign`, { method: 'POST' });
    const data = await res.json();
    if (!res.ok) { alert('Auto-assign failed: ' + (data.error || 'Unknown')); return; }
    if (data.assigned > 0) alert(data.message);
  } catch (err) {
    alert('Auto-assign request failed: ' + err.message);
  }
}

// ── Change 2a: Badge counter ───────────────────────────────────────────────
async function fetchBadge() {
  try {
    const res = await fetch(`${API_BASE}/api/notifications/badge`);
    if (!res.ok) return;
    const data = await res.json();
    const count = data.count || 0;
    const el = document.getElementById('badge-counter');
    el.textContent = count;
    el.classList.toggle('hidden', count === 0);
    // also update settings page inline badge
    const si = document.getElementById('badge-settings');
    if (si) si.textContent = count;
  } catch (_) {}
}

fetchBadge();

// ── Change 2a: Notification settings ──────────────────────────────────────
async function fetchPreferences() {
  try {
    const res = await fetch(`${API_BASE}/api/notifications/preferences`);
    if (!res.ok) return;
    const data = await res.json();
    document.getElementById('pref-console').checked = data.console ?? true;
    document.getElementById('pref-inapp').checked   = data.inApp   ?? true;
    document.getElementById('pref-email').checked   = data.email   ?? false;
    fetchBadge();
  } catch (_) {}
}

document.getElementById('save-prefs-btn').addEventListener('click', async () => {
  const payload = {
    console: document.getElementById('pref-console').checked,
    inApp:   document.getElementById('pref-inapp').checked,
    email:   document.getElementById('pref-email').checked
  };
  const msg = document.getElementById('settings-message');
  try {
    const res = await fetch(`${API_BASE}/api/notifications/preferences`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload)
    });
    const data = await res.json();
    msg.textContent  = res.ok ? data.message : ('Error: ' + data.error);
    msg.className    = 'message ' + (res.ok ? 'success' : 'error');
    msg.classList.remove('hidden');
    setTimeout(() => msg.classList.add('hidden'), 3000);
  } catch (err) {
    msg.textContent = 'Network error: ' + err.message;
    msg.className   = 'message error';
    msg.classList.remove('hidden');
  }
});

document.getElementById('clear-badge-btn').addEventListener('click', async () => {
  try {
    await fetch(`${API_BASE}/api/notifications/badge/reset`, { method: 'POST' });
    fetchBadge();
  } catch (_) {}
});

// ── Change 3: Undo last command ────────────────────────────────────────────
document.getElementById('undo-btn').addEventListener('click', async () => {
  try {
    const res = await fetch(`${API_BASE}/api/orders/undo`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    });
    const data = await res.json();
    if (!res.ok) { alert('Undo failed: ' + (data.error || 'Unknown error')); return; }
    alert(data.message);
    fetchQueue();
    fetchAudit();
  } catch (err) {
    alert('Undo request failed: ' + err.message);
  }
});

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

// ── Staff management (load-balancing triage) ───────────────────────────────
async function fetchStaff() {
  try {
    const res = await fetch(`${API_BASE}/api/staff`);
    if (!res.ok) return;
    const staff = await res.json();
    renderStaffList(staff);
  } catch (_) {}
}

function renderStaffList(staff) {
  const ul = document.getElementById('staff-list');
  ul.innerHTML = '';
  if (!staff.length) {
    ul.innerHTML = '<li class="staff-empty">No staff registered yet.</li>';
    return;
  }
  staff.forEach(id => {
    const li = document.createElement('li');
    li.className = 'staff-item';
    li.innerHTML = `<span>${id}</span>
      <button class="btn btn-cancel btn-sm" data-staff-remove="${id}">Remove</button>`;
    ul.appendChild(li);
  });
  ul.querySelectorAll('[data-staff-remove]').forEach(btn => {
    btn.addEventListener('click', async () => {
      const staffId = btn.dataset.staffRemove;
      await fetch(`${API_BASE}/api/staff/${encodeURIComponent(staffId)}`, { method: 'DELETE' });
      fetchStaff();
    });
  });
}

document.getElementById('add-staff-btn').addEventListener('click', async () => {
  const input = document.getElementById('new-staff-id');
  const staffId = input.value.trim();
  const msg = document.getElementById('staff-message');
  if (!staffId) { alert('Enter a Staff ID.'); return; }
  try {
    const res = await fetch(`${API_BASE}/api/staff`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ staffId })
    });
    const data = await res.json();
    msg.textContent = res.ok ? data.message : ('Error: ' + data.error);
    msg.className   = 'message ' + (res.ok ? 'success' : 'error');
    msg.classList.remove('hidden');
    setTimeout(() => msg.classList.add('hidden'), 3000);
    if (res.ok) {
      input.value = '';
      fetchStaff();
      // If load-balancing is active, auto-assign any pending orders to the new member
      if (currentStrategy === 'LOAD_BALANCING') { await runAutoAssign(); fetchQueue(); fetchAudit(); }
    }
  } catch (err) {
    alert('Request failed: ' + err.message);
  }
});

// Load staff list when Settings tab is opened
document.querySelectorAll('.tab-btn').forEach(btn => {
  if (btn.dataset.tab === 'settings') {
    btn.addEventListener('click', fetchStaff, { once: false });
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

  // newest first — keep original index for replay
  [...entries].reverse().forEach((e, reverseIdx) => {
    const originalIndex = entries.length - 1 - reverseIdx;
    const replayable = ['SUBMIT', 'CANCEL', 'CLAIM', 'COMPLETE'].includes(e.commandType);
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td>${new Date(e.timestamp).toLocaleString()}</td>
      <td><strong>${e.commandType}</strong></td>
      <td><code>${e.orderId}</code></td>
      <td>${e.actor}</td>
      <td>${replayable
        ? `<button class="btn btn-replay" data-log-index="${originalIndex}">Replay</button>`
        : '—'}</td>`;
    tbody.appendChild(tr);
  });

  // Change 3: replay handlers
  tbody.querySelectorAll('[data-log-index]').forEach(btn => {
    btn.addEventListener('click', handleReplay);
  });
}

async function handleReplay(e) {
  const index = e.currentTarget.dataset.logIndex;
  try {
    const res = await fetch(`${API_BASE}/api/orders/replay/${index}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' }
    });
    const data = await res.json();
    if (!res.ok) { alert('Replay failed: ' + (data.error || 'Unknown error')); return; }
    alert(data.message);
    fetchQueue();
    fetchAudit();
  } catch (err) {
    alert('Replay request failed: ' + err.message);
  }
}
