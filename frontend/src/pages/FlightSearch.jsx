/* eslint-disable no-unused-vars */
import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { motion, AnimatePresence } from 'framer-motion';
import ReactDatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { CalendarIcon } from '@heroicons/react/24/outline';
import FlightDetails from './FlightDetails.jsx';

const API_BASE_URL = 'http://localhost:8080/flights';

const fetchFlights = async ({ queryKey }) => {
    const [_, params] = queryKey;
    const { departureAirport, arriveAirport, startTime, endTime, oneWay, returnDate, maxStops, airline, sortBy, page, pageSize } = params;

    const qs = new URLSearchParams();
    qs.append('departureAirport', departureAirport ?? '');
    qs.append('arriveAirport', arriveAirport ?? '');
    qs.append('startTime', startTime ?? '');
    qs.append('endTime', endTime ?? '');
    qs.append('oneWay',        oneWay ? 'true' : 'false');
    if (typeof maxStops === 'number') qs.append('maxStops', maxStops);
    else qs.append('maxStops', 0);
    qs.append('airline', airline ?? '');
    qs.append('returnDateStart', returnDate ?? '');
    qs.append('returnDateEnd', returnDate ?? '');
    qs.append('sortBy', sortBy);
    qs.append('page', page);
    qs.append('pageSize', pageSize);

    const res = await fetch(`${API_BASE_URL}?${qs.toString()}`);
    if (!res.ok) throw new Error('Error fetching flights');
    return res.json();
};

export default function FlightSearch() {
    const [departureAirport, setDepartureAirport] = useState('');
    const [arriveAirport, setArriveAirport] = useState('');
    const [date, setDate] = useState(null);
    const [oneWay, setOneWay] = useState(true);
    const [returnDate, setReturnDate] = useState(null);
    const [maxStops, setMaxStops] = useState('');
    const [airline, setAirline] = useState('');
    const [cabinClass, setCabinClass] = useState('');
    const [sortBy, setSortBy] = useState('traveltime');
    const [selectedFlightId, setSelectedFlightId] = useState(null);

    const [searchParams, setSearchParams] = useState({
        departureAirport: '',
        arriveAirport: '',
        startTime: '',
        endTime: '',
        oneWay: 'true',
        returnDate: '',
        maxStops: 0,
        sortBy: 'traveltime',
        page: 1,
        pageSize: 5,
    });

    const { data, error, isLoading } = useQuery({
        queryKey: ['flights', searchParams],
        queryFn: fetchFlights,
        keepPreviousData: true,
    });

    const handleSearch = (e) => {
        e.preventDefault();
        setSearchParams({
            departureAirport,
            arriveAirport,
            startTime: date ? date.toISOString() : '',
            endTime: date ? date.toISOString() : '',
            oneWay,
            returnDate: !oneWay && returnDate ? returnDate.toISOString() : '',
            maxStops: maxStops ? Number(maxStops) : '',
            airline,
            sortBy,
            page: 1,
            pageSize: 5,
        });
    };

    useEffect(() => {
        setSearchParams((prev) => ({
            ...prev,
            sortBy,
            page: 1,
        }));
    }, [sortBy]);

    const handleNext = () => {
        setSearchParams((prev) => ({ ...prev, page: prev.page + 1 }));
    };

    const handlePrev = () => {
        setSearchParams((prev) =>
            prev.page > 1 ? { ...prev, page: prev.page - 1 } : prev
        );
    };

    return (
        <div className="bg-gray-100 min-h-screen">
            <div className="container mx-auto px-4 py-8">
                <h1 className="text-2xl font-bold mb-6 text-gray-900">Flight Search</h1>

                <form
                    onSubmit={handleSearch}
                    className="bg-white rounded-lg shadow-md p-6 flex flex-wrap items-end gap-4 mb-8"
                >
                    {/* Departure Airport */}
                    <div className="flex flex-col w-full md:w-1/5">
                        <label className="font-semibold mb-1 text-gray-700">Departure Airport</label>
                        <input
                            type="text"
                            placeholder="e.g., JFK"
                            value={departureAirport}
                            onChange={(e) => setDepartureAirport(e.target.value.toUpperCase())}
                            className="border border-gray-300 rounded-md px-3 py-2 focus:outline-none text-gray-800"
                        />
                    </div>

                    {/* Arrival Airport */}
                    <div className="flex flex-col w-full md:w-1/5">
                        <label className="font-semibold mb-1 text-gray-700">Arrival Airport</label>
                        <input
                            type="text"
                            placeholder="e.g., LAX"
                            value={arriveAirport}
                            onChange={(e) => setArriveAirport(e.target.value.toUpperCase())}
                            className="border border-gray-300 rounded-md px-3 py-2 focus:outline-none text-gray-800"
                        />
                    </div>

                    {/* Round-trip Toggle */}
                    <div className="flex items-center w-full md:w-1/12 p-2 border border-gray-300 rounded-md bg-gray-50">
                        <input type="checkbox" checked={!oneWay} onChange={() => setOneWay((prev) => !prev)} id="roundTripToggle" className="mr-2" />
                        <label htmlFor="roundTripToggle" className="font-semibold text-gray-700">Round-trip</label>
                    </div>

                    {/* Departure Date */}
                    <div className="relative w-full md:w-1/6">
                        <label className="font-semibold mb-1 text-gray-700">Departure Date</label>
                        <div className="relative">
                            <ReactDatePicker
                                selected={date}
                                onChange={(d) => setDate(d)}
                                placeholderText="Select date"
                                className="border border-gray-300 rounded-md px-3 py-2 pl-10 focus:outline-none text-gray-800 w-full"
                                showMonthYearDropdown
                            />
                            <CalendarIcon
                                className="absolute right-3 top-1/2 h-5 w-5 text-gray-400 -translate-y-1/2 pointer-events-none"
                            />
                        </div>
                    </div>

                    {/* Return Date */}
                    <div className="relative w-full md:w-1/6">
                        <label className="font-semibold mb-1 text-gray-700">Return Date</label>
                        <div className="relative">
                            <ReactDatePicker
                                selected={returnDate}
                                onChange={(d) => setReturnDate(d)}
                                placeholderText="Select return date"
                                disabled={oneWay}
                                className={`border ${oneWay ? 'bg-gray-100 cursor-not-allowed' : 'border-gray-300'} rounded-md px-3 py-2 pl-10 focus:outline-none text-gray-800 w-full`}
                                showMonthYearDropdown
                            />
                            <CalendarIcon
                                className="absolute right-3 top-1/2 h-5 w-5 text-gray-400 -translate-y-1/2 pointer-events-none"
                            />
                        </div>
                    </div>

                    {/* Max Stops Filter */}
                    <div className="flex flex-col w-full md:w-1/12">
                        <label className="font-semibold mb-1 text-gray-700">Max Stops</label>
                        <input
                            type="number"
                            min="0"
                            placeholder="e.g., 1"
                            value={maxStops}
                            onChange={(e) => setMaxStops(e.target.value)}
                            className="border border-gray-300 rounded-md px-3 py-2 focus:outline-none text-gray-800"
                        />
                    </div>

                    {/* Airline Filter */}
                    <div className="flex flex-col w-full md:w-1/12">
                        <label className="font-semibold mb-1 text-gray-700">Airline</label>
                        <select
                            value={airline}
                            onChange={(e) => setAirline(e.target.value)}
                            className="border border-gray-300 rounded-md px-3 py-2 focus:outline-none text-gray-800"
                        >
                            <option value="">Any</option>
                            <option value="AA">American Airlines</option>
                            <option value="DL">Delta</option>
                            <option value="UA">United</option>
                        </select>
                    </div>

                    {/* Cabin Class Filter */}
                    <div className="flex flex-col w-full md:w-1/12">
                        <label className="font-semibold mb-1 text-gray-700">Class</label>
                        <select
                            value={cabinClass}
                            onChange={(e) => setCabinClass(e.target.value)}
                            className="border border-gray-300 rounded-md px-3 py-2 focus:outline-none text-gray-800"
                        >
                            <option value="">Any</option>
                            <option value="economy">Economy</option>
                            <option value="first">First</option>
                            <option value="business">Business</option>
                        </select>
                    </div>

                    {/* Sort By */}
                    <div className="flex flex-col w-full md:w-1/9">
                        <label className="font-semibold mb-1 text-gray-700">Sort By</label>
                        <select
                            value={sortBy}
                            onChange={(e) => setSortBy(e.target.value)}
                            className="border border-gray-300 rounded-md px-3 py-2 focus:outline-none text-gray-800"
                        >
                            <option value="traveltime">Travel Time</option>
                            <option value="arrivedatetime">Arrival Time</option>
                            <option value="departdatetime">Departure Time</option>
                        </select>
                    </div>

                    {/* Search Button */}
                    <div className="w-full md:w-1/12 flex justify-end ml-auto px-17 py-1">
                        <button type="submit" className="bg-blue-600 text-white font-semibold py-2 px-4 rounded-md hover:bg-blue-700 transition-colors">
                            Search
                        </button>
                    </div>
                </form>

                <div>
                    {isLoading && <div className="text-gray-600">Loading...</div>}
                    {error && <p className="text-red-500">{error.message}</p>}

                    {data && data.flights?.length > 0 ? (
                        <>
                            <ul className="space-y-4 mb-6">
                                <AnimatePresence>
                                    {data.flights.map((flight) => (
                                        <motion.li
                                            key={flight.id}
                                            onClick={() => setSelectedFlightId(flight.id)}
                                            initial={{ opacity: 0, y: 10 }}
                                            animate={{ opacity: 1, y: 0 }}
                                            exit={{ opacity: 0, y: -10 }}
                                            transition={{ duration: 0.3 }}
                                            style={{ cursor: 'pointer' }}
                                            className="bg-white rounded-lg shadow p-4 flex items-center justify-between"
                                        >
                                            <div>
                                                <h3 className="text-lg font-semibold text-gray-800">{flight.flightNumber}</h3>
                                                <p className="text-gray-600">
                                                    {flight.departureAirport} → {flight.arriveAirport}cd
                                                </p>
                                                <p className="text-sm text-gray-500">
                                                    Depart: {new Date(flight.departureTime).toLocaleString()} |
                                                    Arrive: {new Date(flight.arrivalTime).toLocaleString()}
                                                </p>
                                            </div>
                                            <div className="text-2xl">✈️</div>
                                        </motion.li>
                                    ))}
                                </AnimatePresence>
                            </ul>

                            <div className="flex items-center justify-center gap-4">
                                <button
                                    type="button"
                                    onClick={handlePrev}
                                    disabled={searchParams.page === 1}
                                    className={`px-4 py-2 rounded-md font-medium 
                    ${searchParams.page === 1 ? 'bg-gray-300 text-gray-600 cursor-not-allowed' : 'bg-blue-600 text-white hover:bg-blue-700 transition-colors'}`}
                                >
                                    Previous
                                </button>

                                <span className="font-semibold text-gray-800">Page {searchParams.page}</span>

                                <button
                                    type="button"
                                    onClick={handleNext}
                                    disabled={!data.hasMore}
                                    className={`px-4 py-2 rounded-md font-medium 
                    ${!data.hasMore ? 'bg-gray-300 text-gray-600 cursor-not-allowed' : 'bg-blue-600 text-white hover:bg-blue-700 transition-colors'}`}
                                >
                                    Next
                                </button>
                            </div>

                            {selectedFlightId && (
                                <FlightDetails
                                    flightId={selectedFlightId}
                                    onClose={() => setSelectedFlightId(null)}
                                />
                            )}
                        </>
                    ) : (
                        data && <p className="text-gray-600">No flights found. Try another search.</p>
                    )}
                </div>
            </div>
        </div>
    );
}
