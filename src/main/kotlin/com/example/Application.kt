package com.example

import javafx.application.Application
import javafx.application.Application.launch
import javafx.scene.Scene
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Stage
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class ZeiterfassungsApp : Application() {

    val databaseUrl = "jdbc:mysql://localhost:3306/Time_Tracking?user=root&password=Hallo2023"
    val databaseManager = DatabaseManager(databaseUrl)

    private var currentUser: User? = null

    override fun start(primaryStage: Stage) {
        val loginButton = Button("Login")
        val usernameField = TextField()
        val passwordField = PasswordField()

        val loginLayout = VBox(10.0)
        loginLayout.children.addAll(Label("Username:"), usernameField, Label("Password:"), passwordField, loginButton)

        val loginScene = Scene(loginLayout, 300.0, 200.0)
        primaryStage.scene = loginScene
        primaryStage.title = "Zeiterfassungs App - Modul 226b - Stefan Krinulovic"

        loginButton.setOnAction {
            val username = usernameField.text
            val password = passwordField.text

            val currentUser = databaseManager.getUser(username)
            if (currentUser != null && authenticateUser(username, password)) {
                showMainApp(primaryStage, currentUser)
            } else {
                showAlert("Login Fehlgeschlagen", "Falscher Benutzername oder Passwort")
            }
        }


        primaryStage.show()
    }

    private fun showMainApp(primaryStage: Stage, currentUser: User) {
        val stampButton = Button("Stempeln")
        val absenceButton = Button("Absenz eintragen")
        val pendingabsenceButton = Button("Absenz anschauen")
        val historyButton = Button("Verlauf anzeigen")
        val logoutButton = Button("Logout")

        val mainLayout = VBox(10.0)
        mainLayout.children.addAll(stampButton, absenceButton, pendingabsenceButton, historyButton, logoutButton)

        val mainScene = Scene(mainLayout, 300.0, 200.0)
        primaryStage.scene = mainScene
        primaryStage.title = "Zeiterfassungs App - Hauptmenü"

        // Setzen Sie den übergebenen currentUser
        this.currentUser = currentUser

        stampButton.setOnAction {
            stampTime()
        }

        absenceButton.setOnAction {
            showAbsenceDialog()
        }

        pendingabsenceButton.setOnAction {
            showPendingAbsences()
        }

        historyButton.setOnAction {
            showHistory()
        }

        logoutButton.setOnAction {
            showLogoutDialog(primaryStage)
        }
        if (currentUser.role == Role.BOSS && databaseManager.haspendingAbsences()) {
            pendingabsenceButton.style = "-fx-text-fill: red;" // Setzen Sie die Textfarbe auf rot
        }
    }



    private fun authenticateUser(username: String, password: String): Boolean {
        val user = databaseManager.getUser(username)
        return user?.password == password
    }

    private fun stampTime() {
        if (currentUser == null) {
            showAlert("Fehler", "Benutzer nicht gefunden. Bitte erneut anmelden.")
            return
        }

        val timestamp = LocalDateTime.now()
        val stampType = if (isUserIn()) StampType.OUT else StampType.IN

        val stamp = Stamp(currentUser!!, timestamp, stampType)

        if (databaseManager.insertStamp(stamp)) {
            showAlert("Stempeln", "${stampType.label} erfolgreich um ${timestamp.format(DateTimeFormatter.ofPattern("HH:mm:ss"))}")
        } else {
            showAlert("Fehler", "Stempeln fehlgeschlagen. Bitte versuchen Sie es erneut.")
        }
    }

    private fun isUserIn(): Boolean {
        // Prüfen, ob der Benutzer bereits eingestempelt ist
        val lastStamp = databaseManager.getLastStamp(currentUser!!)
        return lastStamp?.stampType == StampType.IN
    }


    private fun showAbsenceDialog() {

        val startDatePicker = DatePicker()
        val endDatePicker = DatePicker()
        val reasonComboBox = ComboBox<AbsenceReason>()
        reasonComboBox.items.addAll(AbsenceReason.values())

        val confirmButton = Button("Absenz beantragen")
        confirmButton.setOnAction {
            val startDate = startDatePicker.value
            val endDate = endDatePicker.value
            val reason = reasonComboBox.value

            if (startDate != null && endDate != null && reason != null) {
                val userId = currentUser?.id ?: -1

                val success = databaseManager.requestAbsence(userId, startDate, endDate, reason)

                if (success) {
                    showAlert("Absenz beantragen", "Absenzanfrage erfolgreich eingetragen.")
                } else {
                    showAlert("Fehler", "Absenzanfrage konnte nicht eingetragen werden.")
                }
            } else {
                showAlert("Fehler", "Bitte füllen Sie alle Felder aus.")
            }
        }



        val dialog = Dialog<String>()
        dialog.dialogPane.buttonTypes.addAll(ButtonType.CLOSE)
        dialog.title = "Absenz eintragen"


        val layout = VBox(10.0, Label("Von:"), startDatePicker, Label("Bis:"), endDatePicker, Label("Grund:"), reasonComboBox, confirmButton)
        dialog.dialogPane.content = layout

        dialog.showAndWait()
    }

    fun showPendingAbsences() {
        val pendingAbsences = databaseManager.getPendingAbsences()
        val user = currentUser ?: return

        if (pendingAbsences.isNotEmpty()) {
            val dialog = Dialog<String>()
            dialog.title = "Ausstehende Absenzen"
            dialog.dialogPane.buttonTypes.addAll(ButtonType.CLOSE)
            val content = VBox()

            for (absence in pendingAbsences) {
                val label = Label(databaseManager.buildAbsenceMessage(listOf(absence)))

                val buttonPane = HBox()

                if (user.role == Role.BOSS) {
                    val approveButton = Button("Genehmigen")
                    approveButton.setOnAction {
                        val selectedIndex = pendingAbsences.indexOf(absence)
                        if (selectedIndex != -1) {
                            val selectedAbsence = pendingAbsences[selectedIndex]
                            if (databaseManager.approveAbsence(selectedAbsence)) {
                                content.children.remove(label)
                                dialog.dialogPane.content = content
                            } else {
                                showAlert("Fehler", "Genehmigung fehlgeschlagen.")
                            }
                        }
                    }

                    val rejectButton = Button("Ablehnen")
                    rejectButton.setOnAction {
                        val selectedIndex = pendingAbsences.indexOf(absence)
                        if (selectedIndex != -1) {
                            val selectedAbsence = pendingAbsences[selectedIndex]
                            if (databaseManager.rejectAbsence(selectedAbsence)) {
                                content.children.remove(label)
                                dialog.dialogPane.content = content // Aktualisiere die Dialog-Ansicht
                            } else {
                                showAlert("Fehler", "Ablehnung fehlgeschlagen.")
                            }
                        }
                    }

                    buttonPane.children.addAll(approveButton, rejectButton)
                }

                buttonPane.children.add(label)
                content.children.add(buttonPane)
            }

            dialog.dialogPane.content = content

            dialog.showAndWait()
        } else {
            showAlert("Ausstehende Absenzen", "Keine ausstehenden Absenzen vorhanden.")
        }
    }



    private fun showHistory() {
        val user = currentUser ?: return

        val stamps = databaseManager.getStampHistory(user) ?: emptyList()

        val historyMessage = databaseManager.buildHistoryMessage(stamps)

        if (stamps.isNotEmpty()) {
            showAlert("Stempelverlauf", historyMessage)
        } else {
            showAlert("Stempelverlauf", "Keine Stempelungen vorhanden.")
        }

    }


    private fun showLogoutDialog(primaryStage: Stage) {
        val alert = Alert(Alert.AlertType.CONFIRMATION)
        alert.title = "Logout"
        alert.headerText = null
        alert.contentText = "Sind Sie sicher, dass Sie sich abmelden möchten?"

        val result = alert.showAndWait()
        if (result.get() == ButtonType.OK) {
            // Zurück zum Login-Bildschirm
            start(primaryStage)
        }
    }

    private fun showAlert(title: String, content: String) {
        val alert = Alert(Alert.AlertType.INFORMATION)
        alert.title = title
        alert.headerText = null
        alert.contentText = content
        alert.showAndWait()
    }
}

private fun showAlertAbsence(title: String, content: String) {
    val alert = Alert(Alert.AlertType.INFORMATION)
    alert.title = title
    alert.headerText = null
    alert.contentText = content
    alert.showAndWait()
}


fun main() {
    launch(ZeiterfassungsApp::class.java)
}
