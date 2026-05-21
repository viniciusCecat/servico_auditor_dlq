package com.viniciuscecatto.servicoauditordlq.infrastructure.adapters.out.persistence.entity;

import com.viniciuscecatto.servicoauditordlq.domain.model.SeveridadeErro;
import com.viniciuscecatto.servicoauditordlq.domain.model.StatusAnalise;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "dlq_audit_records")
public class RegistroAuditoriaEntity {

    @Id
    @Column(name = "error_id", nullable = false)
    private UUID errorId;

    @Column(name = "queue_name", nullable = false, length = 120)
    private String queueName;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;

    @Column(name = "audit_timestamp", nullable = false)
    private Instant timestamp;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StatusAnalise status;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private SeveridadeErro severity;

    @Column(name = "failure_reason", nullable = false, length = 1000)
    private String failureReason;

    public UUID getErrorId() {
        return errorId;
    }

    public void setErrorId(UUID errorId) {
        this.errorId = errorId;
    }

    public String getQueueName() {
        return queueName;
    }

    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public StatusAnalise getStatus() {
        return status;
    }

    public void setStatus(StatusAnalise status) {
        this.status = status;
    }

    public SeveridadeErro getSeverity() {
        return severity;
    }

    public void setSeverity(SeveridadeErro severity) {
        this.severity = severity;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }
}
