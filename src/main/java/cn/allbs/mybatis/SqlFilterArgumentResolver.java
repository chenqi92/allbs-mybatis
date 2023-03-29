package cn.allbs.mybatis;

import cn.allbs.common.utils.StringUtil;
import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 功能: mybatis plus sql 注入问题
 *
 * @author ChenQi
 * @version 1.0
 * @since 2021/3/4 16:46
 */
@Slf4j
public class SqlFilterArgumentResolver implements HandlerMethodArgumentResolver {

    private final static String[] KEYWORDS = {"master", "truncate", "insert", "select", "delete", "update", "declare", "alter", "drop", "sleep"};

    /**
     * 判断Controller是否包含page 参数
     *
     * @param parameter 参数
     * @return 是否过滤
     */
    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.getParameterType().equals(Page.class);
    }

    /**
     * @param parameter     入参集合
     * @param mavContainer  model 和 view
     * @param webRequest    web相关
     * @param binderFactory 入参解析
     */
    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);

        String[] ascs = request.getParameterValues("ascs");
        String[] descs = request.getParameterValues("descs");
        String current = request.getParameter("current");
        String size = request.getParameter("size");

        Page page = new Page();
        if (StringUtil.isNotBlank(current)) {
            page.setCurrent(Long.parseLong(current));
        }

        if (StringUtil.isNotBlank(size)) {
            page.setSize(Long.parseLong(size));
        }

        List<OrderItem> orderItemList = new ArrayList<>();
        Optional.ofNullable(ascs).ifPresent(s -> orderItemList.addAll(Arrays.stream(s).filter(sqlInjectPredicate()).map(OrderItem::asc).collect(Collectors.toList())));
        Optional.ofNullable(descs).ifPresent(s -> orderItemList.addAll(Arrays.stream(s).filter(sqlInjectPredicate()).map(OrderItem::desc).collect(Collectors.toList())));
        page.addOrder(orderItemList);

        return page;
    }

    /**
     * 判断用户输入里面有没有关键字
     *
     * @return Predicate
     */
    private Predicate<String> sqlInjectPredicate() {
        return sql -> {
            for (String keyword : KEYWORDS) {
                if (StringUtil.containsIgnoreCase(sql, keyword)) {
                    return false;
                }
            }
            return true;
        };
    }

}
