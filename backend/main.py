"""
FastAPI server for semantic search on Firebase vector database.

This server:
1. Receives search queries from Android app
2. Converts query to embedding using Gemini
3. Fetches all hikes/observations with embeddings from Firebase
4. Computes cosine similarity
5. Returns top-N most similar results
"""

from fastapi import FastAPI, HTTPException, Query
from fastapi.middleware.cors import CORSMiddleware
from pydantic import BaseModel
from typing import List, Optional
import os
from dotenv import load_dotenv
import firebase_admin
from firebase_admin import credentials, firestore
import numpy as np
from math import sqrt
import requests
import json

# Load environment variables
load_dotenv()

app = FastAPI(title="M-Hike Vector Search API")

# CORS middleware to allow Android app requests
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # In production, specify your app's domain
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Initialize Gemini
GEMINI_API_KEY = os.getenv("GEMINI_API_KEY")
if not GEMINI_API_KEY:
    raise ValueError("GEMINI_API_KEY not found in environment variables")
genai.configure(api_key=GEMINI_API_KEY)

# Initialize Firebase Admin
FIREBASE_SERVICE_ACCOUNT = os.getenv("FIREBASE_SERVICE_ACCOUNT_PATH")
if not FIREBASE_SERVICE_ACCOUNT or not os.path.exists(FIREBASE_SERVICE_ACCOUNT):
    raise ValueError(f"Firebase service account file not found: {FIREBASE_SERVICE_ACCOUNT}")

if not firebase_admin._apps:
    cred = credentials.Certificate(FIREBASE_SERVICE_ACCOUNT)
    firebase_admin.initialize_app(cred)

db = firestore.client()

# Request/Response models
class SearchRequest(BaseModel):
    query: str
    firebase_uid: str
    search_type: str = "hikes"  # "hikes" or "observations" or "all"
    top_k: int = 10

class SearchResult(BaseModel):
    id: str
    type: str  # "hike" or "observation"
    score: float
    name: Optional[str] = None
    location: Optional[str] = None
    description: Optional[str] = None
    observation_text: Optional[str] = None
    hike_id: Optional[int] = None

class SearchResponse(BaseModel):
    results: List[SearchResult]
    query_embedding_length: int

def get_embedding(text: str) -> List[float]:
    """Convert text to embedding using Gemini API (HTTP request)."""
    try:
        url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:embedContent?key={GEMINI_API_KEY}"
        
        payload = {
            "model": "models/gemini-2.5-flash",
            "content": {
                "parts": [
                    {"text": text}
                ]
            }
        }
        
        response = requests.post(url, json=payload)
        response.raise_for_status()
        
        result = response.json()
        if "embedding" in result and "values" in result["embedding"]:
            return result["embedding"]["values"]
        else:
            raise ValueError(f"Unexpected response format: {result}")
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to generate embedding: {str(e)}")

def cosine_similarity(vec1: List[float], vec2: List[float]) -> float:
    """Calculate cosine similarity between two vectors."""
    vec1 = np.array(vec1)
    vec2 = np.array(vec2)
    
    dot_product = np.dot(vec1, vec2)
    norm1 = np.linalg.norm(vec1)
    norm2 = np.linalg.norm(vec2)
    
    if norm1 == 0 or norm2 == 0:
        return 0.0
    
    return float(dot_product / (norm1 * norm2))

def fetch_hikes_with_embeddings(firebase_uid: str) -> List[dict]:
    """Fetch all hikes with embeddings from Firebase."""
    hikes_ref = db.collection("users").document(firebase_uid).collection("hikes")
    hikes = []
    
    for doc in hikes_ref.stream():
        data = doc.to_dict()
        if data and "embedding_vector" in data:
            hikes.append({
                "id": doc.id,
                "type": "hike",
                "embedding": data["embedding_vector"],
                "name": data.get("name", ""),
                "location": data.get("location", ""),
                "description": data.get("description", ""),
                "hike_id": int(doc.id) if doc.id.isdigit() else None
            })
    
    return hikes

def fetch_observations_with_embeddings(firebase_uid: str) -> List[dict]:
    """Fetch all observations with embeddings from Firebase."""
    observations = []
    hikes_ref = db.collection("users").document(firebase_uid).collection("hikes")
    
    for hike_doc in hikes_ref.stream():
        observations_ref = hike_doc.reference.collection("observations")
        for obs_doc in observations_ref.stream():
            data = obs_doc.to_dict()
            if data and "embedding_vector" in data:
                observations.append({
                    "id": obs_doc.id,
                    "type": "observation",
                    "embedding": data["embedding_vector"],
                    "observation_text": data.get("observationText", ""),
                    "comments": data.get("comments", ""),
                    "location": data.get("location", ""),
                    "hike_id": int(hike_doc.id) if hike_doc.id.isdigit() else None
                })
    
    return observations

@app.get("/")
def root():
    return {"message": "M-Hike Vector Search API", "status": "running"}

@app.post("/search", response_model=SearchResponse)
async def search(request: SearchRequest):
    """
    Semantic search endpoint.
    
    - **query**: Search text (e.g., "mountain trail with parking")
    - **firebase_uid**: Firebase user UID
    - **search_type**: "hikes", "observations", or "all"
    - **top_k**: Number of results to return (default: 10)
    """
    try:
        # 1. Convert query to embedding
        query_embedding = get_embedding(request.query)
        
        # 2. Fetch items with embeddings from Firebase
        items = []
        if request.search_type in ["hikes", "all"]:
            items.extend(fetch_hikes_with_embeddings(request.firebase_uid))
        if request.search_type in ["observations", "all"]:
            items.extend(fetch_observations_with_embeddings(request.firebase_uid))
        
        if not items:
            return SearchResponse(
                results=[],
                query_embedding_length=len(query_embedding)
            )
        
        # 3. Calculate cosine similarity for each item
        scored_items = []
        for item in items:
            if not item.get("embedding"):
                continue
            
            similarity = cosine_similarity(query_embedding, item["embedding"])
            scored_items.append({
                **item,
                "score": similarity
            })
        
        # 4. Sort by similarity score (descending)
        scored_items.sort(key=lambda x: x["score"], reverse=True)
        
        # 5. Return top-K results
        top_results = scored_items[:request.top_k]
        
        results = []
        for item in top_results:
            if item["type"] == "hike":
                results.append(SearchResult(
                    id=item["id"],
                    type="hike",
                    score=item["score"],
                    name=item.get("name"),
                    location=item.get("location"),
                    description=item.get("description"),
                    hike_id=item.get("hike_id")
                ))
            else:  # observation
                results.append(SearchResult(
                    id=item["id"],
                    type="observation",
                    score=item["score"],
                    observation_text=item.get("observation_text"),
                    location=item.get("location"),
                    hike_id=item.get("hike_id")
                ))
        
        return SearchResponse(
            results=results,
            query_embedding_length=len(query_embedding)
        )
    
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Search failed: {str(e)}")

@app.get("/health")
def health_check():
    """Health check endpoint."""
    return {"status": "healthy", "gemini_configured": bool(GEMINI_API_KEY)}

if __name__ == "__main__":
    import uvicorn
    port = int(os.getenv("PORT", 8000))
    host = os.getenv("HOST", "0.0.0.0")
    uvicorn.run(app, host=host, port=port)

