export default function App() {
  return (
    <div className="min-h-screen" style={{ backgroundColor: 'var(--color-summit-white)' }}>
      {/* Header */}
      <header className="border-b" style={{ borderColor: 'var(--border)' }}>
        <div className="container mx-auto px-6 py-8 max-w-7xl">
          <h1 style={{
            fontFamily: "'Roboto Slab', serif",
            fontSize: 'var(--text-5xl)',
            fontWeight: 700,
            color: 'var(--color-mountain-slate)'
          }}>
            La Montaña
          </h1>
          <p className="mt-2" style={{
            fontSize: 'var(--text-lg)',
            color: 'var(--color-mist)'
          }}>
            Imprenta moderna y accesible para profesionales
          </p>
        </div>
      </header>

      <main className="container mx-auto px-6 py-12 max-w-7xl">
        {/* Sección Paleta de Colores */}
        <section className="mb-24">
          <h2 className="mb-8">Paleta de Colores</h2>

          <div className="mb-12">
            <h3 className="mb-4" style={{ fontSize: 'var(--text-xl)' }}>Colores Primarios</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
              <ColorSwatch
                name="Mountain Slate"
                color="var(--color-mountain-slate)"
                description="Texto principal"
              />
              <ColorSwatch
                name="Mountain Blue"
                color="var(--color-mountain-blue)"
                description="CTA secundario"
              />
              <ColorSwatch
                name="Summit White"
                color="var(--color-summit-white)"
                description="Fondo principal"
                border
              />
              <ColorSwatch
                name="Snow"
                color="var(--color-snow)"
                description="Fondo alterno"
                border
              />
            </div>
          </div>

          <div>
            <h3 className="mb-4" style={{ fontSize: 'var(--text-xl)' }}>Colores de Acento</h3>
            <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
              <ColorSwatch
                name="Sunset"
                color="var(--color-sunset)"
                description="CTA principal"
              />
              <ColorSwatch
                name="Earth"
                color="var(--color-earth)"
                description="Acento cálido"
              />
              <ColorSwatch
                name="Forest"
                color="var(--color-forest)"
                description="Success"
              />
              <ColorSwatch
                name="Mist"
                color="var(--color-mist)"
                description="Texto secundario"
              />
            </div>
          </div>
        </section>

        {/* Sección Tipografía */}
        <section className="mb-24">
          <h2 className="mb-8">Tipografía</h2>

          <div className="space-y-8 p-8 rounded-lg" style={{ backgroundColor: 'var(--color-snow)' }}>
            <div>
              <p className="text-sm mb-2" style={{ color: 'var(--color-mist)' }}>H1 - Roboto Slab 700</p>
              <h1>Tecnología de Vanguardia</h1>
            </div>
            <div>
              <p className="text-sm mb-2" style={{ color: 'var(--color-mist)' }}>H2 - Roboto Slab 700</p>
              <h2>Precios Accesibles</h2>
            </div>
            <div>
              <p className="text-sm mb-2" style={{ color: 'var(--color-mist)' }}>H3 - Roboto Slab 700</p>
              <h3>Calidad Profesional</h3>
            </div>
            <div>
              <p className="text-sm mb-2" style={{ color: 'var(--color-mist)' }}>Body - Inter 400</p>
              <p style={{ fontFamily: "'Inter', sans-serif" }}>
                La Montaña combina lo mejor de la tecnología moderna con precios justos,
                diseñada especialmente para profesionales en formación que no quieren
                comprometer la calidad de sus proyectos.
              </p>
            </div>
          </div>
        </section>

        {/* Sección Botones */}
        <section className="mb-24">
          <h2 className="mb-8">Botones</h2>

          <div className="flex flex-wrap gap-4">
            <button
              className="px-6 py-3 font-semibold transition-all hover:-translate-y-0.5"
              style={{
                backgroundColor: 'var(--color-sunset)',
                color: 'white',
                borderRadius: 0,
                transition: 'var(--transition-base)'
              }}
            >
              CTA Principal (Brutalist)
            </button>

            <button
              className="px-6 py-3 font-semibold transition-all"
              style={{
                backgroundColor: 'transparent',
                color: 'var(--color-mountain-blue)',
                border: '2px solid var(--color-mountain-blue)',
                borderRadius: 'var(--radius-md)',
                transition: 'var(--transition-base)'
              }}
            >
              Botón Secundario
            </button>

            <button
              className="px-6 py-3 font-semibold underline-offset-4 hover:underline"
              style={{
                backgroundColor: 'transparent',
                color: 'var(--color-mountain-slate)',
                transition: 'var(--transition-base)'
              }}
            >
              Botón Ghost
            </button>
          </div>
        </section>

        {/* Sección Cards */}
        <section className="mb-24">
          <h2 className="mb-8">Componentes</h2>

          <div className="grid md:grid-cols-3 gap-8">
            <ServiceCard
              title="Impresión Digital"
              description="Alta calidad con tiempos de entrega rápidos"
              price="Desde $50"
            />
            <ServiceCard
              title="Diseño Gráfico"
              description="Apoyo profesional para tus proyectos"
              price="Desde $100"
            />
            <ServiceCard
              title="Acabados Premium"
              description="Dale el toque final a tus trabajos"
              price="Desde $30"
            />
          </div>
        </section>

        {/* Footer */}
        <footer className="pt-12 mt-12 border-t" style={{ borderColor: 'var(--border)' }}>
          <div className="text-center" style={{ color: 'var(--color-mist)' }}>
            <p>© 2026 La Montaña — Sistema de diseño creado para imprenta moderna</p>
          </div>
        </footer>
      </main>
    </div>
  );
}

function ColorSwatch({
  name,
  color,
  description,
  border
}: {
  name: string;
  color: string;
  description: string;
  border?: boolean;
}) {
  return (
    <div>
      <div
        className="w-full h-24 rounded-lg mb-3"
        style={{
          backgroundColor: color,
          border: border ? '1px solid var(--border)' : 'none'
        }}
      />
      <p className="font-semibold" style={{ color: 'var(--color-mountain-slate)' }}>
        {name}
      </p>
      <p className="text-sm" style={{ color: 'var(--color-mist)' }}>
        {description}
      </p>
    </div>
  );
}

function ServiceCard({
  title,
  description,
  price
}: {
  title: string;
  description: string;
  price: string;
}) {
  return (
    <div
      className="p-6 transition-all hover:shadow-lg"
      style={{
        backgroundColor: 'white',
        border: '1px solid var(--border)',
        borderRadius: 'var(--radius-lg)',
        boxShadow: 'var(--shadow-card)',
        transition: 'var(--transition-base)'
      }}
    >
      <h4 className="mb-3">{title}</h4>
      <p className="mb-4" style={{ color: 'var(--color-mist)' }}>
        {description}
      </p>
      <p
        className="font-semibold"
        style={{
          fontFamily: "'JetBrains Mono', monospace",
          color: 'var(--color-sunset)',
          fontSize: 'var(--text-lg)'
        }}
      >
        {price}
      </p>
    </div>
  );
}