package com.ukdw.rplbo.soal_ug_javafx;

import com.ukdw.rplbo.soal_ug_javafx.data.Mahasiswa_table;
import com.ukdw.rplbo.soal_ug_javafx.data.Matakuliah_table;
import com.ukdw.rplbo.soal_ug_javafx.data.Nilai_table;
import com.ukdw.rplbo.soal_ug_javafx.entity.Mahasiswa;
import com.ukdw.rplbo.soal_ug_javafx.entity.Matakuliah;
import com.ukdw.rplbo.soal_ug_javafx.entity.Nilai;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.*;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AppController {
    @FXML
    private ComboBox<String> option;
    @FXML
    private TableView<Object> table;
    @FXML
    private TableColumn<Object,String> column1;
    @FXML
    private TableColumn<Object,String> column2;
    @FXML
    private TableColumn<Object,String> column3;

    @FXML
    private BarChart<String, Number> barchart;
    @FXML
    private LineChart<String, Number> linechart;
    @FXML
    private PieChart piechart;


    Mahasiswa_table mhs_table = new Mahasiswa_table();
    Matakuliah_table mtkl_table = new Matakuliah_table();
    Nilai_table nilai_table = new Nilai_table();


    public AppController() throws SQLException {
    }

    @FXML
    public void initialize() throws SQLException {
        ObservableList<String> options = FXCollections.observableArrayList(
                "Mahasiswa",
                "Matakuliah"
        );
        option.setItems(options);
        option.setValue("Mahasiswa");

        option.valueProperty().addListener((observable, oldValue, newValue) -> {
            table.getItems().clear();

            if ("Matakuliah".equals(newValue)) {
                linechart.setVisible(true);
                column1.setText("kode_mk");
                column1.setCellValueFactory(new PropertyValueFactory<>("kode_mk"));
                column2.setText("nama");
                column2.setCellValueFactory(new PropertyValueFactory<>("nama"));


                column3.setText("sks");
                column3.setCellValueFactory(new PropertyValueFactory<>("sks"));

                table.setItems(FXCollections.observableArrayList(mtkl_table.fetch_all_matkul()));
            } else {
                linechart.setVisible(false);
                column1.setText("NIM");
                column1.setCellValueFactory(new PropertyValueFactory<>("NIM"));
                column2.setText("nama");
                column2.setCellValueFactory(new PropertyValueFactory<>("nama"));

                column3.setText(" ");
                column3.setCellValueFactory(null);

                table.setItems(FXCollections.observableArrayList(mhs_table.fetch_all_mahasiswa()));
            }
        });

        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {

                if (newSelection instanceof Mahasiswa) {
                    Mahasiswa m = (Mahasiswa) newSelection;
                    System.out.println("Clicked Mahasiswa: " + m.getNama() + " (" + m.getNIM() + ")");

                    // -- chart --
                    update_barchart("nim",m.getNIM());
                    update_piechart("nim",m.getNIM());


                } else if (newSelection instanceof Matakuliah) {
                    Matakuliah m = (Matakuliah) newSelection;
                    System.out.println("Clicked Mahasiswa: " + m.getNama() + " (" + m.getKode_mk() + ")");

                    // -- chart --
                    update_barchart("kode_mk",m.getKode_mk());
                    update_piechart("kode_mk",m.getKode_mk());
                    update_linechart(m.getKode_mk());
                }
            }
        });

        linechart.setVisible(false);
        column1.setText("NIM");
        column1.setCellValueFactory(new PropertyValueFactory<>("NIM"));
        column2.setText("nama");
        column2.setCellValueFactory(new PropertyValueFactory<>("nama"));
        column3.setText(" ");

        ObservableList<Object> data = FXCollections.observableArrayList(mhs_table.fetch_all_mahasiswa());
        table.setItems(data);

    }

    public void update_barchart(String target_col, String val) {
        List<Nilai> nilaiList = nilai_table.fetch_nilai_by(target_col, val);

        String[] grades = {"A", "A-", "B+", "B", "B-", "C+", "C", "D", "E"};
        Map<String, Integer> gradeCount = new LinkedHashMap<>();
        for (String g : grades) gradeCount.put(g, 0);

        for (Nilai n : nilaiList) {
            String grade = n.getNilai();
            if (gradeCount.containsKey(grade)) {
                gradeCount.put(grade, gradeCount.get(grade) + 1);
            }
        }

        barchart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Jumlah Nilai");
        for (Map.Entry<String, Integer> entry : gradeCount.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        barchart.getData().add(series);
    }

    public void update_linechart(String kode_mk) {
        List<Nilai> nilaiList = nilai_table.fetch_nilai_by("kode_mk", kode_mk);

        Map<String, Double> gradePoint = new LinkedHashMap<>();
        gradePoint.put("A",  4.0); gradePoint.put("A-", 3.7);
        gradePoint.put("B+", 3.3); gradePoint.put("B",  3.0);
        gradePoint.put("B-", 2.7); gradePoint.put("C+", 2.3);
        gradePoint.put("C",  2.0); gradePoint.put("D",  1.0);
        gradePoint.put("E",  0.0);

        Map<String, Double>  totalPoin = new LinkedHashMap<>();
        Map<String, Integer> count     = new LinkedHashMap<>();

        for (Nilai n : nilaiList) {
            // fetch entity Mahasiswa via nim pada record nilai
            Mahasiswa mhs = mhs_table.fetch_mahasiswa_by_nim(n.getNIM());
            if (mhs == null) continue;

            // angkatan diambil dari NIM: "71200001" -> substring(2,4)="20" -> "2020"
            // "71210005" -> substring(2,4)="21" -> "2021", dst.
            String angkatan = "20" + mhs.getNIM().substring(2, 4);

            double poin = gradePoint.getOrDefault(n.getNilai(), 0.0);
            totalPoin.put(angkatan, totalPoin.getOrDefault(angkatan, 0.0) + poin);
            count.put(angkatan,     count.getOrDefault(angkatan, 0) + 1);
        }

        linechart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Mean Nilai");

        List<String> angkatanSorted = new ArrayList<>(totalPoin.keySet());
        java.util.Collections.sort(angkatanSorted);

        for (String angkatan : angkatanSorted) {
            double mean = Math.round((totalPoin.get(angkatan) / count.get(angkatan)) * 100.0) / 100.0;
            series.getData().add(new XYChart.Data<>(angkatan, mean));
        }
        linechart.getData().add(series);
    }

    public void update_piechart(String target_col, String val) {
        List<Nilai> nilaiList = nilai_table.fetch_nilai_by(target_col, val);

        String[] grades = {"A", "A-", "B+", "B", "B-", "C+", "C", "D", "E"};
        Map<String, Integer> gradeCount = new LinkedHashMap<>();
        for (String g : grades) gradeCount.put(g, 0);

        for (Nilai n : nilaiList) {
            String grade = n.getNilai();
            if (gradeCount.containsKey(grade)) {
                gradeCount.put(grade, gradeCount.get(grade) + 1);
            }
        }

        ObservableList<PieChart.Data> pieData = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : gradeCount.entrySet()) {
            if (entry.getValue() > 0) {
                pieData.add(new PieChart.Data(
                        entry.getKey() + " (" + entry.getValue() + ")",
                        entry.getValue()
                ));
            }
        }
        piechart.setData(pieData);
    }
}