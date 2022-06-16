package com.tkppp.tripleclubmileage.domain

import com.tkppp.tripleclubmileage.mileage.domain.Mileage
import com.tkppp.tripleclubmileage.mileage.domain.MileageRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ActiveProfiles
import java.util.*

@DataJpaTest
@ActiveProfiles("dev")
class MileageRepositoryTest(
    @Autowired private val mileageRepository: MileageRepository
) {

    @Test
    @DisplayName("주어진 userId 를 통해 일치하는 마일리지 엔티티를 반환해야한다")
    fun findByUserIdTest() {
        // given
        val userId = UUID.randomUUID()
        val mileages = (0..3).map {
            Mileage(
                userId = if(it == 0) userId else UUID.randomUUID()
            )
        }
        mileageRepository.saveAll(mileages)

        // when
        val result = mileageRepository.findByUserId(userId)

        // then
        assertThat(result?.userId).isEqualTo(userId)
    }

}