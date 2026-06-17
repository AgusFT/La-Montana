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