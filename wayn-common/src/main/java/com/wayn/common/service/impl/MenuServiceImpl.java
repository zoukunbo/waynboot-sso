package com.wayn.common.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wayn.common.dao.MenuDao;
import com.wayn.common.dao.RoleMenuDao;
import com.wayn.common.domain.Menu;
import com.wayn.common.domain.RoleMenu;
import com.wayn.common.domain.vo.MenuVO;
import com.wayn.common.domain.vo.Tree;
import com.wayn.common.exception.BusinessException;
import com.wayn.common.service.MenuService;
import com.wayn.common.util.TreeBuilderUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.*;

/**
 * <p>
 * 菜单表 服务实现类
 * </p>
 *
 * @author wayn
 * @since 2019-04-13
 */
@Service
public class MenuServiceImpl extends ServiceImpl<MenuDao, Menu> implements MenuService {

    @Autowired
    private MenuDao menuDao;

    @Autowired
    private RoleMenuDao roleMenuDao;

    @CacheEvict(value = "menuCache", allEntries = true)
    @Override
    public boolean save(Menu menu) {
        menuDao.insert(menu);
        return true;
    }

    @CacheEvict(value = "menuCache", allEntries = true)
    @Override
    public boolean update(Menu menu) {
        return updateById(menu);
    }

    @CacheEvict(value = "menuCache", allEntries = true)
    @Override
    public boolean remove(Long id) throws BusinessException {
        return deleteById(id);
    }


    @Override
    public List<String> selectMenuIdsByUid(String id) {
        return menuDao.selectMenuIdsByUid(id);
    }

    @Cacheable(value = "menuCache", key = "#root.method + '_' + #root.args[0]")
    @Override
    public List<String> selectResourceByUid(String id) {
        return menuDao.selectResourceByUid(id);
    }

    /**
     * 获取首页菜单
     */
    @Cacheable(value = "menuCache", key = "#root.method + '_' + #root.args[0]")
    @Override
    public List<Menu> selectTreeMenuByUserId(String id) {
        List<Menu> menus = roleMenuDao.selectRoleMenuIdsByUserId(id);
        List<Menu> treeMenus = selectTreeMenuByMenusAndPid(menus, 0L);
        return treeMenus;
    }

    /**
     * 将菜单列表封装成菜单树，不包含按钮
     *
     * @param menus
     * @param pid   顶级父id
     * @return
     */
    public List<Menu> selectTreeMenuByMenusAndPid(List<Menu> menus, Long pid) {
        List<Menu> returnList = new ArrayList<>();
        menus.forEach(menu -> {
            if (pid.equals(menu.getPid())) {
                // 只设置菜单类型为 目录1 或者菜单2 的记录
                if (menu.getType() < 2) {
                    menu.setChildren(selectTreeMenuByMenusAndPid(menus, menu.getId()));
                }
                returnList.add(menu);
            }

        });
        return returnList;
    }

    /**
     * 获取菜单树
     */
    @Cacheable(value = "menuCache", key = "#root.method + '_menuTree'")
    @Override
    public Tree<Menu> getTree() {
        List<Tree<Menu>> trees = new ArrayList<>();
        List<Menu> menus = menuDao.selectList(new QueryWrapper<Menu>().ne("type", 3));
        for (Menu menu : menus) {
            Tree<Menu> tree = new Tree<>();
            tree.setId(menu.getId().toString());
            tree.setParentId(menu.getPid().toString());
            tree.setText(menu.getMenuName());
            trees.add(tree);
        }
        return TreeBuilderUtil.build(trees);
    }

    /**
     * 获取菜单树，包含按钮，并根据用户id查询该用户是否包含此菜单，设置菜单是否选中
     *
     * @param id 用户id
     */
    @Cacheable(value = "menuCache", key = "#root.method + '_roleID_' + #root.args[0]")
    @Override
    public Tree<Menu> getTree(String id) {
        List<RoleMenu> list = roleMenuDao.selectList(new QueryWrapper<RoleMenu>().eq("roleId", id));
        List<Long> menuIds = new ArrayList<>();
        list.forEach(item -> menuIds.add(item.getMenuId()));
        // 去掉菜单的父菜单，jstree默认会勾选父菜单
        if (menuIds.size() > 0) {
            List<Menu> list2 = menuDao.selectBatchIds(menuIds);
            List<Long> temp = menuIds;
            for (Menu menu : list2) {
                if (temp.contains(menu.getPid())) {
                    menuIds.remove(menu.getPid());
                }
            }
        }
        List<Menu> menus = menuDao.selectList(new QueryWrapper<>());
        List<Tree<Menu>> trees = new ArrayList<>();
        menus.forEach(menu -> {
            Tree<Menu> tree = new Tree<>();
            tree.setId(menu.getId().toString());
            tree.setParentId(menu.getPid().toString());
            tree.setText(menu.getMenuName());
            Map<String, Object> state = new HashMap<>();
            Long menuId = menu.getId();
            // 设置选中状态
            if (menuIds.contains(menuId)) {
                state.put("selected", true);
            } else {
                state.put("selected", false);
            }
            tree.setState(state);
            trees.add(tree);
        });
        return TreeBuilderUtil.build(trees);
    }

    @CacheEvict(value = "menuCache", allEntries = true)
    @Override
    public boolean updateById(Menu entity) {
        return super.updateById(entity);
    }

    @CacheEvict(value = "menuCache", allEntries = true)
    public boolean deleteById(Serializable id) {
        return deleteById(id);
    }

    @Cacheable(value = "menuCache", key = "#root.method + '_' + #root.args[0]")
    @Override
    public List<Menu> list(MenuVO menu) {
        QueryWrapper<Menu> wrapper = new QueryWrapper<>();
        wrapper.like("menuName", menu.getMenuName());
        wrapper.in(StringUtils.isNotEmpty(menu.getType()), "type", Arrays.asList(menu.getType().split(",")));
        wrapper.orderByAsc("sort");
        List<Menu> menus = menuDao.selectList(wrapper);
        List<Menu> menusList = new ArrayList<>(menus);
        if (StringUtils.isNotEmpty(menu.getMenuName())) {
            selectChildren(menus, menusList);
        }
        return menusList;
    }

    /**
     * 根据pid查询子菜单
     *
     * @param menus
     * @param menusList
     * @return
     */
    public List<Menu> selectChildren(List<Menu> menus, List<Menu> menusList) {
        menus.forEach(item -> {
            if (item.getType() < 3) {
                menusList.addAll(selectChildren(menuDao.selectList(
                        new QueryWrapper<Menu>().eq("pid", item.getId()).orderByDesc("sort")), menusList));
            }
        });
        return menus;
    }

}
