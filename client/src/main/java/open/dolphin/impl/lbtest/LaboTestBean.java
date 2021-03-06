package open.dolphin.impl.lbtest;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.swing.JTable.PrintMode;
import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.JTableHeader;
import open.dolphin.client.AbstractChartDocument;
import open.dolphin.client.ClientContext;
import open.dolphin.client.GUIConst;
import open.dolphin.delegater.LaboDelegater;
import open.dolphin.helper.DBTask;
import open.dolphin.infomodel.NLaboItem;
import open.dolphin.infomodel.NLaboModule;
import open.dolphin.infomodel.PatientModel;
import open.dolphin.infomodel.SampleDateComparator;
import open.dolphin.project.Project;
import open.dolphin.table.ListTableModel;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.Layer;

/**
 * LaboTestBean
 * 
 * @author Kazushi Minagawa, Digital Globe, Inc.
 * @author modified by masuda, Masuda Naika
 *
 */
public class LaboTestBean extends AbstractChartDocument {

    private static final String TITLE = "ラボ";
    private static final int DEFAULT_DIVIDER_LOC = 250;     //masuda 210 -> 250
    private static final int DEFAULT_DIVIDER_WIDTH = 10;
    private static final String COLUMN_HEADER_ITEM = "項 目";
    private static final String GRAPH_TITLE = "検査結果";
    private static final String X_AXIS_LABEL = "検体採取日";
    private static final String GRAPH_TITLE_LINUX = "Lab. Test";
    private static final String X_AXIS_LABEL_LINUX = "Sampled Date";
    private static final int FONT_SIZE_WIN = 12;
    private static final String FONT_MS_GOTHIC = "MSGothic";
    private static final int MAX_RESULT = 6;

    private ListTableModel<LabTestRowObject> tableModel;
    private JTable table;
    private JPanel graphPanel;

    private JTextField countField;
    private int dividerWidth;
    private int dividerLoc;

    // １回の検索で得る抽出件数
    private int maxResult = MAX_RESULT;

    private boolean widthAdjusted;
    
//masuda^
    private static final ImageIcon addIcon = ClientContext.getImageIconAlias("icon_add_small");
    private static final ImageIcon backIcon = ClientContext.getImageIconAlias("icon_back_small");
    private static final ImageIcon forwdIcon = ClientContext.getImageIconAlias("icon_forward_small");
    private int selectedColumn;
    
    private JTextField tf_period;
    private int firstResult = 0;
//masuda$
    
    public LaboTestBean() {
        setTitle(TITLE);
    }

    public int getMaxResult() {
        return maxResult;
    }

    public void setMaxResult(int maxResult) {
        this.maxResult = maxResult;
    }

//masuda^   検体種別を表示したいのと要望    
    public void createTable(List<NLaboModule> modules) {

        // 現在のデータをクリアする
        if (tableModel != null && tableModel.getDataProvider() != null) {
            tableModel.getDataProvider().clear();
        }

        // グラフもクリアする
        graphPanel.removeAll();
        graphPanel.revalidate();

        // Table のカラムヘッダーを生成する
        String[] header = new String[getMaxResult() + 1];
        header[0] = COLUMN_HEADER_ITEM;
        for (int col = 1; col < header.length; col++) {
            header[col] = "";
        }

        // 結果がゼロであれば返る
        if (modules == null || modules.isEmpty()) {
            tableModel = new ListTableModel<>(header, 0);
            table.setModel(tableModel);
            setColumnWidth();
            return;
        }
        
        // 検体採取日の降順なので昇順にソートする
        Collections.sort(modules, new SampleDateComparator());

        List<LabTestRowObject> bloodExams = new ArrayList<>();
        List<LabTestRowObject> urineExams = new ArrayList<>();
        List<LabTestRowObject> otherExams = new ArrayList<>();

        int moduleIndex = 0;

        for (NLaboModule module : modules) {

            // 検体採取日
            if(module.getSampleDate().length()==12){
                // FALCOの日付データはyyyymmddhhmm形式でDBに登録されているので、
                // DBに登録されたデータを他データの形式に併せて出力する (苦肉の策)
                // 2013/1/19 FALCOのlabocentercodeは"FALCO"でないことがあるため、
                // labocentercodeを判断条件から除外
                SimpleDateFormat df12 = new SimpleDateFormat("yyyyMMddhhmm");
                SimpleDateFormat dfDef= new SimpleDateFormat("yyyy-MM-dd hh:mm");
                try{
                    header[moduleIndex + 1] = dfDef.format(df12.parse(module.getSampleDate()));
                }
                catch(Exception e){
                    header[moduleIndex + 1] = module.getSampleDate();
                }
            }
            else{
                header[moduleIndex + 1] = module.getSampleDate();
            }
            //header[moduleIndex + 1] = module.getSampleDate();
            // 画面がちっちゃいと日付が見えないらしい
            header[moduleIndex + 1] = header[moduleIndex + 1].substring(2);
            for (NLaboItem item : module.getItems()) {

                // 検体名を取得する
                String specimenName = item.getSpecimenName();
                // 検体で分類してリストを選択する
                List<LabTestRowObject> rowObjectList;
                if (specimenName != null) {     // null check 橋本先生のご指摘
                    if (specimenName.contains("血")) {
                        rowObjectList = bloodExams;
                    } else if (specimenName.contains("尿") || specimenName.contains("便")) {
                        rowObjectList = urineExams;
                    } else {
                        rowObjectList = otherExams;
                    }
                } else {
                    rowObjectList = otherExams;
                }

                boolean found = false;

                for (LabTestRowObject rowObject : rowObjectList) {
                	// 20130206 katou
                	// HbA1c NGSPとJDSが共存していると表示がおかしくなる問題対応
                    // if (item.getItemCode().equals(rowObject.getItemCode())) {
                    if ((item.getItemCode().equals(rowObject.getItemCode()) &&
                        (item.getItemName().equals(rowObject.getItemName())))) {
                        found = true;
                        LabTestValueObject value = new LabTestValueObject();
                        value.setSampleDate(module.getSampleDate());
                        value.setValue(item.getValue());
                        value.setOut(item.getAbnormalFlg());
                        value.setComment1(item.getComment1());
                        value.setComment2(item.getComment2());
                        rowObject.addLabTestValueObjectAt(moduleIndex, value);
                        rowObject.setNormalValue(item.getNormalValue());    // 基準値記録漏れ対策
                        break;
                    }
                }

                if (!found) {
                    LabTestRowObject row = new LabTestRowObject();
                    row.setLabCode(item.getLaboCode());
                    row.setGroupCode(item.getGroupCode());
                    row.setParentCode(item.getParentCode());
                    row.setItemCode(item.getItemCode());
                    row.setItemName(item.getItemName());
                    row.setUnit(item.getUnit());
                    row.setNormalValue(item.getNormalValue());
                    //
                    LabTestValueObject value = new LabTestValueObject();
                    value.setSampleDate(module.getSampleDate());
                    value.setValue(item.getValue());
                    value.setOut(item.getAbnormalFlg());
                    value.setComment1(item.getComment1());
                    value.setComment2(item.getComment2());
                    row.addLabTestValueObjectAt(moduleIndex, value);
                    //
                    rowObjectList.add(row);
                }
            }

            moduleIndex++;
        }
        
        List<LabTestRowObject> dataProvider = new ArrayList<>();
        
        if (!bloodExams.isEmpty()) {
            Collections.sort(bloodExams);
            LabTestRowObject specimen = new LabTestRowObject();
            specimen.setSpecimenName("血液検査");
            bloodExams.add(0, specimen);
            dataProvider.addAll(bloodExams);
        }
        if (!urineExams.isEmpty()) {
            Collections.sort(urineExams);
            LabTestRowObject specimen = new LabTestRowObject();
            specimen.setSpecimenName("尿・便");
            urineExams.add(0, specimen);
            dataProvider.addAll(urineExams);
        }
        if (!otherExams.isEmpty()) {
            Collections.sort(otherExams);
            LabTestRowObject specimen = new LabTestRowObject();
            specimen.setSpecimenName("その他");
            otherExams.add(0, specimen);
            dataProvider.addAll(otherExams);
        }

        // Table Model
        tableModel = new ListTableModel<>(header, 0);

        // 検査結果テーブルを生成する
        table.setModel(tableModel);
        setColumnWidth();

        // dataProvider
        tableModel.setDataProvider(dataProvider);
        
//masuda^   カラムにNLaboModuleを設定しておく
        for (int i = 0; i < modules.size(); ++i) {
            table.getColumnModel().getColumn(i + 1).setIdentifier(modules.get(i));
        }
//masuda$
    }
    
    // 印刷
    @Override
    public void print() {

        PatientModel pm = getContext().getPatient();
        StringBuilder sb = new StringBuilder();
        sb.append(pm.getPatientId());
        sb.append(" ");
        sb.append(pm.getFullName());
        sb.append(" - ");
        sb.append(Project.getUserModel().getFacilityModel().getFacilityName());
        String footer = sb.toString();

        // 印刷不具合対策。quaquaが原因じゃなかった？
        JTableHeader header = table.getTableHeader();
        Font tableFont = table.getFont();
        Font headerFont = header.getFont();
        table.setFont(new Font(Font.MONOSPACED, tableFont.getStyle(), tableFont.getSize()));
        header.setFont(new Font(Font.MONOSPACED, headerFont.getStyle(), headerFont.getSize()));
        try {
            table.print(PrintMode.FIT_WIDTH, null, new MessageFormat(footer));
        } catch (PrinterException ex) {
        }
        table.setFont(tableFont);
        header.setFont(headerFont);
    }
    
    @Override
    public void enter() {
        super.enter();
        getContext().enabledAction(GUIConst.ACTION_PRINT, true);
    }
//masuda$


    /**
     * Tableのカラム幅を調整する。
     */
    private void setColumnWidth() {
        // カラム幅を調整する
        if (!widthAdjusted) {
            table.getTableHeader().getColumnModel().getColumn(0).setPreferredWidth(180);
            table.getTableHeader().getColumnModel().getColumn(1).setPreferredWidth(100);
            table.getTableHeader().getColumnModel().getColumn(2).setPreferredWidth(100);
            table.getTableHeader().getColumnModel().getColumn(3).setPreferredWidth(100);
            table.getTableHeader().getColumnModel().getColumn(4).setPreferredWidth(100);
            table.getTableHeader().getColumnModel().getColumn(5).setPreferredWidth(100);
            widthAdjusted = true;
        }
    }

    /**
     * GUIコンポーネントを初期化する。
     */
    private void initialize() {

        // Divider
        dividerWidth = DEFAULT_DIVIDER_WIDTH;
        dividerLoc = DEFAULT_DIVIDER_LOC;

        JPanel controlPanel = createControlPanel();

        graphPanel = new JPanel(new BorderLayout());
        graphPanel.setPreferredSize(new Dimension(500, dividerLoc));

        // 検査結果テーブルを生成する
        table = new JTable();

//masuda^
        // 行高
        //table.setRowHeight(ClientContext.getHigherRowHeight());
        
        // Rendererを設定する
        //table.setDefaultRenderer(Object.class, new LabTestRenderer());
        LabTestRenderer renderer = new LabTestRenderer();
        renderer.setTable(table);
        renderer.setDefaultRenderer();
//masuda$
        
        // 行選択を可能にする
        table.setRowSelectionAllowed(true);

        //-----------------------------------------------
        // Copy 機能を実装する
        //-----------------------------------------------
        KeyStroke copy = KeyStroke.getKeyStroke(KeyEvent.VK_C, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask());
        final AbstractAction copyAction = new AbstractAction("コピー") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                copyRow();
            }
        };
        
        final AbstractAction copyLatestAction = new AbstractAction("直近の結果のみコピー") {

            @Override
            public void actionPerformed(ActionEvent ae) {
                copyLatest();
            }
        };
        
//masuda^   削除
        final AbstractAction deleteAction = new AbstractAction("削除") {
            @Override
            public void actionPerformed(ActionEvent ae) {
                deleteColumnData();
            }
        };
//masuda$

        table.getInputMap().put(copy, "Copy");
        table.getActionMap().put("Copy", copyLatestAction);

        table.addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent me) {
                mabeShowPopup(me);
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                mabeShowPopup(me);
            }

            public void mabeShowPopup(MouseEvent e) {

                if (!e.isPopupTrigger()) {
                    return;
                }

                int row = table.rowAtPoint(e.getPoint());

                if (row < 0 ) {
                    return;
                }

                JPopupMenu contextMenu = new JPopupMenu();
                contextMenu.add(new JMenuItem(copyLatestAction));
                contextMenu.add(new JMenuItem(copyAction));
//masuda^   削除
                if (e.isShiftDown()) {
                    contextMenu.add(new JMenuItem(deleteAction));
                }
                selectedColumn = table.columnAtPoint(e.getPoint());
//masuda$
                contextMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        });

        // グラフ表示のリスナを登録する
        ListSelectionModel m = table.getSelectionModel();
        m.addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (e.getValueIsAdjusting() == false) {
                    createAndShowGraph(table.getSelectedRows());
                }
            }
        });

        JScrollPane jScrollPane1 = new JScrollPane();
        jScrollPane1.setViewportView(table);
        jScrollPane1.setPreferredSize(new java.awt.Dimension(3, 600));

        JPanel tablePanel = new JPanel(new BorderLayout(0, 7));
        tablePanel.add(controlPanel, BorderLayout.SOUTH);
        tablePanel.add(jScrollPane1, BorderLayout.CENTER);

        // Lyouts
        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, graphPanel, tablePanel);
        splitPane.setDividerSize(dividerWidth);
        splitPane.setContinuousLayout(false);
        splitPane.setOneTouchExpandable(true);
        splitPane.setDividerLocation(dividerLoc);

        getUI().setLayout(new BorderLayout());
        getUI().add(splitPane, BorderLayout.CENTER);

        getUI().setBorder(BorderFactory.createEmptyBorder(12, 12, 11, 11));
    }

    @Override
    public void start() {
        initialize();
        searchLaboTest();
        enter();
    }

    @Override
    public void stop() {
        if (tableModel != null && tableModel.getDataProvider() != null) {
            tableModel.getDataProvider().clear();
        }
    }
    
//masuda^   データ削除
    private void deleteColumnData() {
        
        try {
            // 選択中のデータを取得
            NLaboModule nlab = (NLaboModule) table.getColumnModel().getColumn(selectedColumn).getIdentifier();
            
            final long id = nlab.getId();
            final String frmt = nlab.getReportFormat();
//katoh^
            if (id == 0) {// || frmt == null) {
                return;
            }
//katoh$
            // 削除確認
            String dateStr = nlab.getSampleDate();
            Toolkit.getDefaultToolkit().beep();
            String[] options = {"取消", "削除"};
            String msg = dateStr + "の検査データを削除しますか？";
            int val = JOptionPane.showOptionDialog(getContext().getFrame(), msg, "検査削除",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
            if (val != 1) {
                // 取り消し
                return;
            }
            
            // データベースから削除
            DBTask task = new DBTask<Void, Void>(getContext()) {

                @Override
                protected Object doInBackground() throws Exception {
                    LaboDelegater del = LaboDelegater.getInstance();
                    if ("MML".equals(frmt)) {
                        del.deleteMmlLaboModule(id);
                    } else {
                        del.deleteNlaboModule(id);
                    }
                    return null;
                }

                @Override
                protected void succeeded(Void result) {
                    // 再表示
                    searchLaboTest();
                }
            };
            task.execute();
            
        } catch (HeadlessException ex) {
        }
    }
//masuda$

    /**
     * 選択されている行で直近のデータをコピーする。
     */
    public void copyLatest() {
        StringBuilder sb = new StringBuilder();
        int numRows=table.getSelectedRowCount();
        int[] rowsSelected=table.getSelectedRows();
        for (int i = 0; i < numRows; i++) {
            LabTestRowObject rdm = tableModel.getObject(rowsSelected[i]);
            if (rdm != null) {
                sb.append(rdm.toClipboardLatest()).append("\n");
            }
        }
        if (sb.length() > 0) {
            StringSelection stsel = new StringSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
        }
    }

    /**
     * 選択されている行をコピーする。
     */
    public void copyRow() {
        StringBuilder sb = new StringBuilder();
        int numRows=table.getSelectedRowCount();
        int[] rowsSelected=table.getSelectedRows();
        for (int i = 0; i < numRows; i++) {
            LabTestRowObject rdm = tableModel.getObject(rowsSelected[i]);
            if (rdm != null) {
                sb.append(rdm.toClipboard()).append("\n");
            }
        }
        if (sb.length() > 0) {
            StringSelection stsel = new StringSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(stsel, stsel);
        }
    }

    /**
     * LaboTest の検索タスクをコールする。
     */
    //private void searchLaboTest(final int firstResult) {
    private void searchLaboTest() {
        
        final String pid = getContext().getPatient().getPatientId();

        DBTask task = new DBTask<List<NLaboModule>, Void>(getContext()) {

            @Override
            protected List<NLaboModule> doInBackground() throws Exception {

                List<NLaboModule> modules = LaboDelegater.getInstance()
                        .getLaboTest(pid, firstResult, getMaxResult());
                return modules;
            }

            @Override
            protected void succeeded(List<NLaboModule> modules) {
                int moduleCount = modules != null ? modules.size() : 0;
                countField.setText(String.valueOf(moduleCount));
                createTable(modules);
                updateTfPeriod();
            }
        };

        task.execute();

    }

    /**
     * 検査結果テーブルで選択された行（検査項目）の折れ線グラフを生成する。
     * 複数選択対応
     * JFreeChart を使用する。
     */
    private void createAndShowGraph(int[] selectedRows) {

        if (selectedRows == null || selectedRows.length == 0) {
            return;
        }
        
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // 選択されている行（検査項目）をイテレートし、dataset へ値を設定する
        for (int cnt = 0; cnt < selectedRows.length; cnt++) {

            int row = selectedRows[cnt];
            List<LabTestRowObject> dataProvider = tableModel.getDataProvider();
            LabTestRowObject rowObj = dataProvider.get(row);
            
//masuda^   検体名の行はスキップ
            if (rowObj.getSpecimenName() != null) {
                continue;
            }
//masuda$
            List<LabTestValueObject> values = rowObj.getValues();

            boolean valueIsNumber = true;
            
            // 検体採取日ごとの値を設定する
            // カラムの１番目から採取日がセットされている
//masuda^   最後の検査が表示されない
            //for (int col = 1; col < getMaxResult(); col++) {
            for (int col = 1; col <= getMaxResult(); col++) {
//masuda$
                String sampleTime = tableModel.getColumnName(col);

                // 検体採取日="" -> 検査なし
                if (sampleTime.equals("")) {
                    break;
                }

                LabTestValueObject value = values.get(col -1);

//masuda^   中止された時などvalueがnullのときがある。そんなときはnull値にする。
                try {
                    double val = Double.parseDouble(value.getValue());
                    dataset.setValue(val, rowObj.nameWithUnit(), sampleTime);
                } catch (NumberFormatException | NullPointerException e) {
                    dataset.setValue(null, rowObj.nameWithUnit(), sampleTime);
                }
            }
//masuda$
        }

        JFreeChart chart = ChartFactory.createLineChart(
//masuda^   グラフタイトルと検体検査日ラベルはなくしてグラフを大きくする
                    //getGraphTitle(),                // Title
                    //getXLabel(),                    // x-axis Label
                    null, null,
//masuda$
                    "",                             // y-axis Label
                    dataset,                        // Dataset
                    PlotOrientation.VERTICAL,       // Plot Orientation
                    true,                           // Show Legend
                    true,                           // Use tooltips
                    false                           // Configure chart to generate URLs?
                    );

        // Win の文字化け
        if (ClientContext.isWin()) {
//masuda^   タイトルをなくしたので
            //chart.getTitle().setFont(getWinFont());
//masuda$
            chart.getLegend().setItemFont(getWinFont());
            chart.getCategoryPlot().getDomainAxis().setLabelFont(getWinFont());
            chart.getCategoryPlot().getDomainAxis().setTickLabelFont(getWinFont());
        }
        
//masuda^
        // 背景色を設定 薄くする
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(new Color(220, 220, 220));
        // グラフにドットをつける
        IgnoreNullLineRenderer renderer = new IgnoreNullLineRenderer();
        plot.setRenderer(renderer);
        // 選択した項目が一つならば基準範囲を表示する
        if (selectedRows.length == 1) {
            List<LabTestRowObject> dataProvider = tableModel.getDataProvider();
            LabTestRowObject rowObj = dataProvider.get(selectedRows[0]);
            String normalValue = rowObj.getNormalValue();
            try {
                String[] values = normalValue.split("-");
                float low = Float.valueOf(values[0]);
                float hi = Float.valueOf(values[1]);
                IntervalMarker marker = new IntervalMarker(low, hi);
                marker.setPaint(new Color(200, 230, 200));
                plot.addRangeMarker(marker, Layer.BACKGROUND);
            } catch (NullPointerException | NumberFormatException e) {
            }
        }
//masuda$
        
        ChartPanel chartPanel = new ChartPanel(chart);

        graphPanel.removeAll();
        graphPanel.add(chartPanel, BorderLayout.CENTER);
        graphPanel.validate();
    }

    //====================================================================
    private String getGraphTitle() {
        return ClientContext.isLinux() ? GRAPH_TITLE_LINUX : GRAPH_TITLE;
    }

    private String getXLabel() {
        return ClientContext.isLinux() ? X_AXIS_LABEL_LINUX : X_AXIS_LABEL;
    }

    private Font getWinFont() {
        return new Font(FONT_MS_GOTHIC, Font.PLAIN, FONT_SIZE_WIN);
    }
    //====================================================================

    /**
     * 抽出期間パネルを返す
     */
    private JPanel createControlPanel() {
        
/*
        String[] menu = getExtractionMenu();
        int cnt = menu.length / 2;
        NameValuePair[] periodObject = new NameValuePair[cnt];
        int valIndex = 0;
        for (int i = 0; i < cnt; i++) {
            periodObject[i] = new NameValuePair(menu[valIndex], menu[valIndex+1]);
            valIndex += 2;
        }

        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(Box.createHorizontalStrut(7));

        // 抽出期間コンボボックス
        p.add(new JLabel("過去"));
        p.add(Box.createRigidArea(new Dimension(5, 0)));
        extractionCombo = new JComboBox(periodObject);

        extractionCombo.addItemListener(new ItemListener() {

            @Override
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                    NameValuePair pair = (NameValuePair) extractionCombo.getSelectedItem();
                    int firstResult = Integer.parseInt(pair.getValue());
                    searchLaboTest(firstResult);
                }
            }
        });
        JPanel comboPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        comboPanel.add(extractionCombo);
*/
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.X_AXIS));
        p.add(Box.createHorizontalStrut(7));

        JButton backBtn = new JButton(backIcon);
        backBtn.addActionListener(new ActionListener(){
            
            @Override
            public void actionPerformed(ActionEvent e) {
                firstResult += getMaxResult();
                searchLaboTest();
            }
        });
        p.add(backBtn);

        tf_period = new JTextField();
        tf_period.setEditable(false);
        updateTfPeriod();
        Dimension d = tf_period.getPreferredSize();
        tf_period.setMaximumSize(d);
        p.add(tf_period);
        
        JButton forwdBtn = new JButton(forwdIcon);
        forwdBtn.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                int old = firstResult;
                firstResult = Math.max(0, firstResult - getMaxResult());
                if (old != firstResult) {
                    searchLaboTest();
                }
            }
        });
        p.add(forwdBtn);
        p.add(Box.createHorizontalStrut(10));
        
        // 院内検査登録ボタン
        JButton addBtn = new JButton("院内検査追加", addIcon);
        addBtn.addActionListener(new ActionListener(){

            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    InFacilityLabo fLabo = new InFacilityLabo();
                    fLabo.setContext(LaboTestBean.this.getContext());
                    boolean toUpdate = fLabo.start();
                    if (toUpdate) {
                        searchLaboTest();
                    }
                } catch (Exception ex) {
                }
            }
        });
        p.add(addBtn);
//masuda$
        
        // グル
        p.add(Box.createHorizontalGlue());

        // 件数フィールド
        p.add(new JLabel("件数"));
        p.add(Box.createRigidArea(new Dimension(5, 0)));
        countField = new JTextField(2);
        countField.setEditable(false);
        JPanel countPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        countPanel.add(countField);
        p.add(countPanel);

        // スペース
        p.add(Box.createHorizontalStrut(7));

        return p;
    }
    
    private void updateTfPeriod() {
        StringBuilder sb = new StringBuilder();
        sb.append("過去:");
        sb.append(String.valueOf(firstResult + 1));
        sb.append(" - ");
        sb.append(String.valueOf(firstResult + getMaxResult()));
        sb.append("件");
        tf_period.setText(sb.toString());
    }
}
