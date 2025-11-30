# M-Hike Vector Search Backend

Python FastAPI server for semantic search on Firebase vector database.

## Features

- **Semantic Search**: Convert text queries to embeddings and find similar hikes/observations
- **Cosine Similarity**: Calculate similarity scores between query and stored vectors
- **Firebase Integration**: Directly reads vectors from Firestore
- **RESTful API**: Simple HTTP endpoints for Android app

## Setup

### 1. Install Dependencies

```bash
cd backend
pip install -r requirements.txt
```

### 2. Configure Environment

1. Copy `.env.example` to `.env`:
   ```bash
   cp .env.example .env
   ```

2. Edit `.env` and add:
   - `GEMINI_API_KEY`: Your Gemini API key
   - `FIREBASE_SERVICE_ACCOUNT_PATH`: Path to Firebase service account JSON

### 3. Get Firebase Service Account Key

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Go to **Project Settings** → **Service Accounts**
4. Click **Generate New Private Key**
5. Save the JSON file as `service-account-key.json` in the `backend/` folder
6. **Important**: Add `service-account-key.json` to `.gitignore` (never commit it!)

### 4. Run the Server

```bash
python main.py
```

Or with uvicorn directly:
```bash
uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

The server will run on `http://localhost:8000`

## API Endpoints

### POST `/search`

Search for similar hikes/observations.

**Request Body:**
```json
{
  "query": "mountain trail with parking",
  "firebase_uid": "user_firebase_uid_here",
  "search_type": "hikes",  // "hikes", "observations", or "all"
  "top_k": 10
}
```

**Response:**
```json
{
  "results": [
    {
      "id": "1",
      "type": "hike",
      "score": 0.85,
      "name": "Mountain Peak Trail",
      "location": "Rocky Mountain",
      "description": "Challenging trail...",
      "hike_id": 1
    }
  ],
  "query_embedding_length": 768
}
```

### GET `/health`

Health check endpoint.

**Response:**
```json
{
  "status": "healthy",
  "gemini_configured": true
}
```

## How It Works

1. **Query Embedding**: User sends a text query → Server converts it to embedding using Gemini
2. **Fetch Vectors**: Server fetches all hikes/observations with `embedding_vector` from Firebase
3. **Similarity Calculation**: Computes cosine similarity between query embedding and each stored vector
4. **Ranking**: Sorts results by similarity score (highest first)
5. **Return Results**: Returns top-K most similar items

## Cosine Similarity Algorithm

```python
similarity = dot(query_vector, stored_vector) / (norm(query_vector) * norm(stored_vector))
```

- Returns value between -1 and 1
- 1.0 = identical
- 0.0 = no similarity
- -1.0 = opposite

## Deployment

### Local Development
```bash
python main.py
```

### Production (using gunicorn)
```bash
gunicorn -w 4 -k uvicorn.workers.UvicornWorker main:app --bind 0.0.0.0:8000
```

### Docker (optional)
Create `Dockerfile`:
```dockerfile
FROM python:3.11-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install -r requirements.txt
COPY . .
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8000"]
```

## Security Notes

- **Never commit** `service-account-key.json` to Git
- Use environment variables for sensitive data
- In production, restrict CORS origins to your app domain
- Consider adding authentication/rate limiting

## Troubleshooting

### "GEMINI_API_KEY not found"
- Check `.env` file exists and contains `GEMINI_API_KEY`

### "Firebase service account file not found"
- Download service account key from Firebase Console
- Place it in `backend/` folder
- Update `FIREBASE_SERVICE_ACCOUNT_PATH` in `.env`

### "Failed to generate embedding"
- Check Gemini API key is valid
- Check API quota/limits

### "No results found"
- Make sure hikes/observations have `embedding_vector` field in Firebase
- Check `firebase_uid` is correct

