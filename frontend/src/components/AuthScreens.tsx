/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from "react";
import { Lock, Mail, Phone, Calendar, User, MapPin, KeyRound, Check, FileText, HeartPulse, ShieldCheck, Activity } from "lucide-react";
import { loginUser, registerUser, sendOtp, verifyOtp, getSimulatedCode } from "../api/client";
import { RegisterRequestDTO, Gender, UserRole, OtpPurpose } from "../types";
import { ApiErrorAlert } from "./CommonUI";

interface AuthScreensProps {
  onAuthSuccess: () => void;
  initialRegister?: boolean;
}

export const AuthScreens: React.FC<AuthScreensProps> = ({ onAuthSuccess, initialRegister = false }) => {
  const [isRegister, setIsRegister] = useState(initialRegister);
  const [isOtpView, setIsOtpView] = useState(false);
  const [activeTab, setActiveTab] = useState<"patient" | "doctor" | "admin">("patient");

  // Common errors
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(false);
  const [feedbackMsg, setFeedbackMsg] = useState("");

  // Login inputs
  const [loginEmail, setLoginEmail] = useState("patient@healthmate.com");
  const [loginPassword, setLoginPassword] = useState("patient123");

  // Registration inputs
  const [regName, setRegName] = useState("");
  const [regEmail, setRegEmail] = useState("");
  const [regPassword, setRegPassword] = useState("");
  const [regPhone, setRegPhone] = useState("");
  const [regRole, setRegRole] = useState<UserRole>("ROLE_PATIENT");
  const [regGender, setRegGender] = useState<Gender>("FEMALE");
  const [regDob, setRegDob] = useState("");
  const [regAddress, setRegAddress] = useState("");

  // OTP inputs
  const [otpEmail, setOtpEmail] = useState("");
  const [otpPurpose, setOtpPurpose] = useState<OtpPurpose>("PASSWORD_RESET");
  const [otpCode, setOtpCode] = useState("");
  const [otpSent, setOtpSent] = useState(false);
  const [discoveredDemoCode, setDiscoveredDemoCode] = useState<string | undefined>(undefined);

  // Handles Login
  const handleLoginSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!loginEmail || !loginPassword) {
      setError("Please complete all login fields.");
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await loginUser({ email: loginEmail, password: loginPassword });
      onAuthSuccess();
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  // Handles Registration
  const handleRegisterSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!regName.trim() || !regEmail.trim() || !regPassword.trim() || !regPhone.trim() || !regDob || !regAddress.trim()) {
      setError("All fields are strictly required.");
      return;
    }

    if (regPassword.length < 8) {
      setError("Password must be at least 8 characters long.");
      return;
    }

    setLoading(true);
    try {
      const dto: RegisterRequestDTO = {
        name: regName,
        email: regEmail,
        password: regPassword,
        phoneNumber: regPhone,
        role: regRole,
        gender: regGender,
        dateOfBirth: regDob,
        address: regAddress,
      };
      await registerUser(dto);
      onAuthSuccess();
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  // Handles sending simulated or real OTP
  const handleSendOtp = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!otpEmail) {
      setError("Email address is required.");
      return;
    }
    setError(null);
    setLoading(true);
    try {
      await sendOtp({ email: otpEmail, purpose: otpPurpose });
      setOtpSent(true);
      setFeedbackMsg(`Verification OTP code issued successfully to ${otpEmail}!`);

      // If in demo mode, retrieve code directly so they can test it instantly
      const mockCode = getSimulatedCode(otpEmail, otpPurpose);
      setDiscoveredDemoCode(mockCode);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  // Handles verification of OTP
  const handleVerifyOtpSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!otpCode || otpCode.length !== 6) {
      setError("Please fill in a valid 6-character verification code.");
      return;
    }
    setError(null);
    setLoading(true);
    try {
      const result = await verifyOtp({
        email: otpEmail,
        otpCode: otpCode,
        purpose: otpPurpose,
      });

      setFeedbackMsg("Verification success! Simulation approved authorization.");
      // Auto-populate & switch to password entry
      setLoginEmail(otpEmail);
      setLoginPassword("password123"); // Mock reset password to this default
      setIsOtpView(false);
      setIsRegister(false);
      setError(null);
      alert(`OTP successfully verified! For demo purposes, we have pre-configured a temporary password reset code ("password123") you can now sign in with.`);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex flex-col justify-between">
      {/* Top compact info strip in brand colors */}
      <div className="bg-blue-600 px-4 py-2 text-center text-xs font-bold text-white tracking-widest uppercase shadow-xs">
        ⚡ 2026 HIPAA-Compliant Secured Clinical Instance Active
      </div>

      <div className="flex-1 w-full max-w-7xl mx-auto px-4 md:px-8 py-8 flex flex-col lg:flex-row items-stretch justify-center gap-8 lg:gap-12">
        
        {/* INTERACTIVE COMPANION INFO PANEL (Left on desktop, top on mobile) */}
        <div className="flex-1 bg-white rounded-2xl border border-slate-200 shadow-sm p-6 sm:p-8 flex flex-col justify-between space-y-6">
          <div>
            <div className="flex items-center gap-2 mb-4">
              <span className="flex items-center justify-center w-10 h-10 rounded-xl bg-blue-50 text-blue-600 shadow-sm">
                <HeartPulse className="w-5 h-5 animate-pulse" />
              </span>
              <div>
                <h3 className="text-xs font-black text-slate-800 tracking-wider">HEALTH MATE EHR</h3>
                <p className="text-[9px] font-mono font-bold text-slate-400 uppercase">Interactive Operations Center</p>
              </div>
            </div>

            <h1 className="text-2xl sm:text-3xl font-extrabold text-slate-900 tracking-tight leading-8 sm:leading-9">
              Your Clinical Workflow, <span className="text-blue-600">Unified & Secure</span>
            </h1>
            <p className="text-xs text-slate-500 mt-2 font-medium leading-relaxed">
              Welcome to the Health Mate EHR Operations Gateway. Unified scheduling, practitioner slot generation, and real-time medical logs tailored for modern clinical precision.
            </p>

            {/* Portal functions highlights with elegant layouts */}
            <div className="mt-8 space-y-3">
              <h4 className="text-[10px] font-bold text-slate-400 uppercase tracking-widest">Portal Functions & Solutions</h4>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-3.5">
                <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 flex items-start gap-2.5">
                  <span className="w-6 h-6 rounded-lg bg-blue-100 text-blue-700 flex items-center justify-center shrink-0 mt-0.5 font-mono text-[9px] font-bold">01</span>
                  <div>
                    <h5 className="text-[11px] font-bold text-slate-800 uppercase">Consult Scheduling</h5>
                    <p className="text-[10px] text-slate-500 mt-0.5 leading-snug">Patients easily filter slot availability by medical specialty and schedule instant visits.</p>
                  </div>
                </div>

                <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 flex items-start gap-2.5">
                  <span className="w-6 h-6 rounded-lg bg-blue-100 text-blue-700 flex items-center justify-center shrink-0 mt-0.5 font-mono text-[9px] font-bold">02</span>
                  <div>
                    <h5 className="text-[11px] font-bold text-slate-800 uppercase">Active Slots Engine</h5>
                    <p className="text-[10px] text-slate-500 mt-0.5 leading-snug">Doctors generate concrete, date-specific diagnostic tables to accept bookings.</p>
                  </div>
                </div>

                <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 flex items-start gap-2.5">
                  <span className="w-6 h-6 rounded-lg bg-blue-100 text-blue-700 flex items-center justify-center shrink-0 mt-0.5 font-mono text-[9px] font-bold">03</span>
                  <div>
                    <h5 className="text-[11px] font-bold text-slate-800 uppercase">HIPAA Diagnosis Logs</h5>
                    <p className="text-[10px] text-slate-500 mt-0.5 leading-snug">Authorized medical professionals log and finalize medication prescription summaries.</p>
                  </div>
                </div>

                <div className="p-3 bg-slate-50 rounded-xl border border-slate-100 flex items-start gap-2.5">
                  <span className="w-6 h-6 rounded-lg bg-blue-100 text-blue-700 flex items-center justify-center shrink-0 mt-0.5 font-mono text-[9px] font-bold">04</span>
                  <div>
                    <h5 className="text-[11px] font-bold text-slate-800 uppercase">Traceable Auditing</h5>
                    <p className="text-[10px] text-slate-500 mt-0.5 leading-snug">Administrators inspect activity logs, user registries, and medical status profiles.</p>
                  </div>
                </div>
              </div>
            </div>

            {/* INTERACTIVE TOUR COMPONENT: Explore Operator Workflows */}
            <div className="mt-8 p-4 bg-blue-50/60 rounded-xl border border-blue-100">
              <div className="flex items-center justify-between mb-3.5">
                <span className="text-[11px] font-bold text-blue-900 uppercase tracking-widest flex items-center gap-1.5">
                  <Activity className="w-3.5 h-3.5 text-blue-600" />
                  Interactive Role Capabilities
                </span>
                <span className="text-[8px] bg-blue-600 text-white font-bold px-2 py-0.5 rounded-full uppercase tracking-wider">
                  Live Previewer
                </span>
              </div>

              {/* Selector Tabs */}
              <div className="grid grid-cols-3 gap-1.5 p-1 bg-white rounded-lg border border-slate-200 mb-3">
                <button
                  type="button"
                  onClick={() => setActiveTab("patient")}
                  className={`py-1.5 rounded text-[10px] font-bold transition-all text-center cursor-pointer ${
                    activeTab === "patient" ? "bg-blue-600 text-white shadow-xs" : "text-slate-600 hover:text-slate-800 hover:bg-slate-50"
                  }`}
                >
                  🧑‍🦽 Patient
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab("doctor")}
                  className={`py-1.5 rounded text-[10px] font-bold transition-all text-center cursor-pointer ${
                    activeTab === "doctor" ? "bg-blue-600 text-white shadow-xs" : "text-slate-600 hover:text-slate-800 hover:bg-slate-50"
                  }`}
                >
                  🥼 Practitioner
                </button>
                <button
                  type="button"
                  onClick={() => setActiveTab("admin")}
                  className={`py-1.5 rounded text-[10px] font-bold transition-all text-center cursor-pointer ${
                    activeTab === "admin" ? "bg-blue-600 text-white shadow-xs" : "text-slate-600 hover:text-slate-800 hover:bg-slate-50"
                  }`}
                >
                  💼 Admin
                </button>
              </div>

              {/* Dynamic Tour Details Output */}
              <div className="text-xs text-slate-600 min-h-[58px] transition-all duration-205">
                {activeTab === "patient" && (
                  <p className="animate-fade-in text-[11px] leading-relaxed">
                    <strong className="text-slate-800">Patient Dashboard</strong>: Schedule immediate practitioner visits, input medical query logs, self-register profile records, and view approved consult diagnosis reports. Try test login <code className="bg-white/80 px-1 py-0.5 rounded text-blue-700 font-mono font-bold text-[10px]">patient@healthmate.com</code>.
                  </p>
                )}
                {activeTab === "doctor" && (
                  <p className="animate-fade-in text-[11px] leading-relaxed">
                    <strong className="text-slate-800">Doctor Workspace</strong>: Define flexible appointment time slots, review patient consult symptom charts, write real-time treatments or prescriptions, and organize patient lists. Try test login <code className="bg-white/80 px-1 py-0.5 rounded text-blue-700 font-mono font-bold text-[10px]">doctor@healthmate.com</code>.
                  </p>
                )}
                {activeTab === "admin" && (
                  <p className="animate-fade-in text-[11px] leading-relaxed">
                    <strong className="text-slate-800">System Admin Control</strong>: Unrestricted tracking of clinic usage metric charts, HIPAA compliance security checklists, operator profile directories, and diagnostic status monitors. Try test login <code className="bg-white/80 px-1 py-0.5 rounded text-blue-700 font-mono font-bold text-[10px]">admin@healthmate.com</code>.
                  </p>
                )}
              </div>
            </div>
          </div>

          {/* Unsplash Consultation Image */}
          <div className="relative rounded-xl overflow-hidden border border-slate-200 shadow-xs h-36 hidden md:block select-none pointer-events-none shrink-0 border-slate-200">
            <img
              src="https://images.unsplash.com/photo-1576091160399-112ba8d25d1d?auto=format&fit=crop&w=1200&q=80"
              alt="Medical Consultation Display Setup"
              referrerPolicy="no-referrer"
              className="w-full h-full object-cover grayscale-20 hover:grayscale-0 transition-all duration-500"
            />
            <div className="absolute inset-0 bg-gradient-to-r from-slate-900/40 to-transparent flex items-end p-3">
              <span className="text-[10px] font-mono tracking-wide font-black text-white bg-blue-600/90 px-2 py-0.5 rounded shadow-xs uppercase">
                Digital Primary Care Workflow Illustrated
              </span>
            </div>
          </div>

          {/* Secure guidelines */}
          <div className="flex items-center gap-2 border-t border-slate-100 pt-4 text-[10px] text-slate-400">
            <ShieldCheck className="w-4 h-4 text-emerald-500 shrink-0" />
            <p className="font-semibold leading-relaxed">
              Standard secure credentials validation. All database records are persistent, encrypted, and isolated to ensure absolute clinical isolation.
            </p>
          </div>
        </div>

        {/* AUTHENTICATION FORM WRAPPER PANEL (Right side) */}
        <div className="w-full lg:max-w-md shrink-0 flex flex-col justify-center">
          <div className="bg-white py-8 px-4 shadow-sm border border-slate-200 sm:rounded-xl sm:px-10">
          <ApiErrorAlert error={error} onDismiss={() => setError(null)} />

          {feedbackMsg && (
            <div className="mb-4 p-3.5 bg-emerald-50 border border-emerald-100 rounded-lg text-emerald-800 text-xs flex gap-2">
              <Check className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
              <p className="font-semibold">{feedbackMsg}</p>
            </div>
          )}

          {/* OTP FORM SCREEN */}
          {isOtpView ? (
            <div>
              <div className="flex items-center justify-between border-b border-slate-150 pb-3 mb-6">
                <span className="text-sm font-bold text-slate-800 font-sans">OTP Verification Workflow</span>
                <button
                  onClick={() => {
                    setIsOtpView(false);
                    setError(null);
                    setFeedbackMsg("");
                  }}
                  className="text-xs text-blue-600 font-semibold hover:underline"
                >
                  Return to Sign In
                </button>
              </div>

              {!otpSent ? (
                <form onSubmit={handleSendOtp} className="space-y-4">
                  <p className="text-xs text-slate-500">
                    Initiate security code generation to confirm account ownership.
                  </p>
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                      Email Address
                    </label>
                    <div className="relative">
                      <Mail className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input
                        type="email"
                        required
                        placeholder="john.doe@hospital.com"
                        value={otpEmail}
                        onChange={(e) => setOtpEmail(e.target.value)}
                        className="w-full pl-9 pr-4 py-2 border border-slate-205 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                      Verification Purpose
                    </label>
                    <select
                      value={otpPurpose}
                      onChange={(e) => setOtpPurpose(e.target.value as OtpPurpose)}
                      className="w-full px-3 py-2 border border-slate-205 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 bg-white"
                    >
                      <option value="PASSWORD_RESET">PASSWORD_RESET</option>
                      <option value="PASSWORD_CHANGE">PASSWORD_CHANGE</option>
                    </select>
                  </div>

                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full mt-2 flex justify-center py-2.5 px-4 text-sm font-bold text-white bg-blue-600 hover:bg-blue-700 transition rounded-lg disabled:bg-slate-300 shadow-sm"
                  >
                    {loading ? "Issuing Request..." : "Send Verification OTP"}
                  </button>
                </form>
              ) : (
                <form onSubmit={handleVerifyOtpSubmit} className="space-y-4">
                  <p className="text-xs text-slate-500">
                    A 6-digit secret code has been dispatched. Enter it below to complete authorization.
                  </p>

                  {/* HIGH-FIDELITY DEMO TOOLTIP BOX */}
                  {discoveredDemoCode && (
                    <div className="p-3 bg-amber-50 rounded-lg border border-amber-100 text-amber-800 mt-2 mb-2 animate-fade-in">
                      <p className="text-[11px] font-bold uppercase tracking-wider text-amber-900 mb-1">
                        🔒 Demo Mode Simulator Access
                      </p>
                      <p className="text-xs">
                        The simulated secure verification OTP dispatched is:{" "}
                        <span className="font-mono bg-white px-2 py-0.5 rounded border border-amber-200 font-bold text-amber-900 text-sm tracking-widest">
                          {discoveredDemoCode}
                        </span>
                      </p>
                    </div>
                  )}

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                      OTP Verification Code
                    </label>
                    <div className="relative">
                      <Lock className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input
                        type="text"
                        required
                        maxLength={6}
                        placeholder="123456"
                        value={otpCode}
                        onChange={(e) => setOtpCode(e.target.value)}
                        className="w-full pl-9 pr-4 py-2 border border-slate-205 rounded-lg text-sm text-slate-800 tracking-widest font-mono font-bold focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                      />
                    </div>
                  </div>

                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => setOtpSent(false)}
                      className="w-1/3 py-2.5 text-sm font-semibold border border-slate-200 hover:bg-slate-50 transition rounded-lg text-slate-600"
                    >
                      Back
                    </button>
                    <button
                      type="submit"
                      disabled={loading}
                      className="w-2/3 py-2.5 px-4 text-sm font-bold text-white bg-blue-600 hover:bg-blue-700 transition rounded-lg disabled:bg-slate-300 shadow-sm"
                    >
                      {loading ? "Authenticating..." : "Verify OTP Code"}
                    </button>
                  </div>
                </form>
              )}
            </div>
          ) : !isRegister ? (
            /* SIGN IN SCREEN */
            <div>
              <div className="text-center mb-6">
                <p className="text-slate-600 text-sm">Please log in to manage schedules, patients, or dashboards.</p>
              </div>

              {/* DEMO ACCOUNTS HELPER BOX */}
              <div className="mb-6 p-4 bg-slate-50 rounded-xl border border-slate-200 text-xs shadow-xs">
                <p className="font-bold text-slate-800 uppercase tracking-widest mb-2.5">💡 Quick-Test Demo Access Gateways</p>
                <div className="grid grid-cols-1 gap-2.5 md:grid-cols-3">
                  <button
                    onClick={() => {
                      setLoginEmail("patient@healthmate.com");
                      setLoginPassword("patient123");
                    }}
                    type="button"
                    className="p-3 border border-slate-200 hover:border-blue-400 rounded-lg bg-white text-left text-slate-600 hover:text-blue-600 transition shadow-xs cursor-pointer"
                  >
                    <span className="font-bold block text-slate-800 mb-0.5">🧑‍🦽 Patient</span>
                    patient@healthmate.com
                  </button>
                  <button
                    onClick={() => {
                      setLoginEmail("doctor@healthmate.com");
                      setLoginPassword("doctor123");
                    }}
                    type="button"
                    className="p-3 border border-slate-200 hover:border-blue-400 rounded-lg bg-white text-left text-slate-600 hover:text-blue-600 transition shadow-xs cursor-pointer"
                  >
                    <span className="font-bold block text-slate-800 mb-0.5">🥼 Doctor</span>
                    doctor@healthmate.com
                  </button>
                  <button
                    onClick={() => {
                      setLoginEmail("admin@healthmate.com");
                      setLoginPassword("admin123");
                    }}
                    type="button"
                    className="p-3 border border-slate-200 hover:border-blue-400 rounded-lg bg-white text-left text-slate-600 hover:text-blue-600 transition shadow-xs cursor-pointer"
                  >
                    <span className="font-bold block text-slate-800 mb-0.5">💼 Admin</span>
                    admin@healthmate.com
                  </button>
                </div>
              </div>

              <form onSubmit={handleLoginSubmit} className="space-y-4">
                <div>
                  <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider mb-1">
                    Email address
                  </label>
                  <div className="relative">
                    <Mail className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                    <input
                      type="email"
                      required
                      placeholder="enter your security email"
                      value={loginEmail}
                      onChange={(e) => setLoginEmail(e.target.value)}
                      className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                    />
                  </div>
                </div>

                <div>
                  <div className="flex justify-between items-center mb-1">
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wider">
                      Password
                    </label>
                    <button
                      type="button"
                      onClick={() => {
                        setIsOtpView(true);
                        setOtpSent(false);
                        setOtpEmail(loginEmail);
                        setDiscoveredDemoCode(undefined);
                      }}
                      className="text-xs font-bold text-blue-600 hover:underline"
                    >
                      Forgot password?
                    </button>
                  </div>
                  <div className="relative">
                    <Lock className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                    <input
                      type="password"
                      required
                      placeholder="••••••••"
                      value={loginPassword}
                      onChange={(e) => setLoginPassword(e.target.value)}
                      className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                    />
                  </div>
                </div>

                <div className="pt-2">
                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full cursor-pointer flex justify-center py-2.5 px-4 text-sm font-bold text-white bg-blue-600 hover:bg-blue-700 transition rounded-lg shadow-md"
                  >
                    {loading ? "Authenticating Session..." : "Sign In Gateway"}
                  </button>
                </div>
              </form>

              <div className="mt-6 border-t border-slate-200 pt-6 text-center">
                <p className="text-xs text-slate-500">
                  New operator or patient wishing to self-register?{" "}
                  <button
                    onClick={() => {
                      setIsRegister(true);
                      setError(null);
                      setFeedbackMsg("");
                    }}
                    className="font-bold text-blue-600 hover:underline"
                  >
                    Register Account Link
                  </button>
                </p>
              </div>
            </div>
          ) : (
            /* REGISTRATION SCREEN */
            <div>
              <div className="text-center mb-6">
                <p className="text-slate-600 text-sm">Please provide high-integrity details in accordance with healthcare HIPAA standards.</p>
              </div>

              <form onSubmit={handleRegisterSubmit} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                      Full Legal Name
                    </label>
                    <div className="relative">
                      <User className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input
                        type="text"
                        required
                        placeholder="Asha Sharma"
                        value={regName}
                        onChange={(e) => setRegName(e.target.value)}
                        className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-indigo-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                      Email Address
                    </label>
                    <div className="relative">
                      <Mail className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input
                        type="email"
                        required
                        placeholder="asha@example.com"
                        value={regEmail}
                        onChange={(e) => setRegEmail(e.target.value)}
                        className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-indigo-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                      Credential Password
                    </label>
                    <div className="relative">
                      <Lock className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input
                        type="password"
                        required
                        placeholder="minimum 8 characters"
                        value={regPassword}
                        onChange={(e) => setRegPassword(e.target.value)}
                        className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-indigo-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                      Phone Number
                    </label>
                    <div className="relative">
                      <Phone className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input
                        type="tel"
                        required
                        placeholder="9876543210"
                        value={regPhone}
                        onChange={(e) => setRegPhone(e.target.value)}
                        className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-indigo-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                      Assigned Role
                    </label>
                    <select
                      value={regRole}
                      onChange={(e) => setRegRole(e.target.value as UserRole)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-indigo-500 bg-white"
                    >
                      <option value="ROLE_PATIENT">Patient Profile</option>
                      <option value="ROLE_DOCTOR">Doctor Practitioner</option>
                      <option value="ROLE_ADMIN">Health Administrator</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                      Gender Marker
                    </label>
                    <select
                      value={regGender}
                      onChange={(e) => setRegGender(e.target.value as Gender)}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-indigo-500 bg-white"
                    >
                      <option value="FEMALE">Female</option>
                      <option value="MALE">Male</option>
                      <option value="OTHER">Other / Non-Binary</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-700 uppercase tracking-wider mb-1">
                      Date of Birth
                    </label>
                    <div className="relative">
                      <Calendar className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input
                        type="date"
                        required
                        value={regDob}
                        onChange={(e) => setRegDob(e.target.value)}
                        className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-indigo-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="block text-xs font-bold text-slate-700 uppercase tracking-wide mb-1">
                      Residential Address
                    </label>
                    <div className="relative">
                      <MapPin className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input
                        type="text"
                        required
                        placeholder="City, Lane, Apt"
                        value={regAddress}
                        onChange={(e) => setRegAddress(e.target.value)}
                        className="w-full pl-9 pr-4 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                      />
                    </div>
                  </div>
                </div>

                <div className="pt-2">
                  <button
                    type="submit"
                    disabled={loading}
                    className="w-full flex justify-center py-2.5 px-4 text-sm font-bold text-white bg-blue-600 hover:bg-blue-700 transition rounded-lg shadow-md cursor-pointer"
                  >
                    {loading ? "Registering secure record..." : "Register New Account"}
                  </button>
                </div>
              </form>

              <div className="mt-6 border-t border-slate-200 pt-6 text-center">
                <p className="text-xs text-slate-500">
                  Already have access?{" "}
                  <button
                    onClick={() => {
                      setIsRegister(false);
                      setError(null);
                      setFeedbackMsg("");
                    }}
                    className="font-bold text-blue-600 hover:underline"
                  >
                    Back to Sign In
                  </button>
                </p>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
    </div>
  );
};
