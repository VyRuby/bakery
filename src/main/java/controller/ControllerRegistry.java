package controller;

public class ControllerRegistry {

    private static CheckInController checkInController;

    // ===== REGISTER =====
    public static void setCheckInController(CheckInController controller) {
        checkInController = controller;
    }

    // ===== GET =====
    public static CheckInController getCheckInController() {
        return checkInController;
    }
}
