# Demo video checklist (record on a real phone, voice-over, uploaded unlisted to YouTube)

1. **Register with Google (SSO)** - show the account picker, land in the app. Accept the "fingerprint login?" offer.
2. **Close and reopen the app** - show the fingerprint/face unlock screen.
3. **Settings** - change name > Save > leave Settings > come back: the new name is still there (feedback fix). Switch language to Afrikaans, then isiZulu, then back.
4. **Sell** - add a photo (camera), tap *Scan textbook barcode*, scan a real textbook -> title fills in. Try a grocery barcode -> "not a book barcode". Publish.
5. **Offline mode** - turn on airplane mode: banner appears, listings still show (cached). Create a listing -> "saved offline". Turn airplane mode off -> it uploads by itself and a notification appears.
6. **Second phone / account** - make an offer on the first account's listing -> first phone gets a **push notification** in real time. Schedule a meeting -> seller gets a push.
7. **Meet tab** - buyer shows the 6-digit code; seller types it in -> confirmed. Try confirming again / with buyer account -> rejected (feedback fix).
8. **Profile** - Student Score went up, First Trade badge unlocked.
9. **Saved listings** - heart a listing, open Saved listings.
10. **Behind the scenes** - Swagger at `/swagger`, `/api/v1/health`, the Azure blob container with the photo, the GitHub Actions run (green, with APK artifact), and Logcat showing log messages.
