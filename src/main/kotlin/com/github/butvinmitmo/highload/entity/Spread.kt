package com.github.butvinmitmo.highload.entity

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
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
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_type_id", nullable = false)
    val layoutType: LayoutType,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    val author: User,
) {
    @Id
    @Generated
    @Column(columnDefinition = "uuid", insertable = false, nullable = false)
    lateinit var id: UUID

    @OneToMany(mappedBy = "spread", fetch = FetchType.LAZY)
    @JsonManagedReference
    val spreadCards: MutableList<SpreadCard> = mutableListOf()

    @OneToMany(mappedBy = "spread", fetch = FetchType.LAZY)
    @JsonManagedReference
    val interpretations: MutableList<Interpretation> = mutableListOf()

    @Generated
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    lateinit var createdAt: Instant
}
