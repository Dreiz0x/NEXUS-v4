# NEXUS — Guía Rápida de Compilación con GitHub Actions

## Paso 1: Crear un Repositorio en GitHub

1. Ve a [github.com/new](https://github.com/new)
2. Nombre del repositorio: `nexus-app`
3. Visibilidad: **Public** (para usar GitHub Actions gratis) o **Private**
4. **No** inicialices con README (ya tenemos uno)
5. Click en **Create repository**

## Paso 2: Subir el Código

Abre una terminal en la carpeta del proyecto y ejecuta:

```bash
cd nexus-app

# Inicializar Git
git init
git add .
git commit -m "feat: NEXUS v1.0.0 - Personal Document Intelligence System"

# Conectar con tu repositorio (reemplaza TU_USUARIO)
git remote add origin https://github.com/TU_USUARIO/nexus-app.git
git branch -M main
git push -u origin main
```

## Paso 3: Esperar la Compilación Automática

1. Ve a tu repositorio en GitHub
2. Click en la pestaña **Actions**
3. Verás el workflow **"Build NEXUS APK"** ejecutándose automáticamente
4. Espera ~5-10 minutos a que termine (icono verde = éxito)

## Paso 4: Descargar el APK

1. En la pestaña **Actions**, click en el workflow completado
2. Baja hasta la sección **Artifacts**
3. Click en **nexus-debug-apk** para descargar el ZIP
4. Descomprime el ZIP para obtener `app-debug.apk`

## Paso 5: Instalar en tu Dispositivo

1. Transfiere el APK a tu teléfono Android (cable USB, email, Drive, etc.)
2. En tu teléfono, ve a **Ajustes > Seguridad > Fuentes desconocidas** y actívalo
3. Abre el archivo APK para instalarlo
4. Cuando la app se abra, concede el permiso de almacenamiento

## Paso 6: Configurar la API Local (Opcional)

Para habilitar la búsqueda semántica con IA:

### Opción A: Usando Ollama (Recomendado)

```bash
# Instalar Ollama en tu PC
curl -fsSL https://ollama.ai/install.sh | sh

# Descargar un modelo
ollama pull llama3.2

# Iniciar el servidor (compatible con OpenAI API)
OLLAMA_HOST=0.0.0.0:8080 ollama serve
```

### Opción B: Usando llama.cpp

```bash
# Descargar llama.cpp y un modelo GGUF
./llama-server -m modelo.gguf --host 0.0.0.0 --port 8080
```

Luego en NEXUS, ve a **Configuración > API ENDPOINT** y pon la IP de tu PC:
```
http://192.168.1.XXX:8080
```

## Paso 7: Configurar OCR (Opcional)

Para que NEXUS pueda leer texto de imágenes:

1. Descarga `eng.traineddata` de: https://github.com/tesseract-ocr/tessdata/raw/main/eng.traineddata
2. Colócalo en `app/src/main/assets/tessdata/eng.traineddata`
3. Vuelve a compilar y subir a GitHub

---

## Solución de Problemas

### El workflow falla en GitHub Actions

- Revisa los logs en la pestaña Actions
- Asegúrate de que el repositorio sea público (o tengas minutos de Actions disponibles)

### La app no indexa documentos

- Verifica que concediste el permiso de "Acceso a todos los archivos"
- En Android 11+: Ajustes > Apps > NEXUS > Permisos > Archivos y multimedia > Permitir gestión de todos los archivos

### La búsqueda semántica no funciona

- Verifica que tu API local está corriendo: abre `http://127.0.0.1:8080/v1/models` en un navegador
- En NEXUS, ve a Configuración y usa el botón [TEST] para verificar la conexión
- Si la API está en otra máquina, asegúrate de que ambos dispositivos están en la misma red WiFi

---

**NEXUS v1.0.0 // 100% LOCAL // ZERO TELEMETRY**
