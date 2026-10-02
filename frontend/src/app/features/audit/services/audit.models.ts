export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface AuditLogEntry {
  id: number;
  userId: number | null;
  username: string | null;
  action: string;
  entityName: string;
  entityId: string | null;
  beforeJson: string | null;
  afterJson: string | null;
  occurredAt: string;
}
