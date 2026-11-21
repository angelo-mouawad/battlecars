#pragma once

#define WIFI_SSID     "your-wifi-name"
#define WIFI_PASSWORD "your-wifi-password"

#define WS_HOST    "localhost"
#define WS_PORT    8080
#define WS_PATH    "/ws?client=esp"
#define WS_USE_SSL 0

// Must match HUB_TOKEN on the backend, otherwise the hub is refused.
#define HUB_TOKEN  "paste-the-backend-HUB_TOKEN-here"
