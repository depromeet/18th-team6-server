package depromeet.hotsix.obrit.item.repository

import depromeet.hotsix.obrit.item.entity.ItemReplacementHistory
import org.springframework.data.jpa.repository.JpaRepository

interface ItemReplacementHistoryRepository : JpaRepository<ItemReplacementHistory, Long>
