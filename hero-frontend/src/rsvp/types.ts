export type EventState = "OPEN" | "CLOSED" | "CANCELLED";
export type RsvpStatus = "YES_CONFIRMED" | "YES_WAITLISTED" | "NO" | "MAYBE";
export type Choice = "YES" | "NO" | "MAYBE";

export interface Me {
  id: number;
  email: string;
}

export interface EventResponse {
  id: number;
  title: string;
  description: string | null;
  startTime: string;
  location: string;
  capacity: number | null;
  state: EventState;
  locked: boolean;
}

export interface CreatedInvitation {
  id: number;
  email: string;
  token: string;
  link: string;
  issuedAt: string;
}

export interface InvitationListItem {
  id: number;
  email: string;
  issuedAt: string;
}

export interface InviteeView {
  eventTitle: string;
  eventDescription: string | null;
  eventLocation: string;
  eventStartTime: string;
  eventState: EventState;
  locked: boolean;
  invitedEmail: string;
  currentStatus: RsvpStatus | null;
}

export interface AttendeeRow {
  invitationId: number;
  email: string;
  status: RsvpStatus | null;
  updatedAt: string | null;
}

export interface DashboardCounts {
  confirmed: number;
  waitlisted: number;
  no: number;
  maybe: number;
  pending: number;
  capacity: number | null;
}

export interface Dashboard {
  counts: DashboardCounts;
  attendees: AttendeeRow[];
}
