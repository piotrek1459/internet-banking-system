export default function HeroIllustration() {
  return (
    <svg viewBox="0 0 680 560" className="hero-illustration" role="img" aria-label="Internet banking dashboard preview">
      <defs>
        <linearGradient id="heroBg" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#dbeafe" />
          <stop offset="55%" stopColor="#eff6ff" />
          <stop offset="100%" stopColor="#f8fafc" />
        </linearGradient>
        <linearGradient id="heroPanel" x1="0%" y1="0%" x2="100%" y2="100%">
          <stop offset="0%" stopColor="#0f172a" />
          <stop offset="100%" stopColor="#1e3a8a" />
        </linearGradient>
        <linearGradient id="heroAccent" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stopColor="#2563eb" />
          <stop offset="100%" stopColor="#38bdf8" />
        </linearGradient>
      </defs>

      <rect x="0" y="0" width="680" height="560" rx="40" fill="url(#heroBg)" />
      <circle cx="92" cy="96" r="78" fill="#bfdbfe" opacity="0.5" />
      <circle cx="610" cy="94" r="54" fill="#c7d2fe" opacity="0.42" />
      <circle cx="606" cy="470" r="84" fill="#dbeafe" opacity="0.6" />

      <g transform="translate(62 68)">
        <rect x="0" y="0" width="556" height="362" rx="28" fill="url(#heroPanel)" />
        <rect x="0" y="0" width="556" height="66" rx="28" fill="#0b1120" />
        <circle cx="38" cy="33" r="8" fill="#38bdf8" />
        <rect x="64" y="24" width="132" height="18" rx="9" fill="#1e293b" />
        <rect x="420" y="22" width="96" height="22" rx="11" fill="#2563eb" />

        <rect x="32" y="96" width="162" height="230" rx="22" fill="#f8fafc" opacity="0.96" />
        <rect x="220" y="96" width="304" height="116" rx="22" fill="#f8fafc" opacity="0.98" />
        <rect x="220" y="230" width="144" height="96" rx="22" fill="#dbeafe" opacity="0.98" />
        <rect x="380" y="230" width="144" height="96" rx="22" fill="#f8fafc" opacity="0.98" />

        <rect x="52" y="122" width="86" height="12" rx="6" fill="#cbd5e1" />
        <rect x="52" y="148" width="112" height="26" rx="13" fill="#0f172a" />
        <rect x="52" y="188" width="92" height="10" rx="5" fill="#cbd5e1" />
        <rect x="52" y="210" width="108" height="10" rx="5" fill="#e2e8f0" />
        <rect x="52" y="252" width="108" height="44" rx="16" fill="#eff6ff" />
        <rect x="64" y="266" width="62" height="10" rx="5" fill="#93c5fd" />
        <rect x="64" y="282" width="84" height="8" rx="4" fill="#cbd5e1" />

        <rect x="246" y="120" width="110" height="12" rx="6" fill="#cbd5e1" />
        <rect x="246" y="146" width="172" height="28" rx="14" fill="#0f172a" />
        <path
          d="M246 192 C278 176, 312 214, 344 196 S410 160, 448 176 S500 202, 520 150"
          fill="none"
          stroke="url(#heroAccent)"
          strokeWidth="8"
          strokeLinecap="round"
        />
        <circle cx="520" cy="150" r="9" fill="#38bdf8" />

        <rect x="244" y="252" width="78" height="10" rx="5" fill="#3b82f6" />
        <rect x="244" y="274" width="96" height="12" rx="6" fill="#0f172a" opacity="0.8" />
        <rect x="404" y="252" width="70" height="10" rx="5" fill="#93c5fd" />
        <rect x="404" y="274" width="88" height="12" rx="6" fill="#0f172a" opacity="0.8" />

        <rect x="430" y="346" width="80" height="18" rx="9" fill="#2563eb" />
      </g>

      <g transform="translate(62 448)">
        <rect x="0" y="0" width="220" height="58" rx="18" fill="#ffffff" />
        <rect x="18" y="16" width="84" height="10" rx="5" fill="#94a3b8" />
        <rect x="18" y="32" width="126" height="12" rx="6" fill="#0f172a" />
        <rect x="158" y="15" width="44" height="28" rx="14" fill="#dcfce7" />
      </g>

      <g transform="translate(438 432)">
        <rect x="0" y="0" width="160" height="74" rx="22" fill="#0f172a" />
        <rect x="18" y="18" width="56" height="10" rx="5" fill="#334155" />
        <rect x="18" y="36" width="90" height="18" rx="9" fill="#f8fafc" />
        <rect x="118" y="22" width="22" height="22" rx="11" fill="#2563eb" />
      </g>
    </svg>
  )
}
