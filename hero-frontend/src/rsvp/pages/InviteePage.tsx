import { useEffect, useState } from "react";
import { useParams } from "react-router-dom";
import { ApiError, api } from "../api";
import type { Choice, InviteeView, RsvpStatus } from "../types";

function statusLine(s: RsvpStatus | null) {
  if (s === "YES_CONFIRMED") return <span className="badge green">You're CONFIRMED</span>;
  if (s === "YES_WAITLISTED") return <span className="badge amber">You're on the WAITLIST</span>;
  if (s === "NO") return <span className="badge red">You said NO</span>;
  if (s === "MAYBE") return <span className="badge amber">You said MAYBE</span>;
  return <span className="badge gray">You haven't responded yet</span>;
}

export function InviteePage() {
  const { token } = useParams();
  const [view, setView] = useState<InviteeView | null>(null);
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    if (!token) return;
    api.inviteeView(token).then(setView).catch((e) => setError(e.message));
  }, [token]);

  const choose = async (choice: Choice) => {
    if (!token) return;
    setSubmitting(true);
    setError("");
    try {
      setView(await api.submitRsvp(token, choice));
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed");
    } finally {
      setSubmitting(false);
    }
  };

  if (error) return <div className="card"><div className="error">{error}</div></div>;
  if (!view) return <div className="card"><p className="muted">Loading…</p></div>;

  const canRsvp = !view.locked && view.eventState !== "CANCELLED";

  return (
    <div className="card">
      <h2 style={{ marginBottom: 4 }}>{view.eventTitle}</h2>
      <div className="muted" style={{ color: "var(--muted)", fontSize: 13 }}>
        {new Date(view.eventStartTime).toLocaleString()} · {view.eventLocation}
      </div>
      {view.eventDescription && <p style={{ marginTop: 12 }}>{view.eventDescription}</p>}
      <hr style={{ borderColor: "var(--border)", margin: "16px 0" }} />
      <p style={{ fontSize: 13, color: "var(--muted)" }}>Invited as <strong>{view.invitedEmail}</strong></p>
      <p>{statusLine(view.currentStatus)}</p>
      {view.eventState === "CANCELLED" && (
        <p className="error">This event has been cancelled.</p>
      )}
      {view.locked && view.eventState !== "CANCELLED" && (
        <p className="error">The event has started. RSVPs are locked.</p>
      )}
      {canRsvp && (
        <>
          <p style={{ marginTop: 16, marginBottom: 0, color: "var(--muted)", fontSize: 13 }}>Your response:</p>
          <div className="choice-row">
            <button onClick={() => choose("YES")} disabled={submitting}>Yes</button>
            <button onClick={() => choose("MAYBE")} disabled={submitting} className="secondary">Maybe</button>
            <button onClick={() => choose("NO")} disabled={submitting} className="secondary">No</button>
          </div>
        </>
      )}
    </div>
  );
}
