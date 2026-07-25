import { Link } from "react-router-dom";

export default function HomePage() {
  return (
    <div>
      <div className="card">
        <h2>Welcome to EuchreFlow</h2>
        <p className="muted">
          A multi-tenant platform for running progressive euchre tournaments — live seating
          cards, instant score sync between partners, and a real-time leaderboard.
        </p>
      </div>
      <div className="grid-cards">
        <div className="card">
          <h2>I'm running the event</h2>
          <p className="muted">
            Create your group, add the roster, generate the schedule, and track scores.
          </p>
          <Link to="/organizer">
            <button>Open organizer console</button>
          </Link>
        </div>
        <div className="card">
          <h2>I'm a player</h2>
          <p className="muted">Enter your join code to see who you're playing and enter scores.</p>
          <Link to="/play">
            <button>Join with a code</button>
          </Link>
        </div>
      </div>
    </div>
  );
}
