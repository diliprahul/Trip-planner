import { createContext, useContext, useState } from "react";

const TripContext = createContext();

export const TripProvider = ({ children }) => {
  const [tripRequest, setTripRequest] = useState(null);
  const [itinerary, setItinerary] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(null);

  return (
    <TripContext.Provider
      value={{
        tripRequest,
        setTripRequest,
        itinerary,
        setItinerary,
        loading,
        setLoading,
        error,
        setError,
      }}
    >
      {children}
    </TripContext.Provider>
  );
};

export const useTrip = () => useContext(TripContext);
