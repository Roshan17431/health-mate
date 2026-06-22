/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React from "react";
import { AlertCircle, CheckCircle, Loader2, Info } from "lucide-react";
import { AppointmentStatus, SlotStatus } from "../types";

// --------------------
// STATUS BADGES
// --------------------
interface StatusBadgeProps {
  status: AppointmentStatus | SlotStatus | string;
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status }) => {
  let bgClass = "bg-slate-100 text-slate-800 border-slate-200";
  let dotClass = "bg-slate-400";

  switch (status) {
    case "SCHEDULED":
      bgClass = "bg-sky-50 text-sky-700 border-sky-150";
      dotClass = "bg-sky-500";
      break;
    case "CONFIRMED":
      bgClass = "bg-emerald-50 text-emerald-700 border-emerald-150";
      dotClass = "bg-emerald-500";
      break;
    case "COMPLETED":
      bgClass = "bg-slate-50 text-slate-600 border-slate-250";
      dotClass = "bg-slate-400";
      break;
    case "CANCELED":
      bgClass = "bg-rose-50 text-rose-700 border-rose-150";
      dotClass = "bg-rose-500";
      break;
    case "AVAILABLE":
      bgClass = "bg-emerald-50 text-emerald-700 border-emerald-150";
      dotClass = "bg-emerald-500";
      break;
    case "BOOKED":
      bgClass = "bg-indigo-50 text-indigo-700 border-indigo-150";
      dotClass = "bg-indigo-500";
      break;
    case "BLOCKED":
      bgClass = "bg-amber-50 text-amber-700 border-amber-150";
      dotClass = "bg-amber-500";
      break;
  }

  return (
    <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 text-xs font-medium rounded-full border ${bgClass}`}>
      <span className={`w-1.5 h-1.5 rounded-full ${dotClass}`}></span>
      <span className="capitalize">{status.toLowerCase()}</span>
    </span>
  );
};

// --------------------
// LOADING STATE
// --------------------
interface LoadingStateProps {
  message?: string;
  id?: string;
}

export const LoadingState: React.FC<LoadingStateProps> = ({ message = "Loading healthcare data...", id }) => {
  return (
    <div id={id} className="flex flex-col items-center justify-center p-12 text-center">
      <Loader2 className="w-8 h-8 text-indigo-600 animate-spin mb-3" />
      <p className="text-sm font-medium text-slate-500">{message}</p>
    </div>
  );
};

// --------------------
// EMPTY STATE
// --------------------
interface EmptyStateProps {
  title?: string;
  description?: string;
  actionLabel?: string;
  onAction?: () => void;
  id?: string;
}

export const EmptyState: React.FC<EmptyStateProps> = ({
  title = "No records found",
  description = "There are no entries available matching your filters.",
  actionLabel,
  onAction,
  id,
}) => {
  return (
    <div id={id} className="flex flex-col items-center justify-center p-10 text-center border border-dashed border-slate-200 rounded-xl bg-slate-50/20 max-w-lg mx-auto my-6">
      <Info className="w-10 h-10 text-slate-300 mb-3" />
      <h3 className="text-sm font-semibold text-slate-800 mb-1">{title}</h3>
      <p className="text-xs text-slate-500 mb-4">{description}</p>
      {actionLabel && onAction && (
        <button
          onClick={onAction}
          className="inline-flex items-center justify-center px-3 py-1.5 text-xs font-semibold text-white bg-indigo-600 hover:bg-indigo-700 transition rounded-lg"
        >
          {actionLabel}
        </button>
      )}
    </div>
  );
};

// --------------------
// API ERROR ALERT
// --------------------
interface ApiErrorAlertProps {
  error: unknown;
  onDismiss?: () => void;
  id?: string;
}

export const ApiErrorAlert: React.FC<ApiErrorAlertProps> = ({ error, onDismiss, id }) => {
  if (!error) return null;

  let message = "An unexpected error occurred. Please try again.";
  if (typeof error === "string") {
    message = error;
  } else if (typeof error === "object") {
    const errObj = error as Record<string, unknown>;
    if (typeof errObj.message === "string" && errObj.message.trim()) {
      message = errObj.message;
    } else if (typeof errObj.error === "string" && errObj.error.trim()) {
      message = errObj.error;
    }
  }

  return (
    <div id={id} className="flex items-start gap-3 p-4 bg-rose-50 border border-rose-100 rounded-lg text-rose-800 text-sm my-3 animate-fade-in">
      <AlertCircle className="w-5 h-5 text-rose-500 shrink-0 mt-0.5" />
      <div className="flex-1">
        <p className="font-semibold text-rose-900">Operation Failed</p>
        <p className="text-rose-700 mt-0.5">{message}</p>
      </div>
      {onDismiss && (
        <button
          type="button"
          onClick={onDismiss}
          className="text-rose-400 hover:text-rose-600 font-medium text-xs transition"
        >
          Dismiss
        </button>
      )}
    </div>
  );
};

// --------------------
// CONFIRMATION DIALOG
// --------------------
interface ConfirmDialogProps {
  isOpen: boolean;
  title: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
  isDestructive?: boolean;
  onConfirm: () => void;
  onCancel: () => void;
  id?: string;
}

export const ConfirmDialog: React.FC<ConfirmDialogProps> = ({
  isOpen,
  title,
  message,
  confirmLabel = "Confirm",
  cancelLabel = "Cancel",
  isDestructive = false,
  onConfirm,
  onCancel,
  id,
}) => {
  if (!isOpen) return null;

  return (
    <div id={id} className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs">
      <div className="w-full max-w-md bg-white rounded-xl shadow-xl border border-slate-100 p-6 animate-zoom-in">
        <h3 className="text-base font-semibold text-slate-950 mb-2">{title}</h3>
        <p className="text-sm text-slate-600 mb-6 leading-relaxed">{message}</p>
        <div className="flex items-center justify-end gap-3">
          <button
            onClick={onCancel}
            className="px-4 py-2 text-sm font-semibold text-slate-700 hover:bg-slate-100 transition rounded-lg"
          >
            {cancelLabel}
          </button>
          <button
            onClick={onConfirm}
            className={`px-4 py-2 text-sm font-semibold text-white rounded-lg transition ${
              isDestructive ? "bg-rose-600 hover:bg-rose-700" : "bg-indigo-600 hover:bg-indigo-700"
            }`}
          >
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
};

// --------------------
// DOCTOR SPECIALIZATION HERO ICONS
// --------------------
export function getSpecColor(specialization: string): { bg: string; text: string } {
  const s = specialization.toLowerCase();
  if (s.includes("cardio")) return { bg: "bg-rose-50", text: "text-rose-600" };
  if (s.includes("pedia")) return { bg: "bg-sky-50", text: "text-sky-600" };
  if (s.includes("derma")) return { bg: "bg-emerald-50", text: "text-emerald-600" };
  if (s.includes("neuro")) return { bg: "bg-indigo-50", text: "text-indigo-600" };
  return { bg: "bg-slate-50", text: "text-slate-600" };
}
