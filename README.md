# Trip Planner Application

A full-stack Trip Planner application that generates a day-wise travel itinerary based on user preferences, destination, and trip duration. The system prioritizes user-selected categories and intelligently fills remaining days with high-priority recommendations.

---

## 🚀 Features

- Create trips with origin, destination, duration, and preferences
- Preference-based itinerary generation (Nature, Historic, Museum, Park, etc.)
- Intelligent fallback for remaining days without violating user intent
- Day-wise travel plan with images and descriptions
- Hotel suggestions near destination
- Clean, responsive, card-based UI
- Robust backend with RESTful APIs

---

## 🧱 Tech Stack

### Frontend
- Angular
- TypeScript
- HTML & CSS
- Responsive card-based UI

### Backend
- Java
- Spring Boot
- REST APIs
- Overpass API (tourist places & hotels)
- Geocoding API
- Wikipedia API (images)

### Tools
- Git & GitHub
- Maven
- Postman

---

## 🧠 How It Works

1. User creates a trip with preferences and number of days
2. Backend fetches nearby tourist places using geographic coordinates
3. Places are categorized and filtered strictly based on preferences
4. If days exceed matched preferences, top-priority non-conflicting places are recommended
5. Backend generates a day-wise itinerary and hotel suggestions
6. Frontend displays results in an interactive, user-friendly layout


