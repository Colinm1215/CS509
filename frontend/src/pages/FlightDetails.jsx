import React, { useEffect, useState } from 'react';

const API_BASE_URL = 'http://localhost:8080';

export default function FlightDetails({ flightId, onClose }) {
    const [flight, setFlight] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [reserving, setReserving] = useState(false);

    useEffect(() => {
        async function fetchFlight() {
            try {
                const res = await fetch(`${API_BASE_URL}/flights/${flightId}`);
                if (!res.ok) throw new Error(`Status ${res.status}`);
                const data = await res.json();
                setFlight(data);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        }
        fetchFlight();
    }, [flightId]);

    const handleReserve = async () => {
        if (!window.confirm('Are you sure you want to reserve this flight?')) return;
        setReserving(true);
        try {
            const res = await fetch(
                `${API_BASE_URL}/flights/${flight.id}/reserve`,
                { method: 'POST' }
            );
            if (res.status === 409) {
                alert('Sorry, no seats available on that flight.');
                return;
            }
            if (!res.ok) {
                throw new Error(`Failed to reserve: ${res.status}`);
            }
            alert('Reservation confirmed!');
            onClose();
        } catch (err) {
            alert(`Error: ${err.message}`);
        } finally {
            setReserving(false);
        }
    };

    if (loading) {
        return (
            <div className="overlay fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                <div className="spinner text-white text-lg">
                    Loading flight details…
                </div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="overlay fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                <div className="error bg-white rounded-lg p-4">
                    Error: {error}
                    <button onClick={onClose} className="ml-4 text-blue-600">Close</button>
                </div>
            </div>
        );
    }

    if (!flight) {
        return (
            <div className="overlay fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
                <div className="bg-white rounded-lg p-4">
                    No flight found.
                    <button onClick={onClose} className="ml-4 text-blue-600">Close</button>
                </div>
            </div>
        );
    }

    return (
        <div className="overlay fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center">
            <div className="flight-details bg-white rounded-lg p-6 w-full max-w-md relative">
                <button
                    onClick={onClose}
                    className="absolute top-2 right-2 text-gray-600 hover:text-gray-900"
                >
                    ✕
                </button>

                <h2 className="text-xl font-bold mb-4">Flight {flight.number} Details</h2>
                <p><strong>Airline:</strong> {flight.airline}</p>
                <p>
                    <strong>Origin:</strong> {flight.origin} @{' '}
                    {new Date(flight.departureTime).toLocaleString()}
                </p>
                <p>
                    <strong>Destination:</strong> {flight.destination} @{' '}
                    {new Date(flight.arrivalTime).toLocaleString()}
                </p>

                <button
                    onClick={handleReserve}
                    disabled={reserving}
                    className="mt-4 bg-blue-600 text-white py-2 px-4 rounded hover:bg-blue-700 disabled:opacity-50"
                >
                    {reserving ? 'Reserving…' : 'Reserve'}
                </button>
            </div>
        </div>
    );
}