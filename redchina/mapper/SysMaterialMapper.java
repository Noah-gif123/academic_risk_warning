package com.example.redchina.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.redchina.entity.SysMaterial;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 * 文创素材Mapper
 */
public interface SysMaterialMapper extends BaseMapper<SysMaterial> {

    // 按标签ID查询素材（用于个性化推荐）
    List<SysMaterial> selectByTagId(@Param("tagId") Long tagId);

    // 按多个标签ID查询素材
    List<SysMaterial> selectByTagIds(@Param("tagIds") List<Long> tagIds);

    // 按语言类型查询素材
    List<SysMaterial> selectByLanguageType(@Param("languageType") String languageType);

    // 分页查询素材（支持标签、语言筛选）
    IPage<SysMaterial> selectMaterialPage(IPage<SysMaterial> page, @Param("tagId") Long tagId, @Param("languageType") String languageType);

    // 查询热门素材（按下载次数排序）
    List<SysMaterial> selectHotMaterials(@Param("limit") Integer limit);

    // 素材下载次数+1
    int incrementDownloadCount(@Param("id") Long id);
}
