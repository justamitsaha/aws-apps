from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, text
from app.shared.models import Document, DocumentChunk
from app.reporting.schemas import (
    RetentionPlan, Action, Offer, Citation, ChunkMatch, DocumentSummary, 
    QuestionRequest
)
from app.file_reader.schemas import CustomerProfile, AiInteractionSchema
from app.shared.llm import llm_client
from app.shared.config import settings
import httpx
import json
import logging
from decimal import Decimal
from typing import List, Optional

logger = logging.getLogger(__name__)

async def fetch_customer_profile(customer_id: int) -> Optional[CustomerProfile]:
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get(f"{settings.APP_API_URL}/api/v1/customer-profile/{customer_id}")
            if response.status_code == 200:
                return CustomerProfile(**response.json())
        except Exception as e:
            logger.error(f"Error fetching profile: {e}")
    return None

async def fetch_cached_recommendation(customer_id: str) -> Optional[dict]:
    async with httpx.AsyncClient() as client:
        try:
            response = await client.get(f"{settings.APP_API_URL}/api/v1/customer-profile/ai-cache/get-saved/{customer_id}")
            if response.status_code == 200:
                return response.json()
        except Exception as e:
            logger.error(f"Error fetching cache: {e}")
    return None

async def save_recommendation(customer_id: str, plan: RetentionPlan):
    async with httpx.AsyncClient() as client:
        try:
            interaction = {
                "customerId": customer_id,
                "rawPrompt": "AI Generated Plan",
                "aiResponse": plan.model_dump_json()
            }
            await client.post(f"{settings.APP_API_URL}/api/v1/customer-profile/ai-cache/save", json=interaction)
        except Exception as e:
            logger.error(f"Error saving recommendation: {e}")

async def generate_retention_plan(profile: CustomerProfile, context: Optional[str] = None) -> RetentionPlan:
    system_prompt = "You are an AI assistant that analyzes customer churn risk and provides retention plans in JSON format."
    
    context_str = f"\nPOLICY CONTEXT (retrieved from company docs):\n{context}" if context else ""
    context_header = " + POLICY CONTEXT" if context else ""
    context_rule = "- If POLICY CONTEXT does not support an offer, set offer.type = 'NONE'." if context else ""

    prompt = f"""
    You are an API that returns ONLY valid JSON. No markdown. No extra text.

    Return JSON strictly matching this structure:
    {{
        "riskLevel": "LOW|MEDIUM|HIGH",
        "reasoning": ["string"],
        "actions": [
            {{ "title": "string", "details": "string", "priority": "HIGH|MEDIUM|LOW" }}
        ],
        "offer": null or {{
            "type": "DISCOUNT|UPGRADE|SUPPORT|NONE",
            "description": "string",
            "discountPercent": number or null,
            "durationMonths": number or null
        }}
    }}

    Rules:
    - Use ONLY the input fields below{context_header}.
    - Do NOT invent discount/coupon codes.
    - Enum values must be UPPERCASE exactly as shown.
    {context_rule}

    INPUT:
    age={profile.age}
    tenure={profile.tenure}
    monthlyCharges={profile.monthlyCharges}
    contract={profile.contract}
    techSupport={profile.techSupport}
    internetService={profile.internetService}
    paymentMethod={profile.paymentMethod}
    {context_str}
    """
    
    response_text = await llm_client.chat_completion(system_prompt, prompt)
    # Clean JSON
    if "```json" in response_text:
        response_text = response_text.split("```json")[1].split("```")[0].strip()
    elif "```" in response_text:
        response_text = response_text.split("```")[1].strip()
    
    return RetentionPlan.model_validate_json(response_text)

async def ingest_document(file_name: str, doc_type: str, content_type: str, text_content: str, db: AsyncSession):
    # 1. Save document metadata
    doc = Document(file_name=file_name, document_type=doc_type, content_type=content_type)
    db.add(doc)
    await db.commit()
    await db.refresh(doc)
    
    # 2. Chunk text (simple chunking by size/overlap)
    chunk_size = 1000
    overlap = 100
    chunks = []
    start = 0
    while start < len(text_content):
        end = start + chunk_size
        chunks.append(text_content[start:end])
        start += chunk_size - overlap
    
    # 3. Create embeddings and save chunks
    for i, chunk_text in enumerate(chunks):
        embedding = await llm_client.get_embedding(chunk_text)
        db_chunk = DocumentChunk(
            document_id=doc.id,
            chunk_index=i,
            chunk_text=chunk_text,
            embedding=embedding
        )
        db.add(db_chunk)
    
    await db.commit()
    return doc.id

async def search_similar_chunks(query: str, top_k: int, doc_type: str, db: AsyncSession) -> List[ChunkMatch]:
    query_embedding = await llm_client.get_embedding(query)
    
    # Using raw SQL for pgvector similarity search since it's cleaner than SQLAlchemy ORM for this
    # 1 - (embedding <=> query_embedding) is cosine similarity
    sql = text("""
        SELECT
            c.id AS chunk_id,
            c.document_id,
            d.file_name,
            c.chunk_index,
            c.chunk_text,
            1 - (c.embedding <=> CAST(:q AS public.vector(1536))) AS score
        FROM aws.document_chunks c
        JOIN aws.documents d ON d.id = c.document_id
        WHERE d.document_type = :doc_type
        ORDER BY c.embedding <=> CAST(:q AS public.vector(1536))
        LIMIT :top_k
    """)
    
    result = await db.execute(sql, {"q": str(query_embedding), "doc_type": doc_type, "top_k": top_k})
    
    matches = []
    for row in result:
        matches.append(ChunkMatch(
            chunkId=row.chunk_id,
            documentId=row.document_id,
            fileName=row.file_name,
            chunkIndex=row.chunk_index,
            chunkText=row.chunk_text,
            score=float(row.score)
        ))
    return matches

async def search_chunks_by_document(document_id: int, query_embedding: List[float], top_k: int, db: AsyncSession) -> List[ChunkMatch]:
    sql = text("""
        SELECT
            c.id AS chunk_id,
            c.document_id,
            d.file_name,
            c.chunk_index,
            c.chunk_text,
            1 - (c.embedding <=> CAST(:q AS public.vector(1536))) AS score
        FROM aws.document_chunks c
        JOIN aws.documents d ON d.id = c.document_id
        WHERE c.document_id = :document_id
        ORDER BY c.embedding <=> CAST(:q AS public.vector(1536))
        LIMIT :top_k
    """)
    
    result = await db.execute(sql, {"q": str(query_embedding), "document_id": document_id, "top_k": top_k})
    
    matches = []
    for row in result:
        matches.append(ChunkMatch(
            chunkId=row.chunk_id,
            documentId=row.document_id,
            fileName=row.file_name,
            chunkIndex=row.chunk_index,
            chunkText=row.chunk_text,
            score=float(row.score)
        ))
    return matches

async def get_all_documents(db: AsyncSession) -> List[DocumentSummary]:
    result = await db.execute(select(Document).order_by(Document.created_at.desc()))
    return [DocumentSummary(
        id=doc.id,
        fileName=doc.file_name,
        documentType=doc.document_type,
        contentType=doc.content_type,
        createdAt=doc.created_at
    ) for doc in result.scalars().all()]
