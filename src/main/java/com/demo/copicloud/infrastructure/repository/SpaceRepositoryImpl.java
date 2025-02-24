package com.demo.copicloud.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.copicloud.domain.space.entity.Space;
import com.demo.copicloud.domain.space.repository.SpaceRepository;
import com.demo.copicloud.infrastructure.mapper.SpaceMapper;
import org.springframework.stereotype.Service;

@Service
public class SpaceRepositoryImpl extends ServiceImpl<SpaceMapper, Space> implements SpaceRepository {
}
