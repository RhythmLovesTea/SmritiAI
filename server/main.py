from __future__ import annotations

from typing import Any, Optional

import requests
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel


class PersonContext(BaseModel):
    name: str
    relationship: Optional[str] = None
    last_visit: Optional[str] = None
    summary: Optional[str] = None
    emotion: Optional[str] = None


class MemoryContext(BaseModel):
    name: str
    relationship: Optional[str] = None
    summary: Optional[str] = None
    timestamp: Optional[int] = None


class ChatContext(BaseModel):
    person: Optional[PersonContext] = None
    memories: list[MemoryContext] = []


class ChatRequest(BaseModel):
    query: str
    context: Optional[ChatContext] = None


class ChatResponse(BaseModel):
    reply: str


app = FastAPI(title="SmritiAI Local Chat Server")

OLLAMA_URL = "http://127.0.0.1:11434/api/generate"
OLLAMA_MODEL = "phi3:mini"


def build_prompt(query: str, context: Optional[ChatContext]) -> str:
    person_block = ""
    if context and context.person:
        p = context.person
        person_block = (
            "RECOGNIZED PERSON:\n"
            f"- Name: {p.name}\n"
            f"- Relationship: {p.relationship or 'unknown'}\n"
            f"- Last visit: {p.last_visit or 'unknown'}\n"
            f"- Summary: {p.summary or 'unknown'}\n"
            f"- Emotion: {p.emotion or 'unknown'}\n"
            "\n"
        )

    memories_block = ""
    if context and context.memories:
        lines: list[str] = []
        for m in context.memories[:6]:
            lines.append(
                f"- {m.name} ({m.relationship or 'unknown'}): {m.summary or ''}".strip()
            )
        memories_block = "SAVED MEMORIES (snippets):\n" + "\n".join(lines) + "\n\n"

    return (
        "You are Smriti AI, a warm and caring memory assistant for elderly users.\n"
        "RULES:\n"
        "1) Be short, calm, and kind.\n"
        "2) If context is missing, ask one clarifying question.\n"
        "3) Never hallucinate specific facts.\n\n"
        f"{person_block}"
        f"{memories_block}"
        f"USER QUERY: {query}\n"
        "ASSISTANT REPLY:\n"
    )


@app.post("/chat", response_model=ChatResponse)
def chat(req: ChatRequest) -> ChatResponse:
    prompt = build_prompt(req.query, req.context)
    try:
        r = requests.post(
            OLLAMA_URL,
            json={
                "model": OLLAMA_MODEL,
                "prompt": prompt,
                "stream": False,
                "options": {"temperature": 0.4},
            },
            timeout=120,
        )
    except requests.RequestException as e:
        raise HTTPException(status_code=503, detail=f"Ollama not reachable: {e}") from e

    if r.status_code != 200:
        raise HTTPException(status_code=502, detail=f"Ollama error: {r.text}")

    data: dict[str, Any] = r.json()
    reply = (data.get("response") or "").strip()
    if not reply:
        raise HTTPException(status_code=502, detail="Empty model response")
    return ChatResponse(reply=reply)

