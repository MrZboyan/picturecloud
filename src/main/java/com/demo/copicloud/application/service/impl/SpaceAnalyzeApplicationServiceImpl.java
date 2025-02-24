package com.demo.copicloud.application.service.impl;

import com.demo.copicloud.application.service.SpaceAnalyzeApplicationService;
import com.demo.copicloud.domain.sapceAnalyze.service.SpaceAnalyzeDomainService;
import com.demo.copicloud.domain.space.entity.Space;
import com.demo.copicloud.domain.user.entity.User;
import com.demo.copicloud.interfaces.dto.space.space.analyze.*;
import com.demo.copicloud.interfaces.vo.space.analyze.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * @author Zangdibo
 * createDate 2025-01-13 16:17:46
 */
@Slf4j
@Service
public class SpaceAnalyzeApplicationServiceImpl implements SpaceAnalyzeApplicationService {

    @Resource
    private SpaceAnalyzeDomainService spaceAnalyzeDomainService;

    /**
     * 空间资源使用分析
     *
     * @param spaceUsageAnalyzeRequest 空间分析请求
     * @param loginUser                当前登录用户
     */
    @Override
    public SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser) {
        return spaceAnalyzeDomainService.getSpaceUsageAnalyze(spaceUsageAnalyzeRequest, loginUser);
    }

    /**
     * 空间分类情况分析
     *
     * @param spaceCategoryAnalyzeRequest 空间分类情况分析请求
     * @param loginUser                   当前登录用户
     */
    @Override
    public List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser) {
        return spaceAnalyzeDomainService.getSpaceCategoryAnalyze(spaceCategoryAnalyzeRequest, loginUser);
    }

    /**
     * 空间标签情况分析
     *
     * @param spaceTagAnalyzeRequest 空间分类情况分析请求
     * @param loginUser              当前登录用户
     */
    @Override
    public List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser) {
        return spaceAnalyzeDomainService.getSpaceTagAnalyze(spaceTagAnalyzeRequest, loginUser);
    }

    @Override
    public List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser) {
        return spaceAnalyzeDomainService.getSpaceSizeAnalyze(spaceSizeAnalyzeRequest, loginUser);
    }

    @Override
    public List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser) {
        return spaceAnalyzeDomainService.getSpaceUserAnalyze(spaceUserAnalyzeRequest, loginUser);
    }

    /**
     * 获取空间排行
     *
     * @param spaceRankAnalyzeRequest 请求参数
     */
    @Override
    public List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser) {
        return spaceAnalyzeDomainService.getSpaceRankAnalyze(spaceRankAnalyzeRequest, loginUser);
    }

}




