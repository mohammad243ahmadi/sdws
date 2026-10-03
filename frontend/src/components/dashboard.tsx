"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { ArrowLeftRight, ArrowUpRight, BanknoteArrowUp, HandCoins, ReceiptText, Smartphone, WalletCards } from "lucide-react";
import { api, money, type Transaction, type Wallet } from "@/lib/api";
import { TransactionsTable } from "@/components/transactions-table";

const actions = [
  { href: "/send", label: "Send money", icon: ArrowLeftRight },
  { href: "/add", label: "Add money", icon: BanknoteArrowUp },
  { href: "/withdraw", label: "Withdraw", icon: ArrowUpRight },
  { href: "/request", label: "Request", icon: HandCoins },
  { href: "/topup", label: "Mobile top-up", icon: Smartphone },
  { href: "/bills", label: "Pay a bill", icon: ReceiptText },
];

export function Dashboard() {
  const [wallet, setWallet] = useState<Wallet | null>(null);
  const [transactions, setTransactions] = useState<Transaction[]>([]);
  const [error, setError] = useState("");

  useEffect(() => {
    Promise.all([api<Wallet>("/api/wallet"), api<Transaction[]>("/api/transactions")])
      .then(([walletData, transactionData]) => { setWallet(walletData); setTransactions(transactionData.slice(0, 5)); })
      .catch((cause) => setError(cause instanceof Error ? cause.message : "Unable to load wallet data."));
  }, []);

  return (
    <>
      <div className="page-heading"><div><h1>Good to see you, {wallet?.ownerName ?? ""}</h1><p>Your wallet at a glance.</p></div><span className="status-line"><i className="status-dot" />Account active</span></div>
      {error && <div className="feedback error" role="alert">{error}</div>}
      <section className="balance-panel" aria-label="Wallet balance">
        <div className="balance-copy">
          <div className="balance-label">Available balance</div>
          <div className="balance-value">{wallet ? money(wallet.balance) : "—"} <span>AFN</span></div>
          <div className="account-meta"><span>ACCOUNT <code>{wallet?.accountNumber ?? "Loading"}</code></span><span>PHONE <code>{wallet?.phone ?? ""}</code></span></div>
        </div>
        <div className="balance-icon"><WalletCards aria-hidden="true" /></div>
      </section>
      <div className="section-title"><h2>Move money</h2></div>
      <nav className="action-grid" aria-label="Wallet actions">
        {actions.map(({ href, label, icon: Icon }) => <Link key={href} href={href} className="action-tile"><span className="action-icon"><Icon aria-hidden="true" /></span>{label}</Link>)}
      </nav>
      <div className="section-title"><h2>Recent activity</h2><Link className="text-link" href="/history">View history</Link></div>
      <TransactionsTable transactions={transactions} />
    </>
  );
}