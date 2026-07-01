#!/bin/bash

PORT=4053
JAR_NAME="omniAutomation.jar"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PROJECT_DIR="$(cd "$SCRIPT_DIR/../.." && pwd)"
LOG_FILE="$PROJECT_DIR/logs/backend.log"

echo "============================================"
echo " Backend Deploy - Port: $PORT"
echo " Proje Dizini: $PROJECT_DIR"
echo "============================================"

# --- 4053 portunda çalışan uygulama varsa durdur ---
echo "[1/3] Port $PORT kontrol ediliyor..."
PID=$(lsof -ti tcp:$PORT)
if [ -n "$PID" ]; then
    echo "  -> PID $PID durdurulyor..."
    kill -9 $PID
    sleep 2
    echo "  -> Uygulama durduruldu."
else
    echo "  -> Port $PORT boş, devam ediliyor."
fi

# --- Maven clean install ---
echo "[2/3] Maven build başlatılıyor..."
cd "$PROJECT_DIR" || { echo "HATA: Proje dizinine gidilemedi!"; exit 1; }

mvn clean install -DskipTests
if [ $? -ne 0 ]; then
    echo "HATA: Maven build başarısız!"
    exit 1
fi
echo "  -> Build başarılı."

# --- JAR dosyasını bul ---
JAR_PATH=$(find "$PROJECT_DIR/target" -name "*.jar" -not -name "*sources*" -not -name "*javadoc*" | head -1)
if [ -z "$JAR_PATH" ]; then
    echo "HATA: JAR dosyası bulunamadı! (target/ klasörü kontrol edildi)"
    exit 1
fi
echo "  -> JAR bulundu: $JAR_PATH"

# --- Uygulamayı başlat ---
echo "[3/3] Uygulama başlatılıyor..."
mkdir -p "$PROJECT_DIR/logs"
nohup java -jar "$JAR_PATH" > "$LOG_FILE" 2>&1 &
NEW_PID=$!
echo "  -> Uygulama PID $NEW_PID ile başlatıldı."
echo "  -> Log dosyası: $LOG_FILE"

sleep 3
if kill -0 $NEW_PID 2>/dev/null; then
    echo "  -> Uygulama başarıyla çalışıyor."
else
    echo "HATA: Uygulama başlatılamadı! Log kontrol edin: $LOG_FILE"
    exit 1
fi

echo "============================================"
echo " Backend başarıyla deploy edildi!"
echo " Port: $PORT | PID: $NEW_PID"
echo "============================================"
