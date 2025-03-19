import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import './App.css';
import { motion, AnimatePresence } from 'framer-motion';

// URL for our backend API
const API_BASE_URL = 'http://localhost:8080/flights';

// Fetch function to call the API with query parameters including pagination.
const fetchFlights = async ({ queryKey }) => {
  const [_key, { departureAirport, arriveAirport, startTime, endTime, sortBy, page, pageSize }] = queryKey;
  const params = new URLSearchParams({
    departureAirport,
    arriveAirport,
    startTime,
    endTime,
    sortBy,
    page,
    pageSize,
  });
  const res = await fetch(`${API_BASE_URL}?${params.toString()}`);
  if (!res.ok) {
    throw new Error('Error fetching flights');
  }
  return res.json();
};

function App() {
  // Form state
  const [departureAirport, setDepartureAirport] = useState('');
  const [arriveAirport, setArriveAirport] = useState('');
  const [date, setDate] = useState('');
  const [sortBy, setSortBy] = useState('traveltime');

  // Pagination state
  const [page, setPage] = useState(1);
  const pageSize = 5; // Adjust as needed

  // Trigger query when form is submitted.
  const { data, error, isLoading, refetch } = useQuery({
    queryKey: ['flights',
      {
        departureAirport,
        arriveAirport,
        startTime: date.toString(),
        endTime: date.toString(),
        sortBy,
        page,
        pageSize }],
    queryFn: fetchFlights,
    enabled: false,
  });

  const handleSearch = (e) => {
    e.preventDefault();
    // Reset page on new search
    setPage(1);
    refetch();
  };

  const handleNext = () => {
    setPage(prev => prev + 1);
    refetch();
  };

  const handlePrev = () => {
    if (page > 1) {
      setPage(prev => prev - 1);
      refetch();
    }
  };

  return (
      <div className="min-h-screen bg-gray-50 p-6">
        <h1 className="text-4xl font-bold text-center text-gray-800 mb-8">Flight Search</h1>
        <form onSubmit={handleSearch} className="bg-white p-6 rounded-lg shadow-md flex flex-wrap gap-4 items-center justify-center">
          <input
              type="text"
              placeholder="Departure Airport (e.g., JFK)"
              value={departureAirport}
              onChange={(e) => setDepartureAirport(e.target.value.toUpperCase())}
              required
              className="input-field"
          />
          <input
              type="text"
              placeholder="Arrival Airport (e.g., LAX)"
              value={arriveAirport}
              onChange={(e) => setArriveAirport(e.target.value.toUpperCase())}
              required
              className="input-field"
          />
          <input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              required
              className="input-field"
          />
          <select
              value={sortBy}
              onChange={(e) => setSortBy(e.target.value)}
              className="input-field"
          >
            <option value="traveltime">Sort by Travel Time</option>
            <option value="arrivedatetime">Sort by Arrival Time</option>
            <option value="departdatetime">Sort by Departure Time</option>
          </select>
          <button type="submit" className="btn-primary">
            Search
          </button>
        </form>


        <div className="max-w-4xl mx-auto bg-white p-6 rounded-lg shadow-md">
          {/* Loading State */}
          {isLoading && (
              <div className="flex justify-center items-center py-8">
                <div className="animate-spin rounded-full h-10 w-10 border-t-4 border-blue-600"></div>
              </div>
          )}

          {/* Error Message */}
          {error && (
              <p className="text-center text-red-500 font-semibold py-4">
                {error.message}
              </p>
          )}

          {/* Flight Results */}
          {data && data.flights && data.flights.length > 0 ? (
              <>
                <ul className="space-y-4">
                  <AnimatePresence>
                    {data.flights.map((flight) => (
                        <motion.li
                            key={flight.id}
                            className="flex justify-between items-center bg-gray-50 p-6 rounded-lg shadow-sm hover:shadow-md transition"
                            initial={{ opacity: 0, y: 10 }}
                            animate={{ opacity: 1, y: 0 }}
                            exit={{ opacity: 0, y: -10 }}
                            transition={{ duration: 0.3 }}
                        >
                          {/* Flight Details */}
                          <div>
                            <h3 className="text-lg font-semibold text-blue-600">
                              {flight.flightNumber}
                            </h3>
                            <p className="text-gray-600">
                              {flight.departAirport} → {flight.arriveAirport}
                            </p>
                            <p className="text-gray-500 text-sm">
                              Depart: {flight.departDateTime} | Arrive: {flight.arriveDateTime}
                            </p>
                          </div>

                          {/* Placeholder for Airline Logo */}
                          <div className="w-12 h-12 bg-gray-300 rounded-full flex items-center justify-center">
                            ✈️
                          </div>
                        </motion.li>
                    ))}
                  </AnimatePresence>
                </ul>

                {/* Pagination */}
                <div className="flex justify-center items-center gap-4 mt-6">
                  <button
                      onClick={handlePrev}
                      disabled={page === 1}
                      className="bg-gray-500 text-white px-4 py-2 rounded-lg hover:bg-gray-600 disabled:opacity-50"
                  >
                    Previous
                  </button>
                  <span className="px-4 py-2 bg-blue-100 text-blue-700 rounded-lg">
          Page {page}
        </span>
                  <button
                      onClick={handleNext}
                      disabled={!data.hasMore}
                      className="bg-gray-500 text-white px-4 py-2 rounded-lg hover:bg-gray-600 disabled:opacity-50"
                  >
                    Next
                  </button>
                </div>
              </>
          ) : data ? (
              <p className="text-center text-gray-600 font-medium py-6">
                No flights found. Try another search.
              </p>
          ) : null}
        </div>

      </div>
  );
}

export default App;
