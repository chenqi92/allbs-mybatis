package cn.allbs.mybatis.datascope;

import java.lang.annotation.*;

/**
 * 注解 ScopeField
 *
 * @author ChenQi
 * @date 2023/3/28
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.ANNOTATION_TYPE})
public @interface ScopeField {

    /**
     * 当前表中需要过滤的字段名（该值可无）
     */
    String value() default "";
}
