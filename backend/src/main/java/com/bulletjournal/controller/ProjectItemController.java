package com.bulletjournal.controller;

import com.bulletjournal.clients.UserClient;
import com.bulletjournal.controller.models.ProjectItemType;
import com.bulletjournal.controller.models.ProjectItems;
import com.bulletjournal.controller.models.ProjectType;
import com.bulletjournal.controller.utils.ProjectItemsGrouper;
import com.bulletjournal.controller.utils.ZonedDateTimeHelper;
import com.bulletjournal.repository.LabelDaoJpa;
import com.bulletjournal.repository.TaskDaoJpa;
import com.bulletjournal.repository.TransactionDaoJpa;
import com.bulletjournal.repository.UserDaoJpa;
import com.bulletjournal.repository.models.Task;
import com.bulletjournal.repository.models.Transaction;
import com.bulletjournal.repository.models.User;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
public class ProjectItemController {

    protected static final String PROJECT_ITEMS_ROUTE = "/api/projectItems";

    @Autowired
    private TaskDaoJpa taskDaoJpa;

    @Autowired
    private TransactionDaoJpa transactionDaoJpa;

    @Autowired
    private LabelDaoJpa labelDaoJpa;

    @Autowired
    private UserDaoJpa userDaoJpa;

    @Autowired
    private UserClient userClient;

    @GetMapping(PROJECT_ITEMS_ROUTE)
    @ResponseBody
    public List<ProjectItems> getProjectItems(
            @Valid @RequestParam List<ProjectType> types,
            @NotBlank @RequestParam String startDate,
            @NotBlank @RequestParam String endDate,
            @NotBlank @RequestParam String timezone) {

        if (types.isEmpty()) {
            return Collections.emptyList();
        }

        String username = MDC.get(UserClient.USER_NAME_KEY);

        // Set start time and end time
        ZonedDateTime startTime = ZonedDateTimeHelper.getStartTime(startDate, null, timezone);
        ZonedDateTime endTime = ZonedDateTimeHelper.getEndTime(endDate, null, timezone);

        Map<ProjectItemType, Map<Long, List<Long>>> labelIds = new HashMap<>();
        Map<ZonedDateTime, ProjectItems> projectItemsMap =
                getZonedDateTimeProjectItemsMap(types, username, startTime, endTime, labelIds);
        List<ProjectItems> projectItems = ProjectItemsGrouper.getSortedProjectItems(projectItemsMap);
        return ProjectItems.addOwnerAvatar(
                this.labelDaoJpa.getLabelsForProjectItems(projectItems, labelIds),
                this.userClient);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    protected Map<ZonedDateTime, ProjectItems> getZonedDateTimeProjectItemsMap(
            List<ProjectType> types, String username, ZonedDateTime startTime, ZonedDateTime endTime,
            Map<ProjectItemType, Map<Long, List<Long>>> labelIds) {

        Map<ZonedDateTime, List<Task>> taskMap = null;
        Map<ZonedDateTime, List<Transaction>> transactionMap = null;
        User user = this.userDaoJpa.getByName(username);

        // Task query
        if (types.contains(ProjectType.TODO)) {
            List<Task> tasks = taskDaoJpa.getTasksBetween(user.getName(), startTime, endTime);
            Map<Long, List<Long>> taskLabels = tasks.stream().distinct().collect(
                    Collectors.toMap(t -> t.getId(), t -> t.getLabels()));
            labelIds.put(ProjectItemType.TASK, taskLabels);
            // Group tasks by date
            taskMap = ProjectItemsGrouper.groupTasksByDate(tasks, false);
        }
        // Ledger query
        if (types.contains(ProjectType.LEDGER)) {
            List<Transaction> transactions = transactionDaoJpa.getTransactionsBetween(user.getName(), startTime, endTime);
            Map<Long, List<Long>> transactionLabels = transactions.stream().collect(
                    Collectors.toMap(t -> t.getId(), t -> t.getLabels()));
            labelIds.put(ProjectItemType.TRANSACTION, transactionLabels);
            // Group transaction by date
            transactionMap = ProjectItemsGrouper.groupTransactionsByDate(transactions);
        }

        Map<ZonedDateTime, ProjectItems> projectItemsMap = new HashMap<>();
        projectItemsMap = ProjectItemsGrouper.mergeTasksMap(projectItemsMap, taskMap);
        projectItemsMap = ProjectItemsGrouper.mergeTransactionsMap(projectItemsMap, transactionMap);

        return projectItemsMap;
    }
}
