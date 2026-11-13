import { useEffect, useMemo, useState } from "react";
import { Activity, ArrowUpRight, CheckCircle2, Files, ScanLine, ShieldAlert, RefreshCw, ShieldCheck, Users } from "lucide-react";
import { Link } from "react-router-dom";
import Topbar from "../components/Topbar";
import FileTable from "../components/FileTable";
import UploadPanel from "../components/UploadPanel";
import { listFiles, createMetadata, uploadFile, downloadFile, errorMessage } from "../services/fileApi";
import { rows } from "../utils/format";
import { useAuth } from "../context/AuthContext";

function Metric({ label, value, note, Icon, tone }) {
  return <div className="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
    <div className="flex justify-between"><div><p className="text-[11px] font-semibold text-slate-400">{label}</p><p className="mt-2 text-3xl font-extrabold text-[#15233b]">{value}</p></div><span className={`rounded-xl p-3 ${tone}`}><Icon size={19}/></span></div>
    <p className="mt-3 text-[10px] text-slate-400">{note}</p>
  </div>;
}

export default function Dashboard() {
  const { role, user } = useAuth();
  const isUser = role === "USER";
  const isAnalyst = role === "SECURITY_ANALYST";
  const [files, setFiles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [downloading, setDownloading] = useState(null);

  async function refresh() {
    setLoading(true); setError("");
    try { setFiles(rows(await listFiles({ page: 0, size: 100 }))); }
    catch (e) { setError(errorMessage(e)); }
    finally { setLoading(false); }
  }

  useEffect(() => { refresh(); }, []);

  const visible = useMemo(
    () => files.filter(f => (f.originalName || "").toLowerCase().includes(search.toLowerCase())),
    [files, search]
  );
  const count = (...statuses) => files.filter(f => statuses.includes(f.status)).length;

  async function upload(file, progress) {
    try {
      const meta = await createMetadata(file);
      const id = meta?.fileId ?? meta?.id;
      if (!id) throw new Error("Metadata API response did not contain fileId.");
      await uploadFile(id, file, progress);
      await refresh();
    } catch (e) { throw new Error(errorMessage(e)); }
  }

  async function download(file) {
    const id = file.fileId ?? file.id;
    setDownloading(id);
    try { await downloadFile(id, file.originalName); }
    catch (e) { setError(errorMessage(e)); }
    finally { setDownloading(null); }
  }

  const title = isUser ? "My overview" : isAnalyst ? "Security operations" : "System overview";
  const subtitle = isUser
    ? "Manage your uploads and protected documents"
    : isAnalyst
      ? "Monitor file scanning, validation outcomes, and security activity"
      : "Monitor FileNexus system-wide document activity";

  return <>
    <Topbar title={title} subtitle={subtitle} search={search} onSearch={setSearch}/>
    <div className="space-y-6 p-5 sm:p-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-lg font-extrabold text-[#172640]">
            {isUser ? `Welcome${user?.username ? `, ${user.username}` : ""}` : isAnalyst ? "Security monitoring overview" : "Application administration overview"}
          </h2>
          <p className="mt-1 text-xs text-slate-500">
            {isUser ? "Track your documents from upload through secure approval." : "Review workflow health and file security outcomes."}
          </p>
        </div>
        <button onClick={refresh} className="inline-flex items-center gap-2 rounded-xl border border-slate-200 bg-white px-3.5 py-2.5 text-xs font-bold text-slate-600"><RefreshCw size={14}/>Refresh data</button>
      </div>

      {error && <div role="alert" className="rounded-xl border border-rose-200 bg-rose-50 p-3 text-xs text-rose-700">{error}</div>}

      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <Metric label={isUser ? "My documents" : "Total documents"} value={loading ? "…" : files.length} note={isUser ? "Your uploaded files" : "Documents visible to your role"} Icon={Files} tone="bg-blue-50 text-blue-700"/>
        <Metric label="Approved files" value={loading ? "…" : count("APPROVED")} note="Available for secure download" Icon={CheckCircle2} tone="bg-emerald-50 text-emerald-700"/>
        <Metric label="In scanning / quarantine" value={loading ? "…" : count("SCANNING", "QUARANTINED", "UPLOADED", "VALIDATING", "VALIDATED")} note="Awaiting final scan outcome" Icon={ScanLine} tone="bg-indigo-50 text-indigo-700"/>
        <Metric label="Rejected / failed" value={loading ? "…" : count("REJECTED", "SCAN_FAILED", "SCAN_TIMEOUT", "VALIDATION_FAILED")} note="Not available for download" Icon={ShieldAlert} tone="bg-rose-50 text-rose-700"/>
      </div>

      {(isAnalyst || role === "ADMIN") && (
        <div className="rounded-2xl bg-[#10264b] p-5 text-white">
          <div className="flex gap-4"><ShieldCheck className="text-blue-200"/><div><h3 className="text-sm font-extrabold">{isAnalyst ? "Security investigation workspace" : "System-wide security oversight"}</h3><p className="mt-1 max-w-3xl text-xs leading-5 text-blue-100/75">{isAnalyst ? "Review rejected files, validation failures, scan outcomes, and audit activity. Investigate security events without changing user account privileges." : "Oversee document processing, user access, security controls, and operational configuration."}</p><Link to={isAnalyst ? "/security" : "/admin"} className="mt-3 inline-flex items-center gap-1 text-xs font-bold text-white underline">{isAnalyst ? "Open security operations" : "Open administration"} <ArrowUpRight size={13}/></Link></div></div>
        </div>
      )}

      <div className={`grid gap-6 ${isUser ? "xl:grid-cols-[minmax(0,1.6fr)_minmax(300px,.85fr)]" : ""}`}>
        <section>
          <div className="mb-4 flex items-center justify-between"><div><h3 className="text-sm font-extrabold text-[#172640]">{isUser ? "My recent documents" : "Recent system documents"}</h3><p className="mt-1 text-[11px] text-slate-400">{isUser ? "Your latest uploads" : "Latest files visible to your role"}</p></div><Link to="/files" className="flex items-center gap-1 text-xs font-bold text-blue-700">View all <ArrowUpRight size={14}/></Link></div>
          {loading ? <div className="rounded-2xl bg-white p-10 text-center text-xs text-slate-400">Loading files...</div> : <FileTable files={visible.slice(0, 7)} onDownload={download} downloading={downloading}/>}
        </section>
        {isUser && <UploadPanel onUpload={upload}/>}
      </div>

      {isUser && <div className="rounded-2xl bg-[#10264b] p-5 text-white"><div className="flex gap-4"><Activity className="text-blue-200"/><div><h3 className="text-sm font-extrabold">Security-first file handling</h3><p className="mt-1 max-w-2xl text-xs leading-5 text-blue-100/75">Your uploads enter quarantine, pass file validation, and are scanned for malware. Only approved files become downloadable.</p></div></div></div>}
    </div>
  </>;
}
