package com.jzo2o.customer.controller.inner;

import cn.hutool.core.bean.BeanUtil;
import com.jzo2o.api.customer.dto.response.AddressBookResDTO;
import com.jzo2o.customer.model.domain.AddressBook;
import com.jzo2o.customer.service.IAddressBookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;

//内部接口 - 地址薄相关接口
@RestController
@RequestMapping("inner/address-book")
public class InnerAddressBookController {
    @Resource
    private IAddressBookService addressBookService;

    @GetMapping("/{id}")
    public AddressBookResDTO detail(@PathVariable("id") Long id) {
        AddressBook addressBook = addressBookService.getById(id);
        return BeanUtil.toBean(addressBook, AddressBookResDTO.class);
    }
}