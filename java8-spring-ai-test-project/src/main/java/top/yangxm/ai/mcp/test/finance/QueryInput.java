package top.yangxm.ai.mcp.test.finance;

import java.io.Serializable;

public class QueryInput implements Serializable {
    public String filterConsignment;
    public String filterLowestBuyAmt;
    public String filterRiskLevel;
    public String filterProdType;
    public String filterCurrtype;
    public String filterStatus;
    public String filterOrganization;
    public String filterFinanceChoice;
    public String filterCurrentProd;
    public String filterDayProd;
    public String filter06FixProd;
    public String filter612FixProd;
    public String filter12FixProd;
    public String QueryFundStatus;
    public String sortType;
}