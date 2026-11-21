# Winkerly

AI All-Rounder multi-model dengan backend FastAPI + frontend Flutter.

## Setup
1. Salin `backend/.env.example` ke `backend/.env` dan isi API key.
2. Jalankan backend: `uvicorn main:app --host 0.0.0.0 --port 8000`
3. Build APK otomatis via GitHub Actions setelah push ke branch `main`.
4. Tambahkan Secret `OPENROUTER_API_KEY` di Settings > Secrets > Actions.
