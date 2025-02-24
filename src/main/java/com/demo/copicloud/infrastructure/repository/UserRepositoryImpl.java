package com.demo.copicloud.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.copicloud.domain.user.entity.User;
import com.demo.copicloud.domain.user.repository.UserRepository;
import com.demo.copicloud.infrastructure.mapper.UserMapper;
import org.springframework.stereotype.Service;

@Service
public class UserRepositoryImpl extends ServiceImpl<UserMapper, User> implements UserRepository {
}
