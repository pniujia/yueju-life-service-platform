package com.jzo2o.foundations.controller.operation;


import com.jzo2o.common.model.PageResult;
import com.jzo2o.common.model.Result;
import com.jzo2o.foundations.model.domain.Serve;
import com.jzo2o.foundations.model.dto.request.RegionPageQueryReqDTO;
import com.jzo2o.foundations.model.dto.request.ServePageQueryReqDTO;
import com.jzo2o.foundations.model.dto.request.ServeUpsertReqDTO;
import com.jzo2o.foundations.model.dto.response.RegionResDTO;
import com.jzo2o.foundations.model.dto.response.ServeResDTO;
import com.jzo2o.foundations.service.IServeService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 运营端 - 区域服务相关接口
 */
@RestController("operationServeController")
@RequestMapping("/operation/serve")
@Api(tags = "运营端 - 区域服务相关接口")
public class ServeController {

    @Autowired
    private IServeService serveService;

    @GetMapping("/page")
    @ApiOperation("区域服务分页查询")
    public PageResult<ServeResDTO> page(ServePageQueryReqDTO servePageQueryReqDTO) {
        return serveService.findByPage(servePageQueryReqDTO);
    }

    @PostMapping("/batch")
    @ApiOperation("区域服务新增")
    public Result batchAdd(@RequestBody List<ServeUpsertReqDTO> list) {
        serveService.batchAdd(list);
        return Result.ok();
    }

    @PutMapping("/{id}")
    @ApiOperation("区域服务修改价格")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "区域服务id", required = true, dataTypeClass = Long.class),
            @ApiImplicitParam(name = "price", value = "区域服务修改之后的价格", required = true, dataTypeClass = BigDecimal.class)
    })
    public void update(@PathVariable("id") Long id, BigDecimal price) {
        Serve serve = new Serve();
        serve.setId(id);
        serve.setPrice(price);
        serveService.updateById(serve);//mp动态SQL update serve set price = ? where id = ?
    }

    @DeleteMapping("/{id}")
    @ApiOperation("区域服务删除")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "区域服务id", required = true, dataTypeClass = Long.class)
    })
    public void delete(@PathVariable("id") Long id) {
        serveService.deleteById(id);
    }

    @PutMapping("/onSale/{id}")
    @ApiOperation("区域服务上架")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "区域服务id", required = true, dataTypeClass = Long.class)
    })
    public void onSale(@PathVariable("id") Long id) {
        serveService.onSale(id);
    }

    @PutMapping("/offSale/{id}")
    @ApiOperation("区域服务下架")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "区域服务id", required = true, dataTypeClass = Long.class),
    })
    public void offSale(@PathVariable("id") Long id) {
        serveService.offSale(id);
    }

    @PutMapping("/onHot/{id}")
    @ApiOperation("设置热门")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "区域服务id", required = true, dataTypeClass = Long.class),
    })
    public void onHot(@PathVariable("id") Long id) {
        Serve serve = new Serve();
        serve.setId(id);
        serve.setHotTimeStamp(new Date().getTime());//上热门的时间
        serve.setIsHot(1);//热门
        serveService.updateById(serve);
    }


    @PutMapping("/offHot/{id}")
    @ApiOperation("取消热门")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "区域服务id", required = true, dataTypeClass = Long.class),
    })
    public void offHot(@PathVariable("id") Long id) {
        Serve serve = new Serve();
        serve.setId(id);
        serve.setIsHot(0);//非热门
        serveService.updateById(serve);
    }
}