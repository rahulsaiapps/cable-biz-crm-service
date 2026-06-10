package com.cablepulse.model;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "employees")
public class Employee {

    @Id
    @Column(name = "employee_id")
    private String employeeId;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false)
    private EmployeeRole role;

    @Column(name = "email")
    private String email;

    @Column(name = "description")
    private String description;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "employee_assigned_villages", joinColumns = @JoinColumn(name = "employee_id"))
    @Column(name = "village_name")
    private List<String> assignedVillages = new ArrayList<>();

    public Employee() {}

    public Employee(String employeeId, String fullName, EmployeeRole role) {
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.role = role;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public EmployeeRole getRole() {
        return role;
    }

    public void setRole(EmployeeRole role) {
        this.role = role;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getAssignedVillages() {
        return assignedVillages;
    }

    public void setAssignedVillages(List<String> assignedVillages) {
        this.assignedVillages = assignedVillages != null ? new ArrayList<>(assignedVillages) : new ArrayList<>();
    }
}
