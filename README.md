## 依赖jar包
| 引入包             | 版本      |
| ------------------ |---------|
| jdk                | 1.8     |
| spring boot        | 2.7.9   |
| mybatis-plus-extension | 3.5.3.1 |
| mybatis-plus | 3.5.3.1 |
| mybatis-plus-boot-starter | 3.5.3.1 |
| spring-boot-starter-security | 2.7.9   |

## 使用
### 添加依赖
{% tabs tag-hide %}
<!-- tab maven -->

```xml
<dependency>
  <groupId>cn.allbs</groupId>
  <artifactId>allbs-mybatis</artifactId>
  <version>2.0.1</version>
</dependency>
```

<!-- endtab -->

<!-- tab Gradle -->

```
implementation 'cn.allbs:allbs-mybatis:2.0.1'
```

<!-- endtab -->

<!-- tab Kotlin -->

```
implementation("cn.allbs:allbs-mybatis:2.0.1")
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
```

#### 日志打印示例

![image-20230327111534230](https://nas.allbs.cn:9006/cloudpic/2023/03/d7858814b394d0c4f4793f6cc0f1171f.png)

![image-20230327111549175](https://nas.allbs.cn:9006/cloudpic/2023/03/936d0f475f49c3f453be9ec73159fa79.png)

#### 审计字段自动插入

![image-20230327111732400](https://nas.allbs.cn:9006/cloudpic/2023/03/4878b68ea6a97f484ed4622a1c354143.png)
