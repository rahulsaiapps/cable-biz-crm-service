package com.cablepulse.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_transactions")
public class DailyTransaction {

    @Id
    @Column(name = "transaction_id")
    private String transactionId;

    @Column(name = "amount_collected", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountCollected;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_of_payment", nullable = false)
    private PaymentMode modeOfPayment;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "field_agent_id", nullable = false)
    private Employee fieldAgent;

    public DailyTransaction() {}

    public DailyTransaction(String transactionId, BigDecimal amountCollected, PaymentMode modeOfPayment, LocalDateTime recordedAt, Customer customer, Employee fieldAgent) {
        this.transactionId = transactionId;
        this.amountCollected = amountCollected;
        this.modeOfPayment = modeOfPayment;
        this.recordedAt = recordedAt;
        this.customer = customer;
        this.fieldAgent = fieldAgent;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public BigDecimal getAmountCollected() {
        return amountCollected;
    }

    public void setAmountCollected(BigDecimal amountCollected) {
        this.amountCollected = amountCollected;
    }

    public PaymentMode getModeOfPayment() {
        return modeOfPayment;
    }

    public void setModeOfPayment(PaymentMode modeOfPayment) {
        this.modeOfPayment = modeOfPayment;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Employee getFieldAgent() {
        return fieldAgent;
    }

    public void setFieldAgent(Employee fieldAgent) {
        this.fieldAgent = fieldAgent;
    }
}
