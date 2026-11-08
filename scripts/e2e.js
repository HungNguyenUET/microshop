// E2E smoke test cho MicroShop (stack phai dang chay): cd scripts && npm run e2e
// Lan dau: cd scripts && npm install && npx playwright install chromium
const { chromium } = require('playwright');
const BASE = process.env.BASE || 'http://localhost:8080';
const sleep = ms => new Promise(r => setTimeout(r, ms));
let failed = 0;
const check = (ok, msg) => { console.log((ok ? 'PASS ' : 'FAIL ') + msg); if (!ok) failed++; };

async function login(browser, user) {
  const ctx = await browser.newContext();
  const page = await ctx.newPage();
  await page.goto(BASE + '/oauth2/authorization/microshop');      // -> redirect sang auth-service:9000/login
  await page.fill('#username', user);
  await page.fill('#password', user);
  await page.click('button[type=submit]');
  await page.waitForURL(BASE + '/**');
  await page.waitForLoadState('networkidle');
  // Goi API giong app.js: cookie session + header X-XSRF-TOKEN
  const api = (method, path, body) => page.evaluate(async ([method, path, body]) => {
    const xsrf = document.cookie.split('; ').find(c => c.startsWith('XSRF-TOKEN='))?.split('=')[1];
    const r = await fetch(path, { method, headers: { 'Content-Type': 'application/json',
      ...(xsrf ? { 'X-XSRF-TOKEN': decodeURIComponent(xsrf) } : {}) }, body: body ? JSON.stringify(body) : undefined });
    const text = await r.text();
    let json; try { json = JSON.parse(text); } catch { json = text; }
    return { status: r.status, body: json };
  }, [method, path, body]);
  return { ctx, page, api };
}

async function waitOrder(api, id, statuses) {
  for (let i = 0; i < 30; i++) {
    const o = (await api('GET', '/api/orders/' + id)).body;
    if (statuses.includes(o.status)) return o;
    await sleep(500);
  }
  return (await api('GET', '/api/orders/' + id)).body;
}

(async () => {
  const browser = await chromium.launch();

  // 0. Khach chua login: xem san pham OK, /api/cart -> 401
  const anon = await (await browser.newContext()).request;
  const prods = await anon.get(BASE + '/api/products?size=20');
  const products = (await prods.json()).content;
  check(prods.status() === 200 && products.length >= 8, `anonymous GET /api/products -> ${prods.status()}, ${products.length} san pham`);
  check((await anon.get(BASE + '/api/cart')).status() === 401, 'anonymous GET /api/cart -> 401');

  // 1. Login user/user qua SSO (auth-service)
  const { page, api } = await login(browser, 'user');
  const me = (await api('GET', '/api/me')).body;
  check(me.username === 'user' && me.roles.includes('USER'), `login user -> roles ${JSON.stringify(me.roles)}`);
  check((await api('GET', '/api/admin/orders')).status === 403, 'user goi /api/admin/orders -> 403');

  const wallet0 = (await api('GET', '/api/wallet')).body.balance;
  await api('DELETE', '/api/cart');

  // 2. Happy path: 2 x Ban phim + 1 Chuot -> dat hang -> thanh toan
  const cheap = products.filter(p => p.price < 3000000).slice(0, 2);
  let r = await api('POST', '/api/cart/items', { productId: cheap[0].id, quantity: 2 });
  check(r.status === 200 || r.status === 201, `them ${cheap[0].name} x2 vao gio -> ${r.status}`);
  await api('POST', '/api/cart/items', { productId: cheap[1].id, quantity: 1 });
  const cart = (await api('GET', '/api/cart')).body;
  check(cart.items.length === 2, `gio hang co ${cart.items.length} dong, tong ${cart.total}`);

  r = await api('POST', '/api/orders');
  check(r.status === 201 && r.body.status === 'PENDING_PAYMENT', `dat hang -> ${r.status} ${r.body.status}`);
  const order1 = r.body;
  check((await api('GET', '/api/cart')).body.items.length === 0, 'gio hang trong sau khi dat');
  r = await api('POST', `/api/orders/${order1.id}/payment`);
  check(r.status === 202, `POST /payment -> ${r.status} ${r.body.status}`);
  const paid = await waitOrder(api, order1.id, ['PAID', 'PAYMENT_FAILED']);
  check(paid.status === 'PAID', `order qua Kafka -> ${paid.status}`);
  const wallet1 = (await api('GET', '/api/wallet')).body.balance;
  check(Number(wallet0) - Number(wallet1) === Number(order1.totalAmount), `vi bi tru ${Number(wallet0) - Number(wallet1)} (= ${order1.totalAmount})`);

  // 3. Het tien: mua Laptop (25.990.000) khi vi con < 26tr
  const laptop = products.find(p => p.sku === 'LP-01');
  await api('POST', '/api/cart/items', { productId: laptop.id, quantity: 1 });
  const order2 = (await api('POST', '/api/orders')).body;
  await api('POST', `/api/orders/${order2.id}/payment`);
  const failedOrder = await waitOrder(api, order2.id, ['PAID', 'PAYMENT_FAILED']);
  check(failedOrder.status === 'PAYMENT_FAILED', `laptop -> ${failedOrder.status}: ${failedOrder.failureReason}`);

  // 4. Nap tien -> thanh toan lai -> PAID
  r = await api('POST', '/api/wallet/top-up', { amount: 10000000 });
  check(r.status === 200, `nap 10.000.000 -> ${r.status}, so du ${r.body.balance}`);
  await api('POST', `/api/orders/${order2.id}/payment`);
  const retry = await waitOrder(api, order2.id, ['PAID']);
  check(retry.status === 'PAID', `thanh toan lai -> ${retry.status}`);

  // 5. Thanh toan lai don da PAID -> 409
  r = await api('POST', `/api/orders/${order2.id}/payment`);
  check(r.status === 409, `thanh toan don da PAID -> ${r.status}`);

  // 6. Notification-service nhan event tu Kafka
  await sleep(1000);
  const notes = (await api('GET', '/api/notifications')).body;
  const list = notes.content || notes;
  check(list.length >= 5, `co ${list.length} thong bao: ${list.slice(0, 5).map(n => n.type || n.title).join(', ')}`);

  // 7. Admin
  const admin = await login(browser, 'admin');
  r = await admin.api('GET', '/api/admin/orders');
  check(r.status === 200, `admin GET /api/admin/orders -> ${r.status}`);
  r = await admin.api('GET', '/api/admin/users');
  check(r.status === 200 && r.body.length >= 2, `admin GET /api/admin/users -> ${r.status}, ${r.body.length} users`);
  r = await admin.api('POST', '/api/products', { sku: 'E2E-' + Date.now(), name: 'San pham test', category: 'Test', price: 1000, description: 'e2e' });
  check(r.status === 201, `admin tao san pham -> ${r.status}`);
  r = await api('POST', '/api/products', { sku: 'X', name: 'X', category: 'X', price: 1 });
  check(r.status === 403, `user tao san pham -> ${r.status}`);

  // 8. Khong co XSRF token -> 403
  const noCsrf = await page.evaluate(async () => (await fetch('/api/cart', { method: 'DELETE' })).status);
  check(noCsrf === 403, `DELETE khong co X-XSRF-TOKEN -> ${noCsrf}`);

  // 9. UI + logout
  await page.goto(BASE + '/');
  await page.waitForSelector('#products .card');
  await page.screenshot({ path: process.env.SHOT || 'e2e-products.png', fullPage: false });
  await page.click('text=Đăng xuất');
  await page.waitForURL(BASE + '/**');
  const after = await page.evaluate(async () => (await fetch('/api/me')).status);
  check(after === 401, `sau logout /api/me -> ${after}`);

  await browser.close();
  console.log(failed ? `\n${failed} FAIL` : '\nALL PASS');
  process.exit(failed ? 1 : 0);
})().catch(e => { console.error(e); process.exit(1); });
