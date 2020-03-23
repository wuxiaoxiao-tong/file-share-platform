package com.ncu.xzx.controller;

import com.alibaba.fastjson.JSONObject;
import com.ncu.xzx.model.*;
import com.ncu.xzx.service.FileService;
import com.ncu.xzx.service.UserLoadService;
import com.ncu.xzx.service.UserService;
import com.ncu.xzx.service.UserTokenService;
import com.ncu.xzx.utils.PassToken;
import com.ncu.xzx.utils.Response;
import com.ncu.xzx.utils.TokenUtil;
import com.ncu.xzx.utils.UserLoginToken;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    UserService userService;

    @Autowired
    UserTokenService userTokenService;

    @Autowired
    UserLoadService userLoadService;

    @Autowired
    FileService fileService;

    @Autowired
    RedisTemplate redisTemplate; //默认提供的用来操作对象的redis操作实例

    @Autowired
    StringRedisTemplate stringRedisTemplate; //默认提供的用来操作字符串的redis操作实例

    @PassToken
    @PostMapping("/login")
    public Response login(String userName, String password) {
        User user = userService.login(userName, password);
        String token = TokenUtil.getToken(user);
        userTokenService.addUserToken(user.getId(), token);
        return new Response(true);
    }

    @PassToken
    @PostMapping("/register")
    public Response register(String userName, String password) {
        User user = new User();
        user.setUserName(userName);
        user.setPassword(password);
        int result = userService.register(user);
        if (result > 0) {
            String token = TokenUtil.getToken(user);
            userTokenService.addUserToken(user.getId(), token);
            return new Response(true);
        }
        return new Response(false);
    }

    @PassToken
    @RequestMapping("/visit-count")
    public Response getVisitCount(){
        ValueOperations ops = stringRedisTemplate.opsForValue();
        Object visitCount = ops.get("visitCount");
        return new Response(visitCount);
    }

    @RequestMapping("/role")
    public Response getRole() {
        Role role = new Role();
        role.setName("admin");
        String[] roles = {"Home","Dashbord","Driver","Driver-index","Permission","PageUser","PageAdmin","Roles","Table","BaseTable","ComplexTable","Icons","Icons-index","Components","Sldie-yz","Upload","Carousel","Echarts","Sldie-chart","Dynamic-chart","Map-chart","Excel","Excel-out","Excel-in","Mutiheader-out","Error","Page404","Github","NavTest","Nav1","Nav2","Nav2-1","Nav2-2","Nav2-2-1","Nav2-2-2","*404"};
        role.setRoles(Arrays.asList(roles));
        role.setIntroduce("test");
        return new Response(role);
    }

    @GetMapping("/load-history")
    @UserLoginToken
    public Response getUserLoadHistory(HttpServletRequest request) {
        String token = request.getHeader("Authorization");
        UserToken userToken = userTokenService.getByToken(token);
        int userId = userToken.getUserId();
        List<FileVo> fileVoList= fileService.getByUserId(userId);
        List<FileDto> fileDtoList = fileService.FileVoToFileDto(fileVoList);
        return new Response(fileDtoList);
    }
}
