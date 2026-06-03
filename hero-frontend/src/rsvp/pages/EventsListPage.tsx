import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api";
import type { EventResponse } from "../types";

function stateBadge(e: EventResponse) {
  if (e.state === "CANCELLED") return <span className="badge red">CANCELLED</span>;
  if (e.locked) return <span className="badge gray">LOCKED</span>;
  if (e.state === "CLOSED") return <span className="badge amber">CLOSED</span>;
  return <span className="badge green">OPEN</span>;
}

export function EventsListPage() {
  const [events, setEvents] = useState<EventResponse[] | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    api.listEvents().then(setEvents).catch((e) => setError(e.message));
  }, []);

  return (
    <div className="card">
      <div className="flex-between">
        <h2>Your events</h2>
        <Link to="/host/events/new"><button>+ New event</button></Link>
      </div>
      {error && <div className="error">{error}</div>}
      {events === null && !error && <p className="muted">Loading…</p>}
      {events && events.length === 0 && <p className="muted">No events yet. Create one to get started.</p>}
      {events && events.length > 0 && (
        <table>
          <thead>
            <tr>
              <th>Title</th>
              <th>Starts</th>
              <th>Capacity</th>
              <th>State</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            {events.map((e) => (
              <tr key={e.id}>
                <td>{e.title}</td>
                <td>{new Date(e.startTime).toLocaleString()}</td>
                <td>{e.capacity ?? "—"}</td>
                <td>{stateBadge(e)}</td>
                <td><Link to={`/host/events/${e.id}`}>Open</Link></td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
