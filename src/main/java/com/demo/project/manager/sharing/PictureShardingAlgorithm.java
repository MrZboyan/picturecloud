package com.demo.project.manager.sharing;

import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;

@Component
public class PictureShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    /**
     * description: 自定义分片算法
     *
     * @param collection 可用的目标表名集合
     * @param preciseShardingValue 分片值（包含逻辑表名和实际分片键值）
     * @return 目标表名
     **/
    @Override
    public String doSharding(Collection<String> collection, PreciseShardingValue<Long> preciseShardingValue) {
        Long spaceId = preciseShardingValue.getValue();
        String logicTableName = preciseShardingValue.getLogicTableName();
        if (spaceId == null) {
            return logicTableName;
        }
        String tableName = "picture_" + spaceId;
        if (collection.contains(tableName)) {
            return tableName;
        } else {
            return logicTableName;
        }
    }

    /**
     * description: 范围分片算法
     *
     * @param collection 可用的目标表名集合
     * @param rangeShardingValue 分片范围值
     * @return java.util.Collection<java.lang.String>
     **/
    @Override
    public Collection<String> doSharding(Collection<String> collection, RangeShardingValue<Long> rangeShardingValue) {
        return List.of();
    }

}
