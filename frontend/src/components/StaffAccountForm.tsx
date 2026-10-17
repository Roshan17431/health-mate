import React, { useId, useState } from "react";
import { createStaffUser } from "../api/client";
import { Gender, RegisterRequestDTO } from "../types";
import { ApiErrorAlert } from "./CommonUI";

export function StaffAccountForm({ onCreated }: { onCreated: () => void }) {
  const prefix = `staff-${useId()}`;
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const [form, setForm] = useState<RegisterRequestDTO>({ name: "", email: "", password: "", phoneNumber: "",
    role: "ROLE_DOCTOR", gender: "OTHER", dateOfBirth: "", address: "" });
  const inputClass = "w-full px-3 py-2 border border-slate-200 rounded-lg text-sm bg-white focus:outline-none focus:border-teal-500";
  const fields = [
    ["name", "Full name", "text", "name"], ["email", "Email address", "email", "email"],
    ["password", "Temporary password", "password", "new-password"], ["phoneNumber", "Phone number", "tel", "tel"],
    ["dateOfBirth", "Date of birth", "date", "bday"], ["address", "Address", "text", "street-address"],
  ] as const;
  const submit = async (event: React.FormEvent) => {
    event.preventDefault(); setError(null);
    if (new TextEncoder().encode(form.password).length > 72) {
      setError("Password must contain at most 72 UTF-8 bytes."); return;
    }
    setBusy(true);
    try { await createStaffUser(form); onCreated(); }
    catch (err) { setError(err); }
    finally { setBusy(false); }
  };
  return <details className="p-4 bg-slate-50 border border-slate-100 rounded-xl mb-5">
    <summary className="text-sm font-semibold text-slate-800 cursor-pointer">Add a clinic team member</summary>
    <p className="text-sm text-slate-500 mt-2">Create a doctor or administrator account. Add a doctor profile in the Doctors tab after creating their account.</p>
    <ApiErrorAlert error={error} onDismiss={() => setError(null)} />
    <form onSubmit={submit} className="mt-4 space-y-4">
      <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {fields.map(([key, label, type, autoComplete]) => <div key={key}>
          <label htmlFor={`${prefix}-${key}`} className="block text-xs font-semibold text-slate-600 mb-1">{label}</label>
          <input id={`${prefix}-${key}`} type={type} required autoComplete={autoComplete}
            minLength={key === "password" ? 8 : undefined}
            max={key === "dateOfBirth" ? new Date().toISOString().slice(0, 10) : undefined}
            value={form[key]} onChange={e => setForm({ ...form, [key]: e.target.value })} className={inputClass} />
        </div>)}
        <div><label htmlFor={`${prefix}-role`} className="block text-xs font-semibold text-slate-600 mb-1">Team role</label>
          <select id={`${prefix}-role`} value={form.role} onChange={e => setForm({ ...form, role: e.target.value as RegisterRequestDTO["role"] })} className={inputClass}>
            <option value="ROLE_DOCTOR">Doctor</option><option value="ROLE_ADMIN">Administrator</option>
          </select></div>
        <div><label htmlFor={`${prefix}-gender`} className="block text-xs font-semibold text-slate-600 mb-1">Gender</label>
          <select id={`${prefix}-gender`} value={form.gender} onChange={e => setForm({ ...form, gender: e.target.value as Gender })} className={inputClass}>
            <option value="OTHER">Other / Non-binary</option><option value="FEMALE">Female</option><option value="MALE">Male</option>
          </select></div>
      </div>
      <button type="submit" disabled={busy} className="px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-lg text-sm font-semibold disabled:bg-slate-300">{busy ? "Creating account…" : "Create team account"}</button>
    </form>
  </details>;
}
