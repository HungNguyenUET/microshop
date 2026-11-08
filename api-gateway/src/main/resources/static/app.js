// MicroShop UI - vanilla JS, goi API qua API Gateway (cung origin -> cookie session tu dong gui kem).
// Khong co access token nao o day: gateway tu gan token khi forward xuong service (TokenRelay).

const state = { me: null, products: [], polling: new Set() };
const $ = (sel) => document.querySelector(sel);
const vnd = (n) => new Intl.NumberFormat('vi-VN').format(n) + ' đ';
const esc = (s) => String(s ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
const STATUS_LABEL = {
  PENDING_PAYMENT: 'Chờ thanh toán', PAYMENT_PROCESSING: 'Đang thanh toán',
  PAID: 'Đã thanh toán', PAYMENT_FAILED: 'Thanh toán lỗi', SUCCESS: 'Thành công', FAILED: 'Thất bại',
};

function csrfToken() {
  const match = document.cookie.match(/(?:^|; )XSRF-TOKEN=([^;]+)/);
  return match ? decodeURIComponent(match[1]) : '';
}

// Wrapper cho fetch: gui header CSRF voi request thay doi du lieu, doc loi dang ProblemDetail
async function api(path, { method = 'GET', body } = {}) {
  const headers = { Accept: 'application/json' };
  if (body !== undefined) headers['Content-Type'] = 'application/json';
  if (method !== 'GET') headers['X-XSRF-TOKEN'] = csrfToken();
  const res = await fetch(path, { method, headers, body: body !== undefined ? JSON.stringify(body) : undefined });
  if (res.status === 401) { state.me = null; renderAccount(); throw new Error('Bạn cần đăng nhập'); }
  if (res.status === 204) return null;
  const data = res.headers.get('content-type')?.includes('json') ? await res.json() : null;
  if (!res.ok) throw new Error(data?.detail || data?.title || `Lỗi ${res.status}`);
  return data;
}

function flash(message, isError = false) {
  const el = $('#flash');
  el.textContent = message;
  el.className = 'flash' + (isError ? ' error' : '');
  el.hidden = false;
  clearTimeout(flash.timer);
  flash.timer = setTimeout(() => (el.hidden = true), 4000);
}

const run = (fn) => async (...args) => { try { await fn(...args); } catch (e) { flash(e.message, true); } };

// ------------------------------------------------------------------ account
async function loadMe() {
  try {
    const res = await fetch('/api/me', { headers: { Accept: 'application/json' } });
    state.me = res.ok ? await res.json() : null;
  } catch { state.me = null; }
  renderAccount();
}

function renderAccount() {
  const el = $('#account');
  const isAdmin = state.me?.roles?.includes('ADMIN');
  document.querySelectorAll('[data-auth]').forEach((b) => (b.hidden = !state.me));
  document.querySelectorAll('[data-admin]').forEach((b) => (b.hidden = !isAdmin));
  if (!state.me) {
    el.innerHTML = '<button class="primary" id="login">Đăng nhập</button>';
    $('#login').onclick = () => (location.href = '/oauth2/authorization/microshop');
    return;
  }
  const initial = esc((state.me.name || state.me.username).charAt(0).toUpperCase());
  el.innerHTML = `
    <span class="balance" id="balance"></span>
    <span class="avatar">${state.me.picture ? `<img src="${esc(state.me.picture)}" alt="">` : initial}</span>
    <span>${esc(state.me.name)}</span>
    ${state.me.roles.map((r) => `<span class="role">${esc(r)}</span>`).join('')}
    <form method="post" action="/logout" style="margin:0">
      <input type="hidden" name="_csrf" value="${esc(csrfToken())}">
      <button type="submit">Đăng xuất</button>
    </form>`;
  refreshBalance();
  refreshCartCount();
}

async function refreshBalance() {
  if (!state.me) return;
  try { $('#balance').textContent = 'Ví: ' + vnd((await api('/api/wallet')).balance); } catch { /* ignore */ }
}

// ------------------------------------------------------------------ products
async function loadProducts() {
  const q = encodeURIComponent($('#search').value.trim());
  const page = await api(`/api/products?q=${q}&size=50`);
  state.products = page.content;
  $('#products').innerHTML = page.content.length ? page.content.map((p) => `
    <article class="card">
      <span class="cat">${esc(p.category)} · ${esc(p.sku)}</span>
      <span class="name">${esc(p.name)}</span>
      <span class="desc">${esc(p.description)}</span>
      <span class="price">${vnd(p.price)}</span>
      <button class="primary" data-add="${p.id}">Thêm vào giỏ</button>
    </article>`).join('') : '<div class="empty">Không tìm thấy sản phẩm.</div>';
}

const addToCart = run(async (productId) => {
  if (!state.me) { location.href = '/oauth2/authorization/microshop'; return; }
  await api('/api/cart/items', { method: 'POST', body: { productId, quantity: 1 } });
  flash('Đã thêm vào giỏ hàng');
  refreshCartCount();
});

// ------------------------------------------------------------------ cart
async function refreshCartCount() {
  try {
    const cart = await api('/api/cart');
    const count = cart.items.reduce((n, i) => n + i.quantity, 0);
    $('#cart-count').textContent = count || '';
  } catch { /* ignore */ }
}

async function loadCart() {
  const cart = await api('/api/cart');
  $('#cart-count').textContent = cart.items.reduce((n, i) => n + i.quantity, 0) || '';
  if (!cart.items.length) { $('#cart').innerHTML = '<div class="empty">Giỏ hàng trống.</div>'; return; }
  $('#cart').innerHTML = `
    <div class="table-wrap"><table>
      <thead><tr><th>Sản phẩm</th><th class="num">Đơn giá</th><th>Số lượng</th><th class="num">Thành tiền</th><th></th></tr></thead>
      <tbody>${cart.items.map((i) => `
        <tr>
          <td>${esc(i.productName)}</td>
          <td class="num">${vnd(i.unitPrice)}</td>
          <td><span class="qty">
            <button data-qty="${i.productId}" data-value="${i.quantity - 1}" ${i.quantity <= 1 ? 'disabled' : ''}>−</button>
            ${i.quantity}
            <button data-qty="${i.productId}" data-value="${i.quantity + 1}">+</button></span></td>
          <td class="num">${vnd(i.lineTotal)}</td>
          <td><button data-remove="${i.productId}">Xóa</button></td>
        </tr>`).join('')}</tbody>
    </table></div>
    <div class="summary"><span>Tổng: <b class="price">${vnd(cart.total)}</b></span>
      <button class="primary" id="checkout">Đặt hàng</button></div>`;
  $('#checkout').onclick = checkout;
}

const changeQty = run(async (productId, quantity) => {
  await api(`/api/cart/items/${productId}`, { method: 'PUT', body: { quantity } });
  loadCart();
});

const removeItem = run(async (productId) => {
  await api(`/api/cart/items/${productId}`, { method: 'DELETE' });
  loadCart();
});

const checkout = run(async () => {
  const order = await api('/api/orders', { method: 'POST' });
  flash(`Đã tạo đơn #${order.id.slice(0, 8)}, tổng ${vnd(order.totalAmount)}. Bấm "Thanh toán" để trả tiền.`);
  $('#cart-count').textContent = '';
  showTab('orders');
});

// ------------------------------------------------------------------ orders + payment
async function loadOrders() {
  const orders = await api('/api/orders');
  $('#orders').innerHTML = orders.length ? `
    <div class="table-wrap"><table>
      <thead><tr><th>Mã đơn</th><th>Sản phẩm</th><th class="num">Tổng</th><th>Trạng thái</th><th></th></tr></thead>
      <tbody>${orders.map((o) => `
        <tr>
          <td>#${o.id.slice(0, 8)}<div class="muted">${new Date(o.createdAt).toLocaleString('vi-VN')}</div></td>
          <td>${o.items.map((i) => `${esc(i.productName)} × ${i.quantity}`).join('<br>')}</td>
          <td class="num">${vnd(o.totalAmount)}</td>
          <td><span class="status ${o.status}">${STATUS_LABEL[o.status]}</span>
              ${o.failureReason ? `<div class="muted">${esc(o.failureReason)}</div>` : ''}</td>
          <td>${['PENDING_PAYMENT', 'PAYMENT_FAILED'].includes(o.status)
              ? `<button class="primary" data-pay="${o.id}">Thanh toán</button>` : ''}</td>
        </tr>`).join('')}</tbody>
    </table></div>` : '<div class="empty">Chưa có đơn hàng.</div>';
}

// Thanh toan la BAT DONG BO: gateway -> order-service tra 202, ket qua den qua Kafka -> polling trang thai
const pay = run(async (orderId) => {
  await api(`/api/orders/${orderId}/payment`, { method: 'POST' });
  flash('Đã gửi yêu cầu thanh toán, đang chờ payment-service xử lý...');
  loadOrders();
  pollOrder(orderId);
});

function pollOrder(orderId, attempt = 0) {
  if (state.polling.has(orderId) && attempt === 0) return;
  state.polling.add(orderId);
  setTimeout(async () => {
    try {
      const order = await api(`/api/orders/${orderId}`);
      if (order.status === 'PAYMENT_PROCESSING' && attempt < 20) { pollOrder(orderId, attempt + 1); return; }
      state.polling.delete(orderId);
      if (order.status === 'PAID') flash(`Đơn #${orderId.slice(0, 8)} đã thanh toán thành công`);
      if (order.status === 'PAYMENT_FAILED') flash(`Đơn #${orderId.slice(0, 8)}: ${order.failureReason}`, true);
      if (currentTab() === 'orders') loadOrders();
      refreshBalance();
    } catch { state.polling.delete(orderId); }
  }, 700);
}

// ------------------------------------------------------------------ wallet
async function loadWallet() {
  const [wallet, txs] = await Promise.all([api('/api/wallet'), api('/api/wallet/transactions')]);
  $('#wallet').innerHTML = `
    <div class="card" style="max-width:360px">
      <span class="muted">Số dư hiện tại</span>
      <span class="price" style="font-size:24px">${vnd(wallet.balance)}</span>
      <button class="primary" id="topup">Nạp 10.000.000 đ (demo)</button>
    </div>
    <h2>Lịch sử giao dịch</h2>
    ${txs.length ? `<div class="table-wrap"><table>
      <thead><tr><th>Thời gian</th><th>Loại</th><th class="num">Số tiền</th><th>Kết quả</th></tr></thead>
      <tbody>${txs.map((t) => `
        <tr><td>${new Date(t.createdAt).toLocaleString('vi-VN')}</td>
            <td>${t.type === 'TOP_UP' ? 'Nạp tiền' : 'Thanh toán đơn #' + (t.orderId || '').slice(0, 8)}</td>
            <td class="num">${t.type === 'TOP_UP' ? '+' : '−'}${vnd(t.amount)}</td>
            <td><span class="status ${t.status}">${STATUS_LABEL[t.status]}</span>
                ${t.reason ? `<div class="muted">${esc(t.reason)}</div>` : ''}</td></tr>`).join('')}</tbody>
    </table></div>` : '<div class="empty">Chưa có giao dịch.</div>'}`;
  $('#topup').onclick = run(async () => {
    await api('/api/wallet/top-up', { method: 'POST', body: { amount: 10000000 } });
    flash('Nạp tiền thành công');
    loadWallet();
    refreshBalance();
  });
}

// ------------------------------------------------------------------ notifications
async function loadNotifications() {
  const items = await api('/api/notifications');
  $('#notifications').innerHTML = items.length ? items.map((n) => `
    <div class="notification"><b>${esc(n.title)}</b>${esc(n.message)}
      <div class="muted">${new Date(n.createdAt).toLocaleString('vi-VN')}</div></div>`).join('')
    : '<div class="empty">Chưa có thông báo.</div>';
}

// ------------------------------------------------------------------ admin
async function loadAdmin() {
  const [users, orders] = await Promise.all([api('/api/admin/users'), api('/api/admin/orders')]);
  $('#admin-users').innerHTML = `<div class="table-wrap"><table>
    <thead><tr><th>Username</th><th>Đăng nhập bằng</th><th>Roles</th><th></th></tr></thead>
    <tbody>${users.map((u) => {
      const admin = u.roles.includes('ADMIN');
      return `<tr><td>${esc(u.username)}<div class="muted">${esc(u.displayName)}</div></td>
        <td>${esc(u.provider)}</td>
        <td>${u.roles.map((r) => `<span class="role">${esc(r)}</span>`).join(' ')}</td>
        <td>${u.id === state.me.id ? '<span class="muted">(bạn)</span>'
          : `<button data-role="${u.id}" data-admin-flag="${admin}">${admin ? 'Bỏ ADMIN' : 'Cấp ADMIN'}</button>`}</td></tr>`;
    }).join('')}</tbody></table></div>`;
  $('#admin-orders').innerHTML = orders.length ? `<div class="table-wrap"><table>
    <thead><tr><th>Mã đơn</th><th>Khách</th><th class="num">Tổng</th><th>Trạng thái</th></tr></thead>
    <tbody>${orders.map((o) => `<tr><td>#${o.id.slice(0, 8)}</td><td>${esc(o.username)}</td>
      <td class="num">${vnd(o.totalAmount)}</td><td><span class="status ${o.status}">${STATUS_LABEL[o.status]}</span></td></tr>`).join('')}
    </tbody></table></div>` : '<div class="empty">Chưa có đơn hàng.</div>';
}

const toggleAdmin = run(async (userId, isAdmin) => {
  const roles = isAdmin ? ['USER'] : ['USER', 'ADMIN'];
  await api(`/api/admin/users/${userId}/roles`, { method: 'PUT', body: { roles } });
  flash('Đã cập nhật quyền. User nhận quyền mới ở lần làm mới token kế tiếp (tối đa 5 phút).');
  loadAdmin();
});

$('#product-form').onsubmit = run(async (event) => {
  event.preventDefault();
  const form = new FormData(event.target);
  const body = Object.fromEntries(form.entries());
  body.price = Number(body.price);
  await api('/api/products', { method: 'POST', body });
  event.target.reset();
  flash('Đã thêm sản phẩm');
  loadProducts();
});

// ------------------------------------------------------------------ tabs + events
const loaders = { products: loadProducts, cart: loadCart, orders: loadOrders, wallet: loadWallet,
                  notifications: loadNotifications, admin: loadAdmin };
const currentTab = () => document.querySelector('.tabs button.active')?.dataset.tab;

function showTab(name) {
  document.querySelectorAll('.tabs button').forEach((b) => b.classList.toggle('active', b.dataset.tab === name));
  document.querySelectorAll('main > section').forEach((s) => (s.hidden = s.id !== 'tab-' + name));
  run(loaders[name])();
}

$('#tabs').onclick = (e) => { const tab = e.target.closest('button')?.dataset.tab; if (tab) showTab(tab); };
$('#search').oninput = () => { clearTimeout(loadProducts.timer); loadProducts.timer = setTimeout(run(loadProducts), 300); };
document.body.addEventListener('click', (e) => {
  const t = e.target.closest('button');
  if (!t) return;
  if (t.dataset.add) addToCart(t.dataset.add);
  if (t.dataset.qty) changeQty(t.dataset.qty, Number(t.dataset.value));
  if (t.dataset.remove) removeItem(t.dataset.remove);
  if (t.dataset.pay) pay(t.dataset.pay);
  if (t.dataset.role) toggleAdmin(t.dataset.role, t.dataset.adminFlag === 'true');
});

if (new URLSearchParams(location.search).has('loginError')) flash('Đăng nhập thất bại', true);
loadMe();
run(loadProducts)();
