package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;

import com.example.redchina.entity.SysCulturalTerm;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 文化术语Mapper接口（多语言支持核心）
 */
public interface SysCulturalTermMapper extends BaseMapper<SysCulturalTerm> {
    /**
     * 分页查询术语（支持标签筛选、关键词搜索、语言类型筛选）
     * @param page 分页参数
     * @param tagId 标签ID（可选）
     * @param keyword 搜索关键词（中文/英文，可选）
     * @param languageType 语言类型（可选，如zh_CN/en_US）
     * @return 分页术语列表
     */
    IPage<SysCulturalTerm> selectTermPage(IPage<SysCulturalTerm> page,
                                          @Param("tagId") Long tagId,
                                          @Param("keyword") String keyword,
                                          @Param("languageType") String languageType);

    /**
     * 模糊查询术语（支持中文/英文，适配前端搜索）
     * @param keyword 搜索关键词
     * @return 匹配的术语列表
     */
    List<SysCulturalTerm> selectByKeyword(@Param("keyword") String keyword);

    /**
     * 根据标签ID和语言类型查询术语（个性化推荐用）
     * @param tagId 标签ID
     * @param languageType 语言类型（zh_CN/en_US/ja_JP/ko_KR）
     * @return 术语列表
     */
    List<SysCulturalTerm> selectByTagIdAndLanguage(@Param("tagId") Long tagId,
                                                   @Param("languageType") String languageType);

    /**
     * 根据语言类型查询所有术语（多语言导出用）
     * @param languageType 语言类型
     * @return 该语言下的所有术语
     */
    List<SysCulturalTerm> selectAllByLanguage(@Param("languageType") String languageType);

    /**
     * 批量查询术语（根据术语ID列表）
     * @param termIds 术语ID列表
     * @return 术语列表
     */
    List<SysCulturalTerm> selectBatchByIds(@Param("termIds") List<Long> termIds);

    /**
     * 校验中文术语唯一性（新增/编辑时使用）
     * @param termChinese 中文术语
     * @param id 术语ID（编辑时传入，排除自身）
     * @return 存在数量（0=唯一，1=已存在）
     */
    int checkTermChineseUnique(@Param("termChinese") String termChinese,
                               @Param("id") Long id);

    /**
     * 查询术语关联的标签名称（用于前端展示）
     * @param termId 术语ID
     * @return 标签名称
     */
    String selectTagNameByTermId(@Param("termId") Long termId);

    List<SysCulturalTerm> selectHotTerms(@Param("limit") Integer limit);

    List<SysCulturalTerm> selectByTagIds(@Param("tagIds") List<Long> tagIds);
}
