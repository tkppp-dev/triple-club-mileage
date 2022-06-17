package com.tkppp.tripleclubmileage.mileage.domain

import com.tkppp.tripleclubmileage.mileage.util.LogStatus
import com.tkppp.tripleclubmileage.mileage.util.ReviewAction
import org.hibernate.annotations.GenericGenerator
import java.time.LocalDateTime
import java.util.*
import javax.persistence.*

@Entity
@Table(
    indexes = [Index(name = "ml_i_userid", columnList = "userId"),
        Index(name = "ml_i_placeid", columnList = "placeId"),
        Index(name = "m1_i_reviewid", columnList = "reviewId")
    ]
)
class MileageLog(
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    val id: UUID? = null,

    @Column(nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(columnDefinition = "TINYINT default 0", nullable = false)
    val contentPoint: Int = 0,

    @Column(columnDefinition = "TINYINT default 0", nullable = false)
    val imagePoint: Int = 0,

    @Column(columnDefinition = "TINYINT default 0", nullable = false)
    val bonusPoint: Int = 0,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val action: ReviewAction,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    val status: LogStatus,

    @Column(columnDefinition = "TINYINT default 0", nullable = false)
    val variation: Int,

    @Column(columnDefinition = "BINARY(16)", nullable = false)
    val userId: UUID,

    @Column(columnDefinition = "BINARY(16)", nullable = false)
    val placeId: UUID,

    @Column(columnDefinition = "BINARY(16)", nullable = false)
    val reviewId: UUID
) {

    fun getTotalPoint(): Int {
        return contentPoint + imagePoint + bonusPoint
    }

}