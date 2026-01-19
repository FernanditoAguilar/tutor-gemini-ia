import os
from google import genai

def list_my_models():
    # 1. Recuperar la API Key
    api_key = os.environ.get("GEMINI_API_KEY")
    
    if not api_key:
        print("❌ ERROR: No se encontró la variable GEMINI_API_KEY.")
        print("Asegúrate de haber ejecutado: export GEMINI_API_KEY='tu_clave'")
        return

    print("🛰️ Conectando con Google AI Services...")
    
    try:
        # 2. Inicializar el cliente (Nueva librería google-genai)
        client = genai.Client(api_key=api_key)
        
        print("✅ Conexión exitosa. Listando modelos disponibles:\n")
        print(f"{'MODEL NAME':<40} {'CAPABILITIES'}")
        print("-" * 60)

        # 3. Iterar y mostrar modelos
        for model in client.models.list():
            print(f"{model.name:<40} {model.supported_actions}")
            
    except Exception as e:
        print(f"❌ Ocurrió un error técnico: {e}")





# --- PUNTO DE ENTRADA ---
if __name__ == "__main__":
    list_my_models()