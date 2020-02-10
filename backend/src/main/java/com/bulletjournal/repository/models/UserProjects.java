package com.bulletjournal.repository.models;

import javax.persistence.*;

@Entity
@Table(name = "user_projects")
public class UserProjects extends AuditModel {

    @Id
    private String name;

    @Lob
    @Column(columnDefinition="BLOB")
    private byte[] projects;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public byte[] getProjects() {
        return projects;
    }

    public void setProjects(byte[] projects) {
        this.projects = projects;
    }
}