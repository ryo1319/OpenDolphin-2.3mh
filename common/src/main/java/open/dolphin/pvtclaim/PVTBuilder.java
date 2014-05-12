package open.dolphin.pvtclaim;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import open.dolphin.common.util.BeanUtils;
import open.dolphin.infomodel.*;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;

/**
 * PVTBuilder
 *
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 */
public final class PVTBuilder {

    private static final Namespace mmlCm = Namespace.getNamespace("mmlCm","http://www.medxml.net/MML/SharedComponent/Common/1.0");
    private static final Namespace mmlNm = Namespace.getNamespace("mmlNm","http://www.medxml.net/MML/SharedComponent/Name/1.0");
    private static final Namespace mmlFc = Namespace.getNamespace("mmlFc","http://www.medxml.net/MML/SharedComponent/Facility/1.0");
    private static final Namespace mmlDp = Namespace.getNamespace("mmlDp","http://www.medxml.net/MML/SharedComponent/Department/1.0");
    private static final Namespace mmlAd = Namespace.getNamespace("mmlAd","http://www.medxml.net/MML/SharedComponent/Address/1.0");
    private static final Namespace mmlPh = Namespace.getNamespace("mmlPh","http://www.medxml.net/MML/SharedComponent/Phone/1.0");
    private static final Namespace mmlPsi = Namespace.getNamespace("mmlPsi","http://www.medxml.net/MML/SharedComponent/PersonalizedInfo/1.0");
    private static final Namespace mmlCi = Namespace.getNamespace("mmlCi","http://www.medxml.net/MML/SharedComponent/CreatorInfo/1.0");
    private static final Namespace mmlPi = Namespace.getNamespace("mmlPi","http://www.medxml.net/MML/ContentModule/PatientInfo/1.0");
    private static final Namespace mmlHi = Namespace.getNamespace("mmlHi","http://www.medxml.net/MML/ContentModule/HealthInsurance/1.1");
    private static final Namespace mmlSc = Namespace.getNamespace("mmlSc","http://www.medxml.net/MML/SharedComponent/Security/1.0");
    private static final Namespace claim = Namespace.getNamespace("claim","http://www.medxml.net/claim/claimModule/2.1");
    
    private static final String MmlBody = "MmlBody";
    private static final String MmlModuleItem = "MmlModuleItem";
    private static final String docInfo = "docInfo";
    private static final String content = "content";
    private static final String contentModuleType = "contentModuleType";
    private static final String patientInfo = "patientInfo";
    private static final String healthInsurance = "healthInsurance";
    private static final String docId = "docId";
    private static final String uid = "uid";
    private static final String e_claim = "claim";
    private static final String mmlCm_Id = "mmlCm:Id";
    private static final String mmlNm_Name = "mmlNm:Name";
    private static final String repCode = "repCode";
    private static final String tableId = "tableId";
    private static final String mmlNm_family = "mmlNm:family";
    private static final String P = "P";
    private static final String I = "I";
    private static final String A = "A";
    private static final String mmlNm_given = "mmlNm:given";
    private static final String mmlNm_fullname = "mmlNm:fullname";
    private static final String mmlPi_birthday = "mmlPi:birthday";
    private static final String mmlPi_sex = "mmlPi:sex";
    private static final String mmlAd_Address = "mmlAd:Address";
    private static final String addressClass = "addressClass";
    private static final String mmlAd_full = "mmlAd:full";
    private static final String mmlAd_zip = "mmlAd:zip";
    private static final String mmlPh_Phone = "mmlPh:Phone";
    private static final String mmlPh_area = "mmlPh:area";
    private static final String mmlPh_city = "mmlPh:city";
    private static final String mmlPh_number = "mmlPh:number";
    private static final String mmlPh_memo = "mmlPh:memo";
    private static final String HealthInsuranceModule = "HealthInsuranceModule";
    private static final String insuranceClass = "insuranceClass";
    private static final String ClassCode = "ClassCode";
    private static final String insuranceNumber = "insuranceNumber";
    private static final String clientId = "clientId";
    private static final String group = "group";
    private static final String number = "number";
    private static final String familyClass = "familyClass";
    private static final String startDate = "startDate";
    private static final String expiredDate = "expiredDate";
    private static final String paymentInRatio = "paymentInRatio";
    private static final String paymentOutRatio = "paymentOutRatio";
    private static final String publicInsurance = "publicInsurance";
    private static final String priority = "priority";
    private static final String providerName = "providerName";
    private static final String provider = "provider";
    private static final String recipient = "recipient";
    private static final String paymentRatio = "paymentRatio";
    private static final String ratioType = "ratioType";
    private static final String CreatorInfo = "CreatorInfo";
    private static final String PersonalizedInfo = "PersonalizedInfo";
    private static final String Id = "Id";
    private static final String personName = "personName";
    private static final String Name = "Name";
    private static final String fullname = "fullname";
    private static final String Facility = "Facility";
    private static final String Department = "Department";
    private static final String name = "name";
    private static final String ClaimModule = "ClaimModule";
    private static final String information = "information";
    private static final String status = "status";
    private static final String registTime = "registTime";
    private static final String admitFlag = "admitFlag";
    private static final String insuranceUid = "insuranceUid";

    private static final char FULL_SPACE = '　';
    private static final char HALF_SPACE = ' ';
    
    private PatientModel patientModel;
    
    private AddressModel curAddress;
    
    private TelephoneModel curTelephone;
    
    private ArrayList<PVTHealthInsuranceModel> pvtInsurnaces;
    
    private PVTHealthInsuranceModel curInsurance;
    
    private PVTPublicInsuranceItemModel curPublicItem;
    
    private PVTClaim pvtClaim;
    
    private String curRepCode;

    private final boolean DEBUG;
    
    private static final Logger logger = Logger.getLogger(PVTBuilder.class.getSimpleName());
    
    public PVTBuilder() {
        DEBUG = false;
    }
    
    /**
     * CLAIM モジュールをパースする。
     *
     * @param reader CLAIM モジュールへの Reader
     */
    public void parse(Reader reader) {
        
        try {
            SAXBuilder docBuilder = new SAXBuilder();
            Document doc = docBuilder.build(reader);
            Element root = doc.getRootElement();
            parseBody(root.getChild(MmlBody));
            reader.close();
            
        } catch (IOException | JDOMException e) {
            logger.warning(e.getMessage());
        }
    }
    
    /**
     * CLAIM モジュールをパースして得た PatientVisitModel オブジェクトを返す。
     *
     * @return パース結果の PatientVisitModel
     */
    public PatientVisitModel getProduct() {
        
        // PatientVisitModelを生成する
        PatientVisitModel model = new PatientVisitModel();
        
        // 患者モデルを設定する
        if (patientModel != null) {
            
            model.setPatientModel(patientModel);
            
            // ORCA CLAIM 特有の処理を行う
            // 全角のスペースを半角スペースに変換する
            String fullName = patientModel.getFullName();
            fullName = fullName.replace(FULL_SPACE, HALF_SPACE);
            patientModel.setFullName(fullName);
            
            // FamilyName と GivenName を設定する
            int index = fullName.indexOf(HALF_SPACE);
            if (patientModel.getFamilyName() == null && index > 0) {
                patientModel.setFamilyName(fullName.substring(0, index));
            }
            if (patientModel.getGivenName() == null && index > 0) {
                patientModel.setGivenName(fullName.substring(index+1));
            }
            
            String kana = patientModel.getKanaName();
            if (kana != null) {
                kana = kana.replace(FULL_SPACE, HALF_SPACE);
                patientModel.setKanaName(kana);
                int index2 = kana.indexOf(HALF_SPACE);
                if (patientModel.getKanaFamilyName() == null && index2 > 0) {
                    patientModel.setKanaFamilyName(kana.substring(0, index2));
                }
                if (patientModel.getKanaGivenName() == null && index2 > 0) {
                    patientModel.setKanaGivenName(kana.substring(index2+1));
                }
            }
            
            // 住所をEmbedded に変換する
            Collection<AddressModel> addresses = patientModel.getAddresses();
            if (addresses != null && addresses.size() > 0) {
                for (AddressModel bean : addresses) {
                    String addr = bean.getAddress().replace(FULL_SPACE, HALF_SPACE);
                    addr = addr.replace('ー', '-');
                    SimpleAddressModel simple = new SimpleAddressModel();
                    simple.setZipCode(bean.getZipCode());
                    simple.setAddress(addr);
                    patientModel.setSimpleAddressModel(simple);
                    break;  // TODO
                }
            }
            
            // 電話をフィールドにする
            Collection<TelephoneModel> telephones = patientModel.getTelephones();
            if (telephones != null) {
                for (TelephoneModel bean : telephones) {
                    // MEMO へ設定
                    patientModel.setTelephone(bean.getMemo());
                }
            }
            
            // 健康保険モジュールを設定する
            if (pvtInsurnaces != null && pvtInsurnaces.size() > 0) {
                for (PVTHealthInsuranceModel bean : pvtInsurnaces) {
                    // 健康保険モジュールの BeanXml byte を生成し、
                    // 永続化のためのフォルダ HealthInsuranceModelに変換し
                    // それを患者属性に追加する
                    HealthInsuranceModel insModel = new HealthInsuranceModel();
                    insModel.setBeanBytes(BeanUtils.xmlEncode(bean));
                    // EJB 3.0 の関連を設定する
                    patientModel.addHealthInsurance(insModel);
                    insModel.setPatient(patientModel);
                }
            }
        }
        
        //------------------------------------------------------
        // 受付情報を設定する
        // status=info ありだって、ヤレヤレ...
        //------------------------------------------------------
        if (pvtClaim != null) {

            // 2.0 から
            model.setDeptCode(pvtClaim.getClaimDeptCode());         // 診療科コード
            model.setDeptName(pvtClaim.getClaimDeptName());         // 診療科名
            model.setDoctorId(pvtClaim.getAssignedDoctorId());      // 担当医コード
            model.setDoctorName(pvtClaim.getAssignedDoctorName());  // 担当医名
            model.setJmariNumber(pvtClaim.getJmariCode());          // JMARI
            model.setPvtDate(pvtClaim.getClaimRegistTime());        // 受付登録日時
            model.setInsuranceUid(pvtClaim.getInsuranceUid());      // UUID
            if (pvtInsurnaces != null && pvtInsurnaces.size() > 0) {
                PVTHealthInsuranceModel bean = pvtInsurnaces.get(0);    // 受付た保険情報
                model.setFirstInsurance(bean.toString());
            }
        }
        
//pns^  ORCA から送られてきた「診療内容」を pvt にセット
        model.setMemo(pvtClaim.getClaimAppMemo());
//pns$
//masuda^   患者登録のみのとき
        if (pvtClaim == null || "info".equals(pvtClaim.getClaimStatus())) {
            model.setPvtDate(null);
        }
//masuda$

        return model;
    }
    
    /**
     * MmlBody 要素をパースする。
     *
     * @param current 要素
     */
    public void parseBody(Element body) {
        
        // MmlModuleItem のリストを得る
        List<Element> children = body.getChildren(MmlModuleItem);
        
        //----------------------------
        // それをイテレートする
        //-------------------------
        for (Element moduleItem : children) {
            
            //---------------------------------------------------
            // ModuleItem = docInfo + content なので夫々の要素を得る
            //---------------------------------------------------
            Element docInfoEle = moduleItem.getChild(docInfo);
            Element contentEle = moduleItem.getChild(content);
            
            // docInfo の contentModuleType を調べる
            String attr = docInfoEle.getAttributeValue(contentModuleType);
            
            if (attr == null) {
                continue;
            }
            //------------------------------
            // contentModuleTypeで分岐する
            //------------------------------
            switch (attr) {
                case patientInfo:
                    //-----------------------
                    // 患者モジュールをパースする
                    //-----------------------
                    if (DEBUG) {
                        debug("patientInfo　をパース中");
                    }   patientModel = new PatientModel();
                    parsePatientInfo(docInfoEle, contentEle);
                    break;
                case healthInsurance:
                    //------------------------------
                    // 健康保険モジュールをパースする
                    //------------------------------
                    String uuid = docInfoEle.getChild(docId).getChildTextTrim(uid);
                    debug("healthInsurance　をパース中");
                    debug("HealthInsurance UUID = " + uuid);
                    if (pvtInsurnaces == null) {
                        pvtInsurnaces = new ArrayList<>();
                    }
                    curInsurance = new PVTHealthInsuranceModel();
                    curInsurance.setGUID(uuid);
                    pvtInsurnaces.add(curInsurance);
                    parseHealthInsurance(docInfoEle, contentEle);
                    break;
                case e_claim:
                    //------------------------------
                    // 受付情報をパースする
                    //------------------------------
                    debug("claim　をパース中");
                    pvtClaim = new PVTClaim();
                    parseClaim(docInfoEle, contentEle);
                    break;
                default:
                    logger.log(Level.WARNING, "Unknown attribute value : {0}", attr);
                    break;
            }
        }
    }
    
    /**
     * 患者モジュールをパースする。
     *
     * @param content 患者要素
     */
    private void parsePatientInfo(Element docInfo, Element content) {
        
        //-------------------------------------
        // 患者モジュールの要素をイテレートする
        //-------------------------------------
        List<Element> children = content.getChildren();
        
        for (Element child : children) {
            
            String qname = child.getQualifiedName();
            
            if (qname == null) {
                continue;
            }
            
            switch (qname) {
                case mmlCm_Id:
                    String pid = child.getTextTrim();
                    patientModel.setPatientId(pid);
                    debug("patientId = " + pid);
                    break;
                case mmlNm_Name:
                    for (Attribute attr : child.getAttributes()) {
                        String attrName = attr.getName();
                        if (attrName != null) {
                            switch (attrName) {
                                case repCode:
                                    curRepCode = attr.getValue();
                                    debug("curRepCode = " + attr.getValue());
                                    break;
                                case tableId:
                                    debug("tableId = " + attr.getValue());
                                    break;
                            }
                        }
                    }
                    break;
                case mmlNm_family:
                    if (curRepCode != null) {
                        switch (curRepCode) {
                            case P:
                                patientModel.setKanaFamilyName(child.getTextTrim());
                                break;
                            case I:
                                patientModel.setFamilyName(child.getTextTrim());
                                break;
                            case A:
                                patientModel.setRomanFamilyName(child.getTextTrim());
                                break;
                        }
                        debug("family = " + child.getTextTrim());
                    }
                    break;
                case mmlNm_given:
                    if (curRepCode != null) {
                        switch (curRepCode) {
                            case P:
                                patientModel.setKanaGivenName(child.getTextTrim());
                                break;
                            case I:
                                patientModel.setGivenName(child.getTextTrim());
                                break;
                            case A:
                                patientModel.setRomanGivenName(child.getTextTrim());
                                break;
                        }
                        debug("given = " + child.getTextTrim());
                    }
                    break;
                case mmlNm_fullname:
                    if (curRepCode != null) {
                        switch (curRepCode) {
                            case P:
                                patientModel.setKanaName(child.getTextTrim());
                                break;
                            case I:
                                patientModel.setFullName(child.getTextTrim());
                                break;
                            case A:
                                patientModel.setRomanName(child.getTextTrim());
                                break;
                        }
                        debug("fullName = " + child.getTextTrim());
                    }
                    break;
                case mmlPi_birthday:
                    patientModel.setBirthday(child.getTextTrim());
                    debug("birthday = " + child.getTextTrim());
                    break;
                case mmlPi_sex:
                    patientModel.setGender(child.getTextTrim());
                    debug("gender = " + child.getTextTrim());
                    break;
                case mmlAd_Address:
                    curAddress = new AddressModel();
                    patientModel.addAddress(curAddress);
                    for (Attribute attr : child.getAttributes()) {
                        String attrName = attr.getName();
                        if (attrName != null) {
                            switch (attrName) {
                                case addressClass:
                                    curRepCode = attr.getValue();
                                    curAddress.setAddressType(attr.getValue());
                                    debug("addressClass = " + attr.getValue());
                                    break;
                                case tableId:
                                    curAddress.setAddressTypeCodeSys(attr.getValue());
                                    debug("tableId = " + attr.getValue());
                                    break;
                            }
                        }
                    }
                    break;
                case mmlAd_full:
                    curAddress.setAddress(child.getTextTrim());
                    debug("address = " + child.getTextTrim());
                    break;
                case mmlAd_zip:
                    curAddress.setZipCode(child.getTextTrim());
                    debug("zip = " + child.getTextTrim());
                    break;
                case mmlPh_Phone:
                    curTelephone = new TelephoneModel();
                    patientModel.addTelephone(curTelephone);
                    break;
                case mmlPh_area: {
                    String val = child.getTextTrim();
                    //val = ZenkakuUtils.utf8Replace(val);
                    curTelephone.setArea(val);
                    debug("area = " + val);
                    break;
                }
                case mmlPh_city: {
                    String val = child.getTextTrim();
                    //val = ZenkakuUtils.utf8Replace(val);
                    curTelephone.setCity(val);
                    debug("city = " + val);
                    break;
                }
                case mmlPh_number: {
                    String val = child.getTextTrim();
                    //val = ZenkakuUtils.utf8Replace(val);
                    curTelephone.setNumber(val);
                    debug("number = " + val);
                    break;
                }
                case mmlPh_memo:
                    // ORCA
                    curTelephone.setMemo(child.getTextTrim());
                    debug("memo = " + child.getTextTrim());
                    break;
            }
            
            parsePatientInfo(docInfo, child);
        }
    }
    
    /**
     * 健康保険モジュールをパースする。
     *
     * @param content 健康保険要素
     */
    private void parseHealthInsurance(Element docInfo, Element content) {
        
        // HealthInsuranceModule を得る
        Element hModule = content.getChild(HealthInsuranceModule, mmlHi);
        if (hModule == null) {
            debug("No HealthInsuranceModule");
            return;
        }
        
        // InsuranceClass を解析する
        Element insuranceClassEle = hModule.getChild(insuranceClass, mmlHi);
        if (insuranceClass != null) {
            curInsurance.setInsuranceClass(insuranceClassEle.getTextTrim());
            if (insuranceClassEle.getAttribute(ClassCode, mmlHi) != null) {
                curInsurance.setInsuranceClassCode(insuranceClassEle.getAttributeValue(ClassCode, mmlHi));
            }
            if (insuranceClassEle.getAttribute(tableId, mmlHi) != null) {
                curInsurance.setInsuranceClassCodeSys(insuranceClassEle.getAttributeValue(tableId, mmlHi));
            }
        }
        
        // insurance Number を得る
        if (hModule.getChildTextTrim(insuranceNumber, mmlHi) != null) {
            curInsurance.setInsuranceNumber(hModule.getChildTextTrim(insuranceNumber, mmlHi));
        }
        
        // clientId を得る
        Element clientIdEle = hModule.getChild(clientId, mmlHi);
        if (clientIdEle != null) {
            if (clientIdEle.getChild(group,mmlHi) != null) {
                curInsurance.setClientGroup(clientIdEle.getChildTextTrim(group,mmlHi));
            }
            if (clientIdEle.getChild(number,mmlHi) != null) {
                curInsurance.setClientNumber(clientIdEle.getChildTextTrim(number,mmlHi));
            }
        }
        
        // familyClass を得る
        if (hModule.getChild(familyClass, mmlHi) != null) {
            curInsurance.setFamilyClass(hModule.getChildTextTrim(familyClass, mmlHi));
        }
        
        // startDateを得る
        if (hModule.getChild(startDate, mmlHi) != null) {
            curInsurance.setStartDate(hModule.getChildTextTrim(startDate, mmlHi));
        }
        
        // expiredDateを得る
        if (hModule.getChild(expiredDate, mmlHi) != null) {
            curInsurance.setExpiredDate(hModule.getChildTextTrim(expiredDate, mmlHi));
        }
        
        // payInRatio を得る
        if (hModule.getChild(paymentInRatio, mmlHi) != null) {
            curInsurance.setPayInRatio(hModule.getChildTextTrim(paymentInRatio, mmlHi));
        }
        
        // payOutRatio を得る
        if (hModule.getChild(paymentOutRatio, mmlHi) != null) {
            curInsurance.setPayOutRatio(hModule.getChildTextTrim(paymentOutRatio, mmlHi));
        }

        debug("insuranceClass = " + curInsurance.getInsuranceClass());
        debug("insurance ClassCode = " + curInsurance.getInsuranceClassCode());
        debug("insurance tableId = " + curInsurance.getInsuranceClassCodeSys());
        debug("insuranceNumber = " + curInsurance.getInsuranceNumber());
        debug("group = " + curInsurance.getClientGroup());
        debug("number = " + curInsurance.getClientNumber());
        debug("familyClass = " + curInsurance.getFamilyClass());
        debug("startDate = " + curInsurance.getStartDate());
        debug("expiredDate = " + curInsurance.getExpiredDate());
        debug("paymentInRatio = " + curInsurance.getPayInRatio());
        debug("paymentOutRatio = " + curInsurance.getPayOutRatio());

        
        //--------------------------------
        // publicInsurance をパースする
        //--------------------------------
        Element publicInsuranceEle = hModule.getChild(publicInsurance, mmlHi);
        if (publicInsuranceEle != null) {
            
            List<Element> children = publicInsuranceEle.getChildren();
            
            for (Element publicInsuranceItem : children) {
                
                curPublicItem = new PVTPublicInsuranceItemModel();
                curInsurance.addPvtPublicInsuranceItem(curPublicItem);
            
                // priority
                if (publicInsuranceItem.getAttribute(priority, mmlHi) != null) {
                    curPublicItem.setPriority(publicInsuranceItem.getAttributeValue(priority, mmlHi));
                }
                
                // providerName
                if (publicInsuranceItem.getChild(providerName, mmlHi) != null) {
                    curPublicItem.setProviderName(publicInsuranceItem.getChildTextTrim(providerName, mmlHi));
                }
                
                // provider
                if (publicInsuranceItem.getChild(provider, mmlHi) != null) {
                    curPublicItem.setProvider(publicInsuranceItem.getChildTextTrim(provider, mmlHi));
                }
                
                // recipient
                if (publicInsuranceItem.getChild(recipient, mmlHi) != null) {
                    curPublicItem.setRecipient(publicInsuranceItem.getChildTextTrim(recipient, mmlHi));
                }
                
                // startDate
                if (publicInsuranceItem.getChild(startDate, mmlHi) != null) {
                    curPublicItem.setStartDate(publicInsuranceItem.getChildTextTrim(startDate, mmlHi));
                }
                
                // expiredDate
                if (publicInsuranceItem.getChild(expiredDate, mmlHi) != null) {
                    curPublicItem.setExpiredDate(publicInsuranceItem.getChildTextTrim(expiredDate, mmlHi));
                }
                
                // paymentRatio
                Element paymentRatioEle = publicInsuranceItem.getChild(paymentRatio, mmlHi);
                if (paymentRatioEle != null) {
                    curPublicItem.setPaymentRatio(paymentRatioEle.getTextTrim());
                    if (paymentRatioEle.getAttribute(ratioType, mmlHi) != null) {
                        curPublicItem.setPaymentRatioType(paymentRatioEle.getAttributeValue(ratioType, mmlHi));
                    }
                }

                debug("priority = " + curPublicItem.getPriority());
                debug("providerName = " + curPublicItem.getProviderName());
                debug("provider = " + curPublicItem.getProvider());
                debug("recipient = " + curPublicItem.getRecipient());
                debug("startDate = " + curPublicItem.getStartDate());
                debug("expiredDate = " + curPublicItem.getExpiredDate());
                debug("paymentRatio = " + curPublicItem.getPaymentRatio());
                debug("paymentRatioType = " + curPublicItem.getPaymentRatioType());
            }
        }
    }
    
    /**
     * 受付情報をパースする。
     *
     * @param content 受付情報要素
     */
    private void parseClaim(Element docInfo, Element content) {
        
        //-------------------------------------------------------
        // ClaimModule の DocInfo に含まれる診療科と担当医を抽出する
        //-------------------------------------------------------
        Element creatorInfo = docInfo.getChild(CreatorInfo, mmlCi);
        Element psiInfo = creatorInfo.getChild(PersonalizedInfo, mmlPsi);
        
        // 担当医ID
        pvtClaim.setAssignedDoctorId(psiInfo.getChildTextTrim(Id, mmlCm));
        
        // 担当医名
        Element personNameEle = psiInfo.getChild(personName, mmlPsi);
        Element nameEle = personNameEle.getChild(Name, mmlNm);
        if (nameEle != null) {
            Element fullName = nameEle.getChild(fullname, mmlNm);
            if (fullName != null) {
                pvtClaim.setAssignedDoctorName(fullName.getTextTrim());
            }
        }
        
        // 施設情報 JMARI 4.0 から
        Element facility = psiInfo.getChild(Facility, mmlFc);
        pvtClaim.setJmariCode(facility.getChildTextTrim(Id, mmlCm));
        
        // 診療科情報
        Element dept = psiInfo.getChild(Department, mmlDp);
        pvtClaim.setClaimDeptName(dept.getChildTextTrim(name, mmlDp));
        pvtClaim.setClaimDeptCode(dept.getChildTextTrim(Id, mmlCm));
        
        // ClaimInfoを解析する
        Element claimModule = content.getChild(ClaimModule, claim);
        Element claimInfo = claimModule.getChild(information, claim);
        
        // status
        pvtClaim.setClaimStatus(claimInfo.getAttributeValue(status, claim));
        
        // registTime
        pvtClaim.setClaimRegistTime(claimInfo.getAttributeValue(registTime, claim));
        
        // admitFlag
        pvtClaim.setClaimAdmitFlag(claimInfo.getAttributeValue(admitFlag, claim));
        
        // insuranceUid
        pvtClaim.setInsuranceUid(claimInfo.getAttributeValue(insuranceUid, claim));
        
//pns^  「診療内容」情報受け取り
        Element appoint = claimInfo.getChild("appoint", claim);
        Element memo = appoint.getChild("memo", claim);
        pvtClaim.setClaimAppMemo(memo.getText().trim());
//pns$
        
        // DEBUG 出力
        debug("担当医ID = " + pvtClaim.getAssignedDoctorId());
        debug("担当医名 = " + pvtClaim.getAssignedDoctorName());
        debug("JMARI コード = " + pvtClaim.getJmariCode());
        debug("診療科名 = " + pvtClaim.getClaimDeptName());
        debug("診療科コード = " + pvtClaim.getClaimDeptCode());
        debug("status = " + pvtClaim.getClaimStatus());
        debug("registTime = " + pvtClaim.getClaimRegistTime());
        debug("admitFlag = " + pvtClaim.getClaimAdmitFlag());
        debug("insuranceUid = " + pvtClaim.getInsuranceUid());
    }
    
    private void debug(String msg) {
        if (DEBUG) {
            logger.info(msg);
        }
    }
}
