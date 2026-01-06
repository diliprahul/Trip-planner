import { useState } from "react";
import { createTrip, generateItinerary } from "../api/tripApi";
import { useTrip } from "../context/TripContext";
import { useNavigate } from "react-router-dom";
import "./TripForm.css";

const TripForm = () => {
  const { setItinerary, setLoading, loading, setError } = useTrip();
  const navigate = useNavigate();

  const [formData, setFormData] = useState({
    origin: "",
    destination: "",
    days: 1,
  });

  const handleChange = (e) => {
    setFormData({ ...formData, [e.target.name]: e.target.value });
  };

  const handleSubmit = async (e) => {
    e.preventDefault();

    try {
      setLoading(true);
      setError(null);

      const payload = {
        ...formData,
        categories: ["sightseeing"],
      };

      const createdTrip = await createTrip(payload);
      if (!createdTrip?.id) throw new Error("Trip ID missing");

      const itinerary = await generateItinerary(createdTrip.id);
      setItinerary(itinerary);
      navigate("/result");
    } catch (err) {
      console.error(err);
      setError("Itinerary generation failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="trip-form-wrapper">
      <div className="trip-form-card">
        <h1>Plan Your Trip</h1>
        <p className="subtitle">
          Create a personalized travel itinerary in seconds
        </p>

        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label>Origin</label>
            <input
              name="origin"
              placeholder="e.g. Vijayawada"
              value={formData.origin}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Destination</label>
            <input
              name="destination"
              placeholder="e.g. Hyderabad"
              value={formData.destination}
              onChange={handleChange}
              required
            />
          </div>

          <div className="form-group">
            <label>Days</label>
            <input
              type="number"
              name="days"
              min="1"
              value={formData.days}
              onChange={handleChange}
              required
            />
          </div>

          <button type="submit" disabled={loading}>
            {loading ? "Creating Trip..." : "Create Trip"}
          </button>
        </form>
      </div>
    </div>
  );
};

export default TripForm;
