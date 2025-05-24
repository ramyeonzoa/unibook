package com.unibook.domain.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "schools")
public class School {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long schoolId;

    @Column(nullable = false, length = 100)
    private String schoolName;

    @Column(nullable = false, length = 100)
    private String primaryDomain;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "school_domains", joinColumns = @JoinColumn(name = "school_id"))
    @Column(name = "domain")
    private Set<String> allDomains = new HashSet<>();

    @Column(length = 100)
    private String location;

    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL)
    private List<Department> departments = new ArrayList<>();

    @OneToMany(mappedBy = "school", cascade = CascadeType.ALL)
    private List<User> users = new ArrayList<>();

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public String getSchoolName() {
        return schoolName;
    }

    public void setSchoolName(String schoolName) {
        this.schoolName = schoolName;
    }

    public String getPrimaryDomain() {
        return primaryDomain;
    }

    public void setPrimaryDomain(String primaryDomain) {
        this.primaryDomain = primaryDomain;
    }

    public Set<String> getAllDomains() {
        return allDomains;
    }

    public void setAllDomains(Set<String> allDomains) {
        this.allDomains = allDomains;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List<Department> getDepartments() {
        return departments;
    }

    public void setDepartments(List<Department> departments) {
        this.departments = departments;
    }

    public List<User> getUsers() {
        return users;
    }

    public void setUsers(List<User> users) {
        this.users = users;
    }
}