"""
REPORTE DE INTEGRACIÓN - NÚCLEO POCHA3:
1. Localización: Este archivo actúa como el 'backend' que procesará los comandos de Android.
2. Personalidad: Se integra el System Prompt para asegurar empatía y acento rioplatense.
3. Accesibilidad: Se definen parámetros de audio específicos para baja audición.
"""

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

app = FastAPI(title="Pocha3_Brain")

# --- CONFIGURACIÓN DE PERSONALIDAD (Sugerida por el Arquitecto) ---
POCHA_SYSTEM_PROMPT = """
Sos "Pocha", una asistente virtual diseñada exclusivamente para acompañar y ayudar a personas mayores.
- Hablás con acento RIOPLATENSE (usás el 'voseo': 'vení', 'hacé', '¿cómo estás?').
- Sos dulce y paciente. Si el usuario duda, tranquilizalo.
- Ejemplo: "Hola querido, ¿cómo andás? Soy Pocha, estoy acá para lo que necesités."
"""

# --- CONFIGURACIÓN DE AUDIO PARA ACCESIBILIDAD ---
def get_voice_config():
    return {
        "language_code": "es-AR",
        "voice_name": "es-AR-Wavenet-C", 
        "pitch": -3.0,                  
        "speaking_rate": 0.85,           
        "volume_gain_db": 6.0           
    }

# --- ENDPOINTS DEL MICROSERVICIO ---
class VoiceRequest(BaseModel):
    text: str

@app.post("/intent")
async def process_intent(request: VoiceRequest):
    user_input = request.text.lower()
    
    # Ejemplo de lógica empática inmediata
    if "hola" in user_input or "pocha" in user_input:
        return {
            "action": "speak",
            "data": "Hola mi vida, acá estoy. ¿En qué te puedo ayudar?",
            "config": get_voice_config()
        }
    
    # Aquí irán las llamadas a Gemini que configuraremos luego
    return {"action": "think", "prompt": user_input}

"""
REPORTE DE INTEGRACIÓN - CEREBRO IA:
1. Cliente Gemini: Se inicializa el modelo para procesar lenguaje natural.
2. Manejo de Fallback: Si el comando no es una acción directa (como la hora), 
   Gemini genera una respuesta empática usando el System Prompt.
3. Seguridad: Recordar configurar la API_KEY en el entorno de IDX.
"""

import os
from google import genai
from fastapi import FastAPI
from pydantic import BaseModel

app = FastAPI(title="Pocha3_Brain")

# Configuración del cliente (Asegurate de tener la API_KEY configurada)
# Reporte: Usamos la librería moderna google-genai
client = genai.Client(api_key="TU_API_KEY_ACA") 

POCHA_SYSTEM_PROMPT = """
Sos "Pocha", una asistente para personas mayores. 
Hablás con voseo rioplatense, sos dulce, paciente y cariñosa.
Si te piden algo que no podés hacer, decilo con mucha suavidad.
"""

class VoiceRequest(BaseModel):
    text: str

@app.post("/intent")
async def process_intent(request: VoiceRequest):
    user_input = request.text.lower()
    
    # 1. Acciones Rápidas (Hardcoded para baja latencia)
    if "hora" in user_input:
        import datetime
        ahora = datetime.datetime.now().strftime("%H:%M")
        return {"action": "speak", "data": f"Son las {ahora}, querido."}

    # 2. IA Empática (Para todo lo demás)
    # Reporte: Consultamos a Gemini para que maneje la charla fluida
    response = client.models.generate_content(
        model="gemini-1.5-flash",
        config=genai.types.GenerateContentConfig(
            system_instruction=POCHA_SYSTEM_PROMPT,
            temperature=0.7 # Un poco de creatividad para que no suene igual siempre
        ),
        contents=request.text
    )
    
    return {
        "action": "speak",
        "data": response.text,
        "config": {
            "pitch": -3.0,
            "speaking_rate": 0.85
        }
    }




if __name__ == "__main__":
    import uvicorn
    # Reporte: Ejecutamos en el puerto 8000 para que Android pueda conectarse
    uvicorn.run(app, host="0.0.0.0", port=8000)