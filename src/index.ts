import express, { Request, Response } from 'express';
import fs from 'fs';
import path from 'path';
import crypto from 'crypto';

const app = express();
const PORT = parseInt(process.env.PORT || '8080', 10);
const DATA_DIR = process.env.DATA_DIR || path.join(__dirname, '../data');
const DATA_FILE = path.join(DATA_DIR, 'urls.json');

// In-memory store: code -> long_url
const store = new Map<string, string>();

// Ensure data directory exists and load persistent data
function initStorage() {
  try {
    if (!fs.existsSync(DATA_DIR)) {
      fs.mkdirSync(DATA_DIR, { recursive: true });
    }
    if (fs.existsSync(DATA_FILE)) {
      const data = fs.readFileSync(DATA_FILE, 'utf-8');
      const parsed = JSON.parse(data);
      for (const [code, url] of Object.entries(parsed)) {
        if (typeof url === 'string') {
          store.set(code, url);
        }
      }
      console.log(`[Storage] Loaded ${store.size} URLs from ${DATA_FILE}`);
    }
  } catch (err) {
    console.warn(`[Storage] Could not initialize persistence storage:`, err);
  }
}

// Persist in-memory store to disk
function saveToDisk() {
  try {
    if (!fs.existsSync(DATA_DIR)) {
      fs.mkdirSync(DATA_DIR, { recursive: true });
    }
    const obj = Object.fromEntries(store);
    fs.writeFileSync(DATA_FILE, JSON.stringify(obj, null, 2), 'utf-8');
  } catch (err) {
    console.error(`[Storage] Failed to save to disk:`, err);
  }
}

// Generate 8-character random code [A-Za-z0-9]
const CHARS = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789';
function generateCode(): string {
  let code = '';
  const randomBytes = crypto.randomBytes(8);
  for (let i = 0; i < 8; i++) {
    code += CHARS[randomBytes[i] % CHARS.length];
  }
  return code;
}

initStorage();

app.use(express.json());

// Root endpoint: metadata
app.get('/', (_req: Request, res: Response) => {
  res.json({
    service: 'pastebin-typescript',
    description: 'Mini URL shortener pastebin API in TypeScript',
    docs: '/paste',
  });
});

// Health endpoint for HEALTHCHECK
app.get('/health', (_req: Request, res: Response) => {
  res.status(200).json({ status: 'ok' });
});

// POST /paste: create short URL
app.post('/paste', (req: Request, res: Response) => {
  const { url } = req.body || {};

  if (!url || typeof url !== 'string' || url.trim() === '') {
    res.status(400).json({ error: 'url is required' });
    return;
  }

  const trimmedUrl = url.trim();
  if (!trimmedUrl.startsWith('http://') && !trimmedUrl.startsWith('https://')) {
    res.status(400).json({ error: 'url must be a valid http(s) URL' });
    return;
  }

  let code: string;
  let attempts = 0;
  do {
    code = generateCode();
    attempts++;
  } while (store.has(code) && attempts < 10);

  store.set(code, trimmedUrl);
  saveToDisk();

  res.status(201).json({
    code,
    short_url: `/${code}`,
    long_url: trimmedUrl,
  });
});

// GET /:code: redirect to long URL
app.get('/:code', (req: Request, res: Response) => {
  const rawCode = req.params.code;
  const code = Array.isArray(rawCode) ? rawCode[0] : rawCode;

  // Code must be exactly 8 alphanumeric characters
  if (!code || !/^[A-Za-z0-9]{8}$/.test(code)) {
    res.status(404).json({ error: 'not found' });
    return;
  }

  const longUrl = store.get(code);
  if (!longUrl) {
    res.status(404).json({ error: 'not found' });
    return;
  }

  res.redirect(302, longUrl);
});

// 404 for unknown endpoints
app.use((_req: Request, res: Response) => {
  res.status(404).json({ error: 'not found' });
});

app.listen(PORT, '0.0.0.0', () => {
  console.log(`[Server] Pastebin listening on http://0.0.0.0:${PORT}`);
});
