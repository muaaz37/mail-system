import {User} from './user';

export type LoginRequest = {
  email: string;
  password: string;
}

export type RegisterRequest = {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
}

export type AuthResponse = {
  user: User;
  token: string;
}
