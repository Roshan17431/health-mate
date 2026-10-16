import { HeartPulse } from "lucide-react";

export function Brand() {
  return (
    <div className="brand" aria-label="Health Mate">
      <span className="brand-mark"><HeartPulse size={24} strokeWidth={2} aria-hidden="true" /></span>
      <span>Health<span className="brand-name-light"> Mate</span><span className="brand-dot">.</span></span>
    </div>
  );
}
