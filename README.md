# Rex — Setup Guide (100% Free)

Yeh ek Android app hai jo "Jarvis" bolte hi background se jaag jaati hai, tera sawaal sunti hai,
aur bolkar jawab deti hai (color grading, fitness, tech, sab kuch) — Siri jaisa experience.
Poori tarah free tools se banaya gaya hai.

Neeche jo steps hain, sirf tu hi kar sakta hai (accounts banana, keys lena) — code sab ready hai.

## Step 1 — Google Gemini API key lo (FREE)
1. https://aistudio.google.com/app/apikey par jaao (Google account se login).
2. "Create API key" dabao — koi credit card nahi maangega.
3. Free tier mein rate-limit hai (kuch requests per minute) — ek akele insaan ke
   personal use ke liye kaafi zyada hai.

## Step 2 — Backend deploy karo (Netlify — free)
1. Is poore `FramexAssistant` folder ko GitHub repo mein daalo (ya Netlify par seedha
   `netlify/functions` folder wala project drag-drop karo).
2. Netlify dashboard mein: Site settings -> Environment variables -> naya variable add karo:
   - Key: `GEMINI_API_KEY`
   - Value: Step 1 wali key
3. Deploy hone ke baad tumhe ek URL milega jaisa:
   `https://your-site-name.netlify.app/.netlify/functions/chat`
4. Yeh URL copy karke `app/src/main/java/com/framex/assistant/BackendClient.kt` file mein
   `BACKEND_URL` variable mein paste karo (line jaha `REPLACE-WITH-YOUR-NETLIFY-SITE` likha hai).

## Step 3 — "Rex" wake-word banao (FREE, custom)
"Rex" Picovoice ke free built-in words mein nahi hai, isliye custom train karna padega —
lekin yeh bhi free hai aur console pe hi ho jaata hai, coding nahi karni:
1. https://console.picovoice.ai par free account banao aur login karo.
2. Wahan se apna **AccessKey** copy karo — ise
   `app/src/main/java/com/framex/assistant/WakeWordService.kt` file mein
   `PICOVOICE_ACCESS_KEY` variable mein paste karo.
3. Console ke andar "Porcupine" (wake word) section mein jaao, "Create Wake Word" dabao.
4. Word likho: **Rex** (ya "Hey Rex" agar zyada natural lagna chahiye).
5. Platform select karo: **Android**.
6. Train hone do (kuch second lagenge), phir download button se `.ppn` file milegi.
7. Us file ka naam badal kar **`rex.ppn`** rakho.
8. Is `rex.ppn` file ko project ke andar is folder mein daalo:
   `FramexAssistant/app/src/main/assets/rex.ppn`
   (assets folder already project mein bana hua hai, khaali hai — bas file waha daal do)
9. Bas — ab app "Rex" bolte hi jaag jaayegi.

## Step 4 — Android Studio mein build karo (FREE)
1. Android Studio install karo (free, developer.android.com/studio).
2. "Open" karke is `FramexAssistant` folder ko open karo.
3. Gradle sync hone do (pehli baar thoda time lagega, internet chahiye).
4. Apna phone USB se connect karo (Developer Options + USB Debugging on karke),
   ya emulator use karo.
5. Green "Run" button dabao — app phone pe install ho jayegi.

## Step 5 — Test karo
1. App kholo, "Start Assistant" button dabao, mic permission allow karo.
2. Notification bar mein "Listening for Jarvis…" dikhega.
3. Bolo: "Jarvis" — phir apna sawaal bolo, jaise "Jarvis, explain color temperature".
4. Jawab bolkar milega, aur agar English mein galti thi toh pehle correction bhi sunayi degi.

## Free tier limits jo jaanna zaroori hai
- Gemini free tier: kaafi generous hai personal use ke liye, lekin agar bahut zyada
  request bhejoge thodi der mein (jaise ek minute mein bahut saare sawal), rate-limit
  lag sakta hai — thoda ruk kar dobara try karna padega.
- Picovoice free tier: har mahine limited "activations" (wake-word detections) milte
  hain — ek akele user ke liye normally kaafi hote hain.
- Netlify free tier: functions ke liye monthly limit hai, personal app ke liye kaafi hai.
- Koi bhi cheez kabhi bhi paid nahi maangegi jab tak tu khud upgrade na kare.

## Important notes
- Yeh app tabhi kaam karegi jab phone mein internet ho (backend call ke liye).
- Battery thodi zyada use hogi kyunki mic hamesha wake-word ke liye active rehta hai.
- Agar Play Store par publish karna hai, toh usska alag process hai (signing, listing,
  review) — jab yahan tak pahunch jao tab bata dena, wo bhi step-by-step kar denge.
