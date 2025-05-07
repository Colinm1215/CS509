import React, { useState } from 'react';

const API_BASE_URL = 'http://localhost:8080';

function getLastSegment(flight) {
    let current = flight;
    while (current?.nextFlight) current = current.nextFlight;
    return current;
}

function countSegments(flight) {
    let count = 0;
    let current = flight;
    while (current?.nextFlight) {
        count++;
        current = current.nextFlight;
    }
    return count;
}

function getTripDuration(start, end) {
    const startTime = new Date(start);
    const endTime = new Date(end);
    const diffMin = Math.floor((endTime - startTime) / 60000);
    return `${Math.floor(diffMin / 60)}h ${diffMin % 60}m`;
}

function parseAirline(airline) {
    const c = airline.trim().toUpperCase()[0];
    let a;
    switch (c) {
        case 'D':
            a = 'Delta';
            break;
        case 'S':
            a = 'Southwest';
            break;
        default:
            a = airline;
    }
    return a;
}

function renderFlightPath(flight, label) {
    const segments = [];
    const segmentIDs = [];
    let current = flight;
    while (current) {
        segments.push(current);
        segmentIDs.push(current.id)
        current = current.nextFlight;
    }

    return (
        <div className="mb-6">
            {segments.map((seg, i) => (
                <div key={i} className="mb-2">
                    <h3 className="text-lg font-semibold text-gray-800">
                        {label} – {seg.flightNumber}
                    </h3>
                    <p className="text-sm font-medium text-gray-700">
                        Airline: {parseAirline(seg.airline)}
                    </p>
                    <p className="text-sm font-medium text-gray-700">
                        Departure: {seg.departureAirport}
                    </p>
                    <p className="text-sm font-medium text-gray-700">
                        Arrival: {seg.arrivalAirport}
                    </p>
                    <p className="text-sm font-medium text-gray-700">
                        Depart: {new Date(seg.departureTime).toLocaleString()}
                    </p>
                    <p className="text-sm font-medium text-gray-700">
                        Arrive: {new Date(seg.arrivalTime).toLocaleString()}
                    </p>
                </div>
            ))}
            <p className="text-sm text-gray-600 mt-2">
                Total Duration: {getTripDuration(flight.departureTime, getLastSegment(flight).arrivalTime)} | Stops: {countSegments(flight)}
            </p>
        </div>
    );
}

export default function FlightDetails({ flight, onClose }) {
    const [reserving, setReserving] = useState(false);

    if (!flight) return null;

    const handleReserve = async () => {
        if (!window.confirm('Are you sure you want to reserve this flight?')) return;
        setReserving(true);
        try {
            const res = await fetch(`${API_BASE_URL}/flights/${flight.id}/reserve`, { method: 'POST' });
            if (res.status === 409) {
                alert('Sorry, no seats available on that flight.');
                return;
            }
            if (!res.ok) throw new Error(`Failed to reserve: ${res.status}`);
            alert('Reservation confirmed!');
            onClose();
        } catch (err) {
            alert(`Error: ${err.message}`);
        } finally {
            setReserving(false);
        }
    };

    const isRoundTrip = !!flight.returnTrip;

    return (
        <div className="overlay fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
            <div className="bg-white rounded-lg p-6 w-full max-w-2xl relative shadow-lg">
                <button
                    onClick={onClose}
                    className="absolute top-2 right-2 text-gray-600 hover:text-gray-900 text-xl font-bold"
                >
                    ✕
                </button>

                <h2 className="text-2xl font-bold mb-4 text-gray-900">Flight Details</h2>

                {renderFlightPath(flight, isRoundTrip ? 'Outbound Flight' : 'Flight')}

                {isRoundTrip && renderFlightPath(flight.returnTrip, 'Return Flight')}

                <div className="flex justify-end mt-4">
                    <button
                        onClick={handleReserve}
                        disabled={reserving}
                        className="bg-blue-600 text-white py-2 px-4 rounded hover:bg-blue-700 disabled:opacity-50"
                    >
                        {reserving ? 'Reserving…' : 'Reserve'}
                    </button>
                </div>
            </div>
        </div>
    );
}
