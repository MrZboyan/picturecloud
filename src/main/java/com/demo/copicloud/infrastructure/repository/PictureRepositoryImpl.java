package com.demo.copicloud.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.demo.copicloud.domain.picture.entity.Picture;
import com.demo.copicloud.domain.picture.repository.PictureRepository;
import com.demo.copicloud.infrastructure.mapper.PictureMapper;
import org.springframework.stereotype.Service;

@Service
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {
}
