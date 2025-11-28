package com.github.butvinmitmo.divinationservice.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.Generated
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "spread")
class Spread(
    @Column(columnDefinition = "text")
    val question: String?,
    @Column(name = "layout_type_id", nullable = false)
    val layoutTypeId: UUID,
    @Column(name = "author_id", nullable = false)
    val authorId: UUID,
) {
    @Id
    @Generated
    @Column(columnDefinition = "uuid", insertable = false, nullable = false)
    lateinit var id: UUID

    @OneToMany(mappedBy = "spread", fetch = FetchType.LAZY)
    val spreadCards: MutableList<SpreadCard> = mutableListOf()

    @OneToMany(mappedBy = "spread", fetch = FetchType.LAZY)
    val interpretations: MutableList<Interpretation> = mutableListOf()

    @Generated
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    lateinit var createdAt: Instant
}
