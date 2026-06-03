import { useEffect, useState, type FormEvent } from "react";
import { useNavigate, useParams } from "react-router-dom";
import { ApiError, api } from "../api";
import type { CreatedInvitation, Dashboard, EventResponse, RsvpStatus } from "../types";

function statusBadge(s: RsvpStatus | null) {
  if (s === "YES_CONFIRMED") return <span className="badge green">CONFIRMED</span>;
  if (s === "YES_WAITLISTED") return <span className="badge amber">WAITLISTED</span>;
  if (s === "NO") return <span className="badge red">NO</span>;
  if (s === "MAYBE") return <span className="badge amber">MAYBE</span>;
  return <span className="badge gray">PENDING</span>;
}

export function EventDashboardPage() {
  const { id } = useParams();
  const nav = useNavigate();
  const eventId = Number(id);

  const [event, setEvent] = useState<EventResponse | null>(null);
  const [dash, setDash] = useState<Dashboard | null>(null);
  const [error, setError] = useState("");
  const [inviteEmail, setInviteEmail] = useState("");
  const [lastInvite, setLastInvite] = useState<CreatedInvitation | null>(null);
  const [inviting, setInviting] = useState(false);

  const reload = async () => {
    setError("");
    try {
      const [e, d] = await Promise.all([api.getEvent(eventId), api.dashboard(eventId)]);
      setEvent(e);
      setDash(d);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to load");
    }
  };

  useEffect(() => {
    reload();
  }, [eventId]);

  const onInvite = async (ev: FormEvent) => {
    ev.preventDefault();
    setInviting(true);
    setError("");
    try {
      const created = await api.invite(eventId, inviteEmail);
      setLastInvite(created);
      setInviteEmail("");
      await reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to invite");
    } finally {
      setInviting(false);
    }
  };

  const onClose = async () => {
    if (!confirm("Close event to new invitees? Existing invitees can still change their RSVP until start.")) return;
    try {
      await api.closeEvent(eventId);
      await reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed");
    }
  };

  const onCancel = async () => {
    if (!confirm("Cancel this event? This is permanent.")) return;
    try {
      await api.cancelEvent(eventId);
      await reload();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed");
    }
  };

  if (!event || !dash) {
    return (
      <div className="card">
        {error ? <div className="error">{error}</div> : <p className="muted">Loading…</p>}
        <button className="secondary" onClick={() => nav("/host/events")}>Back</button>
      </div>
    );
  }

  const newInvitesAllowed = event.state === "OPEN" && !event.locked;

  return (
    <>
      <div className="card">
        <div className="flex-between">
          <div>
            <h2 style={{ marginBottom: 4 }}>{event.title}</h2>
            <div className="muted" style={{ color: "var(--muted)", fontSize: 13 }}>
              {new Date(event.startTime).toLocaleString()} · {event.location}
            </div>
          </div>
          <div className="gap-8">
            {event.state === "OPEN" && !event.locked && (
              <button className="secondary" onClick={onClose}>Close</button>
            )}
            {event.state !== "CANCELLED" && (
              <button className="danger" onClick={onCancel}>Cancel event</button>
            )}
          </div>
        </div>
        {event.description && <p style={{ marginTop: 12 }}>{event.description}</p>}
        <div style={{ marginTop: 8, fontSize: 13, color: "var(--muted)" }}>
          State: {event.state} {event.locked && " · LOCKED (event started)"}
        </div>
      </div>

      <div className="card">
        <h3>Counts</h3>
        <div className="counts">
          <div className="count-box"><div className="n">{dash.counts.confirmed}</div><div className="l">Confirmed</div></div>
          <div className="count-box"><div className="n">{dash.counts.waitlisted}</div><div className="l">Waitlisted</div></div>
          <div className="count-box"><div className="n">{dash.counts.maybe}</div><div className="l">Maybe</div></div>
          <div className="count-box"><div className="n">{dash.counts.no}</div><div className="l">No</div></div>
          <div className="count-box"><div className="n">{dash.counts.pending}</div><div className="l">Pending</div></div>
        </div>
        <div className="muted" style={{ color: "var(--muted)", fontSize: 13 }}>
          Capacity: {dash.counts.capacity ?? "unlimited"}
        </div>
      </div>

      {newInvitesAllowed && (
        <div className="card">
          <h3>Invite someone</h3>
          <form onSubmit={onInvite} className="gap-8" style={{ alignItems: "flex-end" }}>
            <div style={{ flex: 1 }}>
              <label>Email</label>
              <input
                type="email"
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
                placeholder="name@example.com"
                required
              />
            </div>
            <button type="submit" disabled={inviting}>Invite</button>
          </form>
          {lastInvite && (
            <div style={{ marginTop: 16 }}>
              <div className="notice">Invite created for {lastInvite.email}. Share this link:</div>
              <div className="link-box">{lastInvite.link}</div>
              <button
                className="secondary"
                style={{ marginTop: 8 }}
                onClick={() => navigator.clipboard.writeText(lastInvite.link)}
              >
                Copy link
              </button>
            </div>
          )}
        </div>
      )}

      <div className="card">
        <h3>Attendees</h3>
        {error && <div className="error">{error}</div>}
        {dash.attendees.length === 0 ? (
          <p className="muted">No invitees yet.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Email</th>
                <th>Status</th>
                <th>Last update</th>
              </tr>
            </thead>
            <tbody>
              {dash.attendees.map((a) => (
                <tr key={a.invitationId}>
                  <td>{a.email}</td>
                  <td>{statusBadge(a.status)}</td>
                  <td>{a.updatedAt ? new Date(a.updatedAt).toLocaleString() : "—"}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}
