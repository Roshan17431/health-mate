/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useState, useEffect, useId } from "react";
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
  const fieldIdPrefix = `patient-${useId()}`;
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
      if (selectedDoctor && slotDate) {
        setAvailableSlots(await fetchTimeSlotsByDate(selectedDoctor.id, slotDate, "AVAILABLE"));
        setSelectedSlot(null);
      }
    } catch (err) {
      setCancelingApp(null);
      setError(err);
    }
  };

  if (loading) {
    return <LoadingState message="Loading your care workspace…" />;
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
        title="Cancel appointment"
        message={`Are you sure you want to cancel your upcoming session on ${
          cancelingApp
            ? new Date(cancelingApp.appointmentDateTime).toLocaleDateString()
            : ""
        }? This will return this time slot to available status.`}
        confirmLabel="Cancel appointment"
        cancelLabel="Keep appointment"
        isDestructive={true}
        onConfirm={handleConfirmCancellation}
        onCancel={() => setCancelingApp(null)}
      />

      {/* WELCOME HEADER HERO */}
      <div className="dashboard-card dashboard-welcome bg-white border border-slate-200 rounded-xl p-6 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div>
          <span className="text-xs font-semibold tracking-wider text-teal-600 bg-teal-50 px-2 py-1 rounded">
            YOUR CARE, AT A GLANCE
          </span>
          <h1 className="dashboard-title text-xl font-semibold text-slate-900 mt-2">Welcome back, {user.name}</h1>
          <p className="text-sm text-slate-500 mt-1">
            Email: {patient?.email} | Medical ID: HM-P-{patient?.id}
          </p>
        </div>
        <div className="flex gap-4 border-t pt-4 md:border-t-0 md:pt-0">
          <div className="bg-slate-50 border border-slate-100 p-2.5 rounded-lg text-center min-w-[100px]">
            <span className="text-sm font-semibold text-slate-500 block">Scheduled</span>
            <span className="text-xl font-semibold text-slate-800">
              {appointments.filter((a) => a.status === "SCHEDULED").length}
            </span>
          </div>
          <div className="bg-slate-50 border border-slate-100 p-2.5 rounded-lg text-center min-w-[100px]">
            <span className="text-sm font-semibold text-slate-500 block">Confirmed</span>
            <span className="text-xl font-semibold text-emerald-600">
              {appointments.filter((a) => a.status === "CONFIRMED").length}
            </span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        {/* LEFT COLUMN: ACTIVE BOOKINGS MODULE */}
        <div className="lg:col-span-8 space-y-6">
          {/* APPOINTMENT BOOKING COMPONENT */}
          <div className="dashboard-card bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
            <div className="border-b border-slate-100 pb-3 mb-4 flex items-center justify-between">
              <h2 className="text-sm font-semibold text-slate-900 flex items-center gap-2">
                <BookOpen className="w-4 h-4 text-teal-600" />
                Book an appointment
              </h2>
            </div>

            {bookingStatus ? (
              // BOOKING COMPLETED SUCCESS CARD
              <div className="p-6 bg-emerald-50 rounded-xl border border-emerald-100 text-center animate-fade-in">
                <div className="w-12 h-12 bg-emerald-500 text-white rounded-full flex items-center justify-center mx-auto mb-3">
                  <CheckCircle className="w-7 h-7" />
                </div>
                <h3 className="text-base font-semibold text-emerald-950">Your appointment is booked</h3>
                <p className="text-sm text-emerald-700 mt-1">
                  Your appointment booking was recorded with the diagnostic registration queue.
                </p>

                <div className="dashboard-card my-5 p-4 bg-white rounded-lg border border-emerald-100 inline-block">
                  <span className="text-xs font-semibold text-slate-400  block">
                    Health Confirmation Code
                  </span>
                  <span className="text-lg font-mono font-semibold text-slate-800 select-all tracking-wider">
                    {bookingStatus.appointmentCode}
                  </span>
                </div>

                <div className="text-sm text-slate-500 space-y-1 mb-4">
                  <p>Date & Time: {new Date(bookingStatus.appointmentDateTime).toLocaleString()}</p>
                  <p>Doctor Practitioner ID: #{bookingStatus.doctorId}</p>
                </div>

                <button
                  onClick={() => setBookingStatus(null)}
                  className="px-4 py-2 text-sm font-semibold text-white bg-teal-600 hover:bg-teal-700 transition rounded-lg"
                >
                  Book another appointment
                </button>
              </div>
            ) : (
              // BOOKING FORM
              <form onSubmit={handleBookNow} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* STEP 1: SELECT SPECIALIZATION AND DOCTOR */}
                  <div>
                    <label htmlFor={`${fieldIdPrefix}-specialty`} className="block text-sm font-semibold text-slate-600 uppercase mb-1">
                      1. Specialty
                    </label>
                    <select
                      id={`${fieldIdPrefix}-specialty`}
                      value={selectedSpec}
                      onChange={(e) => {
                        setSelectedSpec(e.target.value);
                        // Reset doctor state if specialization doesn't match
                        setSelectedDoctor(null);
                      }}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-teal-500 focus:ring-2 focus:ring-teal-100 bg-white shadow-xs"
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
                    <label htmlFor={`${fieldIdPrefix}-doctor`} className="block text-sm font-semibold text-slate-600 uppercase mb-1">
                      2. Doctor
                    </label>
                    <select
                      id={`${fieldIdPrefix}-doctor`}
                      value={selectedDoctor?.id || ""}
                      onChange={(e) => {
                        const d = doctors.find((x) => x.id === parseInt(e.target.value));
                        setSelectedDoctor(d || null);
                      }}
                      className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-teal-500 focus:ring-2 focus:ring-teal-100 bg-white shadow-xs"
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
                    <label htmlFor={`${fieldIdPrefix}-date`} className="block text-sm font-semibold text-slate-600 uppercase mb-1">
                      3. Date
                    </label>
                    <div className="relative">
                      <Calendar className="absolute left-3 top-3 w-4 h-4 text-slate-400" />
                      <input
                        id={`${fieldIdPrefix}-date`}
                        type="date"
                        value={slotDate}
                        min={new Date().toISOString().split("T")[0]}
                        onChange={(e) => setSlotDate(e.target.value)}
                        className="w-full pl-9 pr-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
                      />
                    </div>
                  </div>

                  {/* STEP 3: PICK RELEVANT TIMESLOT OR SHOW MESSAGE */}
                  <div className="md:col-span-2" role="group" aria-labelledby={`${fieldIdPrefix}-available-times`}>
                    <div id={`${fieldIdPrefix}-available-times`} className="block text-[13px] leading-5 font-medium text-slate-600 normal-case tracking-normal mb-1">
                      4. Available times
                    </div>
                    {availableSlots.length === 0 ? (
                      <div className="p-2 border border-dashed rounded-lg bg-slate-50/50 text-slate-500 text-sm min-h-[38px] flex items-center justify-center">
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
                            className={`p-2 rounded text-sm text-left transition font-medium border ${
                              selectedSlot?.id === slot.id
                                ? "bg-teal-600 border-teal-700 text-white"
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
                  <label htmlFor={`${fieldIdPrefix}-consultation-notes`} className="block text-sm font-semibold text-slate-600 uppercase mb-1">
                    Anything your doctor should know?
                  </label>
                  <textarea
                    id={`${fieldIdPrefix}-consultation-notes`}
                    rows={2}
                    value={consultNotes}
                    onChange={(e) => setConsultNotes(e.target.value)}
                    placeholder="Tell your doctor what you would like to discuss…"
                    className="w-full px-3 py-2 border border-slate-200 rounded-lg text-sm text-slate-800 focus:outline-none focus:border-teal-500 focus:ring-2 focus:ring-teal-100"
                  />
                </div>

                <div className="pt-2 flex justify-end">
                  <button
                    type="submit"
                    disabled={isBookingLoading || !selectedSlot}
                    className="cursor-pointer inline-flex items-center justify-center px-5 py-2.5 bg-teal-600 hover:bg-teal-700 text-white font-semibold text-sm  rounded-lg shadow-sm transition disabled:bg-slate-200 disabled:text-slate-500 disabled:cursor-not-allowed"
                  >
                    {isBookingLoading ? "Processing Appointment..." : "Book appointment"}
                  </button>
                </div>
              </form>
            )}
          </div>

          {/* ACTIVE APPOINTMENTS LOGS */}
          <div className="dashboard-card bg-white border border-slate-200 rounded-xl p-6 shadow-sm">
            <div className="border-b border-slate-100 pb-3 mb-4">
              <h2 className="text-sm font-semibold text-slate-900">Your appointments</h2>
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
                      className="p-4 border border-slate-100 hover:border-slate-200 transition rounded-xl bg-slate-50/10 flex flex-col md:flex-row justify-between items-start md:items-center gap-4"
                    >
                      <div className="space-y-1">
                        <div className="flex items-center gap-2">
                          <span className="font-mono text-sm font-semibold bg-slate-100 px-2 py-0.5 rounded text-slate-700">
                            {app.appointmentCode}
                          </span>
                          <StatusBadge status={app.status} />
                        </div>
                        <h4 className="font-semibold text-slate-800 text-sm mt-1">
                          Dr. {doc ? doc.name : `MD (Practitioner #${app.doctorId})`}
                        </h4>
                        <p className="text-sm text-slate-500 font-medium">
                          Specialization: {doc ? doc.specialization : "General Care"}
                        </p>
                        <div className="flex items-center gap-3 text-slate-500 text-xs mt-1 pt-1 border-t border-slate-100">
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
                          <p className="text-sm text-slate-600 bg-slate-50 p-2 rounded mt-2 border border-slate-100 italic">
                            &quot;{app.notes}&quot;
                          </p>
                        )}
                      </div>

                      {isCancelable && (
                        <button
                          onClick={() => handleCancelClick(app)}
                          className="px-3 py-1.5 border border-rose-200 hover:bg-rose-50 rounded-lg text-rose-600 text-sm font-semibold cursor-pointer transition flex items-center gap-1 inline-block shrink-0 self-end md:self-center"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                          Cancel appointment
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
          <div className="dashboard-card bg-white border border-slate-200 rounded-xl p-5 shadow-sm">
            <h3 className="text-sm font-semibold text-slate-900 border-b border-slate-100 pb-2.5 mb-3 flex items-center gap-1.5">
              <Stethoscope className="w-4 h-4 text-teal-600" />
              Your care team
            </h3>

            {/* QUICK SPECIALIZATION BENTO */}
            <div className="flex flex-wrap gap-1.5 mb-4">
              <button
                onClick={() => setSelectedSpec("")}
                className={`px-2.5 py-1 rounded-md text-xs uppercase font-semibold transition ${
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
                  className={`px-2.5 py-1 rounded-md text-xs uppercase font-semibold transition ${
                    selectedSpec.toLowerCase() === spec.toLowerCase()
                      ? "bg-teal-600 text-white"
                      : "bg-teal-50 text-teal-700 hover:bg-teal-100"
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
                        ? "border-teal-400 bg-teal-50/20"
                        : "border-slate-100 hover:border-slate-200 bg-white"
                    }`}
                  >
                    <div className={`w-9 h-9 rounded-lg flex items-center justify-center shrink-0 ${colors.bg}`}>
                      <Stethoscope className={`w-5 h-5 ${colors.text}`} />
                    </div>
                    <div>
                      <h4 className="font-semibold text-slate-900 text-sm">Dr. {doc.name}</h4>
                      <p className="text-xs text-slate-500 font-medium">
                        {doc.specialization} • {doc.experience} Yr Exp
                      </p>
                      <p className="text-xs text-slate-400 mt-1">Available: {doc.availableHours}</p>
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
