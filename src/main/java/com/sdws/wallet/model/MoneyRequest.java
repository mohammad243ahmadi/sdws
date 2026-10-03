package com.sdws.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "money_requests")
@Getter @Setter @NoArgsConstructor
public class MoneyRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private AppUser requester;
    @ManyToOne(optional = false)
    private AppUser target;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Enums.RequestStatus status = Enums.RequestStatus.PENDING;
    private LocalDateTime createdAt = LocalDateTime.now();
}
