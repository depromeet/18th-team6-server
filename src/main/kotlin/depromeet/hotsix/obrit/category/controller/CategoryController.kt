package depromeet.hotsix.obrit.category.controller

import depromeet.hotsix.obrit.category.dto.CategoryResponse
import depromeet.hotsix.obrit.category.dto.CreateCategoryRequest
import depromeet.hotsix.obrit.category.service.CategoryService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "Categories", description = "Category APIs")
@RestController
@RequestMapping("/categories")
class CategoryController(private val categoryService: CategoryService) {

    @Operation(
        summary = "List categories",
        description = "Lists preset categories and the user's custom categories.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Categories returned.",
            ),
        ],
    )
    @GetMapping
    fun listCategories(
        @Parameter(description = "Development user id.", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
    ): List<CategoryResponse> = categoryService.listCategories(userId)

    @Operation(
        summary = "Create category",
        description = "Creates a custom category for the user.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "Category created.",
            ),
            ApiResponse(
                responseCode = "400",
                description = "Invalid category request.",
            ),
        ],
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun createCategory(
        @Parameter(description = "Development user id.", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
        @Valid @RequestBody request: CreateCategoryRequest,
    ): CategoryResponse = categoryService.createCategory(userId, request)

    @Operation(
        summary = "Delete category",
        description = "Deletes a custom category and its items. Preset categories cannot be deleted.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "Category deleted."),
            ApiResponse(
                responseCode = "400",
                description = "Preset category deletion was rejected.",
            ),
            ApiResponse(
                responseCode = "404",
                description = "Category not found.",
            ),
        ],
    )
    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteCategory(
        @Parameter(description = "Development user id.", required = true, example = "1")
        @RequestHeader("X-User-Id") userId: Long,
        @PathVariable categoryId: Long,
    ) {
        categoryService.deleteCategory(userId, categoryId)
    }
}
