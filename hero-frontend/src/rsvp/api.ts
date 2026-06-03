import type {
  Choice,
  CreatedInvitation,
  Dashboard,
  EventResponse,
  InvitationListItem,
  InviteeView,
  Me,
} from "./types";

const BASE = "http://localhost:8280/api";

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    credentials: "include",
    headers: { "Content-Type": "application/json", ...(init.headers || {}) },
    ...init,
  });
  if (!res.ok) {
    let message = `${res.status} ${res.statusText}`;
    try {
      const body = await res.json();
      if (body?.message) message = body.message;
    } catch {
      /* non-JSON body */
    }
    throw new ApiError(res.status, message);
  }
  if (res.status === 204) return undefined as T;
  return res.json() as Promise<T>;
}

export class ApiError extends Error {
  status: number;
  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

export const api = {
  // auth
  signup: (email: string, password: string) =>
    request<Me>("/auth/signup", { method: "POST", body: JSON.stringify({ email, password }) }),
  login: (email: string, password: string) =>
    request<Me>("/auth/login", { method: "POST", body: JSON.stringify({ email, password }) }),
  logout: () => request<void>("/auth/logout", { method: "POST" }),
  me: () => request<Me>("/auth/me"),

  // events
  listEvents: () => request<EventResponse[]>("/host/events"),
  createEvent: (body: {
    title: string;
    description?: string;
    startTime: string;
    location: string;
    capacity?: number | null;
  }) => request<EventResponse>("/host/events", { method: "POST", body: JSON.stringify(body) }),
  getEvent: (id: number) => request<EventResponse>(`/host/events/${id}`),
  closeEvent: (id: number) => request<EventResponse>(`/host/events/${id}/close`, { method: "POST" }),
  cancelEvent: (id: number) => request<EventResponse>(`/host/events/${id}/cancel`, { method: "POST" }),

  // invitations
  invite: (eventId: number, email: string) =>
    request<CreatedInvitation>(`/host/events/${eventId}/invitations`, {
      method: "POST",
      body: JSON.stringify({ email }),
    }),
  listInvitations: (eventId: number) =>
    request<InvitationListItem[]>(`/host/events/${eventId}/invitations`),

  // dashboard
  dashboard: (eventId: number) => request<Dashboard>(`/host/events/${eventId}/dashboard`),

  // invitee
  inviteeView: (token: string) => request<InviteeView>(`/invite/${token}`),
  submitRsvp: (token: string, choice: Choice) =>
    request<InviteeView>(`/invite/${token}/rsvp`, {
      method: "POST",
      body: JSON.stringify({ choice }),
    }),
};
