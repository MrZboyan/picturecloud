package com.demo.copicloud.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.copicloud.domain.space.entity.SpaceUser;
import com.demo.copicloud.domain.space.repository.SpaceUserRepository;
import com.demo.copicloud.infrastructure.mapper.SpaceUserMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceUserRepositoryImpl extends ServiceImpl<SpaceUserMapper, SpaceUser> implements SpaceUserRepository {
}
