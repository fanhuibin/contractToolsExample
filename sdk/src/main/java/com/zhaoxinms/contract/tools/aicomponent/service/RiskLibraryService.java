package com.zhaoxinms.contract.tools.aicomponent.service;

import com.zhaoxinms.contract.tools.aicomponent.model.review.*;
import com.zhaoxinms.contract.tools.aicomponent.dto.review.*;

import java.util.List;

public interface RiskLibraryService {
    List<ReviewClauseType> listClauseTypes(Boolean enabled);
    List<ReviewPoint> listPointsByClause(Long clauseTypeId, String keyword, Boolean enabled);
    List<TreeNode> tree(Boolean enabled);
    /**
     * Tree API with prompts as third-level children under points.
     * Returns: clause -> points -> prompts
     */
    List<TreeNodeV2> treeWithPrompts(Boolean enabled);
    List<OrderItem> previewSelection(List<Long> pointIds);

    // DTOs for lightweight responses
    class TreeNode {
        public ReviewClauseType clauseType;
        public List<ReviewPoint> points;
    }
    /**
     * Lightweight DTOs for tree-with-prompts response
     */
    class TreeNodeV2 {
        public ReviewClauseType clauseType;
        public java.util.List<PointNode> points;
    }

    class PointNode {
        public ReviewPoint point;
        public java.util.List<ReviewPrompt> prompts;
    }
    class OrderItem {
        public String clauseType;
        public Long pointId;
        public String pointName;
        public String algorithmType;
    }

    // --- CRUD & sort ---
    // ClauseType
    ReviewClauseType createClauseType(ClauseTypeDTO dto);
    ReviewClauseType updateClauseType(Long id, ClauseTypeDTO dto);
    boolean deleteClauseType(Long id);
    boolean deleteClauseType(Long id, boolean force);
    boolean enableClauseType(Long id, boolean value);
    boolean enableClauseType(Long id, boolean value, boolean cascade);
    void reorderClauseTypes(java.util.List<IdSortDTO> items);

    // Point
    ReviewPoint createPoint(PointDTO dto);
    ReviewPoint updatePoint(Long id, PointDTO dto);
    boolean deletePoint(Long id);
    boolean deletePoint(Long id, boolean force);
    boolean enablePoint(Long id, boolean value);
    boolean enablePoint(Long id, boolean value, boolean cascade);
    void reorderPoints(Long clauseTypeId, java.util.List<IdSortDTO> items);

    // Prompt
    java.util.List<ReviewPrompt> listPrompts(Long pointId);
    ReviewPrompt createPrompt(PromptDTO dto);
    ReviewPrompt updatePrompt(Long id, PromptDTO dto);
    boolean deletePrompt(Long id);
    boolean deletePrompt(Long id, boolean force);
    boolean enablePrompt(Long id, boolean value);
    boolean enablePrompt(Long id, boolean value, boolean cascade);
    void reorderPrompts(Long pointId, java.util.List<IdSortDTO> items);

    // Action
    java.util.List<ReviewAction> listActions(Long promptId);
    ReviewAction createAction(ActionDTO dto);
    ReviewAction updateAction(Long id, ActionDTO dto);
    boolean deleteAction(Long id);
    boolean enableAction(Long id, boolean value);
    void reorderActions(Long promptId, java.util.List<IdSortDTO> items);

    // Profile
    java.util.List<ReviewProfile> listProfiles();
    ReviewProfile createProfile(ProfileDTO dto);
    ReviewProfile updateProfile(Long id, ProfileDTO dto);
    boolean deleteProfile(Long id);
    boolean deleteProfile(Long id, boolean force);
    boolean setDefaultProfile(Long id, boolean isDefault);
    java.util.List<ReviewProfileItem> listProfileItems(Long profileId);
    void saveProfileItems(Long profileId, java.util.List<ProfileItemDTO> items);

    // Execute review (lightweight placeholder using DB prompts/actions)
    java.util.Map<String, Object> executeReview(Long profileId, java.util.List<Long> pointIds);

    // Execute review with AI (real Qwen call): multipart file + selected points
    java.util.Map<String, Object> executeReviewWithAI(org.springframework.web.multipart.MultipartFile file,
                                                     Long profileId,
                                                     java.util.List<Long> pointIds);

    // Prompt versioning & examples
    java.util.List<com.zhaoxinms.contract.tools.aicomponent.model.review.ReviewPromptVersion> listPromptVersions(Long promptId);
    com.zhaoxinms.contract.tools.aicomponent.model.review.ReviewPromptVersion createPromptVersion(com.zhaoxinms.contract.tools.aicomponent.dto.review.PromptVersionDTO dto);
    com.zhaoxinms.contract.tools.aicomponent.model.review.ReviewPromptVersion updatePromptVersion(Long id, com.zhaoxinms.contract.tools.aicomponent.dto.review.PromptVersionDTO dto);
    boolean publishPromptVersion(Long id, boolean publish);
    boolean deletePromptVersion(Long id);
    java.util.List<com.zhaoxinms.contract.tools.aicomponent.model.review.ReviewPromptExample> listPromptExamples(Long promptVersionId);
    com.zhaoxinms.contract.tools.aicomponent.model.review.ReviewPromptExample createPromptExample(com.zhaoxinms.contract.tools.aicomponent.dto.review.PromptExampleDTO dto);
    com.zhaoxinms.contract.tools.aicomponent.model.review.ReviewPromptExample updatePromptExample(Long id, com.zhaoxinms.contract.tools.aicomponent.dto.review.PromptExampleDTO dto);
    boolean deletePromptExample(Long id);
}


