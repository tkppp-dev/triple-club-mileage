package com.tkppp.tripleclubmileage.mileage.domain

import org.hibernate.annotations.GenericGenerator
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.GeneratedValue
import javax.persistence.Id

@Entity
class Mileage(
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name="uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID? = null,

    @Column(columnDefinition = "BINARY(16)", nullable = false, unique = true)
    val userId: UUID,

    @Column(columnDefinition = "INT", nullable = false)
    var point: Int = 0
)