package com.sdws.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Getter @Setter @NoArgsConstructor
public class WalletTransaction {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private AppUser user;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Enums.TxType type;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Enums.TxStatus status = Enums.TxStatus.SUCCESS;
    private String description;
    private LocalDateTime createdAt = LocalDateTime.now();
}
