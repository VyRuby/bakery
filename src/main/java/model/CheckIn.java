package model;

import java.io.Serializable;
import java.sql.Date;
import java.sql.Time;

public class CheckIn implements Serializable {

    private int checkInID;
    private String employeeID;
    private String fullName; // join từ EMPLOYEE

    private Date workDate;
    private Time checkInTime;
    private Time checkOutTime;

    private boolean isLate;
    private boolean isEarlyLeave;

    public CheckIn() {}

    public CheckIn(int checkInID, String employeeID, String fullName,
                   Date workDate, Time checkInTime, Time checkOutTime,
                   boolean isLate, boolean isEarlyLeave) {

        this.checkInID = checkInID;
        this.employeeID = employeeID;
        this.fullName = fullName;
        this.workDate = workDate;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.isLate = isLate;
        this.isEarlyLeave = isEarlyLeave;
    }

    public int getCheckInID() { return checkInID; }
    public String getEmployeeID() { return employeeID; }
    public String getFullName() { return fullName; }

    public Date getWorkDate() { return workDate; }
    public Time getCheckInTime() { return checkInTime; }
    public Time getCheckOutTime() { return checkOutTime; }

    // ⚠️ QUAN TRỌNG: JavaFX TableColumn<Boolean>
    public boolean isLate() { return isLate; }
    public boolean isEarlyLeave() { return isEarlyLeave; }
}
