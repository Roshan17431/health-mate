/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from "react";
import { getSessionUser, clearSession } from "./api/client";
import { UserDTO } from "./types";
import { AppShell } from "./components/AppShell";
import { AuthScreens } from "./components/AuthScreens";
import { DashboardPatient } from "./components/DashboardPatient";
import { DashboardDoctor } from "./components/DashboardDoctor";
import { DashboardAdmin } from "./components/DashboardAdmin";
import { Lock, Heart, ShieldAlert } from "lucide-react";

export default function App() {
  const [user, setUser] = useState<UserDTO | null>(null);
  const [authChecked, setAuthChecked] = useState(false);

  // Sync auth session state
  const syncAuth = () => {
    setUser(getSessionUser());
    setAuthChecked(true);
  };

  useEffect(() => {
    syncAuth();

    // Dynamically listen to auth state changes raised by the client
    window.addEventListener("healthmate_auth_changed", syncAuth);
    return () => {
      window.removeEventListener("healthmate_auth_changed", syncAuth);
    };
  }, []);

  // Handles logout
  const handleSignOut = () => {
    setUser(null);
  };

  if (!authChecked) {
    return (
      <div className="min-h-screen bg-slate-50 flex flex-col items-center justify-center p-6 text-center">
        <Heart className="w-8 h-8 text-indigo-600 animate-pulse mb-2" />
        <p className="text-sm text-slate-500 font-medium">Validating Secure Medical Keychain...</p>
      </div>
    );
  }

  // Render Authentication screen if no session exists
  if (!user) {
    return <AuthScreens onAuthSuccess={syncAuth} />;
  }

  // Render Dashboard based on Dynamic Persona/Role-Security Guard
  let dashboardView: React.ReactNode;

  switch (user.role) {
    case "ROLE_ADMIN":
      dashboardView = <DashboardAdmin />;
      break;
    case "ROLE_DOCTOR":
      dashboardView = <DashboardDoctor user={user} />;
      break;
    case "ROLE_PATIENT":
      dashboardView = <DashboardPatient user={user} />;
      break;
    default:
      dashboardView = (
        <div className="bg-white border border-slate-200 p-8 rounded-xl max-w-lg mx-auto my-12 text-center shadow">
          <ShieldAlert className="w-12 h-12 text-rose-500 mx-auto mb-3" />
          <h2 className="text-lg font-bold text-slate-800">Access Authorization Error</h2>
          <p className="text-sm text-slate-500 mt-2">
            Your account role specification ({user.role}) is unrecognized by current gateway layers.
          </p>
          <button
            onClick={() => {
              clearSession();
              syncAuth();
            }}
            className="mt-4 px-4 py-2 bg-indigo-600 text-white rounded-lg text-xs font-semibold hover:bg-indigo-750 transition"
          >
            Clear Stale Session
          </button>
        </div>
      );
  }

  return <AppShell onSignOut={handleSignOut}>{dashboardView}</AppShell>;
}
