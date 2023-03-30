package cn.allbs.mybatis;

import cn.allbs.mybatis.datascope.DataPmsHandler;
import cn.allbs.mybatis.datascope.DataPmsInterceptor;
import cn.allbs.mybatis.datascope.DefaultPmsHandler;
import cn.allbs.mybatis.filter.DruidSqlLogFilter;
import cn.allbs.mybatis.properties.MybatisProperties;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.sql.DataSource;
import java.util.List;

/**
 * 功能:
 *
 * @author ChenQi
 * @version 1.0
 * @since 2021/3/4 16:41
 */
@Configuration
@ConditionalOnBean(DataSource.class)
@AutoConfigureAfter(DataSourceAutoConfiguration.class)
@EnableConfigurationProperties(MybatisProperties.class)
public class MybatisPlusConfig implements WebMvcConfigurer {

    /**
     * mybatis plus 配置分页
     */
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(MybatisProperties properties, DataPmsHandler dataPmsHandler) {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        if (properties.isDataPms()) {
            // 数据权限
            DataPmsInterceptor dataPermissionInterceptor = new DataPmsInterceptor(dataPmsHandler);
            interceptor.addInnerInterceptor(dataPermissionInterceptor);
        }
        // 分页支持
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        return interceptor;
    }

    @Bean
    @ConditionalOnMissingBean(DataPmsHandler.class)
    @ConditionalOnProperty(prefix = "mybatis-plus", name = "data-pms", havingValue = "true")
    public DataPmsHandler dataPmsHandler() {
        return new DefaultPmsHandler();
    }

    /**
     * 增加请求参数解析器，对请求中的参数注入SQL 检查
     *
     * @param resolverList
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolverList) {
        resolverList.add(new SqlFilterArgumentResolver());
    }

    @Bean
    public DruidSqlLogFilter sqlLogFilter(MybatisProperties properties) {
        return new DruidSqlLogFilter(properties);
    }

    /**
     * 审计字段自动填充
     *
     * @return {@link MetaObjectHandler}
     */
    @Bean
    public MybatisPlusMetaObjectHandler mybatisPlusMetaObjectHandler(MybatisProperties properties) {
        return new MybatisPlusMetaObjectHandler(properties);
    }
}
