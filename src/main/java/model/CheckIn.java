package model;

import java.sql.Date;
import java.sql.Time;

public class CheckIn {

    private int checkInID;
    private String employeeID;
    private Date workDate;
    private Time checkInTime;
    private Time checkOutTime;
    private boolean isLate;

    public CheckIn(int checkInID, String employeeID, Date workDate,
                   Time checkInTime, Time checkOutTime, boolean isLate) {
        this.checkInID = checkInID;
        this.employeeID = employeeID;
        this.workDate = workDate;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.isLate = isLate;
    }

    public int getCheckInID() { return checkInID; }
    public String getEmployeeID() { return employeeID; }
    public Date getWorkDate() { return workDate; }
    public Time getCheckInTime() { return checkInTime; }
    public Time getCheckOutTime() { return checkOutTime; }
    public boolean isLate() { return isLate; }
}
