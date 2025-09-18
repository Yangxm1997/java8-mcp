package top.yangxm.ai.mcp.test.finance;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.yangxm.ai.mcp.commons.util.Lists;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public final class ProductManager {
    private ProductManager() {
    }

    private static final Logger logger = LoggerFactory.getLogger(ProductManager.class);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final List<String> CONSIGNMNETS = Lists.of("1", "2");
    private static final List<String> RISK_LEVELS = Lists.of("1", "2", "3", "4", "5");
    private static final List<String> PRODUCT_TYPES = Lists.of("1", "2", "3", "4", "5", "6");
    private static final List<String> CURR_TYPES = Lists.of("1", "2", "3");
    private static final List<String> STATUS = Lists.of("1", "2");
    private static final List<String> ORGANIZATIONS = Lists.of("GW", "GY", "PAWM", "EW", "ZY1", "YZX", "MS", "PWM", "Y3", "Y04", "PY8", "Y05");
    private static final List<String> FUND_STATUS = Lists.of("1", "2");
    private static final int PRODUCT_SIZE = 5000;
    private static final List<ProductInfo> PRODUCT_INFOS = new ArrayList<>(PRODUCT_SIZE);

    static {
        for (int i = 100000; i < 1000000; i++) {
            if (SECURE_RANDOM.nextBoolean()) {
                String prodCode = String.format("%06d", i);
                PRODUCT_INFOS.add(randomProductInfo(prodCode));
                logger.info("Add product info, prodCode: {}", prodCode);
                if (PRODUCT_INFOS.size() >= PRODUCT_SIZE) {
                    break;
                }
            }
        }
    }

    private static ProductInfo randomProductInfo(String prodCode) {
        ProductInfo productInfo = new ProductInfo();
        productInfo.prodCode = prodCode;
        productInfo.prodType = randomValue(PRODUCT_TYPES);
        productInfo.prodName = "理财产品 " + prodCode;
        productInfo.profitName = "业绩比较基准";
        productInfo.profitValue = randomProfit();
        productInfo.proFitBeginDate = "";
        productInfo.proFitEndDate = "";
        productInfo.proFitFixDes = "FixDes " + prodCode;
        productInfo.profitListDesNew = "ListDesNew " + prodCode;
        productInfo.profitListDes = "ListDes " + prodCode;
        productInfo.profitTip = "Tip " + prodCode;
        productInfo.riskLevel = randomValue(RISK_LEVELS);
        productInfo.subsidiaryShow = "理财 " + prodCode + " 子产品";
        productInfo.limit_data = String.valueOf(SECURE_RANDOM.nextInt(1000) + 1);
        productInfo.lowestAmt = String.valueOf(SECURE_RANDOM.nextInt(10000000) + 1);
        productInfo.raiseDate = "";
        productInfo.recentOpenDay = "";
        productInfo.flagConsignment = randomValue(CONSIGNMNETS);
        productInfo.flagCurrtype = randomValue(CURR_TYPES);
        productInfo.flagStatus = randomValue(STATUS);
        productInfo.flagOrganization = randomValue(ORGANIZATIONS);
        productInfo.flagFundStatus = randomValue(FUND_STATUS);
        productInfo.flagFinanceChoice = SECURE_RANDOM.nextBoolean();
        productInfo.flagCurrentProd = SECURE_RANDOM.nextBoolean();
        productInfo.flagIsOpenDay = SECURE_RANDOM.nextBoolean();
        return productInfo;
    }

    private static String randomValue(List<String> values) {
        int len = values.size();
        int ram = SECURE_RANDOM.nextInt(len);
        return values.get(ram);
    }

    private static String randomProfit() {
        double f = 1 + (5 - 1) * SECURE_RANDOM.nextDouble();
        double d = 1 + (2 - 1) * SECURE_RANDOM.nextDouble();
        return String.format("%.2f%%-%.2f%%", f, f + d);
    }

    public static List<ProductInfo> query(QueryInput q) {
        List<ProductInfo> result = new ArrayList<>();
        for (ProductInfo prod : PRODUCT_INFOS) {
            if (q.filterConsignment != null
                    && !"0".equals(q.filterConsignment)
                    && !ArrayUtils.contains(q.filterConsignment.split("\\|"), prod.flagConsignment)) {
                continue;
            }


            if (q.filterRiskLevel != null
                    && !"0".equals(q.filterRiskLevel)
                    && !ArrayUtils.contains(q.filterRiskLevel.split("\\|"), prod.riskLevel)) {
                continue;
            }

            if (q.filterProdType != null
                    && !"0".equals(q.filterProdType)
                    && !ArrayUtils.contains(q.filterProdType.split("\\|"), prod.prodType)) {
                continue;
            }

            if (q.filterCurrtype != null
                    && !"0".equals(q.filterCurrtype)
                    && !ArrayUtils.contains(q.filterCurrtype.split("\\|"), prod.flagCurrtype)) {
                continue;
            }

            if (q.filterStatus != null
                    && !"0".equals(q.filterStatus)
                    && !ArrayUtils.contains(q.filterStatus.split("\\|"), prod.flagStatus)) {
                continue;
            }

            if (q.filterOrganization != null
                    && !"0".equals(q.filterOrganization)
                    && !ArrayUtils.contains(q.filterOrganization.split("\\|"), prod.flagOrganization)) {
                continue;
            }

            if ("1".equals(q.filterFinanceChoice) && !prod.flagFinanceChoice) {
                continue;
            }

            if ("1".equals(q.filterCurrentProd) && !prod.flagCurrentProd) {
                continue;
            }

            if ("1".equals(q.filterDayProd) && !prod.flagIsOpenDay) {
                continue;
            }

            if (!"0".equals(q.QueryFundStatus) && !q.QueryFundStatus.equals(prod.flagFundStatus)) {
                continue;
            }

            if (!"0".equals(q.filterLowestBuyAmt)) {
                int buyAmt = Integer.parseInt(prod.lowestAmt);
                String amtFlag;
                if (buyAmt < 10000) {
                    amtFlag = "1";
                } else if (buyAmt < 50000) {
                    amtFlag = "2";
                } else if (buyAmt < 500000) {
                    amtFlag = "3";
                } else if (buyAmt < 5000000) {
                    amtFlag = "4";
                } else {
                    amtFlag = "5";
                }
                if (!ArrayUtils.contains(q.filterLowestBuyAmt.split("\\|"), amtFlag)) {
                    continue;
                }
            }

            if ("1".equals(q.filter06FixProd) || "1".equals(q.filter612FixProd) || "1".equals(q.filter12FixProd)) {
                int limitData = Integer.parseInt(prod.limit_data);
                if (!(("1".equals(q.filter06FixProd) && limitData > 0 && limitData <= 182)
                        || ("1".equals(q.filter612FixProd) && limitData > 182 && limitData <= 365)
                        || ("1".equals(q.filter12FixProd) && limitData > 365))) {
                    continue;
                }
            }

            result.add(prod);
        }
        return result;
    }
}
