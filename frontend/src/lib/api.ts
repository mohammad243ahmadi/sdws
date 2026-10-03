export type User = {
  id: number;
  fullName: string;
  phone: string;
  email: string | null;
  role: "USER" | "ADMIN";
  active: boolean;
};

export type Wallet = {
  userId: number;
  ownerName: string;
  phone: string;
  accountNumber: string;
  balance: number;
};

export type Transaction = {
  id: number;
  type: string;
  amount: number;
  status: string;
  description: string;
  createdAt: string;
  userName: string;
  userPhone: string;
};

export type MoneyRequest = {
  id: number;
  requester: { id: number; fullName: string; phone: string };
  target: { id: number; fullName: string; phone: string };
  amount: number;
  status: "PENDING" | "ACCEPTED" | "DECLINED";
  createdAt: string;
};

export type MoneyRequests = { sent: MoneyRequest[]; incoming: MoneyRequest[] };
type CsrfResponse = { headerName: string; token: string };

export async function api<T>(path: string, init: RequestInit = {}): Promise<T> {
  const method = (init.method ?? "GET").toUpperCase();
  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (init.body) headers.set("Content-Type", "application/json");

  if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
    const csrf = await fetch("/api/auth/csrf", { credentials: "same-origin", cache: "no-store" });
    if (!csrf.ok) throw new Error("Could not initialize request protection.");
    const token = (await csrf.json()) as CsrfResponse;
    headers.set(token.headerName, token.token);
  }

  const response = await fetch(path, {
    ...init,
    method,
    headers,
    credentials: "same-origin",
    cache: "no-store",
  });

  if (!response.ok) {
    const error = (await response.json().catch(() => null)) as { message?: string } | null;
    throw new Error(error?.message ?? `Request failed (${response.status}).`);
  }
  if (response.status === 204) return undefined as T;
  return (await response.json()) as T;
}

export function money(value: number): string {
  return new Intl.NumberFormat("en-US", { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(value);
}

export function dateTime(value: string): string {
  return new Intl.DateTimeFormat("en-GB", { dateStyle: "medium", timeStyle: "short" }).format(new Date(value));
}