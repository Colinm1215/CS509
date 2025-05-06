import { Link } from "react-router-dom";

function Navbar() {
    return (
        <nav className="w-full bg-blue-600 text-gray-200 py-4 shadow-md">
            <div className="max-w-6xl mx-auto flex justify-between items-center px-6">
                <span className="text-xl font-bold">World Plane, Inc.</span>
                <div className="text-grey-200 flex space-x-6">
                    <Link to="/"
                          className="text-gray-950 underline decoration-sky-400 underline-offset-3 hover:decoration-2 dark:text-white">Home</Link>
                    <Link to="/search"
                          className="text-gray-950 underline decoration-sky-400 underline-offset-3 hover:decoration-2 dark:text-white">Flight
                        Search</Link>
                </div>
            </div>
        </nav>
    );
}

export default Navbar;
