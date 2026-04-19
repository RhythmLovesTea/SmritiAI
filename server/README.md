# SmritiAI Local Laptop Server (FastAPI + Ollama)

This folder runs the **local AI server** on a Debian/Ubuntu laptop on the **same WiFi** as the phone.

## 1) Install Ollama (Debian/Ubuntu)

```bash
curl -fsSL https://ollama.com/install.sh | sh
```

Start Ollama:

```bash
ollama serve
```

Pull a small model:

```bash
ollama pull phi3:mini
```

## 2) Create Python environment

```bash
python3 -m venv .venv
source .venv/bin/activate
pip install -r requirements.txt
```

## 3) Run FastAPI

```bash
uvicorn main:app --host 0.0.0.0 --port 8000
```

Test:

```bash
curl -X POST http://127.0.0.1:8000/chat \
  -H 'Content-Type: application/json' \
  -d '{"query":"Who is this?","context":{"person":{"name":"Rahul","relationship":"Nephew","last_visit":"3 days ago"}}}'
```

## 4) Phone → laptop connection

On the laptop, find your WiFi IP:

```bash
ip -4 addr show
```

Look for something like `inet 192.168.1.5/24`.

In Android, set `BuildConfig.LOCAL_AI_BASE_URL` to:

```text
http://192.168.1.5:8000/
```

Notes:
- Both devices must be on the same WiFi.
- Allow port `8000` in firewall if enabled.

