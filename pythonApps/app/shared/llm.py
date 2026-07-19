from openai import AsyncOpenAI
from app.shared.config import settings
import json

class LlmClient:
    def __init__(self):
        self.client = AsyncOpenAI(api_key=settings.OPENAI_API_KEY_PRACTICE)

    async def get_embedding(self, text: str):
        response = await self.client.embeddings.create(
            input=text,
            model="text-embedding-3-small" # 1536 dims
        )
        return response.data[0].embedding

    async def chat_completion(self, system_prompt: str, user_prompt: str):
        response = await self.client.chat.completions.create(
            model="gpt-4o",
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt}
            ],
            temperature=0.1
        )
        return response.choices[0].message.content

llm_client = LlmClient()
