package com.tkppp.tripleclubmileage.mileage.service

import com.tkppp.tripleclubmileage.error.CustomException
import com.tkppp.tripleclubmileage.error.ErrorCode
import com.tkppp.tripleclubmileage.mileage.domain.Mileage
import com.tkppp.tripleclubmileage.mileage.domain.MileageLog
import com.tkppp.tripleclubmileage.mileage.domain.MileageLogRepository
import com.tkppp.tripleclubmileage.mileage.domain.MileageRepository
import com.tkppp.tripleclubmileage.mileage.dto.AddDeleteCountingDto
import com.tkppp.tripleclubmileage.mileage.dto.MileageSaveRequestDto
import com.tkppp.tripleclubmileage.mileage.util.LogStatus
import com.tkppp.tripleclubmileage.mileage.util.ReviewAction
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
internal class MileageServiceTest {

    private val mileageRepository = mockk<MileageRepository>()
    private val mileageLogRepository = mockk<MileageLogRepository>()
    private val mileageService = MileageService(mileageRepository, mileageLogRepository)

    @Nested
    @DisplayName("getMileagePoint() 테스트")
    inner class GetMileagePointTest {

        // global given
        private val userId = UUID.randomUUID()
        private val point = (0..100).random()

        @Test
        @DisplayName("존재하는 userId 를 전달받으면 현재 마일리지 포인트를 반환한다")
        fun getMileagePoint_shouldReturnMileagePoint() {
            // stub
            every { mileageRepository.findByUserId(userId) } returns Mileage(
                id = UUID.randomUUID(),
                userId = userId,
                point = point
            )

            // when
            val result = mileageService.getMileagePoint(userId)

            // then
            assertThat(result).isEqualTo(point)
        }

        @Test
        @DisplayName("존재하지 않는 userId 를 전달받으면 CustomException 을 던져야한다")
        fun getMileagePoint_shouldThrowCustomException() {
            // stub
            every { mileageRepository.findByUserId(any()) } returns null

            // when
            val ex = assertThrows<CustomException> {
                mileageService.getMileagePoint(userId)
            }

            // then
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MILEAGE_DATA_NOT_FOUND)
        }
    }

    @Nested
    @DisplayName("getMileagePoint() 테스트")
    inner class GetMileageLogListTest {

        // global given
        private val userId = UUID.randomUUID()

        @Test
        @DisplayName("DB에 존재하는 userId 를 전달받으면 일치하는 로그의 응답 DTO 리스트를 반환한다")
        fun getMileageLogList_shouldReturnLogList() {
            // given
            val logs = listOf(
                MileageLog(
                    action = ReviewAction.ADD,
                    status = LogStatus.INCREASE,
                    variation = 3,
                    userId = userId,
                    placeId = UUID.randomUUID()
                )
            )
            // stub
            every { mileageLogRepository.findByUserId(userId) } returns logs

            // when
            val result = mileageService.getMileageLogList(userId)

            // then
            assertThat(result.size).isEqualTo(logs.size)
            assertThat(result.first().action).isEqualTo(logs.first().action)
            assertThat(result.first().status).isEqualTo(logs.first().status)
            assertThat(result.first().variation).isEqualTo(logs.first().variation)
            assertThat(result.first().createdAt).isEqualTo(logs.first().createdAt)

        }

        @Test
        @DisplayName("DB에 존재하지 않는 userId 를 전달받으면 빈 리스트를 반환한다")
        fun getMileageLogList_shouldReturnEmptyList() {
            // stub
            every { mileageLogRepository.findByUserId(userId) } returns listOf()

            // when
            val result = mileageService.getMileageLogList(userId)

            // then
            assertThat(result.size).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("isFirstReview() 테스트")
    inner class IsFirstReviewTest {

        // global given
        private val placeId = UUID.randomUUID()

        @Test
        @DisplayName("placeId 가 일치하는 row 중 action 컬럼의 ADD, DELETE 개수가 같으면 true 를 반환한다")
        fun isFirstReview_shouldReturnTrue() {
            // given
            val addCnt = 3L
            val deleteCnt = 3L
            // stub
            every { mileageLogRepository.findGroupByAction(placeId) } returns listOf(
                AddDeleteCountingDto(action = ReviewAction.ADD, cnt = addCnt),
                AddDeleteCountingDto(action = ReviewAction.DELETE, cnt = deleteCnt)
            )

            // when
            val result = mileageService.isFirstReview(placeId)

            // then
            assertThat(result).isEqualTo(true)
        }

        @Test
        @DisplayName("DB에 존재하지 않는 userId 를 전달받으면 빈 리스트를 반환한다")
        fun isFirstReview_shouldReturnFalse() {
            // given
            val addCnt = 3L
            val deleteCnt = 2L
            // stub
            every { mileageLogRepository.findGroupByAction(placeId) } returns listOf(
                AddDeleteCountingDto(action = ReviewAction.ADD, cnt = addCnt),
                AddDeleteCountingDto(action = ReviewAction.DELETE, cnt = deleteCnt)
            )

            // when
            val result = mileageService.isFirstReview(placeId)

            // then
            assertThat(result).isEqualTo(false)
        }
    }

    @Nested
    @DisplayName("getPoints() 테스트")
    inner class GetPointsTest {

        @Test
        @DisplayName("내용 > 0, 이미지 개수 = 0, 처음작성한 리뷰가 이닌 경우 Triple(1,0,0)을 반환한다")
        fun isFirstReview_shouldReturnTriple100() {
            // given
            val dto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.ADD,
                reviewId = UUID.randomUUID(),
                content = "Not empty",
                attachedPhotoIds = listOf(),
                userId = UUID.randomUUID(),
                placeId = UUID.randomUUID()
            )

            // stub
            every { mileageLogRepository.findGroupByAction(any()) } returns listOf(
                AddDeleteCountingDto(action = ReviewAction.ADD, cnt = 0),
                AddDeleteCountingDto(action = ReviewAction.DELETE, cnt = 1)
            )

            // when
            val result = mileageService.getPoints(dto)

            // then
            assertThat(result).isEqualTo(Triple(1, 0, 0))
        }

        @Test
        @DisplayName("내용 = 0, 이미지 개수 > 0, 처음작성한 리뷰가 이닌 경우 Triple(0,1,0)을 반환한다")
        fun isFirstReview_shouldReturnTriple010() {
            // given
            val dto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.ADD,
                reviewId = UUID.randomUUID(),
                content = "",
                attachedPhotoIds = listOf(UUID.randomUUID()),
                userId = UUID.randomUUID(),
                placeId = UUID.randomUUID()
            )

            // stub
            every { mileageLogRepository.findGroupByAction(any()) } returns listOf(
                AddDeleteCountingDto(action = ReviewAction.ADD, cnt = 0),
                AddDeleteCountingDto(action = ReviewAction.DELETE, cnt = 1)
            )

            // when
            val result = mileageService.getPoints(dto)

            // then
            assertThat(result).isEqualTo(Triple(0, 1, 0))
        }

        @Test
        @DisplayName("내용 = 0, 이미지 개수 = 0, 처음작성한 리뷰인 경우 Triple(0,0,1)을 반환한다")
        fun isFirstReview_shouldReturnTriple001() {
            // given
            val dto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.ADD,
                reviewId = UUID.randomUUID(),
                content = "",
                attachedPhotoIds = listOf(),
                userId = UUID.randomUUID(),
                placeId = UUID.randomUUID()
            )

            // stub
            every { mileageLogRepository.findGroupByAction(any()) } returns listOf(
                AddDeleteCountingDto(action = ReviewAction.ADD, cnt = 1),
                AddDeleteCountingDto(action = ReviewAction.DELETE, cnt = 1)
            )

            // when
            val result = mileageService.getPoints(dto)

            // then
            assertThat(result).isEqualTo(Triple(0, 0, 1))
        }
    }

    @Nested
    @DisplayName("getTotalPoint() 테스트")
    inner class GetTotalPointTest {

        @Test
        @DisplayName("넘겨받은 Triple 의 원소 총합을 반환한다")
        fun getTotalPoint_shouldReturnElementSum() {
            // given
            val p1 = Triple(1, 0, 0)
            val p2 = Triple(1, 1, 0)
            val p3 = Triple(1, 1, 1)

            // when
            val r1 = mileageService.getTotalPoint(p1)
            val r2 = mileageService.getTotalPoint(p2)
            val r3 = mileageService.getTotalPoint(p3)

            // then
            assertThat(r1).isEqualTo(p1.first + p1.second + p1.third)
            assertThat(r2).isEqualTo(p2.first + p2.second + p2.third)
            assertThat(r3).isEqualTo(p3.first + p3.second + p3.third)
        }
    }

    @Nested
    @DisplayName("saveMileagePoint() 테스트")
    inner class SaveMileagePointTest {

        @Test
        @DisplayName("action: ADD 일 때 ")
    }
}