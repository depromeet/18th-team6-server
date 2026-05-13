package depromeet.hotsix.obrit.category.controller

import depromeet.hotsix.obrit.category.controller.docs.CategoryControllerApi
import depromeet.hotsix.obrit.category.dto.request.CreateCategoryRequest
import depromeet.hotsix.obrit.category.dto.response.CategoryResponse
import depromeet.hotsix.obrit.category.service.CategoryService
import depromeet.hotsix.obrit.global.dto.ApiResponse
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
class CategoryController(private val categoryService: CategoryService) : CategoryControllerApi {

    @GetMapping
    override fun listCategories(@RequestHeader("X-User-Id") userId: Long): ApiResponse<List<CategoryResponse>> {
        val result = categoryService.listAllAccessibleCategories(userId)
        return ApiResponse.ok(result)
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    override fun createCategory(
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: CreateCategoryRequest,
    ): ApiResponse<CategoryResponse> = ApiResponse.ok(categoryService.createCategory(userId, request))

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    override fun deleteCategory(@RequestHeader("X-User-Id") userId: Long, @PathVariable categoryId: Long) {
        categoryService.deleteCategory(userId, categoryId)
    }
}
