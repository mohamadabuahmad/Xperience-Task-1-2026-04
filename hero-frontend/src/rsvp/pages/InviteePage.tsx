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
  if (!view) return <div className="card"><p className="loading">Loading your invitation…</p></div>;

  const canRsvp = !view.locked && view.eventState !== "CANCELLED";

  return (
    <div className="invite-card">
      <div className="invite-eyebrow">You're Invited</div>
      <h2>{view.eventTitle}</h2>
      <div className="invite-meta">
        {new Date(view.eventStartTime).toLocaleString()} · {view.eventLocation}
      </div>
      {view.eventDescription && <p>{view.eventDescription}</p>}
      <hr />
      <p style={{ fontSize: 13, marginBottom: 6 }}>
        Invited as <strong>{view.invitedEmail}</strong>
      </p>
      <p style={{ margin: 0 }}>{statusLine(view.currentStatus)}</p>
      {view.eventState === "CANCELLED" && (
        <p className="error" style={{ marginTop: 18 }}>This event has been cancelled.</p>
      )}
      {view.locked && view.eventState !== "CANCELLED" && (
        <p className="error" style={{ marginTop: 18 }}>The event has started. RSVPs are locked.</p>
      )}
      {canRsvp && (
        <>
          <p style={{ marginTop: 22, marginBottom: 0, color: "var(--muted)", fontSize: 12, letterSpacing: "0.14em", textTransform: "uppercase" }}>
            Your reply
          </p>
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
