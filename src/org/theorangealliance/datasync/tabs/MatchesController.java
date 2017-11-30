package org.theorangealliance.datasync.tabs;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableColumn;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import org.theorangealliance.datasync.DataSyncController;
import org.theorangealliance.datasync.json.MatchGeneralJSON;
import org.theorangealliance.datasync.models.MatchGeneral;
import org.theorangealliance.datasync.util.Config;
import org.theorangealliance.datasync.util.TOAEndpoint;
import org.theorangealliance.datasync.util.TOARequestBody;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Optional;

/**
 * Created by Kyle Flynn on 11/29/2017.
 */
public class MatchesController {

    private DataSyncController controller;
    private ObservableList<MatchGeneral> matchList;

    public MatchesController(DataSyncController instance) {
        this.controller = instance;

        this.controller.colMatchName.setCellValueFactory(new PropertyValueFactory<MatchGeneral, String>("matchName"));
        this.controller.colMatchDone.setCellValueFactory(new PropertyValueFactory<MatchGeneral, Boolean>("isDone"));
        this.controller.colMatchPosted.setCellValueFactory(new PropertyValueFactory<MatchGeneral, Boolean>("isUploaded"));
        this.controller.colMatchDone.setCellFactory(CheckBoxTableCell.forTableColumn(this.controller.colMatchDone));
        this.controller.colMatchPosted.setCellFactory(CheckBoxTableCell.forTableColumn(this.controller.colMatchPosted));

        this.matchList = FXCollections.observableArrayList();
        this.controller.tableMatches.setItems(this.matchList);

        this.controller.btnMatchScheduleUpload.setDisable(true);
    }

    public void getMatchesByFile() {
        File matchFile = new File(Config.SCORING_DIR + "\\matches.txt");
        if (matchFile.exists()) {
            try {
                matchList.clear();
                BufferedReader reader = new BufferedReader(new FileReader(matchFile));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] matchInfo = line.split("\\|\\|")[0].split("\\|");
                    int tournamentLevel = Integer.parseInt(matchInfo[1]);
                    int matchNumber = Integer.parseInt(matchInfo[2]);
                    int fieldNumber = matchNumber % 2 == 0 ? 2 : 1;
                    MatchGeneral match = new MatchGeneral(
                            MatchGeneral.buildMatchName(tournamentLevel, matchNumber),
                            MatchGeneral.buildTOATournamentLevel(tournamentLevel, matchNumber),
                            null,
                            fieldNumber);
                    matchList.add(match);
                }
                reader.close();
                controller.btnMatchScheduleUpload.setDisable(false);
                controller.sendInfo("Successfully imported " + matchList.size() + " matches from the Scoring System.");
            } catch (Exception e) {
                e.printStackTrace();
                controller.sendError("Could not open file. " + e.getLocalizedMessage());
            }
        } else {
            controller.sendError("Could not locate matches.txt from the Scoring System. Did you generate a match schedule?");
        }
    }

    public void postMatchSchedule() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Are you sure about this?");
        alert.setHeaderText("This operation cannot be undone.");
        alert.setContentText("You are about to POST " + matchList.size() + " matches to TOA's databases. This schedule will be made available to the public. Make sure this schedule is correct before you upload.");

        ButtonType okayButton = new ButtonType("Upload Matches");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(okayButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == okayButton) {
            controller.sendInfo("Uploading data from event " + Config.EVENT_ID + "...");
            TOAEndpoint scheduleEndpoint = new TOAEndpoint("POST", "upload/event/schedule/matches");
            scheduleEndpoint.setCredentials(Config.EVENT_API_KEY, Config.EVENT_ID);
            TOARequestBody requestBody = new TOARequestBody();
            requestBody.setEventKey(Config.EVENT_ID);
            for (int i = 0; i < matchList.size(); i++) {
                MatchGeneral match = matchList.get(i);
                MatchGeneralJSON matchJSON = new MatchGeneralJSON();
                matchJSON.setEventKey(Config.EVENT_ID);
                char qualChar = match.getMatchName().contains("Qual") ? 'Q' : 'E';
                matchJSON.setMatchKey(Config.EVENT_ID + "-" + qualChar + String.format("%03d", match.getCanonicalMatchNumber()) + "-1");
                matchJSON.setTournamentLevel(match.getTournamentLevel());
                matchJSON.setMatchName(match.getMatchName());
                matchJSON.setPlayNumber(0);
                matchJSON.setFieldNumber(match.getFieldNumber());
                matchJSON.setCreatedBy("TOA-DataSync");
                matchJSON.setCreatedOn(getCurrentTime());
                requestBody.addValue(matchJSON);
            }
            scheduleEndpoint.setBody(requestBody);
            scheduleEndpoint.execute(((response, success) -> {
                if (success) {
                    controller.sendInfo("Successfully uploaded data to TOA. " + response);
                } else {
                    controller.sendError("Connection to TOA unsuccessful. " + response);
                }
            }));
        }
    }

    private String getCurrentTime() {
        Date dt = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return sdf.format(dt);
    }

}
