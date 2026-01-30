package controller;

public class ControllerRegistry {

    private static CheckInController checkInController;
    private static PayrollController payrollController;

    // ===== REGISTER =====
    public static void setCheckInController(CheckInController controller) {
        checkInController = controller;
    }

    public static void setPayrollController(PayrollController controller) {
        payrollController = controller;
    }

    // ===== GET =====
    public static CheckInController getCheckInController() {
        return checkInController;
    }

    public static PayrollController getPayrollController() {
        return payrollController;
    }
}
