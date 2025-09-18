package top.yangxm.ai.mcp.test.finance;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.io.Serializable;

public class ProductInfo implements Serializable {
    public String profitName;
    public String profitValue;
    public String proFitBeginDate;
    public String proFitEndDate;
    public String proFitFixDes;
    public String profitListDesNew;
    public String profitListDes;
    public String profitTip;
    public String riskLevel;
    public String subsidiaryShow;
    public String prodName;
    public String prodType;
    public String prodCode;
    public String limit_data;
    public String lowestAmt;
    public String raiseDate;
    public String recentOpenDay;

    @JsonIgnore
    public String flagConsignment;

    @JsonIgnore
    public String flagCurrtype;

    @JsonIgnore
    public String flagStatus;

    @JsonIgnore
    public String flagOrganization;

    @JsonIgnore
    public String flagFundStatus;

    @JsonIgnore
    public boolean flagFinanceChoice;

    @JsonIgnore
    public boolean flagCurrentProd;

    @JsonIgnore
    public boolean flagIsOpenDay;
}
