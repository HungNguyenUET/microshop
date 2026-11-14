#!/usr/bin/env bash
# Image JRE khong co curl/wget -> dung /dev/tcp cua bash de goi /actuator/health.
PORT="${1:?port required}"
exec 3<>"/dev/tcp/127.0.0.1/${PORT}" || exit 1
printf 'GET /actuator/health HTTP/1.1\r\nHost: localhost\r\nConnection: close\r\n\r\n' >&3
grep -q '"status":"UP"' <&3
