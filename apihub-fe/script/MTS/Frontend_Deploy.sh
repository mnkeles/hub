#!/bin/bash

PORT=4054
PROJECT_DIR="$(cd "$(dirname "$0")/../.." && pwd)"
LOG_FILE="$PROJECT_DIR/logs/frontend.log"

echo "============================================"
echo " Frontend Deploy - Port: $PORT"
echo "============================================"

# --- 4054 portunda çalışan uygulama varsa durdur ---
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

# --- Frontend dizinine git ---
if [ ! -d "$PROJECT_DIR" ]; then
    echo "HATA: Frontend dizini bulunamadı: $PROJECT_DIR"
    exit 1
fi
cd "$PROJECT_DIR" || { echo "HATA: Frontend dizinine gidilemedi!"; exit 1; }

# --- npm run build ---
#echo "[2/3] Frontend build başlatılıyor..."
#npm run build
#if [ $? -ne 0 ]; then
#    echo "HATA: npm run build başarısız!"
#    exit 1
#fi
#echo "  -> Build başarılı."

# --- Uygulamayı başlat ---
echo "[3/3] Frontend uygulaması başlatılıyor..."
mkdir -p "$PROJECT_DIR/logs"
nohup npm start > "$LOG_FILE" 2>&1 &
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
echo " Frontend başarıyla deploy edildi!"
echo " Port: $PORT | PID: $NEW_PID"
echo "============================================"
