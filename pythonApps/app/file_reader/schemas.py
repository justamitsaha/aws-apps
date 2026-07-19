from pydantic import BaseModel, ConfigDict
from decimal import Decimal
from typing import Optional, List
from datetime import datetime

class CustomerProfile(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    customerId: int
    surname: Optional[str] = None
    age: Optional[int] = None
    tenure: Optional[int] = None
    monthlyCharges: Optional[Decimal] = None
    totalCharges: Optional[Decimal] = None
    contract: Optional[str] = None
    internetService: Optional[str] = None
    techSupport: Optional[str] = None
    paymentMethod: Optional[str] = None
    churn: Optional[bool] = None

class AiInteractionSchema(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: Optional[int] = None
    customerId: str
    rawPrompt: str
    aiResponse: str
    createdAt: Optional[datetime] = None
