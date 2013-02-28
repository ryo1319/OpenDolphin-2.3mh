package open.dolphin.delegater;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import open.dolphin.client.ClientContext;
import open.dolphin.infomodel.*;
import open.dolphin.util.BeanUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jboss.resteasy.client.ClientRequest;


/**
 * Bsiness Delegater のルートクラス。
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public class BusinessDelegater {

    protected static final String CAMMA = ",";
    
//masuda^
    private static final String CHARSET_UTF8 = "; charset=UTF-8";
    protected static final String MEDIATYPE_JSON_UTF8 = MediaType.APPLICATION_JSON + CHARSET_UTF8;
    protected static final String MEDIATYPE_TEXT_UTF8 = MediaType.TEXT_PLAIN + CHARSET_UTF8;
//masuda$
    
    protected Logger logger;

    protected boolean DEBUG;
    
    public BusinessDelegater() {
        logger = ClientContext.getDelegaterLogger();
        DEBUG = (logger.getLevel() == Level.DEBUG);
    }

    protected ClientRequest getClientRequest(String path, MultivaluedMap<String, String> qmap) {
        return RESTEasyClient.getInstance().getClientRequest(path, qmap);
    }
    
    protected ClientRequest getClientRequest(String path) {
        return RESTEasyClient.getInstance().getClientRequest(path);
    }

    protected void debug(int status, String entity) {
        logger.debug("---------------------------------------");
        logger.debug("status = " + status);
        logger.debug(entity);
    }
    
    protected JsonConverter getConverter() {
        return JsonConverter.getInstance();
    }

    protected String toRestFormat(Date date) {
        try {
            SimpleDateFormat frmt = new SimpleDateFormat(IInfoModel.ISO_DF_FORMAT);
            return frmt.format(date);
        } catch (Exception e) {
            e.printStackTrace(System.err);
        }
        return null;
    }
    
    /**
     * バイナリの健康保険データをオブジェクトにデコードする。
     */
    protected void decodePvtHealthInsurance(Collection<PatientVisitModel> list) {
        
        if (list != null && !list.isEmpty()) {
            for (PatientVisitModel pm : list) {
                decodeHealthInsurance(pm.getPatientModel());
            }
        }
    }

    protected void decodeHealthInsurance(PatientModel patient) {

        // Health Insurance を変換をする beanXML2PVT
        Collection<HealthInsuranceModel> c = patient.getHealthInsurances();

        if (c != null && !c.isEmpty()) {

            for (HealthInsuranceModel model : c) {
                try {
                    // byte[] を XMLDecord
                    PVTHealthInsuranceModel hModel = (PVTHealthInsuranceModel) 
                            BeanUtils.xmlDecode(model.getBeanBytes());
                    patient.addPvtHealthInsurance(hModel);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
            }

            c.clear();
            patient.setHealthInsurances(null);
        }
    }
    
    protected void isHTTP200(int status) throws Exception {
        if (status != 200) {
            String msg = "HTTP" + String.valueOf(status);
            throw new Exception(msg);
        }
    }
}
