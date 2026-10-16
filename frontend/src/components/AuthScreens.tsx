/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState } from "react";
import { Lock, Mail, Phone, Calendar, User, MapPin, Check, HeartPulse, Stethoscope, ArrowRight, ArrowLeft } from "lucide-react";
import { Brand } from "./Brand";
import { loginUser, registerUser, sendOtp, verifyOtp, resetPassword } from "../api/client";
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
  const [loginEmail, setLoginEmail] = useState("");
  const [loginPassword, setLoginPassword] = useState("");

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
  const [resetToken, setResetToken] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const leaveOtpView = () => {
    setIsOtpView(false);
    setOtpSent(false);
    setOtpCode("");
    setResetToken("");
    setNewPassword("");
    setConfirmPassword("");
    setError(null);
  };

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

  // Handles sending backend-issued OTP codes.
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
      const verified = await verifyOtp({
        email: otpEmail,
        otpCode: otpCode,
        purpose: otpPurpose,
      });

      if (otpPurpose === "PASSWORD_RESET") {
        if (!verified.resetToken) {
          throw new Error("Password reset could not be authorized. Request a new code.");
        }
        setResetToken(verified.resetToken);
        setOtpCode("");
        setFeedbackMsg("Code verified. Choose your new password.");
        return;
      }
      setFeedbackMsg("Verification successful. You can sign in to your account.");
      setLoginEmail(otpEmail);
      setLoginPassword("");
      leaveOtpView();
      setIsRegister(false);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  const handleResetPassword = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);
    if (newPassword.length < 8 || new TextEncoder().encode(newPassword).length > 72) {
      setError("Use at least 8 characters and no more than 72 bytes for your password.");
      return;
    }
    if (newPassword !== confirmPassword) {
      setError("Passwords do not match.");
      return;
    }
    setLoading(true);
    try {
      const result = await resetPassword({ resetToken, newPassword, confirmPassword });
      setLoginEmail(otpEmail);
      setLoginPassword("");
      leaveOtpView();
      setIsRegister(false);
      setFeedbackMsg(result.message);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="auth-page">
      <header className="auth-header">
        <Brand />
        <p className="auth-header-note"><span aria-hidden="true" /> A little more connected. A little more cared for.</p>
      </header>

      <main className="auth-layout">
        <section className="auth-story" aria-label="About Health Mate">
          <p className="eyebrow">A healthier kind of connection</p>
          <h2>Good care.<br />Less hassle.<br /><em>More you.</em></h2>
          <p className="auth-story-intro">A simpler way to manage appointments, connect with your care team, and make room for what matters.</p>

          <div className="care-art" aria-hidden="true">
            <div className="care-orbit"><div className="care-symbol"><HeartPulse strokeWidth={1.5} /></div></div>
            <div className="care-note care-note-one">
              <span className="care-note-icon"><Calendar size={19} /></span>
              <div><strong>Care on your schedule</strong><small>Find a time that works for you</small></div>
            </div>
            <div className="care-note care-note-two">
              <span className="care-note-icon"><Stethoscope size={19} /></span>
              <div><strong>Your care team, closer</strong><small>One place to stay connected</small></div>
            </div>
          </div>

          <div className="story-portals">
            <div className="portal-tabs" aria-label="Explore Health Mate for your role">
              <button type="button" aria-pressed={activeTab === "patient"} onClick={() => setActiveTab("patient")}>For patients</button>
              <button type="button" aria-pressed={activeTab === "doctor"} onClick={() => setActiveTab("doctor")}>For doctors</button>
              <button type="button" aria-pressed={activeTab === "admin"} onClick={() => setActiveTab("admin")}>For clinics</button>
            </div>
            <p className="portal-description" aria-live="polite">
              {activeTab === "patient" && "Find your doctor, choose an appointment, and keep track of your visits. Your next step toward feeling better starts here."}
              {activeTab === "doctor" && "Make space for better care. Manage your availability, review appointments, and keep your day in focus."}
              {activeTab === "admin" && "Keep your clinic connected. Manage your team, patient records, appointments, and notifications in one place."}
            </p>
          </div>
        </section>

        <section className="auth-form-panel" aria-labelledby="auth-title">
          <div className={`auth-form-content ${isRegister && !isOtpView ? "is-register" : ""}`}>
            <ApiErrorAlert error={error} onDismiss={() => setError(null)} />
            {feedbackMsg && <div className="auth-feedback" role="status"><Check size={18} className="shrink-0" /><p>{feedbackMsg}</p></div>}

            {isOtpView ? (
              <>
                <button type="button" className="auth-back" disabled={loading} onClick={() => { leaveOtpView(); setFeedbackMsg(""); }}><ArrowLeft size={15} /> Back to sign in</button>
                <div className="auth-heading">
                  <p className="eyebrow">A little help getting back</p>
                  <h1 id="auth-title">{resetToken ? "Choose a new password" : otpSent ? "Check your code" : "Verify your account"}</h1>
                  <p>{resetToken ? "Use at least 8 characters. This verification lasts up to 5 minutes." : otpSent ? "Enter your 6-character verification code to continue." : "Request a verification code for your account."}</p>
                </div>
                {resetToken ? (
                  <form className="auth-form" onSubmit={handleResetPassword}>
                    <input type="email" name="username" autoComplete="username" value={otpEmail} readOnly hidden aria-label="Email address" />
                    <div className="auth-field">
                      <label htmlFor="reset-password">New password</label>
                      <div className="field-with-icon"><Lock aria-hidden="true" /><input id="reset-password" className="auth-input" type="password" autoComplete="new-password" required minLength={8} maxLength={72} value={newPassword} onChange={(e) => setNewPassword(e.target.value)} /></div>
                    </div>
                    <div className="auth-field">
                      <label htmlFor="reset-password-confirm">Confirm new password</label>
                      <div className="field-with-icon"><Lock aria-hidden="true" /><input id="reset-password-confirm" className="auth-input" type="password" autoComplete="new-password" required minLength={8} maxLength={72} value={confirmPassword} onChange={(e) => setConfirmPassword(e.target.value)} /></div>
                    </div>
                    <button type="submit" className="primary-button" disabled={loading}>{loading ? "Updating password…" : "Update password"}<ArrowRight aria-hidden="true" /></button>
                    <button type="button" className="secondary-button" disabled={loading} onClick={() => { setResetToken(""); setNewPassword(""); setConfirmPassword(""); setOtpSent(false); setFeedbackMsg(""); setError(null); }}>Request a new code</button>
                  </form>
                ) : !otpSent ? (
                  <form className="auth-form" onSubmit={handleSendOtp}>
                    <div className="auth-field">
                      <label htmlFor="otp-email">Email address</label>
                      <div className="field-with-icon"><Mail aria-hidden="true" /><input id="otp-email" className="auth-input" type="email" autoComplete="email" required placeholder="you@example.com" value={otpEmail} onChange={(e) => setOtpEmail(e.target.value)} /></div>
                    </div>
                    <div className="auth-field">
                      <label htmlFor="otp-purpose">What do you need help with?</label>
                      <select id="otp-purpose" className="auth-input" value={otpPurpose} onChange={(e) => setOtpPurpose(e.target.value as OtpPurpose)}>
                        <option value="PASSWORD_RESET">Reset password</option>
                        <option value="PASSWORD_CHANGE">Change password</option>
                      </select>
                    </div>
                    <button type="submit" className="primary-button" disabled={loading}>{loading ? "Requesting code…" : "Send verification code"}<ArrowRight aria-hidden="true" /></button>
                  </form>
                ) : (
                  <form className="auth-form" onSubmit={handleVerifyOtpSubmit}>
                    <div className="auth-field">
                      <label htmlFor="otp-code">Verification code</label>
                      <div className="field-with-icon"><Lock aria-hidden="true" /><input id="otp-code" className="auth-input tracking-widest font-mono" type="text" autoComplete="one-time-code" required maxLength={6} placeholder="Enter 6-character code" value={otpCode} onChange={(e) => setOtpCode(e.target.value)} /></div>
                    </div>
                    <button type="submit" className="primary-button" disabled={loading}>{loading ? "Verifying…" : "Verify code"}<ArrowRight aria-hidden="true" /></button>
                    <button type="button" className="secondary-button" disabled={loading} onClick={() => { setOtpSent(false); setOtpCode(""); setFeedbackMsg(""); }}>Use a different email</button>
                  </form>
                )}
              </>
            ) : !isRegister ? (
              <>
                <div className="auth-heading">
                  <p className="eyebrow">Your health, all together</p>
                  <h1 id="auth-title">Welcome back.</h1>
                  <p>A familiar place for your next step in care.<br />Sign in to your Health Mate account.</p>
                </div>
                <form className="auth-form" onSubmit={handleLoginSubmit}>
                  <div className="auth-field">
                    <label htmlFor="login-email">Email address</label>
                    <div className="field-with-icon"><Mail aria-hidden="true" /><input id="login-email" className="auth-input" type="email" autoComplete="username" required placeholder="you@example.com" value={loginEmail} onChange={(e) => setLoginEmail(e.target.value)} /></div>
                  </div>
                  <div className="auth-field">
                    <div className="password-label"><label htmlFor="login-password">Password</label><button type="button" className="text-link" onClick={() => { setIsOtpView(true); setOtpPurpose("PASSWORD_RESET"); setOtpSent(false); setOtpEmail(loginEmail); setResetToken(""); setError(null); setFeedbackMsg(""); }}>Forgot password?</button></div>
                    <div className="field-with-icon"><Lock aria-hidden="true" /><input id="login-password" className="auth-input" type="password" autoComplete="current-password" required placeholder="Enter your password" value={loginPassword} onChange={(e) => setLoginPassword(e.target.value)} /></div>
                  </div>
                  <button type="submit" className="primary-button" disabled={loading}>{loading ? "Signing you in…" : "Sign in"}<ArrowRight aria-hidden="true" /></button>
                </form>
                <p className="auth-switch">New to Health Mate?<button type="button" className="text-link" onClick={() => { setIsRegister(true); setError(null); setFeedbackMsg(""); }}>Create an account</button></p>
                <p className="auth-form-note"><Lock size={13} aria-hidden="true" /> Your care starts with your own account.</p>
              </>
            ) : (
              <>
                <div className="auth-heading">
                  <p className="eyebrow">A fresh start</p>
                  <h1 id="auth-title">Let's get you connected.</h1>
                  <p>Create your Health Mate account. All fields are required.</p>
                </div>
                <form className="auth-form" onSubmit={handleRegisterSubmit}>
                  <div className="registration-grid">
                    <div className="auth-field">
                      <label htmlFor="reg-name">Full name</label>
                      <div className="field-with-icon"><User aria-hidden="true" /><input id="reg-name" className="auth-input" required type="text" autoComplete="name" placeholder="Your full name" value={regName} onChange={(e) => setRegName(e.target.value)} /></div>
                    </div>
                    <div className="auth-field">
                      <label htmlFor="reg-email">Email address</label>
                      <div className="field-with-icon"><Mail aria-hidden="true" /><input id="reg-email" className="auth-input" required type="email" autoComplete="email" placeholder="you@example.com" value={regEmail} onChange={(e) => setRegEmail(e.target.value)} /></div>
                    </div>
                    <div className="auth-field">
                      <label htmlFor="reg-password">Password</label>
                      <div className="field-with-icon"><Lock aria-hidden="true" /><input id="reg-password" className="auth-input" required type="password" autoComplete="new-password" minLength={8} placeholder="At least 8 characters" value={regPassword} onChange={(e) => setRegPassword(e.target.value)} /></div>
                    </div>
                    <div className="auth-field">
                      <label htmlFor="reg-phone">Phone number</label>
                      <div className="field-with-icon"><Phone aria-hidden="true" /><input id="reg-phone" className="auth-input" required type="tel" autoComplete="tel" placeholder="Your phone number" value={regPhone} onChange={(e) => setRegPhone(e.target.value)} /></div>
                    </div>
                    <div className="auth-field">
                      <label htmlFor="reg-role">Account type</label>
                      <select id="reg-role" className="auth-input" value={regRole} onChange={(e) => setRegRole(e.target.value as UserRole)}><option value="ROLE_PATIENT">Patient</option><option value="ROLE_DOCTOR">Doctor</option><option value="ROLE_ADMIN">Administrator</option></select>
                    </div>
                    <div className="auth-field">
                      <label htmlFor="reg-gender">Gender</label>
                      <select id="reg-gender" className="auth-input" value={regGender} onChange={(e) => setRegGender(e.target.value as Gender)}><option value="FEMALE">Female</option><option value="MALE">Male</option><option value="OTHER">Other / Non-binary</option></select>
                    </div>
                    <div className="auth-field">
                      <label htmlFor="reg-dob">Date of birth</label>
                      <div className="field-with-icon"><Calendar aria-hidden="true" /><input id="reg-dob" className="auth-input" required type="date" autoComplete="bday" value={regDob} onChange={(e) => setRegDob(e.target.value)} /></div>
                    </div>
                    <div className="auth-field">
                      <label htmlFor="reg-address">Address</label>
                      <div className="field-with-icon"><MapPin aria-hidden="true" /><input id="reg-address" className="auth-input" required type="text" autoComplete="street-address" placeholder="Street, city, apartment" value={regAddress} onChange={(e) => setRegAddress(e.target.value)} /></div>
                    </div>
                  </div>
                  <button type="submit" className="primary-button" disabled={loading}>{loading ? "Creating your account…" : "Create account"}<ArrowRight aria-hidden="true" /></button>
                </form>
                <p className="auth-switch">Already have an account?<button type="button" className="text-link" onClick={() => { setIsRegister(false); setError(null); setFeedbackMsg(""); }}>Sign in</button></p>
              </>
            )}
          </div>
        </section>
      </main>
      <footer className="auth-footer"><span>© {new Date().getFullYear()} Health Mate</span><span>Thoughtfully connected. Simply cared for.</span></footer>
    </div>
  );
};
