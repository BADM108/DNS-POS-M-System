#!/usr/bin/env bash
# DNS BookShop POS - launcher for Linux/Mac
# Make sure Java 17 or newer is installed.
cd "$(dirname "$0")"
exec java -jar DNS-BookShop-POS.jar "$@"
