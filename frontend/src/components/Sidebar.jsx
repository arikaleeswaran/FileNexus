import { NavLink } from "react-router-dom";
import {
  LayoutDashboard, Files, CloudUpload, ShieldCheck, Shield,
  Settings, LogOut, UserRound, Users
} from "lucide-react";
import { useAuth } from "../context/AuthContext";

const navigation = {
  USER: [
    ["/", "My overview", LayoutDashboard],
    ["/files", "My documents", Files],
    ["/upload", "Upload files", CloudUpload]
  ],
  SECURITY_ANALYST: [
    ["/", "Operations overview", LayoutDashboard],
    ["/files", "All documents", Files],
    ["/security", "Security operations", ShieldCheck]
  ],
  ADMIN: [
    ["/", "System overview", LayoutDashboard],
    ["/files", "All documents", Files],
    ["/security", "Security center", ShieldCheck],
    ["/admin", "Administration", Users]
  ]
};

export default function Sidebar() {
  const { user, role, signOut } = useAuth();
  const links = navigation[role] || navigation.USER;

  return (
    <aside className="fixed inset-y-0 left-0 z-30 hidden w-[250px] flex-col bg-[#0b1730] text-slate-300 lg:flex">
      <div className="flex h-[88px] items-center gap-3 border-b border-white/10 px-7">
        <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-blue-500 text-white"><Shield size={21}/></div>
        <div><div className="text-[19px] font-extrabold text-white">FileNexus</div><div className="text-[9px] font-semibold tracking-[.22em] text-blue-300">SECURE WORKSPACE</div></div>
      </div>

      <div className="px-5 pt-8">
        <p className="mb-3 px-3 text-[10px] font-bold uppercase tracking-[.2em] text-slate-500">
          {role === "USER" ? "My workspace" : role === "SECURITY_ANALYST" ? "Security operations" : "Administration"}
        </p>
        <nav className="space-y-1.5">
          {links.map(([to, label, Icon]) => (
            <NavLink key={to} to={to} end={to === "/"}
              className={({ isActive }) => `flex items-center gap-3 rounded-xl px-3.5 py-3 text-[13px] font-semibold transition ${isActive ? "bg-blue-600 text-white" : "text-slate-400 hover:bg-white/5 hover:text-white"}`}>
              <Icon size={18}/>{label}
            </NavLink>
          ))}
        </nav>
      </div>

      <div className="mt-auto p-5">
        <div className="mb-4 rounded-2xl border border-white/10 bg-white/[.04] p-4">
          <div className="mb-2 flex items-center gap-2 text-xs font-semibold text-white"><ShieldCheck size={15} className="text-emerald-400"/>Protected storage</div>
          <p className="text-[11px] leading-5 text-slate-400">Files are validated and scanned before approval and secure download.</p>
        </div>
        <div className="mb-3 flex items-center gap-2 border-t border-white/10 pt-4">
          <span className="flex h-8 w-8 items-center justify-center rounded-full bg-white/10"><UserRound size={15}/></span>
          <div className="min-w-0 flex-1"><p className="truncate text-[11px] font-bold text-white">{user?.username || user?.email || "Signed in"}</p><p className="text-[9px] uppercase tracking-wider text-blue-300">{role || "USER"}</p></div>
        </div>
        <button onClick={signOut} className="flex w-full items-center gap-2 rounded-xl px-3 py-2 text-xs font-semibold text-slate-400 hover:bg-rose-500/10 hover:text-rose-300"><LogOut size={15}/>Sign out</button>
        <div className="mt-3 flex justify-between border-t border-white/10 pt-3 text-[10px] text-slate-500"><span>FileNexus · Secure workspace</span><Settings size={14}/></div>
      </div>
    </aside>
  );
}
