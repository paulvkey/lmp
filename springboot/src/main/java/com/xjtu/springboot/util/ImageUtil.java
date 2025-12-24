package com.xjtu.springboot.util;

import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtil {
    /**
     * 精准判断：是否为图片（通过ImageIO解析文件流，无法伪造）
     * @return true=是图片；false=非图片
     */
    public static boolean isImage(MultipartFile file) {
        if (file.isEmpty()) {
            return false;
        }
        // 流必须重复读取，需标记为可重置（或通过ByteArrayInputStream包装）
        try (InputStream inputStream = file.getInputStream()) {
            // ImageIO.read()能解析出BufferedImage → 是有效图片
            BufferedImage image = ImageIO.read(inputStream);
            return image != null;
        } catch (IOException e) {
            // 解析失败（非图片/文件损坏）
            return false;
        }
    }

    /**
     * 获取图片宽高
     * @return 数组：[宽度, 高度]；解析失败返回null
     */
    public static int[] getWidthHeight(MultipartFile file) {
        if (file.isEmpty()) {
            return null;
        }
        // 关键：MultipartFile的流默认只能读一次，需重置或重新获取
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                return null;
            }
            int width = image.getWidth();
            int height = image.getHeight();
            return new int[]{width, height};
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 一站式判断+获取宽高（推荐业务使用）
     * @return 数组：[是否为图片(1/0), 宽度, 高度]；异常返回 [0, 0, 0]
     */
    public static int[] checkAndGetSize(MultipartFile file) {
        try {
            // 第一步：精准判断是否为图片
            boolean isImage = isImage(file);
            if (!isImage) {
                return new int[]{0, 0, 0}; // 非图片
            }
            // 第二步：获取宽高
            int[] size = getWidthHeight(file);
            if (size == null) {
                return new int[]{1, 0, 0}; // 是图片但解析宽高失败（极少）
            }
            return new int[]{1, size[0], size[1]}; // 是图片+宽高
        } catch (Exception e) {
            return new int[]{0, 0, 0};
        }
    }
}
