# 🚀 Dev Collaboration: Progress & Handover

> **Note**: This branch is for active development. The `main` branch remains the stable "Curated" version.

## ✅ Current Status
# 👗 Fashion Sensor — AI Outfit Recommender

An AI-powered outfit recommendation web app that suggests stylish outfits based on user input (style, audience, wardrobe, etc.) and provides shopping inspiration.

---

# 🚀 Tech Stack

- **Frontend:** HTML, CSS (Tailwind), JavaScript  
- **Backend:** Spring Boot (Java)  
- **AI Integration:** OpenAI (gpt-4o-mini)  
- **Image API:** Unsplash  

---

# 🧠 What Changed (Important)

Initially, the project used **Google Gemini API**, but it caused:
- ❌ 404 errors
- ❌ unstable responses

👉 So we **migrated to OpenAI**, which is now:
- ✔ Stable  
- ✔ Working  
- ⚠ Rate-limited  

---

# 📊 Current Status

| Component  | Status |
|------------|--------|
| Backend    | ✔ Working |
| Frontend   | ✔ Connected |
| OpenAI API | ✔ Working |
| Gemini     | ❌ Removed |
| Project    | 🚀 90% Complete |

---

# ⚠️ Known Issue

### 🔴 Error:

### 📌 Reason:
- Too many API calls (button clicked multiple times)

### ✅ Fix:
- Click **"Suggest Outfit" only once**
- Wait 20–30 seconds before retrying

---
# Create .env file
OPENAI_API_KEY=your_openai_api_key_here
UNSPLASH_ACCESS_KEY=your_unsplash_key_here
---
# How It Works

User selects:
Style
Audience
Wardrobe (optional)
Request goes to backend
Backend calls OpenAI API
AI returns JSON outfit suggestions
Frontend displays:
Outfit cards
Shopping items
Pinterest inspiration
---
# Pending Improvent 
 Add request cooldown (2–3 sec)
⏳ Disable button while loading
⏳ Better UI for outfit cards
⏳ Add outfit images
⏳ Save favorite outfits
---
# For Shaurya
IMPORTANT:
❌ Do NOT use Gemini anymore
❌ Do NOT add GEMINI_API_KEY
✅ Use OpenAI only
Steps:
Add OPENAI_API_KEY in .env
Run backend
Use frontend normally (single click)
---

# Open Frontend 
http://127.0.0.1:3000/frontend/index.html
|
<img width="870" height="901" alt="Screenshot 2026-05-03 at 11 35 09 PM" src="https://github.com/user-attachments/assets/86786a43-5b72-4905-b2a1-e71d9a58b8d7" />
|<img width="887" height="921" alt="Screenshot 2026-05-03 at 11 34 15 PM" src="https://github.com/user-attachments/assets/96ca06da-60b2-4b26-b129-e39eb08b4426" />


# 🛠 Setup Instructions

## 1. Clone & Checkout Branch

```bash
git fetch origin
git checkout dev-collaboration
git pull


