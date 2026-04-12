


# 👗 Fashion Sensor AI

An AI-powered outfit recommendation web app that suggests stylish outfits based on user inputs and wardrobe images.

---

## 🚀 Features

* 🧠 **AI-based outfit suggestions:** Intelligent matching based on occasion and style.
* 🖼️ **Dynamic Visuals:** Integrated **Unsplash API** to fetch high-quality clothing imagery in real-time.
* 📸 **Smart Analysis:** Upload wardrobe photos for context-aware style suggestions.
* 🛍️ **Direct Shopping:** Instant **Amazon Search** links generated for every recommended item.
* 🔄 **"Rethink Outfit":** One-click logic for instant variety and new combinations.
* 🧍 **Targeted Filtering:** Specialized logic for Men, Women, and Kids.
* ⚡ **Zero Latency UI:** Modern frontend that updates dynamically without page reloads.

---

## 🧠 How It Works

1. **Input:** User uploads an image or selects specific style preferences.
2. **Backend Processing:** Spring Boot logic processes the request and coordinates with the **Unsplash Proxy Service**.
3. **Visual Generation:** The backend fetches specific assets based on clothing tags (e.g., "Cargo Pants").
4. **Display:** Frontend renders a "Shopping Shortlist" with live price-check links and visual inspiration.

---

## 🛠️ Tech Stack

### **Frontend**
* **HTML5 & Tailwind CSS** (Responsive & Modern UI)
* **JavaScript (ES6+)** (Async API fetching & DOM manipulation)

### **Backend**
* **Java (Spring Boot)** (Enterprise-grade REST architecture)
* **Spring WebFlux** (Reactive handling for API calls)

### **Integration**
* **Unsplash API:** Powering the dynamic wardrobe visuals.
* **Amazon Affiliate Logic:** Powering the shopping redirects.

---

## 📂 Project Structure

```text
Fashion sensor/
│
├── backend/
│   ├── controller/ (FashionController.java - REST Endpoints)
│   ├── service/    (UnsplashProxyService.java - API Logic)
│   ├── model/      (SuggestionResponse.java - Data Mapping)
│   └── config/     (CorsConfig.java - Security)
│
├── frontend/
│   ├── index.html  (UI Structure)
│   ├── script.js   (Frontend Logic)
│   └── styles.css  (Custom Tailoring)
````

-----

## 📸 Screenshots

> **Tip:** Replace these placeholders with your actual project screenshots\!

| Home Screen | Suggestion Results |
|<img width="1792" height="926" alt="Screenshot 2026-04-12 at 1 55 50 PM" src="https://github.com/user-attachments/assets/81974c8d-657b-4c26-9208-0b1850ee35eb" />
| <img width="1791" height="1035" alt="Screenshot 2026-04-12 at 1 56 08 PM" src="https://github.com/user-attachments/assets/61931f0e-d6ba-48b9-881a-69345e66e1de" />

| <img width="1792" height="931" alt="Screenshot 2026-04-12 at 1 56 26 PM" src="https://github.com/user-attachments/assets/1ca3f2a3-b532-4c8a-85e2-314029d80d7d" />
 | <img width="1791" height="1032" alt="Screenshot 2026-04-12 at 1 56 40 PM" src="https://github.com/user-attachments/assets/9bb4355b-0b0c-4d7d-9bf9-31e2dab4f6d2" />
 |

-----

## ⚙️ Setup & Run

### 1\. Run Backend

```bash
cd backend
# Set your API Key for Unsplash
export UNSPLASH_ACCESS_KEY=your_key_here
./mvnw spring-boot:run
```

*Backend runs at: `http://localhost:8080`*

### 2\. Run Frontend

  * Open `frontend/index.html` via Live Server.
  * *Frontend runs at: `http://127.0.0.1:3000`*

-----

## 👨‍💻 Authors

  * **Shreyansh Keshari** — *Full Stack Developer & AI Lead*
  * **Shaurya Verma** — *Full Stack Developer & System Architect*

-----

## ⭐ Show Your Support

If you like this project:

  * Star ⭐ the repo
  * Connect with us on **LinkedIn** 🚀

<!-- end list -->

````

---

### **How to push this final version:**
1. Open your terminal in the `Fashion sensor` folder.
2. Run:
```bash
git add README.md
git commit -m "docs: finalized README with co-author and feature updates"
git push origin main
````


