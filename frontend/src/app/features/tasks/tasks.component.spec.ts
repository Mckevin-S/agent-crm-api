import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of } from 'rxjs';
import { ApiService } from '../../core/api.service';
import { Task } from '../../core/models';
import { NotifyService } from '../../core/notify.service';
import { addDays, isoDate } from '../shared/dates';
import { FollowUpActions } from '../shared/follow-up.service';
import { TasksComponent } from './tasks.component';

function task(id: number, offset: number, status: Task['status'] = 'OPEN'): Task {
  return {
    id, customerId: 'C-10234', title: `Tâche ${id}`, priority: 'MEDIUM', priorityLabel: 'Moyenne',
    dueDate: isoDate(addDays(offset)), status, overdue: offset < 0, createdAt: new Date().toISOString(),
  };
}

describe('TasksComponent', () => {
  const tasks = [task(1, -2), task(2, 0), task(3, 4), task(4, -10, 'DONE')];

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TasksComponent],
      providers: [
        provideNoopAnimations(),
        { provide: ApiService, useValue: { tasks: () => of(tasks) } },
        { provide: NotifyService, useValue: { error: () => undefined } },
        { provide: FollowUpActions, useValue: {} },
      ],
    });
  });

  it("regroupe les tâches ouvertes par échéance", () => {
    const component = TestBed.createComponent(TasksComponent).componentInstance;
    component.ngOnInit();
    const groups = component.groups();
    expect(groups.map((g) => g.key)).toEqual(['overdue', 'today', 'upcoming']);
    expect(groups.map((g) => g.tasks.map((t) => t.id))).toEqual([[1], [2], [3]]);
  });

  it('ajoute les tâches terminées sur demande', () => {
    const component = TestBed.createComponent(TasksComponent).componentInstance;
    component.ngOnInit();
    component.showDone.set(true);
    expect(component.groups().at(-1)?.tasks.map((t) => t.id)).toEqual([4]);
  });
});
