package top.yangxm.ai.mcp.test.finance;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import top.yangxm.ai.mcp.commons.json.McpJsonMapper;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpTool;
import top.yangxm.ai.mcp.org.springaicommunity.mcp.annotation.McpToolParam;

import java.util.List;

@Component
public class FinanceProductTool {
    private static final Logger logger = LoggerFactory.getLogger(FinanceProductTool.class);
    private static final McpJsonMapper jsonMapper = McpJsonMapper.getDefault();

    @McpTool(description = "根据筛选条件查询理财产品列表")
    public List<ProductInfo> queryFinanceProductList(
            @McpToolParam(description = "【筛选项】产品销售渠道（0=不筛选此项，1=自营，2=代销）。可多选，使用|分割，如：1|2")
            String filterConsignment,
            @McpToolParam(description = "【筛选项】最低购买金额（0=不筛选此项，1=0-1万，2=1-5万，3=5-50万，4=50-500万，5=500万以上）。可多选，使用|分割，如：1|2")
            String filterLowestBuyAmt,
            @McpToolParam(description = "【筛选项】产品风险等级（0=不筛选此项，1=风险等级1，2=风险等级2，3=风险等级3，4=风险等级4，5=风险等级5）。" +
                    "可多选，使用|分割，如：1|2")
            String filterRiskLevel,
            @McpToolParam(description = "【筛选项】产品类型（0=不筛选此项，1=固定收益类，2=权益类，3=商品及金融衍生品类，4=混合类，5=传统产品，6=结构性存款）。" +
                    "可多选，使用|分割，如：1|2")
            String filterProdType,
            @McpToolParam(description = "【筛选项】产品币种（0=不筛选此项，1=人民币，2=美元，3=其他币种）。可多选，使用|分割，如：1|2")
            String filterCurrtype,

            @McpToolParam(description = "【筛选项】产品状态（0=不筛选此项，GW=高盛工银理财，GY=工银理财，PAWM=平安理财，EW=光大理财，" +
                    "ZY1=招银理财，YZX=信银理财，MS=民生理财，PWM=中邮理财，Y3=中银理财，Y04=苏银理财，" +
                    "PY8=浦银理财，Y05=兴银理财）。可多选，使用|分割，如：GW|PAWM")
            String filterStatus,
            @McpToolParam(description = "【筛选项】代销机构（0=不筛选此项，填写机构编码进行筛选）")
            String filterOrganization,
            @McpToolParam(description = "【筛选项】是否工银研选（0=不限，1=筛选工银研选）")
            String filterFinanceChoice,
            @McpToolParam(description = "【筛选项】是否现金管理产品（0=不限，1=筛选现金管理产品）")
            String filterCurrentProd,
            @McpToolParam(description = "【筛选项】是否日开产品（0=不筛选此项此项，1=筛选日开产品）")
            String filterDayProd,
            @McpToolParam(description = "【筛选项】筛选0-6个月期限产品（0=不筛选此项此项，1=筛选0-6个月期限产品）")
            String filter06FixProd,
            @McpToolParam(description = "【筛选项】筛选6-12个月期限产品（0=不筛选此项此项，1=筛选6-12个月期限产品）")
            String filter612FixProd,
            @McpToolParam(description = "【筛选项】筛选12个月以上期限产品（0=不筛选此项此项，1=筛选12个月以上期限产品）")
            String filter12FixProd,
            @McpToolParam(description = "【筛选项】在售状态（0=不筛选此项此项，1=只展示在售产品，2=只展示非在售产品）")
            String QueryFundStatus,
            @McpToolParam(description = "排序类型（0=默认排序，1=近1月年化收益降序，2=近3月年化收益降序，" +
                    "3=近6月年化收益降序，4=近1年年化收益降序，5=成立以来年化收益降序，6=七日年化收益降序）")
            String sortType) {

        QueryInput queryInput = new QueryInput();
        queryInput.filterConsignment = filterConsignment;
        queryInput.filterLowestBuyAmt = filterLowestBuyAmt;
        queryInput.filterRiskLevel = filterRiskLevel;
        queryInput.filterProdType = filterProdType;
        queryInput.filterCurrtype = filterCurrtype;
        queryInput.filterStatus = filterStatus;
        queryInput.filterOrganization = filterOrganization;
        queryInput.filterFinanceChoice = filterFinanceChoice;
        queryInput.filterCurrentProd = filterCurrentProd;
        queryInput.filterDayProd = filterDayProd;
        queryInput.filter06FixProd = filter06FixProd;
        queryInput.filter612FixProd = filter612FixProd;
        queryInput.filter12FixProd = filter12FixProd;
        queryInput.QueryFundStatus = QueryFundStatus;
        queryInput.sortType = sortType;
        logger.info("【queryFinanceProductList】入参：{}", jsonMapper.writeValueAsString(queryInput));
        return ProductManager.query(queryInput);
    }
}
