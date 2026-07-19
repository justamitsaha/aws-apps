from sqlalchemy.ext.asyncio import create_async_engine, AsyncSession, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase
from app.shared.config import settings

# Construct the async URL if it's from the environment
db_url = settings.SPRING_R2DBC_URL
if db_url.startswith("r2dbc:postgresql://"):
    db_url = db_url.replace("r2dbc:postgresql://", "postgresql+asyncpg://")

engine = create_async_engine(db_url, echo=False)
AsyncSessionLocal = async_sessionmaker(bind=engine, class_=AsyncSession, expire_on_commit=False)

class Base(DeclarativeBase):
    pass

async def get_db():
    async with AsyncSessionLocal() as session:
        yield session
