package com.github.butvinmitmo.userservice.infrastructure.persistence

import com.github.butvinmitmo.userservice.application.interfaces.repository.UserRepository
import com.github.butvinmitmo.userservice.domain.model.Role
import com.github.butvinmitmo.userservice.domain.model.User
import com.github.butvinmitmo.userservice.infrastructure.persistence.mapper.RoleEntityMapper
import com.github.butvinmitmo.userservice.infrastructure.persistence.mapper.UserEntityMapper
import com.github.butvinmitmo.userservice.infrastructure.persistence.repository.SpringDataRoleRepository
import com.github.butvinmitmo.userservice.infrastructure.persistence.repository.SpringDataUserRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.UUID

@Repository
class R2dbcUserRepository(
    private val springDataUserRepository: SpringDataUserRepository,
    private val springDataRoleRepository: SpringDataRoleRepository,
    private val userEntityMapper: UserEntityMapper,
    private val roleEntityMapper: RoleEntityMapper,
) : UserRepository {
    override fun findById(id: UUID): Mono<User> =
        springDataUserRepository
            .findById(id)
            .flatMap { userEntity ->
                springDataRoleRepository
                    .findById(userEntity.roleId)
                    .map { roleEntity ->
                        userEntityMapper.toDomain(userEntity, roleEntityMapper.toDomain(roleEntity))
                    }
            }

    override fun findByUsername(username: String): Mono<User> =
        springDataUserRepository
            .findByUsername(username)
            .flatMap { userEntity ->
                springDataRoleRepository
                    .findById(userEntity.roleId)
                    .map { roleEntity ->
                        userEntityMapper.toDomain(userEntity, roleEntityMapper.toDomain(roleEntity))
                    }
            }

    override fun findAllPaginated(
        offset: Long,
        limit: Int,
    ): Flux<User> =
        getRoleMap()
            .flatMapMany { roleMap ->
                springDataUserRepository
                    .findAllPaginated(offset, limit)
                    .map { userEntity ->
                        val role = roleMap[userEntity.roleId]!!
                        userEntityMapper.toDomain(userEntity, role)
                    }
            }

    override fun count(): Mono<Long> = springDataUserRepository.count()

    override fun save(user: User): Mono<User> {
        val entity = userEntityMapper.toEntity(user)
        return springDataUserRepository
            .save(entity)
            .flatMap { savedEntity ->
                // Re-fetch the entity to get database-generated fields (created_at)
                springDataUserRepository
                    .findById(savedEntity.id!!)
                    .flatMap { fetchedEntity ->
                        springDataRoleRepository
                            .findById(fetchedEntity.roleId)
                            .map { roleEntity ->
                                userEntityMapper.toDomain(fetchedEntity, roleEntityMapper.toDomain(roleEntity))
                            }
                    }
            }
    }

    override fun existsById(id: UUID): Mono<Boolean> = springDataUserRepository.existsById(id)

    override fun deleteById(id: UUID): Mono<Void> = springDataUserRepository.deleteById(id)

    private fun getRoleMap(): Mono<Map<UUID, Role>> =
        springDataRoleRepository
            .findAll()
            .map { roleEntityMapper.toDomain(it) }
            .collectList()
            .map { roles -> roles.associateBy { it.id } }
}
