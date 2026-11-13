import { useState } from "react";
import { Link, Navigate, Route, Routes, useLocation } from "react-router-dom";
import { Menu, X } from "lucide-react";
import Sidebar from "./components/Sidebar";
import ProtectedRoute from "./components/ProtectedRoute";
import Dashboard from "./pages/Dashboard";
import FilesPage from "./pages/FilesPage";
import UploadPage from "./pages/UploadPage";
import FileDetailsPage from "./pages/FileDetailsPage";
import SecurityPage from "./pages/SecurityPage";
import AdminPage from "./pages/AdminPage";
import LoginPage from "./pages/LoginPage";
import RegisterPage from "./pages/RegisterPage";
import { useAuth } from "./context/AuthContext";

function WorkspaceLayout() {
  const [open, setOpen] = useState(false);
  const location = useLocation();
  const { role, signOut } = useAuth();

  const links = {
    USER: [["/", "My overview"], ["/files", "My documents"], ["/upload", "Upload"]],
    SECURITY_ANALYST: [["/", "Operations overview"], ["/files", "All documents"], ["/security", "Security operations"]],
    ADMIN: [["/", "System overview"], ["/files", "All documents"], ["/security", "Security center"], ["/admin", "Administration"]]
  }[role] || [["/", "My overview"], ["/files", "My documents"], ["/upload", "Upload"]];

  return (
    <div className="min-h-screen bg-[#f3f6fb]">
      <Sidebar/>
      <div className="min-h-screen lg:pl-[250px]">
        <div className="lg:hidden">
          <div className="flex items-center justify-between bg-[#0b1730] px-5 py-4 text-white">
            <b>FileNexus</b>
            <button aria-label="Toggle navigation" onClick={() => setOpen(v => !v)}>{open ? <X/> : <Menu/>}</button>
          </div>
          {open && <nav className="flex flex-wrap gap-2 bg-[#0b1730] px-4 pb-4 text-xs text-white">
            {links.map(([to, label]) => <Link key={to} to={to} onClick={() => setOpen(false)} className={`rounded-lg px-3 py-2 ${location.pathname === to ? "bg-blue-600" : "bg-white/10"}`}>{label}</Link>)}
            <button onClick={signOut} className="rounded-lg bg-rose-500/20 px-3 py-2">Sign out</button>
          </nav>}
        </div>
        <main className="mx-auto min-h-screen max-w-[1600px]">
          <Routes>
            <Route path="/" element={<Dashboard/>}/>
            <Route path="/files" element={<FilesPage/>}/>
            <Route path="/files/:fileId" element={<FileDetailsPage/>}/>
            <Route element={<ProtectedRoute allowedRoles={["USER", "ADMIN"]}/>}>
              <Route path="/upload" element={<UploadPage/>}/>
            </Route>
            <Route element={<ProtectedRoute allowedRoles={["ADMIN", "SECURITY_ANALYST"]}/>}>
              <Route path="/security" element={<SecurityPage/>}/>
            </Route>
            <Route element={<ProtectedRoute allowedRoles={["ADMIN"]}/>}>
              <Route path="/admin" element={<AdminPage/>}/>
            </Route>
            <Route path="*" element={<Navigate to="/" replace/>}/>
          </Routes>
        </main>
      </div>
    </div>
  );
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage/>}/>
      <Route path="/register" element={<RegisterPage/>}/>
      <Route element={<ProtectedRoute/>}>
        <Route path="/*" element={<WorkspaceLayout/>}/>
      </Route>
    </Routes>
  );
}
