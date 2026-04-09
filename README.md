# 👗 Fashion Sensor AI

An AI-powered outfit recommendation web app that suggests stylish outfits based on user inputs and wardrobe images.

---

## 🚀 Features

* 🧠 AI-based outfit suggestions
* 📸 Upload wardrobe image for smart analysis
* 👕 Multiple outfit recommendations
* 🔄 "Rethink Outfit" button for new suggestions
* 🧍 Gender-based filtering (Men / Women / Kids)
* 🎨 Style-based filtering (Casual, Formal, Streetwear)
* 🛍️ Amazon product links for outfit items
* 📌 Pinterest-style inspiration (dynamic images)
* ⚡ Dynamic frontend (no page reloads)

---

## 🧠 How It Works

1. User uploads an image OR selects preferences
2. Backend processes input (AI + rules)
3. Generates multiple outfit combinations
4. Frontend displays:

   * Outfit suggestions
   * Detected items
   * Amazon links
   * Style inspiration images

---

## 🛠️ Tech Stack

### Frontend:

* HTML
* Tailwind CSS
* JavaScript

### Backend:

* Java (Spring Boot)
* REST APIs

### AI / Logic:

* Rule-based + AI-ready architecture
* Image handling (multipart upload)

---

## 📸 Screenshots

*Add screenshots here after UI polish*

---

## 📂 Project Structure

```
Fashion sensor/
│
├── backend/
│   ├── controller/
│   ├── service/
│   ├── model/
│   └── config/
│
├── frontend/
│   ├── index.html
│   ├── script.js
│   └── styles.css
```

---

## ⚙️ Setup & Run

### 1. Run Backend

```bash
cd backend
./mvnw spring-boot:run
```

Backend runs at:

```
http://localhost:8080
```

---

### 2. Run Frontend

* Open `frontend/index.html`
* Run with Live Server

Frontend runs at:

```
http://127.0.0.1:3000
```

---

## 🔗 API Endpoint

```
POST /api/suggest
```

Handles:

* Form data
* Image upload
* Returns outfit suggestions

---

## 💡 Future Improvements

* 🤖 Real AI vision model integration
* 👤 User login & saved outfits
* 🧥 Virtual wardrobe management
* 📱 Mobile responsiveness improvements
* 🌐 Deployment (live hosting)

---

## 👨‍💻 Author

**Shreyansh Keshari**

---

## ⭐ Show Your Support

If you like this project:

* Star ⭐ the repo
* Share it on LinkedIn 🚀
