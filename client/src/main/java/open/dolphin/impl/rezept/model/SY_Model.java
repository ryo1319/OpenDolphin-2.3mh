package open.dolphin.impl.rezept.model;

import java.util.Date;
import open.dolphin.impl.rezept.RezeUtil;
import open.dolphin.infomodel.ModelUtils;

/**
 * 傷病名レコード
 * 
 * @author masuda, Masuda Naika
 */
public class SY_Model implements IRezeModel {
    
    public static final String ID = "SY";
    
    private String srycd;       // 傷病名コード
    private Date startDate;     // 診療開始日
    private String outcome;     // 転帰区分
    private String modifier;    // 修飾語コード
    private String diagName;    // 傷病名称
    private boolean mainDiag;   // 主傷病
    private String comment;     // コメント
    
    private int byoKanrenKbn;
    
    private int hitCount;
    private boolean pass;
    
    public String getSrycd() {
        return srycd;
    }
    public String getStartDateStr() {
        return RezeUtil.getInstance().getDateStr(startDate);
    }
    public Date getStartDate() {
        return startDate;
    }
    public String getOutcome() {
        return outcome;
    }
    public String getOutcomeStr() {
        if (outcome == null) {
            return "";
        }
        switch (outcome) {
            case "2":
                return "治ゆ";
            case "3":
                return("死亡");
            case "4":
                //return("中止（転医）");
                return("中止");
            case "1":   // 治癒、死亡、中止以外
            default:
                return "";
        }
    }
    public boolean isActive() {
        if (outcome == null) {
            return true;
        }
        switch (outcome) {
            case "2":   // 治ゆ
            case "3":   // 死亡
            case "4":   // 中止
                return false;
            case "1":   // 治癒、死亡、中止以外
            default:
                return true;
        }
    }
    public String getModifier() {
        return modifier;
    }
    public String getDiagName() {
        return diagName;
    }
    public boolean isMainDiag() {
        return mainDiag;
    }
    public String getComment() {
        return comment;
    }

    public void setDiagName(String diagName) {
        this.diagName = diagName; 
    }
    public void setByoKanrenKbn(int i){
        byoKanrenKbn = i;
    }
    public int getByoKanrenKbn(){
        return byoKanrenKbn;
    }
    public String getByoKanrenKbnStr() {
        return ModelUtils.getByoKanrenKbnStr(byoKanrenKbn);
    }
    
    @Override
    public void parseLine(String csv) {
        String[] tokens = TokenSplitter.split(csv);
        srycd = tokens[1].trim();
        startDate = RezeUtil.getInstance().fromYearMonthDate(tokens[2]);
        outcome = tokens[3].trim();
        modifier = tokens[4].trim();
        diagName = tokens[5].trim();
        mainDiag = "01".equals(tokens[6]);
        comment = tokens[7].trim();
    }
    
    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    public int getHitCount() {
        return hitCount;
    }
    
    public void incrementHitCount() {
        hitCount++;
    }
    
    public void setPass(boolean pass) {
        this.pass = pass;
    }
    public boolean isPass() {
        return pass;
    }
}
