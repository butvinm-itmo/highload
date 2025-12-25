// User & Auth Types
export interface Role {
  id: string;
  name: 'USER' | 'MEDIUM' | 'ADMIN';
}

export interface UserDto {
  id: string;
  username: string;
  role: Role;
  createdAt: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface AuthTokenResponse {
  token: string;
  expiresAt: string;
  username: string;
  role: Role | string; // Backend returns string, but we convert to Role object
}

export interface CreateUserRequest {
  username: string;
  password: string;
  role?: 'USER' | 'MEDIUM' | 'ADMIN';
}

export interface UpdateUserRequest {
  username?: string;
  password?: string;
  role?: 'USER' | 'MEDIUM' | 'ADMIN';
}

// Tarot Card Types
export interface ArcanaTypeDto {
  id: string;
  name: 'MAJOR' | 'MINOR';
}

export interface CardDto {
  id: string;
  name: string;
  arcanaType: ArcanaTypeDto;
}

export interface LayoutTypeDto {
  id: string;
  name: string;
  cardsCount: number;
}

// Spread Types
export interface SpreadCardDto {
  id: string;
  card: CardDto;
  positionInSpread: number;
  isReversed: boolean;
}

export interface InterpretationDto {
  id: string;
  text: string;
  createdAt: string;
  author: UserDto;
  spreadId: string;
  fileUrl?: string;
}

export interface SpreadDto {
  id: string;
  question: string;
  layoutType: LayoutTypeDto;
  createdAt: string;
  author: UserDto;
  cards: SpreadCardDto[];
  interpretations: InterpretationDto[];
}

export interface SpreadSummaryDto {
  id: string;
  question: string;
  layoutTypeName: string;
  createdAt: string;
  authorUsername: string;
  cardsCount: number;
  interpretationsCount: number;
}

export interface CreateSpreadRequest {
  question: string;
  layoutTypeId: string;
}

export interface CreateInterpretationRequest {
  text: string;
}

export interface UpdateInterpretationRequest {
  text: string;
}

// Pagination & Response Types
export interface PaginatedResponse<T> {
  data: T[];
  totalCount: number;
}

export interface ScrollResponse<T> {
  data: T[];
  afterCursor?: string;
}

// File Upload Types
export interface FileUploadResponse {
  url: string;
}

// Notification Types
export type NotificationType = 'NEW_SPREAD' | 'NEW_INTERPRETATION';

export type ReferenceType = 'SPREAD' | 'INTERPRETATION';

export interface NotificationDto {
  id: string;
  type: NotificationType;
  title: string;
  message: string;
  isRead: boolean;
  createdAt: string;
  referenceId: string;
  referenceType: ReferenceType;
}

export interface UnreadCountResponse {
  count: number;
}

export interface MarkAllReadResponse {
  markedAsRead: number;
}

// API Error Response
export interface ApiError {
  message: string;
  status: number;
  timestamp?: string;
}
