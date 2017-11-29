package org.theorangealliance.datasync.tabs;

import com.google.gson.annotations.SerializedName;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.cell.PropertyValueFactory;
import org.theorangealliance.datasync.DataSyncController;
import org.theorangealliance.datasync.json.EventParticipantJSON;
import org.theorangealliance.datasync.json.TeamJSON;
import org.theorangealliance.datasync.models.Team;
import org.theorangealliance.datasync.util.Config;
import org.theorangealliance.datasync.util.TOAEndpoint;
import org.theorangealliance.datasync.util.TOARequestBody;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.Optional;

/**
 * Created by Kyle Flynn on 11/29/2017.
 */
public class TeamsController {

    private DataSyncController controller;
    private ObservableList<Team> teamsList;

    public TeamsController(DataSyncController instance) {
        this.controller = instance;
        this.controller.colTeamsTeam.setCellValueFactory(new PropertyValueFactory<Team, Integer>("teamKey"));
        this.controller.colTeamsRegion.setCellValueFactory(new PropertyValueFactory<Team, String>("regionKey"));
        this.controller.colTeamsLeague.setCellValueFactory(new PropertyValueFactory<Team, String>("leagueKey"));
        this.controller.colTeamsShort.setCellValueFactory(new PropertyValueFactory<Team, String>("teamNameShort"));
        this.controller.colTeamsLong.setCellValueFactory(new PropertyValueFactory<Team, String>("teamNameLong"));
        this.controller.colTeamsLocation.setCellValueFactory(new PropertyValueFactory<Team, String>("location"));

        this.teamsList = FXCollections.observableArrayList();
        this.controller.tableTeams.setItems(this.teamsList);

        this.controller.btnTeamsPost.setDisable(true);
        this.controller.btnTeamsDelete.setDisable(true);
    }

    public void getTeamsByURL() {
        TOAEndpoint teamsEndpoint = new TOAEndpoint("event/" + Config.EVENT_ID + "/teams");
        teamsEndpoint.setCredentials(Config.EVENT_API_KEY, Config.EVENT_ID);
        teamsEndpoint.execute(((response, success) -> {
           if (success) {
               teamsList.clear();
               controller.sendInfo("Successfully pulled teams for event " + Config.EVENT_ID);
               TeamJSON[] teams = teamsEndpoint.getGson().fromJson(response, TeamJSON[].class);
               for (TeamJSON team : teams) {
                   Team eventTeam = new Team(
                           team.getTeamKey(),
                           team.getRegionKey(),
                           team.getLeagueKey(),
                           team.getTeamNameShort(),
                           team.getTeamNameLong(),
                           team.getLocation());
                   teamsList.add(eventTeam);
               }

               teamsList.sort((team1, team2) -> (team1.getTeamKey() > team2.getTeamKey() ? 1 : -1));

               controller.btnTeamsPost.setDisable(false);
               controller.btnTeamsDelete.setDisable(false);
           } else {
               controller.sendError("Connection to TOA unsuccessful. " + response);
               controller.btnTeamsPost.setDisable(true);
               controller.btnTeamsDelete.setDisable(true);
           }
        }));
    }

    public void postEventTeams() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Are you sure about this?");
        alert.setHeaderText("This operation cannot be undone.");
        alert.setContentText("You are about to POST " + teamsList.size() + " teams to TOA's databases. Make sure your team list is correct before uploading.");

        ButtonType okayButton = new ButtonType("Upload Teams");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(okayButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == okayButton){
            // Begin the purging of the data table...
            controller.sendInfo("Uploading data from event " + Config.EVENT_ID + "...");
            TOAEndpoint deleteEndpoint = new TOAEndpoint("POST", "upload/event/teams");
            deleteEndpoint.setCredentials(Config.EVENT_API_KEY, Config.EVENT_ID);
            TOARequestBody requestBody = new TOARequestBody();
            requestBody.setEventKey(Config.EVENT_ID);
            for (int i = 0; i < teamsList.size(); i++) {
                Team team = teamsList.get(i);
                EventParticipantJSON eventTeam = new EventParticipantJSON();
                // TEAM LIST NEEDS TO BE SORTED FOR THIS TO WORK PROPERLY
                eventTeam.setParticipantKey(Config.EVENT_ID + "-T" + (i+1));
                eventTeam.setTeamKey(team.getTeamKey());
                eventTeam.setEventKey(Config.EVENT_ID);
                eventTeam.setIsActive(1);
                eventTeam.setAddedFromUI(1);
                eventTeam.setCreatedBy("TOA-DataSync");
                eventTeam.setCreatedOn(getCurrentTime());
                requestBody.addValue(eventTeam);
            }
            deleteEndpoint.setBody(requestBody);
            deleteEndpoint.execute((response, success) -> {
                if (success) {
                    controller.sendInfo("Successfully uploaded data to TOA. " + response);
                } else {
                    controller.sendError("Connection to TOA unsuccessful. " + response);
                }
            });
        }
    }

    public void deleteEventTeams() {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Are you sure about this?");
        alert.setHeaderText("This operation cannot be undone.");
        alert.setContentText("You are about to purge TOA's EventParticipant data for EventID " + Config.EVENT_ID + ". We don't recommend doing this unless our databases are wrong and you have teams present/not present at your event.");

        ButtonType okayButton = new ButtonType("Purge Anyway");
        ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        alert.getButtonTypes().setAll(okayButton, cancelButton);

        Optional<ButtonType> result = alert.showAndWait();
        if (result.get() == okayButton){
            // Begin the purging of the data table...
            controller.sendInfo("Purging data from event " + Config.EVENT_ID + "...");
            TOAEndpoint deleteEndpoint = new TOAEndpoint("DELETE", "upload/event/teams");
            deleteEndpoint.setCredentials(Config.EVENT_API_KEY, Config.EVENT_ID);
            TOARequestBody requestBody = new TOARequestBody();
            requestBody.setEventKey(Config.EVENT_ID);
            deleteEndpoint.setBody(requestBody);
            deleteEndpoint.execute((response, success) -> {
                if (success) {
                    controller.sendInfo("Successfully purged data from TOA. " + response);
                } else {
                    controller.sendError("Connection to TOA unsuccessful. " + response);
                }
            });
        }
    }

    String getCurrentTime() {
        Date dt = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        return sdf.format(dt);
    }

}
