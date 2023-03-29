package cn.allbs.mybatis;

import cn.allbs.mybatis.properties.MybatisProperties;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.config.GlobalConfig;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.ClassUtils;

import java.nio.charset.Charset;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 类 MybatisPlusMetaObjectHandler
 * </p>
 *
 * @author ChenQi
 * @since 2023/3/23 10:18
 */
@Slf4j
@RequiredArgsConstructor
public class MybatisPlusMetaObjectHandler implements MetaObjectHandler {

    private final MybatisProperties mybatisProperties;

    @Override
    public void insertFill(MetaObject metaObject) {
        log.debug("mybatis plus start insert fill ....");
        LocalDateTime now = LocalDateTime.now();

        // 审计字段自动填充
        fillValIfNullByName(mybatisProperties.getMetaCustom().getCreateTime(), now, metaObject, false);
        fillValIfNullByName(mybatisProperties.getMetaCustom().getUpdateTime(), now, metaObject, false);
        fillValIfNullByName(mybatisProperties.getMetaCustom().getCreateName(), getUserName(), metaObject, false);
        fillValIfNullByName(mybatisProperties.getMetaCustom().getUpdateName(), getUserName(), metaObject, false);

        // 删除标记自动填充
        fillValIfNullByName(mybatisProperties.getMetaCustom().getDelFlg(), new GlobalConfig.DbConfig().getLogicNotDeleteValue(), metaObject, false);
    }

    @Override
    public void updateFill(MetaObject metaObject) {
        fillValIfNullByName(mybatisProperties.getMetaCustom().getUpdateTime(), LocalDateTime.now(), metaObject, true);
        fillValIfNullByName(mybatisProperties.getMetaCustom().getUpdateName(), getUserName(), metaObject, true);
    }

    /**
     * 填充值，先判断是否有手动设置，优先手动设置的值，例如：job必须手动设置
     *
     * @param fieldName  属性名
     * @param fieldVal   属性值
     * @param metaObject MetaObject
     * @param isCover    是否覆盖原有值,避免更新操作手动入参
     */
    private static void fillValIfNullByName(String fieldName, Object fieldVal, MetaObject metaObject, boolean isCover) {
        // 0. 如果填充值为空
        if (fieldVal == null) {
            return;
        }
        // 1. 没有 get 方法
        if (!metaObject.hasSetter(fieldName)) {
            return;
        }
        // 2. 如果用户有手动设置的值
        Object userSetValue = metaObject.getValue(fieldName);
        String setValueStr = StrUtil.str(userSetValue, Charset.defaultCharset());
        if (StrUtil.isNotBlank(setValueStr) && !isCover) {
            return;
        }
        // 3. field 类型相同时设置
        Class<?> getterType = metaObject.getGetterType(fieldName);
        if (ClassUtils.isAssignableValue(getterType, fieldVal)) {
            metaObject.setValue(fieldName, fieldVal);
            return;
        }
        if (String.class.getName().equals(getterType.getName())) {
            metaObject.setValue(fieldName, fieldVal);
            return;
        }
        if (Integer.class.getName().equals(getterType.getName())) {
            metaObject.setValue(fieldName, Integer.valueOf(fieldVal.toString()));
        }
    }

    /**
     * 获取上下文中用户名
     *
     * @return 当前用户名
     */
    private String getUserName() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (Optional.ofNullable(authentication).isPresent()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof String) {
                return (String) principal;
            }
            if (principal instanceof UserDetails) {
                return ((UserDetails) principal).getUsername();
            }
        }
        return null;
    }
}
