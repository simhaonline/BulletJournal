import { Task } from '../tasks/interface';
import { Note } from '../notes/interface';
import { Transaction } from '../transactions/interface';
import { Label } from '../label/interface';
import { ContentType } from '../myBuJo/constants';

export interface ProjectItems {
  tasks: Task[];
  notes: Note[];
  transactions: Transaction[];
  date: string;
  dayOfWeek: string;
}

export interface ProjectItem {
  id: number;
  name: string;
  owner?: string;
  ownerAvatar?: string;
  projectId: number;
  labels: Label[];
  contentType: ContentType;
}

export interface Content {
  id: number;
  owner: string;
  text: string;
  createdAt: number;
  updatedAt: number;
  revisions: Revision[];
  ownerAvatar: string;
}

export interface Revision {
  id: number;
  createdAt: number;
  user: string;
  content?: string;
}
