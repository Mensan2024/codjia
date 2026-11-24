#!/bin/bash
# Lance une commande et relève le pic de mémoire résidente (VmHWM) du processus Java et de ffmpeg.
"$@" &
PID=$!
PEAK_J=0; PEAK_F=0
while kill -0 $PID 2>/dev/null; do
  J=$(grep VmHWM /proc/$PID/status 2>/dev/null | awk '{print $2}'); [ -n "$J" ] && [ "$J" -gt "$PEAK_J" ] && PEAK_J=$J
  FP=$(pgrep -P $PID ffmpeg | head -1)
  if [ -n "$FP" ]; then F=$(grep VmHWM /proc/$FP/status 2>/dev/null | awk '{print $2}'); [ -n "$F" ] && [ "$F" -gt "$PEAK_F" ] && PEAK_F=$F; fi
  sleep 0.5
done
wait $PID; CODE=$?
echo "{\"rssPeakJavaMb\":$((PEAK_J/1024)),\"rssPeakFfmpegMb\":$((PEAK_F/1024)),\"exit\":$CODE}"
