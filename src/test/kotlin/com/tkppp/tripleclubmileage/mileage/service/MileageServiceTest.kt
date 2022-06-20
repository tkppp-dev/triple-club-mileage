package com.tkppp.tripleclubmileage.mileage.service

import com.tkppp.tripleclubmileage.error.CustomException
import com.tkppp.tripleclubmileage.error.ErrorCode
import com.tkppp.tripleclubmileage.mileage.domain.*
import com.tkppp.tripleclubmileage.mileage.dto.AddDeleteCountingDto
import com.tkppp.tripleclubmileage.mileage.dto.MileageSaveRequestDto
import com.tkppp.tripleclubmileage.mileage.util.LogStatus
import com.tkppp.tripleclubmileage.mileage.util.ReviewAction
import io.mockk.*
import io.mockk.MockKAnnotations.init
import io.mockk.junit5.MockKExtension
import org.assertj.core.api.Assertions.`as`
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.*

import org.junit.jupiter.api.extension.ExtendWith
import java.util.*

@ExtendWith(MockKExtension::class)
class MileageServiceTest {

    private val mileageRepository = mockk<MileageRepository>()
    private val mileageLogRepository = mockk<MileageLogRepository>()
    private val mileageService = MileageService(mileageRepository, mileageLogRepository)
    private val spyService = spyk(mileageService)

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
        fun isFirstReview_shouldReturnPair10() {
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
        fun isFirstReview_shouldReturnPair01() {
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
        @Test
        @DisplayName("내용 > 0, 이미지 개수 > 0 인 경우 Pair(1,1)을 반환한다")
        fun isFirstReview_shouldReturnPair11() {
            // given
            val dto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.ADD,
                reviewId = UUID.randomUUID(),
                content = "Not Empty",
                attachedPhotoIds = listOf(UUID.randomUUID()),
                userId = UUID.randomUUID(),
                placeId = UUID.randomUUID()
            )

            // when
            val result = mileageService.getPoints(dto)

            // then
            assertThat(result).isEqualTo(Pair(1, 1))
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
            val p2 = 10
            val p3 = 100

            // when
            val result = mileageService.getTotalPoint(p1, p2, p3)

            // then
            assertThat(result).isEqualTo(p1 + p2 + p3)
        }
    }

    @Nested
    @DisplayName("getMileageEntity() 테스트")
    inner class GetMileageEntityTest {

        // global given
        private val userId = UUID.randomUUID()

        @Test
        @DisplayName("action = ADD 일 경우 존재하지 않는 userId 를 받을 경우 id가 null 인 엔티티를 반환한다")
        fun getMileageEntity_shouldReturnNullIdEntity() {
            // stub
            every { mileageRepository.findByUserId(userId) } returns null

            // when
            val result = mileageService.getMileageEntity(ReviewAction.ADD, userId)

            // then
            assertThat(result.id).isNull()
            assertThat(result.userId).isEqualTo(userId)
        }

        @Test
        @DisplayName("action = ADD 일 때 존재하는 userId 를 받을 경우 id가 null 이 아닌엔티티를 반환한다")
        fun getMileageEntity_shouldReturnNotNullIdEntity() {
            // given
            val point = 10
            // stub
            every { mileageRepository.findByUserId(userId) } returns Mileage(id = UUID.randomUUID(), userId, point = point)

            // when
            val result = mileageService.getMileageEntity(ReviewAction.ADD, userId)

            // then
            assertThat(result.id).isNotNull
            assertThat(result.userId).isEqualTo(userId)
            assertThat(result.point).isEqualTo(point)
        }

        @Test
        @DisplayName("action != ADD 일 때 존재하지 않는 userId 를 받을 경우 CustomException(MILEAGE_DATA_NOT_FOUND)를 던진다")
        fun getMileageEntity_shouldThrowCustomException(){
            // stub
            every { mileageRepository.findByUserId(userId) } returns null

            // when
            val ex1 = assertThrows<CustomException> { mileageService.getMileageEntity(ReviewAction.MOD, userId) }
            val ex2 = assertThrows<CustomException> { mileageService.getMileageEntity(ReviewAction.DELETE, userId) }

            // then
            assertThat(ex1.errorCode).isEqualTo(ErrorCode.MILEAGE_DATA_NOT_FOUND)
            assertThat(ex2.errorCode).isEqualTo(ErrorCode.MILEAGE_DATA_NOT_FOUND)
        }

        @Test
        @DisplayName("action != ADD 일 때 존재하는 userId 를 받을 경우 id가 null 이 아닌엔티티를 반환한다")
        fun getMileageEntity_shouldReturnNotNullIdEntityWhenNotAdd() {
            // given
            val point = 10
            // stub
            every { mileageRepository.findByUserId(userId) } returns Mileage(id = UUID.randomUUID(), userId, point = point)

            // when
            val resultMod = mileageService.getMileageEntity(ReviewAction.MOD, userId)
            val resultDel = mileageService.getMileageEntity(ReviewAction.DELETE, userId)

            // then
            assertThat(resultMod.id).isNotNull
            assertThat(resultMod.userId).isEqualTo(userId)
            assertThat(resultMod.point).isEqualTo(point)

            assertThat(resultDel.id).isNotNull
            assertThat(resultDel.userId).isEqualTo(userId)
            assertThat(resultDel.point).isEqualTo(point)
        }
    }

    @Nested
    @DisplayName("getRecentLog() 테스트")
    inner class GetRecentLogTest {

        @Test
        @DisplayName("존재하지 않는 reviewId를 받는 경우 CustomException(MILEAGE_LOG_NOT_FOUND)를 던진다")
        fun getRecentLog_shouldThrowCustomException() {
            // stub
            every { mileageLogRepository.findRecentLog(any()) } returns null

            // when
            val ex = assertThrows<CustomException> { mileageService.getRecentLog(UUID.randomUUID()) }

            // then
            assertThat(ex.errorCode).isEqualTo(ErrorCode.MILEAGE_LOG_NOT_FOUND)
        }

        @Test
        @DisplayName("존재하 reviewId를 받는 경우 가장 최신 로그를 반환한다")
        fun getRecentLog_shouldSuccess() {
            // given
            val reviewId = UUID.randomUUID()
            val log = MileageLog(
                id = UUID.randomUUID(),
                action = ReviewAction.ADD,
                status = LogStatus.INCREASE,
                variation = 0,
                userId = UUID.randomUUID(),
                placeId = UUID.randomUUID(),
                reviewId = reviewId
            )

            // stub
            every { mileageLogRepository.findRecentLog(reviewId) } returns log

            // when
            val result = mileageService.getRecentLog(reviewId)

            // then
            assertThat(result.id).isEqualTo(log.id)
            assertThat(result.action).isEqualTo(log.action)
            assertThat(result.status).isEqualTo(log.status)
            assertThat(result.createdAt).isEqualTo(log.createdAt)
            assertThat(result.contentPoint).isEqualTo(0)
            assertThat(result.imagePoint).isEqualTo(0)
            assertThat(result.bonusPoint).isEqualTo(0)
            assertThat(result.variation).isEqualTo(log.variation)
            assertThat(result.userId).isEqualTo(log.userId)
            assertThat(result.placeId).isEqualTo(log.placeId)
            assertThat(result.reviewId).isEqualTo(reviewId)
        }
    }

    @Nested
    @DisplayName("getMileageLogEntityWhenActionAdd() 테스트")
    inner class GetMileageLogEntityWhenActionAddTest {
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

        @BeforeEach
        fun setup(){
            init(spyService)
        }

        @Test
        @DisplayName("최초로 작성하지 않는 리뷰인 경우 보너스 포인트를 지급하지 말아야한다")
        fun getMileageLogEntityWhenActionAdd_shouldReturnBp0Entity() {
            // stub
            every { spyService.isFirstReview(any()) } returns false

            // when
            val logEntity = spyService.getMileageLogEntityWhenActionAdd(dto)

            // then
            assertThat(logEntity.action).isEqualTo(ReviewAction.ADD)
            assertThat(logEntity.status).isEqualTo(LogStatus.INCREASE)
            assertThat(logEntity.contentPoint).isEqualTo(1)
            assertThat(logEntity.imagePoint).isEqualTo(0)
            assertThat(logEntity.bonusPoint).isEqualTo(0)
            assertThat(logEntity.variation).isEqualTo(1)
            assertThat(logEntity.userId).isEqualTo(dto.userId)
            assertThat(logEntity.placeId).isEqualTo(dto.placeId)
        }
        @Test
        @DisplayName("최초로 작성한 리뷰인 경우 보너스 포인트를 지급해야한다")
        fun getMileageLogEntityWhenActionAdd_shouldReturnBp1Entity() {
            // stub
            every { spyService.isFirstReview(any()) } returns true

            // when
            val logEntity = spyService.getMileageLogEntityWhenActionAdd(dto)

            // then
            assertThat(logEntity.action).isEqualTo(ReviewAction.ADD)
            assertThat(logEntity.status).isEqualTo(LogStatus.INCREASE)
            assertThat(logEntity.contentPoint).isEqualTo(1)
            assertThat(logEntity.imagePoint).isEqualTo(0)
            assertThat(logEntity.bonusPoint).isEqualTo(1)
            assertThat(logEntity.variation).isEqualTo(2)
            assertThat(logEntity.userId).isEqualTo(dto.userId)
            assertThat(logEntity.placeId).isEqualTo(dto.placeId)
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

        @BeforeEach
        fun setup(){
            init(spyService)
        }

        @Test
        @DisplayName("마일리지 포인트 총합이 이전보다 작은 DTO 를 받을 경우 status 가 DECREASE 인 MileageLog 엔티티를 반환한다")
        fun getMileageLogEntityWhenActionMod_shouldReturnDecreaseStatusMileageLogEntity() {
            // stub
            every { spyService.getRecentLog(any()) } returns recentLog

            // when
            val logEntity = spyService.getMileageLogEntityWhenActionMod(dto)

            // then
            assertThat(logEntity.action).isEqualTo(ReviewAction.MOD)
            assertThat(logEntity.status).isEqualTo(LogStatus.DECREASE)
            assertThat(logEntity.contentPoint).isEqualTo(0)
            assertThat(logEntity.imagePoint).isEqualTo(0)
            assertThat(logEntity.bonusPoint).isEqualTo(1)
            assertThat(logEntity.variation).isEqualTo(-1)
            assertThat(logEntity.userId).isEqualTo(dto.userId)
            assertThat(logEntity.placeId).isEqualTo(dto.placeId)
        }

        @Test
        @DisplayName("마일리지 포인트 총합이 이전과 같은 DTO 를 받을 경우 status 가 SAME 인 MileageLog 엔티티를 반환한다")
        fun getMileageLogEntityWhenActionMod_shouldReturnSameStatusMileageLogEntity() {
            // given
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
            every { spyService.getRecentLog(any()) } returns recentLog

            // when
            val logEntity = spyService.getMileageLogEntityWhenActionMod(sdto)

            // then
            assertThat(logEntity.action).isEqualTo(ReviewAction.MOD)
            assertThat(logEntity.status).isEqualTo(LogStatus.SAME)
            assertThat(logEntity.contentPoint).isEqualTo(1)
            assertThat(logEntity.imagePoint).isEqualTo(0)
            assertThat(logEntity.bonusPoint).isEqualTo(1)
            assertThat(logEntity.variation).isEqualTo(0)
            assertThat(logEntity.userId).isEqualTo(sdto.userId)
            assertThat(logEntity.placeId).isEqualTo(sdto.placeId)
        }

        @Test
        @DisplayName("마일리지 포인트 총합이 이전보다 높은 DTO 를 받을 경우 status 가 INCREASE 인 MileageLog 엔티티를 반환한다")
        fun getMileageLogEntityWhenActionMod_shouldReturnIncreaseStatusMileageLogEntity() {
            // given
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
            every { spyService.getRecentLog(any()) } returns recentLog

            // when
            val logEntity = spyService.getMileageLogEntityWhenActionMod(idto)

            // then
            assertThat(logEntity.action).isEqualTo(ReviewAction.MOD)
            assertThat(logEntity.status).isEqualTo(LogStatus.INCREASE)
            assertThat(logEntity.contentPoint).isEqualTo(1)
            assertThat(logEntity.imagePoint).isEqualTo(1)
            assertThat(logEntity.bonusPoint).isEqualTo(1)
            assertThat(logEntity.variation).isEqualTo(1)
            assertThat(logEntity.userId).isEqualTo(idto.userId)
            assertThat(logEntity.placeId).isEqualTo(idto.placeId)
        }
    }

    @Nested
    @DisplayName("getMileageLogEntityWhenActionDelete() 테스트")
    inner class GetMileageLogEntityWhenActionDeleteTest {
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

        @BeforeEach
        fun setup(){
            init(spyService)
        }

        @Test
        @DisplayName("정상 수행 테스트")
        fun getMileageLogEntityWhenActionDelete_shouldSuccess() {
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
            every { spyService.getRecentLog(any()) } returns recentLog

            // when
            val logEntity = spyService.getMileageLogEntityWhenActionDelete(dto)

            // then
            assertThat(logEntity.action).isEqualTo(ReviewAction.DELETE)
            assertThat(logEntity.status).isEqualTo(LogStatus.DECREASE)
            assertThat(logEntity.contentPoint).isEqualTo(0)
            assertThat(logEntity.imagePoint).isEqualTo(0)
            assertThat(logEntity.bonusPoint).isEqualTo(0)
            assertThat(logEntity.variation).isEqualTo(-2)
            assertThat(logEntity.userId).isEqualTo(dto.userId)
            assertThat(logEntity.placeId).isEqualTo(dto.placeId)
        }
    }

    @Nested
    @DisplayName("saveMileagePoint() 테스트")
    inner class SaveMileagePointTest {

        private val initialPoint = 10
        private val userId = UUID.randomUUID()
        private lateinit var mileage: Mileage
        private lateinit var slot: CapturingSlot<Mileage>

        @BeforeEach
        fun setup(){
            init(spyService)
            mileage = Mileage(id = UUID.randomUUID(), userId = userId, point = initialPoint)
            slot = slot()
        }

        @Test
        @DisplayName("action.ADD - 정상 수행 테스트")
        fun saveMileagePoint_shouldSuccessWhenActionAdd() {
            // given
            val dto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.ADD,
                reviewId = UUID.randomUUID(),
                content = "Not empty",
                attachedPhotoIds = listOf(),
                userId = userId,
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

            // stub
            every { spyService.getMileageEntity(any(), any()) } returns mileage
            every { spyService.getMileageLogEntityWhenActionAdd(any()) } returns newLog
            every { mileageLogRepository.save(any()) } returns newLog
            every { mileageRepository.save(capture(slot)) } returns mileage

            // when
            spyService.saveMileagePoint(dto)

            // then
            verify(exactly = 1) {
                spyService.getMileageEntity(dto.action, dto.userId)
                spyService.getMileageLogEntityWhenActionAdd(dto)
                mileageRepository.save(any())
                mileageLogRepository.save(any())
            }
            assertThat(slot.captured.point).isEqualTo(initialPoint + newLog.variation)
        }

        @Test
        @DisplayName("action.MOD - 정상 수행 테스트")
        fun saveMileagePoint_shouldSuccessWhenActionMod() {
            // given
            // 총합이 0인 로그
            val dto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.MOD,
                reviewId = UUID.randomUUID(),
                content = "",
                attachedPhotoIds = listOf(),
                userId = userId,
                placeId = UUID.randomUUID()
            )
            val newLog = MileageLog(
                action = dto.action,
                status = LogStatus.DECREASE,
                contentPoint = 0,
                imagePoint = 0,
                bonusPoint = 1,
                variation = -1,
                userId = userId,
                placeId = dto.placeId,
                reviewId = dto.reviewId
            )

            // stub
            every { spyService.getMileageEntity(any(), any()) } returns mileage
            every { spyService.getMileageLogEntityWhenActionMod(any()) } returns newLog
            every { mileageLogRepository.save(any()) } returns newLog
            every { mileageRepository.save(capture(slot)) } returns mileage

            // when
            spyService.saveMileagePoint(dto)

            // then
            verify(exactly = 1) {
                spyService.getMileageEntity(dto.action, dto.userId)
                spyService.getMileageLogEntityWhenActionMod(dto)
                mileageRepository.save(any())
                mileageLogRepository.save(any())
            }
            assertThat(slot.captured.point).isEqualTo(initialPoint + newLog.variation)
        }

        @Test
        @DisplayName("action.DELETE - 정상 수행 테스트")
        fun saveMileagePoint_shouldSuccessWhenActionDelete() {
            // given
            // 총합이 0인 로그
            val dto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.DELETE,
                reviewId = UUID.randomUUID(),
                content = "",
                attachedPhotoIds = listOf(),
                userId = userId,
                placeId = UUID.randomUUID()
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
            val mileage = Mileage(id = UUID.randomUUID(), userId = dto.userId, point = 10)

            // stub
            every { spyService.getMileageEntity(any(), any()) } returns mileage
            every { spyService.getMileageLogEntityWhenActionDelete(any()) } returns newLog
            every { mileageLogRepository.save(any()) } returns newLog
            every { mileageRepository.save(capture(slot)) } returns mileage

            // when
            spyService.saveMileagePoint(dto)

            // then
            verify(exactly = 1) {
                spyService.getMileageEntity(dto.action, dto.userId)
                spyService.getMileageLogEntityWhenActionDelete(dto)
                mileageRepository.save(any())
                mileageLogRepository.save(any())
            }
            assertThat(slot.captured.point).isEqualTo(initialPoint + newLog.variation)
        }

    }

}