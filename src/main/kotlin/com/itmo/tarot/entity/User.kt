package com.itmo.tarot.entity

import jakarta.persistence.*

@Entity
@Table(name = "users")
data class User(
    @Id
    val id: Long,
    
    @OneToMany(mappedBy = "author", cascade = [CascadeType.ALL], orphanRemoval = true)
    val spreads: MutableList<Spread> = mutableListOf(),
    
    @OneToMany(mappedBy = "author", cascade = [CascadeType.ALL], orphanRemoval = true)
    val interpretations: MutableList<Interpretation> = mutableListOf()
)