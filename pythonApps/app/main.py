import sys
from pathlib import Path

# Add the parent directory of 'app' to sys.path so 'import app' works
current_dir = Path(__file__).resolve().parent
parent_dir = current_dir.parent
if str(parent_dir) not in sys.path:
    sys.path.insert(0, str(parent_dir))

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.file_reader.routers import router as file_reader_router
from app.reporting.routers import router as reporting_router
from app.shared.config import settings
import uvicorn

app = FastAPI(title="AWS Customer Churn AI Analysis")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include routers
app.include_router(file_reader_router, tags=["File Reader"])
app.include_router(reporting_router, tags=["Reporting"])

@app.get("/upload/health")
async def health():
    return {"status": "UP", "service": "Python FastAPI"}

if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
    uvicorn.run("app.main:app", host="0.0.0.0", port=port, reload=True)
