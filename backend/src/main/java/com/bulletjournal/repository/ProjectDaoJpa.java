package com.bulletjournal.repository;

import com.bulletjournal.controller.models.CreateProjectParams;
import com.bulletjournal.controller.models.UpdateProjectParams;
import com.bulletjournal.controller.utils.ProjectRelationsProcessor;
import com.bulletjournal.exceptions.ResourceNotFoundException;
import com.bulletjournal.exceptions.UnAuthorizedException;
import com.bulletjournal.repository.models.Group;
import com.bulletjournal.repository.models.Project;
import com.bulletjournal.repository.models.User;
import com.bulletjournal.repository.models.UserProjects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Repository
public class ProjectDaoJpa {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private GroupRepository groupRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserProjectsRepository userProjectsRepository;

    @Autowired
    private UserDaoJpa userDaoJpa;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public List<com.bulletjournal.controller.models.Project> getProjects(String owner) {
        UserProjects userProjects = this.userProjectsRepository.findById(owner)
                .orElseThrow(() -> new ResourceNotFoundException("UserProjects " + owner + " not found"));
        Map<Long, Project> projects = this.projectRepository.findByOwner(owner)
                .stream().collect(Collectors.toMap(p -> p.getId(), p -> p));
        return ProjectRelationsProcessor.processProjectRelations(
                projects, userProjects.getProjects());
    }

    private List<Project> merge(Map<Long, Project> projects,
                                List<com.bulletjournal.controller.models.Project> projectRelations) {
        List<Project> result = new ArrayList<>();
        for (com.bulletjournal.controller.models.Project p : projectRelations) {
            result.add(projects.get(p.getId()));
            for (com.bulletjournal.controller.models.Project subProject : p.getSubProjects()) {

            }
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Project create(CreateProjectParams createProjectParams, String owner) {
        List<User> userList = this.userRepository.findByName(owner);
        if (userList.isEmpty()) {
            this.userDaoJpa.create(owner);
        }

        Project project = new Project();
        project.setOwner(owner);
        project.setName(createProjectParams.getName());
        project.setType(createProjectParams.getProjectType().getValue());
        project.setGroup(this.groupRepository.findByNameAndOwner(Group.DEFAULT_NAME, owner).get(0));
        this.projectRepository.save(project);
        return project;
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public Project partialUpdate(String requester, Long projectId, UpdateProjectParams updateProjectParams) {
        Project project = this.projectRepository
                .findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("Project " + projectId + " not found"));

        if (!Objects.equals(project.getOwner(), requester)) {
            throw new UnAuthorizedException("Project " + projectId + " is owner by " +
                    project.getOwner() + " while request is from " + requester);
        }

        updateIfPresent(
                updateProjectParams.hasName(), updateProjectParams.getName(), (value) -> project.setName(value));

        if (updateProjectParams.hasGroupId() &&
                !Objects.equals(updateProjectParams.getGroupId(), project.getGroup().getId())) {
            Group group = this.groupRepository
                    .findById(updateProjectParams.getGroupId())
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Group " + updateProjectParams.getGroupId() + " not found"));
            project.setGroup(group);
        }

        return this.projectRepository.save(project);
    }

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRED)
    public void updateUserProjects(String user, List<com.bulletjournal.controller.models.Project> projects) {
        UserProjects userProjects = new UserProjects();
        userProjects.setProjects(ProjectRelationsProcessor.processProjectRelations(projects));
        userProjects.setName(user);
        this.userProjectsRepository.save(userProjects);
    }

    private <T> void updateIfPresent(Boolean isPresent, T value, Consumer<T> getter) {
        if (isPresent) {
            getter.accept(value);
        }
    }
}