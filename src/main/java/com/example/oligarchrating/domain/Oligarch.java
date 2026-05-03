package com.example.oligarchrating.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "oligarchs")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
@EqualsAndHashCode(of = "id")
public class Oligarch {

    @Id
    @Column(nullable = false, updatable = false)
    private Long id;

    @Version
    private Long version;

    @Setter
    @Column(nullable = false, length = 100)
    private String firstName;

    @Setter
    @Column(nullable = false, length = 100)
    private String lastName;

    @Setter
    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal assetsValue;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Setter
    @Column(nullable = false)
    private Instant updatedAt;
}
