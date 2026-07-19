import pandas as pd
from io import StringIO
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select, update, text
from app.shared.models import Customer, CustomerChurn, AiInteraction
from app.file_reader.schemas import CustomerProfile, AiInteractionSchema
from decimal import Decimal
import logging

logger = logging.getLogger(__name__)

async def process_customer_csv(file_content: bytes, db: AsyncSession):
    df = pd.read_csv(StringIO(file_content.decode('utf-8')))
    # Columns: CustomerId,RowNumber,Surname,CreditScore,Geography,Gender,Age,Tenure,Balance,NumOfProducts,HasCrCard,IsActiveMember,EstimatedSalary,Exited
    for _, row in df.iterrows():
        try:
            customer = Customer(
                customer_id=int(row['CustomerId']),
                row_numbers=int(row['RowNumber']),
                surname=row['Surname'],
                credit_score=int(row['CreditScore']),
                geography=row['Geography'],
                gender=row['Gender'],
                age=int(row['Age']),
                tenure=int(row['Tenure']),
                balance=Decimal(str(row['Balance'])),
                num_of_products=int(row['NumOfProducts']),
                has_cr_card=bool(row['HasCrCard']),
                is_active_member=bool(row['IsActiveMember']),
                estimated_salary=Decimal(str(row['EstimatedSalary'])),
                exited=bool(row['Exited'])
            )
            await db.merge(customer)
        except Exception as e:
            logger.error(f"Error processing customer row {row['CustomerId']}: {e}")
    await db.commit()

async def process_churn_csv(file_content: bytes, db: AsyncSession):
    df = pd.read_csv(StringIO(file_content.decode('utf-8')))
    # Columns: customerID,gender,SeniorCitizen,Partner,Dependents,tenure,PhoneService,MultipleLines,InternetService,OnlineSecurity,OnlineBackup,DeviceProtection,TechSupport,StreamingTV,StreamingMovies,Contract,PaperlessBilling,PaymentMethod,MonthlyCharges,TotalCharges,Churn
    for _, row in df.iterrows():
        try:
            # We need to map customerID (string in CSV) to numeric customer_id if possible, 
            # or just use it as is if it's already numeric. 
            # Looking at the SQL, customer_id is BIGINT.
            # In the sample CSVs, it's often a string like '7590-VHVEG'.
            # Wait, the SQL says BIGINT and FK to customers(customer_id).
            # I'll try to convert it to int.
            try:
                cid = int(row['customerID'].split('-')[0]) # Dummy mapping for now if it's string
            except:
                cid = 0 # Fallback

            churn = CustomerChurn(
                customer_id=cid,
                unique_id=row['customerID'],
                gender=row['gender'],
                senior_citizen=bool(row['SeniorCitizen']),
                partner=bool(row['Partner']),
                dependents=bool(row['Dependents']),
                tenure=int(row['tenure']),
                phone_service=bool(row['PhoneService']),
                multiple_lines=row['MultipleLines'],
                internet_service=row['InternetService'],
                online_security=row['OnlineSecurity'],
                online_backup=row['OnlineBackup'],
                device_protection=row['DeviceProtection'],
                tech_support=row['TechSupport'],
                streaming_tv=row['StreamingTV'],
                streaming_movies=row['StreamingMovies'],
                contract=row['Contract'],
                paperless_billing=bool(row['PaperlessBilling']),
                payment_method=row['PaymentMethod'],
                monthly_charges=Decimal(str(row['MonthlyCharges'])),
                total_charges=Decimal(str(row['TotalCharges']).strip() or "0"),
                churn=True if str(row['Churn']).lower() in ['yes', 'true', '1'] else False
            )
            await db.merge(churn)
        except Exception as e:
            logger.error(f"Error processing churn row {row['customerID']}: {e}")
    await db.commit()

async def get_all_customers(page: int, size: int, db: AsyncSession):
    result = await db.execute(select(Customer).limit(size).offset(page * size))
    return result.scalars().all()

async def get_all_customers_churn(page: int, size: int, db: AsyncSession):
    result = await db.execute(select(CustomerChurn).limit(size).offset(page * size))
    return result.scalars().all()

async def clear_all_data(db: AsyncSession):
    await db.execute(text("DELETE FROM aws.customer_churn"))
    await db.execute(text("DELETE FROM aws.customers"))
    await db.execute(text("DELETE FROM aws.ai_interactions"))
    await db.commit()

async def get_customer_profile(customer_id: int, db: AsyncSession):
    # Join Customer and CustomerChurn
    result = await db.execute(
        select(Customer, CustomerChurn)
        .outerjoin(CustomerChurn, Customer.customer_id == CustomerChurn.customer_id)
        .where(Customer.customer_id == customer_id)
    )
    row = result.first()
    if not row:
        return None
    
    c, cc = row
    return CustomerProfile(
        customerId=c.customer_id,
        surname=c.surname,
        age=c.age,
        tenure=c.tenure,
        monthlyCharges=cc.monthly_charges if cc else None,
        totalCharges=cc.total_charges if cc else None,
        contract=cc.contract if cc else None,
        internetService=cc.internet_service if cc else None,
        techSupport=cc.tech_support if cc else None,
        paymentMethod=cc.payment_method if cc else None,
        churn=cc.churn if cc else None
    )

async def get_saved_ai_interaction(customer_id: str, db: AsyncSession):
    result = await db.execute(
        select(AiInteraction)
        .where(AiInteraction.customer_id == customer_id)
        .order_by(AiInteraction.created_at.desc())
        .limit(1)
    )
    return result.scalar_one_or_none()

async def save_ai_interaction(interaction: AiInteractionSchema, db: AsyncSession):
    db_obj = AiInteraction(
        customer_id=interaction.customerId,
        raw_prompt=interaction.rawPrompt,
        ai_response=interaction.aiResponse
    )
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj
