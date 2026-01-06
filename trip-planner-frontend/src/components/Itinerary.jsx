import { useEffect } from "react";
import { useNavigate } from "react-router-dom";
import { useTrip } from "../context/TripContext";

const Itinerary = () => {
  const { itinerary } = useTrip();
  const navigate = useNavigate();

  useEffect(() => {
    if (!itinerary) {
      navigate("/", { replace: true });
    }
  }, [itinerary, navigate]);

  if (!itinerary) return null;

  const open = (url) => {
    if (url) window.open(url, "_blank", "noopener,noreferrer");
  };

  return (
    <div style={{ background: "#f8fafc", minHeight: "100vh" }}>
      {/* HEADER */}
      <div
        style={{
          background: "linear-gradient(135deg, #2563eb, #1e40af)",
          color: "white",
          padding: "40px 24px",
          textAlign: "center",
        }}
      >
        <h1 style={{ fontSize: "32px", marginBottom: "8px" }}>
          {itinerary.origin} → {itinerary.destination}
        </h1>
        <p style={{ opacity: 0.9 }}>
          {itinerary.days}-day personalized travel plan
        </p>
      </div>

      <div style={{ maxWidth: "1100px", margin: "auto", padding: "32px 16px" }}>
        {/* DAY PLANS */}
        <h2 style={{ marginBottom: "24px" }}>Day-wise Experiences</h2>

        <div
          style={{
            display: "grid",
            gridTemplateColumns: "repeat(auto-fill, minmax(300px, 1fr))",
            gap: "28px",
          }}
        >
          {itinerary.dayPlans.map((plan) => (
            <div
              key={plan.dayNumber}
              onClick={() => open(plan.mapsUrl)}
              style={{
                background: "white",
                borderRadius: "16px",
                boxShadow: "0 10px 25px rgba(0,0,0,0.08)",
                cursor: "pointer",
                transition: "transform 0.2s ease, box-shadow 0.2s ease",
              }}
              onMouseEnter={(e) => {
                e.currentTarget.style.transform = "translateY(-6px)";
                e.currentTarget.style.boxShadow =
                  "0 18px 40px rgba(0,0,0,0.12)";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.transform = "none";
                e.currentTarget.style.boxShadow =
                  "0 10px 25px rgba(0,0,0,0.08)";
              }}
            >
              <img
                src={plan.imageUrl}
                alt={plan.placeName}
                style={{
                  width: "100%",
                  height: "180px",
                  objectFit: "cover",
                  borderTopLeftRadius: "16px",
                  borderTopRightRadius: "16px",
                }}
              />

              <div style={{ padding: "18px" }}>
                <span
                  style={{
                    display: "inline-block",
                    background: "#e0e7ff",
                    color: "#1e3a8a",
                    padding: "4px 12px",
                    borderRadius: "999px",
                    fontSize: "12px",
                    fontWeight: "600",
                    marginBottom: "10px",
                  }}
                >
                  Day {plan.dayNumber}
                </span>

                <h3 style={{ margin: "10px 0 6px" }}>
                  {plan.placeName}
                </h3>

                <p style={{ fontSize: "14px", color: "#475569" }}>
                  {plan.description}
                </p>

                <p
                  style={{
                    fontSize: "12px",
                    marginTop: "12px",
                    color: "#2563eb",
                    fontWeight: 600,
                  }}
                >
                  Open in Google Maps →
                </p>
              </div>
            </div>
          ))}
        </div>

        {/* HOTELS */}
        <h2 style={{ marginTop: "56px", marginBottom: "20px" }}>
          Nearby Stays
        </h2>

        <div style={{ display: "grid", gap: "16px" }}>
          {itinerary.hotels.map((hotel, i) => (
            <div
              key={i}
              onClick={() => open(hotel.searchUrl)}
              style={{
                background: "white",
                padding: "18px 20px",
                borderRadius: "12px",
                borderLeft: "6px solid #10b981",
                boxShadow: "0 6px 16px rgba(0,0,0,0.06)",
                cursor: hotel.searchUrl ? "pointer" : "default",
              }}
            >
              <h4 style={{ marginBottom: "6px" }}>{hotel.name}</h4>
              {hotel.address && (
                <p style={{ fontSize: "14px", color: "#475569" }}>
                  {hotel.address}
                </p>
              )}
              {hotel.searchUrl && (
                <p
                  style={{
                    fontSize: "12px",
                    marginTop: "6px",
                    color: "#059669",
                    fontWeight: 600,
                  }}
                >
                  Find hotels on Google Maps →
                </p>
              )}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default Itinerary;
