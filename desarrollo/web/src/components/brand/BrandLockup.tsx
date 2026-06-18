import Image from "next/image";


export function BrandLockup({ compact = false }: { compact?: boolean; }) {
  return (
    <div className="brand-lockup">
      <Image
        className="brand-logo"
        src="/logo.jpg"
        alt="La Montaña Impresiones"
        width={64}
        height={64}
        priority />
      {!compact && (
        <div>
          <div className="brand-title">La Montaña</div>
          <div className="brand-subtitle">impresiones</div>
        </div>
      )}
    </div>
  );
}
