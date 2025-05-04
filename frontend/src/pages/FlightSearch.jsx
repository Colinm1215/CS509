import { useEffect, useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import {  AnimatePresence } from 'framer-motion';
import ReactDatePicker from 'react-datepicker';
import 'react-datepicker/dist/react-datepicker.css';
import { CalendarIcon } from '@heroicons/react/24/outline';

const API_BASE_URL = 'http://localhost:8080/flights';

const fetchFlights = async ({ queryKey }) => {
    const [_, params] = queryKey;
    const { departureAirport, arriveAirport, startTime, endTime, sortBy, page, pageSize } = params;

    const qs = new URLSearchParams();
    qs.append('departureAirport', departureAirport ?? '');
    qs.append('arriveAirport', arriveAirport ?? '');
    qs.append('startTime', startTime ?? '');
    qs.append('endTime', endTime ?? '');
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
    const [sortBy, setSortBy] = useState('traveltime');

    const [searchParams, setSearchParams] = useState({
        departureAirport: '',
        arriveAirport: '',
        startTime: '',
        endTime: '',
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
                    className="bg-white rounded-lg shadow-md p-6 flex flex-col md:flex-row md:items-end gap-4 mb-8"
                >
                    <div className="flex flex-col w-full md:w-1/4">
                        <label className="font-semibold mb-1 text-gray-700">Departure Airport</label>
                        <input
                            type="text"
                            placeholder="e.g., JFK"
                            value={departureAirport}
                            onChange={(e) => setDepartureAirport(e.target.value.toUpperCase())}
                            className="border border-gray-300 rounded-md px-3 py-2 focus:outline-none text-gray-800"
                        />
                    </div>

                    <div className="flex flex-col w-full md:w-1/4">
                        <label className="font-semibold mb-1 text-gray-700">Arrival Airport</label>
                        <input
                            type="text"
                            placeholder="e.g., LAX"
                            value={arriveAirport}
                            onChange={(e) => setArriveAirport(e.target.value.toUpperCase())}
                            className="border border-gray-300 rounded-md px-3 py-2 focus:outline-none text-gray-800"
                        />
                    </div>

                    <div className="relative w-full md:w-1/4">
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

                    <div className="flex flex-col w-full md:w-1/5">
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

                    <button
                        type="submit"
                        className="bg-blue-600 text-white font-semibold py-2 px-4 rounded-md hover:bg-blue-700 transition-colors md:mb-0"
                    >
                        Search
                    </button>
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
                                            initial={{ opacity: 0, y: 10 }}
                                            animate={{ opacity: 1, y: 0 }}
                                            exit={{ opacity: 0, y: -10 }}
                                            transition={{ duration: 0.3 }}
                                            className="bg-white rounded-lg shadow p-4 flex items-center justify-between"
                                        >
                                            <div>
                                                <h3 className="text-lg font-semibold text-gray-800">{flight.flightNumber}</h3>
                                                <p className="text-gray-600">
                                                    {flight.departureAirport} → {flight.arrivalAirport}
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
                        </>
                    ) : (
                        data && <p className="text-gray-600">No flights found. Try another search.</p>
                    )}
                </div>
            </div>
        </div>
    );
}
