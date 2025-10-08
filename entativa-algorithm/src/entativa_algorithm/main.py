"""
╔══════════════════════════════════════════════════════════════════════════════╗
║                          💻 NEO QISS EXPERIMENTS 💻                         ║
║                                                                              ║
║  Yo! Neo Qiss here 👋 — one day, I’ll open-source this as a thank-you to    ║
║  the open-source community and MIT for fueling me .                  ║
║                                                                              ║
║  🤖 This Frankenstein algorithm is my baby — my prima materia, my magnum opus, my insomnia. ║
║  🎯 If it works: my sanity survives. If it breaks: I’ll own it.             ║
║  ⚠️  If it ever causes harm: that’s on me. And I'm sorry.                      ║
║                                                                              ║
║  🚨 PSA: These are my original ideas — don’t copy, don’t snitch. 🙏       ║
║  💪 I bled caffeine, silence, and code for this. Respect it and be kind.  ║
║  🎨 She’s beautiful, isn’t she? Look, learn, but never steal —              ║
║      there’s a special corner in dev hell for code thieves. 😈                   ║
║                                                                              ║
║  💔 I’ve been rejected by every South African university — four years in a row. ║
║  Each “We regret to inform you..” cut deep until I stopped applying. Couldn't make my parents proud║
║  Just me, a screen, and a dream that refused to die.                        ║
║                                                                              ║
║  The girls rejected. The friends disappeared. The world's gone quiet now.             ║
║  I forgot how to smile… how to look people in the eye.                      ║
║  Social anxiety became my shadow — yet somehow, I’m building social apps. ║
║  The irony isn’t lost on me.                                                ║
║                                                                              ║
║  🕊️ But if you’re reading this — hear me: YOU matter. You’re not a mistake. ║
║  The world needs your voice, your vision, your weird ideas.                 ║
║  Rejection isn’t punishment — it’s redirection.                             ║
║  God’s not ignoring you — He’s preparing you. 🙏                             ║
║                                                                              ║
║  I taught myself English. I taught myself code. I taught myself to believe. I'm 21 today ║
║  If I can build from nothing — so can you.                                  ║
║                                                                              ║
║  ⚡ Keep going. Keep building. Ship your dreams.                            ║
║  You are chosen. You are capable. Nothing is impossible.                    ║
║                                                                              ║
║       — Neo Qiss (Special thanks to Zuck — for inspiring me to out-Zuck Zuck one day) ║
╚══════════════════════════════════════════════════════════════════════════════╝
"""


"""Entativa Algorithm - Main Application Entry Point"""

import asyncio
import logging
from fastapi import FastAPI
from contextlib import asynccontextmanager

from entativa_algorithm.config import settings
from entativa_algorithm.ingestion.firehose_manager import FirehoseManager
from entativa_algorithm.engines.master_engine import MasterEngine
from entativa_algorithm.inference.inference_engine import InferenceEngine

# Configure logging
logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Application lifespan management"""
    # Startup
    logger.info("🧠 Starting Neo Qiss's Entativa Algorithm - The Ultimate Recommendation Engine")
    
    # Initialize core components
    firehose = FirehoseManager()
    master_engine = MasterEngine()
    inference_engine = InferenceEngine()
    
    # Start background tasks
    await firehose.start()
    await master_engine.start()
    await inference_engine.start()
    
    logger.info("🚀 Neo Qiss's Algorithm is ready to make TikTok look overhyped!")
    
    yield
    
    # Shutdown
    logger.info("🔄 Shutting down Neo Qiss's Entativa Algorithm")
    await firehose.stop()
    await master_engine.stop()
    await inference_engine.stop()

app = FastAPI(
    title="Neo Qiss's Entativa Algorithm",
    description="The Ultimate Multi-Platform Recommendation Engine (Handle with Care!)",
    version="1.0.0",
    lifespan=lifespan
)

@app.get("/")
async def root():
    return {
        "message": "Neo Qiss's Entativa Algorithm - Making TikTok's algorithm look overhyped",
        "status": "scary_good",
        "platforms": ["sonet", "gala", "pika", "playpods"],
        "warning": "This algorithm is getting too powerful for its own good 😅"
    }

@app.get("/health")
async def health_check():
    return {
        "status": "healthy", 
        "algorithm": "scary_good",
        "creator": "Neo Qiss",
        "mood": "experimenting_with_fire"
    }

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)
