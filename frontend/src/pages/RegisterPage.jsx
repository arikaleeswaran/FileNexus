import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { ShieldCheck, UserPlus } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { getApiError } from "../services/apiClient";

export default function RegisterPage() {
  const { signUp } = useAuth();
  const navigate = useNavigate();
  const [form, setForm] = useState({ username: "", email: "", password: "" });
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");
  const [busy, setBusy] = useState(false);

  function update(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    setError("");
    setMessage("");
    setBusy(true);
    try {
      await signUp(form);
      setMessage("Account created successfully. You can now sign in.");
      setTimeout(() => navigate("/login", { replace: true }), 900);
    } catch (err) {
      setError(getApiError(err));
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="flex min-h-screen items-center justify-center bg-[#f3f6fb] px-4 py-10">
      <section className="w-full max-w-md overflow-hidden rounded-3xl border border-slate-200 bg-white shadow-xl shadow-slate-200/60">
        <div className="bg-[#0b1730] px-8 py-8 text-white">
          <div className="mb-5 flex h-12 w-12 items-center justify-center rounded-2xl bg-blue-500"><ShieldCheck size={25}/></div>
          <p className="text-[10px] font-bold uppercase tracking-[.22em] text-blue-300">FileNexus account</p>
          <h1 className="mt-2 text-2xl font-extrabold">Create your account</h1>
          <p className="mt-2 text-xs text-slate-300">Your account is assigned the standard USER role.</p>
        </div>
        <form onSubmit={submit} className="space-y-4 p-8">
          {error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">{error}</div>}
          {message && <div role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs text-emerald-700">{message}</div>}
          <label className="block"><span className="mb-2 block text-xs font-bold text-slate-600">Username</span>
            <input required minLength={3} name="username" autoComplete="username" value={form.username} onChange={update} placeholder="Your name" className="h-11 w-full rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"/>
          </label>
          <label className="block"><span className="mb-2 block text-xs font-bold text-slate-600">Email address</span>
            <input required type="email" name="email" autoComplete="email" value={form.email} onChange={update} placeholder="you@example.com" className="h-11 w-full rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"/>
          </label>
          <label className="block"><span className="mb-2 block text-xs font-bold text-slate-600">Password</span>
            <input required type="password" minLength={8} name="password" autoComplete="new-password" value={form.password} onChange={update} placeholder="At least 8 characters" className="h-11 w-full rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"/>
          </label>
          <button disabled={busy} className="flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-blue-600 text-sm font-bold text-white hover:bg-blue-700 disabled:opacity-60">
            <UserPlus size={16}/>{busy ? "Creating account..." : "Create account"}
          </button>
          <p className="text-center text-xs text-slate-500">Already registered? <Link to="/login" className="font-bold text-blue-700 hover:underline">Sign in</Link></p>
        </form>
      </section>
    </main>
  );
}
