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