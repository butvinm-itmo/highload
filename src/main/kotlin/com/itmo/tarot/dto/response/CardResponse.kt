package com.itmo.tarot.dto.response

import com.itmo.tarot.entity.ArcanaType

data class CardResponse(
    val id: Int,
    val name: String,
    val arcanaType: ArcanaType,
    val position: Int,
    val isReversed: Boolean
)