package com.cablepulse.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "isp_vendor_settlements")
public class IspSettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String connectionTypeName;

    @Column(nullable = false)
    private Double amountPaid;

    @Column(nullable = false)
    private String paymentStatus;

    @Column(nullable = true)
    private String settlementNotes;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    public IspSettlement() {
    }

    public IspSettlement(Long id, String connectionTypeName, Double amountPaid, String paymentStatus, String settlementNotes, LocalDateTime transactionDate) {
        this.id = id;
        this.connectionTypeName = connectionTypeName;
        this.amountPaid = amountPaid;
        this.paymentStatus = paymentStatus;
        this.settlementNotes = settlementNotes;
        this.transactionDate = transactionDate;
    }

    public IspSettlement(String connectionTypeName, Double amountPaid, String paymentStatus, String settlementNotes) {
        this.connectionTypeName = connectionTypeName;
        this.amountPaid = amountPaid;
        this.paymentStatus = paymentStatus;
        this.settlementNotes = settlementNotes;
        this.transactionDate = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getConnectionTypeName() {
        return connectionTypeName;
    }

    public void setConnectionTypeName(String connectionTypeName) {
        this.connectionTypeName = connectionTypeName;
    }

    public Double getAmountPaid() {
        return amountPaid;
    }

    public void setAmountPaid(Double amountPaid) {
        this.amountPaid = amountPaid;
    }

    public String getPaymentStatus() {
        return paymentStatus;
    }

    public void setPaymentStatus(String paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public String getSettlementNotes() {
        return settlementNotes;
    }

    public void setSettlementNotes(String settlementNotes) {
        this.settlementNotes = settlementNotes;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }
}
