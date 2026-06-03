import { type ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";

export function RequireAuth({ children }: { children: ReactNode }) {
  const { me, loading } = useAuth();
  if (loading) return <div className="card"><p className="muted">Loading…</p></div>;
  if (!me) return <Navigate to="/login" replace />;
  return <>{children}</>;
}
