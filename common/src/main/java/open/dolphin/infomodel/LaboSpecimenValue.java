package open.dolphin.infomodel;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.*;

/**
 * LaboSpecimenValue
 *
 * @author Minagawa,Kazushi
 * @author modifed by masuda, Masuda Naika
 */
@Entity
@Table(name = "d_labo_specimen")
public class LaboSpecimenValue implements Serializable {
    
    @JsonIgnore
    @Id @GeneratedValue(strategy=GenerationType.AUTO)
    private long id;
    
    @JsonBackReference  // bi-directional references
    @ManyToOne
    @JoinColumn(name="module_id", nullable=false)
    private LaboModuleValue laboModule;
    
    private String specimenName;
    
    private String specimenCode;
    
    private String specimenCodeId;
    
    @JsonManagedReference   // bi-directional references
    @JsonDeserialize(contentAs=LaboItemValue.class)
    @OneToMany(mappedBy="laboSpecimen", cascade={CascadeType.PERSIST, CascadeType.REMOVE})
    private List<LaboItemValue> laboItems;
    
    
    public LaboSpecimenValue() {
    }
    
    public long getId() {
        return id;
    }
    
    public void setId(long id) {
        this.id = id;
    }
    
    public LaboModuleValue getLaboModule() {
        return laboModule;
    }
    
    public void setLaboModule(LaboModuleValue laboModule) {
        this.laboModule = laboModule;
    }
    
    public List<LaboItemValue> getLaboItems() {
        return laboItems;
    }
    
    public void setLaboItems(List<LaboItemValue> laboItems) {
        this.laboItems = laboItems;
    }
    
    public void addLaboItem(LaboItemValue item) {
        if (laboItems == null) {
            laboItems = new ArrayList<LaboItemValue>();
        }
        laboItems.add(item);
    }
    
    public String getSpecimenCode() {
        return specimenCode;
    }
    
    public void setSpecimenCode(String specimenCode) {
        this.specimenCode = specimenCode;
    }
    
    public String getSpecimenCodeId() {
        return specimenCodeId;
    }
    
    public void setSpecimenCodeId(String specimenCodeId) {
        this.specimenCodeId = specimenCodeId;
    }
    
    public String getSpecimenName() {
        return specimenName;
    }
    
    public void setSpecimenName(String specimenName) {
        this.specimenName = specimenName;
    }
}
