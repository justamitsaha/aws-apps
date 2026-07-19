from sqlalchemy import Column, BigInteger, Integer, String, Numeric, Boolean, Text, ForeignKey, TIMESTAMP, func
from sqlalchemy.orm import relationship
from pgvector.sqlalchemy import Vector
from app.shared.database import Base

class Customer(Base):
    __tablename__ = "customers"
    __table_args__ = {"schema": "aws"}

    customer_id = Column(BigInteger, primary_key=True)
    row_numbers = Column(Integer)
    surname = Column(String(100))
    credit_score = Column(Integer)
    geography = Column(String(50))
    gender = Column(String(10))
    age = Column(Integer)
    tenure = Column(Integer)
    balance = Column(Numeric(15, 2))
    num_of_products = Column(Integer)
    has_cr_card = Column(Boolean)
    is_active_member = Column(Boolean)
    estimated_salary = Column(Numeric(15, 2))
    exited = Column(Boolean)

class CustomerChurn(Base):
    __tablename__ = "customer_churn"
    __table_args__ = {"schema": "aws"}

    customer_id = Column(BigInteger, ForeignKey("aws.customers.customer_id"), primary_key=True)
    unique_id = Column(String(50))
    gender = Column(String(10))
    senior_citizen = Column(Boolean)
    partner = Column(Boolean)
    dependents = Column(Boolean)
    tenure = Column(Integer)
    phone_service = Column(Boolean)
    multiple_lines = Column(String(20))
    internet_service = Column(String(30))
    online_security = Column(String(30))
    online_backup = Column(String(30))
    device_protection = Column(String(30))
    tech_support = Column(String(30))
    streaming_tv = Column(String(30))
    streaming_movies = Column(String(30))
    contract = Column(String(30))
    paperless_billing = Column(Boolean)
    payment_method = Column(String(50))
    monthly_charges = Column(Numeric(10, 2))
    total_charges = Column(Numeric(12, 2))
    churn = Column(Boolean)

class AiInteraction(Base):
    __tablename__ = "ai_interactions"
    __table_args__ = {"schema": "aws"}

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    customer_id = Column(String(50))
    raw_prompt = Column(Text)
    ai_response = Column(Text)  # Store JSON string
    created_at = Column(TIMESTAMP, server_default=func.now())

class Document(Base):
    __tablename__ = "documents"
    __table_args__ = {"schema": "aws"}

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    file_name = Column(Text, nullable=False)
    document_type = Column(Text, nullable=False)
    content_type = Column(Text)
    created_at = Column(TIMESTAMP, server_default=func.now())

class DocumentChunk(Base):
    __tablename__ = "document_chunks"
    __table_args__ = {"schema": "aws"}

    id = Column(BigInteger, primary_key=True, autoincrement=True)
    document_id = Column(BigInteger, ForeignKey("aws.documents.id", ondelete="CASCADE"))
    chunk_index = Column(Integer, nullable=False)
    chunk_text = Column(Text, nullable=False)
    embedding = Column(Vector(1536), nullable=False)
    created_at = Column(TIMESTAMP, server_default=func.now())
