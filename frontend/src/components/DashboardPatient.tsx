/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect } from "react";
import { Search, Calendar, Clock, BookOpen, AlertCircle, Trash2, ShieldAlert, CheckCircle, Info, Stethoscope } from "lucide-react";
import {
  fetchPatientByUserId,
  fetchDoctors,
  fetchTimeSlotsByDate,
  bookAppointment,
  fetchAppointmentsByPatient,
  cancelAppointment,
} from "../api/client";
import { UserDTO, PatientDTO, DoctorDTO, TimeSlotDTO, AppointmentDTO } from "../types";
import { StatusBadge, LoadingState, EmptyState, ApiErrorAlert, ConfirmDialog, getSpecColor } from "./CommonUI";

interface DashboardPatientProps {
  user: UserDTO;
}

export const DashboardPatient: React.FC<DashboardPatientProps> = ({ user }) => {
  const [patient, setPatient] = useState<PatientDTO | null>(null);
  const [appointments, setAppointments] = useState<AppointmentDTO[]>([]);
  const [doctors, setDoctors] = useState<DoctorDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);

  // Booking Flow States
  const [selectedSpec, setSelectedSpec] = useState<string>("");
  const [selectedDoctor, setSelectedDoctor] = useState<DoctorDTO | null>(null);
  const [slotDate, setSlotDate] = useState<string>("");
  const [availableSlots, setAvailableSlots] = useState<TimeSlotDTO[]>([]);
  const [selectedSlot, setSelectedSlot] = useState<TimeSlotDTO | null>(null);
  const [consultNotes, setConsultNotes] = useState("");
  const [bookingStatus, setBookingStatus] = useState<AppointmentDTO | null>(null);
  const [isBookingLoading, setIsBookingLoading] = useState(false);

  // Active Cancellation Modal States
  const [cancelingApp, setCancelingApp] = useState<AppointmentDTO | null>(null);

  // Fetch Patient Profile and associated appointments and doctors
  const loadPatientData = async () => {
    setLoading(true);
    setError(null);
    try {
      const pProfile = await fetchPatientByUserId(user.id);
      setPatient(pProfile);

      const [appsList, docsList] = await Promise.all([
        fetchAppointmentsByPatient(pProfile.id),
        fetchDoctors(),
      ]);

      setAppointments(appsList.reverse());
      setDoctors(docsList);

      // Pre-set default Booking values
      if (docsList.length > 0) {
        setSelectedDoctor(docsList[0]);
        // Default calendar slot date to today plus 1 day
        const tomorrow = new Date();
        tomorrow.setDate(tomorrow.getDate() + 1);
        const yyyy = tomorrow.getFullYear();
        const mm = String(tomorrow.getMonth() + 1).padStart(2, "0");
        const dd = String(tomorrow.getDate()).padStart(2, "0");
        setSlotDate(`${yyyy}-${mm}-${dd}`);
      }
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadPatientData();
  }, [user.id]);

  // Load Time slots whenever Doctor or Slot Date changes
  useEffect(() => {
    if (selectedDoctor && slotDate) {
      const loadSlots = async () => {
        try {
          const slots = await fetchTimeSlotsByDate(selectedDoctor.id, slotDate, "AVAILABLE");
          setAvailableSlots(slots);
          setSelectedSlot(null);
        } catch (err) {
          console.error("Failed fetching prospective day-slots", err);
        }
      };
      loadSlots();
    }
  }, [selectedDoctor, slotDate]);

  // Initiates Booking Request
  const handleBookNow = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDoctor || !slotDate || !selectedSlot) {
      setError("Please select a Doctor, a preferred Date, and an AVAILABLE Time Slot.");
      return;
    }
    setError(null);
    setIsBookingLoading(true);
    try {
      const dt = `${slotDate}T${selectedSlot.startTime}`;
      const app = await bookAppointment({
        appointmentDateTime: dt,
        notes: consultNotes,
        doctorId: selectedDoctor.id,
        patientId: patient!.id,
        timeSlotId: selectedSlot.id,
      });

      setBookingStatus(app);
      setConsultNotes("");
      setSelectedSlot(null);

      // Reload appointments and available slots list
      const appsList = await fetchAppointmentsByPatient(patient!.id);
      setAppointments(appsList.reverse());

      // Refresh slots remaining
      const slots = await fetchTimeSlotsByDate(selectedDoctor.id, slotDate, "AVAILABLE");
      setAvailableSlots(slots);
    } catch (err) {
      setError(err);
    } finally {
      setIsBookingLoading(false);
    }
  };

  // Triggers Cancellation
  const handleCancelClick = (app: AppointmentDTO) => {
    setCancelingApp(app);
  };

  const handleConfirmCancellation = async () => {
    if (!cancelingApp) return;
    try {
      await cancelAppointment(cancelingApp.id);
      setCancelingApp(null);
      // Refresh current records
      const appsList = await fetchAppointmentsByPatient(patient!.id);
      setAppointments(appsList.reverse());
    } catch (err) {
      setError(err);
    }
  };

  if (loading) {
    return <LoadingState message="Retrieving Health Records & Portals..." />;
  }

  // Filter doctors by specialization query
  const filteredDoctors = selectedSpec
    ? doctors.filter((d) => d.specialization.toLowerCase().includes(selectedSpec.toLowerCase()))
    : doctors;

  // Filter unique specializations list for dropdown selector
  const availableSpecs = Array.from(new Set(doctors.map((d) => d.specialization))).filter(Boolean) as string[];

  return (
    <div className="space-y-6">
      {/* ERROR DISPLAY */}
      <ApiErrorAlert error={error} onDismiss={() => setError(null)} />

      {/* CONFIRM CANCELLATION MODAL */}
      <ConfirmDialog
        isOpen={!!cancelingApp}
        title="Cancel Healthcare Appointment"
        message={`Are you sure you want to cancel your upcoming session on ${
          cancelingApp
            ? new Date(cancelingApp.appointmentDateTime).toLocaleDateString()
            : ""
        }? This will return this time slot to available status.`}
        confirmLabel="Yes, Cancel Session"
        cancelLabel="Discard"
        isDestructive={true}
        onConfirm={handleConfirmCancellation}
        onCancel={() => setCancelingApp(null)}
      />

      {/* WELCOME HEADER HERO */}
      <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <span className="text-[10px] font-bold tracking-wider text-blue-600 bg-blue-50 px-2 py-1 rounded">
            PATIENT ACCESS PORTAL
          </span>
          <h1 className="text-xl font-bold text-slate-900 mt-2">Welcome Back, {user.name}</h1>
          <p className="text-xs text-slate-500 mt-1">
            Registered Email: {patient?.email} | Medical ID: HM-P-{patient?.id}
          </p>
        </div>
        <div className="flex gap-4 border-t pt-4 md:border-t-0 md:pt-0">
          <div className="bg-slate-50 border border-slate-100 p-2.5 rounded-lg text-center min-w-[100px]">
            <span className="text-xs font-semibold text-slate-500 block">Scheduled</span>
            <span className="text-xl font-bold text-slate-800">
              {appointments.filter((a) => a.status === "SCHEDULED").length}
            </span>
          </div>
          <div className="bg-slate-50 border border-slate-100 p-2.5 rounded-lg text-center min-w-[100px]">
            <span className="text-xs font-semibold text-slate-500 block">Confirmed</span>
            <span className="text-xl font-bold text-emerald-600">
              {appointments.filter((a) => a.status === "CONFIRMED").length}
            </span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* LEFT COLUMN: ACTIVE BOOKINGS MODULE */}
        <div className="lg:col-span-8 space-y-6">
          {/* APPOINTMENT BOOKING COMPONENT */}
          <div className="bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
            <div className="border-b border-slate-150 pb-3 mb-4 flex items-center justify-between">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <BookOpen className="w-4 h-4 text-blue-600" />
                Schedule Consult with Practitioner
              </h2>
            </div>

            {bookingStatus ? (
              // BOOKING COMPLETED SUCCESS CARD
              <div className="p-6 bg-emerald-50 rounded-xl border border-emerald-100 text-center animate-fade-in">
                <div className="w-12 h-12 bg-emerald-500 text-white rounded-full flex items-center justify-center mx-auto mb-3">
                  <CheckCircle className="w-7 h-7" />
                </div>
                <h3 className="text-base font-bold text-emerald-950">Consult Scheduled Successfully</h3>
                <p className="text-xs text-emerald-700 mt-1">
                  Your appointment booking was recorded with the diagnostic registration queue.
                </p>

                <div className="my-5 p-4 bg-white rounded-lg border border-emerald-150 inline-block">
                  <span className="text-[10px] font-bold text-slate-400 uppercase tracking-wider block">
                    Health Confirmation Code
                  </span>
                  <span className="text-lg font-mono font-bold text-slate-800 select-all tracking-wider">
                    {bookingStatus.appointmentCode}
                  </span>
                </div>

                <div className="text-xs text-slate-500 space-y-1 mb-4">
                  <p>Date & Time: {new Date(bookingStatus.appointmentDateTime).toLocaleString()}</p>
                  <p>Doctor Practitioner ID: #{bookingStatus.doctorId}</p>
                </div>

                <button
                  onClick={() => setBookingStatus(null)}
                  className="px-4 py-2 text-xs font-semibold text-white bg-blue-600 hover:bg-blue-700 transition rounded-lg"
                >
                  Book Another Consult
                </button>
              </div>
            ) : (
              // BOOKING FORM
              <form onSubmit={handleBookNow} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* STEP 1: SELECT SPECIALIZATION AND DOCTOR */}
                  <div>
                    <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                      1. Medical Specialization Filter
                    </label>
                    <select
                      value={selectedSpec}
                      onChange={(e) => {
                        setSelectedSpec(e.target.value);
                        // Reset doctor state if specialization doesn't match
                        setSelectedDoctor(null);
                      }}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-105 bg-white shadow-xs"
                    >
                      <option value="">-- All Specializations --</option>
                      {availableSpecs.map((spec) => (
                        <option key={spec} value={spec}>
                          {spec}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div>
                    <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                      2. Choose Available Practitioner
                    </label>
                    <select
                      value={selectedDoctor?.id || ""}
                      onChange={(e) => {
                        const d = doctors.find((x) => x.id === parseInt(e.target.value));
                        setSelectedDoctor(d || null);
                      }}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-105 bg-white shadow-xs"
                    >
                      <option value="" disabled>
                        -- Select Practitioner --
                      </option>
                      {filteredDoctors.map((doc) => (
                        <option key={doc.id} value={doc.id}>
                          {doc.name} ({doc.specialization}) - {doc.experience} Years Exp.
                        </option>
                      ))}
                    </select>
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  {/* STEP 2: SELECT DATE */}
                  <div className="md:col-span-1">
                    <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                      3. Select Preferred Date
                    </label>
                    <div className="relative">
                      <Calendar className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input
                        type="date"
                        value={slotDate}
                        min={new Date().toISOString().split("T")[0]}
                        onChange={(e) => setSlotDate(e.target.value)}
                        className="w-full pl-9 pr-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                      />
                    </div>
                  </div>

                  {/* STEP 3: PICK RELEVANT TIMESLOT OR SHOW MESSAGE */}
                  <div className="md:col-span-2">
                    <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                      4. Choose Available Hour Block
                    </label>
                    {availableSlots.length === 0 ? (
                      <div className="p-2 border border-dashed rounded-lg bg-slate-50/50 text-slate-500 text-xs min-h-[38px] flex items-center justify-center">
                        <AlertCircle className="w-3.5 h-3.5 text-slate-400 mr-1.5 shrink-0" />
                        No AVAILABLE concrete slots generated for this selected day.
                      </div>
                    ) : (
                      <div className="grid grid-cols-2 sm:grid-cols-3 gap-2 max-h-[120px] overflow-y-auto p-1 border rounded-lg border-slate-100">
                        {availableSlots.map((slot) => (
                          <button
                            key={slot.id}
                            type="button"
                            onClick={() => setSelectedSlot(slot)}
                            className={`p-2 rounded text-xs text-left transition font-medium border ${
                              selectedSlot?.id === slot.id
                                ? "bg-blue-600 border-blue-700 text-white"
                                : "bg-emerald-50/40 border-emerald-100 hover:bg-slate-50 text-slate-700"
                            }`}
                          >
                            <Clock className="w-3 h-3 inline mr-1" />
                            {slot.startTime.substring(0, 5)} - {slot.endTime.substring(0, 5)}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                {/* CONSULTATION NOTES */}
                <div>
                  <label className="block text-xs font-semibold text-slate-600 uppercase mb-1">
                    Brief Consult Notes (Symptoms, Prior Medications)
                  </label>
                  <textarea
                    rows={2}
                    value={consultNotes}
                    onChange={(e) => setConsultNotes(e.target.value)}
                    placeholder="Enter notes for pre-diagnosis review..."
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-100"
                  />
                </div>

                <div className="pt-2 flex justify-end">
                  <button
                    type="submit"
                    disabled={isBookingLoading || !selectedSlot}
                    className="cursor-pointer inline-flex items-center justify-center px-5 py-2.5 bg-blue-600 hover:bg-blue-700 text-white font-bold text-xs uppercase tracking-wide rounded-lg shadow-sm transition disabled:bg-slate-200 disabled:text-slate-500 disabled:cursor-not-allowed"
                  >
                    {isBookingLoading ? "Processing Appointment..." : "Confirm & Schedule Appointment"}
                  </button>
                </div>
              </form>
            )}
          </div>

          {/* ACTIVE APPOINTMENTS LOGS */}
          <div className="bg-white border border-slate-202 rounded-xl p-6 shadow-sm">
            <div className="border-b border-slate-150 pb-3 mb-4">
              <h2 className="text-sm font-bold text-slate-900">Registered Appointment Logs</h2>
            </div>

            {appointments.length === 0 ? (
              <EmptyState
                title="No Scheduled Sessions"
                description="Book your first diagnostics evaluation with an active doctor above."
              />
            ) : (
              <div className="space-y-4">
                {appointments.map((app) => {
                  const doc = doctors.find((d) => d.id === app.doctorId);
                  const isCancelable = app.status === "SCHEDULED" || app.status === "CONFIRMED";

                  return (
                    <div
                      key={app.id}
                      className="p-4 border border-slate-150 hover:border-slate-200 transition rounded-xl bg-slate-50/10 flex flex-col md:flex-row justify-between items-start md:items-center gap-4"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-xs font-bold bg-slate-100 px-2 py-0.5 rounded text-slate-700">
                            {app.appointmentCode}
                          </span>
                          <StatusBadge status={app.status} />
                        </div>
                        <h4 className="font-bold text-slate-800 text-sm mt-1">
                          Dr. {doc ? doc.name : `MD (Practitioner #${app.doctorId})`}
                        </h4>
                        <p className="text-xs text-slate-500 font-medium">
                          Specialization: {doc ? doc.specialization : "General Care"}
                        </p>
                        <div className="flex items-center gap-3 text-slate-500 text-[11px] mt-1 pt-1 border-t border-slate-100">
                          <span className="flex items-center gap-1">
                            <Calendar className="w-3 h-3 text-slate-400" />
                            {new Date(app.appointmentDateTime).toLocaleDateString(undefined, {
                              weekday: "short",
                              year: "numeric",
                              month: "short",
                              day: "numeric",
                            })}
                          </span>
                          <span className="flex items-center gap-1">
                            <Clock className="w-3 h-3 text-slate-400" />
                            {new Date(app.appointmentDateTime).toLocaleTimeString(undefined, {
                              hour: "2-digit",
                              minute: "2-digit",
                            })}
                          </span>
                        </div>
                        {app.notes && (
                          <p className="text-xs text-slate-600 bg-slate-50 p-2 rounded mt-2 border border-slate-100 italic">
                            &quot;{app.notes}&quot;
                          </p>
                        )}
                      </div>

                      {isCancelable && (
                        <button
                          onClick={() => handleCancelClick(app)}
                          className="px-3 py-1.5 border border-rose-200 hover:bg-rose-50 rounded-lg text-rose-600 text-xs font-semibold cursor-pointer transition flex items-center gap-1 inline-block shrink-0 self-end md:self-center"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          Cancel Session
                        </button>
                      )}
                    </div>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* RIGHT COLUMN: DIRECTORY & SEARCH */}
        <div className="lg:col-span-4 space-y-6">
          <div className="bg-white border border-slate-200 rounded-xl p-5 shadow-sm">
            <h3 className="text-sm font-bold text-slate-900 border-b border-slate-150 pb-2.5 mb-3 flex items-center gap-1.5">
              <Stethoscope className="w-4 h-4 text-blue-600" />
              Medical Practitioners
            </h3>

            {/* QUICK SPECIALIZATION BENTO */}
            <div className="flex flex-wrap gap-1.5 mb-4">
              <button
                onClick={() => setSelectedSpec("")}
                className={`px-2.5 py-1 rounded-md text-[10px] uppercase font-bold transition ${
                  selectedSpec === ""
                    ? "bg-slate-800 text-white"
                    : "bg-slate-100 text-slate-600 hover:bg-slate-200"
                }`}
              >
                All
              </button>
              {availableSpecs.map((spec) => (
                <button
                  key={spec}
                  onClick={() => setSelectedSpec(spec)}
                  className={`px-2.5 py-1 rounded-md text-[10px] uppercase font-bold transition ${
                    selectedSpec.toLowerCase() === spec.toLowerCase()
                      ? "bg-blue-600 text-white"
                      : "bg-blue-50 text-blue-700 hover:bg-blue-100"
                  }`}
                >
                  {spec}
                </button>
              ))}
            </div>

            <div className="space-y-3">
              {filteredDoctors.map((doc) => {
                const colors = getSpecColor(doc.specialization);
                return (
                  <div
                    key={doc.id}
                    onClick={() => {
                      setSelectedDoctor(doc);
                      // Clear specialize filter block
                      setSelectedSpec("");
                    }}
                    className={`p-3 border rounded-xl cursor-pointer text-left transition flex gap-3 ${
                      selectedDoctor?.id === doc.id
                        ? "border-blue-400 bg-blue-50/20"
                        : "border-slate-150 hover:border-slate-250 bg-white"
                    }`}
                  >
                    <div className={`w-9 h-9 rounded-lg flex items-center justify-center shrink-0 ${colors.bg}`}>
                      <Stethoscope className={`w-5 h-5 ${colors.text}`} />
                    </div>
                    <div>
                      <h4 className="font-bold text-slate-900 text-xs">Dr. {doc.name}</h4>
                      <p className="text-[11px] text-slate-500 font-medium">
                        {doc.specialization} • {doc.experience} Yr Exp
                      </p>
                      <p className="text-[10px] text-slate-400 mt-1">Available: {doc.availableHours}</p>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};
