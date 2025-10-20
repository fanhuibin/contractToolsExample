package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.zhaoxinms.contract.tools.aicomponent.service.RiskLibraryService;
import com.zhaoxinms.contract.tools.api.common.ApiResponse;
import com.zhaoxinms.contract.tools.aicomponent.dto.review.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai/review-lib")
@RequiredArgsConstructor
@Slf4j
public class RiskLibraryController {

    private final RiskLibraryService riskLibraryService;

    @GetMapping("/clause-types")
    public ApiResponse<?> listClauseTypes(@RequestParam(value = "enabled", required = false) Boolean enabled) {
        return ApiResponse.success("ok", riskLibraryService.listClauseTypes(enabled));
    }

    @GetMapping("/clause/{clauseTypeId}/points")
    public ApiResponse<?> listPoints(@PathVariable Long clauseTypeId,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return ApiResponse.success("ok", riskLibraryService.listPointsByClause(clauseTypeId, keyword, enabled));
    }

    @GetMapping("/tree")
    public ApiResponse<?> tree(@RequestParam(value = "enabled", required = false) Boolean enabled) {
        return ApiResponse.success("ok", riskLibraryService.tree(enabled));
    }

    @GetMapping("/tree-prompts")
    public ApiResponse<?> treeWithPrompts(@RequestParam(value = "enabled", required = false) Boolean enabled) {
        return ApiResponse.success("ok", riskLibraryService.treeWithPrompts(enabled));
    }

    @PostMapping("/selection/preview")
    public ApiResponse<?> previewSelection(@RequestBody List<Long> pointIds) {
        return ApiResponse.success("ok", riskLibraryService.previewSelection(pointIds));
    }

    // ================= CRUD & Sort =================
    // ClauseType
    @PostMapping("/clause-type")
    public ApiResponse<?> createClauseType(@RequestBody ClauseTypeDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.createClauseType(dto));
    }

    @PutMapping("/clause-type/{id}")
    public ApiResponse<?> updateClauseType(@PathVariable Long id, @RequestBody ClauseTypeDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.updateClauseType(id, dto));
    }

    @DeleteMapping("/clause-type/{id}")
    public ApiResponse<?> deleteClauseType(@PathVariable Long id, @RequestParam(value = "force", required = false) Boolean force) {
        boolean ok = Boolean.TRUE.equals(force) ? riskLibraryService.deleteClauseType(id, true) : riskLibraryService.deleteClauseType(id);
        return ok ? ApiResponse.success("ok", true) : ApiResponse.businessError(Boolean.TRUE.equals(force) ? "删除失败" : "存在下级数据或删除失败");
    }

    @PatchMapping("/clause-type/{id}/enabled")
    public ApiResponse<?> enableClauseType(@PathVariable Long id, @RequestParam("value") boolean value,
                                      @RequestParam(value = "cascade", required = false) Boolean cascade) {
        boolean ok = Boolean.TRUE.equals(cascade) ? riskLibraryService.enableClauseType(id, value, true) : riskLibraryService.enableClauseType(id, value);
        return ok ? ApiResponse.success("ok", true) : ApiResponse.businessError("操作失败");
    }

    @PutMapping("/clause-types/reorder")
    public ApiResponse<?> reorderClauseTypes(@RequestBody List<IdSortDTO> items) {
        riskLibraryService.reorderClauseTypes(items);
        return ApiResponse.success("ok", true);
    }

    // Point
    @PostMapping("/point")
    public ApiResponse<?> createPoint(@RequestBody PointDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.createPoint(dto));
    }

    @PutMapping("/point/{id}")
    public ApiResponse<?> updatePoint(@PathVariable Long id, @RequestBody PointDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.updatePoint(id, dto));
    }

    @DeleteMapping("/point/{id}")
    public ApiResponse<?> deletePoint(@PathVariable Long id, @RequestParam(value = "force", required = false) Boolean force) {
        boolean ok = Boolean.TRUE.equals(force) ? riskLibraryService.deletePoint(id, true) : riskLibraryService.deletePoint(id);
        return ok ? ApiResponse.success("ok", true) : ApiResponse.businessError(Boolean.TRUE.equals(force) ? "删除失败" : "被引用或删除失败");
    }

    @PatchMapping("/point/{id}/enabled")
    public ApiResponse<?> enablePoint(@PathVariable Long id, @RequestParam("value") boolean value,
                                 @RequestParam(value = "cascade", required = false) Boolean cascade) {
        boolean ok = Boolean.TRUE.equals(cascade) ? riskLibraryService.enablePoint(id, value, true) : riskLibraryService.enablePoint(id, value);
        return ok ? ApiResponse.success("ok", true) : ApiResponse.businessError("操作失败");
    }

    @PutMapping("/points/reorder")
    public ApiResponse<?> reorderPoints(@RequestParam Long clauseTypeId, @RequestBody List<IdSortDTO> items) {
        riskLibraryService.reorderPoints(clauseTypeId, items);
        return ApiResponse.success("ok", true);
    }

    // Prompt
    @GetMapping("/point/{pointId}/prompts")
    public ApiResponse<?> listPrompts(@PathVariable Long pointId) {
        return ApiResponse.success("ok", riskLibraryService.listPrompts(pointId));
    }

    @PostMapping("/prompt")
    public ApiResponse<?> createPrompt(@RequestBody PromptDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.createPrompt(dto));
    }

    @PutMapping("/prompt/{id}")
    public ApiResponse<?> updatePrompt(@PathVariable Long id, @RequestBody PromptDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.updatePrompt(id, dto));
    }

    @DeleteMapping("/prompt/{id}")
    public ApiResponse<?> deletePrompt(@PathVariable Long id, @RequestParam(value = "force", required = false) Boolean force) {
        boolean ok = Boolean.TRUE.equals(force) ? riskLibraryService.deletePrompt(id, true) : riskLibraryService.deletePrompt(id);
        return ok ? ApiResponse.success("ok", true) : ApiResponse.businessError(Boolean.TRUE.equals(force) ? "删除失败" : "被引用或删除失败");
    }

    @PatchMapping("/prompt/{id}/enabled")
    public ApiResponse<?> enablePrompt(@PathVariable Long id, @RequestParam("value") boolean value,
                                  @RequestParam(value = "cascade", required = false) Boolean cascade) {
        boolean ok = Boolean.TRUE.equals(cascade) ? riskLibraryService.enablePrompt(id, value, true) : riskLibraryService.enablePrompt(id, value);
        return ok ? ApiResponse.success("ok", true) : ApiResponse.businessError("操作失败");
    }

    @PutMapping("/prompts/reorder")
    public ApiResponse<?> reorderPrompts(@RequestParam Long pointId, @RequestBody List<IdSortDTO> items) {
        riskLibraryService.reorderPrompts(pointId, items);
        return ApiResponse.success("ok", true);
    }

    // Action
    @GetMapping("/prompt/{promptId}/actions")
    public ApiResponse<?> listActions(@PathVariable Long promptId) {
        return ApiResponse.success("ok", riskLibraryService.listActions(promptId));
    }

    @PostMapping("/action")
    public ApiResponse<?> createAction(@RequestBody ActionDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.createAction(dto));
    }

    @PutMapping("/action/{id}")
    public ApiResponse<?> updateAction(@PathVariable Long id, @RequestBody ActionDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.updateAction(id, dto));
    }

    @DeleteMapping("/action/{id}")
    public ApiResponse<?> deleteAction(@PathVariable Long id) {
        return riskLibraryService.deleteAction(id) ? ApiResponse.success("ok", true) : ApiResponse.businessError("删除失败");
    }

    @PatchMapping("/action/{id}/enabled")
    public ApiResponse<?> enableAction(@PathVariable Long id, @RequestParam("value") boolean value) {
        return riskLibraryService.enableAction(id, value) ? ApiResponse.success("ok", true) : ApiResponse.businessError("操作失败");
    }

    @PutMapping("/actions/reorder")
    public ApiResponse<?> reorderActions(@RequestParam Long promptId, @RequestBody List<IdSortDTO> items) {
        riskLibraryService.reorderActions(promptId, items);
        return ApiResponse.success("ok", true);
    }

    // Profile
    @GetMapping("/profiles")
    public ApiResponse<?> listProfiles() {
        return ApiResponse.success("ok", riskLibraryService.listProfiles());
    }

    @PostMapping("/profile")
    public ApiResponse<?> createProfile(@RequestBody ProfileDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.createProfile(dto));
    }

    @PutMapping("/profile/{id}")
    public ApiResponse<?> updateProfile(@PathVariable Long id, @RequestBody ProfileDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.updateProfile(id, dto));
    }

    @DeleteMapping("/profile/{id}")
    public ApiResponse<?> deleteProfile(@PathVariable Long id, @RequestParam(value = "force", required = false) Boolean force) {
        boolean ok = Boolean.TRUE.equals(force) ? riskLibraryService.deleteProfile(id, true) : riskLibraryService.deleteProfile(id);
        return ok ? ApiResponse.success("ok", true) : ApiResponse.businessError(Boolean.TRUE.equals(force) ? "删除失败" : "被引用或删除失败");
    }

    @PatchMapping("/profile/{id}/default")
    public ApiResponse<?> setDefaultProfile(@PathVariable Long id, @RequestParam("value") boolean value) {
        return riskLibraryService.setDefaultProfile(id, value) ? ApiResponse.success("ok", true) : ApiResponse.businessError("操作失败");
    }

    @GetMapping("/profile/{id}/items")
    public ApiResponse<?> listProfileItems(@PathVariable Long id) {
        return ApiResponse.success("ok", riskLibraryService.listProfileItems(id));
    }

    @PostMapping("/profile/{id}/items")
    public ApiResponse<?> saveProfileItems(@PathVariable Long id, @RequestBody List<ProfileItemDTO> items) {
        riskLibraryService.saveProfileItems(id, items);
        return ApiResponse.success("ok", true);
    }

    // ================ Execute review (placeholder) ================
    @PostMapping(value = "/review/execute", consumes = {"multipart/form-data"})
    public ApiResponse<?> executeReview(@RequestParam(value = "profileId", required = false) Long profileId,
                                   @RequestPart(value = "file") org.springframework.web.multipart.MultipartFile file,
                                   @RequestPart(value = "pointIds", required = false) List<Long> pointIds) {
        log.info("[review/execute] profileId={}, filePresent={}, points={}", profileId, (file != null && !file.isEmpty()), (pointIds == null ? 0 : pointIds.size()));
        if (file == null || file.isEmpty()) {
            return ApiResponse.businessError("缺少审核文件(file)");
        }
        log.info("[review/execute] enter AI path. filename={}, size={} bytes, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());
        var data = riskLibraryService.executeReviewWithAI(file, profileId, pointIds);
        return ApiResponse.success("ok", data);
    }

    // ---------- Prompt versioning & examples APIs ----------
    @GetMapping("/prompt/version/list")
    public ApiResponse<?> listPromptVersions(@RequestParam Long promptId) {
        return ApiResponse.success("ok", riskLibraryService.listPromptVersions(promptId));
    }
    @PostMapping("/prompt/version")
    public ApiResponse<?> createPromptVersion(@RequestBody PromptVersionDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.createPromptVersion(dto));
    }
    @PutMapping("/prompt/version/{id}")
    public ApiResponse<?> updatePromptVersion(@PathVariable Long id, @RequestBody PromptVersionDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.updatePromptVersion(id, dto));
    }
    @PutMapping("/prompt/version/{id}/publish")
    public ApiResponse<?> publishPromptVersion(@PathVariable Long id, @RequestParam boolean publish) {
        return ApiResponse.success("ok", riskLibraryService.publishPromptVersion(id, publish));
    }
    @DeleteMapping("/prompt/version/{id}")
    public ApiResponse<?> deletePromptVersion(@PathVariable Long id) {
        return ApiResponse.success("ok", riskLibraryService.deletePromptVersion(id));
    }

    @GetMapping("/prompt/example/list")
    public ApiResponse<?> listPromptExamples(@RequestParam Long promptVersionId) {
        return ApiResponse.success("ok", riskLibraryService.listPromptExamples(promptVersionId));
    }
    @PostMapping("/prompt/example")
    public ApiResponse<?> createPromptExample(@RequestBody PromptExampleDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.createPromptExample(dto));
    }
    @PutMapping("/prompt/example/{id}")
    public ApiResponse<?> updatePromptExample(@PathVariable Long id, @RequestBody PromptExampleDTO dto) {
        return ApiResponse.success("ok", riskLibraryService.updatePromptExample(id, dto));
    }
    @DeleteMapping("/prompt/example/{id}")
    public ApiResponse<?> deletePromptExample(@PathVariable Long id) {
        return ApiResponse.success("ok", riskLibraryService.deletePromptExample(id));
    }
}


