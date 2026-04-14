package com.mtjava.smsadminlite.mapper;

import com.mtjava.smsadminlite.model.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper。
 *
 * @Mapper 让 MyBatis 自动生成这个接口的实现类并注册为 Spring Bean。
 * 具体 SQL 写在 resources/mapper/UserMapper.xml 里。
 */
@Mapper
public interface UserMapper {

    List<User> selectAll();

    User selectById(@Param("id") Long id);

    User selectByPhone(@Param("phone") String phone);

    /** insert 完成后，MyBatis 会把数据库生成的自增 id 回填到 user.id 字段。 */
    void insert(User user);

    long count();
}
