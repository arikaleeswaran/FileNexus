import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { ArrowDownToLine, ArrowLeft } from "lucide-react";
import Topbar from "../components/Topbar";
import Status from "../components/Status";
import { getFile, getValidation, getScans, getAudit, downloadFile, errorMessage } from "../services/fileApi";
import { rows, bytes, date } from "../utils/format";
import { useAuth } from "../context/AuthContext";

function History({ title, data }) {
  return <section className="rounded-2xl border border-slate-200 bg-white p-5"><h3 className="mb-3 text-sm font-extrabold">{title}</h3>{data.length ? data.map((record, index) => <pre key={index} className="mb-2 whitespace-pre-wrap break-words rounded-xl bg-slate-50 p-3 font-sans text-[11px] text-slate-600">{Object.entries(record).map(([key, value]) => `${key}: ${typeof value === "object" ? JSON.stringify(value) : value ?? "—"}`).join("\n")}</pre>) : <p className="text-xs text-slate-400">No records available.</p>}</section>;
}

export default function FileDetailsPage() {
  const { fileId } = useParams();
  const { role } = useAuth();
  const privileged = role === "ADMIN" || role === "SECURITY_ANALYST";
  const [file, setFile] = useState(null);
  const [hist, setHist] = useState({ validation: [], scans: [], audit: [] });
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    let cancelled = false;
    async function load() {
      setFile(null); setError(""); setHist({ validation: [], scans: [], audit: [] });
      try {
        const result = await getFile(fileId);
        if (cancelled) return;
        setFile(result);

        const requests = [
          getValidation(fileId),
          privileged ? getScans(fileId) : Promise.resolve(null),
          privileged ? getAudit(fileId) : Promise.resolve(null)
        ];
        const settled = await Promise.allSettled(requests);
        if (cancelled) return;
        setHist({
          validation: settled[0].status === "fulfilled" ? rows(settled[0].value) : [],
          scans: settled[1].status === "fulfilled" && settled[1].value ? rows(settled[1].value) : [],
          audit: settled[2].status === "fulfilled" && settled[2].value ? rows(settled[2].value) : []
        });
        const validationFailure = settled[0].status === "rejected";
        if (validationFailure) setError(errorMessage(settled[0].reason));
      } catch (e) {
        if (!cancelled) setError(errorMessage(e));
      }
    }
    load();
    return () => { cancelled = true; };
  }, [fileId, privileged]);

  async function handleDownload() {
    setBusy(true); setError("");
    try { await downloadFile(fileId, file?.originalName || "download"); }
    catch (e) { setError(errorMessage(e)); }
    finally { setBusy(false); }
  }

  return <>
    <Topbar title="File details" subtitle={`Document record #${fileId}`}/>
    <div className="space-y-5 p-5 sm:p-8">
      <Link to="/files" className="inline-flex items-center gap-2 text-xs font-bold text-slate-500"><ArrowLeft size={14}/>Back to documents</Link>
      {error && <div role="alert" className="rounded-xl bg-rose-50 p-3 text-xs text-rose-700">{error}</div>}
      {!file ? <div className="rounded-2xl bg-white p-10 text-center text-xs text-slate-400">Loading file details...</div> : <>
        <section className="rounded-2xl border border-slate-200 bg-white p-6">
          <div className="flex flex-wrap items-center justify-between gap-4"><div><h2 className="text-lg font-extrabold">{file.originalName}</h2><p className="mt-1 text-xs text-slate-400">File ID: {file.fileId}</p></div><div className="flex items-center gap-3"><Status value={file.status}/><button disabled={file.status !== "APPROVED" || busy} onClick={handleDownload} className="flex items-center gap-2 rounded-xl bg-blue-600 px-4 py-2.5 text-xs font-bold text-white disabled:bg-slate-300"><ArrowDownToLine size={14}/>{busy ? "Downloading..." : "Download"}</button></div></div>
          <div className="mt-6 grid gap-4 border-t pt-5 sm:grid-cols-2 xl:grid-cols-4">{[["File size", bytes(file.fileSize)], ["MIME type", file.detectedMimeType || file.declaredMimeType || "—"], ["Uploaded", date(file.uploadedAt)], ["SHA-256", file.sha256Checksum || "Not available"]].map(([key, value]) => <div key={key}><p className="text-[10px] font-bold uppercase text-slate-400">{key}</p><p className="mt-1 break-all text-xs font-semibold">{value}</p></div>)}</div>
        </section>
        <div className="grid gap-5 xl:grid-cols-2">
          <History title="Validation history" data={hist.validation}/>
          {privileged && <History title="Scan history" data={hist.scans}/>}
          {privileged && <div className="xl:col-span-2"><History title="Audit history" data={hist.audit}/></div>}
        </div>
        {!privileged && <p className="text-xs text-slate-400">Detailed scan and audit records are restricted to security analysts and administrators.</p>}
      </>}
    </div>
  </>;
}
