import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import { applyTheme } from "../theme";
import { useEventStream } from "../useEventStream";
import type {
  FullSchedule,
  GroupView,
  PlayerView,
  SchedulerView,
  SeatingView,
  TournamentView,
} from "../types";

export default function OrganizerPage() {
  const [groups, setGroups] = useState<GroupView[]>([]);
  const [group, setGroup] = useState<GroupView | null>(null);
  const [players, setPlayers] = useState<PlayerView[]>([]);
  const [tournaments, setTournaments] = useState<TournamentView[]>([]);
  const [tournament, setTournament] = useState<TournamentView | null>(null);
  const [schedule, setSchedule] = useState<FullSchedule | null>(null);
  const [schedulers, setSchedulers] = useState<SchedulerView[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.listGroups().then(setGroups).catch((e) => setError(e.message));
    api.listSchedulers().then(setSchedulers).catch(() => {});
  }, []);

  const selectGroup = useCallback(async (g: GroupView) => {
    setGroup(g);
    applyTheme(g.theme);
    setTournament(null);
    setSchedule(null);
    try {
      setPlayers(await api.listPlayers(g.id));
      setTournaments(await api.listTournaments(g.id));
    } catch (e) {
      setError((e as Error).message);
    }
  }, []);

  const refreshSchedule = useCallback(() => {
    if (tournament) api.getSchedule(tournament.id).then(setSchedule).catch(() => {});
  }, [tournament]);

  useEventStream(tournament?.id ?? null, { "score-updated": refreshSchedule });

  const openTournament = async (t: TournamentView) => {
    setTournament(t);
    setSchedule(null);
    if (t.status !== "DRAFT") {
      try {
        setSchedule(await api.getSchedule(t.id));
      } catch (e) {
        setError((e as Error).message);
      }
    }
  };

  return (
    <div>
      {error && <p className="error">{error}</p>}

      <GroupPanel
        groups={groups}
        selected={group}
        onSelect={selectGroup}
        onCreated={(g) => {
          setGroups((prev) => [...prev, g]);
          selectGroup(g);
        }}
        onError={setError}
      />

      {group && (
        <RosterPanel
          group={group}
          players={players}
          onChange={setPlayers}
          onError={setError}
        />
      )}

      {group && (
        <TournamentPanel
          group={group}
          schedulers={schedulers}
          tournaments={tournaments}
          selected={tournament}
          onCreated={(t) => setTournaments((prev) => [t, ...prev])}
          onOpen={openTournament}
          onError={setError}
        />
      )}

      {tournament && (
        <SchedulePanel
          tournament={tournament}
          players={players}
          schedule={schedule}
          onGenerated={(s, updated) => {
            setSchedule(s);
            setTournament(updated);
            setTournaments((prev) => prev.map((t) => (t.id === updated.id ? updated : t)));
          }}
          onScored={refreshSchedule}
          onError={setError}
        />
      )}
    </div>
  );
}

function GroupPanel({
  groups,
  selected,
  onSelect,
  onCreated,
  onError,
}: {
  groups: GroupView[];
  selected: GroupView | null;
  onSelect: (g: GroupView) => void;
  onCreated: (g: GroupView) => void;
  onError: (msg: string) => void;
}) {
  const [slug, setSlug] = useState("");
  const [name, setName] = useState("");
  const [theme, setTheme] = useState("");

  const create = async () => {
    try {
      const g = await api.createGroup(slug.trim(), name.trim(), theme.trim() || undefined);
      setSlug("");
      setName("");
      setTheme("");
      onCreated(g);
    } catch (e) {
      onError((e as Error).message);
    }
  };

  return (
    <div className="card">
      <h2>Group</h2>
      <div className="row">
        <div className="field">
          <label>Existing group</label>
          <select
            value={selected?.id ?? ""}
            onChange={(e) => {
              const g = groups.find((x) => x.id === Number(e.target.value));
              if (g) onSelect(g);
            }}
            style={{ width: "100%" }}
          >
            <option value="" disabled>
              Select…
            </option>
            {groups.map((g) => (
              <option key={g.id} value={g.id}>
                {g.name}
              </option>
            ))}
          </select>
        </div>
      </div>
      <details>
        <summary className="muted">Create a new group</summary>
        <div className="row" style={{ marginTop: "0.75rem" }}>
          <div className="field">
            <label>Slug (url id)</label>
            <input value={slug} onChange={(e) => setSlug(e.target.value)} placeholder="friday-euchre" />
          </div>
          <div className="field">
            <label>Name</label>
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Friday Euchre Club" />
          </div>
        </div>
        <div className="field">
          <label>Theme JSON (optional) — e.g. {"{"}"primary":"#7b2d26"{"}"}</label>
          <input value={theme} onChange={(e) => setTheme(e.target.value)} style={{ width: "100%" }} />
        </div>
        <button onClick={create} disabled={!slug.trim() || !name.trim()}>
          Create group
        </button>
      </details>
    </div>
  );
}

function RosterPanel({
  group,
  players,
  onChange,
  onError,
}: {
  group: GroupView;
  players: PlayerView[];
  onChange: (players: PlayerView[]) => void;
  onError: (msg: string) => void;
}) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [csv, setCsv] = useState("");

  const add = async () => {
    try {
      const p = await api.addPlayer(group.id, name.trim(), email.trim() || undefined);
      onChange([...players, p]);
      setName("");
      setEmail("");
    } catch (e) {
      onError((e as Error).message);
    }
  };

  const importCsv = async () => {
    try {
      const created = await api.importPlayers(group.id, csv);
      onChange([...players, ...created]);
      setCsv("");
    } catch (e) {
      onError((e as Error).message);
    }
  };

  return (
    <div className="card">
      <h2>Roster ({players.length})</h2>
      <div className="row">
        <div className="field">
          <label>Name</label>
          <input value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div className="field">
          <label>Email (optional)</label>
          <input value={email} onChange={(e) => setEmail(e.target.value)} />
        </div>
        <div className="field" style={{ flex: "0 0 auto" }}>
          <button onClick={add} disabled={!name.trim()}>
            Add player
          </button>
        </div>
      </div>

      <details>
        <summary className="muted">Bulk import (one player per line: "Name, email")</summary>
        <textarea
          value={csv}
          onChange={(e) => setCsv(e.target.value)}
          rows={5}
          style={{ width: "100%", marginTop: "0.5rem" }}
          placeholder={"Alice Smith, alice@example.com\nBob Jones"}
        />
        <button className="secondary" onClick={importCsv} disabled={!csv.trim()}>
          Import
        </button>
      </details>

      {players.length > 0 && (
        <table style={{ marginTop: "0.75rem" }}>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Join code</th>
            </tr>
          </thead>
          <tbody>
            {players.map((p) => (
              <tr key={p.id}>
                <td>{p.name}</td>
                <td className="muted">{p.email ?? "—"}</td>
                <td>
                  <code className="pill">{p.joinCode}</code>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}

function TournamentPanel({
  group,
  schedulers,
  tournaments,
  selected,
  onCreated,
  onOpen,
  onError,
}: {
  group: GroupView;
  schedulers: SchedulerView[];
  tournaments: TournamentView[];
  selected: TournamentView | null;
  onCreated: (t: TournamentView) => void;
  onOpen: (t: TournamentView) => void;
  onError: (msg: string) => void;
}) {
  const [name, setName] = useState("");
  const [schedulerId, setSchedulerId] = useState("standard-card");

  const create = async () => {
    try {
      const t = await api.createTournament(group.id, name.trim(), schedulerId);
      setName("");
      onCreated(t);
      onOpen(t);
    } catch (e) {
      onError((e as Error).message);
    }
  };

  return (
    <div className="card">
      <h2>Tournaments</h2>
      <div className="row">
        <div className="field">
          <label>New tournament name</label>
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="Fall Classic" />
        </div>
        <div className="field">
          <label>Scheduling algorithm</label>
          <select value={schedulerId} onChange={(e) => setSchedulerId(e.target.value)} style={{ width: "100%" }}>
            {schedulers.map((s) => (
              <option key={s.id} value={s.id}>
                {s.displayName}
              </option>
            ))}
          </select>
        </div>
        <div className="field" style={{ flex: "0 0 auto" }}>
          <button onClick={create} disabled={!name.trim()}>
            Create
          </button>
        </div>
      </div>

      {tournaments.map((t) => (
        <div key={t.id} className="row" style={{ marginBottom: "0.4rem" }}>
          <strong>{t.name}</strong>
          <span className="pill">{t.status}</span>
          {t.numRounds != null && <span className="muted">{t.numRounds} rounds</span>}
          <button
            className="secondary"
            onClick={() => onOpen(t)}
            style={{ marginLeft: "auto" }}
            disabled={selected?.id === t.id}
          >
            {selected?.id === t.id ? "Open" : "Manage"}
          </button>
        </div>
      ))}
    </div>
  );
}

function SchedulePanel({
  tournament,
  players,
  schedule,
  onGenerated,
  onScored,
  onError,
}: {
  tournament: TournamentView;
  players: PlayerView[];
  schedule: FullSchedule | null;
  onGenerated: (s: FullSchedule, updated: TournamentView) => void;
  onScored: () => void;
  onError: (msg: string) => void;
}) {
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [rounds, setRounds] = useState<string>("");
  const [generating, setGenerating] = useState(false);

  const toggle = (id: number) =>
    setSelectedIds((prev) => (prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]));

  const generate = async () => {
    setGenerating(true);
    try {
      const s = await api.generateSchedule(
        tournament.id,
        selectedIds,
        rounds ? Number(rounds) : undefined,
      );
      const updated = await api.getTournament(tournament.id);
      onGenerated(s, updated);
    } catch (e) {
      onError((e as Error).message);
    } finally {
      setGenerating(false);
    }
  };

  return (
    <div className="card">
      <div className="row" style={{ justifyContent: "space-between" }}>
        <h2>{tournament.name} — schedule</h2>
        <Link to={`/tournaments/${tournament.id}/leaderboard`}>
          <button className="secondary">View leaderboard</button>
        </Link>
      </div>

      <details open={!schedule}>
        <summary className="muted">
          Select players &amp; generate ({selectedIds.length} selected)
        </summary>
        <div className="grid-cards" style={{ marginTop: "0.75rem" }}>
          {players.map((p) => (
            <label key={p.id} className="pill" style={{ cursor: "pointer" }}>
              <input
                type="checkbox"
                checked={selectedIds.includes(p.id)}
                onChange={() => toggle(p.id)}
                style={{ marginRight: "0.4rem" }}
              />
              {p.name}
            </label>
          ))}
        </div>
        <div className="row" style={{ marginTop: "0.75rem" }}>
          <div className="field" style={{ flex: "0 0 auto" }}>
            <label>Rounds (optional)</label>
            <input
              type="number"
              min={1}
              value={rounds}
              onChange={(e) => setRounds(e.target.value)}
              style={{ width: "120px" }}
            />
          </div>
          <div className="field" style={{ flex: "0 0 auto" }}>
            <button onClick={generate} disabled={generating || selectedIds.length < 4}>
              {schedule ? "Regenerate" : "Generate schedule"}
            </button>
          </div>
        </div>
        <p className="muted">
          Uses the "{tournament.schedulerId}" algorithm; falls back to the generated rotation if that
          algorithm doesn't cover the player count.
        </p>
      </details>

      {schedule?.rounds.map((round) => (
        <div key={round.roundId} style={{ marginTop: "1rem" }}>
          <h3>Round {round.roundNumber}</h3>
          {round.tables.map((table) => (
            <TableRow key={table.seatingId} table={table} onScored={onScored} onError={onError} />
          ))}
        </div>
      ))}
    </div>
  );
}

function TableRow({
  table,
  onScored,
  onError,
}: {
  table: SeatingView;
  onScored: () => void;
  onError: (msg: string) => void;
}) {
  const [t1, setT1] = useState(table.team1Score?.toString() ?? "");
  const [t2, setT2] = useState(table.team2Score?.toString() ?? "");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setT1(table.team1Score?.toString() ?? "");
    setT2(table.team2Score?.toString() ?? "");
  }, [table.team1Score, table.team2Score]);

  const save = async () => {
    setSaving(true);
    try {
      await api.submitScore(table.seatingId, Number(t1), Number(t2));
      onScored();
    } catch (e) {
      onError((e as Error).message);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="row" style={{ alignItems: "center", borderBottom: "1px solid #eee", padding: "0.4rem 0" }}>
      <span className="pill">T{table.tableNumber}</span>
      <span style={{ flex: 1, minWidth: 140 }}>
        {table.team1.map((p) => p.name).join(" & ")}
      </span>
      <input type="number" min={0} value={t1} onChange={(e) => setT1(e.target.value)} style={{ width: 64 }} />
      <span className="muted">vs</span>
      <input type="number" min={0} value={t2} onChange={(e) => setT2(e.target.value)} style={{ width: 64 }} />
      <span style={{ flex: 1, minWidth: 140 }}>
        {table.team2.map((p) => p.name).join(" & ")}
      </span>
      <button onClick={save} disabled={saving || t1 === "" || t2 === ""}>
        Save
      </button>
      {table.scored && <span className="pill">✓</span>}
    </div>
  );
}
