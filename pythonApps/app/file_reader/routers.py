from fastapi import APIRouter, Depends, UploadFile, File, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession
from app.shared.database import get_db
from app.file_reader import service
from app.file_reader.schemas import CustomerProfile, AiInteractionSchema
import logging

logger = logging.getLogger(__name__)
router = APIRouter()

@router.post("/upload/customer")
async def upload_customer(file: UploadFile = File(...), db: AsyncSession = Depends(get_db)):
    content = await file.read()
    await service.process_customer_csv(content, db)
    return {"message": "Customer data uploaded successfully"}

@router.post("/upload/churn")
async def upload_churn(file: UploadFile = File(...), db: AsyncSession = Depends(get_db)):
    content = await file.read()
    await service.process_churn_csv(content, db)
    return {"message": "Churn data uploaded successfully"}

@router.get("/customerProfile/customers")
async def get_customers(page: int = 0, size: int = 10, db: AsyncSession = Depends(get_db)):
    return await service.get_all_customers(page, size, db)

@router.get("/customerProfile/customersChurn")
async def get_customers_churn(page: int = 0, size: int = 10, db: AsyncSession = Depends(get_db)):
    return await service.get_all_customers_churn(page, size, db)

@router.delete("/customerProfile/cleanup")
async def cleanup(db: AsyncSession = Depends(get_db)):
    await service.clear_all_data(db)
    return {"message": "Data cleared successfully"}

@router.get("/customerProfile/{id}", response_model=CustomerProfile)
async def get_profile(id: int, db: AsyncSession = Depends(get_db)):
    profile = await service.get_customer_profile(id, db)
    if not profile:
        raise HTTPException(status_code=404, detail="Customer not found")
    return profile

@router.get("/customerProfile/{id}/recommendation")
async def get_cached(id: str, db: AsyncSession = Depends(get_db)):
    interaction = await service.get_saved_ai_interaction(id, db)
    if not interaction:
        raise HTTPException(status_code=404, detail="No cached recommendation found")
    return {
        "id": interaction.id,
        "customerId": interaction.customer_id,
        "rawPrompt": interaction.raw_prompt,
        "aiResponse": interaction.ai_response,
        "createdAt": interaction.created_at
    }

@router.post("/customerProfile/recommendation")
async def save_interaction(interaction: AiInteractionSchema, db: AsyncSession = Depends(get_db)):
    await service.save_ai_interaction(interaction, db)
    return {"message": "AI interaction saved successfully"}
