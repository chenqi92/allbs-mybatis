package cn.allbs.mybatis.datascope;

import cn.allbs.mybatis.execption.UserOverreachException;
import cn.allbs.mybatis.utils.PluginUtils;
import com.baomidou.mybatisplus.core.plugins.InterceptorIgnoreHelper;
import com.baomidou.mybatisplus.extension.parser.JsqlParserSupport;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import lombok.*;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SetOperationList;
import net.sf.jsqlparser.statement.update.Update;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.statement.StatementHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

/**
 * 数据权限处理器
 * 类 DataPermissionInterceptor
 *
 * @author ChenQi
 * @date 2023/3/29
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@SuppressWarnings({"rawtypes"})
public class DataPmsInterceptor extends JsqlParserSupport implements InnerInterceptor {

    protected final Logger logger = LoggerFactory.getLogger(this.getClass());
    private DataPmsHandler dataPermissionHandler;

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
        if (InterceptorIgnoreHelper.willIgnoreTenantLine(ms.getId())) {
            return;
        }
        // 处理包含分页的情况
        if (ms.getId().contains("_mpCount") && InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId().replace("_mpCount", ""))) {
            return;
        }
        PluginUtils.MPBoundSql mpBs = PluginUtils.mpBoundSql(boundSql);
        mpBs.sql(parserSingle(mpBs.sql(), null));
    }

    @Override
    public void beforePrepare(StatementHandler sh, Connection connection, Integer transactionTimeout) {
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
        MappedStatement ms = mpSh.mappedStatement();
        SqlCommandType sct = ms.getSqlCommandType();
        if (sct == SqlCommandType.UPDATE || sct == SqlCommandType.DELETE || sct == SqlCommandType.INSERT) {
            if (InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId())) {
                return;
            }
            handleInsertOrUpdateDataPms(mpSh, ms, sct);
        }
    }

    @Override
    public void beforeGetBoundSql(StatementHandler sh) {
        // do nothing
        PluginUtils.MPStatementHandler mpSh = PluginUtils.mpStatementHandler(sh);
        MappedStatement ms = mpSh.mappedStatement();
        SqlCommandType sct = ms.getSqlCommandType();
        if (sct == SqlCommandType.UPDATE || sct == SqlCommandType.INSERT) {
            if (InterceptorIgnoreHelper.willIgnoreDataPermission(ms.getId())) {
                return;
            }
            handleInsertOrUpdateDataPms(mpSh, ms, sct);
        }
    }

    /**
     * 新增或修改时判断是否存在超出用户权限的数据
     *
     * @param mpSh PluginUtils.MPStatementHandler
     * @param ms   MappedStatement
     * @param sct  SqlCommandType
     */
    private void handleInsertOrUpdateDataPms(PluginUtils.MPStatementHandler mpSh, MappedStatement ms, SqlCommandType sct) {
        PluginUtils.MPBoundSql mpBs = mpSh.mPBoundSql();
        mpBs.sql(parserMulti(mpBs.sql(), ms.getId()));
        try {
            // 当为更新或者插入时处理插入
            Statement statement = CCJSqlParserUtil.parse(mpSh.mPBoundSql().sql());
            if (sct == SqlCommandType.UPDATE) {
                dataPermissionHandler.updateParameter((Update) statement, ms, mpSh.boundSql());
            }
            if (sct == SqlCommandType.INSERT) {
                dataPermissionHandler.insertParameter((Insert) statement, mpSh.boundSql());
            }
        } catch (UserOverreachException e) {
            throw new UserOverreachException();
        } catch (JSQLParserException e) {
            logger.error("Unexpected error for mappedStatement={}, sql={}", ms.getId(), mpBs.sql(), e);
        }
    }

    /**
     * 查询
     */
    @Override
    protected void processSelect(Select select, int index, String sql, Object obj) {
        SelectBody selectBody = select.getSelectBody();
        if (selectBody instanceof PlainSelect) {
            this.setWhere((PlainSelect) selectBody, (String) obj);
        } else if (selectBody instanceof SetOperationList) {
            SetOperationList setOperationList = (SetOperationList) selectBody;
            List<SelectBody> selectBodyList = setOperationList.getSelects();
            selectBodyList.forEach(s -> this.setWhere((PlainSelect) s, (String) obj));
        }
    }

    /**
     * 新增
     */
    @Override
    protected void processInsert(Insert insert, int index, String sql, Object obj) {
    }

    /**
     * 删除
     */
    @Override
    protected void processDelete(Delete delete, int index, String sql, Object obj) {
        final Expression sqlSegment = getUpdateOrDeleteExpression(delete.getTable(), delete.getWhere(), (String) obj);
        if (null != sqlSegment) {
            delete.setWhere(sqlSegment);
        }
    }

    /**
     * 更新
     */
    @Override
    protected void processUpdate(Update update, int index, String sql, Object obj) {
        final Expression sqlSegment = getUpdateOrDeleteExpression(update.getTable(), update.getWhere(), (String) obj);
        if (null != sqlSegment) {
            update.setWhere(sqlSegment);
        }
    }

    /**
     * 设置 where 条件
     *
     * @param plainSelect  查询对象
     * @param whereSegment 查询条件片段
     */
    protected void setWhere(PlainSelect plainSelect, String whereSegment) {
        Table table = (Table) plainSelect.getFromItem();
        Expression sqlSegment = dataPermissionHandler.getSqlSegment(table, plainSelect.getWhere(), whereSegment);
        if (null != sqlSegment) {
            plainSelect.setWhere(sqlSegment);
        }
    }

    protected Expression getUpdateOrDeleteExpression(final Table table, final Expression where, final String whereSegment) {
        return dataPermissionHandler.getSqlSegment(table, where, whereSegment);
    }
}
