package cn.allbs.mybatis.datascope;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;

import java.sql.Connection;

/**
 * 接口 AllbsDataPermissionHandler
 *
 * @author ChenQi
 * @date 2023/3/28
 */
public interface DataPmsHandler {

    /**
     * 获取数据权限 SQL 片段
     *
     * @param table             表相关信息
     * @param where             待执行 SQL Where 条件表达式
     * @param mappedStatementId Mybatis MappedStatement Id 根据该参数可以判断具体执行方法
     * @return JSqlParser 条件表达式，返回的条件表达式会覆盖原有的条件表达式
     */
    Expression getSqlSegment(final Table table, Expression where, String mappedStatementId);

    /**
     * 新增数据时 判断是否存在越权行为，如果存在这种行为则进行拦截并重组sql
     *
     * @param insertStmt Insert
     * @param boundSql   BoundSql
     */
    void insertParameter(Insert insertStmt, BoundSql boundSql);

    /**
     * 更新数据时 判断是否存在越权行为，如果存在这种行为则进行拦截并重组sql
     *
     * @param updateStmt      Update
     * @param mappedStatement MappedStatement
     * @param boundSql        BoundSql
     * @param connection      Connection
     */
    void updateParameter(Update updateStmt, MappedStatement mappedStatement, BoundSql boundSql, Connection connection);
}

