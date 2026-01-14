const API_URL = "http://localhost:8080/api/bot";
let currentTimeframe = 'tick';
let previousPrice = 0;
let lastSignalPrice = 0;

const tickCtx = document.getElementById('tickChart').getContext('2d');
const tickChart = new Chart(tickCtx, {
    type: 'line',
    data: { labels: [], datasets: [{ data: [], borderColor: '#0ecb81', backgroundColor: 'rgba(14,203,129,0.1)', borderWidth: 2, fill: true, pointRadius: 0 }] },
    options: { responsive: true, maintainAspectRatio: false, animation: false, scales: { x: {display: false}, y: {grid: {color: '#232731'}, ticks: {color: '#848e9c'}} }, plugins: {legend: {display: false}} }
});

const priceCtx = document.getElementById('priceChart').getContext('2d');
const priceChart = new Chart(priceCtx, {
    type: 'line',
    data: { labels: [], datasets: [{ data: [], borderColor: '#f0b90b', borderWidth: 2, pointRadius: 2 }] },
    options: { responsive: true, maintainAspectRatio: false, scales: { x: {display: false}, y: {grid: {color: '#232731'}, ticks: {color: '#848e9c'}} }, plugins: {legend: {display: false}} }
});

setInterval(async () => {
    try {
        const priceRes = await fetch(`${API_URL}/price`);
        const priceText = await priceRes.text();
        const currentPrice = parseFloat(priceText);

        if (currentPrice > 0) {
            updatePriceDisplay(currentPrice);
            updateTickChart(currentPrice);

            const signalBox = document.getElementById('displaySignal');

            if (lastSignalPrice === 0) lastSignalPrice = currentPrice;

            const diff = currentPrice - lastSignalPrice;
            const threshold = currentPrice * 0.0001;

            if (diff > threshold) {
                signalBox.innerText = "BUY";
                signalBox.className = "stat-value text-buy";
                lastSignalPrice = currentPrice;
            }
            else if (diff < -threshold) {
                signalBox.innerText = "SELL";
                signalBox.className = "stat-value text-sell";
                lastSignalPrice = currentPrice;
            }
        }

        updateBalance();

    } catch (e) {}
}, 1000);

let previousBalance = null;

async function updateBalance() {
    try {
        const responseUsd = await fetch(`${API_URL}/balance`);
        const balanceText = await responseUsd.text();
        const currentBalance = parseFloat(balanceText);

        if (previousBalance !== null && Math.abs(currentBalance - previousBalance) > 0.01) {
            const diff = currentBalance - previousBalance;
            const isBuy = diff < 0;

            const currentPriceStr = document.getElementById('displayPrice').innerText.replace('$','').replace(',','');
            const currentPrice = parseFloat(currentPriceStr) || 0;

            addTradeRow(isBuy ? 'BUY' : 'SELL', currentPrice, Math.abs(diff));

            const logMsg = isBuy ? "Alım Emri Başarılı" : "Satış Emri Başarılı";
            addSystemLog("TRADE", logMsg, currentPrice);
        }

        previousBalance = currentBalance;
        document.getElementById('walletBalance').innerText = `$${currentBalance.toLocaleString('en-US', {minimumFractionDigits: 2})}`;

        const assetRes = await fetch(`${API_URL}/assets?symbol=BTC`);
        document.getElementById('assetBalance').innerText = parseFloat(await assetRes.text()).toFixed(4);

    } catch (e) { }
}

function addSystemLog(type, msg, price) {
    const tbody = document.getElementById('logTableBody');
    const color = type === 'TRADE' ? (msg.includes('Alım') ? 'text-success' : 'text-danger') : 'text-info';
    const priceDisplay = price ? `$${price.toFixed(2)}` : '-';

    const row = `<tr>
            <td class="text-secondary small">${new Date().toLocaleTimeString()}</td>
            <td class="fw-bold ${color}">${type}</td>
            <td>${msg}</td>
            <td class="text-end font-monospace">${priceDisplay}</td>
        </tr>`;

    tbody.insertAdjacentHTML('afterbegin', row);
    if(tbody.children.length > 50) tbody.lastChild.remove();
}

function addTradeRow(type, price, total) {
    const tbody = document.getElementById('tradeHistoryBody');
    if(tbody.innerHTML.includes("İşlem bekleniyor")) tbody.innerHTML = "";

    const color = type === 'BUY' ? 'text-success' : 'text-danger';
    const row = `<tr>
            <td class="text-secondary small">${new Date().toLocaleTimeString()}</td>
            <td class="fw-bold ${color}">${type}</td>
            <td class="font-monospace">$${price.toFixed(2)}</td>
            <td class="fw-bold text-white">$${total.toFixed(2)}</td>
        </tr>`;

    tbody.insertAdjacentHTML('afterbegin', row);
}

function changeTimeframe(tf) {
    currentTimeframe = tf;
    document.querySelectorAll('#timeframeGroup button').forEach(btn => btn.classList.remove('active'));
    event.target.classList.add('active');

    if (tf === 'tick') {
        document.getElementById('tickChartContainer').style.display = 'block';
        document.getElementById('priceChartContainer').style.display = 'none';
    } else {
        document.getElementById('tickChartContainer').style.display = 'none';
        document.getElementById('priceChartContainer').style.display = 'block';
        loadKlines(tf);
    }
}

async function loadKlines(interval) {
    try {
        const res = await fetch(`${API_URL}/klines?interval=${interval}&limit=50`);
        const data = await res.json();
        priceChart.data.labels = data.map(d => new Date(d.time).toLocaleTimeString());
        priceChart.data.datasets[0].data = data.map(d => d.close);
        priceChart.update();
    } catch (e) { console.error("Klines hatası", e); }
}

function updatePriceDisplay(price) {
    const el = document.getElementById('displayPrice');
    el.innerText = `$${price.toLocaleString('en-US', {minimumFractionDigits: 2, maximumFractionDigits: 2})}`;
    if (previousPrice > 0) {
        el.className = price > previousPrice ? "stat-value price-up" : (price < previousPrice ? "stat-value price-down" : "stat-value text-white");
    }
    previousPrice = price;
}

function updateTickChart(price) {
    tickChart.data.labels.push("");
    tickChart.data.datasets[0].data.push(price);
    if(tickChart.data.labels.length > 100) {
        tickChart.data.labels.shift();
        tickChart.data.datasets[0].data.shift();
    }
    tickChart.update('none');
}

function setPrice(val) { document.getElementById('priceInput').value = val; simulatePrice(); }

async function simulatePrice() {
    const price = document.getElementById('priceInput').value;
    await fetch(`${API_URL}/price?symbol=BTC&price=${price}`, {method: 'POST'});
    addSystemLog("INFO", "Fiyat Simüle Edildi", parseFloat(price));
}

async function changeStrategy(type) {
    await fetch(`${API_URL}/strategy?type=${type}`, {method: 'POST'});
    document.getElementById('strategyBadge').innerText = type;
    addSystemLog("SYSTEM", `Strateji Değişti: ${type}`);
}

let simActive = false;
async function toggleSim() {
    simActive = !simActive;
    await fetch(`${API_URL}/simulation?active=${simActive}`, {method: 'POST'});
    const btn = document.getElementById('simBtn');
    btn.innerText = simActive ? "Durdur" : "Otomatik Akış";
    btn.className = simActive ? "btn btn-danger w-100" : "btn btn-outline-warning w-100";
    addSystemLog("SYSTEM", simActive ? "Simülasyon Başlatıldı" : "Simülasyon Durduruldu");
}

async function toggleAutoPilot() {
    const en = document.getElementById('autoPilotSwitch').checked;
    await fetch(`${API_URL}/autopilot?enable=${en}`, {method: 'POST'});
    addSystemLog("SYSTEM", `AutoPilot: ${en ? 'AKTİF' : 'PASİF'}`);
}

async function toggleRealMarket() {
    const en = document.getElementById('realMarketSwitch').checked;
    await fetch(`${API_URL}/realmarket?active=${en}`, {method: 'POST'});
    addSystemLog("SYSTEM", `Gerçek Piyasa: ${en ? 'AKTİF' : 'KAPALI'}`);
}