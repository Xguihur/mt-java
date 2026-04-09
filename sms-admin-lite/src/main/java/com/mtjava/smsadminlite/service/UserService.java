package com.mtjava.smsadminlite.service;

import com.mtjava.smsadminlite.dto.CreateUserRequest;
import com.mtjava.smsadminlite.model.User;

import java.util.List;

/**
 * 业务层接口。
 *
 * Service 层的重点不是“存数据”，而是“组织业务规则”。
 * 比如：新增用户前先检查手机号是否重复，这类规则应该放在这里。
 */
public interface UserService {

    List<User> listUsers();

    User createUser(CreateUserRequest request);
}
