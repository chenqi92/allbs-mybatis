## 使用
### 添加依赖
{% tabs tag-hide %}
<!-- tab maven -->

```xml
<dependency>
  <groupId>cn.allbs</groupId>
  <artifactId>allbs-mybatis</artifactId>
  <version>2.0.2</version>
</dependency>
```

<!-- endtab -->

<!-- tab Gradle -->

```
implementation 'cn.allbs:allbs-mybatis:2.0.2'
```

<!-- endtab -->

<!-- tab Kotlin -->

```
implementation("cn.allbs:allbs-mybatis:2.0.2")
```
<!-- endtab -->
{% endtabs %}

### 配置示例

```yaml
mybatis-plus:
  mapper-locations: classpath*:mapper/*/*.xml
  global-config:
    banner: false
    db-config:
      id-type: auto
      logic-delete-value: 1
      logic-not-delete-value: 0
# 此处注释是为了使用该包中自定义打印的sql日志，如果放开会打印两次sql日志，只是格式不同
#  configuration:
#    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  type-handlers-package: com.allbs.allbsjwt.config.handler
  # 这边是为了打印自定义格式的sql日志，比如参数自动填充，结果按照类似表格输出。如果不需要或者生成环境此处设为false
  show-sql: true
  # 此处是为了审计自动填充，因为不同表中字段不一样所以逻辑删除、创建者、创建时间、更新者、更新时间字段自定义。有些系统创建者和更新者使用的是id则与本系统不兼容，此处默认的是插入spring security中的用户名
   meta-custom:
    del-flg: delFlag
    create-name: createId
    update-name: updateId
  # 设置中文字符占其他字符的比例，取巧尽量让打印出来的格式工整些，比如某些字体占两个英文的宽度就设置为2
  chine-rate: 1.5
  # 是否开启权限过滤字段
  data-pms: true
```

#### 日志打印示例

![image-20230327111534230](https://nas.allbs.cn:9006/cloudpic/2023/03/d7858814b394d0c4f4793f6cc0f1171f.png)

![image-20230327111549175](https://nas.allbs.cn:9006/cloudpic/2023/03/936d0f475f49c3f453be9ec73159fa79.png)

#### 审计字段自动插入

![image-20230327111732400](https://nas.allbs.cn:9006/cloudpic/2023/03/4878b68ea6a97f484ed4622a1c354143.png)

#### 权限过滤

##### 开启

mybatis-plus.data-pms 设置为`true`,看上方配置示例最后一个配置

##### 使用说明

实现`DataPmsHandler`后写详细的逻辑即可，比如:

`@ScopeField`是用于跟表关联的实体类上的注解，用于标记改表中权限过滤的字段是哪个，以下类举例：下文中`DEFAULT_FILTER_FIELD`默认的是`ent_id`指数据库表中以该字段作为区分，如果有张表突然设置的是`unit_id`而不是`ent_id`则在对应的实体上设置`@ScopeField("unit_id")`

```java
import cn.allbs.allbsjwt.config.utils.SecurityUtils;
import cn.allbs.allbsjwt.config.vo.SysUser;
import cn.allbs.common.constant.StringPool;
import cn.allbs.mybatis.datascope.DataPmsHandler;
import cn.allbs.mybatis.datascope.ScopeField;
import cn.allbs.mybatis.utils.PluginUtils;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

/**
 * 类 CustomPermissionHandler
 *
 * @author ChenQi
 * @date 2023/3/28
 */
@Slf4j
@Component
public class CustomPermissionHandler implements DataPmsHandler {

    private final static String DEFAULT_FILTER_FIELD = "ent_id";

    /**
     * 获取数据权限 SQL 片段
     *
     * @param where             待执行 SQL Where 条件表达式
     * @param mappedStatementId Mybatis MappedStatement Id 根据该参数可以判断具体执行方法
     * @return JSqlParser 条件表达式
     */
    @Override
    public Expression getSqlSegment(final Table table, Expression where, String mappedStatementId) {

        SysUser sysUser = SecurityUtils.getUser();
        // 如果非权限用户则不往下执行，执行原sql
        if (sysUser == null) {
            return where;
        }
        // 在有权限的情况下查询用户所关联的企业列表
        Set<Long> permissionEntList = sysUser.getEntIdList();
//        if (permissionEntList.size() == 0) {
//            return where;
//        }
        TableInfo tableInfo = TableInfoHelper.getTableInfo(table.getName());
        String fieldName = Optional.ofNullable(tableInfo.getEntityType().getAnnotation(ScopeField.class)).map(ScopeField::value).orElse(DEFAULT_FILTER_FIELD);
        String finalFieldName = Optional.of(table).map(Table::getAlias).map(a -> a.getName() + StringPool.DOT + fieldName).orElse(fieldName);

        if (permissionEntList.size() > 1) {
            // 把集合转变为 JSQLParser需要的元素列表
            InExpression inExpression = new InExpression(new Column(finalFieldName), PluginUtils.getItemList(permissionEntList));

            // 组装sql
            return where == null ? inExpression : new AndExpression(where, inExpression);
        }
        // 设置where
        EqualsTo equalsTo = new EqualsTo();
        equalsTo.setLeftExpression(new Column(finalFieldName));
        equalsTo.setRightExpression(new LongValue(permissionEntList.stream().findFirst().orElse(0L)));
        return where == null ? equalsTo : new AndExpression(where, equalsTo);
    }
}
```

##### 效果

![image-20230330102020985](https://nas.allbs.cn:9006/cloudpic/2023/03/f855ae259b5a422c3d2e110e01205efe.png)

![image-20230330102109077](https://nas.allbs.cn:9006/cloudpic/2023/03/5547c31afa2a0d0f482bfd2efcc25c95.png)

##### 忽略权限拦截的方法

- 自定义sql情况下忽略:

在对应的dao指定方法上添加注解`@InterceptorIgnore`

- 使用mybatis plus 内置sdk的情况下忽略:

dao继承的BaseMapper修改为`PmsMapper`

![image-20230330104127313](https://nas.allbs.cn:9006/cloudpic/2023/03/02b3c178fffface3b01bbfb61b60d3a2.png)

- 指定表所有数据都不经过过滤

  在对应的dao上添加注解`@InterceptorIgnore`

#### 源码

[github](https://github.com/chenqi92/allbs-mybatis)
