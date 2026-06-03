import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./rsvp/AuthContext";
import { Nav } from "./rsvp/Nav";
import { RequireAuth } from "./rsvp/RequireAuth";
import { LoginPage } from "./rsvp/pages/LoginPage";
import { SignupPage } from "./rsvp/pages/SignupPage";
import { EventsListPage } from "./rsvp/pages/EventsListPage";
import { EventCreatePage } from "./rsvp/pages/EventCreatePage";
import { EventDashboardPage } from "./rsvp/pages/EventDashboardPage";
import { InviteePage } from "./rsvp/pages/InviteePage";
import "./rsvp/styles.css";

export default function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <div className="app-shell">
          <Nav />
          <Routes>
            <Route path="/" element={<Navigate to="/host/events" replace />} />
            <Route path="/login" element={<LoginPage />} />
            <Route path="/signup" element={<SignupPage />} />
            <Route path="/invite/:token" element={<InviteePage />} />
            <Route
              path="/host/events"
              element={
                <RequireAuth>
                  <EventsListPage />
                </RequireAuth>
              }
            />
            <Route
              path="/host/events/new"
              element={
                <RequireAuth>
                  <EventCreatePage />
                </RequireAuth>
              }
            />
            <Route
              path="/host/events/:id"
              element={
                <RequireAuth>
                  <EventDashboardPage />
                </RequireAuth>
              }
            />
            <Route path="*" element={<Navigate to="/host/events" replace />} />
          </Routes>
        </div>
      </AuthProvider>
    </BrowserRouter>
  );
}
