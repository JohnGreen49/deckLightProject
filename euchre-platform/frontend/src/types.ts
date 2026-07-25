// Mirrors the backend DTOs (com.euchreflow.platform.web.dto).

export interface GroupView {
  id: number;
  slug: string;
  name: string;
  theme: string; // JSON string
}

export interface PlayerView {
  id: number;
  name: string;
  email: string | null;
  joinCode: string;
}

export interface TournamentView {
  id: number;
  name: string;
  status: string;
  schedulerId: string;
  numRounds: number | null;
}

export interface SchedulerView {
  id: string;
  displayName: string;
}

export interface PlayerRef {
  id: number;
  name: string;
}

export interface SeatingView {
  seatingId: number;
  tableNumber: number;
  team1: PlayerRef[];
  team2: PlayerRef[];
  team1Score: number | null;
  team2Score: number | null;
  scored: boolean;
}

export interface RoundView {
  roundId: number;
  roundNumber: number;
  status: string;
  tables: SeatingView[];
}

export interface FullSchedule {
  tournamentId: number;
  name: string;
  rounds: RoundView[];
}

export interface PlayerRoundCard {
  roundNumber: number;
  seatingId: number;
  tableNumber: number;
  partner: PlayerRef;
  opponents: PlayerRef[];
  onTeam1: boolean;
  yourScore: number | null;
  opponentScore: number | null;
  scored: boolean;
}

export interface PlayerSchedule {
  tournamentId: number;
  tournamentName: string;
  you: PlayerRef;
  rounds: PlayerRoundCard[];
}

export interface LeaderboardEntry {
  rank: number;
  playerId: number;
  name: string;
  totalPoints: number;
  gamesPlayed: number;
  gamesWon: number;
}

export interface Leaderboard {
  tournamentId: number;
  entries: LeaderboardEntry[];
}

export interface JoinResult {
  player: PlayerView;
  groupId: number;
  tournaments: TournamentView[];
}

export interface GroupTheme {
  primary?: string;
  accent?: string;
  background?: string;
  surface?: string;
  text?: string;
  logoUrl?: string;
}
