"use client";

import { useEffect, useState } from "react";
import { LoaderCircle, ShieldCheck, UserRoundCheck, UserRoundX } from "lucide-react";
import { api, money, type Transaction, type User } from "@/lib/api";
import { TransactionsTable } from "@/components/transactions-table";

type AdminSectionProps = { section: string };
type AdminOverview = {
  userCount: number;
  activeCount: number;
  transactionCount: number;
  totalBalance: number;
  latestTransactions: Transaction[];
};

export function AdminSection({ section }: AdminSectionProps) {
  const [overview, setOverview] = useState<AdminOverview | null>(null);
  const [users, setUsers] = useState<User[]>([]);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [filter, setFilter] = useState("");
  const [phone, setPhone] = useState("");
  const [pendingId, setPendingId] = useState<number | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    if (section === "overview") {
      api<AdminOverview>("/api/admin/overview").then((data) => { setOverview(data); setError(""); })
        .catch((cause) => setError(cause instanceof Error ? cause.message : "Unable to load overview."));
    } else if (section === "users") {
      api<User[]>("/api/admin/users").then((data) => { setUsers(data); setError(""); })
        .catch((cause) => setError(cause instanceof Error ? cause.message : "Unable to load users."));
    } else if (section === "transactions") {
      const params = new URLSearchParams();
      if (filter) params.set("type", filter);
      if (phone) params.set("phone", phone);
      const query = params.size ? `?${params.toString()}` : "";
      api<Transaction[]>(`/api/admin/transactions${query}`).then((data) => { setTransactions(data); setError(""); })
        .catch((cause) => setError(cause instanceof Error ? cause.message : "Unable to load transactions."));
    }
  }, [section, filter, phone]);

  async function toggleActive(user: User) {
    setError("");
    setPendingId(user.id);
    try {
      await api(`/api/admin/users/${user.id}`, { method: "PATCH", body: JSON.stringify({ active: !user.active }) });
      setUsers((current) => current.map((entry) => entry.id === user.id ? { ...entry, active: !entry.active } : entry));
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Unable to update user status.");
    } finally {
      setPendingId(null);
    }
  }

  const title = section === "users" ? "Users" : section === "transactions" ? "All transactions" : "Admin overview";
  const subtitle = section === "users" ? "Manage account access." : section === "transactions" ? "Search wallet activity across all users." : "System activity and wallet totals.";

  return (
    <>
      <div className="page-heading"><div><h1>{title}</h1><p>{subtitle}</p></div><span className="topbar-mark"><ShieldCheck aria-hidden="true" />ADMIN ACCESS</span></div>
      {error && <div className="feedback error" role="alert">{error}</div>}
      {section === "overview" && overview && <>
        <div className="stats-row">
          <Stat label="Users" value={String(overview.userCount)} />
          <Stat label="Active users" value={String(overview.activeCount)} />
          <Stat label="Transactions" value={String(overview.transactionCount)} />
        </div>
        <div className="section-title"><h2>Total wallet balance</h2></div>
        <div className="page-panel"><strong style={{ fontSize: 26 }}>{money(overview.totalBalance)} <span className="muted">AFN</span></strong></div>
        <div className="section-title"><h2>Latest activity</h2></div>
        <TransactionsTable transactions={overview.latestTransactions} showUser />
      </>}
      {section === "users" && <div className="table-wrap"><table>
        <thead><tr><th>Name</th><th>Phone</th><th>Email</th><th>Role</th><th>Status</th><th>Access</th></tr></thead>
        <tbody>
          {users.map((user) => <tr key={user.id}>
            <td><strong>{user.fullName}</strong></td><td className="mono">{user.phone}</td><td>{user.email || "—"}</td><td>{user.role}</td>
            <td><span className={`badge ${user.active ? "badge-credit" : "badge-debit"}`}>{user.active ? "ACTIVE" : "INACTIVE"}</span></td>
            <td><button className={user.active ? "button-danger" : "button-secondary"} type="button" disabled={pendingId === user.id} onClick={() => toggleActive(user)}>
              {pendingId === user.id ? <LoaderCircle className="spin" aria-hidden="true" /> : user.active ? <><UserRoundX aria-hidden="true" />Deactivate</> : <><UserRoundCheck aria-hidden="true" />Activate</>}
            </button></td>
          </tr>)}
          {users.length === 0 && <tr><td className="empty-cell" colSpan={6}>No users to display.</td></tr>}
        </tbody>
      </table></div>}
      {section === "transactions" && <>
        <div className="admin-filters">
          <div className="field"><label htmlFor="transaction-type">Type</label><select id="transaction-type" value={filter} onChange={(event) => setFilter(event.target.value)}><option value="">All types</option>{["SEND", "RECEIVE", "ADD", "WITHDRAW", "TOPUP", "BILL"].map((type) => <option key={type}>{type}</option>)}</select></div>
          <div className="field"><label htmlFor="phone-search">Phone number</label><input id="phone-search" value={phone} onChange={(event) => setPhone(event.target.value)} inputMode="numeric" placeholder="Filter by phone" /></div>
        </div>
        <TransactionsTable transactions={transactions} showUser />
      </>}
    </>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return <div className="stat-block"><span>{label}</span><strong>{value}</strong></div>;
}