package com.tkppp.tripleclubmileage.mileage.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.tkppp.tripleclubmileage.mileage.domain.MileageLogRepository
import com.tkppp.tripleclubmileage.mileage.domain.MileageRepository
import com.tkppp.tripleclubmileage.mileage.dto.MileageSaveRequestDto
import com.tkppp.tripleclubmileage.mileage.util.ReviewAction
import org.hamcrest.Matchers.hasSize
import org.junit.jupiter.api.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
@ActiveProfiles("dev")
class MileageControllerIntegrationTest(
    @Autowired private val mvc: MockMvc,
    @Autowired private val mileageLogRepository: MileageLogRepository
) {

    private val eventUri = "/events"
    private val mapper = ObjectMapper().registerKotlinModule()
    private val userIds = (0..2).map { UUID.randomUUID() }
    private val placeIds = (0..2).map { UUID.randomUUID() }
    private val reviewIds = (0..8).map { UUID.randomUUID() }

    /**
     * 테스트 시나리오
     * 1. 3명의 유저가 3개에 장소에 각각 하나씩 리뷰를 작성한다 -> reviewIds[userIdIndex * 3 + 0..2] 가 각 유저의 리뷰 ID 가 된다
     * 2. 모든 장소에서 처음 리뷰를 작성한 유저는 0번 유저이고 0번과 1번 유저는 리뷰에 리뷰내용과 사진을 포함하고 2번 유저는 리뷰만 포함한다.
     *  예상 결과
     *    0번: 9점
     *    1번: 6점
     *    2번: 3점
     * 3. 1번 장소에 대한 리뷰(idx = 1, 4, 7), 0,2번 유저는 리뷰를 내용과 사진은 포함하고 1번 유저는 내용만 포함하도록 리뷰를 수정한다.
     *  예상 결과
     *    0번: 9점
     *    1번: 5점
     *    2번: 4점
     * 4. 2번 장소에 대한 리뷰(idx = 2, 5, 8)를 모두 삭제하고 1번 유저가 2번장소에 대해 내용과 이미지를 포함한 리뷰를 새로 작성한다
     *  예상 결과
     *    0번: 6점
     *    1번: 6점
     *    2번: 3점
     */
    @Test
    @DisplayName("MileageController 통합테스트")
    fun mileageControllerIntegrationTest() {
        // given
        val addDtos = reviewIds.mapIndexed { idx, reviewId ->
            val userId = userIds[idx / 3]
            val placeId = placeIds[idx % 3]
            val content = "ADD"
            val attachedPhotoIds = if(idx / 3 < 2) listOf(UUID.randomUUID()) else listOf()
            MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.ADD,
                reviewId = reviewId,
                content = content,
                attachedPhotoIds = attachedPhotoIds,
                userId = userId,
                placeId = placeId
            )
        }

        val modDtos = (0..2).map { idx ->
            val (content, attachedPhotoIds) =
                if(idx == 0 || idx == 2) Pair("Modified", listOf(UUID.randomUUID()))
                else Pair("Modified", listOf())

            MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.MOD,
                reviewId = reviewIds[idx * 3 + 1],
                content = content,
                attachedPhotoIds = attachedPhotoIds,
                userId = userIds[idx],
                placeId = placeIds[1]
            )
        }

        val deleteDtos = (0..2).map { idx ->
            MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.DELETE,
                reviewId = reviewIds[idx * 3 + 2],
                content = "",
                attachedPhotoIds = listOf(),
                userId = userIds[idx],
                placeId = placeIds[2]
            )
        }

        val lastAddDto = MileageSaveRequestDto(
            type = "REVIEW",
            action = ReviewAction.ADD,
            reviewId = UUID.randomUUID(),
            content = "NEW ADD",
            attachedPhotoIds = listOf(UUID.randomUUID()),
            userId = userIds[1],
            placeId = placeIds[2]
        )

        // add test when
        addDtos.forEach {
            mvc.perform(post(eventUri).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(it)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.message").value("MILEAGE_ADD_SUCCESS"))
        }
        // add test then
        for (i in 0..2) {
            val expectedValue = 9 - i * 3
            mvc.perform(get("/api/mileage/${userIds[i]}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.value").value(expectedValue))
        }

        // mod test when
        modDtos.forEach {
            mvc.perform(post(eventUri).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(it)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.message").value("MILEAGE_MODIFIED_SUCCESS"))
        }
        // mod test then
        for (i in 0..2) {
            val expectedValue = if(i == 0) 9 else if(i == 1) 5 else 4
            mvc.perform(get("/api/mileage/${userIds[i]}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.value").value(expectedValue))
        }

        // delete test when
        deleteDtos.forEach {
            mvc.perform(post(eventUri).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(it)))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.message").value("MILEAGE_DELETE_SUCCESS"))
        }
        // last add
        mvc.perform(post(eventUri).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(lastAddDto)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("\$.message").value("MILEAGE_ADD_SUCCESS"))

        // delete test then
        for (i in 0..2) {
            val expectedValue = if(i == 0 || i == 1) 6 else 3
            mvc.perform(get("/api/mileage/${userIds[i]}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$.value").value(expectedValue))
        }

        // returnMileageLog() test
        for(id in userIds){
            val expectedValue = mileageLogRepository.findByUserId(id).size
            mvc.perform(get("/api/mileage/all/${id}"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("\$", hasSize<Int>(expectedValue)))
        }

    }


}