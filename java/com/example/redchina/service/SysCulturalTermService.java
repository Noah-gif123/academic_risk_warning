package com.example.redchina.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.redchina.entity.SysCulturalTerm;


import java.util.List;

public interface SysCulturalTermService extends IService<SysCulturalTerm> {
    // 模糊查询术语（中文/英文）
    List<SysCulturalTerm> selectByKeyword(String keyword);

    // 根据标签ID和语言类型查询术语
    List<SysCulturalTerm> selectByTagIdAndLanguage(Long tagId, String languageType);

    // 根据术语ID获取多语言完整信息
    SysCulturalTerm getTermWithMultiLanguage(Long id);

    // 管理员新增/编辑术语（含多语言）
    boolean saveOrUpdateTerm(SysCulturalTerm term);

    /**
     * 查询热门文化术语（按访问量或推荐度排序）
     * @param limit 限制返回数量
     * @return 热门术语列表
     */
    List<SysCulturalTerm> getHotTerms(Integer limit);

    /**
     * 根据标签ID列表查询文化术语
     * @param tagIds 标签ID列表
     * @return 匹配的术语列表
     */
    List<SysCulturalTerm> getByTagIds(List<Long> tagIds);
}
