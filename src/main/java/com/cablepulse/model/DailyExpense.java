package com.cablepulse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_expenses")
public class DailyExpense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double amount;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExpenseCategory expenseCategory;

    @Column(nullable = false)
    private LocalDateTime loggedAt;

    @Column(name = "logged_by_employee_id")
    private String loggedByEmployeeId;

    public DailyExpense() {
    }

    public DailyExpense(Long id, Double amount, String description, ExpenseCategory expenseCategory, LocalDateTime loggedAt, String loggedByEmployeeId) {
        this.id = id;
        this.amount = amount;
        this.description = description;
        this.expenseCategory = expenseCategory;
        this.loggedAt = loggedAt;
        this.loggedByEmployeeId = loggedByEmployeeId;
    }

    public DailyExpense(Double amount, String description, String expenseCategory, String loggedByEmployeeId) {
        this.amount = amount;
        this.description = description;
        this.expenseCategory = expenseCategory != null ? ExpenseCategory.valueOf(expenseCategory) : null;
        this.loggedByEmployeeId = loggedByEmployeeId;
        this.loggedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ExpenseCategory getExpenseCategory() {
        return expenseCategory;
    }

    public void setExpenseCategory(ExpenseCategory expenseCategory) {
        this.expenseCategory = expenseCategory;
    }

    public LocalDateTime getLoggedAt() {
        return loggedAt;
    }

    public void setLoggedAt(LocalDateTime loggedAt) {
        this.loggedAt = loggedAt;
    }

    public String getLoggedByEmployeeId() {
        return loggedByEmployeeId;
    }

    public void setLoggedByEmployeeId(String loggedByEmployeeId) {
        this.loggedByEmployeeId = loggedByEmployeeId;
    }
}
