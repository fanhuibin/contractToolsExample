package com.zhaoxinms.contract.tools.aicomponent.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhaoxinms.contract.tools.aicomponent.mapper.review.*;
import com.zhaoxinms.contract.tools.aicomponent.model.review.*;
import com.zhaoxinms.contract.tools.aicomponent.service.RiskLibraryService;
import com.zhaoxinms.contract.tools.aicomponent.dto.review.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.ChatCompletionCreateParams;
import com.openai.models.ChatCompletionChunk;
import com.openai.core.http.StreamResponse;
import com.openai.models.FileCreateParams;
import com.openai.models.FileObject;
import com.openai.models.FilePurpose;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhaoxinms.contract.tools.aicomponent.config.AiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Random;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service 
@RequiredArgsConstructor
public class RiskLibraryServiceImpl implements RiskLibraryService {

    private final ReviewClauseTypeMapper clauseTypeMapper;
    private final ReviewPointMapper pointMapper;
    private final ReviewPromptMapper promptMapper;
    private final ReviewActionMapper actionMapper;
    private final ReviewProfileMapper profileMapper;
    private final ReviewProfileItemMapper profileItemMapper;
    private final ReviewPromptVersionMapper promptVersionMapper;
    private final ReviewPromptExampleMapper promptExampleMapper;
    @Autowired
    private AiProperties aiProperties;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public List<ReviewClauseType> listClauseTypes(Boolean enabled) {
        LambdaQueryWrapper<ReviewClauseType> qw = new LambdaQueryWrapper<>();
        if (enabled != null) qw.eq(ReviewClauseType::getEnabled, enabled);
        qw.orderByAsc(ReviewClauseType::getSortOrder).orderByAsc(ReviewClauseType::getId);
        return clauseTypeMapper.selectList(qw);
    }

    @Override
    public List<ReviewPoint> listPointsByClause(Long clauseTypeId, String keyword, Boolean enabled) {
        LambdaQueryWrapper<ReviewPoint> qw = new LambdaQueryWrapper<>();
        if (clauseTypeId != null) qw.eq(ReviewPoint::getClauseTypeId, clauseTypeId);
        if (enabled != null) qw.eq(ReviewPoint::getEnabled, enabled);
        if (keyword != null && !keyword.isEmpty()) {
            qw.like(ReviewPoint::getPointName, keyword).or().like(ReviewPoint::getAlgorithmType, keyword).or().like(ReviewPoint::getPointCode, keyword);
        }
        qw.orderByAsc(ReviewPoint::getSortOrder).orderByAsc(ReviewPoint::getId);
        return pointMapper.selectList(qw);
    }

    @Override
    public List<TreeNode> tree(Boolean enabled) {
        List<ReviewClauseType> clauses = listClauseTypes(enabled);
        List<TreeNode> result = new ArrayList<>();
        for (ReviewClauseType c : clauses) {
            TreeNode n = new TreeNode();
            n.clauseType = c;
            n.points = listPointsByClause(c.getId(), null, enabled);
            result.add(n);
        }
        return result;
    }

    @Override
    public java.util.List<TreeNodeV2> treeWithPrompts(Boolean enabled) {
        // 1) load all clause types (filtered by enabled if provided)
        List<ReviewClauseType> clauses = listClauseTypes(enabled);
        // 2) collect all points under these clauses (respect enabled)
        java.util.List<TreeNodeV2> out = new java.util.ArrayList<>();
        for (ReviewClauseType ct : clauses) {
            TreeNodeV2 node = new TreeNodeV2();
            node.clauseType = ct;
            java.util.List<PointNode> pointNodes = new java.util.ArrayList<>();
            List<ReviewPoint> pts = listPointsByClause(ct.getId(), null, enabled);
            if (pts != null) {
                for (ReviewPoint p : pts) {
                    PointNode pn = new PointNode();
                    pn.point = p;
                    // load prompts for this point (ordered by sort_order,id; respect enabled if provided)
                    LambdaQueryWrapper<ReviewPrompt> qwp = new LambdaQueryWrapper<>();
                    qwp.eq(ReviewPrompt::getPointId, p.getId());
                    if (enabled != null) qwp.eq(ReviewPrompt::getEnabled, enabled);
                    qwp.orderByAsc(ReviewPrompt::getSortOrder).orderByAsc(ReviewPrompt::getId);
                    pn.prompts = promptMapper.selectList(qwp);
                    pointNodes.add(pn);
                }
            }
            node.points = pointNodes;
            out.add(node);
        }
        return out;
    }

    @Override
    public List<OrderItem> previewSelection(List<Long> pointIds) {
        if (pointIds == null || pointIds.isEmpty()) return new ArrayList<>();
        List<ReviewPoint> points = pointMapper.selectBatchIds(pointIds);
        List<ReviewClauseType> clauses = clauseTypeMapper.selectList(null);
        var clauseMap = clauses.stream().collect(Collectors.toMap(ReviewClauseType::getId, it -> it));
        // sort by clause.sortOrder then point.sortOrder
        points.sort(Comparator.comparing((ReviewPoint p) -> clauseMap.get(p.getClauseTypeId()).getSortOrder())
                .thenComparing(ReviewPoint::getSortOrder)
                .thenComparing(ReviewPoint::getId));
        List<OrderItem> out = new ArrayList<>();
        for (ReviewPoint p : points) {
            OrderItem oi = new OrderItem();
            oi.clauseType = clauseMap.get(p.getClauseTypeId()).getClauseName();
            oi.pointId = p.getId();
            oi.pointName = p.getPointName();
            oi.algorithmType = p.getAlgorithmType();
            out.add(oi);
        }
        return out;
    }

    // ================= CRUD & Sort =================
    // ClauseType
    @Override
    @Transactional
    public ReviewClauseType createClauseType(ClauseTypeDTO dto) {
        ReviewClauseType e = new ReviewClauseType();
        e.setClauseCode(dto.getClauseCode());
        e.setClauseName(dto.getClauseName());
        e.setSortOrder(dto.getSortOrder());
        e.setEnabled(dto.getEnabled() == null ? Boolean.TRUE : dto.getEnabled());
        e.setRemark(dto.getRemark());
        clauseTypeMapper.insert(e);
        return e;
    }

    @Override
    @Transactional
    public ReviewClauseType updateClauseType(Long id, ClauseTypeDTO dto) {
        ReviewClauseType e = clauseTypeMapper.selectById(id);
        if (e == null) return null;
        if (dto.getClauseCode() != null) e.setClauseCode(dto.getClauseCode());
        if (dto.getClauseName() != null) e.setClauseName(dto.getClauseName());
        if (dto.getSortOrder() != null) e.setSortOrder(dto.getSortOrder());
        if (dto.getEnabled() != null) e.setEnabled(dto.getEnabled());
        if (dto.getRemark() != null) e.setRemark(dto.getRemark());
        clauseTypeMapper.updateById(e);
        return e;
    }

    @Override
    @Transactional
    public boolean deleteClauseType(Long id) {
        // check children exists
        LambdaQueryWrapper<ReviewPoint> qw = new LambdaQueryWrapper<>();
        qw.eq(ReviewPoint::getClauseTypeId, id);
        if (pointMapper.selectCount(qw) > 0) return false;
        return clauseTypeMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean deleteClauseType(Long id, boolean force) {
        if (!force) return deleteClauseType(id);
        // 级联删除该分类下所有点、其提示/动作，以及方案项
        LambdaQueryWrapper<ReviewPoint> qw = new LambdaQueryWrapper<>();
        qw.eq(ReviewPoint::getClauseTypeId, id);
        List<ReviewPoint> pts = pointMapper.selectList(qw);
        if (pts != null) {
            for (ReviewPoint p : pts) {
                deletePoint(p.getId(), true);
            }
        }
        return clauseTypeMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean enableClauseType(Long id, boolean value) {
        ReviewClauseType e = clauseTypeMapper.selectById(id);
        if (e == null) return false;
        e.setEnabled(value);
        clauseTypeMapper.updateById(e);
        return true;
    }

    @Override
    @Transactional
    public boolean enableClauseType(Long id, boolean value, boolean cascade) {
        ReviewClauseType e = clauseTypeMapper.selectById(id);
        if (e == null) return false;
        e.setEnabled(value);
        clauseTypeMapper.updateById(e);
        if (cascade) {
            // cascade to points -> prompts -> actions
            LambdaQueryWrapper<ReviewPoint> qw = new LambdaQueryWrapper<>();
            qw.eq(ReviewPoint::getClauseTypeId, id);
            List<ReviewPoint> pts = pointMapper.selectList(qw);
            if (pts != null && !pts.isEmpty()) {
                for (ReviewPoint p : pts) {
                    p.setEnabled(value);
                    pointMapper.updateById(p);

                    LambdaQueryWrapper<ReviewPrompt> qwp = new LambdaQueryWrapper<>();
                    qwp.eq(ReviewPrompt::getPointId, p.getId());
                    List<ReviewPrompt> prs = promptMapper.selectList(qwp);
                    if (prs != null && !prs.isEmpty()) {
                        for (ReviewPrompt pr : prs) {
                            pr.setEnabled(value);
                            promptMapper.updateById(pr);

                            LambdaQueryWrapper<ReviewAction> qwa = new LambdaQueryWrapper<>();
                            qwa.eq(ReviewAction::getPromptId, pr.getId());
                            List<ReviewAction> acts = actionMapper.selectList(qwa);
                            if (acts != null && !acts.isEmpty()) {
                                for (ReviewAction a : acts) {
                                    a.setEnabled(value);
                                    actionMapper.updateById(a);
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    @Transactional
    public void reorderClauseTypes(List<IdSortDTO> items) {
        if (items == null) return;
        for (IdSortDTO it : items) {
            ReviewClauseType e = clauseTypeMapper.selectById(it.getId());
            if (e == null) continue;
            e.setSortOrder(it.getSortOrder());
            clauseTypeMapper.updateById(e);
        }
    }

    // Point
    @Override
    @Transactional
    public ReviewPoint createPoint(PointDTO dto) {
        ReviewPoint e = new ReviewPoint();
        e.setClauseTypeId(dto.getClauseTypeId());
        // 自动分配自定义编码：当未传入或为空时，分配下一个 ZX-XXXX 编码
        String incomingCode = dto.getPointCode();
        if (incomingCode == null || incomingCode.trim().isEmpty()) {
            e.setPointCode(generateNextPointCode());
        } else {
            e.setPointCode(incomingCode.trim());
        }
        e.setPointName(dto.getPointName());
        e.setAlgorithmType(dto.getAlgorithmType());
        e.setSortOrder(dto.getSortOrder());
        e.setEnabled(dto.getEnabled() == null ? Boolean.TRUE : dto.getEnabled());
        pointMapper.insert(e);
        return e;
    }

    @Override
    @Transactional
    public ReviewPoint updatePoint(Long id, PointDTO dto) {
        ReviewPoint e = pointMapper.selectById(id);
        if (e == null) return null;
        if (dto.getClauseTypeId() != null) e.setClauseTypeId(dto.getClauseTypeId());
        if (dto.getPointCode() != null) e.setPointCode(dto.getPointCode());
        if (dto.getPointName() != null) e.setPointName(dto.getPointName());
        if (dto.getAlgorithmType() != null) e.setAlgorithmType(dto.getAlgorithmType());
        if (dto.getSortOrder() != null) e.setSortOrder(dto.getSortOrder());
        if (dto.getEnabled() != null) e.setEnabled(dto.getEnabled());
        pointMapper.updateById(e);
        return e;
    }

    @Override
    @Transactional
    public boolean deletePoint(Long id) {
        // check prompts
        LambdaQueryWrapper<ReviewPrompt> qw = new LambdaQueryWrapper<>();
        qw.eq(ReviewPrompt::getPointId, id);
        if (promptMapper.selectCount(qw) > 0) return false;
        // check profile items
        LambdaQueryWrapper<ReviewProfileItem> qw2 = new LambdaQueryWrapper<>();
        qw2.eq(ReviewProfileItem::getPointId, id);
        if (profileItemMapper.selectCount(qw2) > 0) return false;
        return pointMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean deletePoint(Long id, boolean force) {
        if (!force) return deletePoint(id);
        // 级联删除该点下的 prompts 与其 actions、以及方案条目
        // 1) 删除 actions -> prompts
        LambdaQueryWrapper<ReviewPrompt> qwp = new LambdaQueryWrapper<>();
        qwp.eq(ReviewPrompt::getPointId, id);
        List<ReviewPrompt> prs = promptMapper.selectList(qwp);
        if (prs != null && !prs.isEmpty()) {
            List<Long> pidList = prs.stream().map(ReviewPrompt::getId).collect(java.util.stream.Collectors.toList());
            if (!pidList.isEmpty()) {
                LambdaQueryWrapper<ReviewAction> qwa = new LambdaQueryWrapper<>();
                qwa.in(ReviewAction::getPromptId, pidList);
                actionMapper.delete(qwa);
            }
            // 删除 prompts
            promptMapper.delete(qwp);
        }
        // 2) 删除 profile items
        LambdaQueryWrapper<ReviewProfileItem> qpi = new LambdaQueryWrapper<>();
        qpi.eq(ReviewProfileItem::getPointId, id);
        profileItemMapper.delete(qpi);
        // 3) 删除 point
        return pointMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean enablePoint(Long id, boolean value) {
        ReviewPoint e = pointMapper.selectById(id);
        if (e == null) return false;
        e.setEnabled(value);
        pointMapper.updateById(e);
        return true;
    }

    @Override
    @Transactional
    public boolean enablePoint(Long id, boolean value, boolean cascade) {
        ReviewPoint e = pointMapper.selectById(id);
        if (e == null) return false;
        e.setEnabled(value);
        pointMapper.updateById(e);
        if (cascade) {
            // cascade to prompts -> actions
            LambdaQueryWrapper<ReviewPrompt> qwp = new LambdaQueryWrapper<>();
            qwp.eq(ReviewPrompt::getPointId, id);
            List<ReviewPrompt> prs = promptMapper.selectList(qwp);
            if (prs != null && !prs.isEmpty()) {
                for (ReviewPrompt pr : prs) {
                    pr.setEnabled(value);
                    promptMapper.updateById(pr);

                    LambdaQueryWrapper<ReviewAction> qwa = new LambdaQueryWrapper<>();
                    qwa.eq(ReviewAction::getPromptId, pr.getId());
                    List<ReviewAction> acts = actionMapper.selectList(qwa);
                    if (acts != null && !acts.isEmpty()) {
                        for (ReviewAction a : acts) {
                            a.setEnabled(value);
                            actionMapper.updateById(a);
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    @Transactional
    public void reorderPoints(Long clauseTypeId, List<IdSortDTO> items) {
        if (items == null) return;
        for (IdSortDTO it : items) {
            ReviewPoint e = pointMapper.selectById(it.getId());
            if (e == null) continue;
            if (clauseTypeId != null && !clauseTypeId.equals(e.getClauseTypeId())) continue;
            e.setSortOrder(it.getSortOrder());
            pointMapper.updateById(e);
        }
    }

    // Prompt
    @Override
    public List<ReviewPrompt> listPrompts(Long pointId) {
        LambdaQueryWrapper<ReviewPrompt> qw = new LambdaQueryWrapper<>();
        qw.eq(ReviewPrompt::getPointId, pointId).orderByAsc(ReviewPrompt::getSortOrder).orderByAsc(ReviewPrompt::getId);
        return promptMapper.selectList(qw);
    }

    @Override
    @Transactional
    public ReviewPrompt createPrompt(PromptDTO dto) {
        ReviewPrompt e = new ReviewPrompt();
        e.setPointId(dto.getPointId());
        // 分配全局唯一的 prompt_key：优先使用传入的 promptKey，否则取 name；若冲突自动追加 -2/-3...
        String baseKey = dto.getPromptKey();
        if (baseKey == null || baseKey.trim().isEmpty()) baseKey = dto.getName();
        if (baseKey == null || baseKey.trim().isEmpty()) baseKey = "prompt-" + System.currentTimeMillis();
        String uniqueKey = ensurePromptKeyUnique(baseKey.trim(), null);
        e.setPromptKey(uniqueKey);
        e.setName(dto.getName());
        e.setMessage(dto.getMessage());
        e.setStatusType(dto.getStatusType());
        e.setSortOrder(dto.getSortOrder());
        e.setEnabled(dto.getEnabled() == null ? Boolean.TRUE : dto.getEnabled());
        promptMapper.insert(e);
        return e;
    }

    @Override
    @Transactional
    public ReviewPrompt updatePrompt(Long id, PromptDTO dto) {
        ReviewPrompt e = promptMapper.selectById(id);
        if (e == null) return null;
        if (dto.getPointId() != null) e.setPointId(dto.getPointId());
        if (dto.getPromptKey() != null) {
            String nk = dto.getPromptKey().trim();
            if (nk.isEmpty()) nk = e.getName() == null ? ("prompt-" + System.currentTimeMillis()) : e.getName();
            nk = ensurePromptKeyUnique(nk, id);
            e.setPromptKey(nk);
        }
        if (dto.getName() != null) e.setName(dto.getName());
        if (dto.getMessage() != null) e.setMessage(dto.getMessage());
        if (dto.getStatusType() != null) e.setStatusType(dto.getStatusType());
        if (dto.getSortOrder() != null) e.setSortOrder(dto.getSortOrder());
        if (dto.getEnabled() != null) e.setEnabled(dto.getEnabled());
        promptMapper.updateById(e);
        return e;
    }

    // 保障 review_prompt.prompt_key 全局唯一；ignoreId 用于更新时忽略自身
    private String ensurePromptKeyUnique(String key, Long ignoreId) {
        String normalized = key;
        if (normalized.length() > 120) { // 预留给后缀，避免超出 128
            normalized = normalized.substring(0, 120);
        }
        String candidate = normalized;
        int suffix = 2;
        while (true) {
            LambdaQueryWrapper<ReviewPrompt> qw = new LambdaQueryWrapper<>();
            qw.eq(ReviewPrompt::getPromptKey, candidate);
            if (ignoreId != null) {
                qw.ne(ReviewPrompt::getId, ignoreId);
            }
            Long cnt = promptMapper.selectCount(qw);
            if (cnt == null || cnt == 0L) return candidate;
            String base = normalized;
            String add = "-" + suffix;
            if (base.length() + add.length() > 128) {
                base = base.substring(0, 128 - add.length());
            }
            candidate = base + add;
            suffix++;
        }
    }

    @Override
    @Transactional
    public boolean deletePrompt(Long id) {
        // check actions
        LambdaQueryWrapper<ReviewAction> qw = new LambdaQueryWrapper<>();
        qw.eq(ReviewAction::getPromptId, id);
        if (actionMapper.selectCount(qw) > 0) return false;
        return promptMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean deletePrompt(Long id, boolean force) {
        if (!force) return deletePrompt(id);
        // 级联删除该提示下的所有动作
        LambdaQueryWrapper<ReviewAction> qwa = new LambdaQueryWrapper<>();
        qwa.eq(ReviewAction::getPromptId, id);
        actionMapper.delete(qwa);
        return promptMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean enablePrompt(Long id, boolean value) {
        ReviewPrompt e = promptMapper.selectById(id);
        if (e == null) return false;
        e.setEnabled(value);
        promptMapper.updateById(e);
        return true;
    }

    @Override
    @Transactional
    public boolean enablePrompt(Long id, boolean value, boolean cascade) {
        ReviewPrompt e = promptMapper.selectById(id);
        if (e == null) return false;
        e.setEnabled(value);
        promptMapper.updateById(e);
        if (cascade) {
            LambdaQueryWrapper<ReviewAction> qwa = new LambdaQueryWrapper<>();
            qwa.eq(ReviewAction::getPromptId, id);
            List<ReviewAction> acts = actionMapper.selectList(qwa);
            if (acts != null && !acts.isEmpty()) {
                for (ReviewAction a : acts) {
                    a.setEnabled(value);
                    actionMapper.updateById(a);
                }
            }
        }
        return true;
    }

    @Override
    @Transactional
    public void reorderPrompts(Long pointId, List<IdSortDTO> items) {
        if (items == null) return;
        for (IdSortDTO it : items) {
            ReviewPrompt e = promptMapper.selectById(it.getId());
            if (e == null) continue;
            if (pointId != null && !pointId.equals(e.getPointId())) continue;
            e.setSortOrder(it.getSortOrder());
            promptMapper.updateById(e);
        }
    }

    // Action
    @Override
    public List<ReviewAction> listActions(Long promptId) {
        LambdaQueryWrapper<ReviewAction> qw = new LambdaQueryWrapper<>();
        qw.eq(ReviewAction::getPromptId, promptId).orderByAsc(ReviewAction::getSortOrder).orderByAsc(ReviewAction::getId);
        return actionMapper.selectList(qw);
    }

    @Override
    @Transactional
    public ReviewAction createAction(ActionDTO dto) {
        ReviewAction e = new ReviewAction();
        e.setPromptId(dto.getPromptId());
        e.setActionId(dto.getActionId());
        e.setActionType(dto.getActionType());
        e.setActionMessage(dto.getActionMessage());
        e.setSortOrder(dto.getSortOrder());
        e.setEnabled(dto.getEnabled() == null ? Boolean.TRUE : dto.getEnabled());
        actionMapper.insert(e);
        return e;
    }

    @Override
    @Transactional
    public ReviewAction updateAction(Long id, ActionDTO dto) {
        ReviewAction e = actionMapper.selectById(id);
        if (e == null) return null;
        if (dto.getPromptId() != null) e.setPromptId(dto.getPromptId());
        if (dto.getActionId() != null) e.setActionId(dto.getActionId());
        if (dto.getActionType() != null) e.setActionType(dto.getActionType());
        if (dto.getActionMessage() != null) e.setActionMessage(dto.getActionMessage());
        if (dto.getSortOrder() != null) e.setSortOrder(dto.getSortOrder());
        if (dto.getEnabled() != null) e.setEnabled(dto.getEnabled());
        actionMapper.updateById(e);
        return e;
    }

    // 生成下一个 ZX 编码（形如 ZX-0001、ZX-0002 ...），按照 point_code 字符串倒序取最大值再 +1
    private String generateNextPointCode() {
        // 查找现有以 ZX- 前缀的最大编码
        LambdaQueryWrapper<ReviewPoint> qw = new LambdaQueryWrapper<>();
        qw.like(ReviewPoint::getPointCode, "ZX-");
        qw.orderByDesc(ReviewPoint::getPointCode);
        qw.last("LIMIT 1");
        List<ReviewPoint> list = pointMapper.selectList(qw);
        int next = 1;
        if (list != null && !list.isEmpty()) {
            String maxCode = list.get(0).getPointCode();
            if (maxCode != null && maxCode.startsWith("ZX-")) {
                try {
                    String num = maxCode.substring(3).trim();
                    // 去除非数字字符的干扰
                    num = num.replaceAll("[^0-9]", "");
                    if (!num.isEmpty()) {
                        next = Integer.parseInt(num) + 1;
                    }
                } catch (Exception ignore) {}
            }
        }
        return String.format("ZX-%04d", next);
    }

    @Override
    @Transactional
    public boolean deleteAction(Long id) {
        return actionMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean enableAction(Long id, boolean value) {
        ReviewAction e = actionMapper.selectById(id);
        if (e == null) return false;
        e.setEnabled(value);
        actionMapper.updateById(e);
        return true;
    }

    @Override
    @Transactional
    public void reorderActions(Long promptId, List<IdSortDTO> items) {
        if (items == null) return;
        for (IdSortDTO it : items) {
            ReviewAction e = actionMapper.selectById(it.getId());
            if (e == null) continue;
            if (promptId != null && !promptId.equals(e.getPromptId())) continue;
            e.setSortOrder(it.getSortOrder());
            actionMapper.updateById(e);
        }
    }

    // Profile
    @Override
    public List<ReviewProfile> listProfiles() {
        return profileMapper.selectList(null);
    }

    @Override
    @Transactional
    public ReviewProfile createProfile(ProfileDTO dto) {
        ReviewProfile e = new ReviewProfile();
        e.setProfileCode(dto.getProfileCode());
        e.setProfileName(dto.getProfileName());
        e.setDescription(dto.getDescription());
        e.setIsDefault(dto.getIsDefault() != null && dto.getIsDefault());
        profileMapper.insert(e);
        return e;
    }

    @Override
    @Transactional
    public ReviewProfile updateProfile(Long id, ProfileDTO dto) {
        ReviewProfile e = profileMapper.selectById(id);
        if (e == null) return null;
        if (dto.getProfileCode() != null) e.setProfileCode(dto.getProfileCode());
        if (dto.getProfileName() != null) e.setProfileName(dto.getProfileName());
        if (dto.getDescription() != null) e.setDescription(dto.getDescription());
        if (dto.getIsDefault() != null) e.setIsDefault(dto.getIsDefault());
        profileMapper.updateById(e);
        return e;
    }

    @Override
    @Transactional
    public boolean deleteProfile(Long id) {
        LambdaQueryWrapper<ReviewProfileItem> qw = new LambdaQueryWrapper<>();
        qw.eq(ReviewProfileItem::getProfileId, id);
        if (profileItemMapper.selectCount(qw) > 0) return false;
        return profileMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean deleteProfile(Long id, boolean force) {
        LambdaQueryWrapper<ReviewProfileItem> qw = new LambdaQueryWrapper<>();
        qw.eq(ReviewProfileItem::getProfileId, id);
        Long cnt = profileItemMapper.selectCount(qw);
        if (cnt != null && cnt > 0L) {
            if (!force) return false;
            profileItemMapper.delete(qw);
        }
        return profileMapper.deleteById(id) > 0;
    }

    @Override
    @Transactional
    public boolean setDefaultProfile(Long id, boolean isDefault) {
        // unset others if set default
        if (isDefault) {
            List<ReviewProfile> all = profileMapper.selectList(null);
            for (ReviewProfile p : all) {
                if (Boolean.TRUE.equals(p.getIsDefault())) {
                    p.setIsDefault(false);
                    profileMapper.updateById(p);
                }
            }
        }
        ReviewProfile e = profileMapper.selectById(id);
        if (e == null) return false;
        e.setIsDefault(isDefault);
        profileMapper.updateById(e);
        return true;
    }

    @Override
    public List<ReviewProfileItem> listProfileItems(Long profileId) {
        LambdaQueryWrapper<ReviewProfileItem> qw = new LambdaQueryWrapper<>();
        qw.eq(ReviewProfileItem::getProfileId, profileId).orderByAsc(ReviewProfileItem::getSortOrder).orderByAsc(ReviewProfileItem::getId);
        return profileItemMapper.selectList(qw);
    }

    @Override
    @Transactional
    public void saveProfileItems(Long profileId, List<ProfileItemDTO> items) {
        // delete old items then insert new
        LambdaQueryWrapper<ReviewProfileItem> del = new LambdaQueryWrapper<>();
        del.eq(ReviewProfileItem::getProfileId, profileId);
        profileItemMapper.delete(del);
        if (items == null) return;
        for (ProfileItemDTO it : items) {
            ReviewProfileItem e = new ReviewProfileItem();
            e.setProfileId(profileId);
            e.setClauseTypeId(it.getClauseTypeId());
            e.setPointId(it.getPointId());
            e.setSortOrder(it.getSortOrder());
            profileItemMapper.insert(e);
        }
    }

    // ===== Prompt versioning & examples =====
    @Override
    public java.util.List<ReviewPromptVersion> listPromptVersions(Long promptId) {
        LambdaQueryWrapper<ReviewPromptVersion> qw = new LambdaQueryWrapper<>();
        qw.eq(ReviewPromptVersion::getPromptId, promptId)
          .orderByDesc(ReviewPromptVersion::getIsPublished)
          .orderByDesc(ReviewPromptVersion::getId);
        return promptVersionMapper.selectList(qw);
    }

    @Override
    @Transactional
    public ReviewPromptVersion createPromptVersion(PromptVersionDTO dto) {
        ReviewPromptVersion v = new ReviewPromptVersion();
        v.setPromptId(dto.getPromptId());
        v.setVersionCode(dto.getVersionCode());
        v.setIsPublished(Boolean.TRUE.equals(dto.getIsPublished()));
        v.setContentText(dto.getContentText());
        v.setRemark(dto.getRemark());
        promptVersionMapper.insert(v);
        return v;
    }

    @Override
    @Transactional
    public ReviewPromptVersion updatePromptVersion(Long id, PromptVersionDTO dto) {
        ReviewPromptVersion v = promptVersionMapper.selectById(id);
        if (v == null) return null;
        if (dto.getVersionCode() != null) v.setVersionCode(dto.getVersionCode());
        if (dto.getIsPublished() != null) v.setIsPublished(dto.getIsPublished());
        if (dto.getContentText() != null) v.setContentText(dto.getContentText());
        if (dto.getRemark() != null) v.setRemark(dto.getRemark());
        promptVersionMapper.updateById(v);
        return v;
    }

    @Override
    @Transactional
    public boolean publishPromptVersion(Long id, boolean publish) {
        ReviewPromptVersion v = promptVersionMapper.selectById(id);
        if (v == null) return false;
        // unpublish other versions of same prompt if publish=true
        if (publish) {
            LambdaQueryWrapper<ReviewPromptVersion> qw = new LambdaQueryWrapper<>();
            qw.eq(ReviewPromptVersion::getPromptId, v.getPromptId());
            java.util.List<ReviewPromptVersion> all = promptVersionMapper.selectList(qw);
            for (ReviewPromptVersion it : all) {
                if (Boolean.TRUE.equals(it.getIsPublished())) {
                    it.setIsPublished(false);
                    promptVersionMapper.updateById(it);
                }
            }
        }
        v.setIsPublished(publish);
        promptVersionMapper.updateById(v);
        return true;
    }

    @Override
    @Transactional
    public boolean deletePromptVersion(Long id) {
        return promptVersionMapper.deleteById(id) > 0;
    }

    @Override
    public java.util.List<ReviewPromptExample> listPromptExamples(Long promptVersionId) {
        LambdaQueryWrapper<ReviewPromptExample> qw = new LambdaQueryWrapper<>();
        qw.eq(ReviewPromptExample::getPromptVersionId, promptVersionId)
          .orderByAsc(ReviewPromptExample::getSortOrder)
          .orderByAsc(ReviewPromptExample::getId);
        return promptExampleMapper.selectList(qw);
    }

    @Override
    @Transactional
    public ReviewPromptExample createPromptExample(PromptExampleDTO dto) {
        ReviewPromptExample e = new ReviewPromptExample();
        e.setPromptVersionId(dto.getPromptVersionId());
        e.setUserExample(dto.getUserExample());
        e.setAssistantExample(dto.getAssistantExample());
        e.setSortOrder(dto.getSortOrder());
        promptExampleMapper.insert(e);
        return e;
    }

    @Override
    @Transactional
    public ReviewPromptExample updatePromptExample(Long id, PromptExampleDTO dto) {
        ReviewPromptExample e = promptExampleMapper.selectById(id);
        if (e == null) return null;
        if (dto.getUserExample() != null) e.setUserExample(dto.getUserExample());
        if (dto.getAssistantExample() != null) e.setAssistantExample(dto.getAssistantExample());
        if (dto.getSortOrder() != null) e.setSortOrder(dto.getSortOrder());
        promptExampleMapper.updateById(e);
        return e;
    }

    @Override
    @Transactional
    public boolean deletePromptExample(Long id) {
        return promptExampleMapper.deleteById(id) > 0;
    }

    private OpenAIClient createClient() {
        String apiKey = aiProperties.getApiKey().get(new Random().nextInt(aiProperties.getApiKey().size()));
        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .baseUrl(aiProperties.getApiHost())
                .build();
    }

    @Override
    public java.util.Map<String, Object> executeReviewWithAI(org.springframework.web.multipart.MultipartFile file, Long profileId, List<Long> pointIds) {
        try {
            // 1) 解析所选点
            List<Long> selectedPointIds = new ArrayList<>();
            if (pointIds != null && !pointIds.isEmpty()) selectedPointIds.addAll(pointIds);
            else if (profileId != null) {
                var items = listProfileItems(profileId);
                for (var it : items) if (it.getPointId() != null) selectedPointIds.add(it.getPointId());
            }
            if (selectedPointIds.isEmpty()) selectedPointIds.add(-1L);

            // 2) 构建提示：按 point -> prompts（作为模型判定规则与输出字段）
            LambdaQueryWrapper<ReviewPrompt> qwp = new LambdaQueryWrapper<>();
            qwp.in(ReviewPrompt::getPointId, selectedPointIds).eq(ReviewPrompt::getEnabled, true)
                    .orderByAsc(ReviewPrompt::getPointId).orderByAsc(ReviewPrompt::getSortOrder);
            List<ReviewPrompt> prompts = promptMapper.selectList(qwp);
            var pointMap = pointMapper.selectBatchIds(selectedPointIds).stream()
                    .collect(Collectors.toMap(ReviewPoint::getId, it -> it));
            var clauseMap = clauseTypeMapper.selectList(null).stream()
                    .collect(Collectors.toMap(ReviewClauseType::getId, it -> it));

            StringBuilder sb = new StringBuilder();
            sb.append("你是合同智能审核助理。请阅读系统提供的文件内容，并按以下审查点逐一判断，输出严格JSON。\\n");
            sb.append("输出JSON结构：{\\\"traceId\\\":string, \\\"elapsedMs\\\":number, \\\"docMeta\\\":{pages:number, paragraphs:number}, \\\"results\\\":[{\\\"clauseType\\\":string, \\\"pointId\\\":string, \\\"algorithmType\\\":string, \\\"decisionType\\\":string, \\\"statusType\\\":\\\"INFO|WARNING|ERROR\\\", \\\"message\\\":string, \\\"actions\\\":[{\\\"actionID\\\":string, \\\"actionType\\\":string}], \\\"evidence\\\":[{\\\"text\\\":string, \\\"page\\\":number, \\\"paragraphIndex\\\":number, \\\"startOffset\\\":number, \\\"endOffset\\\":number}]}]}\\n");
            sb.append("判定说明：仅对命中的审查点生成 message 与 evidence；未命中仍输出该点但不包含 message 与 evidence（仅给出 decisionType/statusType）。\\n");
            sb.append("动作说明：从 possibleActions 中选择适用的动作，最多 2 条；输出时仅保留 actionID 与 actionType。\\n");
            sb.append("审查点列表（按此顺序输出）：\\n");
            // 批量加载动作，避免 N+1：一次性查出所有 prompt 的动作并分组
            java.util.List<Long> allPromptIds = prompts.stream().map(ReviewPrompt::getId).collect(java.util.stream.Collectors.toList());
            java.util.Map<Long, java.util.List<ReviewAction>> promptIdToActions;
            if (allPromptIds.isEmpty()) {
                promptIdToActions = java.util.Collections.emptyMap();
            } else {
                LambdaQueryWrapper<ReviewAction> qwaAll = new LambdaQueryWrapper<>();
                qwaAll.in(ReviewAction::getPromptId, allPromptIds)
                        .eq(ReviewAction::getEnabled, true)
                        .orderByAsc(ReviewAction::getPromptId)
                        .orderByAsc(ReviewAction::getSortOrder)
                        .orderByAsc(ReviewAction::getId);
                List<ReviewAction> allActs = actionMapper.selectList(qwaAll);
                promptIdToActions = allActs.stream().collect(Collectors.groupingBy(ReviewAction::getPromptId));
            }

            for (ReviewPrompt pr : prompts) {
                ReviewPoint p = pointMap.get(pr.getPointId());
                if (p == null) continue;
                ReviewClauseType ct = clauseMap.get(p.getClauseTypeId());
                List<ReviewAction> acts = promptIdToActions.getOrDefault(pr.getId(), java.util.Collections.emptyList());

                // load latest published prompt version as message template override, and few-shot examples
                String messageTemplate = pr.getMessage() == null ? "" : pr.getMessage().replace("\n"," ");
                com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.zhaoxinms.contract.tools.aicomponent.model.review.ReviewPromptVersion> qv = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
                qv.eq(com.zhaoxinms.contract.tools.aicomponent.model.review.ReviewPromptVersion::getPromptId, pr.getId())
                  .eq(com.zhaoxinms.contract.tools.aicomponent.model.review.ReviewPromptVersion::getIsPublished, true)
                  .orderByDesc(com.zhaoxinms.contract.tools.aicomponent.model.review.ReviewPromptVersion::getId);
                var versions = promptVersionMapper.selectList(qv);
                if (versions != null && !versions.isEmpty()) {
                    var v = versions.get(0);
                    if (v.getContentText() != null && !v.getContentText().isEmpty()) {
                        messageTemplate = v.getContentText().replace("\n"," ");
                    }
                }

                sb.append("- ")
                  .append(ct != null ? ct.getClauseName() : "").append(" | pointId=")
                  .append(p.getPointCode() != null ? p.getPointCode() : p.getId())
                  .append(" | algorithmType=").append(p.getAlgorithmType())
                  .append(" | decisionType=").append(pr.getName())
                  .append(" | expectedLevel=").append(pr.getStatusType())
                  .append(" | messageTemplate=").append(messageTemplate)
                  .append(" | possibleActions=[");
                for (int i = 0; i < acts.size(); i++) {
                    ReviewAction a = acts.get(i);
                    sb.append("{actionID=").append(a.getActionId())
                      .append(", actionType=").append(a.getActionType())
                      .append("}");
                    if (i < acts.size() - 1) sb.append(", ");
                }
                sb.append("]\\n");
            }
            sb.append("要求：只输出严格JSON，不要解释；未命中不包含 message/evidence；evidence.text 必须来自原文，最多3段、每段≤120字；actions 最多2条，仅包含 actionID/actionType。\n");
            sb.append("在JSON输出结束后，另起一行输出用量信息，格式严格如下（不要放入JSON内）：\\n");
            sb.append("USAGE: promptTokens=<number>; completionTokens=<number>; totalTokens=<number>; creditsRemaining=<number|unknown>\n");

            // 3) 上传文件到DashScope
            Path tmp = Files.createTempFile("review-", ".bin");
            file.transferTo(tmp);
            OpenAIClient client = createClient();
            FileObject fobj = client.files().create(FileCreateParams.builder().file(tmp).purpose(FilePurpose.of("file-extract")).build());

            // 4) 模型调用
            ChatCompletionCreateParams.Builder builder = ChatCompletionCreateParams.builder()
                    .addSystemMessage("你是一个专业的合同审核助手。请只返回有效JSON。")
                    .addSystemMessage("fileid://" + fobj.id())
                    .addUserMessage(sb.toString())
                    .model(aiProperties.getModel().getMode());

            // 降温与上限，缩时提稳
            try {
                builder = builder.temperature(0.1).topP(0.3);
            } catch (Exception ignore) {}
            ChatCompletionCreateParams chat = builder.build();
            StringBuilder full = new StringBuilder();
            try (StreamResponse<ChatCompletionChunk> stream = client.chat().completions().createStreaming(chat)) {
                stream.stream().forEach(ch -> full.append(ch.choices().get(0).delta().content().orElse("")));
            }

            // 5) 解析与返回
            String raw = full.toString();
            String json = sanitizeModelJson(raw);
            java.util.Map<String, Object> data;
            try {
                data = objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>(){});
            } catch (Exception parseEx) {
                // 一次纠偏重试：追加"仅返回JSON"提醒
                OpenAIClient client2 = createClient();
                ChatCompletionCreateParams chat2 = ChatCompletionCreateParams.builder()
                        .addSystemMessage("只返回严格JSON，不要解释。")
                        .addSystemMessage("fileid://" + fobj.id())
                        .addUserMessage(sb.toString())
                        .model(aiProperties.getModel().getMode())
                        .build();
                StringBuilder full2 = new StringBuilder();
                try (StreamResponse<ChatCompletionChunk> stream2 = client2.chat().completions().createStreaming(chat2)) {
                    stream2.stream().forEach(ch2 -> full2.append(ch2.choices().get(0).delta().content().orElse("")));
                }
                String raw2 = full2.toString();
                String json2 = sanitizeModelJson(raw2);
                data = objectMapper.readValue(json2, new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>(){});
            }
            if (!data.containsKey("traceId")) data.put("traceId", java.util.UUID.randomUUID().toString());
            // 从原始模型输出中尝试解析 USAGE 行（不放入JSON内部，由前端单独显示）
            try {
                java.util.Map<String, Object> usage = parseUsageFromRaw(raw);
                if (usage != null && !usage.isEmpty()) {
                    // 附带提示词字符数，便于前端展示 "字数"
                    usage.put("promptChars", sb.length());
                    data.put("usage", usage);
                }
            } catch (Exception ignore) {}
            return data;
        } catch (Exception ex) {
            throw new RuntimeException("AI审核失败: " + ex.getMessage(), ex);
        }
    }

    private String sanitizeModelJson(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        int start = s.indexOf("```");
        if (start >= 0) {
            int end = s.indexOf("```", start + 3);
            if (end > start) {
                String inside = s.substring(start + 3, end);
                inside = inside.replaceFirst("^\\s*[a-zA-Z]+\\s*", "");
                return inside.trim();
            }
            s = s.substring(start + 3).trim();
            s = s.replaceFirst("^json\\s*", "");
            return s;
        }
        if (s.startsWith("json\n") || s.startsWith("json\r\n")) return s.substring(5).trim();
        return s;
    }

    private java.util.Map<String, Object> parseUsageFromRaw(String raw) {
        if (raw == null) return java.util.Collections.emptyMap();
        // 查找以 "USAGE:" 开头的行
        String[] lines = raw.split("\r?\n");
        String usageLine = null;
        for (String line : lines) {
            String t = line.trim();
            if (t.startsWith("USAGE:")) { usageLine = t; break; }
        }
        if (usageLine == null) return java.util.Collections.emptyMap();
        // 形如：USAGE: promptTokens=123; completionTokens=456; totalTokens=579; creditsRemaining=9999
        java.util.Map<String, Object> map = new java.util.HashMap<>();
        String payload = usageLine.substring("USAGE:".length()).trim();
        String[] parts = payload.split(";\\s*");
        for (String part : parts) {
            String[] kv = part.split("=");
            if (kv.length == 2) {
                String k = kv[0].trim();
                String v = kv[1].trim();
                if (v.matches("[0-9]+")) {
                    map.put(k, Integer.parseInt(v));
                } else {
                    map.put(k, v);
                }
            }
        }
        return map;
    }

    @Override
    public java.util.Map<String, Object> executeReview(Long profileId, List<Long> pointIds) {
        // Build selected points based on profile or explicit ids
        List<Long> selectedPointIds = new ArrayList<>();
        if (pointIds != null && !pointIds.isEmpty()) {
            selectedPointIds.addAll(pointIds);
        } else if (profileId != null) {
            var items = listProfileItems(profileId);
            for (var it : items) {
                if (it.getPointId() != null) selectedPointIds.add(it.getPointId());
            }
        }
        if (selectedPointIds.isEmpty()) selectedPointIds.add(-1L);

        // Query prompts for selected points
        LambdaQueryWrapper<ReviewPrompt> qwp = new LambdaQueryWrapper<>();
        qwp.in(ReviewPrompt::getPointId, selectedPointIds).eq(ReviewPrompt::getEnabled, true)
                .orderByAsc(ReviewPrompt::getPointId).orderByAsc(ReviewPrompt::getSortOrder);
        List<ReviewPrompt> prompts = promptMapper.selectList(qwp);

        // Build findings grouped by point
        var pointMap = pointMapper.selectBatchIds(selectedPointIds).stream()
                .collect(Collectors.toMap(ReviewPoint::getId, it -> it));
        var clauseMap = clauseTypeMapper.selectList(null).stream()
                .collect(Collectors.toMap(ReviewClauseType::getId, it -> it));

        List<java.util.Map<String, Object>> results = new ArrayList<>();
        for (ReviewPrompt pr : prompts) {
            ReviewPoint p = pointMap.get(pr.getPointId());
            if (p == null) continue;
            ReviewClauseType ct = clauseMap.get(p.getClauseTypeId());
            java.util.Map<String, Object> one = new java.util.HashMap<>();
            one.put("clauseType", ct != null ? ct.getClauseName() : "");
            one.put("pointId", String.valueOf(p.getPointCode() != null ? p.getPointCode() : p.getId()));
            one.put("algorithmType", p.getAlgorithmType());
            one.put("decisionType", pr.getName());
            one.put("statusType", pr.getStatusType());
            one.put("message", pr.getMessage());
            // actions (empty placeholder)
            one.put("actions", java.util.Collections.emptyList());
            // evidence (empty placeholder)
            one.put("evidence", java.util.Collections.emptyList());
            results.add(one);
        }

        java.util.Map<String, Object> out = new java.util.HashMap<>();
        out.put("traceId", java.util.UUID.randomUUID().toString());
        out.put("elapsedMs", 0);
        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("pages", 0);
        meta.put("paragraphs", 0);
        out.put("docMeta", meta);
        out.put("results", results);
        return out;
    }
}


