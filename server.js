/**
 * Mobile Chor - Companion Web Portal Server
 * Zero-dependency Node.js Server for Remote Anti-Theft Device Control & Live Telemetry Tracking
 * Run with: node server.js
 * Access at: http://localhost:3000 (or http://<your-ip>:3000)
 */

const http = require('http');
const url = require('url');

const PORT = 3000;

// Device State Store
let deviceTelemetry = {
    deviceId: "AWAITING_CONNECT",
    model: "No Device Connected Yet",
    manufacturer: "Mobile Chor",
    androidVersion: "Android 14",
    batteryPercent: 100,
    isCharging: false,
    networkType: "WiFi",
    wifiSsid: "Online",
    latitude: 23.8103,
    longitude: 90.4125,
    accuracy: 10,
    isLostMode: false,
    isAlarmPlaying: false,
    isDeviceAdminActive: false,
    simOperator: "Active SIM",
    lastSeen: Date.now(),
    ownerName: "Registered Owner",
    ownerPhone: "+8801700000000",
    emergencyPhone: "+8801500000000",
    customLostMessage: "This phone is lost or stolen. Please return to owner."
};

let commandQueue = [];
let evidenceVault = [];

const server = http.createServer((req, res) => {
    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;
    const method = req.method;

    // CORS Headers
    res.setHeader('Access-Control-Allow-Origin', '*');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, OPTIONS');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type');

    if (method === 'OPTIONS') {
        res.writeHead(200);
        return res.end();
    }

    // API 1: Device Telemetry Sync (App -> Web)
    if (pathname === '/api/device/telemetry' && method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                deviceTelemetry = { ...deviceTelemetry, ...data, lastSeen: Date.now() };
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ status: "OK", timestamp: Date.now() }));
            } catch (e) {
                res.writeHead(400);
                res.end("Invalid JSON");
            }
        });
        return;
    }

    // API 2: Device Poll Commands (App -> Web)
    if (pathname === '/api/device/commands' && method === 'GET') {
        if (commandQueue.length > 0) {
            const nextCmd = commandQueue.shift();
            res.writeHead(200, { 'Content-Type': 'application/json' });
            return res.end(JSON.stringify(nextCmd));
        } else {
            res.writeHead(200, { 'Content-Type': 'application/json' });
            return res.end(JSON.stringify({ command: null }));
        }
    }

    // API 3: Web Dashboard Issue Command (Web Portal -> App)
    if (pathname === '/api/portal/command' && method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                const cmdObj = {
                    id: "cmd_" + Date.now(),
                    command: data.command,
                    ...data
                };
                commandQueue.push(cmdObj);
                console.log(`[Remote Portal] Queued command: ${data.command}`);
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ status: "QUEUED", commandId: cmdObj.id }));
            } catch (e) {
                res.writeHead(400);
                res.end("Invalid Request");
            }
        });
        return;
    }

    // API 4: Evidence Photo Upload (App -> Web)
    if (pathname === '/api/evidence' && method === 'POST') {
        let body = '';
        req.on('data', chunk => body += chunk);
        req.on('end', () => {
            try {
                const data = JSON.parse(body);
                evidenceVault.unshift(data);
                if (evidenceVault.length > 20) evidenceVault.pop();
                res.writeHead(200, { 'Content-Type': 'application/json' });
                res.end(JSON.stringify({ status: "SAVED" }));
            } catch (e) {
                res.writeHead(400);
                res.end("Failed");
            }
        });
        return;
    }

    // API 5: Get Current Web Dashboard Data
    if (pathname === '/api/portal/status' && method === 'GET') {
        res.writeHead(200, { 'Content-Type': 'application/json' });
        return res.end(JSON.stringify({
            telemetry: deviceTelemetry,
            evidence: evidenceVault,
            pendingCommands: commandQueue.length
        }));
    }

    // HTML Web Dashboard Interface
    if (pathname === '/' && method === 'GET') {
        const html = `
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Mobile Chor - Owner Remote Security Portal</title>
            <style>
                :root { --primary: #2563eb; --bg: #f8fafc; --surface: #ffffff; --border: #e2e8f0; --text: #0f172a; --muted: #64748b; --danger: #ef4444; --success: #10b981; --warning: #f97316; }
                body { font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif; background: var(--bg); color: var(--text); margin: 0; padding: 20px; }
                .container { max-width: 1000px; margin: 0 auto; }
                .header { display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid var(--border); padding-bottom: 15px; margin-bottom: 20px; }
                .title { font-size: 24px; font-weight: bold; color: var(--primary); display: flex; align-items: center; gap: 10px; }
                .status-pill { padding: 6px 14px; border-radius: 20px; font-size: 13px; font-weight: bold; }
                .status-online { background: #d1fae5; color: #059669; }
                .grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(220px, 1fr)); gap: 15px; margin-bottom: 25px; }
                .card { background: var(--surface); border: 1px solid var(--border); border-radius: 16px; padding: 18px; box-shadow: 0 2px 4px rgba(0,0,0,0.02); }
                .card-label { font-size: 12px; color: var(--muted); text-transform: uppercase; font-weight: bold; margin-bottom: 6px; }
                .card-value { font-size: 18px; font-weight: bold; color: var(--text); }
                .actions { display: grid; grid-template-columns: repeat(auto-fit, minmax(180px, 1fr)); gap: 12px; margin-bottom: 25px; }
                .btn { border: none; padding: 14px; border-radius: 12px; font-weight: bold; font-size: 14px; cursor: pointer; transition: 0.2s; color: white; display: flex; justify-content: center; align-items: center; gap: 8px; }
                .btn-primary { background: var(--primary); }
                .btn-danger { background: var(--danger); }
                .btn-warning { background: var(--warning); }
                .btn-success { background: var(--success); }
                .btn:hover { opacity: 0.9; transform: translateY(-1px); }
                .section-title { font-size: 18px; font-weight: bold; margin-bottom: 12px; border-bottom: 1px solid var(--border); padding-bottom: 8px; }
                .evidence-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(260px, 1fr)); gap: 15px; }
                .evidence-card { background: var(--surface); border: 1px solid var(--border); border-radius: 14px; overflow: hidden; }
                .evidence-img { width: 100%; height: 180px; object-fit: cover; background: #000; }
                .evidence-info { padding: 12px; font-size: 12px; color: var(--muted); }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <div class="title">🛡️ Mobile Chor Remote Security Portal</div>
                    <div id="statusPill" class="status-pill status-online">Live Sync Connected</div>
                </div>

                <!-- Live Telematics Grid -->
                <div class="grid">
                    <div class="card">
                        <div class="card-label">Target Device</div>
                        <div class="card-value" id="devModel">Loading...</div>
                        <div style="font-size:12px; color:var(--muted); margin-top:4px;" id="devId"></div>
                    </div>
                    <div class="card">
                        <div class="card-label">Battery Level</div>
                        <div class="card-value" id="devBattery">--%</div>
                        <div style="font-size:12px; color:var(--muted); margin-top:4px;" id="devNetwork">--</div>
                    </div>
                    <div class="card">
                        <div class="card-label">Protection Mode</div>
                        <div class="card-value" id="devMode">Normal</div>
                        <div style="font-size:12px; color:var(--muted); margin-top:4px;" id="devAdmin">Admin OK</div>
                    </div>
                    <div class="card">
                        <div class="card-label">GPS Location</div>
                        <div class="card-value" id="devGPS">-- , --</div>
                        <a id="devMapLink" href="#" target="_blank" style="font-size:12px; color:var(--primary); font-weight:bold; display:block; margin-top:4px;">View Live Google Maps</a>
                    </div>
                </div>

                <!-- Remote Action Buttons -->
                <div class="section-title">⚡ Remote Emergency Controls</div>
                <div class="actions">
                    <button class="btn btn-danger" onclick="sendCmd('LOCK')">🔒 Engage Lost Mode</button>
                    <button class="btn btn-success" onclick="sendCmd('UNLOCK')">🔓 Unlock Device</button>
                    <button class="btn btn-warning" onclick="sendCmd('ALARM')">🚨 Sound Emergency Siren</button>
                    <button class="btn btn-primary" onclick="sendCmd('STOP_ALARM')">🔇 Stop Siren</button>
                    <button class="btn btn-primary" onclick="sendCmd('CAPTURE_PHOTO')">📸 Take Intruder Photo</button>
                    <button class="btn btn-danger" onclick="if(confirm('ARE YOU SURE? This will permanently erase evidence.')) sendCmd('WIPE_FULL')">🗑️ Factory Reset Wipe</button>
                </div>

                <!-- Evidence Photo Vault -->
                <div class="section-title">📸 Intruder Photo Evidence Feed</div>
                <div id="evidenceList" class="evidence-grid">
                    <div style="color:var(--muted); padding:20px; text-align:center; grid-column: 1/-1;">Awaiting intrusion attempts...</div>
                </div>
            </div>

            <script>
                async function updateStatus() {
                    try {
                        const res = await fetch('/api/portal/status');
                        const data = await res.json();
                        const t = data.telemetry;

                        document.getElementById('devModel').innerText = (t.manufacturer || '') + ' ' + (t.model || 'Connected Device');
                        document.getElementById('devId').innerText = 'ID: ' + (t.deviceId || 'N/A');
                        document.getElementById('devBattery').innerText = (t.batteryPercent || 100) + '% ' + (t.isCharging ? '⚡' : '');
                        document.getElementById('devNetwork').innerText = (t.networkType || 'Cellular') + ' (' + (t.wifiSsid || 'Connected') + ')';
                        document.getElementById('devMode').innerText = t.isLostMode ? '🚨 LOST MODE' : '🛡️ Protected';
                        document.getElementById('devMode').style.color = t.isLostMode ? 'var(--danger)' : 'var(--success)';
                        document.getElementById('devAdmin').innerText = t.isDeviceAdminActive ? 'Device Admin Active ✓' : 'Admin Off';

                        document.getElementById('devGPS').innerText = (t.latitude || 0).toFixed(4) + ', ' + (t.longitude || 0).toFixed(4);
                        document.getElementById('devMapLink').href = 'https://maps.google.com/?q=' + t.latitude + ',' + t.longitude;

                        // Render evidence items
                        const listEl = document.getElementById('evidenceList');
                        if (data.evidence && data.evidence.length > 0) {
                            listEl.innerHTML = data.evidence.map(e => \`
                                <div class="evidence-card">
                                    <img class="evidence-img" src="\${e.photoUrl}" alt="Intruder Photo" />
                                    <div class="evidence-info">
                                        <strong>Trigger:</strong> \${e.triggerReason}<br/>
                                        <strong>Time:</strong> \${new Date(e.timestamp).toLocaleString()}<br/>
                                        <strong>Battery:</strong> \${e.batteryAtCapture}% | GPS: \${e.latitude}, \${e.longitude}
                                    </div>
                                </div>
                            \`).join('');
                        }
                    } catch (e) {}
                }

                async function sendCmd(command) {
                    await fetch('/api/portal/command', {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json' },
                        body: JSON.stringify({ command: command })
                    });
                    alert('Command "' + command + '" dispatched to device!');
                    setTimeout(updateStatus, 1000);
                }

                setInterval(updateStatus, 2000);
                updateStatus();
            </script>
        </body>
        </html>
        `;
        res.writeHead(200, { 'Content-Type': 'text/html' });
        return res.end(html);
    }

    res.writeHead(404);
    res.end('Not Found');
});

server.listen(PORT, () => {
    console.log(`====================================================`);
    console.log(`🛡️ Mobile Chor Companion Security Portal running on port ${PORT}`);
    console.log(`👉 Access Portal at: http://localhost:${PORT}`);
    console.log(`====================================================`);
});
