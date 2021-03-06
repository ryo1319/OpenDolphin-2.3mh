package open.dolphin.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import open.dolphin.infomodel.AllergyModel;
import open.dolphin.infomodel.IInfoModel;

/**
 * アレルギデータを編集するエディタクラス。
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 */
public class AllergyEditor {
    
    private final AllergyInspector inspector;
    private AllergyEditorView view;
    private final JDialog dialog;
    private final JButton addBtn;
    private final JButton clearBtn;
    private boolean ok;
    
    
    public AllergyEditor(AllergyInspector inspector) {
        
        this.inspector = inspector;
        view = new AllergyEditorView();
        view.getFactorFld().addFocusListener(AutoKanjiListener.getInstance());
        view.getFactorFld().getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                checkBtn();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                checkBtn();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                checkBtn();
            }
        });
        
        view.getMemoFld().addFocusListener(AutoKanjiListener.getInstance());
        
        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat(IInfoModel.DATE_WITHOUT_TIME);
        String todayString = sdf.format(date);
        view.getIdentifiedFld().setText(todayString);
        PopupCalendarListener pcl = new PopupCalendarListener(view.getIdentifiedFld());
        view.getIdentifiedFld().addFocusListener(AutoRomanListener.getInstance());
        
        addBtn = new JButton("追加");
        addBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                add();
            }
        });
        addBtn.setEnabled(false);
        
        clearBtn = new JButton("クリア");
        clearBtn.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clear();
            }
        });
        clearBtn.setEnabled(false);
                
        Object[] options = new Object[]{addBtn,clearBtn};
        
        JOptionPane pane = new JOptionPane(view,
                                           JOptionPane.PLAIN_MESSAGE,
                                           JOptionPane.DEFAULT_OPTION,
                                           null,
                                           options, addBtn);
        dialog = pane.createDialog(inspector.getContext().getFrame(), ClientContext.getFrameTitle("アレルギー登録"));
        dialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowOpened(WindowEvent e) {
                view.getFactorFld().requestFocusInWindow();
            }
        });
        dialog.setVisible(true);
    }
    
    private void checkBtn() {
        
        String factor = view.getFactorFld().getText().trim();
        String date = view.getIdentifiedFld().getText().trim();
        
        boolean newOk = true;
        if (factor.equals("") || date.equals("")) {
            newOk = false;
        }
        
        if (ok != newOk) {
            ok = newOk;
            addBtn.setEnabled(ok);
            clearBtn.setEnabled(ok);
        }
    }
    
    private void add() {
        
        final AllergyModel model = new AllergyModel();
        model.setFactor(view.getFactorFld().getText().trim());
        model.setSeverity((String) view.getReactionCombo().getSelectedItem());
        String memo = view.getMemoFld().getText().trim();
        if (!memo.equals("")) {
            model.setMemo(memo);
        }
        String dateStr = view.getIdentifiedFld().getText().trim();
        if (!dateStr.equals("")) {
            model.setIdentifiedDate(dateStr);
        }
        addBtn.setEnabled(false);
        clearBtn.setEnabled(false);
        inspector.add(model);
    }
    
    private void clear() {
        view.getFactorFld().setText("");
        view.getMemoFld().setText("");
        view.getIdentifiedFld().setText("");
    }
    
}
