/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

export type Gender = "MALE" | "FEMALE" | "OTHER";
export type UserRole = "ROLE_ADMIN" | "ROLE_PATIENT" | "ROLE_DOCTOR";
export type AppointmentStatus = "SCHEDULED" | "COMPLETED" | "CANCELED" | "CONFIRMED";
export type SlotStatus = "AVAILABLE" | "BOOKED" | "BLOCKED";
export type OtpPurpose = "PASSWORD_CHANGE" | "PASSWORD_RESET";
export type DayOfWeek =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

export interface AuthRequestDTO {
  email: string;
  password: string;
}

export interface AuthResponseDTO {
  token: string;
  tokenType: "Bearer" | string;
  expiresIn: number;
  user: UserDTO;
}

export interface RegisterRequestDTO {
  name: string;
  email: string;
  password: string;
  phoneNumber: string;
  role?: UserRole;
  gender: Gender;
  dateOfBirth: string;
  address: string;
}

export interface UserDTO {
  id: number;
  name: string;
  email: string;
  phoneNumber: string;
  role: UserRole;
  gender: Gender;
  dateOfBirth: string;
  address: string;
  createdDate?: string;
  updatedDate?: string;
}

export interface UserUpdateDTO {
  name?: string;
  phoneNumber?: string;
  gender?: Gender;
  dateOfBirth?: string;
  address?: string;
}

export interface DoctorDTO {
  id: number;
  experience: number;
  availableHours: string;
  specialization: string;
  createdAt?: string;
  updatedAt?: string;
  userId: number;
  name: string;
  email: string;
  phone: string;
}

export interface DoctorRequestDTO {
  experience: number;
  availableHours: string;
  specialization: string;
  userId: number;
}

export interface PatientDTO {
  id: number;
  name: string;
  email: string;
  phone: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface PatientRequestDTO {
  userId: number;
}

export interface AppointmentDTO {
  id: number;
  appointmentDateTime: string;
  status: AppointmentStatus;
  notes?: string;
  appointmentCode: string;
  createdAt?: string;
  updatedAt?: string;
  doctorId: number;
  patientId: number;
  timeSlotId?: number;
}

export interface AppointmentRequestDTO {
  appointmentDateTime: string;
  notes?: string;
  doctorId: number;
  patientId: number;
  timeSlotId?: number;
}

export interface DoctorScheduleDTO {
  id: number;
  doctorId: number;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface DoctorScheduleRequestDTO {
  doctorId: number;
  dayOfWeek: DayOfWeek;
  startTime: string;
  endTime: string;
  isActive?: boolean;
}

export interface TimeSlotDTO {
  id: number;
  doctorId: number;
  slotDate: string;
  startTime: string;
  endTime: string;
  status: SlotStatus;
  appointmentId?: number;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface TimeSlotRequestDTO {
  doctorId: number;
  slotDate: string;
  startTime: string;
  endTime: string;
}

export interface NotificationDTO {
  id: number;
  userId: number;
  message: string;
  isRead: boolean;
  timestamp?: string;
}

export interface NotificationRequestDTO {
  userId: number;
  message: string;
}

export interface AuditLogDTO {
  id: number;
  userId?: number;
  action: string;
  details?: string;
  timestamp?: string;
}

export interface AuditLogRequestDTO {
  userId?: number;
  action: string;
  details?: string;
}

export interface SendOtpRequestDTO {
  email: string;
  purpose: OtpPurpose;
}

export interface OtpVerificationRequestDTO {
  email: string;
  otpCode: string;
  purpose: OtpPurpose;
}

export interface OtpTokenDTO {
  id: number;
  email: string;
  purpose: OtpPurpose;
  createdAt?: string;
  expiresAt?: string;
  isUsed: boolean;
  usedAt?: string;
}

export interface ApiError {
  statusCode?: number;
  message?: string;
  error?: string;
  path?: string;
  timestamp?: string;
  errors?: Record<string, string | string[]>;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
  sort?: unknown;
  pageable?: unknown;
}
