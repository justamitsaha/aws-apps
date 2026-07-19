from pydantic import BaseModel, ConfigDict
from typing import List, Optional
from decimal import Decimal
from datetime import datetime

class Action(BaseModel):
    title: str
    details: str
    priority: str

class Offer(BaseModel):
    type: str
    description: str
    discountPercent: Optional[float] = None
    durationMonths: Optional[int] = None

class Citation(BaseModel):
    fileName: str
    chunkIndex: int
    score: float

class RetentionPlan(BaseModel):
    riskLevel: str
    reasoning: List[str]
    actions: List[Action]
    offer: Optional[Offer] = None
    citations: Optional[List[Citation]] = None

class RagSearchRequest(BaseModel):
    query: str
    topK: int = 5
    documentType: str = "retention-policy"

class ChunkMatch(BaseModel):
    chunkId: int
    documentId: int
    fileName: str
    chunkIndex: int
    chunkText: str
    score: float

class QuestionRequest(BaseModel):
    question: str
    context: Optional[str] = None

class DocumentSummary(BaseModel):
    id: int
    fileName: str
    documentType: str
    contentType: Optional[str] = None
    createdAt: datetime
