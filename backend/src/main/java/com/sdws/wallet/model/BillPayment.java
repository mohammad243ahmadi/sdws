package com.sdws.wallet.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "bill_payments")
@Getter @Setter @NoArgsConstructor
public class BillPayment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    private AppUser user;
    private String category;
    private String billNumber;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    private LocalDateTime createdAt = LocalDateTime.now();
}
