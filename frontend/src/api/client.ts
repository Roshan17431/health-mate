/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import {
  Gender,
  UserRole,
  AppointmentStatus,
  SlotStatus,
  OtpPurpose,
  DayOfWeek,
  AuthRequestDTO,
  AuthResponseDTO,
  RegisterRequestDTO,
  UserDTO,
  UserUpdateDTO,
  DoctorDTO,
  DoctorRequestDTO,
  PatientDTO,
  PatientRequestDTO,
  AppointmentDTO,
  AppointmentRequestDTO,
  DoctorScheduleDTO,
  DoctorScheduleRequestDTO,
  TimeSlotDTO,
  TimeSlotRequestDTO,
  NotificationDTO,
  NotificationRequestDTO,
  AuditLogDTO,
  AuditLogRequestDTO,
  SendOtpRequestDTO,
  OtpVerificationRequestDTO,
  OtpTokenDTO,
  PageResponse,
} from "../types";

// Setup API URL
const defaultUrl = "http://localhost:8080";
export const getApiUrl = (): string => {
  return ((import.meta as any).env?.VITE_API_BASE_URL as string) || defaultUrl;
};

// Mode storage
export type ApiMode = "demo" | "live";
export const getApiMode = (): ApiMode => {
  const mode = localStorage.getItem("healthmate_api_mode");
  return (mode as ApiMode) || "demo"; // Default to demo so preview works instantly
};

export const setApiMode = (mode: ApiMode) => {
  localStorage.setItem("healthmate_api_mode", mode);
  window.dispatchEvent(new Event("healthmate_mode_changed"));
};

// Setup current auth session persistence
export const getSessionToken = (): string | null => {
  return localStorage.getItem("healthmate_token");
};

export const getSessionUser = (): UserDTO | null => {
  const userJson = localStorage.getItem("healthmate_user");
  return userJson ? JSON.parse(userJson) : null;
};

export const saveSession = (token: string, user: UserDTO) => {
  localStorage.setItem("healthmate_token", token);
  localStorage.setItem("healthmate_user", JSON.stringify(user));
  window.dispatchEvent(new Event("healthmate_auth_changed"));
};

export const clearSession = () => {
  localStorage.removeItem("healthmate_token");
  localStorage.removeItem("healthmate_user");
  window.dispatchEvent(new Event("healthmate_auth_changed"));
};

// ----------------------------------------------------
// DEMO IN-MEMORY STORAGE WITH LOCALSTORAGE PERSISTENCE
// ----------------------------------------------------
interface DemoDatabase {
  users: UserDTO[];
  passwords: Record<string, string>; // email -> password
  doctors: DoctorDTO[];
  patients: PatientDTO[];
  appointments: AppointmentDTO[];
  schedules: DoctorScheduleDTO[];
  timeSlots: TimeSlotDTO[];
  notifications: NotificationDTO[];
  auditLogs: AuditLogDTO[];
  otps: OtpTokenDTO[];
  otpCodes: Record<string, string>; // email+purpose+id -> code (simulated SMS/email delivery)
}

const INITIAL_DB: DemoDatabase = {
  users: [
    {
      id: 1,
      name: "Dr. Vikram Mehta",
      email: "doctor@healthmate.com",
      phoneNumber: "9876543201",
      role: "ROLE_DOCTOR",
      gender: "MALE",
      dateOfBirth: "1980-04-12",
      address: "Mumbai Healthcare Zone, Block B",
      createdDate: "2026-06-01T09:00:00",
      updatedDate: "2026-06-01T09:00:00",
    },
    {
      id: 2,
      name: "Asha Sharma",
      email: "patient@healthmate.com",
      phoneNumber: "9876543210",
      role: "ROLE_PATIENT",
      gender: "FEMALE",
      dateOfBirth: "1995-08-24",
      address: "Pune Cyber City, Lane 4",
      createdDate: "2026-06-02T10:00:00",
      updatedDate: "2026-06-02T10:00:00",
    },
    {
      id: 3,
      name: "Super Administrator",
      email: "admin@healthmate.com",
      phoneNumber: "9000000000",
      role: "ROLE_ADMIN",
      gender: "OTHER",
      dateOfBirth: "1990-01-01",
      address: "Health Mate HQ Mainframe",
      createdDate: "2026-06-01T08:00:00",
      updatedDate: "2026-06-01T08:00:00",
    },
    {
      id: 4,
      name: "Dr. Priya Sen",
      email: "priya@healthmate.com",
      phoneNumber: "9876543204",
      role: "ROLE_DOCTOR",
      gender: "FEMALE",
      dateOfBirth: "1985-11-20",
      address: "Dermatology Clinic, West Pune",
      createdDate: "2026-06-03T11:00:00",
      updatedDate: "2026-06-03T11:00:00",
    },
    {
      id: 5,
      name: "Dr. Rajesh Iyer",
      email: "rajesh@healthmate.com",
      phoneNumber: "9876543205",
      role: "ROLE_DOCTOR",
      gender: "MALE",
      dateOfBirth: "1975-06-15",
      address: "Pediatrics Ward, Pune Central",
      createdDate: "2026-06-03T11:30:00",
      updatedDate: "2026-06-03T11:30:00",
    },
  ],
  passwords: {
    "doctor@healthmate.com": "doctor123",
    "patient@healthmate.com": "patient123",
    "admin@healthmate.com": "admin123",
    "priya@healthmate.com": "doctor123",
    "rajesh@healthmate.com": "doctor123",
  },
  doctors: [
    {
      id: 1,
      experience: 15,
      availableHours: "09:00-13:00",
      specialization: "Cardiology",
      userId: 1,
      name: "Dr. Vikram Mehta",
      email: "doctor@healthmate.com",
      phone: "9876543201",
      createdAt: "2026-06-01T09:05:00",
      updatedAt: "2026-06-01T09:05:00",
    },
    {
      id: 2,
      experience: 9,
      availableHours: "14:00-18:00",
      specialization: "Dermatology",
      userId: 4,
      name: "Dr. Priya Sen",
      email: "priya@healthmate.com",
      phone: "9876543204",
      createdAt: "2026-06-03T11:05:00",
      updatedAt: "2026-06-03T11:05:00",
    },
    {
      id: 3,
      experience: 20,
      availableHours: "10:00-16:00",
      specialization: "Pediatrics",
      userId: 5,
      name: "Dr. Rajesh Iyer",
      email: "rajesh@healthmate.com",
      phone: "9876543205",
      createdAt: "2026-06-03T11:35:00",
      updatedAt: "2026-06-03T11:35:00",
    },
  ],
  patients: [
    {
      id: 1,
      name: "Asha Sharma",
      email: "patient@healthmate.com",
      phone: "9876543210",
      createdAt: "2026-06-02T10:05:00",
      updatedAt: "2026-06-02T10:05:00",
    },
  ],
  schedules: [
    {
      id: 1,
      doctorId: 1,
      dayOfWeek: "MONDAY",
      startTime: "09:00:00",
      endTime: "13:00:00",
      isActive: true,
      createdAt: "2026-06-01T12:00:00",
      updatedAt: "2026-06-01T12:00:00",
    },
    {
      id: 2,
      doctorId: 1,
      dayOfWeek: "WEDNESDAY",
      startTime: "09:00:00",
      endTime: "13:00:00",
      isActive: true,
      createdAt: "2026-06-01T12:00:00",
      updatedAt: "2026-06-01T12:00:00",
    },
    {
      id: 3,
      doctorId: 2,
      dayOfWeek: "TUESDAY",
      startTime: "14:00:00",
      endTime: "18:00:00",
      isActive: true,
      createdAt: "2026-06-03T12:00:00",
      updatedAt: "2026-06-03T12:00:00",
    },
    {
      id: 4,
      doctorId: 3,
      dayOfWeek: "FRIDAY",
      startTime: "10:00:00",
      endTime: "16:00:00",
      isActive: true,
      createdAt: "2026-06-03T12:00:00",
      updatedAt: "2026-06-03T12:00:00",
    },
  ],
  timeSlots: [
    {
      id: 1,
      doctorId: 1,
      slotDate: "2026-06-22",
      startTime: "09:00:00",
      endTime: "09:30:00",
      status: "AVAILABLE",
    },
    {
      id: 2,
      doctorId: 1,
      slotDate: "2026-06-22",
      startTime: "09:30:00",
      endTime: "10:00:00",
      status: "BOOKED",
      appointmentId: 1,
    },
    {
      id: 3,
      doctorId: 1,
      slotDate: "2026-06-22",
      startTime: "10:00:00",
      endTime: "10:30:00",
      status: "AVAILABLE",
    },
    {
      id: 4,
      doctorId: 1,
      slotDate: "2026-06-22",
      startTime: "10:30:00",
      endTime: "11:00:00",
      status: "AVAILABLE",
    },
    // Doctor 2 slots
    {
      id: 5,
      doctorId: 2,
      slotDate: "2026-06-23",
      startTime: "14:00:00",
      endTime: "14:30:00",
      status: "AVAILABLE",
    },
    {
      id: 6,
      doctorId: 2,
      slotDate: "2026-06-23",
      startTime: "14:30:00",
      endTime: "15:00:00",
      status: "AVAILABLE",
    },
  ],
  appointments: [
    {
      id: 1,
      appointmentDateTime: "2026-06-22T09:30:00",
      status: "CONFIRMED",
      notes: "Routine cardiac evaluation and general checkup.",
      appointmentCode: "HM-7489-A2",
      doctorId: 1,
      patientId: 1,
      timeSlotId: 2,
      createdAt: "2026-06-18T14:30:00",
      updatedAt: "2026-06-18T14:30:00",
    },
  ],
  notifications: [
    {
      id: 1,
      userId: 2,
      message: "Welcome to Health Mate! Your profile is set up successfully.",
      isRead: true,
      timestamp: "2026-06-02T10:05:00",
    },
    {
      id: 2,
      userId: 1,
      message: "Dr. Mehta, your system profile is linked to Cardiology department.",
      isRead: false,
      timestamp: "2026-06-01T09:10:00",
    },
    {
      id: 3,
      userId: 2,
      message: "Your appointment (HM-7489-A2) with Dr. Vikram Mehta is CONFIRMED for 22nd June at 09:30 AM.",
      isRead: false,
      timestamp: "2026-06-18T14:30:00",
    },
  ],
  auditLogs: [
    {
      id: 1,
      userId: 3,
      action: "SYSTEM_STARTUP",
      details: "Health Mate Administration dashboard loaded",
      timestamp: "2026-06-01T08:00:00",
    },
    {
      id: 2,
      userId: 2,
      action: "USER_REGISTER",
      details: "Registered new patient Asha Sharma",
      timestamp: "2026-06-02T10:00:00",
    },
    {
      id: 3,
      userId: 2,
      action: "APPOINTMENT_BOOKED",
      details: "Booked cardiac consult slot id 2",
      timestamp: "2026-06-18T14:30:00",
    },
  ],
  otps: [],
  otpCodes: {},
};

// Getter helper for DB
export const getDB = (): DemoDatabase => {
  const dbStr = localStorage.getItem("healthmate_demo_db");
  if (!dbStr) {
    localStorage.setItem("healthmate_demo_db", JSON.stringify(INITIAL_DB));
    return INITIAL_DB;
  }
  try {
    return JSON.parse(dbStr);
  } catch (e) {
    return INITIAL_DB;
  }
};

// Setter helper for DB
const saveDB = (db: DemoDatabase) => {
  localStorage.setItem("healthmate_demo_db", JSON.stringify(db));
};

// ----------------------------------------------------
// NORMALIZED API WRAPPER WITH BOTH LIVE AND DEMO ROUTING
// ----------------------------------------------------

async function requestLive<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const token = getSessionToken();
  const response = await fetch(`${getApiUrl()}${path}`, {
    ...options,
    headers: {
      Accept: "application/json",
      ...(options.body ? { "Content-Type": "application/json" } : {}),
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...options.headers,
    },
  });

  if (response.status === 204) return undefined as T;

  const text = await response.text();
  const data = text ? JSON.parse(text) : undefined;

  if (!response.ok) {
    if (response.status === 401) {
      clearSession();
    }
    throw data ?? { statusCode: response.status, message: response.statusText };
  }

  return data as T;
}

// --------------------
// 1. AUTHENTICATION
// --------------------

export async function loginUser(dto: AuthRequestDTO): Promise<AuthResponseDTO> {
  if (getApiMode() === "live") {
    const res = await requestLive<AuthResponseDTO>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify(dto),
    });
    saveSession(res.token, res.user);
    return res;
  } else {
    const db = getDB();
    const user = db.users.find((u) => u.email.toLowerCase() === dto.email.toLowerCase());
    if (!user) {
      throw { statusCode: 400, message: "Invalid email or password" };
    }
    const realPassword = db.passwords[user.email.toLowerCase()];
    if (realPassword !== dto.password) {
      throw { statusCode: 400, message: "Invalid email or password" };
    }

    // Capture sign in log
    const logId = db.auditLogs.length + 1;
    db.auditLogs.push({
      id: logId,
      userId: user.id,
      action: "USER_LOGIN",
      details: `User ${user.email} logged in. Role: ${user.role}`,
      timestamp: new Date().toISOString(),
    });
    saveDB(db);

    const token = `demo_token_jwt_${user.id}_${Date.now()}`;
    const authRes: AuthResponseDTO = {
      token,
      tokenType: "Bearer",
      expiresIn: 86400000,
      user,
    };
    saveSession(token, user);
    return authRes;
  }
}

export async function registerUser(dto: RegisterRequestDTO): Promise<AuthResponseDTO> {
  if (getApiMode() === "live") {
    const res = await requestLive<AuthResponseDTO>("/api/auth/register", {
      method: "POST",
      body: JSON.stringify(dto),
    });
    saveSession(res.token, res.user);
    return res;
  } else {
    const db = getDB();
    if (db.users.some((u) => u.email.toLowerCase() === dto.email.toLowerCase())) {
      throw { statusCode: 400, message: "Email already exists" };
    }

    const newUserId = db.users.length + 1;
    const role = dto.role || "ROLE_PATIENT";

    const newUser: UserDTO = {
      id: newUserId,
      name: dto.name,
      email: dto.email,
      phoneNumber: dto.phoneNumber,
      role,
      gender: dto.gender,
      dateOfBirth: dto.dateOfBirth,
      address: dto.address,
      createdDate: new Date().toISOString(),
      updatedDate: new Date().toISOString(),
    };

    db.users.push(newUser);
    db.passwords[dto.email.toLowerCase()] = dto.password;

    // Link patient profile if role is patient
    if (role === "ROLE_PATIENT") {
      const pId = db.patients.length + 1;
      db.patients.push({
        id: pId,
        name: dto.name,
        email: dto.email,
        phone: dto.phoneNumber,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      });
    } else if (role === "ROLE_DOCTOR") {
      const dId = db.doctors.length + 1;
      db.doctors.push({
        id: dId,
        experience: 1,
        availableHours: "09:00-17:00",
        specialization: "General Medicine",
        userId: newUserId,
        name: dto.name,
        email: dto.email,
        phone: dto.phoneNumber,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      });
    }

    // Add Audit Log
    db.auditLogs.push({
      id: db.auditLogs.length + 1,
      userId: newUserId,
      action: "USER_REGISTER",
      details: `Registered new account. Email: ${dto.email}, Role: ${role}`,
      timestamp: new Date().toISOString(),
    });

    // Welcome Notification
    db.notifications.push({
      id: db.notifications.length + 1,
      userId: newUserId,
      message: `Welcome ${dto.name}! Your ${role.replace("ROLE_", "").toLowerCase()} profile is ready.`,
      isRead: false,
      timestamp: new Date().toISOString(),
    });

    saveDB(db);

    const token = `demo_token_jwt_${newUserId}_${Date.now()}`;
    const authRes: AuthResponseDTO = {
      token,
      tokenType: "Bearer",
      expiresIn: 86400000,
      user: newUser,
    };
    saveSession(token, newUser);
    return authRes;
  }
}

// --------------------
// 2. USERS OPERATIONS
// --------------------

export async function fetchUsers(): Promise<UserDTO[]> {
  if (getApiMode() === "live") {
    return requestLive<UserDTO[]>("/api/users");
  } else {
    return getDB().users;
  }
}

export async function fetchUserById(id: number): Promise<UserDTO> {
  if (getApiMode() === "live") {
    return requestLive<UserDTO>(`/api/users/${id}`);
  } else {
    const user = getDB().users.find((u) => u.id === id);
    if (!user) throw { statusCode: 404, message: "User not found" };
    return user;
  }
}

export async function fetchUserByEmail(email: string): Promise<UserDTO> {
  if (getApiMode() === "live") {
    return requestLive<UserDTO>(`/api/users/by-email?email=${encodeURIComponent(email)}`);
  } else {
    const user = getDB().users.find((u) => u.email.toLowerCase() === email.toLowerCase());
    if (!user) throw { statusCode: 404, message: "User not found" };
    return user;
  }
}

export async function updateUser(id: number, dto: UserUpdateDTO): Promise<UserDTO> {
  if (getApiMode() === "live") {
    const updated = await requestLive<UserDTO>(`/api/users/${id}`, {
      method: "PATCH",
      body: JSON.stringify(dto),
    });
    // If the updated user is the currently logged in user, refresh their session user cache
    const curr = getSessionUser();
    if (curr && curr.id === id) {
      saveSession(getSessionToken() || "", updated);
    }
    return updated;
  } else {
    const db = getDB();
    const index = db.users.findIndex((u) => u.id === id);
    if (index === -1) throw { statusCode: 404, message: "User not found" };

    const current = db.users[index];
    const updated: UserDTO = {
      ...current,
      ...dto,
      updatedDate: new Date().toISOString(),
    };

    db.users[index] = updated;

    // Synchronize to corresponding doctor or patient records
    if (current.role === "ROLE_PATIENT") {
      const idx = db.patients.findIndex((p) => p.email.toLowerCase() === current.email.toLowerCase());
      if (idx !== -1) {
        db.patients[idx] = {
          ...db.patients[idx],
          name: updated.name,
          phone: updated.phoneNumber,
          updatedAt: new Date().toISOString(),
        };
      }
    } else if (current.role === "ROLE_DOCTOR") {
      const idx = db.doctors.findIndex((d) => d.userId === id);
      if (idx !== -1) {
        db.doctors[idx] = {
          ...db.doctors[idx],
          name: updated.name,
          phone: updated.phoneNumber,
          updatedAt: new Date().toISOString(),
        };
      }
    }

    db.auditLogs.push({
      id: db.auditLogs.length + 1,
      userId: id,
      action: "USER_UPDATE",
      details: `Updated personal information details.`,
      timestamp: new Date().toISOString(),
    });

    saveDB(db);

    const curr = getSessionUser();
    if (curr && curr.id === id) {
      saveSession(getSessionToken() || "", updated);
    }

    return updated;
  }
}

export async function deleteUser(id: number): Promise<void> {
  if (getApiMode() === "live") {
    return requestLive<void>(`/api/users/${id}`, { method: "DELETE" });
  } else {
    const db = getDB();
    const index = db.users.findIndex((u) => u.id === id);
    if (index === -1) throw { statusCode: 404, message: "User not found" };

    const u = db.users[index];
    db.users.splice(index, 1);

    // Remove subordinates
    db.patients = db.patients.filter((p) => p.email.toLowerCase() !== u.email.toLowerCase());
    db.doctors = db.doctors.filter((d) => d.userId !== id);

    db.auditLogs.push({
      id: db.auditLogs.length + 1,
      action: "USER_DELETED",
      details: `Deleted user: ${u.email} with id: ${id}`,
      timestamp: new Date().toISOString(),
    });

    saveDB(db);
  }
}

// --------------------
// 3. DOCTORS OPERATIONS
// --------------------

export async function fetchDoctors(specialization?: string): Promise<DoctorDTO[]> {
  if (getApiMode() === "live") {
    const query = specialization ? `?specialization=${encodeURIComponent(specialization)}` : "";
    return requestLive<DoctorDTO[]>(`/api/doctors${query}`);
  } else {
    const list = getDB().doctors;
    if (specialization) {
      return list.filter((d) => d.specialization.toLowerCase() === specialization.toLowerCase());
    }
    return list;
  }
}

export async function createDoctor(dto: DoctorRequestDTO): Promise<DoctorDTO> {
  if (getApiMode() === "live") {
    return requestLive<DoctorDTO>("/api/doctors", {
      method: "POST",
      body: JSON.stringify(dto),
    });
  } else {
    const db = getDB();
    const user = db.users.find((u) => u.id === dto.userId);
    if (!user) throw { statusCode: 404, message: "Assignee user account not found" };

    const newId = db.doctors.length + 1;
    const doc: DoctorDTO = {
      id: newId,
      experience: dto.experience,
      availableHours: dto.availableHours,
      specialization: dto.specialization,
      userId: dto.userId,
      name: user.name,
      email: user.email,
      phone: user.phoneNumber,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    db.doctors.push(doc);

    db.auditLogs.push({
      id: db.auditLogs.length + 1,
      action: "DOCTOR_CREATED",
      details: `Created doctor specialization profile for ${user.name} in ${dto.specialization}`,
      timestamp: new Date().toISOString(),
    });

    saveDB(db);
    return doc;
  }
}

export async function fetchDoctorById(id: number): Promise<DoctorDTO> {
  if (getApiMode() === "live") {
    return requestLive<DoctorDTO>(`/api/doctors/${id}`);
  } else {
    const doc = getDB().doctors.find((d) => d.id === id);
    if (!doc) throw { statusCode: 404, message: "Doctor profile not found" };
    return doc;
  }
}

export async function updateDoctor(id: number, dto: DoctorRequestDTO): Promise<DoctorDTO> {
  if (getApiMode() === "live") {
    return requestLive<DoctorDTO>(`/api/doctors/${id}`, {
      method: "PUT",
      body: JSON.stringify(dto),
    });
  } else {
    const db = getDB();
    const index = db.doctors.findIndex((d) => d.id === id);
    if (index === -1) throw { statusCode: 404, message: "Doctor profile not found" };

    const current = db.doctors[index];
    const user = db.users.find((u) => u.id === dto.userId);
    if (!user) throw { statusCode: 404, message: "Linked user not found" };

    const updated: DoctorDTO = {
      ...current,
      experience: dto.experience,
      availableHours: dto.availableHours,
      specialization: dto.specialization,
      userId: dto.userId,
      name: user.name,
      email: user.email,
      phone: user.phoneNumber,
      updatedAt: new Date().toISOString(),
    };

    db.doctors[index] = updated;

    db.auditLogs.push({
      id: db.auditLogs.length + 1,
      action: "DOCTOR_UPDATED",
      details: `Updated doctor profile of Dr. ${user.name}`,
      timestamp: new Date().toISOString(),
    });

    saveDB(db);
    return updated;
  }
}

export async function deleteDoctor(id: number): Promise<void> {
  if (getApiMode() === "live") {
    return requestLive<void>(`/api/doctors/${id}`, { method: "DELETE" });
  } else {
    const db = getDB();
    db.doctors = db.doctors.filter((d) => d.id !== id);
    saveDB(db);
  }
}

// --------------------
// 4. PATIENTS OPERATIONS
// --------------------

export async function createPatient(dto: PatientRequestDTO): Promise<PatientDTO> {
  if (getApiMode() === "live") {
    return requestLive<PatientDTO>("/api/patients", {
      method: "POST",
      body: JSON.stringify(dto),
    });
  } else {
    const db = getDB();
    const user = db.users.find((u) => u.id === dto.userId);
    if (!user) throw { statusCode: 404, message: "User not found" };

    const newPatient: PatientDTO = {
      id: db.patients.length + 1,
      name: user.name,
      email: user.email,
      phone: user.phoneNumber,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    db.patients.push(newPatient);
    saveDB(db);
    return newPatient;
  }
}

export async function fetchPatients(): Promise<PatientDTO[]> {
  if (getApiMode() === "live") {
    return requestLive<PatientDTO[]>("/api/patients");
  } else {
    return getDB().patients;
  }
}

export async function fetchPatientById(id: number): Promise<PatientDTO> {
  if (getApiMode() === "live") {
    return requestLive<PatientDTO>(`/api/patients/${id}`);
  } else {
    const patient = getDB().patients.find((p) => p.id === id);
    if (!patient) throw { statusCode: 404, message: "Patient not found" };
    return patient;
  }
}

export async function fetchPatientByUserId(userId: number): Promise<PatientDTO> {
  if (getApiMode() === "live") {
    return requestLive<PatientDTO>(`/api/patients/user/${userId}`);
  } else {
    const db = getDB();
    const user = db.users.find((u) => u.id === userId);
    if (!user) throw { statusCode: 404, message: "User not found" };

    const patient = db.patients.find((p) => p.email.toLowerCase() === user.email.toLowerCase());
    if (!patient) {
      // Auto register patient record to complete loop
      const pId = db.patients.length + 1;
      const newP: PatientDTO = {
        id: pId,
        name: user.name,
        email: user.email,
        phone: user.phoneNumber,
        createdAt: new Date().toISOString(),
        updatedAt: new Date().toISOString(),
      };
      db.patients.push(newP);
      saveDB(db);
      return newP;
    }
    return patient;
  }
}

export async function deletePatient(id: number): Promise<void> {
  if (getApiMode() === "live") {
    return requestLive<void>(`/api/patients/${id}`, { method: "DELETE" });
  } else {
    const db = getDB();
    db.patients = db.patients.filter((p) => p.id !== id);
    saveDB(db);
  }
}

// --------------------
// 5. APPOINTMENTS
// --------------------

export async function bookAppointment(dto: AppointmentRequestDTO): Promise<AppointmentDTO> {
  if (getApiMode() === "live") {
    return requestLive<AppointmentDTO>("/api/appointments", {
      method: "POST",
      body: JSON.stringify(dto),
    });
  } else {
    const db = getDB();

    const doctor = db.doctors.find((d) => d.id === dto.doctorId);
    const patient = db.patients.find((p) => p.id === dto.patientId);
    if (!doctor) throw { statusCode: 404, message: "Doctor profile not found" };
    if (!patient) throw { statusCode: 404, message: "Patient profile not found" };

    // Generate unique confirmation code
    const randCode = Math.floor(1000 + Math.random() * 9000);
    const code = `HM-${randCode}-${String.fromCharCode(65 + Math.floor(Math.random() * 26))}${db.appointments.length + 1}`;

    const newAppId = db.appointments.length + 1;
    const app: AppointmentDTO = {
      id: newAppId,
      appointmentDateTime: dto.appointmentDateTime,
      status: "SCHEDULED",
      notes: dto.notes,
      appointmentCode: code,
      doctorId: dto.doctorId,
      patientId: dto.patientId,
      timeSlotId: dto.timeSlotId,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    db.appointments.push(app);

    // Update timeslot status if provided
    if (dto.timeSlotId) {
      const ts = db.timeSlots.find((s) => s.id === dto.timeSlotId);
      if (ts) {
        ts.status = "BOOKED";
        ts.appointmentId = newAppId;
      }
    }

    // Add Audit Logs
    const patientUser = db.users.find((u) => u.email.toLowerCase() === patient.email.toLowerCase());
    db.auditLogs.push({
      id: db.auditLogs.length + 1,
      userId: patientUser?.id,
      action: "APPOINTMENT_BOOKED",
      details: `Booked appointment ${code} with Dr. ${doctor.name} for ${dto.appointmentDateTime}`,
      timestamp: new Date().toISOString(),
    });

    // Notify Patient
    if (patientUser) {
      db.notifications.push({
        id: db.notifications.length + 1,
        userId: patientUser.id,
        message: `Your appointment is scheduled! Code: ${code} with Dr. ${doctor.name} on ${new Date(dto.appointmentDateTime).toLocaleString()}`,
        isRead: false,
        timestamp: new Date().toISOString(),
      });
    }

    // Notify Doctor
    const doctorUser = db.users.find((u) => u.id === doctor.userId);
    if (doctorUser) {
      db.notifications.push({
        id: db.notifications.length + 1,
        userId: doctorUser.id,
        message: `New appointment booked by ${patient.name} for ${new Date(dto.appointmentDateTime).toLocaleString()}. Code: ${code}`,
        isRead: false,
        timestamp: new Date().toISOString(),
      });
    }

    saveDB(db);
    return app;
  }
}

export async function fetchAppointments(): Promise<AppointmentDTO[]> {
  if (getApiMode() === "live") {
    return requestLive<AppointmentDTO[]>("/api/appointments");
  } else {
    return getDB().appointments;
  }
}

export async function fetchAppointmentById(id: number): Promise<AppointmentDTO> {
  if (getApiMode() === "live") {
    return requestLive<AppointmentDTO>(`/api/appointments/${id}`);
  } else {
    const app = getDB().appointments.find((a) => a.id === id);
    if (!app) throw { statusCode: 404, message: "Appointment not found" };
    return app;
  }
}

export async function fetchAppointmentByCode(code: string): Promise<AppointmentDTO> {
  if (getApiMode() === "live") {
    return requestLive<AppointmentDTO>(`/api/appointments/code/${encodeURIComponent(code)}`);
  } else {
    const app = getDB().appointments.find((a) => a.appointmentCode.toLowerCase() === code.toLowerCase());
    if (!app) throw { statusCode: 404, message: "Appointment code not found" };
    return app;
  }
}

export async function fetchAppointmentsByDoctor(doctorId: number): Promise<AppointmentDTO[]> {
  if (getApiMode() === "live") {
    return requestLive<AppointmentDTO[]>(`/api/appointments/doctor/${doctorId}`);
  } else {
    return getDB().appointments.filter((a) => a.doctorId === doctorId);
  }
}

export async function fetchAppointmentsByPatient(patientId: number): Promise<AppointmentDTO[]> {
  if (getApiMode() === "live") {
    return requestLive<AppointmentDTO[]>(`/api/appointments/patient/${patientId}`);
  } else {
    return getDB().appointments.filter((a) => a.patientId === patientId);
  }
}

export async function fetchAppointmentsByStatus(
  status: AppointmentStatus,
  page = 0,
  size = 20,
  sort = "createdAt,desc"
): Promise<PageResponse<AppointmentDTO>> {
  if (getApiMode() === "live") {
    return requestLive<PageResponse<AppointmentDTO>>(
      `/api/appointments/status/${status}?page=${page}&size=${size}&sort=${sort}`
    );
  } else {
    const all = getDB().appointments.filter((a) => a.status === status);
    // Simple mock page splitting
    const content = all.slice(page * size, (page + 1) * size);
    return {
      content,
      totalElements: all.length,
      totalPages: Math.ceil(all.length / size),
      size,
      number: page,
      first: page === 0,
      last: (page + 1) * size >= all.length,
      numberOfElements: content.length,
      empty: content.length === 0,
    };
  }
}

export async function updateAppointmentStatus(id: number, status: AppointmentStatus): Promise<AppointmentDTO> {
  if (getApiMode() === "live") {
    return requestLive<AppointmentDTO>(`/api/appointments/${id}/status?status=${status}`, {
      method: "PATCH",
    });
  } else {
    const db = getDB();
    const appIndex = db.appointments.findIndex((a) => a.id === id);
    if (appIndex === -1) throw { statusCode: 404, message: "Appointment not found" };

    const app = db.appointments[appIndex];
    app.status = status;
    app.updatedAt = new Date().toISOString();

    const doctor = db.doctors.find((d) => d.id === app.doctorId);
    const patient = db.patients.find((p) => p.id === app.patientId);

    // Sync corresponding timeslot if completion/cancel
    if ((status === "CANCELED" || status === "COMPLETED") && app.timeSlotId) {
      const ts = db.timeSlots.find((s) => s.id === app.timeSlotId);
      if (ts) {
        ts.status = status === "CANCELED" ? "AVAILABLE" : "BOOKED";
        if (status === "CANCELED") {
          ts.appointmentId = undefined;
        }
      }
    }

    // Auth audit trail
    db.auditLogs.push({
      id: db.auditLogs.length + 1,
      action: "APPOINTMENT_UPDATE",
      details: `Appointment ${app.appointmentCode} status updated to ${status}`,
      timestamp: new Date().toISOString(),
    });

    // Send patient notifications regarding status shifts
    const patientUser = db.users.find((u) => u.email.toLowerCase() === patient?.email.toLowerCase());
    if (patientUser) {
      db.notifications.push({
        id: db.notifications.length + 1,
        userId: patientUser.id,
        message: `Your appointment reminder (${app.appointmentCode}) has been updated to: ${status}.`,
        isRead: false,
        timestamp: new Date().toISOString(),
      });
    }

    saveDB(db);
    return app;
  }
}

export async function cancelAppointment(id: number): Promise<AppointmentDTO> {
  if (getApiMode() === "live") {
    return requestLive<AppointmentDTO>(`/api/appointments/${id}/cancel`, {
      method: "PATCH",
    });
  } else {
    return updateAppointmentStatus(id, "CANCELED");
  }
}

export async function deleteAppointment(id: number): Promise<void> {
  if (getApiMode() === "live") {
    return requestLive<void>(`/api/appointments/${id}`, { method: "DELETE" });
  } else {
    const db = getDB();
    const index = db.appointments.findIndex((a) => a.id === id);
    if (index === -1) throw { statusCode: 404, message: "Appointment not found" };

    const app = db.appointments[index];
    db.appointments.splice(index, 1);

    // Re-active timeslot
    if (app.timeSlotId) {
      const ts = db.timeSlots.find((s) => s.id === app.timeSlotId);
      if (ts) {
        ts.status = "AVAILABLE";
        ts.appointmentId = undefined;
      }
    }

    saveDB(db);
  }
}

// --------------------
// 6. DOCTOR SCHEDULES
// --------------------

export async function createDoctorSchedule(dto: DoctorScheduleRequestDTO): Promise<DoctorScheduleDTO> {
  if (getApiMode() === "live") {
    return requestLive<DoctorScheduleDTO>("/api/doctor-schedules", {
      method: "POST",
      body: JSON.stringify(dto),
    });
  } else {
    const db = getDB();
    const doc = db.doctors.find((d) => d.id === dto.doctorId);
    if (!doc) throw { statusCode: 404, message: "Doctor profile not found" };

    const newSched: DoctorScheduleDTO = {
      id: db.schedules.length + 1,
      doctorId: dto.doctorId,
      dayOfWeek: dto.dayOfWeek,
      startTime: dto.startTime,
      endTime: dto.endTime,
      isActive: dto.isActive !== undefined ? dto.isActive : true,
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    db.schedules.push(newSched);
    saveDB(db);
    return newSched;
  }
}

export async function fetchDoctorScheduleById(id: number): Promise<DoctorScheduleDTO> {
  if (getApiMode() === "live") {
    return requestLive<DoctorScheduleDTO>(`/api/doctor-schedules/${id}`);
  } else {
    const sch = getDB().schedules.find((s) => s.id === id);
    if (!sch) throw { statusCode: 404, message: "Schedule not found" };
    return sch;
  }
}

export async function fetchDoctorSchedules(doctorId: number): Promise<DoctorScheduleDTO[]> {
  if (getApiMode() === "live") {
    return requestLive<DoctorScheduleDTO[]>(`/api/doctor-schedules/doctor/${doctorId}`);
  } else {
    return getDB().schedules.filter((s) => s.doctorId === doctorId);
  }
}

export async function fetchDoctorSchedulesByDay(doctorId: number, day: DayOfWeek): Promise<DoctorScheduleDTO[]> {
  if (getApiMode() === "live") {
    return requestLive<DoctorScheduleDTO[]>(`/api/doctor-schedules/doctor/${doctorId}/day/${day}`);
  } else {
    return getDB().schedules.filter((s) => s.doctorId === doctorId && s.dayOfWeek === day);
  }
}

export async function updateDoctorSchedule(id: number, dto: DoctorScheduleRequestDTO): Promise<DoctorScheduleDTO> {
  if (getApiMode() === "live") {
    return requestLive<DoctorScheduleDTO>(`/api/doctor-schedules/${id}`, {
      method: "PUT",
      body: JSON.stringify(dto),
    });
  } else {
    const db = getDB();
    const idx = db.schedules.findIndex((s) => s.id === id);
    if (idx === -1) throw { statusCode: 404, message: "Schedule not found" };

    const current = db.schedules[idx];
    const updated: DoctorScheduleDTO = {
      ...current,
      dayOfWeek: dto.dayOfWeek,
      startTime: dto.startTime,
      endTime: dto.endTime,
      isActive: dto.isActive !== undefined ? dto.isActive : current.isActive,
      updatedAt: new Date().toISOString(),
    };

    db.schedules[idx] = updated;
    saveDB(db);
    return updated;
  }
}

export async function toggleDoctorScheduleActive(id: number, active: boolean): Promise<DoctorScheduleDTO> {
  if (getApiMode() === "live") {
    return requestLive<DoctorScheduleDTO>(`/api/doctor-schedules/${id}/active?active=${active}`, {
      method: "PATCH",
    });
  } else {
    const db = getDB();
    const sch = db.schedules.find((s) => s.id === id);
    if (!sch) throw { statusCode: 404, message: "Schedule not found" };

    sch.isActive = active;
    sch.updatedAt = new Date().toISOString();
    saveDB(db);
    return sch;
  }
}

export async function deleteDoctorSchedule(id: number): Promise<void> {
  if (getApiMode() === "live") {
    return requestLive<void>(`/api/doctor-schedules/${id}`, { method: "DELETE" });
  } else {
    const db = getDB();
    db.schedules = db.schedules.filter((s) => s.id !== id);
    saveDB(db);
  }
}

// --------------------
// 7. TIME SLOTS
// --------------------

export async function createTimeSlot(dto: TimeSlotRequestDTO): Promise<TimeSlotDTO> {
  if (getApiMode() === "live") {
    return requestLive<TimeSlotDTO>("/api/time-slots", {
      method: "POST",
      body: JSON.stringify(dto),
    });
  } else {
    const db = getDB();
    const doc = db.doctors.find((d) => d.id === dto.doctorId);
    if (!doc) throw { statusCode: 404, message: "Doctor profile not found" };

    // Check for collision
    const collision = db.timeSlots.some(
      (ts) =>
        ts.doctorId === dto.doctorId &&
        ts.slotDate === dto.slotDate &&
        ((dto.startTime >= ts.startTime && dto.startTime < ts.endTime) ||
          (dto.endTime > ts.startTime && dto.endTime <= ts.endTime))
    );

    if (collision) {
      throw { statusCode: 400, message: "This time slot overlaps with an existing slot for this doctor." };
    }

    const newTs: TimeSlotDTO = {
      id: db.timeSlots.length + 1,
      doctorId: dto.doctorId,
      slotDate: dto.slotDate,
      startTime: dto.startTime,
      endTime: dto.endTime,
      status: "AVAILABLE",
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    };

    db.timeSlots.push(newTs);
    saveDB(db);
    return newTs;
  }
}

export async function fetchTimeSlotById(id: number): Promise<TimeSlotDTO> {
  if (getApiMode() === "live") {
    return requestLive<TimeSlotDTO>(`/api/time-slots/${id}`);
  } else {
    const ts = getDB().timeSlots.find((s) => s.id === id);
    if (!ts) throw { statusCode: 404, message: "Time slot not found" };
    return ts;
  }
}

export async function fetchTimeSlotsByDate(
  doctorId: number,
  slotDate: string,
  status?: string
): Promise<TimeSlotDTO[]> {
  if (getApiMode() === "live") {
    const query = status ? `?status=${encodeURIComponent(status)}` : "";
    return requestLive<TimeSlotDTO[]>(`/api/time-slots/doctor/${doctorId}/date/${slotDate}${query}`);
  } else {
    let list = getDB().timeSlots.filter((ts) => ts.doctorId === doctorId && ts.slotDate === slotDate);
    if (status) {
      list = list.filter((ts) => ts.status.toLowerCase() === status.toLowerCase());
    }
    // Sort chronologically
    return list.sort((a, b) => a.startTime.localeCompare(b.startTime));
  }
}

export async function fetchTimeSlotsByRange(
  doctorId: number,
  startDate: string,
  endDate: string,
  status?: string
): Promise<TimeSlotDTO[]> {
  if (getApiMode() === "live") {
    const qParams = [
      `startDate=${encodeURIComponent(startDate)}`,
      `endDate=${encodeURIComponent(endDate)}`,
    ];
    if (status) qParams.push(`status=${encodeURIComponent(status)}`);
    return requestLive<TimeSlotDTO[]>(`/api/time-slots/doctor/${doctorId}/range?${qParams.join("&")}`);
  } else {
    let list = getDB().timeSlots.filter(
      (ts) => ts.doctorId === doctorId && ts.slotDate >= startDate && ts.slotDate <= endDate
    );
    if (status) {
      list = list.filter((ts) => ts.status.toLowerCase() === status.toLowerCase());
    }
    return list.sort((a, b) => a.slotDate.localeCompare(b.slotDate) || a.startTime.localeCompare(b.startTime));
  }
}

export async function updateTimeSlotStatus(id: number, status: SlotStatus): Promise<TimeSlotDTO> {
  if (getApiMode() === "live") {
    return requestLive<TimeSlotDTO>(`/api/time-slots/${id}/status?status=${status}`, {
      method: "PATCH",
    });
  } else {
    const db = getDB();
    const ts = db.timeSlots.find((s) => s.id === id);
    if (!ts) throw { statusCode: 404, message: "Time slot not found" };

    ts.status = status;
    ts.updatedAt = new Date().toISOString();
    saveDB(db);
    return ts;
  }
}

export async function deleteTimeSlot(id: number): Promise<void> {
  if (getApiMode() === "live") {
    return requestLive<void>(`/api/time-slots/${id}`, { method: "DELETE" });
  } else {
    const db = getDB();
    db.timeSlots = db.timeSlots.filter((ts) => ts.id !== id);
    saveDB(db);
  }
}

// --------------------
// 8. NOTIFICATIONS
// --------------------

export async function createNotification(dto: NotificationRequestDTO): Promise<NotificationDTO> {
  if (getApiMode() === "live") {
    return requestLive<NotificationDTO>("/api/notifications", {
      method: "POST",
      body: JSON.stringify(dto),
    });
  } else {
    const db = getDB();
    const newN: NotificationDTO = {
      id: db.notifications.length + 1,
      userId: dto.userId,
      message: dto.message,
      isRead: false,
      timestamp: new Date().toISOString(),
    };
    db.notifications.push(newN);
    saveDB(db);
    return newN;
  }
}

export async function fetchNotificationById(id: number): Promise<NotificationDTO> {
  if (getApiMode() === "live") {
    return requestLive<NotificationDTO>(`/api/notifications/${id}`);
  } else {
    const n = getDB().notifications.find((x) => x.id === id);
    if (!n) throw { statusCode: 404, message: "Notification not found" };
    return n;
  }
}

export async function fetchNotificationsByUserId(userId: number): Promise<NotificationDTO[]> {
  if (getApiMode() === "live") {
    return requestLive<NotificationDTO[]>(`/api/notifications/user/${userId}`);
  } else {
    return getDB().notifications.filter((n) => n.userId === userId).reverse();
  }
}

export async function fetchUnreadNotificationsByUserId(userId: number): Promise<NotificationDTO[]> {
  if (getApiMode() === "live") {
    return requestLive<NotificationDTO[]>(`/api/notifications/user/${userId}/unread`);
  } else {
    return getDB().notifications.filter((n) => n.userId === userId && !n.isRead).reverse();
  }
}

export async function fetchUnreadNotificationCount(userId: number): Promise<{ count: number }> {
  if (getApiMode() === "live") {
    return requestLive<{ count: number }>(`/api/notifications/user/${userId}/unread-count`);
  } else {
    const count = getDB().notifications.filter((n) => n.userId === userId && !n.isRead).length;
    return { count };
  }
}

export async function markNotificationAsRead(id: number): Promise<NotificationDTO> {
  if (getApiMode() === "live") {
    return requestLive<NotificationDTO>(`/api/notifications/${id}/read`, {
      method: "PATCH",
    });
  } else {
    const db = getDB();
    const n = db.notifications.find((x) => x.id === id);
    if (!n) throw { statusCode: 404, message: "Notification not found" };

    n.isRead = true;
    saveDB(db);
    return n;
  }
}

export async function deleteNotification(id: number): Promise<void> {
  if (getApiMode() === "live") {
    return requestLive<void>(`/api/notifications/${id}`, { method: "DELETE" });
  } else {
    const db = getDB();
    db.notifications = db.notifications.filter((x) => x.id !== id);
    saveDB(db);
  }
}

// --------------------
// 9. AUDIT LOGS
// --------------------

export async function createAuditLog(dto: AuditLogRequestDTO): Promise<AuditLogDTO> {
  if (getApiMode() === "live") {
    return requestLive<AuditLogDTO>("/api/audit-logs", {
      method: "POST",
      body: JSON.stringify(dto),
    });
  } else {
    const db = getDB();
    const log: AuditLogDTO = {
      id: db.auditLogs.length + 1,
      userId: dto.userId,
      action: dto.action,
      details: dto.details,
      timestamp: new Date().toISOString(),
    };
    db.auditLogs.push(log);
    saveDB(db);
    return log;
  }
}

export async function fetchAuditLogsByUserId(
  userId: number,
  page = 0,
  size = 20,
  sort = "timestamp,desc"
): Promise<PageResponse<AuditLogDTO>> {
  if (getApiMode() === "live") {
    return requestLive<PageResponse<AuditLogDTO>>(
      `/api/audit-logs/user/${userId}?page=${page}&size=${size}&sort=${sort}`
    );
  } else {
    const all = getDB().auditLogs.filter((l) => l.userId === userId).reverse();
    const content = all.slice(page * size, (page + 1) * size);
    return {
      content,
      totalElements: all.length,
      totalPages: Math.ceil(all.length / size),
      size,
      number: page,
      first: page === 0,
      last: (page + 1) * size >= all.length,
      numberOfElements: content.length,
      empty: content.length === 0,
    };
  }
}

export async function fetchAuditLogsByRange(start: string, end: string): Promise<AuditLogDTO[]> {
  if (getApiMode() === "live") {
    return requestLive<AuditLogDTO[]>(`/api/audit-logs/range?start=${encodeURIComponent(start)}&end=${encodeURIComponent(end)}`);
  } else {
    return getDB().auditLogs.filter((l) => l.timestamp && l.timestamp >= start && l.timestamp <= end).reverse();
  }
}

export async function deleteAuditLog(id: number): Promise<void> {
  if (getApiMode() === "live") {
    return requestLive<void>(`/api/audit-logs/${id}`, { method: "DELETE" });
  } else {
    const db = getDB();
    db.auditLogs = db.auditLogs.filter((l) => l.id !== id);
    saveDB(db);
  }
}

// --------------------
// 10. OTP OPERATIONS
// --------------------

export async function sendOtp(dto: SendOtpRequestDTO): Promise<OtpTokenDTO> {
  if (getApiMode() === "live") {
    return requestLive<OtpTokenDTO>("/api/otp/send", {
      method: "POST",
      body: JSON.stringify(dto),
    });
  } else {
    const db = getDB();
    const newId = db.otps.length + 1;

    // Generate a 6 digit code
    const code = Math.floor(100000 + Math.random() * 900000).toString();

    const dt: OtpTokenDTO = {
      id: newId,
      email: dto.email,
      purpose: dto.purpose,
      createdAt: new Date().toISOString(),
      expiresAt: new Date(Date.now() + 10 * 60 * 1000).toISOString(), // 10 mins expiry
      isUsed: false,
    };

    db.otps.push(dt);
    db.otpCodes[`${dto.email.toLowerCase()}_${dto.purpose}_${newId}`] = code;

    // Simulate system emailing/SMS-ing OTP or alert
    db.notifications.push({
      id: db.notifications.length + 1,
      userId: db.users.find((u) => u.email.toLowerCase() === dto.email.toLowerCase())?.id || 3, // default admin or system user
      message: `[SECURITY/OTP Alert] Simulated OTP Delivery for ${dto.purpose}: your 6-digit credential code is ${code}`,
      isRead: false,
      timestamp: new Date().toISOString(),
    });

    saveDB(db);

    // Return DTO without code for security
    return dt;
  }
}

export async function verifyOtp(dto: OtpVerificationRequestDTO): Promise<OtpTokenDTO> {
  if (getApiMode() === "live") {
    return requestLive<OtpTokenDTO>("/api/otp/verify", {
      method: "POST",
      body: JSON.stringify(dto),
    });
  } else {
    const db = getDB();
    // Find matching transaction
    const latestTokens = db.otps
      .filter((o) => o.email.toLowerCase() === dto.email.toLowerCase() && o.purpose === dto.purpose && !o.isUsed)
      .reverse();

    if (latestTokens.length === 0) {
      throw { statusCode: 400, message: "No active OTP request found for this email and purpose." };
    }

    const token = latestTokens[0];
    const key = `${dto.email.toLowerCase()}_${dto.purpose}_${token.id}`;
    const correctCode = db.otpCodes[key];

    if (correctCode !== dto.otpCode) {
      throw { statusCode: 400, message: "Invalid OTP code. Please verify again." };
    }

    // Check expiry
    if (token.expiresAt && new Date(token.expiresAt) < new Date()) {
      throw { statusCode: 400, message: "This OTP code has expired." };
    }

    token.isUsed = true;
    token.usedAt = new Date().toISOString();

    db.auditLogs.push({
      id: db.auditLogs.length + 1,
      action: "OTP_VERIFIED",
      details: `OTP code successfully verified for ${dto.email} (Purpose: ${dto.purpose})`,
      timestamp: new Date().toISOString(),
    });

    saveDB(db);
    return token;
  }
}

export async function getLatestOtp(email: string, purpose: OtpPurpose): Promise<OtpTokenDTO> {
  if (getApiMode() === "live") {
    return requestLive<OtpTokenDTO>(
      `/api/otp/latest?email=${encodeURIComponent(email)}&purpose=${encodeURIComponent(purpose)}`
    );
  } else {
    const matches = getDB().otps.filter(
      (o) => o.email.toLowerCase() === email.toLowerCase() && o.purpose === purpose
    );
    if (matches.length === 0) throw { statusCode: 404, message: "No tokens found." };
    return matches[matches.length - 1]; // latest
  }
}

export async function fetchOtpsByEmail(email: string): Promise<OtpTokenDTO[]> {
  if (getApiMode() === "live") {
    return requestLive<OtpTokenDTO[]>(`/api/otp?email=${encodeURIComponent(email)}`);
  } else {
    return getDB().otps.filter((o) => o.email.toLowerCase() === email.toLowerCase());
  }
}

export async function deleteExpiredOtps(): Promise<void> {
  if (getApiMode() === "live") {
    return requestLive<void>("/api/otp/expired", { method: "DELETE" });
  } else {
    const db = getDB();
    const nowStr = new Date().toISOString();
    db.otps = db.otps.filter((o) => o.isUsed || (o.expiresAt && o.expiresAt >= nowStr));
    saveDB(db);
  }
}

// Global lookup helper for dev / admin simulator log inspection
export function getSimulatedCode(email: string, purpose: OtpPurpose): string | undefined {
  const db = getDB();
  const tokens = db.otps.filter(
    (o) => o.email.toLowerCase() === email.toLowerCase() && o.purpose === purpose && !o.isUsed
  );
  if (tokens.length === 0) return undefined;
  const token = tokens[tokens.length - 1];
  return db.otpCodes[`${email.toLowerCase()}_${purpose}_${token.id}`];
}
