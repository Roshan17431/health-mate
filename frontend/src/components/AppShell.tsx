/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from "react";
import {
  Bell,
  LogOut,
  User,
  Activity,
  Heart,
  Database,
  Eye,
  Shield,
  Stethoscope,
  Menu,
  X,
  Settings,
  Check,
  Smartphone,
  Globe,
} from "lucide-react";
import {
  getSessionUser,
  clearSession,
  getApiMode,
  setApiMode,
  fetchUnreadNotificationsByUserId,
  markNotificationAsRead,
  fetchNotificationsByUserId,
  getApiUrl,
} from "../api/client";
import { UserDTO, NotificationDTO } from "../types";

interface AppShellProps {
  onSignOut: () => void;
  children: React.ReactNode;
}

export const AppShell: React.FC<AppShellProps> = ({ onSignOut, children }) => {
  const [user, setUser] = useState<UserDTO | null>(null);
  const [apiMode, setApiModeState] = useState<"demo" | "live">("demo");
  const [notifications, setNotifications] = useState<NotificationDTO[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);

  // Popover controls
  const [showNotifMenu, setShowNotifMenu] = useState(false);
  const [showConfigMenu, setShowConfigMenu] = useState(false);
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);

  const loadUserAndNotifs = async () => {
    const curUser = getSessionUser();
    setUser(curUser);
    setApiModeState(getApiMode());

    if (curUser) {
      try {
        const list = await fetchNotificationsByUserId(curUser.id);
        setNotifications(list);
        setUnreadCount(list.filter((n) => !n.isRead).length);
      } catch (err) {
        console.error("Failed loading notifications for user shell", err);
      }
    }
  };

  useEffect(() => {
    loadUserAndNotifs();

    // Attach local triggers for reactive updates
    const handleModeChange = () => {
      setApiModeState(getApiMode());
      loadUserAndNotifs();
    };

    window.addEventListener("healthmate_mode_changed", handleModeChange);
    window.addEventListener("healthmate_auth_changed", loadUserAndNotifs);

    return () => {
      window.removeEventListener("healthmate_mode_changed", handleModeChange);
      window.removeEventListener("healthmate_auth_changed", loadUserAndNotifs);
    };
  }, []);

  // Poll notifications periodically every 15 seconds to simulate real-time patient alerts / doctor scheduling confirmations
  useEffect(() => {
    let intervalId: any;
    if (user) {
      intervalId = setInterval(async () => {
        try {
          const list = await fetchNotificationsByUserId(user.id);
          setNotifications(list);
          setUnreadCount(list.filter((n) => !n.isRead).length);
        } catch (e) {
          // fail silent in polling
        }
      }, 15000);
    }
    return () => clearInterval(intervalId);
  }, [user]);

  // Handle Sign Out click
  const handleLogLevelClear = () => {
    clearSession();
    onSignOut();
  };

  // Switch API Client mode
  const handleToggleMode = (mode: "demo" | "live") => {
    setApiMode(mode);
    setShowConfigMenu(false);
    alert(`Swapped active system gateway context to: ${mode.toUpperCase()} MODE.`);
  };

  const handleMarkRead = async (id: number) => {
    try {
      await markNotificationAsRead(id);
      // Reload matching notif list
      if (user) {
        const list = await fetchNotificationsByUserId(user.id);
        setNotifications(list);
        setUnreadCount(list.filter((n) => !n.isRead).length);
      }
    } catch (e) {
      console.error(e);
    }
  };

  return (
    <div className="min-h-screen bg-slate-50/50 flex flex-col">
      {/* GLOBAL SYSTEM STATUS BANNER */}
      <div className={`text-white text-xs py-1.5 px-4 font-semibold flex items-center justify-between gap-4 select-none ${
        apiMode === "demo" ? "bg-amber-600" : "bg-blue-600"
      }`}>
        <div className="flex items-center gap-1.5">
          <Activity className="w-4 h-4 animate-pulse shrink-0" />
          <span className="tracking-wide">
            {apiMode === "demo"
              ? "SYSTEM GATEWAY: LOCAL SIMULATION PLATFORM (Fully sandbox compliant)"
              : "SYSTEM GATEWAY: LIVE OPERATIONS TARGET (Production connection ready)"}
          </span>
        </div>
        <button
          onClick={() => setShowConfigMenu(!showConfigMenu)}
          className="bg-white/10 hover:bg-white/20 px-2.5 py-0.5 rounded text-[10px] uppercase font-bold flex items-center gap-1 transition select-none tracking-wider shrink-0 cursor-pointer"
        >
          <Settings className="w-3.5 h-3.5" />
          Gateway Connection Settings
        </button>
      </div>

      {/* DETAILED SERVICE SWITCHER MODAL OVERLAY */}
      {showConfigMenu && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/70 backdrop-blur-xs">
          <div className="bg-white rounded-xl border border-slate-200 p-6 max-w-md w-full shadow-2xl animate-zoom-in">
            <h3 className="text-sm font-bold text-slate-800 border-b border-slate-100 pb-2.5 mb-4 flex items-center gap-1.5">
              <Database className="w-4.5 h-4.5 text-blue-600" />
              Gateway Connection Settings
            </h3>
            <p className="text-xs text-slate-500 mb-4 leading-relaxed">
              Health Mate connects natively to a Spring Boot REST API. You can switch between local simulation (ideal for immediate agent reviews and testing) and our live fetch API protocol.
            </p>

            <div className="space-y-3">
              <button
                onClick={() => handleToggleMode("demo")}
                type="button"
                className={`w-full p-4 rounded-xl text-left border flex items-center justify-between transition ${
                  apiMode === "demo"
                    ? "border-amber-500 bg-amber-50/25"
                    : "border-slate-200 hover:border-slate-300 bg-white"
                }`}
              >
                <div>
                  <span className="font-bold text-xs text-slate-900 block flex items-center gap-1">
                    🟢 Simulation Demo Sandbox
                  </span>
                  <span className="block text-[10px] text-slate-400 mt-1 font-medium leading-normal">
                    Pre-populated with active Patients, Doctors, Slots, schedules, notifications, and auditable HIPAA logs. No local server required.
                  </span>
                </div>
                {apiMode === "demo" && <Check className="w-5 h-5 text-amber-600 shrink-0 ml-2 animate-bounce" />}
              </button>

              <button
                onClick={() => handleToggleMode("live")}
                type="button"
                className={`w-full p-4 rounded-xl text-left border flex items-center justify-between transition ${
                  apiMode === "live"
                    ? "border-blue-500 bg-blue-50/25"
                    : "border-slate-200 hover:border-slate-300 bg-white"
                }`}
              >
                <div>
                  <span className="font-bold text-xs text-slate-900 block flex items-center gap-1">
                    🛜 Live EHR Target API
                  </span>
                  <span className="block text-[10px] text-slate-400 mt-1 font-medium leading-normal">
                    Proxies transaction bodies natively to your local REST base (default: http://localhost:8080). Requires live container or Spring launch.
                  </span>
                </div>
                {apiMode === "live" && <Check className="w-5 h-5 text-blue-600 shrink-0 ml-2 animate-bounce" />}
              </button>
            </div>

            <div className="mt-5 pt-3.5 border-t border-slate-100 flex justify-end">
              <button
                onClick={() => setShowConfigMenu(false)}
                className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white rounded-lg text-xs font-semibold cursor-pointer transition shadow-md"
              >
                Done
              </button>
            </div>
          </div>
        </div>
      )}

      {/* HEADER BAR */}
      <header className="bg-white border-b border-slate-200 sticky top-0 z-30 shadow-sm">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex justify-between h-16 items-center">
            {/* BRAND */}
            <div className="flex items-center gap-3">
              <div className="w-9 h-9 rounded-lg bg-blue-600 flex items-center justify-center text-white shadow-sm">
                <Heart className="w-5 h-5 animate-pulse" />
              </div>
              <div>
                <span className="font-bold text-slate-900 text-base tracking-tight font-sans">Health Mate</span>
                <span className="text-[9px] font-bold text-slate-400 uppercase tracking-widest block">
                  EHR Operations Gateway
                </span>
              </div>
            </div>

            {/* NAV MENU / RIGHT SECTION */}
            <div className="flex items-center gap-4">
              {user && (
                <div className="hidden md:flex items-center gap-2 px-3 py-1 bg-slate-50 border border-slate-200 rounded-lg">
                  <User className="w-4 h-4 text-slate-400" />
                  <span className="text-xs font-semibold text-slate-800">{user.name}</span>
                  <span className="text-[10px] font-bold bg-blue-50 text-blue-700 px-2.5 py-0.5 rounded capitalize">
                    {user.role.replace("ROLE_", "").toLowerCase()}
                  </span>
                </div>
              )}

              {/* REAL-TIME NOTIFICATIONS BADGE MENU */}
              {user && (
                <div className="relative">
                  <button
                    onClick={() => {
                      setShowNotifMenu(!showNotifMenu);
                      // dismiss connection setting if open
                      setShowConfigMenu(false);
                    }}
                    type="button"
                    className="p-2 text-slate-500 hover:text-blue-600 hover:bg-slate-50 rounded-lg transition relative shrink-0 cursor-pointer"
                  >
                    <Bell className="w-5 h-5 animate-pulse" />
                    {unreadCount > 0 && (
                      <span className="absolute top-1 right-1 w-4 h-4 bg-rose-600 text-[9px] font-bold text-white rounded-full flex items-center justify-center">
                        {unreadCount}
                      </span>
                    )}
                  </button>

                  {/* NOTIFICATION FLOATER SLIDE */}
                  {showNotifMenu && (
                    <div className="absolute right-0 mt-2 w-80 bg-white rounded-xl shadow-xl border border-slate-200 py-2 z-50 animate-zoom-in max-h-[380px] overflow-y-auto">
                      <div className="px-4 py-2 border-b border-slate-200 flex items-center justify-between">
                        <span className="text-xs font-bold text-slate-800">Clinic Alert Messages</span>
                        <span className="text-[10px] bg-blue-50 text-blue-700 px-2 py-0.5 rounded font-bold">
                          {unreadCount} Unread
                        </span>
                      </div>
                      <div className="divide-y divide-slate-100">
                        {notifications.length === 0 ? (
                          <div className="px-4 py-6 text-center text-xs text-slate-400">
                            No notifications active in system inbox.
                          </div>
                        ) : (
                          notifications.map((n) => (
                            <div
                              key={n.id}
                              className={`p-3 text-xs text-left cursor-pointer transition ${
                                !n.isRead ? "bg-blue-50/10 hover:bg-blue-50/20" : "hover:bg-slate-50"
                              }`}
                              onClick={() => !n.isRead && handleMarkRead(n.id)}
                            >
                              <p className="text-slate-800 font-medium">{n.message}</p>
                              <div className="flex justify-between items-center mt-1.5">
                                <span className="text-[9px] text-slate-400">
                                  {n.timestamp ? new Date(n.timestamp).toLocaleTimeString() : ""}
                                </span>
                                {!n.isRead && (
                                  <span className="text-[8px] font-bold text-blue-600 uppercase hover:underline">
                                    Mark as read
                                  </span>
                                )}
                              </div>
                            </div>
                          ))
                        )}
                      </div>
                    </div>
                  )}
                </div>
              )}

              {/* LOGOUT BUTTON */}
              {user && (
                <button
                  type="button"
                  onClick={handleLogLevelClear}
                  className="inline-flex items-center justify-center gap-1.5 px-3.5 py-2 cursor-pointer bg-slate-900 border border-slate-800 hover:bg-slate-800 font-bold text-xs text-white rounded-lg shadow-sm transition"
                >
                  <LogOut className="w-4 h-4" />
                  Sign Out
                </button>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* CORE FRAME LAYOUT STAGE */}
      <main className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8 animate-fade-in">
        {children}
      </main>

      {/* FOOTER METRICS CARD */}
      <footer className="bg-white border-t border-slate-200 py-5 mt-12 text-center text-xs text-slate-400">
        <div className="max-w-7xl mx-auto px-4 flex flex-col md:flex-row items-center justify-between gap-4">
          <p>© 2026 Health Mate Gateway. All rights reserved.</p>
          <div className="flex items-center gap-3">
            <span className="flex items-center gap-1 font-mono text-[10px]">
              <Globe className="w-3.5 h-3.5 text-slate-400" />
              API: {getApiUrl()}
            </span>
          </div>
        </div>
      </footer>
    </div>
  );
};
