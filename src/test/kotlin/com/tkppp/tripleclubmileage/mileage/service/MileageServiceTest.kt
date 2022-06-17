package com.tkppp.tripleclubmileage.mileage.service

import com.tkppp.tripleclubmileage.error.CustomException
import com.tkppp.tripleclubmileage.error.ErrorCode
import com.tkppp.tripleclubmileage.mileage.domain.*
import com.tkppp.tripleclubmileage.mileage.dto.AddDeleteCountingDto
import com.tkppp.tripleclubmileage.mileage.dto.MileageSaveRequestDto
import com.tkppp.tripleclubmileage.mileage.util.LogStatus
import com.tkppp.tripleclubmileage.mileage.util.ReviewAction
import io.mockk.*
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class MileageServiceTest {

    private val mileageRepository = mockk<MileageRepository>()
    private val mileageLogRepository = mockk<MileageLogRepository>()
    private val mileageService = MileageService(mileageRepository, mileageLogRepository)
    private val spy = spyk(mileageService)

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
                    placeId = UUID.randomUUID(),
                    reviewId = UUID.randomUUID()
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
            val addCnt = 0L
            val deleteCnt = 0L
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
        @DisplayName("placeId 가 일치하는 row 중 action 컬럼의 ADD, DELETE 개수가 다르면 false 를 반환한다")
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
        @DisplayName("내용 > 0, 이미지 개수 = 0 인 경우 Pair(1,0)을 반환한다")
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

            // when
            val result = mileageService.getPoints(dto)

            // then
            assertThat(result).isEqualTo(Pair(1, 0))
        }

        @Test
        @DisplayName("내용 = 0, 이미지 개수 > 0 인 경우 Pair(0,1)을 반환한다")
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

            // when
            val result = mileageService.getPoints(dto)

            // then
            assertThat(result).isEqualTo(Pair(0, 1))
        }
    }

    @Nested
    @DisplayName("getTotalPoint() 테스트")
    inner class GetTotalPointTest {

        @Test
        @DisplayName("넘겨받은 매개변수들의 총합을 반환한다")
        fun getTotalPoint_shouldReturnElementSum() {
            // given
            val p1 = 1
            val p2 = 2
            val p3 = 3

            // when
            val result = mileageService.getTotalPoint(p1, p2, p3)

            // then
            assertThat(result).isEqualTo(p1 + p2 + p3)
        }
    }

    @Nested
    @DisplayName("getMileageSavingDataWhenActionAdd() 테스트")
    inner class GetMileageSavingDataWhenActionAddTest {
        // global given
        // point 총합이 1인 dto
        private val dto = MileageSaveRequestDto(
            type = "REVIEW",
            action = ReviewAction.ADD,
            reviewId = UUID.randomUUID(),
            content = "Not Empty",
            attachedPhotoIds = listOf(),
            userId = UUID.randomUUID(),
            placeId = UUID.randomUUID()
        )

        // global stub
        init {
            // 항상 보너스 포인트를 받는 못하게 설정
            every { mileageLogRepository.findGroupByAction(any()) } returns listOf(
                AddDeleteCountingDto(action = ReviewAction.ADD, cnt = 2),
                AddDeleteCountingDto(action = ReviewAction.DELETE, cnt = 0)
            )
        }

        @Test
        @DisplayName("마일리지 테이블에 존재하지 않는 userId를 받는 경우 id가 null 인 마일리지 엔티티를 반환한다")
        fun getMileageSavingDataWhenActionAdd_shouldReturnNullIdMileageEntity() {
            // stub
            every { mileageRepository.findByUserId(any()) } returns null

            // when
            val (mileageEntity, logEntity, variation) = mileageService.getMileageSavingDataWhenActionAdd(dto)

            // then
            assertThat(mileageEntity.id).isNull()
            assertThat(mileageEntity.userId).isEqualTo(dto.userId)
            assertThat(mileageEntity.point).isEqualTo(0)

            assertThat(logEntity.action).isEqualTo(ReviewAction.ADD)
            assertThat(logEntity.status).isEqualTo(LogStatus.INCREASE)
            assertThat(logEntity.contentPoint).isEqualTo(1)
            assertThat(logEntity.imagePoint).isEqualTo(0)
            assertThat(logEntity.bonusPoint).isEqualTo(0)
            assertThat(logEntity.variation).isEqualTo(variation)
            assertThat(logEntity.userId).isEqualTo(dto.userId)
            assertThat(logEntity.placeId).isEqualTo(dto.placeId)

            assertThat(variation).isEqualTo(1)
        }

        @Test
        @DisplayName("마일리지 테이블에 존재하는 userId를 받는 경우 id가 null 이 아닌 마일리지 엔티티를 반환한다")
        fun getMileageSavingDataWhenActionAdd_shouldReturnNotNullIdMileageEntity() {
            // given
            val point = 10
            // stub
            every { mileageRepository.findByUserId(any()) } returns Mileage(id = UUID.randomUUID(), userId = dto.userId, point = point)

            // when
            val (mileageEntity, logEntity, variation) = mileageService.getMileageSavingDataWhenActionAdd(dto)

            // then
            assertThat(mileageEntity.id).isNotNull
            assertThat(mileageEntity.userId).isEqualTo(dto.userId)
            assertThat(mileageEntity.point).isEqualTo(point)

            assertThat(logEntity.action).isEqualTo(ReviewAction.ADD)
            assertThat(logEntity.status).isEqualTo(LogStatus.INCREASE)
            assertThat(logEntity.contentPoint).isEqualTo(1)
            assertThat(logEntity.imagePoint).isEqualTo(0)
            assertThat(logEntity.bonusPoint).isEqualTo(0)
            assertThat(logEntity.variation).isEqualTo(variation)
            assertThat(logEntity.userId).isEqualTo(dto.userId)
            assertThat(logEntity.placeId).isEqualTo(dto.placeId)

            assertThat(variation).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("getMileageSavingDataWhenActionMod() 테스트")
    inner class GetMileageSavingDataWhenActionModTest {
        // global given
        // point 총합이 0인 dto
        private val dto = MileageSaveRequestDto(
            type = "REVIEW",
            action = ReviewAction.MOD,
            reviewId = UUID.randomUUID(),
            content = "",
            attachedPhotoIds = listOf(),
            userId = UUID.randomUUID(),
            placeId = UUID.randomUUID(),
        )
        // point 총합이 2인 로그
        private val recentLog = MileageLog(
            action = dto.action,
            status = LogStatus.DECREASE,
            contentPoint = 1,
            imagePoint = 0,
            bonusPoint = 1,
            variation = 2,
            userId = dto.userId,
            placeId = dto.placeId,
            reviewId = dto.reviewId
        )

        @Test
        @DisplayName("마일리지 테이블에 존재하지 않는 userId를 받는 경우 CustomException(MILEAGE_DATA_NOT_FOUND)을 던진다")
        fun getMileageSavingDataWhenActionMod_shouldThrowCustomExceptionWhenMileageEntityIsNull() {
            // stub
            every { mileageRepository.findByUserId(any()) } returns null
            // when
            val ex = assertThrows<CustomException>{
                mileageService.getMileageSavingDataWhenActionMod(dto)
            }

            // then
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MILEAGE_DATA_NOT_FOUND)
        }

        @Test
        @DisplayName("마일리지 로그 테이블에 존재하지 않는 reviewId를 받는 경우 CustomException(MILEAGE_LOG_NOT_FOUND)을 던진다")
        fun getMileageSavingDataWhenActionMod_shouldThrowCustomExceptionWhenMileageLogEntityIsNull() {
            // stub
            every { mileageRepository.findByUserId(any()) } returns Mileage(id = UUID.randomUUID(), userId = dto.userId)
            every { mileageLogRepository.findRecentLog(any()) } returns null

            // when
            val ex = assertThrows<CustomException>{
                mileageService.getMileageSavingDataWhenActionMod(dto)
            }

            // then
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MILEAGE_LOG_NOT_FOUND)
        }

        @Test
        @DisplayName("마일리지 포인트 총합이 이전보다 작은 DTO 를 받을 경우 status 가 DECREASE 인 MileageLog 엔티티를 반환한다")
        fun getMileageSavingDataWhenActionMod_shouldReturnDecreaseStatusMileageLogEntity() {
            // given
            val point = 10

            // stub
            every { mileageRepository.findByUserId(any()) } returns Mileage(id = UUID.randomUUID(), userId = dto.userId, point = point)
            every { mileageLogRepository.findRecentLog(any()) } returns recentLog

            // when
            val (mileageEntity, logEntity, variation) = mileageService.getMileageSavingDataWhenActionMod(dto)

            // then
            assertThat(mileageEntity.id).isNotNull
            assertThat(mileageEntity.userId).isEqualTo(dto.userId)
            assertThat(mileageEntity.point).isEqualTo(point)

            assertThat(logEntity.action).isEqualTo(ReviewAction.MOD)
            assertThat(logEntity.status).isEqualTo(LogStatus.DECREASE)
            assertThat(logEntity.contentPoint).isEqualTo(0)
            assertThat(logEntity.imagePoint).isEqualTo(0)
            assertThat(logEntity.bonusPoint).isEqualTo(1)
            assertThat(logEntity.variation).isEqualTo(variation)
            assertThat(logEntity.userId).isEqualTo(dto.userId)
            assertThat(logEntity.placeId).isEqualTo(dto.placeId)

            assertThat(variation).isEqualTo(-1)
        }

        @Test
        @DisplayName("마일리지 포인트 총합이 이전과 같은 DTO 를 받을 경우 status 가 SAME 인 MileageLog 엔티티를 반환한다")
        fun getMileageSavingDataWhenActionMod_shouldReturnSameStatusMileageLogEntity() {
            // given
            val point = 10
            val sdto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.MOD,
                reviewId = UUID.randomUUID(),
                content = "Not Empty",
                attachedPhotoIds = listOf(),
                userId = UUID.randomUUID(),
                placeId = UUID.randomUUID()
            )
            // stub
            every { mileageRepository.findByUserId(any()) } returns Mileage(id = UUID.randomUUID(), userId = sdto.userId, point = point)
            every { mileageLogRepository.findRecentLog(any()) } returns recentLog

            // when
            val (mileageEntity, logEntity, variation) = mileageService.getMileageSavingDataWhenActionMod(sdto)

            // then
            assertThat(mileageEntity.id).isNotNull
            assertThat(mileageEntity.userId).isEqualTo(sdto.userId)
            assertThat(mileageEntity.point).isEqualTo(point)

            assertThat(logEntity.action).isEqualTo(ReviewAction.MOD)
            assertThat(logEntity.status).isEqualTo(LogStatus.SAME)
            assertThat(logEntity.contentPoint).isEqualTo(1)
            assertThat(logEntity.imagePoint).isEqualTo(0)
            assertThat(logEntity.bonusPoint).isEqualTo(1)
            assertThat(logEntity.variation).isEqualTo(variation)
            assertThat(logEntity.userId).isEqualTo(sdto.userId)
            assertThat(logEntity.placeId).isEqualTo(sdto.placeId)

            assertThat(variation).isEqualTo(0)
        }

        @Test
        @DisplayName("마일리지 포인트 총합이 이전보다 높은 DTO 를 받을 경우 status 가 INCREASE 인 MileageLog 엔티티를 반환한다")
        fun getMileageSavingDataWhenActionMod_shouldReturnIncreaseStatusMileageLogEntity() {
            // given
            val point = 10
            val idto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.MOD,
                reviewId = UUID.randomUUID(),
                content = "Not Empty",
                attachedPhotoIds = listOf(UUID.randomUUID()),
                userId = UUID.randomUUID(),
                placeId = UUID.randomUUID()
            )
            // stub
            every { mileageRepository.findByUserId(any()) } returns Mileage(id = UUID.randomUUID(), userId = idto.userId, point = point)
            every { mileageLogRepository.findRecentLog(any()) } returns recentLog

            // when
            val (mileageEntity, logEntity, variation) = mileageService.getMileageSavingDataWhenActionMod(idto)

            // then
            assertThat(mileageEntity.id).isNotNull
            assertThat(mileageEntity.userId).isEqualTo(idto.userId)
            assertThat(mileageEntity.point).isEqualTo(point)

            assertThat(logEntity.action).isEqualTo(ReviewAction.MOD)
            assertThat(logEntity.status).isEqualTo(LogStatus.INCREASE)
            assertThat(logEntity.contentPoint).isEqualTo(1)
            assertThat(logEntity.imagePoint).isEqualTo(1)
            assertThat(logEntity.bonusPoint).isEqualTo(1)
            assertThat(logEntity.variation).isEqualTo(variation)
            assertThat(logEntity.userId).isEqualTo(idto.userId)
            assertThat(logEntity.placeId).isEqualTo(idto.placeId)

            assertThat(variation).isEqualTo(1)
        }
    }

    @Nested
    @DisplayName("getMileageSavingDataWhenActionDelete() 테스트")
    inner class GetMileageSavingDataWhenActionDeleteTest {
        // global given
        private val dto = MileageSaveRequestDto(
            type = "REVIEW",
            action = ReviewAction.DELETE,
            reviewId = UUID.randomUUID(),
            content = "",
            attachedPhotoIds = listOf(),
            userId = UUID.randomUUID(),
            placeId = UUID.randomUUID()
        )

        @Test
        @DisplayName("마일리지 테이블에 존재하지 않는 userId를 받는 경우 CustomException(MILEAGE_DATA_NOT_FOUND)을 던진다")
        fun getMileageSavingDataWhenActionDelete_shouldThrowCustomExceptionWhenMileageEntityIsNull() {
            // stub
            every { mileageRepository.findByUserId(any()) } returns null
            // when
            val ex = assertThrows<CustomException>{
                mileageService.getMileageSavingDataWhenActionDelete(dto)
            }

            // then
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MILEAGE_DATA_NOT_FOUND)
        }

        @Test
        @DisplayName("마일리지 로그 테이블에 존재하지 않는 reviewId를 받는 경우 CustomException(MILEAGE_LOG_NOT_FOUND)을 던진다")
        fun getMileageSavingDataWhenActionDelete_shouldThrowCustomExceptionWhenMileageLogEntityIsNull() {
            // stub
            every { mileageRepository.findByUserId(any()) } returns Mileage(id = UUID.randomUUID(), userId = dto.userId)
            every { mileageLogRepository.findRecentLog(any()) } returns null

            // when
            val ex = assertThrows<CustomException>{
                mileageService.getMileageSavingDataWhenActionDelete(dto)
            }

            // then
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MILEAGE_LOG_NOT_FOUND)
        }

        @Test
        @DisplayName("정상 수행 테스트")
        fun getMileageSavingDataWhenActionDelete_shouldSuccess() {
            // given
            val recentLog = MileageLog(
                action = dto.action,
                status = LogStatus.DECREASE,
                contentPoint = 1,
                imagePoint = 0,
                bonusPoint = 1,
                variation = 2,
                userId = dto.userId,
                placeId = dto.placeId,
                reviewId = dto.reviewId
            )
            // stub
            every { mileageRepository.findByUserId(any()) } returns Mileage(id = UUID.randomUUID(), userId = dto.userId, point = 10)
            every { mileageLogRepository.findRecentLog(any()) } returns recentLog

            // when
            val (mileageEntity, logEntity, variation) = mileageService.getMileageSavingDataWhenActionDelete(dto)

            // then
            assertThat(mileageEntity.id).isNotNull
            assertThat(mileageEntity.userId).isEqualTo(dto.userId)
            assertThat(mileageEntity.point).isEqualTo(10)

            assertThat(logEntity.action).isEqualTo(ReviewAction.DELETE)
            assertThat(logEntity.status).isEqualTo(LogStatus.DECREASE)
            assertThat(logEntity.contentPoint).isEqualTo(0)
            assertThat(logEntity.imagePoint).isEqualTo(0)
            assertThat(logEntity.bonusPoint).isEqualTo(0)
            assertThat(logEntity.variation).isEqualTo(-2)
            assertThat(logEntity.userId).isEqualTo(dto.userId)
            assertThat(logEntity.placeId).isEqualTo(dto.placeId)

            assertThat(variation).isEqualTo(-2)
        }
    }

    @Nested
    @DisplayName("saveMileagePoint() 테스트")
    inner class SaveMileagePointTest {

        @Test
        @DisplayName("action.ADD - 정상 수행 테스트")
        fun saveMileagePoint_shouldSuccessWhenActionAdd(){
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
            val newLog = MileageLog(
                action = dto.action,
                status = LogStatus.INCREASE,
                contentPoint = 1,
                imagePoint = 0,
                bonusPoint = 1,
                variation = 2,
                userId = dto.userId,
                placeId = dto.placeId,
                reviewId = dto.reviewId
            )
            val mileage =  Mileage(id = UUID.randomUUID(), userId = dto.userId, point = 10)
            // stub
            every { mileageRepository.findByUserId(any()) } returns mileage
            every { mileageLogRepository.findGroupByAction(any()) } returns listOf(
                AddDeleteCountingDto(action = ReviewAction.ADD, cnt = 2),
                AddDeleteCountingDto(action = ReviewAction.DELETE, cnt = 2)
            )
            every { mileageLogRepository.save(any()) } returns newLog
            every {
                mileage.point += 2
                mileageRepository.save(any())
            } returns mileage

            // when
            spy.saveMileagePoint(dto)

            // then
            verify(exactly = 1) {
                spy.getMileageSavingDataWhenActionAdd(dto)
                mileageRepository.save(any())
                mileageLogRepository.save(any())
            }
        }

        @Test
        @DisplayName("action.MOD - 정상 수행 테스트")
        fun saveMileagePoint_shouldSuccessWhenActionMod(){
            // given
            // 총합이 0인 로그
            val dto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.MOD,
                reviewId = UUID.randomUUID(),
                content = "",
                attachedPhotoIds = listOf(),
                userId = UUID.randomUUID(),
                placeId = UUID.randomUUID()
            )
            // point 총합이 2인 로그
            val recentLog = MileageLog(
                action = dto.action,
                status = LogStatus.INCREASE,
                contentPoint = 1,
                imagePoint = 0,
                bonusPoint = 1,
                variation = 2,
                userId = dto.userId,
                placeId = dto.placeId,
                reviewId = dto.reviewId
            )
            val newLog = MileageLog(
                action = dto.action,
                status = LogStatus.DECREASE,
                contentPoint = 0,
                imagePoint = 0,
                bonusPoint = 1,
                variation = -1,
                userId = dto.userId,
                placeId = dto.placeId,
                reviewId = dto.reviewId
            )
            val mileage =  Mileage(id = UUID.randomUUID(), userId = dto.userId, point = 10)

            // stub
            every { mileageRepository.findByUserId(any()) } returns mileage
            every { mileageLogRepository.findRecentLog(any()) } returns recentLog
            every { mileageLogRepository.save(any()) } returns newLog
            every {
                mileage.point -= 1
                mileageRepository.save(any())
            } returns mileage

            // when
            spy.saveMileagePoint(dto)

            // then
            verify(exactly = 1) {
                spy.getMileageSavingDataWhenActionMod(dto)
                mileageRepository.save(any())
                mileageLogRepository.save(any())
            }
        }

        @Test
        @DisplayName("action.DELETE - 정상 수행 테스트")
        fun saveMileagePoint_shouldSuccessWhenActionDelete(){
            // given
            // 총합이 0인 로그
            val dto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.DELETE,
                reviewId = UUID.randomUUID(),
                content = "",
                attachedPhotoIds = listOf(),
                userId = UUID.randomUUID(),
                placeId = UUID.randomUUID()
            )
            // point 총합이 2인 로그
            val recentLog = MileageLog(
                action = dto.action,
                status = LogStatus.INCREASE,
                contentPoint = 1,
                imagePoint = 0,
                bonusPoint = 1,
                variation = 2,
                userId = dto.userId,
                placeId = dto.placeId,
                reviewId = dto.reviewId
            )
            val newLog = MileageLog(
                action = dto.action,
                status = LogStatus.DECREASE,
                contentPoint = 0,
                imagePoint = 0,
                bonusPoint = 0,
                variation = -2,
                userId = dto.userId,
                placeId = dto.placeId,
                reviewId = dto.reviewId
            )
            val mileage =  Mileage(id = UUID.randomUUID(), userId = dto.userId, point = 10)

            // stub
            every { mileageRepository.findByUserId(any()) } returns mileage
            every { mileageLogRepository.findRecentLog(any()) } returns recentLog
            every { mileageLogRepository.save(any()) } returns newLog
            every {
                mileage.point -= 2
                mileageRepository.save(any())
            } returns mileage

            // when
            spy.saveMileagePoint(dto)

            // then
            verify(exactly = 1) {
                spy.getMileageSavingDataWhenActionDelete(dto)
                mileageRepository.save(any())
                mileageLogRepository.save(any())
            }
        }

    }

}