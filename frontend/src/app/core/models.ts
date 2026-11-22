export type Priority = 'LOW' | 'MEDIUM' | 'HIGH';
export type TaskStatus = 'OPEN' | 'DONE' | 'CANCELLED';
export type Channel = 'PHONE' | 'EMAIL' | 'MEETING' | 'VIDEO';
export type Direction = 'INBOUND' | 'OUTBOUND';

export interface AgentProfile {
  id: number;
  fullName: string;
  email: string;
  manager: boolean;
  initials: string;
}

export interface Session {
  token: string;
  expiresAt: string;
  agent: AgentProfile;
}

export interface Customer {
  id: string;
  title?: string;
  firstName: string;
  lastName: string;
  fullName: string;
  birthDate?: string;
  email?: string;
  phone?: string;
  city?: string;
  postalCode?: string;
  segment: 'INDIVIDUAL' | 'PROFESSIONAL';
  segmentLabel: string;
  customerSince?: string;
  initials: string;
}

export interface CustomerSummary {
  customer: Customer;
  advisorName?: string;
}

export interface SearchResult {
  customers: CustomerSummary[];
  live: boolean;
}

export interface Contract {
  number: string;
  product: string;
  productLabel: string;
  label: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'TERMINATED';
  statusLabel: string;
  annualPremium: number;
  startDate: string;
  renewalDate: string;
  daysUntilRenewal: number;
}

export interface Claim {
  number: string;
  contractNumber: string;
  nature: string;
  status: string;
  statusLabel: string;
  pending: boolean;
  openedOn: string;
  ageInDays: number;
  estimatedAmount?: number;
}

export interface Recommendation {
  code: string;
  reference: string;
  priority: Priority;
  priorityLabel: string;
  title: string;
  rationale: string;
  suggestedDueDate: string;
}

export interface Task {
  id: number;
  customerId: string;
  customerName?: string;
  title: string;
  notes?: string;
  priority: Priority;
  priorityLabel: string;
  dueDate: string;
  status: TaskStatus;
  overdue: boolean;
  recommendationCode?: string;
  createdAt: string;
  completedAt?: string;
}

export interface Interaction {
  id: number;
  channel: Channel;
  channelLabel: string;
  direction: Direction;
  subject: string;
  notes?: string;
  agentName: string;
  occurredAt: string;
}

export interface Customer360 {
  customer: Customer;
  advisorName?: string;
  live: boolean;
  fetchedAt: string;
  kpis: { annualPremium: number; activeContracts: number; pendingClaims: number; seniorityYears: number; age: number };
  recommendations: Recommendation[];
  contracts: Contract[];
  claims: Claim[];
  openTasks: Task[];
  closedTasks: Task[];
  interactions: Interaction[];
}

export interface PortfolioRecommendation {
  customerId: string;
  customerName: string;
  recommendation: Recommendation;
}

export interface Dashboard {
  customers: number;
  annualPremium: number;
  renewalsNext30Days: number;
  pendingClaims: number;
  openTasks: number;
  overdueTasks: number;
  dueToday: number;
  recommendations: PortfolioRecommendation[];
  upcomingTasks: Task[];
  live: boolean;
}

export interface TeamMember {
  agentId: number;
  fullName: string;
  customers: number;
  openTasks: number;
  overdueTasks: number;
  openRecommendations: number;
  interactionsLast30Days: number;
}

export interface CrmState {
  available: boolean;
  circuitState: string;
  lastSuccessAt?: string;
  lastFailureAt?: string;
  outageSimulated: boolean;
}

export interface Problem {
  title?: string;
  detail?: string;
  status?: number;
  code?: string;
}

export const CHANNEL_LABELS: Record<Channel, string> = {
  PHONE: 'Téléphone',
  EMAIL: 'E-mail',
  MEETING: 'Rendez-vous',
  VIDEO: 'Visio',
};

export const CHANNEL_ICONS: Record<Channel, string> = {
  PHONE: 'call',
  EMAIL: 'mail',
  MEETING: 'handshake',
  VIDEO: 'videocam',
};

export const PRODUCT_ICONS: Record<string, string> = {
  AUTO: 'directions_car',
  HOME: 'home',
  HEALTH: 'health_and_safety',
  PROTECTION: 'shield',
  SAVINGS: 'savings',
};

export const PRIORITY_LABELS: Record<Priority, string> = { HIGH: 'Haute', MEDIUM: 'Moyenne', LOW: 'Basse' };
