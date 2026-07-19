from fastapi import APIRouter, Depends, UploadFile, File, HTTPException, Form
from sqlalchemy.ext.asyncio import AsyncSession
from app.shared.database import get_db
from app.reporting import service
from app.reporting.schemas import (
    RetentionPlan, RagSearchRequest, QuestionRequest, ChunkMatch, DocumentSummary
)
from app.shared.llm import llm_client
import logging
import json

logger = logging.getLogger(__name__)
router = APIRouter()

@router.get("/retention/health")
async def health():
    return {"status": "UP", "service": "Reporting"}

@router.get("/retention/{id}/analyze", response_model=RetentionPlan)
async def analyze_customer(id: int, db: AsyncSession = Depends(get_db)):
    profile = await service.fetch_customer_profile(id)
    if not profile:
        raise HTTPException(status_code=404, detail="Customer not found")
    
    # Check cache
    cached = await service.fetch_cached_recommendation(str(id))
    if cached:
        try:
            return RetentionPlan.model_validate_json(cached["aiResponse"])
        except:
            pass
            
    # AI Analysis with RAG
    query = f"retention policy and offers for: contract={profile.contract}, monthlyCharges={profile.monthlyCharges}, tenure={profile.tenure}"
    matches = await service.search_similar_chunks(query, 5, "RETENTION_POLICY", db)
    
    context = ""
    for m in matches:
        context += f"SOURCE: {m.fileName} | chunk={m.chunkIndex} | score={m.score:.3f}\n{m.chunkText}\n\n"
        
    plan = await service.generate_retention_plan(profile, context)
    
    # Attach citations
    plan.citations = [service.Citation(fileName=m.fileName, chunkIndex=m.chunkIndex, score=m.score) for m in matches]
    
    # Save to cache
    await service.save_recommendation(str(id), plan)
    
    return plan

@router.get("/retention/{id}/analyze/nocache", response_model=RetentionPlan)
async def analyze_no_cache(id: int, db: AsyncSession = Depends(get_db)):
    profile = await service.fetch_customer_profile(id)
    if not profile:
        raise HTTPException(status_code=404, detail="Customer not found")
    
    return await service.generate_retention_plan(profile)

@router.get("/retention/{id}/analyze/rag", response_model=RetentionPlan)
async def analyze_with_rag(id: int, db: AsyncSession = Depends(get_db)):
    profile = await service.fetch_customer_profile(id)
    if not profile:
        raise HTTPException(status_code=404, detail="Customer not found")
    
    # Standard analyze in this implementation already uses RAG if possible
    # But let's follow Java structure: build query based on profile
    return await analyze_customer(id, db)

@router.post("/rag/upload")
async def ingest_rag(
    file: UploadFile = File(...), 
    doc_type: str = Form("RETENTION_POLICY"), 
    db: AsyncSession = Depends(get_db)
):
    content = await file.read()
    text = content.decode('utf-8')
    doc_id = await service.ingest_document(file.filename, doc_type, file.content_type, text, db)
    return {"message": "Document ingested successfully", "id": doc_id}

@router.get("/rag/search", response_model=list[ChunkMatch])
async def search_rag(q: str, topK: int = 5, db: AsyncSession = Depends(get_db)):
    return await service.search_similar_chunks(q, topK, "RETENTION_POLICY", db)

@router.get("/rag/documents", response_model=list[DocumentSummary])
async def list_documents(db: AsyncSession = Depends(get_db)):
    return await service.get_all_documents(db)

@router.post("/rag/{documentId}/ask")
async def ask_rag(documentId: int, request: QuestionRequest, db: AsyncSession = Depends(get_db)):
    # 1. Search chunks for this document
    query_embedding = await llm_client.get_embedding(request.question)
    matches = await service.search_chunks_by_document(documentId, query_embedding, 5, db)
    
    if not matches:
        return {"answer": "Not found in the document", "sources": []}
        
    context = "\n\n".join([m.chunkText for m in matches])
    
    system_prompt = "You answer ONLY using the provided document context. If the answer is not present, say 'Not found in the document.'"
    user_prompt = f"Document context:\n{context}\n\nQuestion:\n{request.question}"
    
    answer = await llm_client.chat_completion(system_prompt, user_prompt)
    
    sources = [{"chunkIndex": m.chunkIndex, "score": m.score} for m in matches]
    return {"answer": answer, "sources": sources}
