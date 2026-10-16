import {
  AppointmentDTO,
  AppointmentRequestDTO,
  AppointmentStatus,
  AuditLogDTO,
  AuditLogRequestDTO,
  AuthRequestDTO,
  AuthResponseDTO,
  DayOfWeek,
  DoctorDTO,
  DoctorRequestDTO,
  DoctorScheduleDTO,
  DoctorScheduleRequestDTO,
  NotificationDTO,
  NotificationRequestDTO,
  OtpPurpose,
  OtpTokenDTO,
  OtpVerificationRequestDTO,
  PageResponse,
  PasswordResetRequestDTO,
  PatientDTO,
  PatientRequestDTO,
  RegisterRequestDTO,
  SendOtpRequestDTO,
  SlotStatus,
  TimeSlotDTO,
  TimeSlotRequestDTO,
  UserDTO,
  UserUpdateDTO,
} from "../types";

const defaultUrl = "http://localhost:8080";

export const getApiUrl = (): string => {
  const configured = ((import.meta as any).env?.VITE_API_BASE_URL as string | undefined) || defaultUrl;
  return configured.replace(/\/+$/, "");
};

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

async function request<T>(path: string, options: RequestInit = {}): Promise<T> {
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

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  let data: unknown;

  try {
    data = text ? JSON.parse(text) : undefined;
  } catch {
    data = text;
  }

  if (!response.ok) {
    if (response.status === 401) {
      clearSession();
    }
    throw data ?? { statusCode: response.status, message: response.statusText };
  }

  return data as T;
}

const query = (params: Record<string, string | number | boolean | undefined>) => {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") {
      search.set(key, String(value));
    }
  });
  const value = search.toString();
  return value ? `?${value}` : "";
};

export async function loginUser(dto: AuthRequestDTO): Promise<AuthResponseDTO> {
  const response = await request<AuthResponseDTO>("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(dto),
  });
  saveSession(response.token, response.user);
  return response;
}

export async function registerUser(dto: RegisterRequestDTO): Promise<AuthResponseDTO> {
  const response = await request<AuthResponseDTO>("/api/auth/register", {
    method: "POST",
    body: JSON.stringify(dto),
  });
  saveSession(response.token, response.user);
  return response;
}

export async function fetchUsers(): Promise<UserDTO[]> {
  return request<UserDTO[]>("/api/users");
}

export async function fetchUserById(id: number): Promise<UserDTO> {
  return request<UserDTO>(`/api/users/${id}`);
}

export async function fetchUserByEmail(email: string): Promise<UserDTO> {
  return request<UserDTO>(`/api/users/by-email${query({ email })}`);
}

export async function updateUser(id: number, dto: UserUpdateDTO): Promise<UserDTO> {
  const updated = await request<UserDTO>(`/api/users/${id}`, {
    method: "PATCH",
    body: JSON.stringify(dto),
  });
  const current = getSessionUser();
  if (current && current.id === id) {
    saveSession(getSessionToken() || "", updated);
  }
  return updated;
}

export async function deleteUser(id: number): Promise<void> {
  return request<void>(`/api/users/${id}`, { method: "DELETE" });
}

export async function fetchDoctors(specialization?: string): Promise<DoctorDTO[]> {
  return request<DoctorDTO[]>(`/api/doctors${query({ specialization })}`);
}

export async function createDoctor(dto: DoctorRequestDTO): Promise<DoctorDTO> {
  return request<DoctorDTO>("/api/doctors", {
    method: "POST",
    body: JSON.stringify(dto),
  });
}

export async function fetchDoctorById(id: number): Promise<DoctorDTO> {
  return request<DoctorDTO>(`/api/doctors/${id}`);
}

export async function updateDoctor(id: number, dto: DoctorRequestDTO): Promise<DoctorDTO> {
  return request<DoctorDTO>(`/api/doctors/${id}`, {
    method: "PUT",
    body: JSON.stringify(dto),
  });
}

export async function deleteDoctor(id: number): Promise<void> {
  return request<void>(`/api/doctors/${id}`, { method: "DELETE" });
}

export async function createPatient(dto: PatientRequestDTO): Promise<PatientDTO> {
  return request<PatientDTO>("/api/patients", {
    method: "POST",
    body: JSON.stringify(dto),
  });
}

export async function fetchPatients(): Promise<PatientDTO[]> {
  return request<PatientDTO[]>("/api/patients");
}

export async function fetchPatientById(id: number): Promise<PatientDTO> {
  return request<PatientDTO>(`/api/patients/${id}`);
}

export async function fetchPatientByUserId(userId: number): Promise<PatientDTO> {
  return request<PatientDTO>(`/api/patients/user/${userId}`);
}

export async function deletePatient(id: number): Promise<void> {
  return request<void>(`/api/patients/${id}`, { method: "DELETE" });
}

export async function bookAppointment(dto: AppointmentRequestDTO): Promise<AppointmentDTO> {
  return request<AppointmentDTO>("/api/appointments", {
    method: "POST",
    body: JSON.stringify(dto),
  });
}

export async function fetchAppointments(): Promise<AppointmentDTO[]> {
  return request<AppointmentDTO[]>("/api/appointments");
}

export async function fetchAppointmentById(id: number): Promise<AppointmentDTO> {
  return request<AppointmentDTO>(`/api/appointments/${id}`);
}

export async function fetchAppointmentByCode(code: string): Promise<AppointmentDTO> {
  return request<AppointmentDTO>(`/api/appointments/code/${encodeURIComponent(code)}`);
}

export async function fetchAppointmentsByDoctor(doctorId: number): Promise<AppointmentDTO[]> {
  return request<AppointmentDTO[]>(`/api/appointments/doctor/${doctorId}`);
}

export async function fetchAppointmentsByPatient(patientId: number): Promise<AppointmentDTO[]> {
  return request<AppointmentDTO[]>(`/api/appointments/patient/${patientId}`);
}

export async function fetchAppointmentsByStatus(
  status: AppointmentStatus,
  page = 0,
  size = 10,
  sort = "appointmentDateTime,desc"
): Promise<PageResponse<AppointmentDTO>> {
  return request<PageResponse<AppointmentDTO>>(
    `/api/appointments/status/${status}${query({ page, size, sort })}`
  );
}

export async function updateAppointmentStatus(
  id: number,
  status: AppointmentStatus
): Promise<AppointmentDTO> {
  return request<AppointmentDTO>(`/api/appointments/${id}/status${query({ status })}`, {
    method: "PATCH",
  });
}

export async function cancelAppointment(id: number): Promise<AppointmentDTO> {
  return request<AppointmentDTO>(`/api/appointments/${id}/cancel`, { method: "PATCH" });
}

export async function deleteAppointment(id: number): Promise<void> {
  return request<void>(`/api/appointments/${id}`, { method: "DELETE" });
}

export async function createDoctorSchedule(dto: DoctorScheduleRequestDTO): Promise<DoctorScheduleDTO> {
  return request<DoctorScheduleDTO>("/api/doctor-schedules", {
    method: "POST",
    body: JSON.stringify(dto),
  });
}

export async function fetchDoctorScheduleById(id: number): Promise<DoctorScheduleDTO> {
  return request<DoctorScheduleDTO>(`/api/doctor-schedules/${id}`);
}

export async function fetchDoctorSchedules(doctorId: number): Promise<DoctorScheduleDTO[]> {
  return request<DoctorScheduleDTO[]>(`/api/doctor-schedules/doctor/${doctorId}`);
}

export async function fetchDoctorSchedulesByDay(
  doctorId: number,
  day: DayOfWeek
): Promise<DoctorScheduleDTO[]> {
  return request<DoctorScheduleDTO[]>(`/api/doctor-schedules/doctor/${doctorId}/day/${day}`);
}

export async function updateDoctorSchedule(
  id: number,
  dto: DoctorScheduleRequestDTO
): Promise<DoctorScheduleDTO> {
  return request<DoctorScheduleDTO>(`/api/doctor-schedules/${id}`, {
    method: "PUT",
    body: JSON.stringify(dto),
  });
}

export async function toggleDoctorScheduleActive(
  id: number,
  active: boolean
): Promise<DoctorScheduleDTO> {
  return request<DoctorScheduleDTO>(`/api/doctor-schedules/${id}/active${query({ active })}`, {
    method: "PATCH",
  });
}

export async function deleteDoctorSchedule(id: number): Promise<void> {
  return request<void>(`/api/doctor-schedules/${id}`, { method: "DELETE" });
}

export async function createTimeSlot(dto: TimeSlotRequestDTO): Promise<TimeSlotDTO> {
  return request<TimeSlotDTO>("/api/time-slots", {
    method: "POST",
    body: JSON.stringify(dto),
  });
}

export async function fetchTimeSlotById(id: number): Promise<TimeSlotDTO> {
  return request<TimeSlotDTO>(`/api/time-slots/${id}`);
}

export async function fetchTimeSlotsByDate(
  doctorId: number,
  slotDate: string,
  status?: SlotStatus
): Promise<TimeSlotDTO[]> {
  return request<TimeSlotDTO[]>(
    `/api/time-slots/doctor/${doctorId}/date/${slotDate}${query({ status })}`
  );
}

export async function fetchTimeSlotsByRange(
  doctorId: number,
  startDate: string,
  endDate: string,
  status?: SlotStatus
): Promise<TimeSlotDTO[]> {
  return request<TimeSlotDTO[]>(
    `/api/time-slots/doctor/${doctorId}/range${query({ startDate, endDate, status })}`
  );
}

export async function updateTimeSlotStatus(id: number, status: SlotStatus): Promise<TimeSlotDTO> {
  return request<TimeSlotDTO>(`/api/time-slots/${id}/status${query({ status })}`, {
    method: "PATCH",
  });
}

export async function deleteTimeSlot(id: number): Promise<void> {
  return request<void>(`/api/time-slots/${id}`, { method: "DELETE" });
}

export async function createNotification(dto: NotificationRequestDTO): Promise<NotificationDTO> {
  return request<NotificationDTO>("/api/notifications", {
    method: "POST",
    body: JSON.stringify(dto),
  });
}

export async function fetchNotificationById(id: number): Promise<NotificationDTO> {
  return request<NotificationDTO>(`/api/notifications/${id}`);
}

export async function fetchNotificationsByUserId(userId: number): Promise<NotificationDTO[]> {
  return request<NotificationDTO[]>(`/api/notifications/user/${userId}`);
}

export async function fetchUnreadNotificationsByUserId(userId: number): Promise<NotificationDTO[]> {
  return request<NotificationDTO[]>(`/api/notifications/user/${userId}/unread`);
}

export async function fetchUnreadNotificationCount(userId: number): Promise<{ count: number }> {
  return request<{ count: number }>(`/api/notifications/user/${userId}/unread-count`);
}

export async function markNotificationAsRead(id: number): Promise<NotificationDTO> {
  return request<NotificationDTO>(`/api/notifications/${id}/read`, { method: "PATCH" });
}

export async function deleteNotification(id: number): Promise<void> {
  return request<void>(`/api/notifications/${id}`, { method: "DELETE" });
}

export async function createAuditLog(dto: AuditLogRequestDTO): Promise<AuditLogDTO> {
  return request<AuditLogDTO>("/api/audit-logs", {
    method: "POST",
    body: JSON.stringify(dto),
  });
}

export async function fetchAuditLogs(): Promise<AuditLogDTO[]> {
  return request<AuditLogDTO[]>("/api/audit-logs");
}

export async function fetchAuditLogsByUserId(
  userId: number,
  page = 0,
  size = 20,
  sort = "timestamp,desc"
): Promise<PageResponse<AuditLogDTO>> {
  return request<PageResponse<AuditLogDTO>>(
    `/api/audit-logs/user/${userId}${query({ page, size, sort })}`
  );
}

export async function fetchAuditLogsByRange(start: string, end: string): Promise<AuditLogDTO[]> {
  return request<AuditLogDTO[]>(`/api/audit-logs/range${query({ start, end })}`);
}

export async function deleteAuditLog(id: number): Promise<void> {
  return request<void>(`/api/audit-logs/${id}`, { method: "DELETE" });
}

export async function sendOtp(dto: SendOtpRequestDTO): Promise<OtpTokenDTO> {
  return request<OtpTokenDTO>("/api/otp/send", {
    method: "POST",
    body: JSON.stringify(dto),
  });
}

export async function verifyOtp(dto: OtpVerificationRequestDTO): Promise<OtpTokenDTO> {
  return request<OtpTokenDTO>("/api/otp/verify", {
    method: "POST",
    body: JSON.stringify(dto),
  });
}

export async function resetPassword(dto: PasswordResetRequestDTO): Promise<{ message: string }> {
  return request<{ message: string }>("/api/auth/password-reset", {
    method: "POST",
    body: JSON.stringify(dto),
  });
}

export async function getLatestOtp(email: string, purpose: OtpPurpose): Promise<OtpTokenDTO> {
  return request<OtpTokenDTO>(`/api/otp/latest${query({ email, purpose })}`);
}

export async function fetchOtpsByEmail(email: string): Promise<OtpTokenDTO[]> {
  return request<OtpTokenDTO[]>(`/api/otp${query({ email })}`);
}

export async function deleteExpiredOtps(): Promise<void> {
  return request<void>("/api/otp/expired", { method: "DELETE" });
}
