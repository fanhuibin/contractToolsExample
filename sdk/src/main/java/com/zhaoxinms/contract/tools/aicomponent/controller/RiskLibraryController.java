package com.zhaoxinms.contract.tools.aicomponent.controller;

import com.zhaoxinms.contract.tools.aicomponent.service.RiskLibraryService;
import com.zhaoxinms.contract.tools.common.Result;
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
    public Result<?> listClauseTypes(@RequestParam(value = "enabled", required = false) Boolean enabled) {
        return Result.success("ok", riskLibraryService.listClauseTypes(enabled));
    }

    @GetMapping("/clause/{clauseTypeId}/points")
    public Result<?> listPoints(@PathVariable Long clauseTypeId,
                                @RequestParam(value = "keyword", required = false) String keyword,
                                @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return Result.success("ok", riskLibraryService.listPointsByClause(clauseTypeId, keyword, enabled));
    }

    @GetMapping("/tree")
    public Result<?> tree(@RequestParam(value = "enabled", required = false) Boolean enabled) {
        return Result.success("ok", riskLibraryService.tree(enabled));
    }

    @GetMapping("/tree-prompts")
    public Result<?> treeWithPrompts(@RequestParam(value = "enabled", required = false) Boolean enabled) {
        return Result.success("ok", riskLibraryService.treeWithPrompts(enabled));
    }

    @PostMapping("/selection/preview")
    public Result<?> previewSelection(@RequestBody List<Long> pointIds) {
        return Result.success("ok", riskLibraryService.previewSelection(pointIds));
    }

    // ================= CRUD & Sort =================
    // ClauseType
    @PostMapping("/clause-type")
    public Result<?> createClauseType(@RequestBody ClauseTypeDTO dto) {
        return Result.success("ok", riskLibraryService.createClauseType(dto));
    }

    @PutMapping("/clause-type/{id}")
    public Result<?> updateClauseType(@PathVariable Long id, @RequestBody ClauseTypeDTO dto) {
        return Result.success("ok", riskLibraryService.updateClauseType(id, dto));
    }

    @DeleteMapping("/clause-type/{id}")
    public Result<?> deleteClauseType(@PathVariable Long id) {
        return riskLibraryService.deleteClauseType(id) ? Result.success("ok", true) : Result.error("存在下级数据或删除失败");
    }

    @PatchMapping("/clause-type/{id}/enabled")
    public Result<?> enableClauseType(@PathVariable Long id, @RequestParam("value") boolean value) {
        return riskLibraryService.enableClauseType(id, value) ? Result.success("ok", true) : Result.error("操作失败");
    }

    @PutMapping("/clause-types/reorder")
    public Result<?> reorderClauseTypes(@RequestBody List<IdSortDTO> items) {
        riskLibraryService.reorderClauseTypes(items);
        return Result.success("ok", true);
    }

    // Point
    @PostMapping("/point")
    public Result<?> createPoint(@RequestBody PointDTO dto) {
        return Result.success("ok", riskLibraryService.createPoint(dto));
    }

    @PutMapping("/point/{id}")
    public Result<?> updatePoint(@PathVariable Long id, @RequestBody PointDTO dto) {
        return Result.success("ok", riskLibraryService.updatePoint(id, dto));
    }

    @DeleteMapping("/point/{id}")
    public Result<?> deletePoint(@PathVariable Long id) {
        return riskLibraryService.deletePoint(id) ? Result.success("ok", true) : Result.error("被引用或删除失败");
    }

    @PatchMapping("/point/{id}/enabled")
    public Result<?> enablePoint(@PathVariable Long id, @RequestParam("value") boolean value) {
        return riskLibraryService.enablePoint(id, value) ? Result.success("ok", true) : Result.error("操作失败");
    }

    @PutMapping("/points/reorder")
    public Result<?> reorderPoints(@RequestParam Long clauseTypeId, @RequestBody List<IdSortDTO> items) {
        riskLibraryService.reorderPoints(clauseTypeId, items);
        return Result.success("ok", true);
    }

    // Prompt
    @GetMapping("/point/{pointId}/prompts")
    public Result<?> listPrompts(@PathVariable Long pointId) {
        return Result.success("ok", riskLibraryService.listPrompts(pointId));
    }

    @PostMapping("/prompt")
    public Result<?> createPrompt(@RequestBody PromptDTO dto) {
        return Result.success("ok", riskLibraryService.createPrompt(dto));
    }

    @PutMapping("/prompt/{id}")
    public Result<?> updatePrompt(@PathVariable Long id, @RequestBody PromptDTO dto) {
        return Result.success("ok", riskLibraryService.updatePrompt(id, dto));
    }

    @DeleteMapping("/prompt/{id}")
    public Result<?> deletePrompt(@PathVariable Long id) {
        return riskLibraryService.deletePrompt(id) ? Result.success("ok", true) : Result.error("被引用或删除失败");
    }

    @PatchMapping("/prompt/{id}/enabled")
    public Result<?> enablePrompt(@PathVariable Long id, @RequestParam("value") boolean value) {
        return riskLibraryService.enablePrompt(id, value) ? Result.success("ok", true) : Result.error("操作失败");
    }

    @PutMapping("/prompts/reorder")
    public Result<?> reorderPrompts(@RequestParam Long pointId, @RequestBody List<IdSortDTO> items) {
        riskLibraryService.reorderPrompts(pointId, items);
        return Result.success("ok", true);
    }

    // Action
    @GetMapping("/prompt/{promptId}/actions")
    public Result<?> listActions(@PathVariable Long promptId) {
        return Result.success("ok", riskLibraryService.listActions(promptId));
    }

    @PostMapping("/action")
    public Result<?> createAction(@RequestBody ActionDTO dto) {
        return Result.success("ok", riskLibraryService.createAction(dto));
    }

    @PutMapping("/action/{id}")
    public Result<?> updateAction(@PathVariable Long id, @RequestBody ActionDTO dto) {
        return Result.success("ok", riskLibraryService.updateAction(id, dto));
    }

    @DeleteMapping("/action/{id}")
    public Result<?> deleteAction(@PathVariable Long id) {
        return riskLibraryService.deleteAction(id) ? Result.success("ok", true) : Result.error("删除失败");
    }

    @PatchMapping("/action/{id}/enabled")
    public Result<?> enableAction(@PathVariable Long id, @RequestParam("value") boolean value) {
        return riskLibraryService.enableAction(id, value) ? Result.success("ok", true) : Result.error("操作失败");
    }

    @PutMapping("/actions/reorder")
    public Result<?> reorderActions(@RequestParam Long promptId, @RequestBody List<IdSortDTO> items) {
        riskLibraryService.reorderActions(promptId, items);
        return Result.success("ok", true);
    }

    // Profile
    @GetMapping("/profiles")
    public Result<?> listProfiles() {
        return Result.success("ok", riskLibraryService.listProfiles());
    }

    @PostMapping("/profile")
    public Result<?> createProfile(@RequestBody ProfileDTO dto) {
        return Result.success("ok", riskLibraryService.createProfile(dto));
    }

    @PutMapping("/profile/{id}")
    public Result<?> updateProfile(@PathVariable Long id, @RequestBody ProfileDTO dto) {
        return Result.success("ok", riskLibraryService.updateProfile(id, dto));
    }

    @DeleteMapping("/profile/{id}")
    public Result<?> deleteProfile(@PathVariable Long id, @RequestParam(value = "force", required = false) Boolean force) {
        boolean ok = Boolean.TRUE.equals(force) ? riskLibraryService.deleteProfile(id, true) : riskLibraryService.deleteProfile(id);
        return ok ? Result.success("ok", true) : Result.error(Boolean.TRUE.equals(force) ? "删除失败" : "被引用或删除失败");
    }

    @PatchMapping("/profile/{id}/default")
    public Result<?> setDefaultProfile(@PathVariable Long id, @RequestParam("value") boolean value) {
        return riskLibraryService.setDefaultProfile(id, value) ? Result.success("ok", true) : Result.error("操作失败");
    }

    @GetMapping("/profile/{id}/items")
    public Result<?> listProfileItems(@PathVariable Long id) {
        return Result.success("ok", riskLibraryService.listProfileItems(id));
    }

    @PostMapping("/profile/{id}/items")
    public Result<?> saveProfileItems(@PathVariable Long id, @RequestBody List<ProfileItemDTO> items) {
        riskLibraryService.saveProfileItems(id, items);
        return Result.success("ok", true);
    }

    // ================ Execute review (placeholder) ================
    @PostMapping(value = "/review/execute", consumes = {"multipart/form-data"})
    public Result<?> executeReview(@RequestParam(value = "profileId", required = false) Long profileId,
                                   @RequestPart(value = "file") org.springframework.web.multipart.MultipartFile file,
                                   @RequestPart(value = "pointIds", required = false) List<Long> pointIds) {
        log.info("[review/execute] profileId={}, filePresent={}, points={}", profileId, (file != null && !file.isEmpty()), (pointIds == null ? 0 : pointIds.size()));
        if (file == null || file.isEmpty()) {
            return Result.error("缺少审核文件(file)");
        }
        log.info("[review/execute] enter AI path. filename={}, size={} bytes, contentType={}",
                file.getOriginalFilename(), file.getSize(), file.getContentType());
        var data = riskLibraryService.executeReviewWithAI(file, profileId, pointIds);
        return Result.success("ok", data);
    }

    // ---------- Prompt versioning & examples APIs ----------
    @GetMapping("/prompt/version/list")
    public Result<?> listPromptVersions(@RequestParam Long promptId) {
        return Result.success("ok", riskLibraryService.listPromptVersions(promptId));
    }
    @PostMapping("/prompt/version")
    public Result<?> createPromptVersion(@RequestBody PromptVersionDTO dto) {
        return Result.success("ok", riskLibraryService.createPromptVersion(dto));
    }
    @PutMapping("/prompt/version/{id}")
    public Result<?> updatePromptVersion(@PathVariable Long id, @RequestBody PromptVersionDTO dto) {
        return Result.success("ok", riskLibraryService.updatePromptVersion(id, dto));
    }
    @PutMapping("/prompt/version/{id}/publish")
    public Result<?> publishPromptVersion(@PathVariable Long id, @RequestParam boolean publish) {
        return Result.success("ok", riskLibraryService.publishPromptVersion(id, publish));
    }
    @DeleteMapping("/prompt/version/{id}")
    public Result<?> deletePromptVersion(@PathVariable Long id) {
        return Result.success("ok", riskLibraryService.deletePromptVersion(id));
    }

    @GetMapping("/prompt/example/list")
    public Result<?> listPromptExamples(@RequestParam Long promptVersionId) {
        return Result.success("ok", riskLibraryService.listPromptExamples(promptVersionId));
    }
    @PostMapping("/prompt/example")
    public Result<?> createPromptExample(@RequestBody PromptExampleDTO dto) {
        return Result.success("ok", riskLibraryService.createPromptExample(dto));
    }
    @PutMapping("/prompt/example/{id}")
    public Result<?> updatePromptExample(@PathVariable Long id, @RequestBody PromptExampleDTO dto) {
        return Result.success("ok", riskLibraryService.updatePromptExample(id, dto));
    }
    @DeleteMapping("/prompt/example/{id}")
    public Result<?> deletePromptExample(@PathVariable Long id) {
        return Result.success("ok", riskLibraryService.deletePromptExample(id));
    }
}


