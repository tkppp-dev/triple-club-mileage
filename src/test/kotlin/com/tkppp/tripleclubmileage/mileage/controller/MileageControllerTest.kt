package com.tkppp.tripleclubmileage.mileage.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.ninjasquad.springmockk.MockkBean
import com.tkppp.tripleclubmileage.mileage.dto.MessageResponseDto
import com.tkppp.tripleclubmileage.mileage.dto.MileageLogResponseDto
import com.tkppp.tripleclubmileage.mileage.dto.MileageSaveRequestDto
import com.tkppp.tripleclubmileage.mileage.service.MileageService
import com.tkppp.tripleclubmileage.mileage.util.LogStatus
import com.tkppp.tripleclubmileage.mileage.util.ReviewAction
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.util.*

@WebMvcTest
internal class MileageControllerTest(
    @Autowired private val mvc: MockMvc,
) {

    @MockkBean
    private lateinit var mileageService: MileageService
    private val mapper = ObjectMapper().registerKotlinModule().registerModule(JavaTimeModule())

    @Nested
    @DisplayName("POST /events 테스트")
    inner class GetMileageSaveEventsTest{
        private val uri = "/events"

        @Test
        @DisplayName("requestDto.action.ADD 일 때 ADD를 포함한 메세지를 몸체에 담아 응답해야한다")
        fun getMileageSaveEvents_WithActionADD() {
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
            val requestBody = mapper.writeValueAsString(dto)
            val message = "MILEAGE_${dto.action.fullname}_SUCCESS"
            // stub
            every { mileageService.saveMileagePoint(any()) } returns Unit

            // when
            mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.message").value(message))
        }

        @Test
        @DisplayName("requestDto.action.MOD 일 때 MOD를 포함한 메세지를 몸체에 담아 응답해야한다")
        fun getMileageSaveEvents_WithActionMOD() {
            // given
            val dto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.DELETE,
                reviewId = UUID.randomUUID(),
                content = "Not empty",
                attachedPhotoIds = listOf(),
                userId = UUID.randomUUID(),
                placeId = UUID.randomUUID()
            )
            val requestBody = mapper.writeValueAsString(dto)
            val message = "MILEAGE_${dto.action.fullname}_SUCCESS"
            // stub
            every { mileageService.saveMileagePoint(any()) } returns Unit

            // when
            mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.message").value(message))
        }

        @Test
        @DisplayName("requestDto.action.DELETE 일 때 DELETE 포함한 메세지를 몸체에 담아 응답해야한다")
        fun getMileageSaveEvents_WithActionDELETE() {
            // given
            val dto = MileageSaveRequestDto(
                type = "REVIEW",
                action = ReviewAction.DELETE,
                reviewId = UUID.randomUUID(),
                content = "Not empty",
                attachedPhotoIds = listOf(),
                userId = UUID.randomUUID(),
                placeId = UUID.randomUUID()
            )
            val requestBody = mapper.writeValueAsString(dto)
            val message = "MILEAGE_${dto.action.fullname}_SUCCESS"
            // stub
            every { mileageService.saveMileagePoint(any()) } returns Unit

            // when
            mvc.perform(post(uri).contentType(MediaType.APPLICATION_JSON).content(requestBody))
                .andExpect(status().isOk)
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("\$.message").value(message))
        }
    }

    @Test
    @DisplayName("GET /api/mileage/{userId} 테스트")
    fun returnMileagePoint() {
        // given
        val userId = UUID.randomUUID()
        val uri = "/api/mileage/${userId}"
        val point = 10
        // stub
        every { mileageService.getMileagePoint(userId) } returns point

        // when
        mvc.perform(get(uri))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("\$.value").value(point))
    }

    @Test
    @DisplayName("GET /api/mileage/all/{userId} 테스트")
    fun returnMileageLog() {
        // given
        val userId = UUID.randomUUID()
        val uri = "/api/mileage/all/${userId}"
        val responseBody = listOf(
            MileageLogResponseDto(
                action = ReviewAction.ADD,
                status = LogStatus.INCREASE,
                variation = 3,
                createdAt = LocalDateTime.now()
            ),
            MileageLogResponseDto(
                action = ReviewAction.MOD,
                status = LogStatus.DECREASE,
                variation = -1,
                createdAt = LocalDateTime.now()
            ),
            MileageLogResponseDto(
                action = ReviewAction.DELETE,
                status = LogStatus.DECREASE,
                variation = -2,
                createdAt = LocalDateTime.now()
            )
        )
        // stub
        every { mileageService.getMileageLogList(userId) } returns responseBody

        // when
        val result = mvc.perform(get(uri))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn()

        // then
        val parsedDto = mapper.readValue<List<MileageLogResponseDto>>(result.response.contentAsString)
        for(i in responseBody.indices){
            assertThat(parsedDto[i].action).isEqualTo(responseBody[i].action)
            assertThat(parsedDto[i].status).isEqualTo(responseBody[i].status)
            assertThat(parsedDto[i].variation).isEqualTo(responseBody[i].variation)
            assertThat(parsedDto[i].createdAt).isEqualTo(responseBody[i].createdAt)
        }

    }
}