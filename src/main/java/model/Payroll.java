package model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Payroll implements Serializable {

    private String employeeID;
    private String fullName;

    private int workDays;
    private int month;
    private int year;

    private BigDecimal bonus;
    private BigDecimal penalty;
    private BigDecimal totalSalary;

    // ⭐ thêm base salary (daily)
    private int baseDailySalary;

    public Payroll() {}

    public Payroll(String employeeID, String fullName,
                   int workDays, int month, int year,
                   BigDecimal bonus, BigDecimal penalty,
                   BigDecimal totalSalary,
                   int baseDailySalary) {

        this.employeeID = employeeID;
        this.fullName = fullName;
        this.workDays = workDays;
        this.month = month;
        this.year = year;
        this.bonus = bonus;
        this.penalty = penalty;
        this.totalSalary = totalSalary;
        this.baseDailySalary = baseDailySalary;
    }

    public String getEmployeeID() { return employeeID; }
    public String getFullName() { return fullName; }
    public int getWorkDays() { return workDays; }
    public int getMonth() { return month; }
    public int getYear() { return year; }

    public BigDecimal getBonus() { return bonus; }
    public BigDecimal getPenalty() { return penalty; }
    public BigDecimal getTotalSalary() { return totalSalary; }

    public int getBaseDailySalary() { return baseDailySalary; }
}
