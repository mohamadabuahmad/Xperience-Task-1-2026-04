import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "./AuthContext";

export function Nav() {
  const { me, logout } = useAuth();
  const navigate = useNavigate();
  return (
    <div className="nav">
      <div>
        <span className="nav-brand">Event RSVP</span>
        {me && (
          <>
            <span style={{ marginLeft: 24 }} />
            <Link to="/host/events">Events</Link>
            <Link to="/host/events/new">Create</Link>
          </>
        )}
      </div>
      <div className="nav-right">
        {me ? (
          <>
            <span className="muted" style={{ color: "var(--muted)", fontSize: 13 }}>{me.email}</span>
            <button
              className="secondary"
              onClick={async () => {
                await logout();
                navigate("/login");
              }}
            >
              Log out
            </button>
          </>
        ) : (
          <>
            <Link to="/login">Log in</Link>
            <Link to="/signup">Sign up</Link>
          </>
        )}
      </div>
    </div>
  );
}
