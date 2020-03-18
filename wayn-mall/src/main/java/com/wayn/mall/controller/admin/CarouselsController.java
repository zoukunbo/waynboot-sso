package com.wayn.mall.controller.admin;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wayn.mall.base.BaseController;
import com.wayn.mall.entity.Carousels;
import com.wayn.mall.service.CarouselsService;
import com.wayn.mall.util.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@RequestMapping("admin/carousels")
public class CarouselsController extends BaseController {

    private static final String PREFIX = "admin/carousels";

    @Autowired
    private CarouselsService carouselsService;

    @GetMapping
    public String index(HttpServletRequest request) {
        request.setAttribute("path", "newbee_mall_carousel");

        return PREFIX + "/carousels";
    }

    /**
     * 列表
     */
    @GetMapping("/list")
    @ResponseBody
    public IPage list(Carousels carousels, HttpServletRequest request) {
        Page<Carousels> page = getPage(request);
        return carouselsService.selectPage(page, carousels);
    }

    /**
     * 列表
     */
    @PostMapping("/save")
    @ResponseBody
    public R save(@RequestBody Carousels carousels, HttpServletRequest request) {
        carouselsService.save(carousels);
        return R.success();
    }

    /**
     * 列表
     */
    @PostMapping("/update")
    @ResponseBody
    public R update(@RequestBody Carousels carousels, HttpServletRequest request) {
        carouselsService.updateById(carousels);
        return R.success();
    }

    /**
     * 详情
     */
    @GetMapping("/info/{id}")
    @ResponseBody
    public R Info(@PathVariable("id") Integer id, HttpServletRequest request) {
        return R.success().add("data", carouselsService.getById(id));
    }

    /**
     * 删除
     */
    @PostMapping("/delete")
    @ResponseBody
    public R delete(@RequestBody List<Integer> ids) {
        carouselsService.removeByIds(ids);
        return R.success();
    }
}
