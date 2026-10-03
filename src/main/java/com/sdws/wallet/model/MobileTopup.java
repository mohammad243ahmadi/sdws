package com.sdws.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "mobile_topups")
@Getter @Setter @NoArgsConstructor
public class MobileTopup {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private AppUser user;
    private String operator;
    private String phoneNumber;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING)
    private Enums.TxStatus status = Enums.TxStatus.SUCCESS;
    private LocalDateTime createdAt = LocalDateTime.now();
}
