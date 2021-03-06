import {all, call, put, select, takeLatest} from 'redux-saga/effects';
import {message} from 'antd';
import {actions as systemActions, GetPublicProjectItem, SystemApiErrorAction, UpdateSystem} from './reducer';
import {PayloadAction} from 'redux-starter-kit';
import {fetchSystemUpdates, getPublicProjectItem} from '../../apis/systemApis';
import {ContentType} from "../myBuJo/constants";
import {getProject, updateProjects} from '../project/actions';
import {updateGroups} from '../group/actions';
import {updateNotifications} from '../notification/actions';
import {ProjectType} from "../project/constants";
import { Task } from '../tasks/interface';

const removeTasksFromLocal = (tasks: Task[]) => {
    tasks.forEach((task: Task)=>{
        localStorage.removeItem(task.name);
    });
}

const saveTasksIntoLocal = (tasks: Task[]) => {
    tasks.forEach((task: Task)=>{
        localStorage.setItem(task.name, `${new Date().valueOf()}`);
    });
}

function* systemApiErrorAction(action: PayloadAction<SystemApiErrorAction>) {
    yield call(message.error, `System Error Received: ${action.payload.error}`);
}

function* SystemUpdate(action: PayloadAction<UpdateSystem>) {
    try {
        const state = yield select();
        const selectedProject = state.project.project;
        const remindingTasks = state.system.reminders;
        const data = yield call(fetchSystemUpdates, '',
            selectedProject && selectedProject.projectType !== ProjectType.LEDGER ? selectedProject.id : undefined);

        const {groupsEtag, notificationsEtag, ownedProjectsEtag, sharedProjectsEtag, remindingTaskEtag} = state.system;
        if (ownedProjectsEtag !== data.ownedProjectsEtag || sharedProjectsEtag !== data.sharedProjectsEtag) {
            yield put(updateProjects());
        }

        if (groupsEtag !== data.groupsEtag) {
            yield put(updateGroups());
        }

        if (notificationsEtag !== data.notificationsEtag) {
            yield put(updateNotifications());
        }

        let newCommingTasks = [] as Task[];
        let endTasks = [] as Task[];

        if(remindingTaskEtag !== data.remindingTaskEtag){
            newCommingTasks = data.reminders.filter((task: any)=>!remindingTasks.includes(task));
            endTasks = remindingTasks.filter((task: any) => !data.reminders.includes(task));
        }

        saveTasksIntoLocal(newCommingTasks);
        removeTasksFromLocal(endTasks);

        let tasksEtag = state.system.tasksEtag;
        let notesEtag = state.system.notesEtag;

        switch (selectedProject.projectType) {
            case ProjectType.TODO:
                if (data.tasksEtag !== tasksEtag) {
                    tasksEtag = data.tasksEtag;
                    yield put(getProject(selectedProject.id))
                }
                break;
            case ProjectType.NOTE:
                if (data.notesEtag !== notesEtag) {
                    notesEtag = data.notesEtag;
                    yield put(getProject(selectedProject.id))
                }
                break;
            default:
        }

        yield put(
            systemActions.systemUpdateReceived({
                tasksEtag: tasksEtag,
                notesEtag: notesEtag,
                groupsEtag: data.groupsEtag,
                notificationsEtag: data.notificationsEtag,
                ownedProjectsEtag: data.ownedProjectsEtag,
                sharedProjectsEtag: data.sharedProjectsEtag,
                remindingTaskEtag: data.remindingTaskEtag,
                reminders: data.reminders
            })
        );
    } catch (error) {
        yield call(message.error, `System Error Received: ${error}`);
    }
}

function* getPublicItem(action: PayloadAction<GetPublicProjectItem>) {
    try {
        const {itemId} = action.payload;
        const data = yield call(getPublicProjectItem, itemId);
        const type: ContentType = data.contentType;
        const note = type === ContentType.NOTE ? data.projectItem : undefined;
        const task = type === ContentType.TASK ? data.projectItem : undefined;
        yield put(systemActions.publicProjectItemReceived({
            contentType: type, contents: data.contents,
            publicNote: note, publicTask: task
        }));
    } catch (error) {
        yield call(message.error, `getPublicItem Received: ${error}`);
    }
}

export default function* systemSagas() {
    yield all([
        yield takeLatest(
            systemActions.systemApiErrorReceived.type,
            systemApiErrorAction
        ),
        yield takeLatest(systemActions.systemUpdate.type, SystemUpdate),
        yield takeLatest(systemActions.fetchPublicProjectItem.type, getPublicItem)
    ]);
}
