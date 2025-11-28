package com.github.butvinmitmo.divinationservice.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
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
class Interpretation(
    @Column(nullable = false, columnDefinition = "text")
    var text: String,
    @Column(name = "author_id", nullable = false)
    val authorId: UUID,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "spread_id", nullable = false)
    val spread: Spread,
) {
    @Id
    @Generated
    @Column(columnDefinition = "uuid", insertable = false, nullable = false)
    lateinit var id: UUID

    @Generated
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    lateinit var createdAt: Instant
}
