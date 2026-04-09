package com.mtjava.smsadminlite.repository;

import com.mtjava.smsadminlite.model.User;

import java.util.List;
import java.util.Optional;

/**
 * 数据访问层接口。
 *
 * 在真实项目里，这一层往往会由 Mapper/DAO 承担，
 * 负责和数据库交互。这里先抽象成接口，便于你理解“面向接口编程”。
 */
public interface UserRepository {

    List<User> findAll();

    Optional<User> findByPhone(String phone);

    User save(User user);
}
