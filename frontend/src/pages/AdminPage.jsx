import { useEffect, useMemo, useState } from "react";
import { Activity, Database, Files, RefreshCw, ShieldCheck, Users, Search, UserCheck, UserX } from "lucide-react";
import Topbar from "../components/Topbar";
import { listFiles, errorMessage } from "../services/fileApi";
import { rows } from "../utils/format";
import { getAdminUsers, updateAdminUserRole, updateAdminUserStatus } from "../services/adminUserApi";

const ROLES = ["USER", "SECURITY_ANALYST", "ADMIN"];
function Stat({ label, value, Icon, tone }) {
  return <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm"><span className={`mb-4 inline-flex rounded-xl p-3 ${tone}`}><Icon size={19}/></span><p className="text-xs text-slate-500">{label}</p><p className="mt-1 text-3xl font-extrabold text-[#15233b]">{value}</p></div>;
}
function formatDate(value) {
  if (!value) return "—";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "—" : date.toLocaleDateString();
}

export default function AdminPage() {
  const [files, setFiles] = useState([]);
  const [users, setUsers] = useState([]);
  const [search, setSearch] = useState("");
  const [roleFilter, setRoleFilter] = useState("ALL");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [loading, setLoading] = useState(true);
  const [busyUserId, setBusyUserId] = useState(null);

  async function refresh() {
    setLoading(true); setError(""); setNotice("");
    const results = await Promise.allSettled([
      listFiles({ page: 0, size: 100 }),
      getAdminUsers()
    ]);
    if (results[0].status === "fulfilled") setFiles(rows(results[0].value));
    else setError(`Documents: ${errorMessage(results[0].reason)}`);
    if (results[1].status === "fulfilled") setUsers(rows(results[1].value));
    else setError(previous => [previous, `Users: ${errorMessage(results[1].reason)}`].filter(Boolean).join(" · "));
    setLoading(false);
  }

  useEffect(() => { refresh(); }, []);

  const filteredUsers = useMemo(() => users.filter(user => {
    const matchesSearch = `${user.username || ""} ${user.email || ""}`.toLowerCase().includes(search.toLowerCase());
    const matchesRole = roleFilter === "ALL" || user.role === roleFilter;
    const matchesStatus = statusFilter === "ALL" || (statusFilter === "ACTIVE" ? user.active : !user.active);
    return matchesSearch && matchesRole && matchesStatus;
  }), [users, search, roleFilter, statusFilter]);

  const count = (...statuses) => files.filter(file => statuses.includes(file.status)).length;

  async function changeStatus(user, active) {
    setBusyUserId(user.userId); setError(""); setNotice("");
    try {
      const updated = await updateAdminUserStatus(user.userId, active);
      setUsers(current => current.map(item => item.userId === user.userId ? updated : item));
      setNotice(`${user.email} ${active ? "activated" : "deactivated"} successfully.`);
    } catch (e) { setError(errorMessage(e)); }
    finally { setBusyUserId(null); }
  }

  async function changeRole(user, role) {
    if (role === user.role) return;
    setBusyUserId(user.userId); setError(""); setNotice("");
    try {
      const updated = await updateAdminUserRole(user.userId, role);
      setUsers(current => current.map(item => item.userId === user.userId ? updated : item));
      setNotice(`${user.email}'s role updated to ${role}.`);
    } catch (e) { setError(errorMessage(e)); }
    finally { setBusyUserId(null); }
  }

  return <>
    <Topbar title="Administration" subtitle="System-wide overview and administrative controls"/>
    <div className="space-y-6 p-5 sm:p-8">
      <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-lg font-extrabold text-[#172640]">Administrator console</h2><p className="mt-1 text-xs text-slate-500">Manage users, roles, account status, and document workflow.</p></div><button onClick={refresh} disabled={loading} className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-xs font-bold text-slate-600 disabled:opacity-50"><RefreshCw size={14} className={loading ? "animate-spin" : ""}/>Refresh</button></div>
      {error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">{error}</div>}
      {notice && <div role="status" className="rounded-xl border border-emerald-200 bg-emerald-50 p-3 text-xs text-emerald-700">{notice}</div>}
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Stat label="Documents in system" value={loading ? "…" : files.length} Icon={Files} tone="bg-blue-50 text-blue-700"/>
        <Stat label="Approved" value={loading ? "…" : count("APPROVED")} Icon={ShieldCheck} tone="bg-emerald-50 text-emerald-700"/>
        <Stat label="Needs review" value={loading ? "…" : count("REJECTED", "VALIDATION_FAILED", "SCAN_FAILED", "SCAN_TIMEOUT")} Icon={Activity} tone="bg-rose-50 text-rose-700"/>
        <Stat label="Registered users" value={loading ? "…" : users.length} Icon={Users} tone="bg-indigo-50 text-indigo-700"/>
      </div>

      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <div className="border-b border-slate-100 p-5"><div className="flex flex-wrap items-center justify-between gap-3"><div><h3 className="text-sm font-extrabold">User administration</h3><p className="mt-1 text-xs text-slate-500">View accounts, activate or deactivate users, and assign application roles.</p></div><span className="rounded-full bg-blue-50 px-3 py-1 text-[10px] font-bold text-blue-700">ADMIN ONLY</span></div>
          <div className="mt-4 grid gap-2 md:grid-cols-[1fr_180px_160px]"><label className="flex items-center gap-2 rounded-xl border border-slate-200 px-3"><Search size={15} className="text-slate-400"/><input value={search} onChange={e => setSearch(e.target.value)} placeholder="Search username or email" className="w-full py-2.5 text-xs outline-none"/></label><select value={roleFilter} onChange={e => setRoleFilter(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-2.5 text-xs"><option value="ALL">All roles</option>{ROLES.map(role => <option key={role}>{role}</option>)}</select><select value={statusFilter} onChange={e => setStatusFilter(e.target.value)} className="rounded-xl border border-slate-200 px-3 py-2.5 text-xs"><option value="ALL">All statuses</option><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select></div>
        </div>
        <div className="overflow-x-auto"><table className="w-full min-w-[850px] text-left"><thead className="bg-slate-50 text-[10px] uppercase tracking-wider text-slate-500"><tr><th className="px-5 py-3">User</th><th className="px-5 py-3">Registered</th><th className="px-5 py-3">Role</th><th className="px-5 py-3">Status</th><th className="px-5 py-3">Actions</th></tr></thead><tbody className="divide-y divide-slate-100">{filteredUsers.map(user => <tr key={user.userId} className="text-xs"><td className="px-5 py-4"><p className="font-bold text-slate-800">{user.username}</p><p className="mt-1 text-[10px] text-slate-400">{user.email} · ID {user.userId}</p></td><td className="px-5 py-4 text-slate-500">{formatDate(user.createdAt)}</td><td className="px-5 py-4"><select aria-label={`Role for ${user.email}`} disabled={busyUserId === user.userId} value={user.role} onChange={e => changeRole(user, e.target.value)} className="rounded-lg border border-slate-200 bg-white px-2 py-2 text-[11px] font-semibold disabled:opacity-50">{ROLES.map(role => <option key={role} value={role}>{role}</option>)}</select></td><td className="px-5 py-4"><span className={`rounded-full px-2.5 py-1 text-[10px] font-bold ${user.active ? "bg-emerald-50 text-emerald-700" : "bg-rose-50 text-rose-700"}`}>{user.active ? "ACTIVE" : "INACTIVE"}</span></td><td className="px-5 py-4"><button disabled={busyUserId === user.userId} onClick={() => changeStatus(user, !user.active)} className={`inline-flex items-center gap-1.5 rounded-lg px-3 py-2 text-[10px] font-bold disabled:opacity-50 ${user.active ? "bg-rose-50 text-rose-700 hover:bg-rose-100" : "bg-emerald-50 text-emerald-700 hover:bg-emerald-100"}`}>{user.active ? <><UserX size={13}/>Deactivate</> : <><UserCheck size={13}/>Activate</>}</button></td></tr>)}{!loading && filteredUsers.length === 0 && <tr><td colSpan="5" className="px-5 py-10 text-center text-xs text-slate-400">No users match these filters.</td></tr>}</tbody></table></div>
        <div className="border-t border-slate-100 px-5 py-3 text-[10px] text-slate-400">Showing {filteredUsers.length} of {users.length} registered users. Password hashes are never returned.</div>
      </section>
      <section className="rounded-2xl border border-slate-200 bg-white p-5"><h3 className="text-sm font-extrabold">Role responsibilities</h3><div className="mt-3 grid gap-3 text-xs text-slate-600 md:grid-cols-3"><div><b className="text-slate-800">USER</b><p className="mt-1">Own documents, upload, validation history, and approved downloads.</p></div><div><b className="text-slate-800">SECURITY_ANALYST</b><p className="mt-1">Cross-user security monitoring, scan history, and audit investigation.</p></div><div><b className="text-slate-800">ADMIN</b><p className="mt-1">User lifecycle, role assignment, and system-wide administration.</p></div></div></section>
    </div>
  </>;
}
