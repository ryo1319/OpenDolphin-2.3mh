package open.dolphin.impl.pacsviewer;

import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.SpecificCharacterSet;
import org.dcm4che2.data.Tag;

/**
 * listTableに保存するobject
 *
 * @author masuda, Masuda Naika
 */
public class ListDicomObject implements Comparable {

    private DicomObject object;
    private String ptId;
    private String ptName;
    private String ptSex;
    private String ptBirthDate;
    private String modalities;
    private String description;
    private String studyDate;
    private String numberOfImage;
    private SpecificCharacterSet charSet;

    public ListDicomObject(DicomObject obj) {
        object = obj;
        charSet = obj.getSpecificCharacterSet();
        ptId = getString(Tag.PatientID);
        ptName = getString(Tag.PatientName).replace("^", " ");
        ptSex = getString(Tag.PatientSex);
        ptBirthDate = getString(Tag.PatientBirthDate);
        modalities = getString(Tag.ModalitiesInStudy);
        description = getString(Tag.StudyDescription);
        studyDate = getString(Tag.StudyDate);
        numberOfImage = getString(Tag.NumberOfStudyRelatedInstances);
    }
    
    // 橋本医院　加藤さま
    // 【1. Dicomサーバに患者名=""の画像があっても画像を表示出来るように修正】からパクリ
    private String getString(int tag) {
        if (object == null) {
            return "";
        }
        String str;
        if (charSet == null) {
            str = object.getString(tag);
        } else {
            str = charSet.decode(object.getBytes(tag));
        }
        return (str == null) ? "" : str;
    }

    public DicomObject getDicomObject() {
        return object;
    }

    public String getPtId() {
        return ptId;
    }

    public String getPtName() {
        return ptName;
    }

    public String getPtSex() {
        return ptSex;
    }

    public String getPtBirthDate() {
        return ptBirthDate;
    }

    public String getModalities() {
        return modalities;
    }

    public String getDescription() {
        return description;
    }

    public String getStudyDate() {
        return studyDate;
    }

    public String getNumberOfImage() {
        return numberOfImage;
    }

    @Override
    public int compareTo(Object o) {
        int sDate = Integer.parseInt(studyDate);
        ListDicomObject test = (ListDicomObject) o;
        int tDate = Integer.parseInt(test.getStudyDate());
        if (sDate == tDate) {
            return 0;
        } else if (sDate > tDate) {
            return 1;
        } else {
            return -1;
        }
    }
}
