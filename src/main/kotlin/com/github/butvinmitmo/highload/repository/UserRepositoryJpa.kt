package com.github.butvinmitmo.highload.repository

import com.github.butvinmitmo.highload.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface UserRepositoryJpa : JpaRepository<User, UUID>
