package com.example.redchina.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.example.redchina.entity.SysCulturalTerm;
import com.example.redchina.mapper.SysCulturalTermMapper;
import com.example.redchina.service.SysCulturalTermService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.util.ArrayList;
import java.util.List;

@Service
public class SysCulturalTermServiceImpl extends ServiceImpl<SysCulturalTermMapper, SysCulturalTerm> implements SysCulturalTermService {


    @Resource
    private SysCulturalTermMapper culturalTermMapper;


    @Override
    public List<SysCulturalTerm> selectByKeyword(String keyword) {
        return baseMapper.selectByKeyword(keyword);
    }

    @Override
    public List<SysCulturalTerm> selectByTagIdAndLanguage(Long tagId, String languageType) {
        return baseMapper.selectByTagIdAndLanguage(tagId, languageType);
    }

    @Override
    public SysCulturalTerm getTermWithMultiLanguage(Long id) {
        // 查询术语完整信息（含所有语言翻译）
        return getById(id);
    }

    @Override
    @Transactional
    public boolean saveOrUpdateTerm(SysCulturalTerm term) {
        // 校验中文术语唯一性
        LambdaQueryWrapper<SysCulturalTerm> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SysCulturalTerm::getTermChinese, term.getTermChinese());
        if (term.getId() != null) {
            queryWrapper.ne(SysCulturalTerm::getId, term.getId());
        }
        if (count(queryWrapper) > 0) {
            throw new RuntimeException("中文术语已存在");
        }
        return saveOrUpdate(term);
    }


    @Override
    public List<SysCulturalTerm> getHotTerms(Integer limit) {
        // 调用Mapper查询热门术语（按推荐状态和创建时间排序）
        return culturalTermMapper.selectHotTerms(limit);
    }

    @Override
    public List<SysCulturalTerm> getByTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return new ArrayList<>();
        }
        return culturalTermMapper.selectByTagIds(tagIds);
    }
}
