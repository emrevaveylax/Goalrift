# Goalrift (Android / Java)

A minimal **authoritative** local server-sim + client-side prediction 2D arcade football prototype inspired by Haxball.

## Modules
- `app`: Android launcher, menus.
- `game`: Core loop, physics, SurfaceView, input, prediction.
- `net`: DTOs and client connector interface.
- `backend-stubs`: Local authoritative server-sim and simple bot; replace with WebSocket in prod.
- `analytics`: Tiny event logger.
- `iap-ads`: Billing/Ads stub managers (no real SDKs).

## Build
Open the folder in Android Studio (Giraffe+). Compile SDK 34, minSdk 23. Run on device/emulator.

## Play
Tap on the menu to start an offline **1v1** vs a simple bot. Left stick to move, big button to shoot. Score by pushing the ball through the opponent goal gap.

## Notes
- Fixed physics step at 60 Hz; server snapshots at 30 Hz.
- Client prediction + reconciliation using `lastProcessedInputSeq`.
- Clean Java, no external engines.
- Extend `ClientConnector` to implement a WebSocket connector.
