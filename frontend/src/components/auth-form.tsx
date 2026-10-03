"use client";

import { useState, type FormEvent } from "react";
import { ArrowRight, CircleDollarSign, LoaderCircle } from "lucide-react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { api, type User } from "@/lib/api";

type AuthFormProps = { mode: "login" | "register" };
type AuthResponse = { user: User };

export function AuthForm({ mode }: AuthFormProps) {
  const router = useRouter();
  const [error, setError] = useState("");
  const [pending, setPending] = useState(false);
  const registering = mode === "register";

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError("");
    setPending(true);
    const values = Object.fromEntries(new FormData(event.currentTarget));

    try {
      if (registering) {
        await api("/api/auth/register", {
          method: "POST",
          body: JSON.stringify({ fullName: values.fullName, phone: values.phone, email: values.email || null, password: values.password }),
        });
        router.push("/login?registered=1");
      } else {
        const response = await api<AuthResponse>("/api/auth/login", {
          method: "POST",
          body: JSON.stringify({ phone: values.phone, password: values.password }),
        });
        router.push(response.user.role === "ADMIN" ? "/admin" : "/dashboard");
        router.refresh();
      }
    } catch (cause) {
      setError(cause instanceof Error ? cause.message : "Unable to complete your request.");
    } finally {
      setPending(false);
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-art" aria-label="Secure Digital Wallet System">
        <div className="brand-lockup"><span className="brand-mark"><CircleDollarSign aria-hidden="true" /></span> SDWS <span className="muted">/ WALLET</span></div>
        <div className="auth-art-copy">
          <span className="auth-eyebrow">Secure digital finance</span>
          <h1>{registering ? "A clearer way to move money." : "Your money, in good order."}</h1>
          <p>Send, receive, and track every wallet movement from one dependable place.</p>
        </div>
        <div className="auth-art-foot">PRIVATE BY DESIGN <span aria-hidden="true">·</span> BUILT FOR EVERYDAY USE</div>
      </section>
      <section className="auth-panel">
        <div className="auth-box">
          <h2>{registering ? "Create your account" : "Welcome back"}</h2>
          <p>{registering ? "Your wallet starts with a few details." : "Sign in to continue to your wallet."}</p>
          {error && <div className="feedback error" role="alert">{error}</div>}
          <form onSubmit={submit}>
            {registering && <div className="field"><label htmlFor="fullName">Full name</label><input id="fullName" name="fullName" autoComplete="name" maxLength={100} required /></div>}
            <div className="field"><label htmlFor="phone">Phone number</label><input id="phone" name="phone" inputMode="numeric" autoComplete="tel" pattern="[0-9]{9,15}" required /></div>
            {registering && <div className="field"><label htmlFor="email">Email <span className="muted">(optional)</span></label><input id="email" name="email" type="email" autoComplete="email" /></div>}
            <div className="field"><label htmlFor="password">Password</label><input id="password" name="password" type="password" autoComplete={registering ? "new-password" : "current-password"} minLength={8} maxLength={100} required /></div>
            <button className="button-primary button-wide" type="submit" disabled={pending}>
              {pending ? <LoaderCircle className="spin" aria-hidden="true" /> : <>{registering ? "Create account" : "Sign in"}<ArrowRight aria-hidden="true" /></>}
            </button>
          </form>
          <p className="auth-switch">{registering ? "Already have an account?" : "New to SDWS?"} {" "}
            <Link href={registering ? "/login" : "/register"}>{registering ? "Sign in" : "Create an account"}</Link>
          </p>
        </div>
      </section>
    </main>
  );
}