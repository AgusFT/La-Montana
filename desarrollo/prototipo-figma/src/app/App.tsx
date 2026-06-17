import { useState, useRef } from "react";
import {
  Mail, Lock, Eye, EyeOff, Shield, FolderOpen, Clock,
  Home, ShoppingBag, Phone, ChevronRight, ChevronDown,
  MapPin, HelpCircle, User, LogOut, Package, FileText,
  Upload, Plus, Minus, Info, CheckSquare, Square
} from "lucide-react";
import { ImageWithFallback } from "@/app/components/figma/ImageWithFallback";
import logoImg from "@/imports/logo.jpg";

/* ─── Types ─── */
type View = "login" | "dashboard" | "cotizar" | "resumen";

/* ─── Data ─── */
const PEDIDOS = [
  { id: "PED-2026-003", estado: "ENTREGADO", fecha: "01/05/2026", total: "$16,000.00" },
  { id: "PED-2026-002", estado: "CANCELADO",  fecha: "15/04/2026", total: "$8,500.00" },
  { id: "PED-2026-001", estado: "PENDIENTE",  fecha: "01/04/2026", total: "$3,900.00" },
];

const ESTADO_STYLE: Record<string, string> = {
  ENTREGADO: "bg-green-100 text-green-700",
  CANCELADO:  "bg-red-100 text-red-600",
  PENDIENTE:  "bg-yellow-100 text-yellow-700",
};

const STEPS = [
  "Pedido recibido", "Archivos cargados", "Revisión administrativa",
  "Estimación", "Producción", "Control de calidad", "Listo para entrega", "Entregado",
];
const CURRENT_STEP = 2;

/* ─── Mountain SVG ─── */
function MountainBg({ className = "" }: { className?: string }) {
  return (
    <svg viewBox="0 0 1440 600" preserveAspectRatio="xMidYMid slice"
      className={`w-full h-full ${className}`} xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="skyG" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="#d8e4f0" />
          <stop offset="100%" stopColor="#b8cfe0" />
        </linearGradient>
      </defs>
      <rect width="1440" height="600" fill="url(#skyG)" />
      <polygon points="0,420 180,180 360,340 540,200 720,320 900,160 1080,300 1260,200 1440,320 1440,600 0,600" fill="#c5d8e5" opacity="0.4" />
      <polygon points="0,480 200,260 400,380 600,240 800,360 1000,220 1200,360 1440,280 1440,600 0,600" fill="#b8cdd8" opacity="0.5" />
      <polygon points="0,520 150,320 320,440 520,280 700,420 880,300 1060,420 1240,320 1440,400 1440,600 0,600" fill="#9fb5c9" opacity="0.6" />
    </svg>
  );
}

/* ─── WhatsApp FAB ─── */
function WhatsAppFAB() {
  return (
    <a href="https://wa.me/" target="_blank" rel="noopener noreferrer"
      className="fixed bottom-6 right-6 w-14 h-14 rounded-full flex items-center justify-center shadow-lg hover:scale-105 transition-transform z-50"
      style={{ backgroundColor: "#25d366" }} aria-label="WhatsApp">
      <svg viewBox="0 0 32 32" fill="white" className="w-8 h-8">
        <path d="M16 2C8.268 2 2 8.268 2 16c0 2.492.658 4.833 1.807 6.857L2 30l7.352-1.781A13.94 13.94 0 0016 30c7.732 0 14-6.268 14-14S23.732 2 16 2zm0 25.4a11.37 11.37 0 01-5.8-1.594l-.416-.247-4.36 1.056 1.1-4.241-.27-.437A11.4 11.4 0 014.6 16C4.6 9.7 9.7 4.6 16 4.6S27.4 9.7 27.4 16 22.3 27.4 16 27.4zm6.27-8.493c-.344-.172-2.036-1.004-2.352-1.119-.316-.115-.546-.172-.776.172-.23.344-.892 1.119-1.093 1.35-.2.23-.401.258-.745.086-.344-.172-1.452-.535-2.766-1.708-1.022-.912-1.713-2.038-1.913-2.382-.2-.344-.021-.53.15-.701.155-.154.344-.402.516-.603.172-.2.23-.344.344-.573.115-.23.058-.43-.029-.602-.086-.172-.776-1.873-1.063-2.564-.28-.672-.565-.58-.776-.591l-.66-.011c-.23 0-.602.086-.917.43-.316.344-1.207 1.18-1.207 2.878s1.236 3.338 1.408 3.568c.172.23 2.43 3.714 5.888 5.21.823.355 1.465.567 1.966.726.826.263 1.578.226 2.173.137.663-.1 2.036-.832 2.323-1.636.287-.804.287-1.493.201-1.636-.086-.143-.316-.23-.66-.402z" />
      </svg>
    </a>
  );
}

/* ─── Shared Dashboard Shell ─── */
interface ShellProps {
  activeNav: string;
  onNavChange: (id: string) => void;
  onLogout: () => void;
  children: React.ReactNode;
}

function DashboardShell({ activeNav, onNavChange, onLogout, children }: ShellProps) {
  const [showUserMenu, setShowUserMenu] = useState(false);

  const navItems = [
    { id: "inicio",    label: "Inicio",           icon: <Home size={15} /> },
    { id: "pedidos",   label: "Mis Pedidos",       icon: <ShoppingBag size={15} /> },
    { id: "cotizar",   label: "Crear Pedido",      icon: <Package size={15} /> },
    { id: "entrega",   label: "Puntos de Entrega", icon: <MapPin size={15} /> },
    { id: "contacto",  label: "Contacto",          icon: <Phone size={15} /> },
  ];

  return (
    <div className="min-h-screen flex" style={{ fontFamily: "'Inter', sans-serif", backgroundColor: "#f0f4f8" }}>
      {/* Sidebar */}
      <aside className="w-52 shrink-0 flex flex-col" style={{ backgroundColor: "#1e2d3d" }}>
        <div className="px-4 py-5 flex items-center gap-3 border-b border-white/10">
          <ImageWithFallback src={logoImg} alt="La Montaña" className="h-10 w-10 object-contain rounded-full shrink-0" />
          <div className="leading-tight">
            <div className="text-white text-xs font-semibold" style={{ fontFamily: "'Playfair Display', serif" }}>La Montaña</div>
            <div className="text-[9px] tracking-widest text-blue-300 uppercase">impresiones</div>
          </div>
        </div>
        <nav className="flex-1 py-4 space-y-1 px-2">
          {navItems.map(({ id, label, icon }) => (
            <button key={id} onClick={() => onNavChange(id)}
              className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-sm font-medium transition-colors ${
                activeNav === id ? "bg-white/15 text-white" : "text-blue-200 hover:bg-white/10 hover:text-white"}`}>
              {icon}{label}
            </button>
          ))}
        </nav>
        <div className="px-3 pb-5">
          <div className="bg-white/10 rounded-xl p-3 text-center">
            <HelpCircle size={20} className="text-blue-300 mx-auto mb-1" />
            <p className="text-white text-xs font-medium">¿Necesitas ayuda?</p>
            <p className="text-blue-300 text-[10px] mt-0.5">Contactanos por WhatsApp</p>
          </div>
        </div>
      </aside>

      {/* Right side */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* Top navbar */}
        <header className="bg-white shadow-sm z-10 px-6 py-3 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <ImageWithFallback src={logoImg} alt="La Montaña" className="h-8 w-8 object-contain rounded-full" />
            <span className="font-semibold text-[#1e2d3d] text-sm hidden sm:block" style={{ fontFamily: "'Playfair Display', serif" }}>
              La Montaña <span className="text-orange-500 font-normal text-xs">impresiones</span>
            </span>
          </div>
          <nav className="hidden md:flex items-center gap-1">
            {[
              { id: "inicio", label: "Inicio" },
              { id: "pedidos", label: "Mis Pedidos" },
              { id: "contacto", label: "Contacto" },
            ].map(({ id, label }) => (
              <button key={id} onClick={() => onNavChange(id)}
                className={`px-4 py-1.5 rounded-lg text-sm font-medium transition-colors ${
                  activeNav === id ? "bg-[#1e2d3d] text-white" : "text-gray-500 hover:text-gray-800 hover:bg-gray-100"}`}>
                {label}
              </button>
            ))}
          </nav>
          <div className="relative">
            <button onClick={() => setShowUserMenu(!showUserMenu)}
              className="flex items-center gap-2 px-3 py-1.5 rounded-lg hover:bg-gray-100 transition-colors">
              <div className="w-7 h-7 rounded-full bg-orange-500 flex items-center justify-center text-white text-xs font-semibold">A</div>
              <span className="text-sm text-gray-700 hidden sm:block">Alejandro</span>
              <ChevronDown size={14} className="text-gray-400" />
            </button>
            {showUserMenu && (
              <div className="absolute right-0 top-full mt-1 bg-white border border-gray-200 rounded-xl shadow-lg py-1 w-40 z-50">
                <button className="w-full flex items-center gap-2 px-4 py-2 text-sm text-gray-700 hover:bg-gray-50">
                  <User size={14} /> Mi perfil
                </button>
                <button onClick={onLogout} className="w-full flex items-center gap-2 px-4 py-2 text-sm text-red-600 hover:bg-red-50">
                  <LogOut size={14} /> Cerrar sesión
                </button>
              </div>
            )}
          </div>
        </header>

        {/* Page content */}
        <main className="flex-1 overflow-auto p-6">{children}</main>

        <footer className="text-center py-3 text-xs text-gray-400 border-t border-gray-100">
          © 2026 La Montaña Impresiones · Todos los derechos reservados
        </footer>
      </div>

      <WhatsAppFAB />
    </div>
  );
}

/* ─── Login Page ─── */
function LoginPage({ onLogin }: { onLogin: () => void }) {
  const [showPassword, setShowPassword] = useState(false);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");

  return (
    <div className="min-h-screen flex flex-col" style={{ fontFamily: "'Inter', sans-serif" }}>
      <header className="bg-white/90 backdrop-blur-sm shadow-sm z-10 relative">
        <div className="max-w-6xl mx-auto px-6 py-3 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <ImageWithFallback src={logoImg} alt="La Montaña" className="h-10 w-10 object-contain rounded-full" />
            <div className="leading-tight">
              <div className="font-semibold text-[#2c3e5a] text-sm" style={{ fontFamily: "'Playfair Display', serif" }}>La Montaña</div>
              <div className="text-[10px] tracking-widest text-[#6b7a8d] uppercase">impresiones</div>
            </div>
          </div>
          <nav className="flex items-center gap-6">
            <a href="#" className="text-sm text-[#4a5568] hover:text-[#2c3e5a] transition-colors">Contacto</a>
            <a href="#" className="text-sm text-[#4a5568] hover:text-[#2c3e5a] transition-colors">Cuenta</a>
          </nav>
        </div>
      </header>

      <main className="flex-1 relative flex items-center justify-center py-12 px-4">
        <div className="absolute inset-0 overflow-hidden"><MountainBg /></div>
        <div className="relative z-10 w-full max-w-sm">
          <div className="bg-white rounded-2xl shadow-2xl px-8 py-10">
            <div className="flex justify-center mb-6">
              <ImageWithFallback src={logoImg} alt="La Montaña" className="h-24 w-24 object-contain rounded-full" />
            </div>
            <form className="space-y-4" onSubmit={(e) => { e.preventDefault(); onLogin(); }}>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"><Mail size={16} /></span>
                <input type="email" placeholder="Email" value={email} onChange={(e) => setEmail(e.target.value)}
                  className="w-full border border-gray-200 rounded-lg pl-9 pr-4 py-2.5 text-sm text-gray-700 placeholder-gray-400 focus:outline-none focus:border-[#2c3e5a] focus:ring-1 focus:ring-[#2c3e5a] bg-white" />
              </div>
              <div className="relative">
                <span className="absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"><Lock size={16} /></span>
                <input type={showPassword ? "text" : "password"} placeholder="Contraseña" value={password} onChange={(e) => setPassword(e.target.value)}
                  className="w-full border border-gray-200 rounded-lg pl-9 pr-10 py-2.5 text-sm text-gray-700 placeholder-gray-400 focus:outline-none focus:border-[#2c3e5a] focus:ring-1 focus:ring-[#2c3e5a] bg-white" />
                <button type="button" onClick={() => setShowPassword(!showPassword)} className="absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 hover:text-gray-600">
                  {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
                </button>
              </div>
              <button type="submit" className="w-full py-2.5 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity" style={{ backgroundColor: "#5c4a2a" }}>
                Iniciar sesión
              </button>
              <button type="button" className="w-full py-2.5 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity" style={{ backgroundColor: "#c8772a" }}>
                Crear Cuenta
              </button>
              <div className="text-center pt-1">
                <a href="#" className="text-xs text-gray-500 hover:text-[#2c3e5a]">¿Olvidó su contraseña?</a>
              </div>
            </form>
          </div>
        </div>
      </main>

      <footer style={{ backgroundColor: "#3a4a5c" }} className="text-white py-6 px-6 relative">
        <div className="max-w-4xl mx-auto flex flex-col sm:flex-row items-center justify-center gap-8 sm:gap-16">
          {[
            { icon: <Shield size={22} />, title: "Seguridad", sub: "Protegemos tus datos" },
            { icon: <FolderOpen size={22} />, title: "Tus archivos", sub: "Siempre disponibles" },
            { icon: <Clock size={22} />, title: "Historial", sub: "Seguimiento en tiempo real" },
          ].map(({ icon, title, sub }) => (
            <div key={title} className="flex items-center gap-3">
              <span className="text-blue-300 shrink-0">{icon}</span>
              <div><p className="text-sm font-semibold">{title}</p><p className="text-xs text-gray-300">{sub}</p></div>
            </div>
          ))}
        </div>
        <WhatsAppFAB />
      </footer>
    </div>
  );
}

/* ─── Dashboard Home ─── */
function DashboardHome({ onCotizar }: { onCotizar: () => void }) {
  return (
    <>
      <div className="mb-5">
        <h1 className="text-xl font-semibold text-[#1e2d3d]">¡Hola Alejandro! 👋</h1>
        <p className="text-sm text-gray-500 mt-0.5">
          Gracias por confiar en La Montaña.<br className="hidden sm:block" />
          Desde aquí podés gestionar y rastrear seguimiento de todos tus pedidos.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-5">
        <div className="lg:col-span-2 space-y-5">
          {/* Pedido Actual */}
          <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
            <div className="px-5 py-4 flex items-center justify-between" style={{ backgroundColor: "#1e2d3d" }}>
              <div>
                <p className="text-blue-300 text-xs font-medium uppercase tracking-wide">Pedido Actual</p>
                <p className="text-white text-lg font-bold mt-0.5">PED-2026-004</p>
              </div>
              <span className="text-xs bg-orange-500 text-white px-3 py-1 rounded-full font-medium">En producción</span>
            </div>
            <div className="p-5">
              <p className="text-xs text-gray-500 mb-4">Tu pedido está siendo evaluado por nuestro equipo administrativo.</p>
              <div className="space-y-2 mb-5">
                {STEPS.map((step, i) => {
                  const done = i < CURRENT_STEP;
                  const active = i === CURRENT_STEP;
                  return (
                    <div key={step} className="flex items-center gap-3">
                      <div className={`w-5 h-5 rounded-full flex items-center justify-center shrink-0 text-[10px] font-bold
                        ${done ? "bg-green-500 text-white" : active ? "bg-orange-500 text-white" : "bg-gray-100 text-gray-400"}`}>
                        {done ? "✓" : i + 1}
                      </div>
                      <span className={`text-sm ${done ? "text-green-600 line-through" : active ? "text-[#1e2d3d] font-semibold" : "text-gray-400"}`}>
                        {step}
                      </span>
                    </div>
                  );
                })}
              </div>
              <button className="flex items-center gap-2 text-sm font-medium text-white px-4 py-2 rounded-lg hover:opacity-90 transition-opacity" style={{ backgroundColor: "#1e2d3d" }}>
                Ver detalle del pedido <ChevronRight size={14} />
              </button>
            </div>
          </div>

          {/* Mis pedidos */}
          <div className="bg-white rounded-2xl shadow-sm overflow-hidden">
            <div className="px-5 py-4 flex items-center justify-between border-b border-gray-100">
              <h2 className="text-sm font-semibold text-[#1e2d3d]">Mis pedidos</h2>
              <button className="text-xs text-orange-500 hover:underline font-medium">Ver todos</button>
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="bg-gray-50">
                    {["N° Pedido","Estado","Fecha","Total","Acciones"].map(h => (
                      <th key={h} className="text-left px-5 py-3 text-xs font-semibold text-gray-500">{h}</th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-gray-50">
                  {PEDIDOS.map((p) => (
                    <tr key={p.id} className="hover:bg-gray-50/50 transition-colors">
                      <td className="px-5 py-3 font-medium text-[#1e2d3d]">{p.id}</td>
                      <td className="px-5 py-3">
                        <span className={`text-[10px] font-bold px-2 py-1 rounded-full uppercase ${ESTADO_STYLE[p.estado]}`}>{p.estado}</span>
                      </td>
                      <td className="px-5 py-3 text-gray-500">{p.fecha}</td>
                      <td className="px-5 py-3 text-gray-700">{p.total}</td>
                      <td className="px-5 py-3">
                        <button className="text-xs text-blue-600 hover:underline font-medium">Ver detalle</button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <div className="space-y-5">
          {/* Resumen */}
          <div className="bg-white rounded-2xl shadow-sm p-5">
            <h2 className="text-sm font-semibold text-[#1e2d3d] mb-4">Resumen General</h2>
            <div className="space-y-3 mb-5">
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center text-sm font-bold text-blue-700 shrink-0">2</div>
                <p className="text-sm text-gray-600">Pedidos enviados</p>
              </div>
              <div className="flex items-center gap-3">
                <div className="w-8 h-8 rounded-full bg-orange-100 flex items-center justify-center text-sm font-bold text-orange-600 shrink-0">1</div>
                <p className="text-sm text-gray-600">Pedido en curso</p>
              </div>
            </div>
            <div className="border-t border-gray-100 pt-4">
              <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide mb-2">Punto de entrega</p>
              <div className="flex items-start gap-2">
                <MapPin size={14} className="text-orange-500 mt-0.5 shrink-0" />
                <div>
                  <p className="text-sm font-semibold text-[#1e2d3d]">Sucursal Centre</p>
                  <p className="text-xs text-gray-400">Av. Córdoba 1234</p>
                  <p className="text-xs text-gray-400">CABA, Argentina</p>
                </div>
              </div>
              <button className="mt-3 text-xs text-blue-600 hover:underline font-medium flex items-center gap-1">
                Cambiar punto de entrega <ChevronRight size={12} />
              </button>
            </div>
          </div>

          {/* Crear pedido */}
          <div className="bg-white rounded-2xl shadow-sm p-5 text-center">
            <div className="w-14 h-14 bg-blue-50 rounded-2xl flex items-center justify-center mx-auto mb-3">
              <FileText size={28} className="text-blue-400" />
            </div>
            <h3 className="text-sm font-semibold text-[#1e2d3d] mb-1">Crear nuevo pedido</h3>
            <p className="text-xs text-gray-400 mb-4">Subí tus archivos y comenzá tu pedido</p>
            <button
              onClick={onCotizar}
              className="w-full py-2.5 rounded-xl text-sm font-semibold text-white hover:opacity-90 transition-opacity flex items-center justify-center gap-2"
              style={{ backgroundColor: "#c8772a" }}
            >
              <Package size={14} /> + Crear pedido
            </button>
          </div>
        </div>
      </div>
    </>
  );
}

/* ─── Cotizar Page ─── */
function CotizarPage({ onCotizar }: { onCotizar: () => void }) {
  const [fileName, setFileName] = useState<string | null>(null);
  const [dragging, setDragging] = useState(false);
  const [copias, setCopias] = useState(1);
  const [tamano, setTamano] = useState("A4 (210 x 297 mm)");
  const [impresion, setImpresion] = useState<"byn" | "color">("byn");
  const [sobreTapa, setSobreTapa] = useState(false);
  const [anillado, setAnillado] = useState(true);
  const fileRef = useRef<HTMLInputElement>(null);

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragging(false);
    const file = e.dataTransfer.files[0];
    if (file) setFileName(file.name);
  };

  const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) setFileName(file.name);
  };

  return (
    <>
      {/* Page header */}
      <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 mb-6">
        <div>
          <h1 className="text-xl font-semibold text-[#1e2d3d]">Crear nuevo pedido</h1>
          <p className="text-sm text-gray-500 mt-1">
            Completá los datos de tu trabajo y cargá los archivos para que podamos cotizarlo.
          </p>
        </div>
        <div className="flex items-start gap-2 bg-blue-50 border border-blue-100 rounded-xl px-4 py-3 max-w-xs shrink-0">
          <Info size={15} className="text-blue-400 mt-0.5 shrink-0" />
          <p className="text-xs text-blue-700 leading-relaxed">
            Todos los pedidos quedan pendientes de revisión. Nos pondremos en contacto para confirmarte los detalles.
          </p>
        </div>
      </div>

      <div className="space-y-5">
        {/* 1 · Cargar archivo */}
        <section className="bg-white rounded-2xl shadow-sm p-6">
          <h2 className="text-sm font-semibold text-[#1e2d3d] flex items-center gap-2 mb-4">
            <span className="w-6 h-6 rounded-full bg-[#1e2d3d] text-white flex items-center justify-center text-xs font-bold">1</span>
            Cargá tu archivo
          </h2>
          <div className="flex flex-col lg:flex-row gap-6">
            {/* Drop zone */}
            <div className="flex-1">
              <div
                onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
                onDragLeave={() => setDragging(false)}
                onDrop={handleDrop}
                className={`border-2 border-dashed rounded-xl p-8 text-center transition-colors cursor-pointer ${
                  dragging ? "border-blue-400 bg-blue-50" : "border-gray-200 hover:border-gray-300 bg-gray-50"}`}
                onClick={() => fileRef.current?.click()}
              >
                <Upload size={32} className="mx-auto mb-3 text-blue-400" />
                {fileName ? (
                  <p className="text-sm font-medium text-[#1e2d3d]">{fileName}</p>
                ) : (
                  <p className="text-sm text-gray-500">
                    Arrastrá y soltá tu archivo aquí o<br />seleccioná desde tu dispositivo
                  </p>
                )}
                <button
                  type="button"
                  className="mt-4 px-5 py-2 rounded-lg text-sm font-semibold text-white hover:opacity-90 transition-opacity"
                  style={{ backgroundColor: "#1e2d3d" }}
                  onClick={(e) => { e.stopPropagation(); fileRef.current?.click(); }}
                >
                  Seleccionar archivo
                </button>
                <input ref={fileRef} type="file" className="hidden" accept=".pdf,.doc,.docx,.jpg,.png" onChange={handleFile} />
              </div>
              <div className="mt-2 space-y-0.5">
                <p className="text-[11px] text-gray-400">Formatos permitidos: PDF, DOC, DOCX, JPG, PNG</p>
                <p className="text-[11px] text-gray-400">Tamaño máximo: 10 MB por archivo</p>
              </div>
            </div>

            {/* File info */}
            <div className="lg:w-64 bg-gray-50 rounded-xl p-4 space-y-3 self-start">
              <p className="text-xs font-semibold text-[#1e2d3d]">Al cargar el archivo:</p>
              <div className="flex items-start gap-2">
                <span className="text-green-500 text-xs mt-0.5">✓</span>
                <p className="text-xs text-gray-600">Detectaremos automáticamente el número de páginas.</p>
              </div>
              <div className="flex items-start gap-2">
                <span className="text-green-500 text-xs mt-0.5">✓</span>
                <p className="text-xs text-gray-600">Validaremos 10 MB por archivo</p>
              </div>
            </div>
          </div>
        </section>

        {/* 2 · Detalles + 3 · Opcionales in a row */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          {/* 2 · Detalles del trabajo */}
          <section className="bg-white rounded-2xl shadow-sm p-6">
            <h2 className="text-sm font-semibold text-[#1e2d3d] flex items-center gap-2 mb-5">
              <span className="w-6 h-6 rounded-full bg-[#1e2d3d] text-white flex items-center justify-center text-xs font-bold">2</span>
              Detalles del trabajo
            </h2>
            <div className="space-y-5">
              {/* Copias */}
              <div>
                <label className="text-xs font-semibold text-gray-600 block mb-1">Número de copias</label>
                <p className="text-[11px] text-gray-400 mb-2">Se considera copias al negar el producto</p>
                <div className="flex items-center gap-3">
                  <button
                    onClick={() => setCopias(Math.max(1, copias - 1))}
                    className="w-8 h-8 rounded-lg border border-gray-200 flex items-center justify-center text-gray-600 hover:bg-gray-50"
                  >
                    <Minus size={14} />
                  </button>
                  <span className="w-10 text-center text-sm font-semibold text-[#1e2d3d]">{copias}</span>
                  <button
                    onClick={() => setCopias(copias + 1)}
                    className="w-8 h-8 rounded-lg border border-gray-200 flex items-center justify-center text-gray-600 hover:bg-gray-50"
                  >
                    <Plus size={14} />
                  </button>
                </div>
              </div>

              {/* Tamaño */}
              <div>
                <label className="text-xs font-semibold text-gray-600 block mb-1">Tamaño de hoja</label>
                <select
                  value={tamano}
                  onChange={(e) => setTamano(e.target.value)}
                  className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm text-gray-700 focus:outline-none focus:border-[#1e2d3d] bg-white"
                >
                  <option>A4 (210 x 297 mm)</option>
                  <option>A3 (297 x 420 mm)</option>
                  <option>Carta (216 x 279 mm)</option>
                  <option>Oficio (216 x 356 mm)</option>
                </select>
              </div>

              {/* Impresión */}
              <div>
                <label className="text-xs font-semibold text-gray-600 block mb-2">Impresión</label>
                <div className="flex gap-3">
                  {[
                    { id: "byn", label: "Blanco y negro" },
                    { id: "color", label: "Color" },
                  ].map(({ id, label }) => (
                    <button
                      key={id}
                      onClick={() => setImpresion(id as "byn" | "color")}
                      className={`flex-1 py-2 rounded-lg border text-sm font-medium transition-colors ${
                        impresion === id
                          ? "border-[#1e2d3d] bg-[#1e2d3d] text-white"
                          : "border-gray-200 text-gray-600 hover:border-gray-300"}`}
                    >
                      {label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </section>

          {/* 3 · Opcionales */}
          <section className="bg-white rounded-2xl shadow-sm p-6">
            <h2 className="text-sm font-semibold text-[#1e2d3d] flex items-center gap-2 mb-5">
              <span className="w-6 h-6 rounded-full bg-[#1e2d3d] text-white flex items-center justify-center text-xs font-bold">3</span>
              Opcionales
            </h2>
            <div className="space-y-4">
              {/* Sobre tapa */}
              <div
                className={`rounded-xl border p-4 cursor-pointer transition-colors ${sobreTapa ? "border-[#1e2d3d] bg-blue-50/40" : "border-gray-200 hover:border-gray-300"}`}
                onClick={() => setSobreTapa(!sobreTapa)}
              >
                <div className="flex items-center gap-3">
                  {sobreTapa
                    ? <CheckSquare size={18} className="text-[#1e2d3d] shrink-0" />
                    : <Square size={18} className="text-gray-300 shrink-0" />}
                  <div>
                    <p className="text-sm font-semibold text-[#1e2d3d]">Sobre tapa</p>
                    {sobreTapa && (
                      <p className="text-xs text-gray-500 mt-1">Tapa plástica transparente para mayor protección.</p>
                    )}
                  </div>
                </div>
              </div>

              {/* Anillado */}
              <div
                className={`rounded-xl border p-4 cursor-pointer transition-colors ${anillado ? "border-[#1e2d3d] bg-blue-50/40" : "border-gray-200 hover:border-gray-300"}`}
                onClick={() => setAnillado(!anillado)}
              >
                <div className="flex items-center gap-3">
                  {anillado
                    ? <CheckSquare size={18} className="text-[#1e2d3d] shrink-0" />
                    : <Square size={18} className="text-gray-300 shrink-0" />}
                  <div>
                    <p className="text-sm font-semibold text-[#1e2d3d]">Anillado</p>
                    {anillado && (
                      <>
                        <p className="text-xs text-gray-500 mt-1">
                          Anillado espiral apto para cuadernos universitarios. Incluye tapa y contratapa.
                        </p>
                        <p className="text-xs font-semibold text-orange-500 mt-1">Subtotal $10.00 por unidad</p>
                      </>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </section>
        </div>

        {/* CTA */}
        <div className="bg-white rounded-2xl shadow-sm p-5 flex flex-col items-center gap-3">
          <button
            onClick={onCotizar}
            className="w-full max-w-md py-3 rounded-xl text-base font-semibold text-white hover:opacity-90 transition-opacity flex items-center justify-center gap-2"
            style={{ backgroundColor: "#c8772a" }}
          >
            <FileText size={16} /> Cotizar pedido
          </button>
          <p className="text-xs text-gray-400 text-center">
            Tu pedido será privado y solo será usado para procesar tu pedido.
          </p>
        </div>
      </div>
    </>
  );
}

/* ─── Resumen Page ─── */
function ResumenPage({ onVolver, onConfirmar }: { onVolver: () => void; onConfirmar: () => void }) {
  const [metodoPago, setMetodoPago] = useState<"transferencia" | "debito" | "efectivo">("efectivo");

  return (
    <>
      {/* Page header */}
      <div className="flex items-start justify-between mb-6">
        <div>
          <h1 className="text-xl font-semibold text-[#1e2d3d]">Detalles de Pedido</h1>
          <p className="text-sm text-gray-500 mt-1">Revisá el resumen de tu pedido antes de crearlo.</p>
        </div>
        <button
          onClick={onVolver}
          className="flex items-center gap-1.5 text-sm text-gray-500 hover:text-[#1e2d3d] transition-colors mt-1"
        >
          ← Volver
        </button>
      </div>

      <div className="max-w-3xl space-y-5">
        {/* Resumen card */}
        <section className="bg-white rounded-2xl shadow-sm p-6">
          <h2 className="text-sm font-semibold text-[#1e2d3d] mb-0.5">Resumen de tu pedido</h2>
          <p className="text-xs text-gray-400 mb-5">Verificá los detalles antes de hacer el pedido.</p>

          <div className="grid grid-cols-2 sm:grid-cols-3 gap-x-6 gap-y-4">
            {/* Col 1 */}
            <div className="space-y-4">
              <div>
                <p className="text-[10px] text-gray-400 uppercase tracking-wide font-semibold mb-0.5">Número de páginas</p>
                <p className="text-sm font-semibold text-[#1e2d3d]">15</p>
              </div>
              <div>
                <p className="text-[10px] text-gray-400 uppercase tracking-wide font-semibold mb-0.5">Cantidad de copias</p>
                <p className="text-sm font-semibold text-[#1e2d3d]">3</p>
              </div>
            </div>

            {/* Col 2 */}
            <div className="space-y-4">
              <div>
                <p className="text-[10px] text-gray-400 uppercase tracking-wide font-semibold mb-0.5">Tamaño de hoja</p>
                <div className="flex items-center gap-1.5">
                  <span className="w-2.5 h-2.5 rounded-full bg-orange-400 shrink-0" />
                  <p className="text-sm font-semibold text-[#1e2d3d]">A4</p>
                </div>
              </div>
              <div>
                <p className="text-[10px] text-gray-400 uppercase tracking-wide font-semibold mb-1">Extras</p>
                <div className="space-y-1">
                  <div className="flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full bg-orange-400 shrink-0" />
                    <p className="text-sm text-[#1e2d3d]">Blanco y negro</p>
                  </div>
                  <div className="flex items-center gap-1.5">
                    <span className="w-2.5 h-2.5 rounded-full bg-orange-400 shrink-0" />
                    <p className="text-sm text-[#1e2d3d]">Anillado</p>
                  </div>
                </div>
              </div>
            </div>

            {/* Col 3 */}
            <div className="space-y-4">
              <div>
                <p className="text-[10px] text-gray-400 uppercase tracking-wide font-semibold mb-0.5">Nombre de archivo</p>
                <p className="text-sm font-semibold text-[#1e2d3d] truncate">TR_Final.PDF</p>
              </div>
              <div>
                <p className="text-[10px] text-gray-400 uppercase tracking-wide font-semibold mb-0.5">Tamaño del archivo</p>
                <p className="text-sm font-semibold text-[#1e2d3d]">8 MB</p>
              </div>
            </div>
          </div>

          {/* Tiempo + Precio */}
          <div className="grid grid-cols-2 gap-4 mt-6 pt-5 border-t border-gray-100">
            <div className="bg-gray-50 rounded-xl p-4 flex items-center gap-3">
              <Clock size={20} className="text-[#1e2d3d] shrink-0" />
              <div>
                <p className="text-[10px] text-gray-400 uppercase tracking-wide font-semibold">Tiempo estimado de entrega</p>
                <p className="text-lg font-bold text-[#1e2d3d]">24 hs</p>
              </div>
            </div>
            <div className="bg-orange-50 rounded-xl p-4 flex items-center gap-3">
              <span className="text-orange-400 text-xl font-bold shrink-0">$</span>
              <div>
                <p className="text-[10px] text-gray-400 uppercase tracking-wide font-semibold">Precio final</p>
                <p className="text-lg font-bold text-[#1e2d3d]">$1500</p>
              </div>
            </div>
          </div>
        </section>

        {/* Método de pago + Punto de entrega */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-5">
          {/* Método de pago */}
          <section className="bg-white rounded-2xl shadow-sm p-6">
            <h2 className="text-sm font-semibold text-[#1e2d3d] mb-4">Método de pago</h2>
            <div className="space-y-3">
              {([
                { id: "transferencia", label: "Transferencia" },
                { id: "debito",        label: "Débito" },
                { id: "efectivo",      label: "Efectivo" },
              ] as const).map(({ id, label }) => (
                <label key={id} className="flex items-center gap-3 cursor-pointer group">
                  <div
                    onClick={() => setMetodoPago(id)}
                    className={`w-5 h-5 rounded-full border-2 flex items-center justify-center shrink-0 transition-colors ${
                      metodoPago === id ? "border-[#1e2d3d]" : "border-gray-300 group-hover:border-gray-400"}`}
                  >
                    {metodoPago === id && <div className="w-2.5 h-2.5 rounded-full bg-[#1e2d3d]" />}
                  </div>
                  <span className={`text-sm ${metodoPago === id ? "font-semibold text-[#1e2d3d]" : "text-gray-600"}`}>{label}</span>
                </label>
              ))}
            </div>
          </section>

          {/* Punto de entrega */}
          <section className="bg-white rounded-2xl shadow-sm p-6">
            <h2 className="text-sm font-semibold text-[#1e2d3d] mb-4">Punto de entrega</h2>
            <div className="flex items-start gap-3 mb-4">
              <MapPin size={18} className="text-orange-400 mt-0.5 shrink-0" />
              <p className="text-sm font-semibold text-[#1e2d3d]">Lope de Vega 2150, CABA</p>
            </div>
            <button className="w-full py-2 rounded-lg border border-gray-200 text-sm text-gray-600 hover:border-gray-300 hover:bg-gray-50 transition-colors">
              Cambiar o agregar punto
            </button>
          </section>
        </div>

        {/* CTA */}
        <div className="bg-white rounded-2xl shadow-sm p-5 flex flex-col items-center gap-3">
          <button
            onClick={onConfirmar}
            className="w-full py-3 rounded-xl text-base font-semibold text-white hover:opacity-90 transition-opacity flex items-center justify-center gap-2"
            style={{ backgroundColor: "#c8772a" }}
          >
            <Package size={16} /> Crear Pedido
          </button>
          <p className="text-xs text-gray-400 text-center flex items-center gap-1.5">
            <Info size={12} className="shrink-0" />
            Nos pondremos en contacto cuando tu pedido haya sido confirmado.
          </p>
        </div>
      </div>
    </>
  );
}

/* ─── Root ─── */
export default function App() {
  const [view, setView] = useState<View>("login");
  const [activeNav, setActiveNav] = useState("inicio");

  const handleNavChange = (id: string) => {
    setActiveNav(id);
    if (id === "cotizar") setView("cotizar");
    else setView("dashboard");
  };

  const goToCotizar = () => { setActiveNav("cotizar"); setView("cotizar"); };
  const goToResumen = () => setView("resumen");
  const goToDashboard = () => { setActiveNav("inicio"); setView("dashboard"); };

  if (view === "login") return <LoginPage onLogin={() => setView("dashboard")} />;

  return (
    <DashboardShell activeNav={activeNav} onNavChange={handleNavChange} onLogout={() => setView("login")}>
      {view === "cotizar"  && <CotizarPage onCotizar={goToResumen} />}
      {view === "resumen"  && <ResumenPage onVolver={goToCotizar} onConfirmar={goToDashboard} />}
      {view === "dashboard" && <DashboardHome onCotizar={goToCotizar} />}
    </DashboardShell>
  );
}
