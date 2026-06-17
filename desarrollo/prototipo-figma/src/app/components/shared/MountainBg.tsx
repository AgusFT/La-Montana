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
  )};

  export default MountainBg;