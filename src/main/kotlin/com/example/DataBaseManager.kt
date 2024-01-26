package com.example

import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.time.LocalDate

class DatabaseManager(private val databaseUrl: String) {
    init {
        // Erstelle die Benutzertabelle, falls sie noch nicht existiert
        createTable()
    }

    private fun createTable() {
        getConnection()?.use { connection ->
            val createTableSQL = """
                CREATE TABLE IF NOT EXISTS Users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(255) UNIQUE,
                    password VARCHAR(255),
                    role VARCHAR(255)
                );
                """.trimIndent()
            val createTablestamps =""" CREATE TABLE IF NOT EXISTS Stamps (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    username VARCHAR(255),
                    timestamp TIMESTAMP,
                    stamp_type VARCHAR(255),
                    FOREIGN KEY (username) REFERENCES Users(username)
                );
                """.trimIndent()
            val createTableabsence ="""CREATE TABLE IF NOT EXISTS Absences (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT,
                    start_date DATE,
                    end_date DATE,
                    reason VARCHAR(255),
                    status VARCHAR(50),
                    FOREIGN KEY (user_id) REFERENCES Users(id)
                );
                """.trimIndent()
               val createstandusers ="""
                insert ignore into time_tracking.users (id, username, password, role) Values ('1', 'user1', 'user1', 'USER');
            """.trimIndent()
            val createstandboss ="""
                insert ignore into time_tracking.users (id, username, password, role) Values ('2', 'boss', 'boss', 'BOSS');
            """.trimIndent()
            connection.prepareStatement(createTableSQL).executeUpdate()
            connection.prepareStatement(createTablestamps).executeUpdate()
            connection.prepareStatement(createTableabsence).executeUpdate()
            connection.prepareStatement(createstandusers).executeUpdate()
            connection.prepareStatement(createstandboss).executeUpdate()
        }
    }

    fun getUser(username: String): User? {
        val selectUserSQL = "SELECT * FROM Users WHERE username = ?"

        return getConnection()?.use { connection ->
            val preparedStatement: PreparedStatement = connection.prepareStatement(selectUserSQL)
            preparedStatement.setString(1, username)

            val resultSet: ResultSet = preparedStatement.executeQuery()

            if (resultSet.next()) {
                val fetcheduserid = resultSet.getInt("id")
                val fetchedUsername = resultSet.getString("username")
                val password = resultSet.getString("password")
                val role = resultSet.getString("role")

                User( fetcheduserid, fetchedUsername, password, Role.valueOf(role))
            } else {
                null
            }
        }
    }


    fun getUserById(userId: Int): User? {
        val sql = "SELECT * FROM Users WHERE id = ?"

        return try {
            getConnection()?.use { connection ->
                connection.prepareStatement(sql).use { preparedStatement ->
                    preparedStatement.setInt(1, userId)

                    val resultSet = preparedStatement.executeQuery()

                    if (resultSet.next()) {
                        val fetchedId = resultSet.getInt("id")
                        val fetchedUsername = resultSet.getString("username")
                        val password = resultSet.getString("password")
                        val role = resultSet.getString("role")

                        User(fetchedId, fetchedUsername, password, Role.valueOf(role))
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    fun insertStamp(stamp: Stamp): Boolean {
        val sql = "INSERT INTO stamps (username, timestamp, stamp_type) VALUES (?, ?, ?)"

        return try {
            DriverManager.getConnection(databaseUrl).use { connection ->
                connection.prepareStatement(sql).use { preparedStatement ->
                    preparedStatement.setString(1, stamp.user.username)
                    preparedStatement.setObject(2, stamp.timestamp)
                    preparedStatement.setString(3, stamp.stampType.name)

                    preparedStatement.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun getLastStamp(user: User): Stamp? {
        val sql = """SELECT * FROM Stamps WHERE username = ? ORDER BY timestamp DESC LIMIT 1""".trimIndent()

        return try {
            getConnection()?.use { connection ->
                connection.prepareStatement(sql).use { preparedStatement ->
                    preparedStatement.setString(1, user.username)

                    val resultSet = preparedStatement.executeQuery()

                    if (resultSet.next()) {
                        val timestamp = resultSet.getTimestamp("timestamp").toLocalDateTime()
                        val stampType = StampType.valueOf(resultSet.getString("stamp_type"))

                        Stamp(user, timestamp, stampType)
                    } else {
                        null
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getStampHistory(user: User): List<StampHistoryEntry> {
        val sqlstamp = "SELECT * FROM Stamps WHERE username = ? ORDER BY timestamp"

        return try {
            getConnection()?.use { connection ->
                connection.prepareStatement(sqlstamp).use { preparedStatement ->
                    preparedStatement.setString(1, user.username)

                    val resultSet = preparedStatement.executeQuery()

                    val stampHistory = mutableListOf<StampHistoryEntry>()
                    while (resultSet.next()) {
                        val timestamp = resultSet.getTimestamp("timestamp").toLocalDateTime()
                        val stampType = StampType.valueOf(resultSet.getString("stamp_type"))

                        val entry = StampHistoryEntry(timestamp, stampType)
                        stampHistory.add(entry)
                    }
                    stampHistory
                }
            } ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
        fun buildHistoryMessage(stamps: List<StampHistoryEntry>): String {
            val stringBuilder = StringBuilder()
            for (entry in stamps) {
                val timestampString = entry.timestamp.toString()
                val stampTypeString = entry.stampType.label
                val message = "$timestampString - $stampTypeString\n"
                stringBuilder.append(message)
            }
            return stringBuilder.toString()
        }

    fun requestAbsence(userId: Int, startDate: LocalDate, endDate: LocalDate, reason: AbsenceReason): Boolean {
        val sqlrequesabsence = "INSERT INTO Absences (user_id, start_date, end_date, reason, status) VALUES (?, ?, ?, ?, ?)"
        var success = false // Variable für den Erfolg des Updates

        try {
            getConnection()?.use { connection ->
                connection.prepareStatement(sqlrequesabsence).use { preparedStatement ->
                    preparedStatement.setInt(1, userId)
                    preparedStatement.setDate(2, java.sql.Date.valueOf(startDate))
                    preparedStatement.setDate(3, java.sql.Date.valueOf(endDate))
                    preparedStatement.setString(4, reason.name)
                    preparedStatement.setString(5, AbsenceStatus.PENDING.name)

                    // Führe das Update aus und setze den Erfolg
                    success = preparedStatement.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return success
    }




    fun buildAbsenceMessage(absences: List<Absence>): String {
        val stringBuilder = StringBuilder()
        for (absence in absences) {
            val user = absence.user
            val startDate = absence.startDate
            val endDate = absence.endDate
            val reason = absence.reason
            val status = absence.status

            val message = "Absenzanfrage von ${user.username} vom $startDate bis $endDate für $reason. Status: $status\n"
            stringBuilder.append(message)
        }
        return stringBuilder.toString()
    }


    fun getPendingAbsences(): List<Absence> {
        val sqlabsence = "SELECT * FROM Absences WHERE status = ?"

        return try {
            val connection = getConnection() ?: throw IllegalStateException("Database connection is null")

            connection.use { connection ->
                connection.prepareStatement(sqlabsence).use { preparedStatement ->
                    preparedStatement.setString(1, AbsenceStatus.PENDING.name)

                    val resultSet = preparedStatement.executeQuery()

                    val absenceList = mutableListOf<Absence>()
                    while (resultSet.next()) {
                        val id = resultSet.getInt("id")
                        val userId = resultSet.getInt("user_id")
                        val startDate = resultSet.getDate("start_date").toLocalDate()
                        val endDate = resultSet.getDate("end_date").toLocalDate()
                        val reason = AbsenceReason.valueOf(resultSet.getString("reason"))
                        val status = AbsenceStatus.valueOf(resultSet.getString("status"))

                        val user = getUserById(userId) ?: continue

                        val absence = Absence(id, user, startDate, endDate, reason, status)
                        absenceList.add(absence)
                    }
                    absenceList.toList() // Konvertiere die MutableList in eine unveränderliche List
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }






    fun approveAbsence(absence: Absence): Boolean {
        return updateAbsenceStatus(absence.id, AbsenceStatus.APPROVED)
    }

    fun rejectAbsence(absence: Absence): Boolean {
        return updateAbsenceStatus(absence.id, AbsenceStatus.REJECTED)
    }

    private fun updateAbsenceStatus(absenceId: Int, status: AbsenceStatus): Boolean {
        val sql = "UPDATE Absences SET status = ? WHERE id = ?"
        var success = false
        try {
            getConnection()?.use { connection ->
                connection.prepareStatement(sql).use { preparedStatement ->
                    preparedStatement.setString(1, status.name)
                    preparedStatement.setInt(2, absenceId)

                    success = preparedStatement.executeUpdate() > 0
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return success
    }

    private fun getConnection(): Connection? {
        return try {
            DriverManager.getConnection(databaseUrl)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
