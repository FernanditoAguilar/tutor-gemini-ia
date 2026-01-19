import os
from google import genai

def test_specific_model():
    api_key = os.environ.get("GEMINI_API_KEY")
    client = genai.Client(api_key=api_key)
    
    # Intentamos llamar directamente al modelo más moderno
    model_id = "gemini-1.5-flash" 
    
    print(f"🧪 Probando conexión directa con: {model_id}...")
    
    try:
        response = client.models.generate_content(
            model=model_id,
            contents="Dime 'Hola Fernando, estoy operativo' en inglés técnico."
        )
        print(f"✅ RESPUESTA DEL MODELO: {response.text}")
    except Exception as e:
        print(f"❌ El modelo {model_id} no respondió.")
        print(f"Detalle del error: {e}")

if __name__ == "__main__":
    test_specific_model()