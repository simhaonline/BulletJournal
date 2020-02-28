package com.bulletjournal.controller;

import com.bulletjournal.clients.UserClient;
import com.bulletjournal.controller.models.CreateTaskParams;
import com.bulletjournal.controller.models.Task;
import com.bulletjournal.controller.models.UpdateTaskParams;
import com.bulletjournal.notifications.Event;
import com.bulletjournal.notifications.NotificationService;
import com.bulletjournal.notifications.RemoveTaskEvent;
import com.bulletjournal.repository.TaskDaoJpa;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;

@RestController
public class TaskController {

    protected static final String TASKS_ROUTE = "/api/projects/{projectId}/tasks";
    protected static final String TASK_ROUTE = "/api/tasks/{taskId}";
    protected static final String COMPLETE_TASK_ROUTE = "/api/tasks/{taskId}/complete";
    protected static final String COMPLETED_TASKS_ROUTE = "/api/projects/{projectId}/completedTasks";

    @Autowired
    private TaskDaoJpa taskDaoJpa;

    @Autowired
    private NotificationService notificationService;

    @GetMapping(TASKS_ROUTE)
    public List<Task> getTasks(@NotNull @PathVariable Long projectId) {
        return this.taskDaoJpa.getTasks(projectId);
    }

    @GetMapping(TASK_ROUTE)
    public Task getTask(@NotNull @PathVariable Long taskId) {
        return this.taskDaoJpa.getTask(taskId).toPresentationModel();
    }

    @PostMapping(TASKS_ROUTE)
    @ResponseStatus(HttpStatus.CREATED)
    public Task createTask(@NotNull @PathVariable Long projectId,
                           @Valid @RequestBody CreateTaskParams task) {
        String username = MDC.get(UserClient.USER_NAME_KEY);
        return taskDaoJpa.create(projectId, username, task).toPresentationModel();
    }

    @PatchMapping(TASK_ROUTE)
    public Task updateTask(@NotNull @PathVariable Long taskId,
                           @Valid @RequestBody UpdateTaskParams updateTaskParams) {
        String username = MDC.get(UserClient.USER_NAME_KEY);
        return this.taskDaoJpa.partialUpdate(username, taskId, updateTaskParams).toPresentationModel();
    }


    @PutMapping(TASKS_ROUTE)
    public void updateTaskRelations(@NotNull @PathVariable Long projectId, @Valid @RequestBody List<Task> tasks) {
        this.taskDaoJpa.updateUserTasks(projectId, tasks);
    }

    @PostMapping(COMPLETE_TASK_ROUTE)
    public Task completeTask(@NotNull @PathVariable Long taskId) {
        String username = MDC.get(UserClient.USER_NAME_KEY);
        return this.taskDaoJpa.complete(username, taskId).toPresentationModel();
    }

    @DeleteMapping(TASK_ROUTE)
    public void deleteTask(@NotNull @PathVariable Long taskId) {
        String username = MDC.get(UserClient.USER_NAME_KEY);
        List<Event> events = this.taskDaoJpa.deleteTask(username, taskId);
        if (!events.isEmpty()) {
            this.notificationService.inform(new RemoveTaskEvent(events, username));
        }
    }

    @GetMapping(COMPLETED_TASKS_ROUTE)
    public List<Task> getCompletedTasks(@NotNull @PathVariable Long projectId) {
        return null;
    }
}