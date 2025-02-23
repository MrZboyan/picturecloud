package com.demo.project.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.demo.project.model.dto.space.analyze.*;
import com.demo.project.model.entity.Space;
import com.demo.project.model.entity.User;
import com.demo.project.model.vo.space.analyze.*;

import java.util.List;

/**
* @author Zangdibo
* createDate 2025-01-13 16:17:46
*/
public interface SpaceAnalyzeService extends IService<Space> {

    /**
     * 空间资源使用分析
     *
     * @param spaceUsageAnalyzeRequest 空间分析请求
     * @param loginUser 当前登录用户
     */
    SpaceUsageAnalyzeResponse getSpaceUsageAnalyze(SpaceUsageAnalyzeRequest spaceUsageAnalyzeRequest, User loginUser);

    /**
     * 空间分类情况分析
     *
     * @param spaceCategoryAnalyzeRequest 空间分类情况分析请求
     * @param loginUser 当前登录用户
     */
    List<SpaceCategoryAnalyzeResponse> getSpaceCategoryAnalyze(SpaceCategoryAnalyzeRequest spaceCategoryAnalyzeRequest, User loginUser);

    /**
     * 空间标签情况分析
     *
     * @param spaceTagAnalyzeRequest 空间分类情况分析请求
     * @param loginUser 当前登录用户
     */
    List<SpaceTagAnalyzeResponse> getSpaceTagAnalyze(SpaceTagAnalyzeRequest spaceTagAnalyzeRequest, User loginUser);

    /**
     * 空间图片大小情况分析
     *
     * @param spaceSizeAnalyzeRequest 空间分类情况分析请求
     * @param loginUser 当前登录用户
     */
    List<SpaceSizeAnalyzeResponse> getSpaceSizeAnalyze(SpaceSizeAnalyzeRequest spaceSizeAnalyzeRequest, User loginUser);

    /**
     * 空间用户情况分析
     *
     * @param spaceUserAnalyzeRequest 空间用户情况分析请求
     */
    List<SpaceUserAnalyzeResponse> getSpaceUserAnalyze(SpaceUserAnalyzeRequest spaceUserAnalyzeRequest, User loginUser);

    List<Space> getSpaceRankAnalyze(SpaceRankAnalyzeRequest spaceRankAnalyzeRequest, User loginUser);
}
