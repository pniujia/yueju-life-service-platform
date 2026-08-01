package com.jzo2o.foundations.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.jzo2o.api.foundations.dto.response.RegionSimpleResDTO;
import com.jzo2o.api.foundations.dto.response.ServeAggregationResDTO;
import com.jzo2o.common.model.PageResult;
import com.jzo2o.foundations.model.domain.Region;
import com.jzo2o.foundations.model.domain.Serve;
import com.jzo2o.foundations.model.dto.request.RegionPageQueryReqDTO;
import com.jzo2o.foundations.model.dto.request.RegionUpsertReqDTO;
import com.jzo2o.foundations.model.dto.request.ServePageQueryReqDTO;
import com.jzo2o.foundations.model.dto.request.ServeUpsertReqDTO;
import com.jzo2o.foundations.model.dto.response.*;

import java.util.List;

/**
 * 区域服务管理
 *
 * @author itcast
 * @create 2023/7/17 16:49
 **/
public interface IServeService extends IService<Serve> {

    /**
     * 分页查询
     *
     * @param servePageQueryReqDTO 查询参数
     * @return 服务信息分页结果
     */
    PageResult<ServeResDTO> findByPage(ServePageQueryReqDTO servePageQueryReqDTO);
    /**
     * 区域服务新增
     *
     * @param list 区域服务集合
     */
    void batchAdd(List<ServeUpsertReqDTO> list);

    /**
     * 区域服务删除
     *
     * @param id 区域服务id
     */
    void deleteById(Long id);

    /**
     * 区域服务上架
     *
     * @param id 区域服务id
     */
    void onSale(Long id);

    /**
     * 区域服务下架
     *
     * @param id 区域服务id
     */
    void offSale(Long id);

    /**
     * 查询指定区域下上架的服务分类及项目信息
     *
     * @param regionId 区域id
     * @return 服务分类及项目信息
     */
    List<ServeCategoryResDTO> firstPageServeList(Long regionId);

    /**
     * 查询指定区域下上架且热门的服务项目信息
     *
     * @param regionId 区域id
     * @return 服务项目信息
     */
    List<ServeAggregationSimpleResDTO> hotServeList(Long regionId);

    /**
     * 查询当前区域下上架服务对应的分类
     *
     * @param regionId 区域id
     * @return 当前区域下上架服务对应的分类
     */
    List<ServeAggregationTypeSimpleResDTO> serveTypeList(Long regionId);

    /**
     * 服务搜索
     *
     * @param cityCode    城市编码
     * @param keyword     关键词
     * @param serveTypeId 服务类型id
     * @return 服务项目信息
     */
    List<ServeSimpleResDTO> search(String cityCode, String keyword, Long serveTypeId);

    /**
     * 查询服务详情
     *
     * @param id 服务id
     * @return 服务详情信息
     */
    ServeAggregationSimpleResDTO findById(Long id);

    /**
     * 根据ID查询服务详情
     *
     * @param id 服务id
     * @return 服务详情
     */
    ServeAggregationResDTO findServeDetailById(Long id);
}