# IT342-PROJECTMANAGEMENTSYSTEM
# Team Members:

# Member 2
NAME: GABISON ANDREI MIGHEL A.
COURSE & YEAR: BSIT-3

My name's Andrei Mighel A. Gabison, I'm a 3rd year BSIT student. I live in Talisay City, Cebu. My hobbies are listening to music, sleeping, and etc. I plan to be a backend developer when after I graduate. I plan to travel around the world and enjoy everything life has to offer.


# Member1
NAME:EHRICA JYNNE M. ESPADA
COURSE & YEAR: BSIT -3


I am Ehrica but you can call me ica. 20 years of age. I currently live at Basak San Nicolas Cebu City. The reason why i took the program BSIT is because I want to learn more about programming and creating websites. I love the color pink and purple.

# Member3
NAME:LUIS MIGUEL A. JACA
COURSE & YEAR: BSIT-3

My name is Miguel or Migs for short, I love to watch CDrama. I love Zhao Lusi, Bai Lu and Wang Churan. I also play games during my free time. I am also a worship drummer in our church together with my two siblings. I play different kinds of instruments as well. 

## 💻 Project Overview

This is a fullstack web + mobile application for managing a project management system. It includes:

- 🌐 **Frontend:** ReactJS (with Vite)
- ⚙️ **Backend:** Spring Boot + Firebase
- 📱 **Mobile Client (Optional):** Android (Kotlin/Java)

---

## 🛠️ Tech Stack

| Layer      | Technology        |
|------------|-------------------|
| Frontend   | React + Vite, Axios, React Router DOM, MUI |
| Backend    | Spring Boot, Maven, Firebase Firestore DB  |
| Mobile App | Android Studio, Firebase                   |

---

## 🚀 Setup Instructions

### 🔷 Frontend (ReactJS + Vite)

```Terminal
cd PROJECT-MANAGEMENT-SYSTEM
npm install     # or yarn
npm run dev     # or yarn dev
```
Runs on http://localhost:5173

🔶 Backend (Spring Boot)
```Terminal
cd PROJECT-MANAGEMENT-SYSTEM
mvn spring-boot:run
```
Runs on: http://localhost:8080

✅ Firebase Setup for Backend

1.Create a Firebase project at firebase.google.com.
2.Generate a Service Account key JSON file.
3. Place it under <project-name>/src/main/resources/serviceAccountKey.json

📱 Android Studio Setup
📦 Prerequisites
Android Studio (latest)

Android SDK (API level 30+)

📲 Steps
1.Open the android/ folder in Android Studio.

2.Wait for Gradle to finish building.

3.Add a real device or emulator.

4.Run using Run ▶ or Shift + F10.

🔗 Connect to Backend API
Update the base URL in your Android app (Retrofit setup):
```Code:
const val BASE_URL = "http://10.0.2.2:8080/api/" // for Android emulator
```
🔐 Firebase Setup for Android
1.In Firebase Console, add your Android app.

2.Download google-services.json.

Place it in:
android/app/google-services.json

Project-level build.gradle:
classpath 'com.google.gms:google-services:4.3.15'

App-level build.gradle:
```Code:
apply plugin: 'com.google.gms.google-services'

dependencies {
    implementation 'com.google.firebase:firebase-auth'
    implementation 'com.google.firebase:firebase-database'
}

```
 
