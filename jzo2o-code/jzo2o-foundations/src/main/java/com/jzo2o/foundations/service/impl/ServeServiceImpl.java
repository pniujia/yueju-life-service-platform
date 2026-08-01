package com.jzo2o.foundations.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import com.jzo2o.common.expcetions.ForbiddenOperationException;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.foundations.constants.RedisConstants;
import com.jzo2o.foundations.mapper.*;
import com.jzo2o.foundations.model.domain.*;
import com.jzo2o.foundations.model.dto.request.ServePageQueryReqDTO;
import com.jzo2o.foundations.model.dto.request.ServeUpsertReqDTO;
import com.jzo2o.foundations.model.dto.response.*;
import com.jzo2o.foundations.service.IServeService;
import com.jzo2o.mysql.utils.PageHelperUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 区域服务管理
 *
 * @author itcast
 * @create 2023/7/17 16:50
 **/
@Service
public class ServeServiceImpl extends ServiceImpl<ServeMapper, Serve> implements IServeService {
    @Autowired
    private ServeItemMapper serveItemMapper;
    @Autowired
    private RegionMapper regionMapper;

    @Override
    public PageResult<ServeResDTO> findByPage(ServePageQueryReqDTO servePageQueryReqDTO) {
        return PageHelperUtils.selectPage(servePageQueryReqDTO,
                () -> baseMapper.queryListByRegionId(servePageQueryReqDTO.getRegionId()));
    }

    @Override
    @Transactional
    public void batchAdd(List<ServeUpsertReqDTO> list) {
        for (ServeUpsertReqDTO serveUpsertReqDTO : list) {
            //1. 服务项目必须是启用状态的才能添加到区域
            ServeItem serveItem = serveItemMapper.selectById(serveUpsertReqDTO.getServeItemId());
            if (ObjectUtil.isEmpty(serveItem) || serveItem.getActiveStatus() != 2) {
                throw new ForbiddenOperationException("服务项不存在");
            }

            //2. 一个服务项目对于一个区域，只能添加一次
            Integer count = this.lambdaQuery()
                    .eq(Serve::getRegionId, serveUpsertReqDTO.getRegionId())
                    .eq(Serve::getServeItemId, serveUpsertReqDTO.getServeItemId())
                    .count();
            if (count > 0) {
                throw new ForbiddenOperationException("该服务项已经添加过了");
            }

            //3. 根据region_id 去region表查询城市编码
            Region region = regionMapper.selectById(serveUpsertReqDTO.getRegionId());
            if (ObjectUtil.isEmpty(region) || ObjectUtil.isEmpty(region.getCityCode())) {
                throw new ForbiddenOperationException("区域不存在");
            }

            //4. 新增
            Serve serve = BeanUtil.toBean(serveUpsertReqDTO, Serve.class);
            serve.setCityCode(region.getCityCode());
            this.save(serve);
        }
    }

    @Override
    public void deleteById(Long id) {
        //根据主键查询记录, 当状态不为草稿状态不能删除
        Serve serve = baseMapper.selectById(id);
        if (ObjectUtil.isNull(serve) || serve.getSaleStatus() != 0){
            throw new ForbiddenOperationException("删除失败, 当前区域服务不是草稿状态");
        }

        //根据主键删除
        baseMapper.deleteById(id);
    }

    @Override
    public void onSale(Long id) {
        //1) 区域服务当前非上架状态
        Serve serve = this.getById(id);
        if (ObjectUtil.isEmpty(serve) || serve.getSaleStatus() == 2) {
            throw new ForbiddenOperationException("该服务项已经上架");
        }

        //2) 服务项目是启用状态
        ServeItem serveItem = serveItemMapper.selectById(serve.getServeItemId());
        if (ObjectUtil.isEmpty(serveItem) || serveItem.getActiveStatus() != 2) {
            throw new ForbiddenOperationException("服务项不存在或者未启用");
        }

        //3) 修改服务的状态
        this.lambdaUpdate()
                .eq(Serve::getId, id)
                .set(Serve::getSaleStatus, 2)
                .update();

        //4）添加同步表数据
        addServeSync(id);
    }

    @Autowired
    private ServeTypeMapper serveTypeMapper;

    @Autowired
    private ServeSyncMapper serveSyncMapper;


    /**
     * 新增服务同步数据
     *
     * @param serveId 服务id
     */
    @Transactional
    public void addServeSync(Long serveId) {
        //服务信息
        Serve serve = baseMapper.selectById(serveId);
        //区域信息
        Region region = regionMapper.selectById(serve.getRegionId());
        //服务项信息
        ServeItem serveItem = serveItemMapper.selectById(serve.getServeItemId());
        //服务类型
        ServeType serveType = serveTypeMapper.selectById(serveItem.getServeTypeId());

        ServeSync serveSync = new ServeSync();
        serveSync.setServeTypeId(serveType.getId());
        serveSync.setServeTypeName(serveType.getName());
        serveSync.setServeTypeIcon(serveType.getServeTypeIcon());
        serveSync.setServeTypeImg(serveType.getImg());
        serveSync.setServeTypeSortNum(serveType.getSortNum());

        serveSync.setServeItemId(serveItem.getId());
        serveSync.setServeItemIcon(serveItem.getServeItemIcon());
        serveSync.setServeItemName(serveItem.getName());
        serveSync.setServeItemImg(serveItem.getImg());
        serveSync.setServeItemSortNum(serveItem.getSortNum());
        serveSync.setUnit(serveItem.getUnit());
        serveSync.setDetailImg(serveItem.getDetailImg());
        serveSync.setPrice(serve.getPrice());

        serveSync.setCityCode(region.getCityCode());
        serveSync.setId(serve.getId());
        serveSync.setIsHot(serve.getIsHot());
        serveSyncMapper.insert(serveSync);
    }

    @Override
    public void offSale(Long id) {
        //1. 查询区域服务的状态,如果是下架状态,则不能再次下架
        Serve serve = baseMapper.selectById(id);
        if (ObjectUtil.isNull(serve) || serve.getSaleStatus() != 2) {
            throw new ForbiddenOperationException("服务状态异常,无法下架");
        }

        //2. 更新下架状态
        serve.setSaleStatus(1);//下架
        baseMapper.updateById(serve);

        //3. 删除同步表数据
        serveSyncMapper.deleteById(id);
    }

    @Caching(
            cacheable = {
                    //返回数据为空，则缓存空值30分钟，这样可以避免缓存穿透
                    @Cacheable(value = RedisConstants.CacheName.SERVE_ICON, key ="#regionId" ,
                            unless ="#result.size() > 0",cacheManager = RedisConstants.CacheManager.THIRTY_MINUTES),

                    //返回值不为空，则永久缓存数据
                    @Cacheable(value = RedisConstants.CacheName.SERVE_ICON, key ="#regionId" ,
                            unless ="#result.size() == 0",cacheManager = RedisConstants.CacheManager.FOREVER)
            }
    )
    @Override
    public List<ServeCategoryResDTO> firstPageServeList(Long regionId) {
        //1 对区域进行校验
        Region region = regionMapper.selectById(regionId);
        if (ObjectUtil.isNull(region) || region.getActiveStatus() != 2) {
            return Collections.emptyList();
        }

        //2. 查询指定区域下上架的服务分类及项目信息
        List<ServeCategoryResDTO> list = baseMapper.findListByRegionId(regionId);
        if (CollUtil.isEmpty(list)) {
            return Collections.emptyList();
        }

        //3. 截取
        list = CollUtil.sub(list, 0, Math.min(list.size(), 2));//服务类型截取
        list.forEach(e ->
                //服务项目截取
                e.setServeResDTOList(CollUtil.sub(e.getServeResDTOList(), 0, Math.min(e.getServeResDTOList().size(), 4)))
        );
        return list;
    }

    @Caching(
            cacheable = {
                    //返回数据为空，则缓存空值30分钟，这样可以避免缓存穿透
                    @Cacheable(value = RedisConstants.CacheName.HOT_SERVE,key ="#regionId" ,
                            unless ="#result.size() > 0",cacheManager = RedisConstants.CacheManager.THIRTY_MINUTES),

                    //返回值不为空，则永久缓存数据
                    @Cacheable(value = RedisConstants.CacheName.HOT_SERVE,key ="#regionId" ,
                            unless ="#result.size() == 0",cacheManager = RedisConstants.CacheManager.FOREVER)
            }
    )
    @Override
    public List<ServeAggregationSimpleResDTO> hotServeList(Long regionId) {
        //1 对区域进行校验
        Region region = regionMapper.selectById(regionId);
        if (ObjectUtil.isNull(region) || region.getActiveStatus() != 2) {
            return Collections.emptyList();
        }

        //2 查询指定区域下上架且热门的服务项目信息
        return baseMapper.findServeListByRegionId(regionId);
    }

    @Override
    public List<ServeAggregationTypeSimpleResDTO> serveTypeList(Long regionId) {
        //1 对区域进行校验
        Region region = regionMapper.selectById(regionId);
        if (ObjectUtil.isNull(region) || region.getActiveStatus() != 2) {
            return Collections.emptyList();
        }

        //2 查询当前区域下上架服务对应的分类
        return baseMapper.findServeTypeListByRegionId(regionId);
    }

    @Autowired
    private RestHighLevelClient client;

    @Override
    public List<ServeSimpleResDTO> search(String cityCode, String keyword, Long serveTypeId) {
        //1. 创建请求对象
        SearchRequest request = new SearchRequest("serve_aggregation");

        //2. 封装请求参数
        BoolQueryBuilder boolQuery = QueryBuilders.boolQuery();
        //城市编码
        boolQuery.must(QueryBuilders.termQuery("city_code", cityCode));
        //服务类型id
        if (serveTypeId != null) {
            boolQuery.must(QueryBuilders.termQuery("serve_type_id", serveTypeId));
        }
        //关键词
        if (StrUtil.isNotEmpty(keyword)) {
            boolQuery.must(QueryBuilders.multiMatchQuery(keyword, "serve_item_name", "serve_type_name"));
        }
        request.source().query(boolQuery);//查询
        request.source().sort("serve_item_sort_num", SortOrder.ASC);//排序
        request.source().from(0).size(100);//处理10个限制

        //3. 执行请求
        SearchResponse response = null;
        try {
            response = client.search(request, RequestOptions.DEFAULT);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //4. 处理返回结果   List<ServeSimpleResDTO>
        if (response.getHits().getTotalHits().value == 0) {
            return List.of();
        }
        return Arrays.stream(response.getHits().getHits())
                .map(e -> JSONUtil.toBean(e.getSourceAsString(), ServeSimpleResDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    public ServeAggregationSimpleResDTO findById(Long id) {
        //1. 根据服务id去serve表中查询服务信息(内含服务项目id)
        Serve serve = baseMapper.selectById(id);
        if (ObjectUtil.isNull(serve)){
            throw new ForbiddenOperationException("服务不存在");
        }

        //2. 根据服务项目id去serve_item表中查询服务项目信息
        ServeItem serveItem = serveItemMapper.selectById(serve.getServeItemId());
        if (ObjectUtil.isNull(serveItem)){
            throw new ForbiddenOperationException("服务项目不存在");
        }

        //3. 将两部分内容组装成返回结果
        ServeAggregationSimpleResDTO dto = BeanUtil.copyProperties(serve, ServeAggregationSimpleResDTO.class);
        dto.setServeItemName(serveItem.getName());
        dto.setServeItemImg(serveItem.getImg());
        dto.setDetailImg(serveItem.getDetailImg());
        dto.setUnit(serveItem.getUnit());

        return dto;
    }

    @Override
    public ServeAggregationResDTO findServeDetailById(Long id) {
        return baseMapper.findServeDetailById(id);
    }
}