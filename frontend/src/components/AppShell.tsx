/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from "react";
import { Bell, LogOut, X } from "lucide-react";
import { Brand } from "./Brand";
import {
  getSessionUser,
  clearSession,
  markNotificationAsRead,
  fetchNotificationsByUserId,
} from "../api/client";
import { UserDTO, NotificationDTO } from "../types";

interface AppShellProps {
  onSignOut: () => void;
  children: React.ReactNode;
}

export const AppShell: React.FC<AppShellProps> = ({ onSignOut, children }) => {
  const [user, setUser] = useState<UserDTO | null>(null);
  const [notifications, setNotifications] = useState<NotificationDTO[]>([]);
  const [unreadCount, setUnreadCount] = useState(0);

  // Popover controls
  const [showNotifMenu, setShowNotifMenu] = useState(false);

  const loadUserAndNotifs = async () => {
    const curUser = getSessionUser();
    setUser(curUser);

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

    window.addEventListener("healthmate_auth_changed", loadUserAndNotifs);

    return () => {
      window.removeEventListener("healthmate_auth_changed", loadUserAndNotifs);
    };
  }, []);

  // Poll notifications periodically so dashboard alerts stay current.
  useEffect(() => {
    let intervalId: ReturnType<typeof setInterval> | undefined;
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
    <div className="workspace">
      <a className="skip-link" href="#workspace-content">Skip to main content</a>
      <header className="workspace-header">
        <div className="workspace-header-inner">
          <div className="flex items-center"><Brand /><span className="workspace-role">Your care workspace</span></div>
          <div className="workspace-header-actions">
            {user && (
              <>
                <div className="relative">
                  <button type="button" className="icon-button" aria-label={`Notifications, ${unreadCount} unread`} aria-expanded={showNotifMenu} aria-controls="notification-panel" onClick={() => setShowNotifMenu(!showNotifMenu)}>
                    <Bell size={20} aria-hidden="true" />
                    {unreadCount > 0 && <span className="notification-count">{unreadCount > 99 ? "99+" : unreadCount}</span>}
                  </button>
                  {showNotifMenu && (
                    <section id="notification-panel" className="notification-menu" aria-label="Notifications">
                      <div className="flex items-center justify-between px-5 py-3"><div><h2 className="text-sm font-semibold">Notifications</h2><p className="text-xs text-slate-500 mt-1">{unreadCount} unread</p></div><button type="button" className="icon-button" aria-label="Close notifications" onClick={() => setShowNotifMenu(false)}><X size={17} /></button></div>
                      {notifications.length === 0 ? <p className="px-5 pb-7 pt-3 text-sm text-slate-500">You're all caught up. No notifications yet.</p> : notifications.map((n) => (
                        <button type="button" key={n.id} className={`notification-item ${!n.isRead ? "unread" : ""}`} onClick={() => !n.isRead && handleMarkRead(n.id)}>
                          <p className="text-slate-800">{n.message}</p>
                          <span className="flex justify-between gap-4 mt-2 text-xs text-slate-500"><span>{n.timestamp ? new Date(n.timestamp).toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" }) : ""}</span>{!n.isRead && <span className="text-teal-700 font-medium">Mark as read</span>}</span>
                        </button>
                      ))}
                    </section>
                  )}
                </div>
                <div className="workspace-profile"><span className="user-avatar" aria-hidden="true">{user.name.charAt(0).toUpperCase()}</span><div><strong>{user.name}</strong><small>{user.role.replace("ROLE_", "").toLowerCase()}</small></div></div>
                <button type="button" className="secondary-button flex items-center gap-2" aria-label="Sign out" onClick={handleLogLevelClear}><LogOut size={16} aria-hidden="true" /><span className="signout-label">Sign out</span></button>
              </>
            )}
          </div>
        </div>
      </header>
      <main id="workspace-content" className="workspace-main" tabIndex={-1}>{children}</main>
      <footer className="workspace-footer"><span>© {new Date().getFullYear()} Health Mate</span><span>A little more connected. A little more cared for.</span></footer>
    </div>
  );
};
