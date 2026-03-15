package com.bidify.server.repository;

import java.util.ArrayList;

import com.bidify.server.model.User;

// giao tiếp với database về phần người dùng
public class UserRepository {
    private final ArrayList<User> users = new ArrayList<>();

    public boolean existsByUsername(String username){ // check nếu username đã tồn tại
        return users.stream().anyMatch(user -> user.getUsername().equals(username));
    }

    public User findByUsername(String username){ // tìm kiếm bằng username
        return users.stream()
                .filter(user -> user.getUsername().equals(username))
                .findFirst()
                .orElse(null);
    }

    public void save(User user){ // lưu người dùng
        users.add(user);
    }
}
