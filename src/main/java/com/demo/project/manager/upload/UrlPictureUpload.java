package com.demo.project.manager.upload;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpResponse;
import cn.hutool.http.HttpStatus;
import cn.hutool.http.HttpUtil;
import cn.hutool.http.Method;
import com.demo.copicloud.infrastructure.exception.BusinessException;
import com.demo.copicloud.infrastructure.exception.ErrorCode;
import com.demo.copicloud.infrastructure.utils.ThrowUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

@Service
public class UrlPictureUpload extends PictureUploadTemplate{
    /**
     * 校验输入源（ URL）
     *
     * @param inputSource 数据源
     */
    @Override
    protected void validPicture(Object inputSource) {
        // 1.校验非空 以及 校验 url 格式
        String fileUrl = (String) inputSource;
        ThrowUtils.throwIf(StrUtil.isBlank(fileUrl), ErrorCode.PARAMS_ERROR,"文件地址为空");
        try {
            new URL(fileUrl);
        } catch (MalformedURLException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR,"文件 url 格式不正确");
        }

        // 2.校验协议
        ThrowUtils.throwIf(!fileUrl.startsWith("https://") && !fileUrl.startsWith("http://"),
                ErrorCode.PARAMS_ERROR,"图片 url 地址仅支持 http 和 https 协议");

        // 3.发送head请求查看文件是否存在 此处采用 try with resource 自动关闭 httpResponse 释放资源
        try (HttpResponse httpResponse = HttpUtil.createRequest(Method.HEAD, fileUrl).execute()) {
            // 使用工具类构造一个 HEAD 请求 如果未正常返回 则不需要执行其他逻辑
            if (httpResponse.getStatus() != HttpStatus.HTTP_OK) {
                return;
            }

            // 4.文件存在则继续校验 content-type 类型
            String content = httpResponse.header("Content-Type");
            if (StrUtil.isNotBlank(content)) {
                // 允许的图片类型
                final List<String> ALLOW_FORMAT_LIST = Arrays.asList("image/jpeg", "image/jpg", "image/png", "image/webp");
                // 统一转成小写进行格式判断
                ThrowUtils.throwIf(!ALLOW_FORMAT_LIST.contains(content.toLowerCase()),
                        ErrorCode.PARAMS_ERROR, "文件类型错误");
            }

            // 5.校验该文件大小
            String length = httpResponse.header("Content-Length");
            if (StrUtil.isNotBlank(length)) {
                try {
                    // 将大小转为 long 类型
                    long fileSize = Long.parseLong(length);
                    final long ONE_M = 1024 * 1024L;
                    ThrowUtils.throwIf(fileSize > 2 * ONE_M, ErrorCode.PARAMS_ERROR, "文件大小不能超过 2M");
                } catch (NumberFormatException e) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小格式异常");
                }
            }
        }
    }

    /**
     * 获取输入源的原始文件名
     *
     * @param inputSource 数据源 url
     */
    @Override
    protected String getOriginFilename(Object inputSource) {
        String fileUrl = (String) inputSource;
        // 从 url 中获取文件名
        return fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
    }

    /**
     * 处理输入源并生成本地临时文件
     *
     * @param inputSource 数据源 url
     * @param file 本地临时文件
     */
    @Override
    protected void processFile(Object inputSource, File file) throws Exception {
        String fileUrl = (String) inputSource;
        HttpUtil.downloadFile(fileUrl, file);
    }
}
