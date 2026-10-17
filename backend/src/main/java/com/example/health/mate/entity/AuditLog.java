package com.example.health.mate.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Data
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    // Durable attribution survives removal of the live user relationship.
    @Column(updatable = false)
    private Long actorUserId;

    @PrePersist
    private void snapshotActor() {
        if (actorUserId == null && user != null) actorUserId = user.getId();
    }

    @Column(nullable = false, length = 500)
    private String action;

    @Column(length = 2000)
    private String details;

    @CreationTimestamp
    private LocalDateTime timestamp;
}
