import type {
  FullSchedule,
  GroupView,
  JoinResult,
  Leaderboard,
  PlayerSchedule,
  PlayerView,
  SchedulerView,
  TournamentView,
} from "./types";

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const res = await fetch(`/api${path}`, {
    headers: { "Content-Type": "application/json" },
    ...options,
  });
  if (!res.ok) {
    let message = `Request failed (${res.status})`;
    try {
      const body = await res.json();
      if (body?.error) message = body.error;
    } catch {
      /* non-JSON error body */
    }
    throw new Error(message);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export const api = {
  // groups
  listGroups: () => request<GroupView[]>("/groups"),
  createGroup: (slug: string, name: string, theme?: string) =>
    request<GroupView>("/groups", {
      method: "POST",
      body: JSON.stringify({ slug, name, theme }),
    }),
  getGroup: (id: number) => request<GroupView>(`/groups/${id}`),
  getGroupBySlug: (slug: string) => request<GroupView>(`/groups/slug/${slug}`),

  // roster
  listPlayers: (groupId: number) => request<PlayerView[]>(`/groups/${groupId}/players`),
  addPlayer: (groupId: number, name: string, email?: string) =>
    request<PlayerView>(`/groups/${groupId}/players`, {
      method: "POST",
      body: JSON.stringify({ name, email }),
    }),
  importPlayers: (groupId: number, csv: string) =>
    request<PlayerView[]>(`/groups/${groupId}/players/import`, {
      method: "POST",
      body: JSON.stringify({ csv }),
    }),

  // tournaments
  listTournaments: (groupId: number) =>
    request<TournamentView[]>(`/groups/${groupId}/tournaments`),
  createTournament: (groupId: number, name: string, schedulerId: string) =>
    request<TournamentView>(`/groups/${groupId}/tournaments`, {
      method: "POST",
      body: JSON.stringify({ name, schedulerId }),
    }),
  getTournament: (id: number) => request<TournamentView>(`/tournaments/${id}`),
  generateSchedule: (id: number, playerIds: number[], requestedRounds?: number) =>
    request<FullSchedule>(`/tournaments/${id}/schedule`, {
      method: "POST",
      body: JSON.stringify({ playerIds, requestedRounds }),
    }),
  getSchedule: (id: number) => request<FullSchedule>(`/tournaments/${id}/schedule`),
  getPlayerSchedule: (id: number, playerId: number) =>
    request<PlayerSchedule>(`/tournaments/${id}/players/${playerId}/schedule`),
  getLeaderboard: (id: number) => request<Leaderboard>(`/tournaments/${id}/leaderboard`),

  // scoring
  submitScore: (
    seatingId: number,
    team1Score: number,
    team2Score: number,
    submittedByPlayerId?: number,
  ) =>
    request<{ seatingId: number; team1Score: number; team2Score: number }>(
      `/seatings/${seatingId}/score`,
      {
        method: "POST",
        body: JSON.stringify({ team1Score, team2Score, submittedByPlayerId }),
      },
    ),

  // meta
  listSchedulers: () => request<SchedulerView[]>("/schedulers"),
  join: (joinCode: string) => request<JoinResult>(`/join/${joinCode}`),
};

/** Base URL for the SSE stream of a tournament. */
export const eventStreamUrl = (tournamentId: number) =>
  `/api/tournaments/${tournamentId}/events`;
