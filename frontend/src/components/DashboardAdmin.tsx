/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useId } from "react";
import { Users, ShieldCheck, UserMinus, Plus, Bell, Trash2, Calendar, ClipboardList, PenTool, Edit3, Save, Layers, HeartPulse } from "lucide-react";
import {
  fetchUsers,
  updateUser,
  deleteUser,
  fetchDoctors,
  createDoctor,
  deleteDoctor,
  fetchPatients,
  fetchAppointments,
  cancelAppointment,
  createNotification,
  fetchAuditLogs,
} from "../api/client";
import { UserDTO, DoctorDTO, PatientDTO, AppointmentDTO, AuditLogDTO, UserRole, Gender } from "../types";
import { StatusBadge, LoadingState, EmptyState, ApiErrorAlert, ConfirmDialog } from "./CommonUI";

export const DashboardAdmin: React.FC = () => {
  const fieldIdPrefix = `admin-${useId()}`;
  const [users, setUsers] = useState<UserDTO[]>([]);
  const [doctors, setDoctors] = useState<DoctorDTO[]>([]);
  const [patients, setPatients] = useState<PatientDTO[]>([]);
  const [appointments, setAppointments] = useState<AppointmentDTO[]>([]);
  const [auditLogs, setAuditLogs] = useState<AuditLogDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  // Active sub-panels
  const [currentTab, setCurrentTab] = useState<"users" | "doctors" | "appointments" | "logs" | "notifier">("users");

  // Inline edits
  const [editingUserId, setEditingUserId] = useState<number | null>(null);
  const [editName, setEditName] = useState("");
  const [editPhone, setEditPhone] = useState("");
  const [editAddress, setEditAddress] = useState("");

  // Create Doctor
  const [selectedUserIdForDoc, setSelectedUserIdForDoc] = useState<string>("");
  const [docSpec, setDocSpec] = useState("General Medicine");
  const [docExp, setDocExp] = useState(5);
  const [docHours, setDocHours] = useState("09:00-17:00");

  // Broadcast notification
  const [notifTargetUserId, setNotifTargetUserId] = useState<string>("");
  const [notifMessage, setNotifMessage] = useState("");
  const [notifFeedback, setNotifFeedback] = useState("");

  // Confirms
  const [deletingUser, setDeletingUser] = useState<UserDTO | null>(null);

  const loadAllSystemData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [u, d, p, a, logs] = await Promise.all([
        fetchUsers(),
        fetchDoctors(),
        fetchPatients(),
        fetchAppointments(),
        fetchAuditLogs(),
      ]);
      setUsers(u);
      setDoctors(d);
      setPatients(p);
      setAppointments(a.reverse());
      setAuditLogs(logs);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAllSystemData();
  }, []);

  // Save Inline User Correction
  const handleSaveInlineUser = async (id: number) => {
    try {
      await updateUser(id, { name: editName, phoneNumber: editPhone, address: editAddress });
      setEditingUserId(null);
      loadAllSystemData();
    } catch (err) {
      setError(err);
    }
  };

  // Triggers User deletion
  const handleDeleteUserClick = (u: UserDTO) => {
    setDeletingUser(u);
  };

  const handleConfirmDeleteUser = async () => {
    if (!deletingUser) return;
    try {
      await deleteUser(deletingUser.id);
      setDeletingUser(null);
      loadAllSystemData();
    } catch (err) {
      setError(err);
    }
  };

  // Links doctor specification record
  const handleLinkDoctor = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedUserIdForDoc) {
      setError("Please select a valid user account with ROLE_DOCTOR.");
      return;
    }
    setError(null);
    try {
      await createDoctor({
        userId: parseInt(selectedUserIdForDoc),
        experience: docExp,
        availableHours: docHours,
        specialization: docSpec,
      });
      setSelectedUserIdForDoc("");
      loadAllSystemData();
    } catch (err) {
      setError(err);
    }
  };

  // Removes doctor profile
  const handleUnlinkDoctor = async (id: number) => {
    try {
      await deleteDoctor(id);
      loadAllSystemData();
    } catch (err) {
      setError(err);
    }
  };

  // Cancel Appointment
  const handleCancelApp = async (id: number) => {
    try {
      await cancelAppointment(id);
      loadAllSystemData();
    } catch (err) {
      setError(err);
    }
  };

  // Dispatches alert notification
  const handleDispatchNotification = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!notifTargetUserId || !notifMessage) {
      setError("Please fill in Target User and the required Message payload.");
      return;
    }
    setError(null);
    try {
      await createNotification({
        userId: parseInt(notifTargetUserId),
        message: notifMessage,
      });
      setNotifFeedback("System notification dispatched successfully.");
      setNotifMessage("");
      loadAllSystemData();
    } catch (err) {
      setError(err);
    }
  };

  if (loading) {
    return <LoadingState message="Loading your clinic workspace…" />;
  }

  // Pre-filter candidate users that can be linked to Doctors (must have ROLE_DOCTOR)
  const candidateDocs = users.filter((u) => u.role === "ROLE_DOCTOR");

  return (
    <div className="space-y-6">
      {/* ERROR CARD */}
      <ApiErrorAlert error={error} onDismiss={() => setError(null)} />

      {/* CONFIRM DELETE MODAL */}
      <ConfirmDialog
        isOpen={!!deletingUser}
        title="Delete account"
        message={`Are you sure you want to permanently delete user account ID: ${
          deletingUser ? deletingUser.id : ""
        } (${
          deletingUser ? deletingUser.email : ""
        })? This will also purge associated patient or doctor department records.`}
        confirmLabel="Delete account"
        cancelLabel="Keep account"
        isDestructive={true}
        onConfirm={handleConfirmDeleteUser}
        onCancel={() => setDeletingUser(null)}
      />

      <div className="mb-8"><p className="eyebrow mb-3">YOUR CLINIC, AT A GLANCE</p><h1 className="dashboard-title">A little clarity for your day.</h1><p className="text-sm text-slate-500 mt-2">Your people, appointments, and clinic activity. All together.</p></div>

      {/* ADMIN STATS SUMMARY CELLS */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="dashboard-card summary-card bg-white border border-slate-100 p-4 rounded-xl shadow-xs">
          <div className="flex justify-between items-center">
            <span className="text-sm font-semibold text-slate-500 uppercase">Total users</span>
            <Users className="w-4 h-4 text-teal-500" />
          </div>
          <span className="text-2xl font-semibold text-slate-900 block mt-1">{users.length}</span>
        </div>
        <div className="dashboard-card summary-card bg-white border border-slate-100 p-4 rounded-xl shadow-xs">
          <div className="flex justify-between items-center">
            <span className="text-sm font-semibold text-slate-500 uppercase">Doctors</span>
            <ShieldCheck className="w-4 h-4 text-emerald-500" />
          </div>
          <span className="text-2xl font-semibold text-slate-900 block mt-1">{doctors.length}</span>
        </div>
        <div className="dashboard-card summary-card bg-white border border-slate-100 p-4 rounded-xl shadow-xs">
          <div className="flex justify-between items-center">
            <span className="text-sm font-semibold text-slate-500 uppercase">Patients</span>
            <HeartPulse className="w-4 h-4 text-teal-600" />
          </div>
          <span className="text-2xl font-semibold text-slate-900 block mt-1">{patients.length}</span>
        </div>
        <div className="dashboard-card summary-card bg-white border border-slate-100 p-4 rounded-xl shadow-xs">
          <div className="flex justify-between items-center">
            <span className="text-sm font-semibold text-slate-500 uppercase">Appointments</span>
            <Calendar className="w-4 h-4 text-sky-500" />
          </div>
          <span className="text-2xl font-semibold text-slate-900 block mt-1">{appointments.length}</span>
        </div>
      </div>

      {/* VIEW PANEL SELECTION MENU */}
      <div className="admin-tabs flex border-b border-slate-100 bg-white p-1 rounded-xl border flex-wrap gap-1">
        <button
          onClick={() => setCurrentTab("users")}
          className={`px-4 py-2 text-sm font-semibold rounded-lg cursor-pointer transition ${
            currentTab === "users" ? "bg-slate-900 text-white" : "text-slate-600 hover:bg-slate-50"
          }`}
        >
          People ({users.length})
        </button>
        <button
          onClick={() => setCurrentTab("doctors")}
          className={`px-4 py-2 text-sm font-semibold rounded-lg cursor-pointer transition ${
            currentTab === "doctors" ? "bg-slate-900 text-white" : "text-slate-600 hover:bg-slate-50"
          }`}
        >
          Doctors ({doctors.length})
        </button>
        <button
          onClick={() => setCurrentTab("appointments")}
          className={`px-4 py-2 text-sm font-semibold rounded-lg cursor-pointer transition ${
            currentTab === "appointments" ? "bg-slate-900 text-white" : "text-slate-600 hover:bg-slate-50"
          }`}
        >
          Appointments ({appointments.length})
        </button>
        <button
          onClick={() => setCurrentTab("logs")}
          className={`px-4 py-2 text-sm font-semibold rounded-lg cursor-pointer transition ${
            currentTab === "logs" ? "bg-slate-900 text-white" : "text-slate-600 hover:bg-slate-50"
          }`}
        >
          Audit logs ({auditLogs.length})
        </button>
        <button
          onClick={() => setCurrentTab("notifier")}
          className={`px-4 py-2 text-sm font-semibold rounded-lg cursor-pointer transition ${
            currentTab === "notifier" ? "bg-slate-900 text-white" : "text-slate-600 hover:bg-slate-50"
          }`}
        >
          Notifications
        </button>
      </div>

      <div className="dashboard-card bg-white border border-slate-100 rounded-xl p-6 shadow-xs">
        {/* PANEL A: USER REGISTRY */}
        {currentTab === "users" && (
          <div className="space-y-4">
            <div className="flex items-center justify-between pb-2 border-b border-slate-100">
              <h3 className="text-sm font-semibold text-slate-950">Your clinic community</h3>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-slate-500  font-semibold">
                    <th className="py-3 px-2">Account ID</th>
                    <th className="py-3 px-2">Name</th>
                    <th className="py-3 px-2">Email</th>
                    <th className="py-3 px-2">Phone</th>
                    <th className="py-3 px-2">Role</th>
                    <th className="py-3 px-2">Date of Birth</th>
                    <th className="py-3 px-2 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-50">
                  {users.map((u) => {
                    const isEditing = editingUserId === u.id;
                    return (
                      <tr key={u.id} className="hover:bg-slate-50/40">
                        <td className="py-3 px-2 font-mono text-slate-400">#{u.id}</td>
                        <td className="py-3 px-2">
                          {isEditing ? (
                            <input
                              id={`${fieldIdPrefix}-user-${u.id}-name`}
                              aria-label={`Name for user ${u.id}`}
                              type="text"
                              value={editName}
                              onChange={(e) => setEditName(e.target.value)}
                              className="px-2 py-1 border rounded text-sm focus:ring-1 focus:ring-teal-500 focus:outline-none"
                            />
                          ) : (
                            <span className="font-semibold text-slate-800">{u.name}</span>
                          )}
                        </td>
                        <td className="py-3 px-2 font-medium text-slate-600">{u.email}</td>
                        <td className="py-3 px-2 text-slate-700">
                          {isEditing ? (
                            <input
                              id={`${fieldIdPrefix}-user-${u.id}-phone`}
                              aria-label={`Phone for user ${u.id}`}
                              type="text"
                              value={editPhone}
                              onChange={(e) => setEditPhone(e.target.value)}
                              className="px-2 py-1 border rounded text-sm focus:ring-1 focus:ring-teal-500 focus:outline-none"
                            />
                          ) : (
                            u.phoneNumber
                          )}
                        </td>
                        <td className="py-3 px-2">
                          <span
                            className={`px-2 py-0.5 rounded text-xs font-semibold ${
                              u.role === "ROLE_ADMIN"
                                ? "bg-rose-50 text-rose-700"
                                : u.role === "ROLE_DOCTOR"
                                ? "bg-purple-50 text-purple-700"
                                : "bg-sky-50 text-sky-700"
                            }`}
                          >
                            {u.role.replace("ROLE_", "")}
                          </span>
                        </td>
                        <td className="py-3 px-2 text-slate-500">{u.dateOfBirth}</td>
                        <td className="py-3 px-2 text-right">
                          <div className="flex items-center justify-end gap-2">
                            {isEditing ? (
                              <button
                                onClick={() => handleSaveInlineUser(u.id)}
                                className="p-1 text-emerald-600 hover:text-emerald-800 transition"
                                title="Save Profile Update"
                              >
                                <Save className="w-4 h-4" />
                              </button>
                            ) : (
                              <button
                                onClick={() => {
                                  setEditingUserId(u.id);
                                  setEditName(u.name);
                                  setEditPhone(u.phoneNumber);
                                  setEditAddress(u.address);
                                }}
                                className="p-1 text-slate-400 hover:text-teal-600 transition"
                                title="Edit Profile Details"
                              >
                                <Edit3 className="w-4 h-4" />
                              </button>
                            )}

                            {u.role !== "ROLE_ADMIN" && (
                              <button
                                onClick={() => handleDeleteUserClick(u)}
                                className="p-1 text-slate-400 hover:text-rose-600 transition"
                                title="Delete User Account"
                              >
                                <UserMinus className="w-4 h-4" />
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* PANEL B: MANAGING DIRECT DOCTOR LINKINGS */}
        {currentTab === "doctors" && (
          <div className="space-y-6">
            <div className="pb-2 border-b border-slate-100 flex items-center justify-between">
              <h3 className="text-sm font-semibold text-slate-950">Your doctors</h3>
            </div>

            {/* REGISTER SPECIALTY ACCREDITATION */}
            <form onSubmit={handleLinkDoctor} className="p-4 bg-slate-50 border border-slate-100 rounded-xl space-y-3">
              <span className="font-semibold text-slate-800 text-xs  block">
                Add a doctor profile
              </span>
              <div className="grid grid-cols-1 md:grid-cols-4 gap-3">
                <div>
                  <label htmlFor={`${fieldIdPrefix}-doctor-account`} className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                    Doctor account
                  </label>
                  <select
                    id={`${fieldIdPrefix}-doctor-account`}
                    value={selectedUserIdForDoc}
                    onChange={(e) => setSelectedUserIdForDoc(e.target.value)}
                    className="w-full px-3 py-1.5 border border-slate-200 rounded text-sm focus:outline-none bg-white"
                  >
                    <option value="" disabled>-- Choose User --</option>
                    {candidateDocs
                      .filter((cu) => !doctors.some((d) => d.userId === cu.id)) // filter unlinked doctors only
                      .map((cu) => (
                        <option key={cu.id} value={cu.id}>
                          {cu.name} ({cu.email})
                        </option>
                      ))}
                  </select>
                </div>

                <div>
                  <label htmlFor={`${fieldIdPrefix}-doctor-specialization`} className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                    Specialization
                  </label>
                  <select
                    id={`${fieldIdPrefix}-doctor-specialization`}
                    value={docSpec}
                    onChange={(e) => setDocSpec(e.target.value)}
                    className="w-full px-3 py-1.5 border border-slate-200 rounded text-sm focus:outline-none bg-white"
                  >
                    <option value="Cardiology">Cardiology</option>
                    <option value="Pediatrics">Pediatrics</option>
                    <option value="Dermatology">Dermatology</option>
                    <option value="General Medicine">General Medicine</option>
                    <option value="Neurology">Neurology</option>
                  </select>
                </div>

                <div>
                  <label htmlFor={`${fieldIdPrefix}-doctor-experience`} className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                    Experience (Years)
                  </label>
                  <input
                    id={`${fieldIdPrefix}-doctor-experience`}
                    type="number"
                    min={0}
                    value={docExp}
                    onChange={(e) => setDocExp(parseInt(e.target.value))}
                    className="w-full px-3 py-1.5 border border-slate-200 rounded text-sm focus:outline-none bg-white"
                  />
                </div>

                <div>
                  <label htmlFor={`${fieldIdPrefix}-doctor-office-hours`} className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                    Office hours
                  </label>
                  <input
                    id={`${fieldIdPrefix}-doctor-office-hours`}
                    type="text"
                    value={docHours}
                    onChange={(e) => setDocHours(e.target.value)}
                    className="w-full px-3 py-1.5 border border-slate-200 rounded text-sm focus:outline-none bg-white"
                  />
                </div>
              </div>

              <div className="flex justify-end pt-2">
                <button
                  type="submit"
                  disabled={!selectedUserIdForDoc}
                  className="px-4 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded text-sm font-semibold cursor-pointer transition disabled:bg-slate-300"
                >
                  Add doctor
                </button>
              </div>
            </form>

            {/* DOCTOR TABLES REGISTER */}
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="border-b border-slate-100 text-slate-500  font-semibold">
                    <th className="py-3 px-2">Doctor ID</th>
                    <th className="py-3 px-2">Doctor</th>
                    <th className="py-3 px-2">Specialty</th>
                    <th className="py-3 px-2">Experience</th>
                    <th className="py-3 px-2">Office Hours</th>
                    <th className="py-3 px-2 text-right">Actions</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {doctors.map((d) => (
                    <tr key={d.id} className="hover:bg-slate-50/40">
                      <td className="py-3 px-2 font-mono text-slate-400">#HM-D-{d.id}</td>
                      <td className="py-3 px-2">
                        <span className="font-semibold text-slate-800">Dr. {d.name}</span>
                        <span className="block text-xs text-slate-400">Assoc. User ID #{d.userId}</span>
                      </td>
                      <td className="py-3 px-2 text-slate-700 font-semibold">{d.specialization}</td>
                      <td className="py-3 px-2 text-slate-500">{d.experience} Years active</td>
                      <td className="py-3 px-2 font-mono text-slate-600">{d.availableHours}</td>
                      <td className="py-3 px-2 text-right">
                        <button
                          onClick={() => handleUnlinkDoctor(d.id)}
                          className="p-1.5 text-slate-400 hover:text-rose-600 cursor-pointer transition"
                          title="Unlink Specialization Profile"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* PANEL C: GLOBAL APPOINTMENT SUPERVISION */}
        {currentTab === "appointments" && (
          <div className="space-y-4">
            <h3 className="text-sm font-semibold text-slate-950 pb-2 border-b border-slate-100">
              Appointments
            </h3>

            {appointments.length === 0 ? (
              <EmptyState title="No appointments recorded." />
            ) : (
              <div className="overflow-x-auto text-xs">
                <table className="w-full text-left border-collapse">
                  <thead>
                    <tr className="border-b border-slate-100 text-slate-500  font-semibold">
                      <th className="py-3 px-2">Confirmation Code</th>
                      <th className="py-3 px-2">Date & Hour Block</th>
                      <th className="py-3 px-2">Patient Index</th>
                      <th className="py-3 px-2">Practitioner ID</th>
                      <th className="py-3 px-2">Status</th>
                      <th className="py-3 px-2 text-right">Override Actions</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100">
                    {appointments.map((a) => (
                      <tr key={a.id} className="hover:bg-slate-50/40">
                        <td className="py-3 px-2 font-mono font-semibold text-slate-800">{a.appointmentCode}</td>
                        <td className="py-3 px-2 text-slate-600 font-medium">
                          {new Date(a.appointmentDateTime).toLocaleString()}
                        </td>
                        <td className="py-3 px-2">Patient ID #{a.patientId}</td>
                        <td className="py-3 px-2">Doctor ID #{a.doctorId}</td>
                        <td className="py-3 px-2">
                          <StatusBadge status={a.status} />
                        </td>
                        <td className="py-3 px-2 text-right">
                          <div className="flex items-center justify-end gap-1.5">
                            {(a.status === "SCHEDULED" || a.status === "CONFIRMED") && (
                              <button
                                onClick={() => handleCancelApp(a.id)}
                                className="px-2 py-1 bg-rose-50 border border-rose-100 hover:bg-rose-100 text-rose-700 rounded text-xs font-semibold cursor-pointer transition"
                              >
                                Cancel session
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        )}

        {/* PANEL D: AUDIT LOGS OVERVIEW */}
        {currentTab === "logs" && (
          <div className="space-y-4">
            <h3 className="text-sm font-semibold text-slate-950 pb-2 border-b border-slate-100">
              Activity log
            </h3>
            <p className="text-sm text-slate-500">
              Review account activity, appointments, and changes made in your clinic.
            </p>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm font-mono">
                <thead>
                  <tr className="border-b border-slate-100 text-slate-500  font-semibold">
                    <th className="py-3 px-2">Audit ID</th>
                    <th className="py-3 px-2">Action / Event</th>
                    <th className="py-3 px-2">User</th>
                    <th className="py-3 px-2">Details</th>
                    <th className="py-3 px-2">Recorded Timestamp</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {auditLogs.map((log) => (
                    <tr key={log.id} className="hover:bg-slate-50 text-slate-700">
                      <td className="py-2.5 px-2 text-slate-400">#LOG-{log.id}</td>
                      <td className="py-2.5 px-2 font-semibold text-teal-700">{log.action}</td>
                      <td className="py-2.5 px-2 font-semibold">
                        {log.userId ? `User ID #${log.userId}` : "SYSTEM PRINCIPAL"}
                      </td>
                      <td className="py-2.5 px-2 text-slate-600 font-sans max-w-sm overflow-hidden text-ellipsis whitespace-nowrap">
                        {log.details}
                      </td>
                      <td className="py-2.5 px-2 text-slate-500 text-xs">
                        {log.timestamp ? new Date(log.timestamp).toLocaleString() : ""}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}

        {/* PANEL E: NOTIFICATION BROADCASTER */}
        {currentTab === "notifier" && (
          <div className="space-y-4 max-w-xl">
            <h3 className="text-sm font-semibold text-slate-950 pb-2 border-b border-slate-100">
              Send a notification
            </h3>

            {notifFeedback && (
              <div className="p-3 bg-emerald-50 border border-emerald-100 rounded text-emerald-800 text-sm font-semibold">
                {notifFeedback}
              </div>
            )}

            <form onSubmit={handleDispatchNotification} className="space-y-4">
              <div>
                <label htmlFor={`${fieldIdPrefix}-notification-recipient`} className="block text-sm font-semibold text-slate-600 uppercase mb-1">
                  Recipient
                </label>
                <select
                  id={`${fieldIdPrefix}-notification-recipient`}
                  value={notifTargetUserId}
                  onChange={(e) => setNotifTargetUserId(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-teal-500 bg-white"
                >
                  <option value="" disabled>-- Choose a recipient --</option>
                  {users.map((u) => (
                    <option key={u.id} value={u.id}>
                      #{u.id} - {u.name} (Role: {u.role.replace("ROLE_", "")})
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label htmlFor={`${fieldIdPrefix}-notification-message`} className="block text-sm font-semibold text-slate-600 uppercase mb-1">
                  Message
                </label>
                <textarea
                  id={`${fieldIdPrefix}-notification-message`}
                  rows={4}
                  value={notifMessage}
                  required
                  placeholder="Enter medical notification alert regarding scheduled queue systems..."
                  onChange={(e) => setNotifMessage(e.target.value)}
                  className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-teal-500"
                />
              </div>

              <button
                type="submit"
                disabled={!notifTargetUserId || !notifMessage}
                className="inline-flex items-center gap-1.5 px-4 py-2 text-white bg-teal-600 hover:bg-teal-700 transition rounded-lg text-sm font-semibold cursor-pointer disabled:bg-slate-300"
              >
                <Bell className="w-4 h-4" />
                Send notification
              </button>
            </form>
          </div>
        )}
      </div>
    </div>
  );
};
