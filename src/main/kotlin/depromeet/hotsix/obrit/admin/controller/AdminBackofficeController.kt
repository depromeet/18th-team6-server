package depromeet.hotsix.obrit.admin.controller

import depromeet.hotsix.obrit.admin.dto.AdminCategoryForm
import depromeet.hotsix.obrit.admin.dto.AdminIconForm
import depromeet.hotsix.obrit.admin.dto.AdminItemForm
import depromeet.hotsix.obrit.admin.dto.AdminReplacementForm
import depromeet.hotsix.obrit.admin.dto.AdminUserForm
import depromeet.hotsix.obrit.admin.service.AdminBackofficeService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes

@Controller
@RequestMapping("/admin")
class AdminBackofficeController(private val adminBackofficeService: AdminBackofficeService) {

    @GetMapping
    fun index(): String = "redirect:/admin/users"

    @GetMapping("/users")
    fun users(@RequestParam(defaultValue = "false") includeDeleted: Boolean, model: Model): String {
        model.addAttribute("activeMenu", "users")
        model.addAttribute("includeDeleted", includeDeleted)
        model.addAttribute("users", adminBackofficeService.listUsers(includeDeleted))
        return "admin/users"
    }

    @GetMapping("/users/add")
    fun addUser(model: Model): String {
        model.addAttribute("activeMenu", "users")
        model.addAttribute("pageTitle", "Add user")
        model.addAttribute("formAction", "/admin/users")
        model.addAttribute("isChange", false)
        return "admin/user-form"
    }

    @GetMapping("/users/{userId}/change")
    fun changeUser(@PathVariable userId: Long, model: Model): String {
        model.addAttribute("activeMenu", "users")
        model.addAttribute("pageTitle", "Change user")
        model.addAttribute("formAction", "/admin/users/$userId/edit")
        model.addAttribute("isChange", true)
        model.addAttribute("user", adminBackofficeService.getUser(userId))
        return "admin/user-form"
    }

    @GetMapping("/icons")
    fun icons(@RequestParam(defaultValue = "false") includeDeleted: Boolean, model: Model): String {
        model.addAttribute("activeMenu", "icons")
        model.addAttribute("includeDeleted", includeDeleted)
        model.addAttribute("icons", adminBackofficeService.listIcons(includeDeleted))
        return "admin/icons"
    }

    @GetMapping("/icons/add")
    fun addIcon(model: Model): String {
        model.addAttribute("activeMenu", "icons")
        model.addAttribute("pageTitle", "Add icon")
        model.addAttribute("formAction", "/admin/icons")
        model.addAttribute("isChange", false)
        return "admin/icon-form"
    }

    @GetMapping("/icons/{iconId}/change")
    fun changeIcon(@PathVariable iconId: Long, model: Model): String {
        model.addAttribute("activeMenu", "icons")
        model.addAttribute("pageTitle", "Change icon")
        model.addAttribute("formAction", "/admin/icons/$iconId/edit")
        model.addAttribute("isChange", true)
        model.addAttribute("icon", adminBackofficeService.getIcon(iconId))
        return "admin/icon-form"
    }

    @PostMapping("/icons")
    fun createIcon(@ModelAttribute form: AdminIconForm, redirectAttributes: RedirectAttributes): String =
        runAdminAction(redirectAttributes, "/admin/icons") {
            adminBackofficeService.createIcon(form)
        }

    @PostMapping("/icons/{iconId}/edit")
    fun updateIcon(
        @PathVariable iconId: Long,
        @ModelAttribute form: AdminIconForm,
        redirectAttributes: RedirectAttributes,
    ): String = runAdminAction(redirectAttributes, "/admin/icons") {
        adminBackofficeService.updateIcon(iconId, form)
    }

    @PostMapping("/icons/{iconId}/delete")
    fun deleteIcon(@PathVariable iconId: Long, redirectAttributes: RedirectAttributes): String =
        runAdminAction(redirectAttributes, "/admin/icons") {
            adminBackofficeService.deleteIcon(iconId)
        }

    @PostMapping("/users")
    fun createUser(@ModelAttribute form: AdminUserForm, redirectAttributes: RedirectAttributes): String =
        runAdminAction(redirectAttributes, "/admin/users") {
            adminBackofficeService.createUser(form)
        }

    @PostMapping("/users/{userId}/edit")
    fun updateUser(
        @PathVariable userId: Long,
        @RequestParam name: String,
        redirectAttributes: RedirectAttributes,
    ): String = runAdminAction(redirectAttributes, "/admin/users") {
        adminBackofficeService.updateUserName(userId, name)
    }

    @PostMapping("/users/{userId}/delete")
    fun deleteUser(@PathVariable userId: Long, redirectAttributes: RedirectAttributes): String =
        runAdminAction(redirectAttributes, "/admin/users") {
            adminBackofficeService.deleteUser(userId)
        }

    @GetMapping("/categories")
    fun categories(@RequestParam(defaultValue = "false") includeDeleted: Boolean, model: Model): String {
        model.addAttribute("activeMenu", "categories")
        model.addAttribute("includeDeleted", includeDeleted)
        model.addAttribute("categories", adminBackofficeService.listCategories(includeDeleted))
        model.addAttribute("users", adminBackofficeService.listUsers(includeDeleted = false))
        return "admin/categories"
    }

    @GetMapping("/categories/add")
    fun addCategory(model: Model): String {
        model.addAttribute("activeMenu", "categories")
        model.addAttribute("pageTitle", "Add category")
        model.addAttribute("formAction", "/admin/categories")
        model.addAttribute("isChange", false)
        model.addAttribute("users", adminBackofficeService.listUsers(includeDeleted = false))
        model.addAttribute("icons", adminBackofficeService.listIconOptions())
        return "admin/category-form"
    }

    @GetMapping("/categories/{categoryId}/change")
    fun changeCategory(@PathVariable categoryId: Long, model: Model): String {
        model.addAttribute("activeMenu", "categories")
        model.addAttribute("pageTitle", "Change category")
        model.addAttribute("formAction", "/admin/categories/$categoryId/edit")
        model.addAttribute("isChange", true)
        model.addAttribute("category", adminBackofficeService.getCategory(categoryId))
        model.addAttribute("users", adminBackofficeService.listUsers(includeDeleted = false))
        model.addAttribute("icons", adminBackofficeService.listIconOptions())
        return "admin/category-form"
    }

    @PostMapping("/categories")
    fun createCategory(@ModelAttribute form: AdminCategoryForm, redirectAttributes: RedirectAttributes): String =
        runAdminAction(redirectAttributes, "/admin/categories") {
            adminBackofficeService.createCategory(form)
        }

    @PostMapping("/categories/{categoryId}/edit")
    fun updateCategory(
        @PathVariable categoryId: Long,
        @ModelAttribute form: AdminCategoryForm,
        redirectAttributes: RedirectAttributes,
    ): String = runAdminAction(redirectAttributes, "/admin/categories") {
        adminBackofficeService.updateCategory(categoryId, form)
    }

    @PostMapping("/categories/{categoryId}/delete")
    fun deleteCategory(@PathVariable categoryId: Long, redirectAttributes: RedirectAttributes): String =
        runAdminAction(redirectAttributes, "/admin/categories") {
            adminBackofficeService.deleteCategory(categoryId)
        }

    @GetMapping("/items")
    fun items(@RequestParam(defaultValue = "false") includeDeleted: Boolean, model: Model): String {
        model.addAttribute("activeMenu", "items")
        model.addAttribute("includeDeleted", includeDeleted)
        model.addAttribute("items", adminBackofficeService.listItems(includeDeleted))
        model.addAttribute("users", adminBackofficeService.listUsers(includeDeleted = false))
        model.addAttribute("categories", adminBackofficeService.listCategoryOptions())
        return "admin/items"
    }

    @GetMapping("/items/add")
    fun addItem(model: Model): String {
        model.addAttribute("activeMenu", "items")
        model.addAttribute("pageTitle", "Add item")
        model.addAttribute("formAction", "/admin/items")
        model.addAttribute("isChange", false)
        model.addAttribute("users", adminBackofficeService.listUsers(includeDeleted = false))
        model.addAttribute("categories", adminBackofficeService.listCategoryOptions())
        return "admin/item-form"
    }

    @GetMapping("/items/{itemId}/change")
    fun changeItem(@PathVariable itemId: Long, model: Model): String {
        model.addAttribute("activeMenu", "items")
        model.addAttribute("pageTitle", "Change item")
        model.addAttribute("formAction", "/admin/items/$itemId/edit")
        model.addAttribute("isChange", true)
        model.addAttribute("item", adminBackofficeService.getItem(itemId))
        model.addAttribute("users", adminBackofficeService.listUsers(includeDeleted = false))
        model.addAttribute("categories", adminBackofficeService.listCategoryOptions())
        return "admin/item-form"
    }

    @PostMapping("/items")
    fun createItem(@ModelAttribute form: AdminItemForm, redirectAttributes: RedirectAttributes): String =
        runAdminAction(redirectAttributes, "/admin/items") {
            adminBackofficeService.createItem(form)
        }

    @PostMapping("/items/{itemId}/edit")
    fun updateItem(
        @PathVariable itemId: Long,
        @ModelAttribute form: AdminItemForm,
        redirectAttributes: RedirectAttributes,
    ): String = runAdminAction(redirectAttributes, "/admin/items") {
        adminBackofficeService.updateItem(itemId, form)
    }

    @PostMapping("/items/{itemId}/delete")
    fun deleteItem(@PathVariable itemId: Long, redirectAttributes: RedirectAttributes): String =
        runAdminAction(redirectAttributes, "/admin/items") {
            adminBackofficeService.deleteItem(itemId)
        }

    @PostMapping("/items/{itemId}/replacements")
    fun recordReplacement(
        @PathVariable itemId: Long,
        @ModelAttribute form: AdminReplacementForm,
        redirectAttributes: RedirectAttributes,
    ): String = runAdminAction(redirectAttributes, "/admin/items") {
        adminBackofficeService.recordReplacement(itemId, form)
    }

    private fun runAdminAction(
        redirectAttributes: RedirectAttributes,
        redirectTo: String,
        action: () -> Unit,
    ): String = try {
        action()
        redirectAttributes.addFlashAttribute("message", "Saved.")
        "redirect:$redirectTo"
    } catch (e: RuntimeException) {
        redirectAttributes.addFlashAttribute("error", e.message ?: "Admin action failed.")
        "redirect:$redirectTo"
    }
}
