package com.xjtu.springboot.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xjtu.springboot.common.Result;
import com.xjtu.springboot.pojo.File;
import com.xjtu.springboot.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class FileController {
    @Autowired
    private FileService fileService;

    @RequestMapping(method = RequestMethod.POST, path = "/file/upload")
    public Result uploadFile(@RequestParam("multipartFile") MultipartFile multipartFile,
                             @RequestParam("fileJson") String fileJson) {
        try {
            File file = new ObjectMapper().readValue(
                    fileJson,
                    new TypeReference<File>() {
                    }
            );
            return Result.success(fileService.uploadFile(multipartFile, file));
        } catch (JsonProcessingException e) {
            return Result.error("文件信息解析失败：" + e.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.POST, path = "/files/upload")
    public Result uploadFiles(@RequestParam("multipartFileList") List<MultipartFile> multipartFileList,
                              @RequestParam("fileListJson") String fileListJson) {
        try {
            List<File> fileList = new ObjectMapper().readValue(
                    fileListJson,
                    new TypeReference<List<File>>() {
                    }
            );
            return Result.success(fileService.uploadFiles(multipartFileList, fileList));
        } catch (JsonProcessingException e) {
            return Result.error("文件信息解析失败：" + e.getMessage());
        }
    }

    @RequestMapping(method = RequestMethod.GET, path = "/file/download")
    public Result downloadFile(
            @RequestParam("fileName") String fileName,
            HttpServletResponse response) {
        if (!StringUtils.isEmpty(fileName)) {
            return Result.error("文件名不能为空");
        }

        try {
            byte[] fileBytes = fileService.downloadFile(fileName);
            if (fileBytes == null || fileBytes.length == 0) {
                return Result.error("文件不存在或内容为空");
            }
            String contentType = fileService.getContentType(fileName);
            response.setContentType(contentType);
            String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replace("+", "%20");
            response.setHeader("Content-Disposition",
                    "attachment; filename*=UTF-8''" + encodedFileName);
            response.setContentLengthLong(fileBytes.length);

            try (OutputStream os = response.getOutputStream();
                 BufferedOutputStream bos = new BufferedOutputStream(os)) {
                bos.write(fileBytes);
                bos.flush();
            }

            return Result.success();
        } catch (IOException e) {
            return Result.error("文件下载失败：" + e.getMessage());
        } catch (Exception e) {
            return Result.error("系统异常：" + e.getMessage());
        }
    }

}
