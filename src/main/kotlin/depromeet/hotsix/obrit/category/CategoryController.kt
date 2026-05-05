package depromeet.hotsix.obrit.category

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/categories")
class CategoryController(private val categoryService: CategoryService) {

    @GetMapping
    fun listCategories(@RequestHeader("X-User-Id") userId: Long): List<CategoryResponse> =
        categoryService.listCategories(userId)

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: CreateCategoryRequest,
    ): CategoryResponse = categoryService.createCategory(userId, request)

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCategory(@RequestHeader("X-User-Id") userId: Long, @PathVariable categoryId: Long) {
        categoryService.deleteCategory(userId, categoryId)
    }
}
