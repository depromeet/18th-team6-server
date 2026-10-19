package depromeet.hotsix.obrit.category.repository

import depromeet.hotsix.obrit.category.entity.Category
import org.springframework.jdbc.core.JdbcTemplate

object CategoryIconFixture {

    fun ensureDefaultIcon(jdbcTemplate: JdbcTemplate) {
        jdbcTemplate.update(
            "INSERT INTO icons (id, name, icon_key, url, created_at, updated_at) " +
                "SELECT ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP " +
                "WHERE NOT EXISTS (SELECT 1 FROM icons WHERE id = ?)",
            Category.USER_DEFAULT_ICON_ID,
            "기본",
            "icons/default.png",
            "/icons/default.png",
            Category.USER_DEFAULT_ICON_ID,
        )
    }
}
