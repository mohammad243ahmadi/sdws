"use client";

import { useEffect, useState, type FormEvent } from "react";
import Link from "next/link";
import { Check, LoaderCircle, X } from "lucide-react";
import { useRouter } from "next/navigation";
import { api, dateTime, money, type MoneyRequest, type MoneyRequests, type Transaction, type User, type Wallet } from "@/lib/api";
import { TransactionsTable } from "@/components/transactions-table";

type WorkspaceSectionProps = { section: string };

const titles: Record<string, { title: string; subtitle: string }> = {
  wallet: { title: "My wallet", subtitle: "Your account details and available balance." },
  receive: { title: "Received money", subtitle: "Incoming transfers to your wallet." },
  history: { title: "Transaction history", subtitle: "Review activity and filter by transaction type." },
  send: { title: "Send money", subtitle: "Transfer funds to another registered wallet." },
  add: { title: "Add money", subtitle: "Add funds to your wallet. This is a local simulation." },
  withdraw: { title: "Withdraw funds", subtitle: "Move funds out of your wallet balance." },
  request: { title: "Request money", subtitle: "Ask another wallet holder to send you funds." },
  requests: { title: "Money requests", subtitle: "Review incoming requests and track requests you sent." },
  topup: { title: "Mobile top-up", subtitle: "Record a simulated mobile top-up." },
  bills: { title: "Bill payment", subtitle: "Record a simulated bill payment." },
  profile: { title: "Profile settings", subtitle: "Update your contact details and password." },
};

type FieldConfig = { name: string; label: string; type?: string; required?: boolean; options?: string[]; defaultValue?: string };

export function WorkspaceSection({ section }: WorkspaceSectionProps) {
  const router = useRouter();
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [requests, setRequests] = useState<MoneyRequests | null>(null);
  const [user, setUser] = useState<User | null>(null);
  const [filter, setFilter] = useState("");
  const [pending, setPending] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const page = titles[section];

  useEffect(() => {
    if (section === "wallet" || section === "receive") {
      const request = section === "wallet"
        ? api<Wallet>("/api/wallet").then((data) => { setWallet(data); setError(""); })
        : api<Transaction[]>("/api/transactions?type=RECEIVE").then((data) => { setTransactions(data); setError(""); });
      request.catch((cause) => setError(cause instanceof Error ? cause.message : "Unable to load wallet data."));
    } else if (section === "history") {
      const query = filter ? `?type=${encodeURIComponent(filter)}` : "";
      api<Transaction[]>(`/api/transactions${query}`).then((data) => { setTransactions(data); setError(""); })
        .catch((cause) => setError(cause instanceof Error ? cause.message : "Unable to load history."));
    } else if (section === "requests") {
      api<MoneyRequests>("/api/money-requests").then((data) => { setRequests(data); setError(""); })
        .catch((cause) => setError(cause instanceof Error ? cause.message : "Unable to load requests."));
    } else if (section === "profile") {
      api<{ user: User }>("/api/auth/me").then((response) => { setUser(response.user); setError(""); })
        .catch((cause) => setError(cause instanceof Error ? cause.message : "Unable to load profile."));
    }
  }, [section, filter]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setMessage("");
    setPending(true);
    const values = Object.fromEntries(new FormData(event.currentTarget));
    const amount = Number(values.amount);
    const bodies: Record<string, { path: string; body: Record<string, unknown> }> = {
      send: { path: "/api/transfers", body: { phone: values.phone, amount, note: values.note || null } },
      add: { path: "/api/wallet/deposits", body: { amount } },
      withdraw: { path: "/api/wallet/withdrawals", body: { amount } },
      request: { path: "/api/money-requests", body: { phone: values.phone, amount } },
      topup: { path: "/api/mobile-topups", body: { operator: values.operator, number: values.number, amount } },
      bills: { path: "/api/bill-payments", body: { category: values.category, billNumber: values.billNumber, amount } },
    };

    try {
      if (section === "profile") {
        await api<User>("/api/users/me", {
          method: "PUT",
          body: JSON.stringify({ fullName: values.fullName, email: values.email || null, currentPassword: values.currentPassword || null, newPassword: values.newPassword || null }),
        });
        setMessage("Profile updated.");
      } else {
        const operation = bodies[section];
        if (!operation) throw new Error("This action is not available.");
        await api(operation.path, { method: "POST", body: JSON.stringify(operation.body) });
        router.push(section === "request" ? "/requests" : "/dashboard");
        router.refresh();
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Unable to complete this action.");
    } finally {
      setPending(false);
    }
  }

  async function respond(request: MoneyRequest, action: "accept" | "decline") {
    setError("");
    setPending(true);
    try {
      await api(`/api/money-requests/${request.id}/${action}`, { method: "POST" });
      setRequests((current) => current ? {
        ...current,
        incoming: current.incoming.map((item) => item.id === request.id ? { ...item, status: action === "accept" ? "ACCEPTED" : "DECLINED" } : item),
      } : current);
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Unable to update request.");
    } finally {
      setPending(false);
    }
  }

  if (!page) return <section className="page-panel"><h1>Page not found</h1><Link className="text-link" href="/dashboard">Return to overview</Link></section>;

  const operationFields: Record<string, FieldConfig[]> = {
    send: [
      { name: "phone", label: "Recipient phone number", required: true },
      { name: "amount", label: "Amount (AFN)", type: "number", required: true },
      { name: "note", label: "Note", required: false },
    ],
    add: [{ name: "amount", label: "Amount (AFN)", type: "number", required: true }],
    withdraw: [{ name: "amount", label: "Amount (AFN)", type: "number", required: true }],
    request: [
      { name: "phone", label: "Request from phone number", required: true },
      { name: "amount", label: "Amount (AFN)", type: "number", required: true },
    ],
    topup: [
      { name: "operator", label: "Operator", options: ["Roshan", "AWCC", "Etisalat", "MTN", "Salaam"] },
      { name: "number", label: "Mobile number", required: true },
      { name: "amount", label: "Amount (AFN)", type: "number", required: true },
    ],
    bills: [
      { name: "category", label: "Category", options: ["Electricity", "Water", "Internet", "TV", "Other"] },
      { name: "billNumber", label: "Bill / customer number", required: true },
      { name: "amount", label: "Amount (AFN)", type: "number", required: true },
    ],
  };

  return (
    <>
      <div className="page-heading"><div><h1>{page.title}</h1><p>{page.subtitle}</p></div></div>
      {error && <div className="feedback error" role="alert">{error}</div>}
      {message && <div className="feedback" role="status">{message}</div>}
      {section === "wallet" && wallet && <section className="page-panel"><div className="balance-detail">
        <div><span>Account holder</span><strong>{wallet.ownerName}</strong></div>
        <div><span>Account number</span><strong className="mono">{wallet.accountNumber}</strong></div>
        <div><span>Available balance</span><strong>{money(wallet.balance)} AFN</strong></div>
      </div></section>}
      {(section === "receive" || section === "history") && <>
        {section === "history" && <div className="field" style={{ maxWidth: 240 }}><label htmlFor="type-filter">Transaction type</label><select id="type-filter" value={filter} onChange={(event) => setFilter(event.target.value)}><option value="">All activity</option>{["SEND", "RECEIVE", "ADD", "WITHDRAW", "TOPUP", "BILL"].map((type) => <option key={type}>{type}</option>)}</select></div>}
        <TransactionsTable transactions={transactions} />
      </>}
      {section === "requests" && requests && <div className="two-column">
        <RequestGroup title="Requests sent to you" requests={requests.incoming} incoming onRespond={respond} pending={pending} />
        <RequestGroup title="Requests you sent" requests={requests.sent} pending={pending} />
      </div>}
      {section === "profile" && user && <form className="form-panel" onSubmit={submit}>
        <Field config={{ name: "fullName", label: "Full name", required: true, defaultValue: user.fullName }} />
        <Field config={{ name: "email", label: "Email", type: "email", required: false, defaultValue: user.email ?? "" }} />
        <p className="form-hint">Leave password fields empty to keep your current password.</p>
        <Field config={{ name: "currentPassword", label: "Current password", type: "password", required: false }} />
        <Field config={{ name: "newPassword", label: "New password", type: "password", required: false }} />
        <button className="button-primary" type="submit" disabled={pending}>{pending ? <LoaderCircle className="spin" aria-hidden="true" /> : "Save profile"}</button>
      </form>}
      {operationFields[section] && <form className="form-panel" onSubmit={submit}>
        {section === "add" && <p className="form-hint">Funds are added directly to your wallet for demonstration purposes.</p>}
        {section === "topup" && <p className="form-hint">No mobile operator is contacted; this is a simulated transaction.</p>}
        {section === "bills" && <p className="form-hint">The payment is recorded here only. No biller is contacted.</p>}
        {operationFields[section].map((config) => <Field key={config.name} config={config} />)}
        <button className="button-primary" type="submit" disabled={pending}>{pending ? <LoaderCircle className="spin" aria-hidden="true" /> : "Confirm"}</button>
      </form>}
      {section === "receive" && wallet && <section className="page-panel" style={{ marginTop: 18 }}><h2 style={{ margin: "0 0 8px", fontSize: 14 }}>Your receiving details</h2><p className="muted">Share your phone number <strong className="mono">{wallet.phone}</strong> or account number <strong className="mono">{wallet.accountNumber}</strong> to receive a transfer.</p></section>}
    </>
  );
}

function Field({ config }: { config: FieldConfig }) {
  return <div className="field"><label htmlFor={config.name}>{config.label}</label>
    {config.options
      ? <select id={config.name} name={config.name} defaultValue={config.options[0]}>{config.options.map((option) => <option key={option}>{option}</option>)}</select>
      : <input id={config.name} name={config.name} type={config.type ?? "text"} defaultValue={config.defaultValue} required={config.required ?? true} min={config.type === "number" ? "0.01" : undefined} step={config.type === "number" ? "0.01" : undefined} inputMode={config.name === "phone" || config.name === "number" ? "numeric" : undefined} pattern={config.name === "phone" || config.name === "number" ? "[0-9]{9,15}" : undefined} maxLength={config.name === "billNumber" ? 100 : undefined} />}
  </div>;
}

function RequestGroup({ title, requests, incoming = false, onRespond, pending }: {
  title: string;
  requests: MoneyRequest[];
  incoming?: boolean;
  onRespond?: (request: MoneyRequest, action: "accept" | "decline") => void;
  pending: boolean;
}) {
  return <section className="request-group"><h2>{title}</h2>
    {requests.length === 0 ? <div className="page-panel muted">No requests yet.</div> : requests.map((request) => <article className="page-panel" key={request.id} style={{ marginBottom: 9 }}>
      <div style={{ display: "flex", justifyContent: "space-between", gap: 10 }}><strong>{incoming ? request.requester.fullName : request.target.fullName}</strong><span className="badge">{request.status}</span></div>
      <div className="muted mono" style={{ marginTop: 4 }}>{incoming ? request.requester.phone : request.target.phone}</div>
      <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 10, marginTop: 12 }}><strong>{money(request.amount)} AFN</strong><span className="muted">{dateTime(request.createdAt)}</span></div>
      {incoming && request.status === "PENDING" && onRespond && <div className="small-actions" style={{ marginTop: 13 }}>
        <button className="button-primary" type="button" disabled={pending} onClick={() => onRespond(request, "accept")}><Check aria-hidden="true" />Pay</button>
        <button className="button-danger" type="button" disabled={pending} onClick={() => onRespond(request, "decline")}><X aria-hidden="true" />Decline</button>
      </div>}
    </article>)}
  </section>;
}