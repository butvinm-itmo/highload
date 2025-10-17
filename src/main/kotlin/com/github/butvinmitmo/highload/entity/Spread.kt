package com.github.butvinmitmo.highload.entity

import com.fasterxml.jackson.annotation.JsonManagedReference
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
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
data class Spread(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "uuid")
    val id: UUID? = null,
    @Column(columnDefinition = "text")
    val question: String? = null,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "layout_type_id", nullable = false)
    val layoutType: LayoutType,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    val author: User,
    @OneToMany(mappedBy = "spread", fetch = FetchType.LAZY)
    @JsonManagedReference
    val spreadCards: List<SpreadCard> = emptyList(),
    @OneToMany(mappedBy = "spread", fetch = FetchType.LAZY)
    @JsonManagedReference
    val interpretations: List<Interpretation> = emptyList(),
) {
    @Generated
    @Column(name = "created_at", nullable = false, updatable = false, insertable = false)
    lateinit var createdAt: Instant
}
