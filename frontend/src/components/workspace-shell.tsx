"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { usePathname, useRouter } from "next/navigation";
import { Activity, ArrowLeftRight, BanknoteArrowDown, BanknoteArrowUp, CircleDollarSign, FileClock, HandCoins, LayoutDashboard, LogOut, ReceiptText, ShieldCheck, Smartphone, UserRound, WalletCards } from "lucide-react";
import { api, type User } from "@/lib/api";

type AuthResponse = { user: User };
type WorkspaceShellProps = { children: React.ReactNode };

const userItems = [
  { href: "/dashboard", label: "Overview", icon: LayoutDashboard },
  { href: "/wallet", label: "My wallet", icon: WalletCards },
  { href: "/send", label: "Send money", icon: ArrowLeftRight },
  { href: "/receive", label: "Received", icon: BanknoteArrowDown },
  { href: "/add", label: "Add money", icon: BanknoteArrowUp },
  { href: "/withdraw", label: "Withdraw", icon: HandCoins },
  { href: "/request", label: "Request money", icon: HandCoins },
  { href: "/requests", label: "Requests", icon: Activity },
  { href: "/topup", label: "Mobile top-up", icon: Smartphone },
  { href: "/bills", label: "Bill payment", icon: ReceiptText },
  { href: "/history", label: "History", icon: FileClock },
  { href: "/profile", label: "Profile", icon: UserRound },
];

const adminItems = [
  { href: "/admin", label: "Overview", icon: LayoutDashboard },
  { href: "/admin/users", label: "Users", icon: UserRound },
  { href: "/admin/transactions", label: "Transactions", icon: FileClock },
];

export function WorkspaceShell({ children }: WorkspaceShellProps) {
  const pathname = usePathname();
  const router = useRouter();
  const [user, setUser] = useState<User | null>(null);
  const [ready, setReady] = useState(false);
  const admin = user?.role === "ADMIN";
  const items = admin ? adminItems : userItems;
  const mustRedirect = user !== null && ((admin && pathname === "/dashboard") || (!admin && pathname.startsWith("/admin")));

  useEffect(() => {
    let mounted = true;
    api<AuthResponse>("/api/auth/me")
      .then((response) => { if (mounted) setUser(response.user); })
      .catch(() => { if (mounted) router.replace("/login"); })
      .finally(() => { if (mounted) setReady(true); });
    return () => { mounted = false; };
  }, [router]);

  useEffect(() => {
    if (admin && pathname === "/dashboard") router.replace("/admin");
    if (user && !admin && pathname.startsWith("/admin")) router.replace("/dashboard");
  }, [admin, pathname, router, user]);

  async function logout() {
    try { await api("/api/auth/logout", { method: "POST" }); }
    finally { router.replace("/login"); }
  }

  if (!ready || !user || mustRedirect) return <div className="loading-state" role="status">Loading wallet…</div>;

  return (
    <div className="workspace">
      <aside className="sidebar">
        <Link className="brand-lockup" href={admin ? "/admin" : "/dashboard"}><span className="brand-mark"><CircleDollarSign aria-hidden="true" /></span> SDWS <span className="muted">/ WALLET</span></Link>
        <div className="nav-caption">{admin ? "Administration" : "Your account"}</div>
        <nav className="side-nav" aria-label="Main navigation">
          {items.map(({ href, label, icon: Icon }) => {
            const active = pathname === href || (href !== "/dashboard" && href !== "/admin" && pathname.startsWith(`${href}/`));
            return <Link key={href} className={`side-link ${active ? "active" : ""}`} href={href} aria-current={active ? "page" : undefined}><Icon aria-hidden="true" />{label}</Link>;
          })}
        </nav>
        <div className="sidebar-footer">
          <div className="user-chip"><strong>{user.fullName}</strong><span>{user.phone}</span></div>
          <button className="logout-button" type="button" onClick={logout}><LogOut aria-hidden="true" />Sign out</button>
        </div>
      </aside>
      <div className="workspace-main">
        <header className="workspace-topbar">
          <div className="crumb">SDWS <span aria-hidden="true">/</span> <strong>{admin ? "Administration" : "Wallet"}</strong></div>
          <div className="topbar-mark">{admin ? <ShieldCheck aria-hidden="true" /> : <CircleDollarSign aria-hidden="true" />}{admin ? "ADMIN CONSOLE" : "SECURE WALLET"}</div>
        </header>
        <main className="workspace-content">{children}</main>
      </div>
    </div>
  );
}