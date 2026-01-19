"""
REPORTE DE ARQUITECTURA - ASISTENTE DE ACCESIBILIDAD:
1. Endpoint /process-voice: Recibe el texto reconocido por Android.
2. Lógica de Intenciones: Clasifica si el usuario quiere la hora, radio o llamar.
3. Integración con Gemini: Usamos la IA para manejar peticiones complejas o informales.
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import datetime

app = FastAPI(title="Pocha3_Brain")

class VoiceRequest(BaseModel):
    text: str  # El texto que Android reconoció de la voz del abuelo

@app.post("/intent")
async def process_intent(request: VoiceRequest):
    """
    Analiza lo que el usuario dijo y decide la acción.
    Reporte: Se usa una lógica de 'Routing' para derivar a diferentes funciones.
    """
    user_input = request.text.lower()
    
    # 1. Acción: Pedir la hora
    if "hora" in user_input:
        ahora = datetime.datetime.now().strftime("%H:%M")
        return {"action": "speak", "data": f"Son las {ahora}"}
    
    # 2. Acción: Escuchar radio
    elif "radio" in user_input:
        # Aquí enviaríamos la URL del streaming para que Android la reproduzca
        return {
            "action": "play_stream", 
            "data": "http://radio.stream.url", 
            "message": "Sintonizando la radio"
        }
    
    # 3. Acción: Llamar a familiar
    elif "llamar a" in user_input:
        familiar = user_input.replace("llamar a", "").strip()
        return {"action": "call", "target": familiar}

    # 4. Fallback: Si no entiende, consulta a Gemini
    else:
        # Aquí llamaríamos a la función de Gemini que ya configuramos
        return {"action": "speak", "data": "No estoy segura de cómo ayudarte con eso, ¿querés que llame a alguien?"}

if __name__ == "__main__":
    import uvicorn
    uvicorn.run(app, host="0.0.0.0", port=8000)