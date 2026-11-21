package com.game.app.controller;

import com.game.common.response.ApiResult;
import com.game.common.security.LoginUser;
import com.game.common.user.dto.UserDTO;
import com.game.common.user.dto.UserLoginDTO;
import com.game.common.user.service.UserService;
import com.game.common.user.vo.LoginVO;
import com.game.common.user.vo.UserVO;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 客户端用户接口（注册 / 登录 / 用户信息）。
 * 除注册、登录外都需要携带 Authorization: Bearer &lt;token&gt;。
 */
@RequestMapping("/user")
@RestController
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 注册用户
     * POST /user/register
     *
     * @return 新用户主键 ID
     */
    @PostMapping("/register")
    public ApiResult<Long> register(@Valid @RequestBody UserDTO dto) {
        return ApiResult.success(userService.register(dto));
    }

    /**
     * 登录
     * POST /user/login
     *
     * @return JWT Token + 基础用户信息
     */
    @PostMapping("/login")
    public ApiResult<LoginVO> login(@Valid @RequestBody UserLoginDTO dto) {
        return ApiResult.success(userService.login(dto));
    }

    /**
     * 查询当前登录用户信息
     * GET /user/info
     *
     * @param loginUser 由 JwtAuthenticationFilter 解析 Token 后放入 SecurityContext 的当前用户
     */
    @GetMapping("/info")
    public ApiResult<UserVO> info(@AuthenticationPrincipal LoginUser loginUser) {
        return ApiResult.success(userService.getInfo(loginUser.getId()));
    }
}
