import React from "react";
import ReactDOM from "react-dom/client";
import { createBrowserRouter, RouterProvider } from "react-router-dom";
import App from "./App";
import HomePage from "./pages/HomePage";
import OrganizerPage from "./pages/OrganizerPage";
import PlayerPage from "./pages/PlayerPage";
import LeaderboardPage from "./pages/LeaderboardPage";
import "./styles.css";

const router = createBrowserRouter([
  {
    path: "/",
    element: <App />,
    children: [
      { index: true, element: <HomePage /> },
      { path: "organizer", element: <OrganizerPage /> },
      { path: "play", element: <PlayerPage /> },
      { path: "tournaments/:tournamentId/leaderboard", element: <LeaderboardPage /> },
    ],
  },
]);

ReactDOM.createRoot(document.getElementById("root")!).render(
  <React.StrictMode>
    <RouterProvider router={router} />
  </React.StrictMode>,
);
