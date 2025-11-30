# Backend Setup Guide - Tiếng Việt

## Bước 1: Cài đặt Python và Dependencies

```bash
cd backend
pip install -r requirements.txt
```

## Bước 2: Lấy Firebase Service Account Key

1. Vào [Firebase Console](https://console.firebase.google.com/)
2. Chọn project của bạn
3. Vào **Project Settings** → **Service Accounts**
4. Click **Generate New Private Key**
5. Lưu file JSON về folder `backend/` với tên `service-account-key.json`

**QUAN TRỌNG**: Không commit file này lên Git!

## Bước 3: Tạo file .env

Tạo file `.env` trong folder `backend/`:

```env
GEMINI_API_KEY=your_gemini_api_key_here
FIREBASE_SERVICE_ACCOUNT_PATH=./service-account-key.json
PORT=8000
HOST=0.0.0.0
```

## Bước 4: Chạy Server

```bash
python main.py
```

Server sẽ chạy tại: `http://localhost:8000`

## Bước 5: Test API

### Test Health Check:
```bash
curl http://localhost:8000/health
```

### Test Search:
```bash
curl -X POST http://localhost:8000/search \
  -H "Content-Type: application/json" \
  -d '{
    "query": "mountain trail",
    "firebase_uid": "your_firebase_uid",
    "search_type": "hikes",
    "top_k": 5
  }'
```

## Cách hoạt động

1. **App gửi query** → Server nhận text query
2. **Gemini tạo embedding** → Server gọi Gemini API để convert query thành vector
3. **Lấy vectors từ Firebase** → Server đọc tất cả hikes/observations có `embedding_vector`
4. **Tính similarity** → Server tính cosine similarity giữa query vector và mỗi stored vector
5. **Trả về kết quả** → Server trả về top-K items có similarity cao nhất

## Thuật toán Cosine Similarity

```python
similarity = (query_vector · stored_vector) / (||query_vector|| × ||stored_vector||)
```

- **1.0** = giống hệt nhau
- **0.0** = không liên quan
- **-1.0** = đối nghịch

## Deploy lên Server

### Option 1: VPS/Cloud Server
1. Upload code lên server
2. Cài đặt dependencies
3. Chạy với `uvicorn` hoặc `gunicorn`
4. Dùng nginx làm reverse proxy

### Option 2: Heroku/Railway/Render
1. Tạo account
2. Connect GitHub repo
3. Set environment variables
4. Deploy tự động

### Option 3: Docker
```bash
docker build -t mhike-backend .
docker run -p 8000:8000 mhike-backend
```

## Lưu ý bảo mật

- **KHÔNG commit** `service-account-key.json` lên Git
- Dùng environment variables cho sensitive data
- Trong production, giới hạn CORS origins
- Cân nhắc thêm authentication/rate limiting

