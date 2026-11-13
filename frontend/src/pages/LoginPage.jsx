import { useState } from "react";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { LockKeyhole, ShieldCheck } from "lucide-react";
import { useAuth } from "../context/AuthContext";
import { getApiError } from "../services/apiClient";

export default function LoginPage() {
  const { signIn } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [form, setForm] = useState({ email: "", password: "" });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  function update(event) {
    setForm((current) => ({ ...current, [event.target.name]: event.target.value }));
  }

  async function submit(event) {
    event.preventDefault();
    setError("");
    setBusy(true);
    try {
      await signIn(form);
      navigate(location.state?.from?.pathname || "/", { replace: true });
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
          <p className="text-[10px] font-bold uppercase tracking-[.22em] text-blue-300">Secure workspace</p>
          <h1 className="mt-2 text-2xl font-extrabold">Welcome back</h1>
          <p className="mt-2 text-xs text-slate-300">Sign in to access your protected documents.</p>
        </div>
        <form onSubmit={submit} className="space-y-5 p-8">
          {error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">{error}</div>}
          <label className="block"><span className="mb-2 block text-xs font-bold text-slate-600">Email address</span>
            <input required type="email" name="email" autoComplete="email" value={form.email} onChange={update} placeholder="you@example.com" className="h-11 w-full rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"/>
          </label>
          <label className="block"><span className="mb-2 block text-xs font-bold text-slate-600">Password</span>
            <input required type="password" name="password" autoComplete="current-password" value={form.password} onChange={update} placeholder="Enter your password" className="h-11 w-full rounded-xl border border-slate-200 px-3 text-sm outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"/>
          </label>
          <button disabled={busy} className="flex h-11 w-full items-center justify-center gap-2 rounded-xl bg-blue-600 text-sm font-bold text-white hover:bg-blue-700 disabled:opacity-60">
            <LockKeyhole size={16}/>{busy ? "Signing in..." : "Sign in"}
          </button>
          <p className="text-center text-xs text-slate-500">New to FileNexus? <Link to="/register" className="font-bold text-blue-700 hover:underline">Create an account</Link></p>
        </form>
      </section>
    </main>
  );
}
