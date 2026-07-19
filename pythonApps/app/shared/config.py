import os
from pydantic_settings import BaseSettings, SettingsConfigDict

class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    SPRING_R2DBC_URL: str = "postgresql+asyncpg://postgres:password@localhost:5432/postgres"
    SPRING_R2DBC_USERNAME: str = "postgres"
    SPRING_R2DBC_PASSWORD: str = "password"
    OPENAI_API_KEY_PRACTICE: str = ""
    APP_API_URL: str = "http://localhost:8000"  # Unified or separated
    API_1_PORT: int = 8000
    API_2_PORT: int = 8001
    UPLOAD_DIR: str = "uploads"

settings = Settings()
