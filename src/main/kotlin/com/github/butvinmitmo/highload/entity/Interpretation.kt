package com.github.butvinmitmo.highload.entity

import com.fasterxml.jackson.annotation.JsonBackReference
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.Generated
import java.time.Instant
import java.util.UUID

@Entity
@Table(
    name = "interpretation",
    uniqueConstraints = [UniqueConstraint(columnNames = ["author_id", "spread_id"])],
)
data class Interpretation(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    val id: UUID? = null,
    @Column(nullable = false, columnDefinition = "text")
    var text: String,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    val author: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spread_id", nullable = false)
    @JsonBackReference
    val spread: Spread,
) {
    @Generated
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    lateinit var createdAt: Instant
}
