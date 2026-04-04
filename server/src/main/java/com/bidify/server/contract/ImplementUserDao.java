package com.bidify.server.contract;

import com.bidify.server.model.User;

public interface ImplementUserDao {
    boolean existsByUsername(String username);
    User findByUsername(String username);
    boolean create(User user);
    void save(User client);
}
