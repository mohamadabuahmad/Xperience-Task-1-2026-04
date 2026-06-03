import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { ApiError, api } from "../api";

export function EventCreatePage() {
  const nav = useNavigate();
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [startTime, setStartTime] = useState("");
  const [location, setLocation] = useState("");
  const [capacity, setCapacity] = useState<string>("");
  const [error, setError] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const onSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError("");
    setSubmitting(true);
    try {
      const cap = capacity.trim() === "" ? null : Number(capacity);
      const created = await api.createEvent({
        title,
        description: description || undefined,
        startTime: new Date(startTime).toISOString(),
        location,
        capacity: cap,
      });
      nav(`/host/events/${created.id}`);
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Failed to create event");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="card">
      <h2>New event</h2>
      <form onSubmit={onSubmit}>
        <label>Title</label>
        <input value={title} onChange={(e) => setTitle(e.target.value)} required />
        <div className="spacer" />
        <label>Description</label>
        <textarea value={description} onChange={(e) => setDescription(e.target.value)} />
        <div className="spacer" />
        <div className="row">
          <div>
            <label>Start time</label>
            <input
              type="datetime-local"
              value={startTime}
              onChange={(e) => setStartTime(e.target.value)}
              required
            />
          </div>
          <div>
            <label>Location</label>
            <input value={location} onChange={(e) => setLocation(e.target.value)} required />
          </div>
          <div>
            <label>Capacity (optional)</label>
            <input
              type="number"
              min={1}
              value={capacity}
              onChange={(e) => setCapacity(e.target.value)}
              placeholder="unlimited"
            />
          </div>
        </div>
        {error && <div className="error">{error}</div>}
        <div className="spacer" />
        <div className="gap-8">
          <button type="submit" disabled={submitting}>Create</button>
          <button type="button" className="secondary" onClick={() => nav(-1)}>Cancel</button>
        </div>
      </form>
    </div>
  );
}
