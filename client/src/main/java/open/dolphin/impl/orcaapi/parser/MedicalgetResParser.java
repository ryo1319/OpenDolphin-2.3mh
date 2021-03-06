package open.dolphin.impl.orcaapi.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import open.dolphin.dao.DaoException;
import open.dolphin.dao.SqlMiscDao;
import open.dolphin.infomodel.ClaimConst;
import open.dolphin.infomodel.TensuMaster;
import open.dolphin.order.MasterItem;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;

/**
 *
 * @author masuda, Masuda Naika
 */
public class MedicalgetResParser extends AbstractOrcaApiParser {
    
    public MedicalgetResParser(String xml) throws JDOMException, IOException {
        super(xml);
    }
    
    public MedicalgetResParser(Document doc) {
        super(doc);
    }
    
    public List<String> getPerformDate() {
        
        List<String> ret = new ArrayList<>();

        Iterator<Element> itr = doc.getDescendants(new ElementFilter(PERFORM_DATE));
        while (itr.hasNext()) {
            Element elm = itr.next();
            ret.add(elm.getText());
        }

        return ret;
    }
    
    public List<MasterItem> getMedMasterItem() throws DaoException {
        
        final String ADMIN_MARK = "[用法] ";
        
        List<MasterItem> list = new ArrayList();
        Set<String> srycdSet = new HashSet<>();
        
        Iterator<Element> itr = doc.getDescendants(new ElementFilter(MEDICAL_INFORMATION_CHILD));
        while (itr.hasNext()) {
            Element elem = itr.next();
            String classCode = elem.getChildText(MEDICAL_CLASS);
            // 薬剤の場合
            if (classCode != null && classCode.matches("2[1239].")) {
                
                Element info= elem.getChild(MEDICATION_INFO);
                List<Element> children = info.getChildren();
                boolean hasAdmin = false;
                String bundleNumber = elem.getChildText(MEDICAL_CLASS_NUMBER);
                
                for (Element child : children) {
                    String srycd = child.getChildText(MEDICATION_CODE);
                    String name = child.getChildText(MEDICATION_NAME);
                    String num = child.getChildText(MEDICATION_NUMBER);
                    MasterItem mi = new MasterItem();
                    mi.setCode(srycd);
                    mi.setName(name);
                    mi.setNumber(num);
                    if (srycd.startsWith("001")) {
                        mi.setBundleNumber(bundleNumber);
                        hasAdmin = true;
                    }
                    list.add(mi);
                    srycdSet.add(srycd);
                }
                
                if (!hasAdmin) {
                    final String srycd = "001000101";
                    MasterItem mi = new MasterItem();
                    mi.setCode(srycd);
                    mi.setBundleNumber(bundleNumber);
                    list.add(mi);
                    srycdSet.add(srycd);
                }
            }
        }
        if (list.isEmpty()) {
            return Collections.emptyList();
        }
        
        // ORCAに問い合わせ
        List<TensuMaster> tmList = SqlMiscDao.getInstance().getTensuMasterList(srycdSet);
        // HashMapに登録
        HashMap<String, TensuMaster> tensuMasterMap = new HashMap<>();
        for (TensuMaster tm : tmList) {
            tensuMasterMap.put(tm.getSrycd(), tm);
        }
        
        // reconstruct
        for (MasterItem mi : list) {
            String srycd = mi.getCode();
            TensuMaster tm = tensuMasterMap.get(srycd);
            mi.setDataKbn(tm.getDataKbn());
            
            if (srycd.startsWith("84") || srycd.startsWith("0084")) {
                // 84コメントコード
                reconstruct84Item(mi, tm);
            } else if (srycd.matches(ClaimConst.REGEXP_COMMENT_MED)) {
                // コメントコード
                mi.setClassCode(ClaimConst.OTHER);
                mi.setUnit(tm.getTaniname());
                //mi.setName(tm.getName());
                mi.setYkzKbn(tm.getYkzkbn());
                mi.setNumber(null);
                mi.setBundleNumber(null);
            } else if (srycd.startsWith("001")) {
                // 用法コードのTensuMasterを作成
                mi.setClassCode(ClaimConst.ADMIN);
                mi.setName(ADMIN_MARK + tm.getName());
                mi.setDummy("X");
                mi.setNumber(null);
            } else {
                // 薬剤コードのTensuMasterを作成
                mi.setClassCode(ClaimConst.YAKUZAI);
                mi.setUnit(tm.getTaniname());
                mi.setName(tm.getName());
                mi.setYkzKbn(tm.getYkzkbn());
            }
        }
        
        srycdSet.clear();
        tensuMasterMap.clear();
        
        return list;
    }
    
    private void reconstruct84Item(MasterItem mi, TensuMaster tm) {
        
        mi.setClassCode(ClaimConst.OTHER);
        mi.setUnit(tm.getTaniname());
        mi.setYkzKbn(tm.getYkzkbn());
        
        String miName = mi.getName();
        String tmName = tm.getName();
        List<String> tokens = new ArrayList();
        
        int len = Math.min(miName.length(), tmName.length());
        StringBuilder sb = new StringBuilder();
        
        for (int i = 0; i < len; ++i) {
            char miChar = miName.charAt(i);
            char tmChar = tmName.charAt(i);
            if (miChar == tmChar && sb.length() > 0) {
                tokens.add(toHankakuNum(sb.toString()));
                sb.setLength(0);
            } else {
                sb.append(miChar);
            }
        }
        if (sb.length() > 0) {
            tokens.add(toHankakuNum(sb.toString()));
        }
        
        sb = new StringBuilder();
        boolean first = true;
        for (String token : tokens) {
            if (first) {
                first = false;
            } else {
                sb.append('-');
            }
            sb.append(token);
        }
        
        mi.setName(tmName);
        mi.setNumber(sb.toString());
    }
    
    private static final String[] ZENKAKU_NUM = {"０", "１", "２", "３", "４", "５", "６", "７", "８", "９", "．"};
    private static final String[] HANKAKU_NUM = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "."};
    
    private String toHankakuNum(String str) {
        str = str.replace(" ", "").replace("　", "");
        for (int i = 0; i < ZENKAKU_NUM.length; ++i) {
            str = str.replace(ZENKAKU_NUM[i], HANKAKU_NUM[i]);
        }
        return str;
    }
}
