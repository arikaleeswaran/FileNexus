import { useEffect, useState } from "react";
import { AlertTriangle, CheckCircle2, RefreshCw, ShieldCheck, Activity, ScanLine, ClipboardList } from "lucide-react";
import { Link } from "react-router-dom";
import Topbar from "../components/Topbar";
import Status from "../components/Status";
import { listFiles, getScans, getAudit, errorMessage } from "../services/fileApi";
import { rows, date } from "../utils/format";

function Metric({ label, value, Icon, tone }) {
  return <div className="rounded-2xl border border-slate-200 bg-white p-5"><span className={`mb-4 inline-flex rounded-xl p-3 ${tone}`}><Icon size={19}/></span><p className="text-xs text-slate-500">{label}</p><p className="mt-1 text-3xl font-extrabold">{value}</p></div>;
}

export default function SecurityPage() {
  const [files, setFiles] = useState([]);
  const [scanCount, setScanCount] = useState(0);
  const [auditCount, setAuditCount] = useState(0);
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(true);

  async function refresh() {
    setLoading(true); setError("");
    try {
      const allFiles = rows(await listFiles({ page: 0, size: 100 }));
      setFiles(allFiles);
      const ids = allFiles.map(f => f.fileId ?? f.id).filter(Boolean);
      const [scans, audits] = await Promise.all([
        Promise.allSettled(ids.map(id => getScans(id))),
        Promise.allSettled(ids.map(id => getAudit(id)))
      ]);
      setScanCount(scans.reduce((sum, r) => sum + (r.status === "fulfilled" ? rows(r.value).length : 0), 0));
      setAuditCount(audits.reduce((sum, r) => sum + (r.status === "fulfilled" ? rows(r.value).length : 0), 0));
    } catch (e) {
      setError(errorMessage(e));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => { refresh(); }, []);

  const approved = files.filter(f => f.status === "APPROVED").length;
  const pending = files.filter(f => ["UPLOADED", "VALIDATING", "VALIDATED", "QUARANTINED", "SCANNING"].includes(f.status)).length;
  const rejected = files.filter(f => ["REJECTED", "VALIDATION_FAILED", "SCAN_FAILED", "SCAN_TIMEOUT"].includes(f.status)).length;

  return <>
    <Topbar title="Security operations" subtitle="Monitor malware scanning, validation, and audit activity"/>
    <div className="space-y-5 p-5 sm:p-8">
      <div className="flex flex-wrap items-center justify-between gap-3"><div><h2 className="text-lg font-extrabold text-[#172640]">Security monitoring</h2><p className="mt-1 text-xs text-slate-500">Cross-user security visibility for investigation.</p></div><button onClick={refresh} className="inline-flex items-center gap-2 rounded-xl border bg-white px-3.5 py-2.5 text-xs font-bold text-slate-600"><RefreshCw size={14}/>Refresh</button></div>
      {error && <div className="rounded-xl bg-rose-50 p-3 text-xs text-rose-700">{error}</div>}
      <div className="rounded-2xl bg-[#10264b] p-6 text-white"><div className="flex items-center gap-3"><ShieldCheck size={27} className="text-blue-200"/><div><p className="text-[10px] font-bold uppercase tracking-widest text-blue-200">Security operations</p><h2 className="mt-1 text-xl font-extrabold">Quarantine first. Approve after scanning.</h2></div></div><p className="mt-4 max-w-3xl text-xs leading-6 text-blue-100/75">Investigate scan outcomes and access events. Scanner failures and timeouts are operational issues and must not be treated as malware detections.</p></div>
      <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-5">
        <Metric label="Total files" value={loading ? "…" : files.length} Icon={ScanLine} tone="bg-blue-50 text-blue-700"/>
        <Metric label="Approved" value={loading ? "…" : approved} Icon={CheckCircle2} tone="bg-emerald-50 text-emerald-700"/>
        <Metric label="Pending" value={loading ? "…" : pending} Icon={Activity} tone="bg-indigo-50 text-indigo-700"/>
        <Metric label="Rejected / failed" value={loading ? "…" : rejected} Icon={AlertTriangle} tone="bg-rose-50 text-rose-700"/>
        <Metric label="Scan / audit events" value={loading ? "…" : `${scanCount} / ${auditCount}`} Icon={ClipboardList} tone="bg-violet-50 text-violet-700"/>
      </div>
      <section className="overflow-hidden rounded-2xl border border-slate-200 bg-white">
        <div className="border-b border-slate-100 p-5"><h3 className="text-sm font-extrabold">Files requiring security attention</h3><p className="mt-1 text-xs text-slate-500">Rejected, failed, or still processing.</p></div>
        <div className="divide-y divide-slate-100">
          {files.filter(f => ["REJECTED", "VALIDATION_FAILED", "SCAN_FAILED", "SCAN_TIMEOUT", "SCANNING", "QUARANTINED"].includes(f.status)).map(file => (
            <Link key={file.fileId ?? file.id} to={`/files/${file.fileId ?? file.id}`} className="flex flex-wrap items-center justify-between gap-3 p-4 hover:bg-slate-50">
              <div><p className="text-xs font-bold text-slate-800">{file.originalName}</p><p className="mt-1 text-[10px] text-slate-400">File #{file.fileId ?? file.id} · {date(file.uploadedAt)}</p></div><Status value={file.status}/>
            </Link>
          ))}
          {!loading && !files.some(f => ["REJECTED", "VALIDATION_FAILED", "SCAN_FAILED", "SCAN_TIMEOUT", "SCANNING", "QUARANTINED"].includes(f.status)) && <p className="p-6 text-center text-xs text-slate-400">No files currently require attention.</p>}
        </div>
      </section>
    </div>
  </>;
}
