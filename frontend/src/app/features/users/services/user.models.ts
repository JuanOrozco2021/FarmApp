export interface UserSummary {
  id: number;
  username: string;
  email: string;
  fullName: string;
  active: boolean;
  roles: string[];
}

export interface CreateUserPayload {
  username: string;
  email: string;
  password: string;
  fullName: string;
  roleCodes: string[];
}

export interface RoleOption {
  id: number;
  code: string;
  name: string;
  permissions: string[];
}
