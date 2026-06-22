/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from "react";
import { List, Calendar, Clock, RotateCcw, ShieldCheck, UserCheck, Check, Ban, Plus, Trash2, Tag, Stethoscope } from "lucide-react";
import {
  fetchDoctors,
  fetchAppointmentsByDoctor,
  fetchPatients,
  updateAppointmentStatus,
  fetchDoctorSchedules,
  createDoctorSchedule,
  deleteDoctorSchedule,
  fetchTimeSlotsByDate,
  createTimeSlot,
  deleteTimeSlot,
} from "../api/client";
import { UserDTO, DoctorDTO, AppointmentDTO, PatientDTO, DoctorScheduleDTO, TimeSlotDTO, DayOfWeek, SlotStatus } from "../types";
import { StatusBadge, LoadingState, EmptyState, ApiErrorAlert, ConfirmDialog } from "./CommonUI";

interface DashboardDoctorProps {
  user: UserDTO;
}

export const DashboardDoctor: React.FC<DashboardDoctorProps> = ({ user }) => {
  const [doctor, setDoctor] = useState<DoctorDTO | null>(null);
  const [appointments, setAppointments] = useState<AppointmentDTO[]>([]);
  const [patients, setPatients] = useState<PatientDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  // Schedules state
  const [schedules, setSchedules] = useState<DoctorScheduleDTO[]>([]);
  const [schedDay, setSchedDay] = useState<DayOfWeek>("MONDAY");
  const [schedStart, setSchedStart] = useState("09:00:00");
  const [schedEnd, setSchedEnd] = useState("17:00:00");

  // Slots state
  const [selectedSlotDate, setSelectedSlotDate] = useState<string>("");
  const [existingSlots, setExistingSlots] = useState<TimeSlotDTO[]>([]);
  const [newSlotStart, setNewSlotStart] = useState("09:00:00");
  const [newSlotEnd, setNewSlotEnd] = useState("09:30:00");

  const loadDoctorProfileAndData = async () => {
    setLoading(true);
    setError(null);
    try {
      const allDoctors = await fetchDoctors();
      let matchedDoc = allDoctors.find((d) => d.userId === user.id);

      if (!matchedDoc) {
        // Fallback simulate creation if not exists
        const fallbackId = allDoctors.length + 1;
        matchedDoc = {
          id: fallbackId,
          experience: 8,
          availableHours: "09:00-17:00",
          specialization: "General Medicine",
          userId: user.id,
          name: user.name,
          email: user.email,
          phone: user.phoneNumber,
        };
      }

      setDoctor(matchedDoc);

      const [appsList, patsList, schedsList] = await Promise.all([
        fetchAppointmentsByDoctor(matchedDoc.id),
        fetchPatients(),
        fetchDoctorSchedules(matchedDoc.id),
      ]);

      setAppointments(appsList.reverse());
      setPatients(patsList);
      setSchedules(schedsList);

      // Pre-set default Slot Date
      const today = new Date();
      setSelectedSlotDate(today.toISOString().split("T")[0]);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDoctorProfileAndData();
  }, [user.id]);

  // Load Time slots side-effect
  useEffect(() => {
    if (doctor && selectedSlotDate) {
      const loadSlots = async () => {
        try {
          const slots = await fetchTimeSlotsByDate(doctor.id, selectedSlotDate);
          setExistingSlots(slots);
        } catch (err) {
          console.error("Failed loading slots", err);
        }
      };
      loadSlots();
    }
  }, [doctor, selectedSlotDate]);

  // Handle status update
  const handleUpdateStatus = async (appId: number, status: "CONFIRMED" | "COMPLETED" | "CANCELED") => {
    try {
      await updateAppointmentStatus(appId, status);
      const list = await fetchAppointmentsByDoctor(doctor!.id);
      setAppointments(list.reverse());
    } catch (err) {
      setError(err);
    }
  };

  // Create Recurring Schedule
  const handleCreateSchedule = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!doctor) return;
    try {
      await createDoctorSchedule({
        doctorId: doctor.id,
        dayOfWeek: schedDay,
        startTime: schedStart,
        endTime: schedEnd,
        isActive: true,
      });
      // Refresh
      const scheds = await fetchDoctorSchedules(doctor.id);
      setSchedules(scheds);
    } catch (err) {
      setError(err);
    }
  };

  // Delete Recurring Schedule
  const handleDeleteSchedule = async (id: number) => {
    try {
      await deleteDoctorSchedule(id);
      setSchedules(schedules.filter((s) => s.id !== id));
    } catch (err) {
      setError(err);
    }
  };

  // Create Concrete Slot
  const handleCreateSlotObj = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!doctor || !selectedSlotDate) return;
    setError(null);
    try {
      await createTimeSlot({
        doctorId: doctor.id,
        slotDate: selectedSlotDate,
        startTime: newSlotStart,
        endTime: newSlotEnd,
      });
      // Refresh
      const slots = await fetchTimeSlotsByDate(doctor.id, selectedSlotDate);
      setExistingSlots(slots);
    } catch (err) {
      setError(err);
    }
  };

  // Delete Concrete Slot
  const handleDeleteSlotObj = async (id: number) => {
    try {
      await deleteTimeSlot(id);
      setExistingSlots(existingSlots.filter((ts) => ts.id !== id));
    } catch (err) {
      setError(err);
    }
  };

  if (loading) {
    return <LoadingState message="Connecting Diagnostics Database..." />;
  }

  return (
    <div className="space-y-6">
      {/* ERROR MSG BANNER */}
      <ApiErrorAlert error={error} onDismiss={() => setError(null)} />

      {/* DOCTOR PROFILE JUMBOTRON */}
      <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm flex flex-col md:flex-row md:items-center justify-between gap-4 animate-fade-in">
        <div className="flex items-center gap-4">
          <div className="w-12 h-12 rounded-xl bg-blue-50 flex items-center justify-center text-blue-600">
            <Stethoscope className="w-6 h-6" />
          </div>
          <div>
            <span className="text-[10px] font-bold tracking-wider text-blue-600 bg-blue-50 px-2.5 py-1 rounded">
              PRACTITIONER PORTAL
            </span>
            <h1 className="text-xl font-bold text-slate-900 mt-1">Dr. {doctor?.name}</h1>
            <p className="text-xs text-slate-500 font-medium">
              Specialization: {doctor?.specialization} | Experience: {doctor?.experience} Years | Dr. ID: #{doctor?.id}
            </p>
          </div>
        </div>
        <div className="flex gap-4 border-t pt-4 md:border-t-0 md:pt-0">
          <div className="bg-slate-50 border border-slate-100 p-2.5 rounded-lg text-center min-w-[100px]">
            <span className="text-xs font-semibold text-slate-500 block">Open Slots</span>
            <span className="text-xl font-bold text-emerald-600">
              {existingSlots.filter((ts) => ts.status === "AVAILABLE").length}
            </span>
          </div>
          <div className="bg-slate-50 border border-slate-100 p-2.5 rounded-lg text-center min-w-[100px]">
            <span className="text-xs font-semibold text-slate-500 block">Pending Reviews</span>
            <span className="text-xl font-bold text-sky-600">
              {appointments.filter((a) => a.status === "SCHEDULED").length}
            </span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* LEFT COMPONENT: PENDING DIAGNOSTIC ASSIGNMENTS */}
        <div className="lg:col-span-8 space-y-6">
          <div className="bg-white border border-slate-202 rounded-xl p-6 shadow-sm">
            <h2 className="text-sm font-bold text-slate-900 border-b border-slate-150 pb-3 mb-4">
              Assigned Patient Appointments
            </h2>

            {appointments.length === 0 ? (
              <EmptyState title="No active assignments" description="You have no patient diagnostic bookings recorded." />
            ) : (
              <div className="space-y-4">
                {appointments.map((app) => {
                  const pat = patients.find((p) => p.id === app.patientId);
                  return (
                    <div
                      key={app.id}
                      className="p-4 border border-slate-150 hover:border-slate-200 transition rounded-xl bg-slate-50/10 flex flex-col md:flex-row justify-between gap-4"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold bg-slate-100 px-2 py-0.5 rounded text-slate-700">
                            {app.appointmentCode}
                          </span>
                          <StatusBadge status={app.status} />
                        </div>
                        <h4 className="font-bold text-slate-900 text-sm mt-1">
                          {pat ? pat.name : `Registered Patient #${app.patientId}`}
                        </h4>
                        <div className="flex items-center gap-3 text-slate-500 text-[11px] font-medium pt-1">
                          <span className="flex items-center gap-1">
                            <Calendar className="w-3.5 h-3.5 text-slate-400" />
                            {new Date(app.appointmentDateTime).toLocaleDateString()}
                          </span>
                          <span className="flex items-center gap-1">
                            <Clock className="w-3.5 h-3.5 text-slate-400" />
                            {new Date(app.appointmentDateTime).toLocaleTimeString()}
                          </span>
                        </div>
                        {app.notes && (
                          <div className="text-xs text-slate-600 bg-slate-50/80 p-2 border border-slate-100 rounded mt-2">
                            <span className="font-semibold text-slate-500 block text-[10px] uppercase">
                              Patient Consult Notes
                            </span>
                            &quot;{app.notes}&quot;
                          </div>
                        )}
                      </div>

                      {/* WORKFLOW OPERATIONS */}
                      <div className="flex md:flex-col justify-end gap-2 shrink-0 md:w-36">
                        {app.status === "SCHEDULED" && (
                          <button
                            onClick={() => handleUpdateStatus(app.id, "CONFIRMED")}
                            className="w-full flex items-center justify-center gap-1 px-3 py-1.5 text-xs font-bold text-white bg-blue-600 hover:bg-blue-700 transition rounded-lg shadow-sm"
                          >
                            <UserCheck className="w-3.5 h-3.5" />
                            Confirm Booking
                          </button>
                        )}
                        {app.status === "CONFIRMED" && (
                          <button
                            onClick={() => handleUpdateStatus(app.id, "COMPLETED")}
                            className="w-full flex items-center justify-center gap-1 px-3 py-1.5 text-xs font-semibold text-white bg-emerald-600 hover:bg-emerald-700 transition rounded-lg"
                          >
                            <ShieldCheck className="w-3.5 h-3.5" />
                            Complete Visit
                          </button>
                        )}
                        {(app.status === "SCHEDULED" || app.status === "CONFIRMED") && (
                          <button
                            onClick={() => handleUpdateStatus(app.id, "CANCELED")}
                            className="w-full flex items-center justify-center gap-1 px-3 py-1.5 text-xs font-semibold text-rose-700 hover:bg-rose-50 border border-rose-200 transition rounded-lg"
                          >
                            <Ban className="w-3.5 h-3.5" />
                            Cancel Visit
                          </button>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* RIGHT COLUMN: RECURRING SCHEDULES & SLOTS ENGINE */}
        <div className="lg:col-span-4 space-y-6">
          {/* GENERATE CONCRETE DAILY SLOTS */}
          <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm">
            <h3 className="text-sm font-bold text-slate-900 border-b border-slate-150 pb-2.5 mb-3 flex items-center gap-1">
              <Calendar className="w-4 h-4 text-blue-600" />
              Slot Availability Engine
            </h3>

            <div className="space-y-3">
              <div>
                <label className="block text-[10px] uppercase font-bold text-slate-500 mb-1">Select Date</label>
                <input
                  type="date"
                  value={selectedSlotDate}
                  onChange={(e) => setSelectedSlotDate(e.target.value)}
                  className="w-full px-3 py-1.5 border border-slate-250 rounded-lg text-xs text-slate-800 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100 bg-white"
                />
              </div>

              {/* SLOTS LISTING */}
              <div className="border border-slate-100 rounded-lg bg-slate-50/50 p-2 text-xs">
                <span className="font-bold text-slate-700 block text-[10px] uppercase mb-1">Concrete Slots List</span>
                {existingSlots.length === 0 ? (
                  <p className="text-slate-400 py-2 text-center text-[11px]">No slots configured for this date.</p>
                ) : (
                  <div className="space-y-1.5 max-h-[140px] overflow-y-auto">
                    {existingSlots.map((ts) => (
                      <div
                        key={ts.id}
                        className="bg-white border border-slate-200 p-1.5 rounded flex items-center justify-between"
                      >
                        <span className="font-mono text-[11px] font-semibold text-slate-600">
                          {ts.startTime.substring(0, 5)} - {ts.endTime.substring(0, 5)}
                        </span>
                        <div className="flex items-center gap-1.5">
                          <span
                            className={`px-1.5 py-0.5 rounded text-[9px] font-bold uppercase ${
                              ts.status === "AVAILABLE"
                                ? "bg-emerald-50 text-emerald-700"
                                : "bg-blue-50 text-blue-700"
                            }`}
                          >
                            {ts.status}
                          </span>
                          <button
                            onClick={() => handleDeleteSlotObj(ts.id)}
                            className="text-slate-400 hover:text-rose-600 cursor-pointer transition"
                            title="Delete Slot"
                          >
                            <Trash2 className="w-3 h-3" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* GENERATOR FORM */}
              <form onSubmit={handleCreateSlotObj} className="border-t border-slate-100 pt-3 space-y-2">
                <span className="font-bold text-slate-700 block text-[10px] uppercase">Add Diagnostics Hour Block</span>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[9px] font-semibold text-slate-500 mb-0.5">Start Time</label>
                    <input
                      type="time"
                      value={newSlotStart}
                      onChange={(e) => setNewSlotStart(`${e.target.value}:00`)}
                      className="w-full px-2 py-1 border border-slate-200 rounded text-xs focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-[9px] font-semibold text-slate-500 mb-0.5">End Time</label>
                    <input
                      type="time"
                      value={newSlotEnd}
                      onChange={(e) => setNewSlotEnd(`${e.target.value}:00`)}
                      className="w-full px-2 py-1 border border-slate-200 rounded text-xs focus:outline-none"
                    />
                  </div>
                </div>
                <button
                  type="submit"
                  className="w-full flex items-center justify-center gap-1 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded text-xs font-semibold cursor-pointer transition"
                >
                  <Plus className="w-3.5 h-3.5" />
                  Generate Slot
                </button>
              </form>
            </div>
          </div>

          {/* RECURRING CALENDAR HOURS */}
          <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm">
            <h3 className="text-sm font-bold text-slate-900 border-b border-slate-150 pb-2.5 mb-3 flex items-center gap-1">
              <Clock className="w-4 h-4 text-blue-600" />
              Recurring Timetable Week
            </h3>

            <div className="space-y-3">
              <div className="space-y-1.5">
                {schedules.map((sch) => (
                  <div key={sch.id} className="bg-slate-50 p-2 rounded-lg border border-slate-100 flex items-center justify-between text-xs">
                    <div>
                      <span className="font-bold text-slate-700 text-[10px] uppercase block">{sch.dayOfWeek}</span>
                      <span className="font-mono text-[11px] text-slate-500">
                        {sch.startTime.substring(0, 5)} - {sch.endTime.substring(0, 5)}
                      </span>
                    </div>
                    <button
                      onClick={() => handleDeleteSchedule(sch.id)}
                      className="text-slate-400 hover:text-rose-600 cursor-pointer transition"
                    >
                      <Trash2 className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>

              {/* TIMETABLE ADD FORM */}
              <form onSubmit={handleCreateSchedule} className="border-t border-slate-100 pt-3 space-y-2">
                <span className="font-bold text-slate-700 block text-[10px] uppercase">Add Recurring Availability</span>
                <div>
                  <label className="block text-[9px] font-semibold text-slate-500 mb-0.5">Day of Week</label>
                  <select
                    value={schedDay}
                    onChange={(e) => setSchedDay(e.target.value as DayOfWeek)}
                    className="w-full px-2 py-1 border border-slate-200 rounded text-xs focus:outline-none bg-white"
                  >
                    <option value="MONDAY">MONDAY</option>
                    <option value="TUESDAY">TUESDAY</option>
                    <option value="WEDNESDAY">WEDNESDAY</option>
                    <option value="THURSDAY">THURSDAY</option>
                    <option value="FRIDAY">FRIDAY</option>
                    <option value="SATURDAY">SATURDAY</option>
                    <option value="SUNDAY">SUNDAY</option>
                  </select>
                </div>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[9px] font-semibold text-slate-500 mb-0.5">Start Time</label>
                    <input
                      type="time"
                      value={schedStart.substring(0, 5)}
                      onChange={(e) => setSchedStart(`${e.target.value}:00`)}
                      className="w-full px-2 py-1 border border-slate-200 rounded text-xs focus:outline-none"
                    />
                  </div>
                  <div>
                    <label className="block text-[9px] font-semibold text-slate-500 mb-0.5">End Time</label>
                    <input
                      type="time"
                      value={schedEnd.substring(0, 5)}
                      onChange={(e) => setSchedEnd(`${e.target.value}:00`)}
                      className="w-full px-2 py-1 border border-slate-200 rounded text-xs focus:outline-none"
                    />
                  </div>
                </div>
                <button
                  type="submit"
                  className="w-full flex items-center justify-center gap-1 py-1.5 bg-slate-800 hover:bg-slate-950 text-white rounded text-xs font-semibold cursor-pointer transition"
                >
                  <Plus className="w-3.5 h-3.5" />
                  Save timetables
                </button>
              </form>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
