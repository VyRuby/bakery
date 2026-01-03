package model;

import java.io.Serializable;

public class Payroll implements Serializable {

    private String employeeID;
    private String fullName;
    private int month;
    private int workDays;
    private double baseSalary;
    private double bonus;
    private double penalty;
    private double totalSalary;

    public Payroll() {}

    public Payroll(String employeeID, String fullName, int month, int workDays,
                   double baseSalary, double bonus, double penalty, double totalSalary) {
        this.employeeID = employeeID;
        this.fullName = fullName;
        this.month = month;
        this.workDays = workDays;
        this.baseSalary = baseSalary;
        this.bonus = bonus;
        this.penalty = penalty;
        this.totalSalary = totalSalary;
    }

    public String getEmployeeID() { return employeeID; }
    public String getFullName() { return fullName; }
    public int getMonth() { return month; }
    public int getWorkDays() { return workDays; }
    public double getBaseSalary() { return baseSalary; }
    public double getBonus() { return bonus; }
    public double getPenalty() { return penalty; }
    public double getTotalSalary() { return totalSalary; }
}
