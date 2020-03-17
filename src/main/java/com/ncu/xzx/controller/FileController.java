package com.ncu.xzx.controller;

import com.ncu.xzx.model.FileVo;
import com.ncu.xzx.model.UserToken;
import com.ncu.xzx.service.FileService;
import com.ncu.xzx.service.UserTokenService;
import com.ncu.xzx.utils.Response;
import com.ncu.xzx.utils.ResponseCode;
import com.ncu.xzx.utils.UserLoginToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Date;

@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    UserTokenService userTokenService;

    @Autowired
    FileService fileService;

    public static String PATH = "/Users/vivo/upload";

//    public static String PATH = "D:\\fileupload";

    @PostMapping("/upload")
    @UserLoginToken
    public Response upload(@RequestParam("token") String token, @RequestParam("file") MultipartFile file) {

        UserToken userToken = userTokenService.getByToken(token);
        int userId = userToken.getUserId();

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        //固定保存路径
        //判断上传文件的保存目录是否存在
        File targetFile = new File(PATH);
        if (!targetFile.exists() && !targetFile.isDirectory()) {
            System.out.println(PATH + "  目录不存在，需要创建");
            //创建目录
            targetFile.mkdirs();
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(PATH + File.separator + fileName);
            out.write(file.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        FileVo fileObject = new FileVo();
        fileObject.setUserId(userId);
        fileObject.setFileName(fileName);
        fileObject.setFilePath(PATH + fileName);
        fileObject.setCreateTime(new Date());

        int result = fileService.upload(fileObject);

        if (result > 0) {
            return new Response("");
        }

        return new Response(ResponseCode.OPERATION_ERROR.getStatus(), ResponseCode.OPERATION_ERROR.getMsg(), "");

    }

    @GetMapping("/download")
    @UserLoginToken
    public Response download(@RequestParam("token") String token, @RequestParam("fileName") String fileName, HttpServletRequest request, HttpServletResponse response) {
        //得到要下载的文件, linux为/  Windows为\\
        File file = new File(PATH + File.separator + fileName);
        //如果文件不存在
        if (!file.exists()) {
            return new Response(ResponseCode.OPERATION_ERROR.getStatus(), ResponseCode.OPERATION_ERROR.getMsg(), "");
        }
        //处理文件名
        String realname = fileName.substring(fileName.indexOf("_") + 1);
        try {
            //设置响应头，控制浏览器下载该文件
            response.setHeader("content-disposition", "attachment;filename=" + URLEncoder.encode(realname, "UTF-8"));
            //读取要下载的文件，保存到文件输入流
            FileInputStream in = null;

            in = new FileInputStream(PATH + File.separator + fileName);
            //创建输出流
            OutputStream out = response.getOutputStream();
            //创建缓冲区
            byte buffer[] = new byte[1024];
            int len = 0;
            //循环将输入流中的内容读取到缓冲区当中
            while ((len = in.read(buffer)) > 0) {
                //输出缓冲区的内容到浏览器，实现文件下载
                out.write(buffer, 0, len);
            }
            //关闭文件输入流
            in.close();
            //关闭输出流
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new Response(ResponseCode.OPERATION_ERROR.getStatus(), ResponseCode.OPERATION_ERROR.getMsg(), "");

    }
}
