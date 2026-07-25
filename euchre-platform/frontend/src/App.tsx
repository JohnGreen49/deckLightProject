import { Link, Outlet } from "react-router-dom";

export default function App() {
  return (
    <>
      <header className="app-header">
        <h1>♣ EuchreFlow</h1>
        <nav>
          <Link to="/">Home</Link>
          <Link to="/organizer">Organizer</Link>
          <Link to="/play">Play</Link>
        </nav>
      </header>
      <main className="container">
        <Outlet />
      </main>
    </>
  );
}
