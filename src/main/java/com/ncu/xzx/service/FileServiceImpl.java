package com.ncu.xzx.service;

import com.ncu.xzx.mapper.FileMapper;
import com.ncu.xzx.mapper.UserMapper;
import com.ncu.xzx.model.FileVo;
import com.ncu.xzx.model.FileDo;
import com.ncu.xzx.model.User;
import com.ncu.xzx.utils.LoginContextUtil;
import com.ncu.xzx.utils.Response;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Service
public class FileServiceImpl implements FileService{

    @Autowired
    FileMapper fileMapper;
    @Autowired
    UserMapper userMapper;

    @Autowired
    UserLoadService userLoadService;

    @Autowired
    UserService userService;

    @Autowired
    RedisTemplate redisTemplate;

    public static String FILE_PATH = "D:\\fileupload";

    public static String PAPER_PATH = "D:\\paperupload";


    @Override
    public int upload(MultipartFile file, int userId) {

        String contentType = file.getContentType();
        String fileName = file.getOriginalFilename();
        //判断上传文件的保存目录是否存在
        File targetFile = new File(FILE_PATH);
        if (!targetFile.exists() && !targetFile.isDirectory()) {
            System.out.println(FILE_PATH + "  目录不存在，需要创建");
            //创建目录
            targetFile.mkdirs();
        }
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(FILE_PATH + File.separator + fileName);
            out.write(file.getBytes());
            out.flush();
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        FileDo fileObject = new FileDo();
        fileObject.setUserId(userId);
        fileObject.setFileName(fileName);
        fileObject.setFilePath(FILE_PATH + fileName);
        int result = fileMapper.uploadFile(fileObject);

        if (result > 0) {
            userLoadService.insertOrUpdateUserLoad(userId, "upload");
            ListOperations listOperations = redisTemplate.opsForList();
            User user = userService.getUserById(userId);
            String uploadRemind = user.getUserName() + "上传了" + fileObject.getFileName();
            try {
                listOperations.leftPush("remindList", uploadRemind);
            }catch (Exception e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    @Override
    public int download(FileDo file) {
        return fileMapper.downloadFile(file);
    }

    @Override
    public Response verify(MultipartFile file) {
        FileInputStream fileInputStream = null;
        DigestInputStream digestInputStream = null;
        try {
            Security.addProvider(new BouncyCastleProvider());
            // 获取上传文件的md5摘要
            String fileName = file.getOriginalFilename();
            System.out.println("fileName   " + fileName);
            byte[] bytes = file.getBytes();
            MessageDigest messageDigest = MessageDigest.getInstance("SM3", "BC");
            messageDigest.update(bytes);
            byte[] userDigest = messageDigest.digest();
            messageDigest.reset();
            // 获取上传文件对应的本地文件的md5摘要
            String pathName = FILE_PATH + File.separator + fileName;
            System.out.println("pathName  " + pathName);
            File localFile = new File(pathName);
            if (!localFile.exists()) {
                return Response.failed("该文件在服务器中没有对应文件");
            }
            System.out.println("localFileLength  " + localFile.length());
            fileInputStream = new FileInputStream(localFile);
            byte[] localBytes = new byte[(int)localFile.length()];
            // 生成摘要
            digestInputStream = new DigestInputStream(fileInputStream, messageDigest);
            while (digestInputStream.read(localBytes) > 0);
            messageDigest= digestInputStream.getMessageDigest();
            byte[] localDigest = messageDigest.digest();
            // 校验两个摘要是否相等
            boolean verifyResult = MessageDigest.isEqual(userDigest, localDigest);
            digestInputStream.close();
            fileInputStream.close();
            return Response.ok(verifyResult);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.failed("校验失败");
        } finally {
            try {
                if (digestInputStream != null) {
                    digestInputStream.close();
                }
                if (fileInputStream != null) {
                    fileInputStream.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<FileDo> getFileList(int offset, int pageSize) {
        return fileMapper.getFilesByPage(offset, pageSize);
    }

    @Override
    public List<FileDo> getByFileName(String fileName) {
        return fileMapper.getByFileName(fileName);
    }

    @Override
    public List<FileDo> getByUserId(int userId) {
        return fileMapper.getByUserId(userId);
    }

    @Override
    public List<FileVo> FileDoToFileVo(List<FileDo> fileDoList) {
        List<FileVo> fileVoList = new ArrayList<>();
        fileDoList.forEach(FileDo -> {
            FileVo fileVo = new FileVo();
            BeanUtils.copyProperties(FileDo, fileVo);
            // 转换时间为字符串
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String createTime = format.format(FileDo.getCreateTime());
            fileVo.setCreateTime(createTime);
            User user = userMapper.getUserById(FileDo.getUserId());
            if (user != null) {
                fileVo.setUserName(user.getUserName());
            }
            fileVoList.add(fileVo);
        });
        return fileVoList;
    }

    @Override
    public int countAllFiles() {
        return fileMapper.countAllFiles();
    }
}
