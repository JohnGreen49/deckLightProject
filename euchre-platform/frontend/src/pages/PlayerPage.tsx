import { useCallback, useEffect, useState } from "react";
import { api } from "../api";
import { applyTheme } from "../theme";
import { useEventStream } from "../useEventStream";
import type { JoinResult, PlayerRoundCard, PlayerSchedule, TournamentView } from "../types";

export default function PlayerPage() {
  const [code, setCode] = useState("");
  const [join, setJoin] = useState<JoinResult | null>(null);
  const [tournament, setTournament] = useState<TournamentView | null>(null);
  const [schedule, setSchedule] = useState<PlayerSchedule | null>(null);
  const [error, setError] = useState<string | null>(null);

  const submitCode = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    try {
      const result = await api.join(code.trim().toUpperCase());
      setJoin(result);
      const group = await api.getGroup(result.groupId);
      applyTheme(group.theme);
      const active = result.tournaments.find((t) => t.status === "ACTIVE");
      if (active) selectTournament(active, result.player.id);
    } catch (err) {
      setError((err as Error).message);
    }
  };

  const selectTournament = useCallback(async (t: TournamentView, playerId: number) => {
    setTournament(t);
    try {
      setSchedule(await api.getPlayerSchedule(t.id, playerId));
    } catch (err) {
      setError((err as Error).message);
    }
  }, []);

  const refresh = useCallback(() => {
    if (tournament && join) {
      api.getPlayerSchedule(tournament.id, join.player.id).then(setSchedule).catch(() => {});
    }
  }, [tournament, join]);

  useEventStream(tournament?.id ?? null, {
    "score-updated": refresh,
  });

  if (!join) {
    return (
      <div className="card center-narrow">
        <h2>Enter your join code</h2>
        <form onSubmit={submitCode}>
          <div className="field">
            <input
              value={code}
              onChange={(e) => setCode(e.target.value)}
              placeholder="e.g. K7M4PQ"
              autoFocus
              style={{ width: "100%", textTransform: "uppercase", letterSpacing: "0.2em" }}
            />
          </div>
          <button type="submit" disabled={!code.trim()}>
            Continue
          </button>
          {error && <p className="error">{error}</p>}
        </form>
      </div>
    );
  }

  return (
    <div>
      <div className="card">
        <h2>Hi {join.player.name} 👋</h2>
        {!tournament && (
          <>
            <p className="muted">Pick your tournament:</p>
            {join.tournaments.length === 0 && <p className="muted">No tournaments yet.</p>}
            {join.tournaments.map((t) => (
              <div key={t.id} className="row" style={{ marginBottom: "0.5rem" }}>
                <span>{t.name}</span>
                <span className="pill">{t.status}</span>
                <button className="secondary" onClick={() => selectTournament(t, join.player.id)}>
                  Open
                </button>
              </div>
            ))}
          </>
        )}
        {tournament && <p className="muted">{tournament.name}</p>}
      </div>

      {schedule && (
        <div className="grid-cards">
          {schedule.rounds.map((card) => (
            <PlayerCard
              key={card.roundNumber}
              card={card}
              playerId={join.player.id}
              onScored={refresh}
              onError={setError}
            />
          ))}
        </div>
      )}
      {schedule && schedule.rounds.length === 0 && (
        <p className="muted">Your schedule isn't ready yet — check back once the organizer starts.</p>
      )}
      {error && <p className="error">{error}</p>}
    </div>
  );
}

function PlayerCard({
  card,
  playerId,
  onScored,
  onError,
}: {
  card: PlayerRoundCard;
  playerId: number;
  onScored: () => void;
  onError: (msg: string) => void;
}) {
  const [yourScore, setYourScore] = useState<string>(card.yourScore?.toString() ?? "");
  const [oppScore, setOppScore] = useState<string>(card.opponentScore?.toString() ?? "");
  const [saving, setSaving] = useState(false);

  // Keep inputs in sync when a live update arrives (e.g. partner entered the score).
  useEffect(() => {
    setYourScore(card.yourScore?.toString() ?? "");
    setOppScore(card.opponentScore?.toString() ?? "");
  }, [card.yourScore, card.opponentScore]);

  const save = async () => {
    setSaving(true);
    try {
      const yours = Number(yourScore);
      const opp = Number(oppScore);
      // Map "your"/"opponent" back to team1/team2 for the API.
      const team1 = card.onTeam1 ? yours : opp;
      const team2 = card.onTeam1 ? opp : yours;
      await api.submitScore(card.seatingId, team1, team2, playerId);
      onScored();
    } catch (err) {
      onError((err as Error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="round-card">
      <div className="row" style={{ justifyContent: "space-between" }}>
        <strong>Round {card.roundNumber}</strong>
        <span className="pill">Table {card.tableNumber}</span>
      </div>
      <p style={{ margin: "0.5rem 0" }}>
        Partner: <strong>{card.partner.name}</strong>
      </p>
      <p className="muted" style={{ margin: "0.25rem 0 0.75rem" }}>
        vs {card.opponents.map((o) => o.name).join(" & ")}
      </p>
      <div className="row">
        <div className="field" style={{ minWidth: 0 }}>
          <label>Your team</label>
          <input
            type="number"
            min={0}
            value={yourScore}
            onChange={(e) => setYourScore(e.target.value)}
            style={{ width: "100%" }}
          />
        </div>
        <div className="field" style={{ minWidth: 0 }}>
          <label>Opponents</label>
          <input
            type="number"
            min={0}
            value={oppScore}
            onChange={(e) => setOppScore(e.target.value)}
            style={{ width: "100%" }}
          />
        </div>
      </div>
      <button onClick={save} disabled={saving || yourScore === "" || oppScore === ""}>
        {card.scored ? "Update score" : "Submit score"}
      </button>
      {card.scored && <span className="pill" style={{ marginLeft: "0.5rem" }}>recorded</span>}
    </div>
  );
}
