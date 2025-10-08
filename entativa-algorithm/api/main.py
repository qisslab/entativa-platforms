"""
╔══════════════════════════════════════════════════════════════════════════════╗
║                          🧠 NEO QISS EXPERIMENTS 🧠                         ║
║                                                                              ║
║  Yo! This is Neo Qiss here 👋 Just a crazy dev experimenting with AI/ML     ║
║  algorithms that might be getting a bit too powerful for my own good 😅     ║
║                                                                              ║
║  🤖 This Frankenstein algorithm is my baby, my art, my sleepless nights     ║
║  🎯 If it works: My sanity stays intact. If it breaks: I accept full blame  ║
║  ⚠️  If this ever causes harm: I take FULL responsibility (it's on me!)     ║
║                                                                              ║
║  🚨 PSA: These are MY original ideas - please don't copy and snitch 🙏      ║
║  💪 I sacrificed A LOT for this, so respect the hustle and creativity       ║
║  🎨 This is pure art in code form - admire it, learn from it, but DON'T     ║
║      steal it. There's a special place in dev hell for code thieves 😈     ║
║                                                                              ║
║  🤣 TL;DR: I'm just a human trying to build something cool. Be kind! ✨     ║
║                                                                              ║
║                     - Neo Qiss (The Mad Scientist of Code)                  ║
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
