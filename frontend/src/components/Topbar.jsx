import { Search, ShieldCheck, LogOut } from "lucide-react";
import { useAuth } from "../context/AuthContext";

export default function Topbar({ title, subtitle, search, onSearch }) {
  const { user, signOut } = useAuth();
  return (
    <header className="sticky top-0 z-20 flex min-h-[82px] items-center justify-between border-b border-slate-200/80 bg-white/90 px-5 backdrop-blur-xl sm:px-8">
      <div><div className="text-[10px] font-bold uppercase tracking-[.2em] text-blue-600">FileNexus workspace</div><h1 className="mt-1 text-xl font-extrabold tracking-tight text-[#15233b] sm:text-2xl">{title}</h1>{subtitle && <p className="mt-0.5 hidden text-xs text-slate-500 sm:block">{subtitle}</p>}</div>
      <div className="flex items-center gap-2 sm:gap-4">
        {onSearch && <label className="hidden h-10 w-56 items-center gap-2 rounded-xl border border-slate-200 bg-slate-50 px-3 md:flex"><Search size={15} className="text-slate-400"/><input value={search} onChange={e => onSearch(e.target.value)} placeholder="Search documents..." className="w-full bg-transparent text-xs outline-none"/></label>}
        <div className="hidden text-right sm:block"><p className="max-w-40 truncate text-[11px] font-bold text-slate-700">{user?.username || user?.email}</p><p className="text-[9px] text-slate-400">Authenticated session</p></div>
        <div className="flex h-9 w-9 items-center justify-center rounded-full bg-[#e6edff] text-blue-700"><ShieldCheck size={17}/></div>
        <button onClick={signOut} title="Sign out" className="rounded-xl p-2.5 text-slate-500 hover:bg-rose-50 hover:text-rose-600"><LogOut size={17}/></button>
      </div>
    </header>
  );
}
