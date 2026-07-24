#!/usr/bin/env bash

set -euo pipefail

PORT=4054
APP_URL="http://127.0.0.1:${PORT}"

# Jenkins job bittikten sonra arka plandaki process'i oldurmesin.
export BUILD_ID=dontKillMe
export JENKINS_NODE_COOKIE=dontKillMe

export NEXT_PUBLIC_API_URL="http://172.31.27.4:4053"

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(cd "${SCRIPT_DIR}/../.." && pwd)"
LOG_DIR="${PROJECT_DIR}/logs"
LOG_FILE="${LOG_DIR}/dev.log"

find_port_pids() {
  if command -v lsof >/dev/null 2>&1; then
    lsof -ti TCP:${PORT} -sTCP:LISTEN 2>/dev/null || true
    return
  fi

  if command -v fuser >/dev/null 2>&1; then
    fuser ${PORT}/tcp 2>/dev/null || true
    return
  fi

  if command -v ss >/dev/null 2>&1; then
    ss -ltnp "sport = :${PORT}" 2>/dev/null \
      | sed -n 's/.*pid=\([0-9]\+\).*/\1/p' \
      | sort -u
    return
  fi

  echo "[ERROR] Port kontrolu icin lsof, fuser veya ss komutlarindan biri gerekli." >&2
  exit 1
}

is_app_ready() {
  if command -v curl >/dev/null 2>&1; then
    curl -fsS --max-time 5 "${APP_URL}" >/dev/null 2>&1
    return
  fi

  if command -v wget >/dev/null 2>&1; then
    wget -q --timeout=5 --spider "${APP_URL}" >/dev/null 2>&1
    return
  fi

  [[ -n "$(find_port_pids)" ]]
}

print_app_log() {
  if [[ -f "${LOG_FILE}" ]]; then
    echo "[INFO] ${LOG_FILE} son 80 satir:"
    tail -n 80 "${LOG_FILE}" || true
  else
    echo "[WARNING] Log dosyasi bulunamadi: ${LOG_FILE}"
  fi
}

echo "[INFO] Port ${PORT} kontrol ediliyor..."
PIDS="$(find_port_pids)"

if [[ -n "${PIDS}" ]]; then
  echo "[INFO] Port ${PORT} uzerinde calisan uygulama bulundu, durduruluyor..."
  kill ${PIDS} || true

  for _ in {1..10}; do
    if [[ -z "$(find_port_pids)" ]]; then
      break
    fi
    sleep 1
  done

  REMAINING_PIDS="$(find_port_pids)"
  if [[ -n "${REMAINING_PIDS}" ]]; then
    echo "[WARNING] Uygulama normal sekilde durmadi, zorla sonlandiriliyor..."
    kill -9 ${REMAINING_PIDS} || true
  fi

  echo "[SUCCESS] Port ${PORT} bosaltildi."
else
  echo "[INFO] Port ${PORT} bos, devam ediliyor."
fi

cd "${PROJECT_DIR}"
echo "[INFO] Calisma dizini: $(pwd)"

if ! command -v node >/dev/null 2>&1; then
  echo "[ERROR] node komutu bulunamadi. Node.js 20.9.0 veya uzeri kurulu olmali." >&2
  exit 1
fi

if ! command -v npm >/dev/null 2>&1; then
  echo "[ERROR] npm komutu bulunamadi. npm kurulu olmali." >&2
  exit 1
fi

echo "[INFO] npm run build baslatiliyor..."
npm run build
echo "[SUCCESS] Build tamamlandi."

mkdir -p "${LOG_DIR}"

echo "[INFO] npm run dev arka planda baslatiliyor (port ${PORT})..."
: > "${LOG_FILE}"

if command -v setsid >/dev/null 2>&1; then
  setsid npm run dev > "${LOG_FILE}" 2>&1 < /dev/null &
else
  nohup npm run dev > "${LOG_FILE}" 2>&1 < /dev/null &
fi

APP_PID=$!

echo "[INFO] Uygulamanin ayaga kalkmasi bekleniyor..."
for i in {1..30}; do
  if is_app_ready; then
    echo "[SUCCESS] Uygulama basariyla ayaga kalkti."
    echo "[INFO] URL: ${APP_URL}"
    echo "[INFO] PID: ${APP_PID}"
    echo "[INFO] Loglar: ${LOG_FILE}"
    echo "[INFO] Deploy islemi basariyla tamamlandi."
    exit 0
  fi

  if ! kill -0 "${APP_PID}" >/dev/null 2>&1; then
    echo "[ERROR] npm run dev beklenmedik sekilde kapandi."
    print_app_log
    exit 1
  fi

  echo "[INFO] Bekleniyor... (${i}/30)"
  sleep 5
done

echo "[ERROR] Uygulama 90 saniye icerisinde ayaga kalkmadi."
print_app_log
exit 1