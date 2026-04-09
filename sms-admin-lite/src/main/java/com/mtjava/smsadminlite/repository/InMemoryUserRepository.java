package com.mtjava.smsadminlite.repository;

import com.mtjava.smsadminlite.model.User;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用内存实现的仓库。
 *
 * 现在你还没开始学数据库，所以这里先用 ConcurrentHashMap 模拟数据表。
 * 后面当你接入 MyBatis/MySQL 时，可以把这个类替换成 Mapper。
 */
@Repository
public class InMemoryUserRepository implements UserRepository {

    private final ConcurrentHashMap<Long, User> userTable = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(0);

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>(userTable.values());
        users.sort(Comparator.comparing(User::getId));
        return users;
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return userTable.values()
                .stream()
                .filter(user -> user.getPhone().equals(phone))
                .findFirst();
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            user.setId(idGenerator.incrementAndGet());
        }
        userTable.put(user.getId(), user);
        return user;
    }
}
