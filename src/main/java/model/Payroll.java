package model;

import java.io.Serializable;
import java.math.BigDecimal;

public class Payroll implements Serializable {

    private String employeeID;
    private String fullName;
    private int month;
    private int workDays;

    private BigDecimal baseSalary;
    private BigDecimal bonus;
    private BigDecimal penalty;
    private BigDecimal totalSalary;

    public Payroll() {}

    public Payroll(String employeeID, String fullName, int month, int workDays,
                   BigDecimal baseSalary, BigDecimal bonus,
                   BigDecimal penalty, BigDecimal totalSalary) {

        this.employeeID = employeeID;
        this.fullName = fullName;
        this.month = month;
        this.workDays = workDays;
        this.baseSalary = baseSalary;
        this.bonus = bonus;
        this.penalty = penalty;
        this.totalSalary = totalSalary;
    }

    public String getEmployeeID() {
        return employeeID;
    }

    public String getFullName() {
        return fullName;
    }

    public int getMonth() {
        return month;
    }

    public int getWorkDays() {
        return workDays;
    }

    public BigDecimal getBaseSalary() {
        return baseSalary;
    }

    public BigDecimal getBonus() {
        return bonus;
    }

    public BigDecimal getPenalty() {
        return penalty;
    }

    public BigDecimal getTotalSalary() {
        return totalSalary;
    }
}
