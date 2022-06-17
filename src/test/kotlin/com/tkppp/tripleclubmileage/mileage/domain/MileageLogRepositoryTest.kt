package com.tkppp.tripleclubmileage.mileage.domain

import com.tkppp.tripleclubmileage.mileage.domain.MileageLog
import com.tkppp.tripleclubmileage.mileage.domain.MileageLogRepository
import com.tkppp.tripleclubmileage.mileage.domain.findRecentLog
import com.tkppp.tripleclubmileage.mileage.util.LogStatus
import com.tkppp.tripleclubmileage.mileage.util.ReviewAction
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import java.util.*

@DataJpaTest
@ActiveProfiles("dev")
class MileageLogRepositoryTest(
    @Autowired private val mileageLogRepository: MileageLogRepository
) {

    @BeforeEach
    fun tearDown() {
        mileageLogRepository.deleteAll()
    }

    @Test
    @DisplayName("주어진 userId를 통해 일치하는 마일리지 로그 엔티티 리스트를 반환해야한다")
    fun findByUserIdTest() {
        // given
        val userId = UUID.randomUUID()
        val times = 2
        val logs = (0..3).map {
            MileageLog(
                action = ReviewAction.ADD,
                status = LogStatus.INCREASE,
                variation = 0,
                userId = if (it < times) userId else UUID.randomUUID(),
                placeId = UUID.randomUUID()
            )
        }

        mileageLogRepository.saveAll(logs)

        // when
        // result1: 존재하는 userId, result2: 존재하지 않는 userId
        val result1 = mileageLogRepository.findByUserId(userId)
        val result2 = mileageLogRepository.findByUserId(UUID.randomUUID())

        // then
        assertThat(result2.size).isEqualTo(0)
        assertThat(result1.size).isEqualTo(times)
        assertThat(result1[0].userId).isEqualTo(logs[0].userId)
        assertThat(result1[0].placeId).isEqualTo(logs[0].placeId)

    }

    @Test
    @DisplayName("주어진 placeId와 userId가 모두 일치하는 row 중 가장 최신 row 를 반환해야한다")
    fun findRecentLogTest() {
        // given
        val userId = UUID.randomUUID()
        val placeId = UUID.randomUUID()
        val logs = (0..3).map {
            MileageLog(
                action = ReviewAction.ADD,
                status = LogStatus.INCREASE,
                variation = 0,
                userId = userId,
                placeId = placeId
            )
        }

        mileageLogRepository.saveAll(logs)

        // when
        val result = mileageLogRepository.findRecentLog(userId, placeId)!!

        // then
        assertThat(result.createdAt).isEqualTo(logs.last().createdAt)
    }

    @Test
    @DisplayName("특정 placeId 와 일치하는 group by 된 row 의 action(ADD, DELETE) 각각의 개수 정보를 반환해야한다")
    fun findGroupByActionTest() {
        // given
        val placeId1 = UUID.randomUUID()
        val placeId2 = UUID.randomUUID()
        val place1AddLogs = listOf(
            MileageLog(
                action = ReviewAction.ADD,
                status = LogStatus.INCREASE,
                variation = 0,
                userId = UUID.randomUUID(),
                placeId = placeId1
            ),
            MileageLog(
                action = ReviewAction.ADD,
                status = LogStatus.INCREASE,
                variation = 0,
                userId = UUID.randomUUID(),
                placeId = placeId1
            ),
        )
        val place1ModLogs = listOf(
            MileageLog(
                action = ReviewAction.MOD,
                status = LogStatus.INCREASE,
                variation = 0,
                userId = UUID.randomUUID(),
                placeId = placeId1
            )
        )
        val place1DeleteLogs = listOf(
            MileageLog(
                action = ReviewAction.DELETE,
                status = LogStatus.INCREASE,
                variation = 0,
                userId = UUID.randomUUID(),
                placeId = placeId1
            ),
            MileageLog(
                action = ReviewAction.DELETE,
                status = LogStatus.INCREASE,
                variation = 0,
                userId = UUID.randomUUID(),
                placeId = placeId1
            )
        )
        val place2AddLogs = listOf(
            MileageLog(
                action = ReviewAction.ADD,
                status = LogStatus.INCREASE,
                variation = 0,
                userId = UUID.randomUUID(),
                placeId = placeId2
            ),
        )
        val logs = listOf(
            *place1AddLogs.toTypedArray(),
            *place1ModLogs.toTypedArray(),
            *place1DeleteLogs.toTypedArray(),
            *place2AddLogs.toTypedArray()
        )

        mileageLogRepository.saveAll(logs)

        // when
        val result = mileageLogRepository.findGroupByAction(placeId1)
        // then
        assertThat(result[0].cnt.toInt()).isEqualTo(place1AddLogs.size)
        assertThat(result[1].cnt.toInt()).isEqualTo(place1DeleteLogs.size)
    }
}