import { Link } from "react-router-dom";

function Home() {
    return (
        <div className="flex flex-col items-center justify-center min-h-screen bg-gray-100 text-center px-6">
            <div className="max-w-2xl">
                <h1 className="text-4xl font-bold text-gray-900 mb-4">
                    Welcome to World Plane, Inc. (WPI) ✈️
                </h1>
                <p className="text-gray-700 mb-6">Your trusted flight booking service.</p>
                <Link to="/search" className="bg-blue-600 text-white px-6 py-3 rounded-md hover:bg-blue-700 transition">
                    Search for Flights
                </Link>
            </div>
        </div>
    );
}

export default Home;
