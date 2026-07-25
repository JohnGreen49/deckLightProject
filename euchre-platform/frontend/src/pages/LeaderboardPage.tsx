import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { api } from "../api";
import { useEventStream } from "../useEventStream";
import type { Leaderboard } from "../types";

export default function LeaderboardPage() {
  const { tournamentId } = useParams();
  const id = tournamentId ? Number(tournamentId) : null;
  const [board, setBoard] = useState<Leaderboard | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [flash, setFlash] = useState(false);

  useEffect(() => {
    if (id == null) return;
    api.getLeaderboard(id).then(setBoard).catch((e) => setError(e.message));
  }, [id]);

  useEventStream(id, {
    "leaderboard-updated": (data) => {
      setBoard(data as Leaderboard);
      setFlash(true);
      setTimeout(() => setFlash(false), 1000);
    },
  });

  if (id == null) return <p>Missing tournament id.</p>;
  if (error) return <p className="error">{error}</p>;
  if (!board) return <p className="muted">Loading leaderboard…</p>;

  return (
    <div className={`card ${flash ? "score-flash" : ""}`}>
      <h2>
        Leaderboard <span className="badge">live</span>
      </h2>
      {board.entries.length === 0 ? (
        <p className="muted">No scores recorded yet.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>#</th>
              <th>Player</th>
              <th>Points</th>
              <th>Played</th>
              <th>Won</th>
            </tr>
          </thead>
          <tbody>
            {board.entries.map((e) => (
              <tr key={e.playerId}>
                <td>{e.rank}</td>
                <td>{e.name}</td>
                <td>
                  <strong>{e.totalPoints}</strong>
                </td>
                <td>{e.gamesPlayed}</td>
                <td>{e.gamesWon}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
