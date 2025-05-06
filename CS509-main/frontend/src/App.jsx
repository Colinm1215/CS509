import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Home from './pages/Home';
import FlightSearch from './pages/FlightSearch';
import Navbar from './components/Navbar';


function App() {
    return (
        <Router>
            <Navbar />
            <div className="flex flex-col items-center justify-center w-full">
                <Routes>
                    <Route path="/" element={<Home />} />
                    <Route path="/search" element={<FlightSearch />} />
                </Routes>
            </div>
        </Router>
    );
}

export default App;