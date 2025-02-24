package com.demo.copicloud.infrastructure.utils;

public class ColorSimilarityUtil {

    /**
     * 计算两个颜色的色调相似度
     *
     * @param color1 第一个颜色，格式为0xRRGGBB
     * @param color2 第二个颜色，格式为0xRRGGBB
     * @return 相似度（0到1之间，1表示完全相同）
     */
    public static double calculateColorSimilarity(String color1, String color2) {
        // 将颜色字符串转换为RGB值
        int[] rgb1 = hexToRgb(color1);
        int[] rgb2 = hexToRgb(color2);

        // 计算欧几里得距离
        double distance = calculateEuclideanDistance(rgb1, rgb2);

        // 将距离转换为相似度（距离越小，相似度越高）
        // 最大距离为sqrt(255^2 + 255^2 + 255^2) ≈ 441.67
        double maxDistance = 441.67;
        double similarity = 1 - (distance / maxDistance);

        // 确保相似度在0到1之间
        return Math.max(0, Math.min(1, similarity));
    }

    /**
     * 将十六进制颜色字符串转换为RGB数组
     *
     * @param hex 颜色字符串，格式为0xRRGGBB
     * @return RGB数组，包含r, g, b三个值
     */
    private static int[] hexToRgb(String hex) {
        // 去掉0x前缀
        hex = hex.replace("0x", "");
        // 解析RGB值
        int r = Integer.parseInt(hex.substring(0, 2), 16);
        int g = Integer.parseInt(hex.substring(2, 4), 16);
        int b = Integer.parseInt(hex.substring(4, 6), 16);
        return new int[]{r, g, b};
    }

    /**
     * 计算两个RGB值之间的欧几里得距离
     *
     * @param rgb1 第一个颜色的RGB值
     * @param rgb2 第二个颜色的RGB值
     * @return 欧几里得距离
     */
    private static double calculateEuclideanDistance(int[] rgb1, int[] rgb2) {
        int rDiff = rgb1[0] - rgb2[0];
        int gDiff = rgb1[1] - rgb2[1];
        int bDiff = rgb1[2] - rgb2[2];
        return Math.sqrt(rDiff * rDiff + gDiff * gDiff + bDiff * bDiff);
    }

}
