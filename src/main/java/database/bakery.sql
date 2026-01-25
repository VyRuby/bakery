-- =========================
-- CREATE DATABASE
-- =========================
CREATE DATABASE IF NOT EXISTS bakery_db;
USE bakery_db;

-- =========================
-- TABLE: EMPLOYEE
-- =========================
CREATE TABLE EMPLOYEE (
    EmployeeID VARCHAR(30) PRIMARY KEY,
    FullName VARCHAR(100),
    DOB DATE,
    Gender ENUM('Male','Female'),
    Phone VARCHAR(30),
    Email VARCHAR(100),
    Address VARCHAR(255),
    HireDate DATE,
    Position VARCHAR(30),
    Status ENUM('Active','Inactive')
);


-- =========================
-- TABLE: EMPLOYEE_PAYROLL
-- =========================
-- Staff: 8~9,000,000
-- Manager: 12000000
CREATE TABLE EMPLOYEE_PAYROLL (
    PayrollID VARCHAR(30) PRIMARY KEY,
    EmployeeID VARCHAR(30) NOT NULL,

    Month TINYINT NOT NULL,
    Year  SMALLINT NOT NULL,

    BaseSalary DECIMAL(12,2) NOT NULL,

    WorkDays INT DEFAULT 0,
    LateWorkday INT DEFAULT 0,

    Bonus DECIMAL(12,2) DEFAULT 0,
    Penalty DECIMAL(12,2) DEFAULT 0,
    TotalSalary DECIMAL(12,2),

    CONSTRAINT fk_payroll_employee
        FOREIGN KEY (EmployeeID)
        REFERENCES EMPLOYEE(EmployeeID),

    CONSTRAINT uq_employee_month_year
        UNIQUE (EmployeeID, Month, Year)
);


-- =========================
-- TABLE: EMPLOYEE_BONUS
-- =========================
CREATE TABLE EMPLOYEE_BONUS (
    BonusID VARCHAR(30) PRIMARY KEY,
    PayrollID VARCHAR(30),
    Description VARCHAR(255),
    Amount DECIMAL(12,2),
    BonusDate DATE DEFAULT (CURDATE()),

    CONSTRAINT fk_bonus_payroll
        FOREIGN KEY (PayrollID)
        REFERENCES EMPLOYEE_PAYROLL(PayrollID)
);

-- =========================
-- TABLE: EMPLOYEE_PENALTY
-- =========================
CREATE TABLE EMPLOYEE_PENALTY (
    PenaltyID VARCHAR(30) PRIMARY KEY,
    PayrollID VARCHAR(30),
    Description VARCHAR(255),
    Amount DECIMAL(12,2),
    PenaltyDate DATE DEFAULT (CURDATE()),

    CONSTRAINT fk_penalty_payroll
        FOREIGN KEY (PayrollID)
        REFERENCES EMPLOYEE_PAYROLL(PayrollID)
);

-- =========================
-- TABLE: EMPLOYEE_CHECKIN
-- =========================
CREATE TABLE EMPLOYEE_CHECKIN (
    CheckInID INT AUTO_INCREMENT PRIMARY KEY,
    EmployeeID VARCHAR(30),
    WorkDate DATE DEFAULT (CURDATE()),
    CheckInTime TIME DEFAULT (CURTIME()),
    CheckOutTime TIME,
    IsLate BOOLEAN DEFAULT 0,

    CONSTRAINT fk_checkin_employee
        FOREIGN KEY (EmployeeID)
        REFERENCES EMPLOYEE(EmployeeID),

    CONSTRAINT uq_employee_date
        UNIQUE (EmployeeID, WorkDate)
);

-- =========================
-- TRIGGER: CHECK LATE (AFTER 08:00)
-- =========================
DELIMITER $$

CREATE TRIGGER trg_checkin_late
BEFORE INSERT ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    IF NEW.CheckInTime > '08:00:00' THEN
        SET NEW.IsLate = 1;
    ELSE
        SET NEW.IsLate = 0;
    END IF;
END$$

DELIMITER ;

-- =========================
-- TABLE: EMPLOYEE_ATTENDANCE_LOG
-- =========================
CREATE TABLE EMPLOYEE_ATTENDANCE_LOG (
    LogID INT AUTO_INCREMENT PRIMARY KEY,
    EmployeeID VARCHAR(30),
    WorkDate DATE,
    Action ENUM('CHECKIN','CHECKOUT'),
    ActionTime DATETIME DEFAULT CURRENT_TIMESTAMP,
    Note VARCHAR(255),

    CONSTRAINT fk_log_employee
        FOREIGN KEY (EmployeeID)
        REFERENCES EMPLOYEE(EmployeeID)
);

-- =========================
-- TRIGGER: VALIDATE CHECKOUT
-- =========================
DELIMITER $$

CREATE TRIGGER trg_checkout_valid
BEFORE UPDATE ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    IF OLD.CheckOutTime IS NOT NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Already checked out';
    END IF;

    IF NEW.CheckOutTime <= OLD.CheckInTime THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Checkout time must be after checkin time';
    END IF;
END$$

DELIMITER ;

-- =========================
-- TRIGGER: LOG CHECK-IN
-- =========================
DELIMITER $$

CREATE TRIGGER trg_log_checkin
AFTER INSERT ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    INSERT INTO EMPLOYEE_ATTENDANCE_LOG
    (EmployeeID, WorkDate, Action, ActionTime, Note)
    VALUES
    (NEW.EmployeeID, NEW.WorkDate, 'CHECKIN', NOW(), 'Employee check-in');
END$$

DELIMITER ;

-- =========================
-- TRIGGER: LOG CHECK-OUT
-- =========================
DELIMITER $$

CREATE TRIGGER trg_log_checkout
AFTER UPDATE ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    IF NEW.CheckOutTime IS NOT NULL THEN
        INSERT INTO EMPLOYEE_ATTENDANCE_LOG
        (EmployeeID, WorkDate, Action, ActionTime, Note)
        VALUES
        (NEW.EmployeeID, NEW.WorkDate, 'CHECKOUT', NOW(), 'Employee check-out');
    END IF;
END$$

DELIMITER ;

-- =========================
-- TRIGGER CHẶN NHÂN VIÊN INACTIVE
-- =========================
DELIMITER $$

CREATE TRIGGER trg_checkin_only_active
BEFORE INSERT ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM EMPLOYEE
        WHERE EmployeeID = NEW.EmployeeID
          AND Status = 'Active'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Employee is not Active – cannot check in';
    END IF;
END$$

DELIMITER ;

-- =========================
-- TRIGGER Thêm trigger AFTER UPDATE cho EMPLOYEE_CHECKIN -> trg_checkin_update_payroll chạy tự động
-- =========================
DELIMITER $$ 

CREATE TRIGGER trg_checkin_update_payroll_after_update
AFTER UPDATE ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    DECLARE v_month INT;
    DECLARE v_year INT;

    SET v_month = MONTH(NEW.WorkDate);
    SET v_year  = YEAR(NEW.WorkDate);

    UPDATE EMPLOYEE_PAYROLL p
    SET
        p.WorkDays = (
            SELECT COUNT(*)
            FROM EMPLOYEE_CHECKIN c
            WHERE c.EmployeeID = NEW.EmployeeID
              AND MONTH(c.WorkDate) = v_month
              AND YEAR(c.WorkDate) = v_year
        ),
        p.LateWorkday = (
            SELECT COUNT(*)
            FROM EMPLOYEE_CHECKIN c
            WHERE c.EmployeeID = NEW.EmployeeID
              AND MONTH(c.WorkDate) = v_month
              AND YEAR(c.WorkDate) = v_year
              AND c.IsLate = 1
        )
    WHERE p.EmployeeID = NEW.EmployeeID
      AND p.Month = v_month
      AND p.Year = v_year;
END$$

DELIMITER ;


-- =========================
--PROCEDURE: CHECK-IN THEO EMAIL
-- =========================
DELIMITER $$

CREATE PROCEDURE sp_employee_checkin_by_email(IN p_email VARCHAR(100))
BEGIN
    DECLARE v_empid VARCHAR(30);

    SELECT EmployeeID
    INTO v_empid
    FROM EMPLOYEE
    WHERE Email = p_email
      AND Status = 'Active';

    IF v_empid IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid email or inactive employee';
    END IF;

    INSERT INTO EMPLOYEE_CHECKIN (EmployeeID)
    VALUES (v_empid);
END$$

DELIMITER ;

-- =========================
--PROCEDURE: CHECK-OUT THEO EMAIL
-- =========================
DELIMITER $$

CREATE PROCEDURE sp_employee_checkout_by_email(IN p_email VARCHAR(100))
BEGIN
    DECLARE v_empid VARCHAR(30);

    SELECT EmployeeID
    INTO v_empid
    FROM EMPLOYEE
    WHERE Email = p_email
      AND Status = 'Active';

    IF v_empid IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Invalid email or inactive employee';
    END IF; 

IF NOT EXISTS (
    SELECT 1
    FROM EMPLOYEE_CHECKIN
    WHERE EmployeeID = v_empid
      AND WorkDate = CURDATE()
      AND CheckOutTime IS NULL
) THEN
    SIGNAL SQLSTATE '45000'
    SET MESSAGE_TEXT = 'No check-in found today';
END IF;

    UPDATE EMPLOYEE_CHECKIN
    SET CheckOutTime = CURTIME()
    WHERE EmployeeID = v_empid
      AND WorkDate = CURDATE();
END$$

DELIMITER ;



-- =========================
-- INSERT EMPLOYEE DATA
-- =========================
INSERT INTO EMPLOYEE VALUES
('E01','Nguyen Hong Ngoc','1998-01-10','Female','0901','a@gmail.com','HN','2001-01-01','Manager','Active'),
('E02','Tran Van Huy','1997-02-11','Male','0902','b@gmail.com','HCM','2020-05-12','Staff','Active'),
('E03','Pham Quoc Anh','2001-03-12','Male','0903','c@gmail.com','DN','2019-06-15','Staff','Active'),
('E04','Pham Thi D','1999-04-13','Female','0904','d@gmail.com','HN','2022-02-10','Staff','Active'),
('E05','Hoang Van E','1995-05-14','Male','0905','e@gmail.com','HCM','2018-03-20','Staff','Inactive'),
('E06','Nguyen Tu Quyen','2004-05-12','Female','0906','tuquyen@gmail.com','HN','2021-07-01','Staff','Active'),
('E07','Vu Van G','1997-07-16','Male','0907','g@gmail.com','HN','2020-08-08','Staff','Active'),
('E08','Dang Thi H','1996-08-17','Female','0908','h@gmail.com','HCM','2019-09-09','Staff','Active'),
('E09','Bui Van I','1995-09-18','Male','0909','i@gmail.com','DN','2018-10-10','Staff','Active'),
('E10','Nguyen Thi J','1999-10-19','Female','0910','j@gmail.com','HN','2022-11-11','Staff','Active');

-- =========================
-- INSERT PAYROLL (REAL TIME)
-- =========================
INSERT INTO EMPLOYEE_PAYROLL
(PayrollID, EmployeeID, Month, Year, BaseSalary)
VALUES
-- ===== MONTH 6 =====
('P01','E01',6,2025,12000000),
('P02','E02',6,2025,8500000),
('P03','E03',6,2025,7800000),
('P04','E04',6,2025,9000000),
('P05','E06',6,2025,8800000),
('P06','E07',6,2025,8300000),
('P07','E08',6,2025,8400000),
('P08','E09',6,2025,8600000),
('P09','E10',6,2025,8800000),

-- ===== MONTH 7 =====
('P10','E01',7,2025,12000000),
('P11','E02',7,2025,8500000),
('P12','E03',7,2025,7800000),
('P13','E04',7,2025,9000000),
('P14','E06',7,2025,8200000),
('P15','E07',7,2025,8300000),
('P16','E08',7,2025,8400000),
('P17','E09',7,2025,8600000),
('P18','E10',7,2025,8800000),

-- ===== MONTH 8 =====
('P19','E01',8,2025,12000000),
('P20','E02',8,2025,8500000),
('P21','E03',8,2025,7800000),
('P22','E04',8,2025,9000000),
('P23','E06',8,2025,8200000),
('P24','E07',8,2025,8300000),
('P25','E08',8,2025,8400000),
('P26','E09',8,2025,8600000),
('P27','E10',8,2025,8800000),

-- ===== MONTH 9 =====
('P28','E01',9,2025,12000000),
('P29','E02',9,2025,8500000),
('P30','E03',9,2025,7800000),
('P31','E04',9,2025,9000000),
('P32','E06',9,2025,8200000),
('P33','E07',9,2025,8300000),
('P34','E08',9,2025,8400000),
('P35','E09',9,2025,8600000),
('P36','E10',9,2025,8800000),

-- ===== MONTH 10 =====
('P37','E01',10,2025,12000000),
('P38','E02',10,2025,8500000),
('P39','E03',10,2025,7800000),
('P40','E04',10,2025,9000000),
('P41','E06',10,2025,8200000),
('P42','E07',10,2025,8300000),
('P43','E08',10,2025,8400000),
('P44','E09',10,2025,8600000),
('P45','E10',10,2025,8800000),

-- ===== MONTH 11 =====
('P46','E01',11,2025,12000000),
('P47','E02',11,2025,8500000),
('P48','E03',11,2025,7800000),
('P49','E04',11,2025,9000000),
('P50','E06',11,2025,8200000),
('P51','E07',11,2025,8300000),
('P52','E08',11,2025,8400000),
('P53','E09',11,2025,8600000),
('P54','E10',11,2025,8800000),

-- ===== MONTH 12 =====
('P55','E01',12,2025,12000000),
('P56','E02',12,2025,8500000),
('P57','E03',12,2025,7800000),
('P58','E04',12,2025,9000000),
('P59','E06',12,2025,8200000),
('P60','E07',12,2025,8300000),
('P61','E08',12,2025,8400000),
('P62','E09',12,2025,8600000),
('P63','E10',12,2025,8800000);
--Test nhanh sau khi insert
-- SELECT EmployeeID, Month, Year, BaseSalary
-- FROM EMPLOYEE_PAYROLL
-- ORDER BY EmployeeID, Month;








-- =========================
-- STORED PROCEDURE: generate CHECK-IN 
-- 
-- =========================
DELIMITER $$

CREATE PROCEDURE sp_GenerateCheckinData()
BEGIN
    DECLARE v_emp VARCHAR(30);
    DECLARE v_date DATE;
    DECLARE v_month INT;
    DECLARE v_day INT;
    DECLARE done INT DEFAULT 0;

    -- Cursor lấy nhân viên Active
    DECLARE emp_cursor CURSOR FOR
        SELECT EmployeeID
        FROM EMPLOYEE
        WHERE Status = 'Active'
          AND EmployeeID BETWEEN 'E01' AND 'E10';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN emp_cursor;

    emp_loop: LOOP
        FETCH emp_cursor INTO v_emp;
        IF done = 1 THEN
            LEAVE emp_loop;
        END IF;

        SET v_month = 6;

        month_loop: WHILE v_month <= 12 DO
            SET v_day = 1;

            day_loop: WHILE v_day <= 26 DO
                -- Random bỏ 20% ngày để không full công
                IF RAND() > 0.2 THEN
                    SET v_date = STR_TO_DATE(
                        CONCAT('2025-', LPAD(v_month,2,'0'), '-', LPAD(v_day,2,'0')),
                        '%Y-%m-%d'
                    );

                  -- CHECK-IN (trigger xử lý Late, WorkDays, LateWorkday & Log)

                    INSERT IGNORE INTO EMPLOYEE_CHECKIN
                    (EmployeeID, WorkDate, CheckInTime)
                    VALUES
                    (
                        v_emp,
                        v_date,
                        SEC_TO_TIME(
                            TIME_TO_SEC('07:40:00')
                            + FLOOR(RAND() * 3600) -- 07:40 → 08:40
                        )
                    );

                    -- CHECK-OUT (trigger validate + log)
                    UPDATE EMPLOYEE_CHECKIN
                    SET CheckOutTime =
                        SEC_TO_TIME(
                            TIME_TO_SEC('16:30:00')
                            + FLOOR(RAND() * 5400) -- 16:30 → 18:00
                        )
                    WHERE EmployeeID = v_emp
                      AND WorkDate = v_date;
                END IF;

                SET v_day = v_day + 1;
            END WHILE;

            SET v_month = v_month + 1;
        END WHILE;
    END LOOP;

    CLOSE emp_cursor;
END$$

DELIMITER ;

-- =========================
-- PROCEDURE: GENERATE CHECKOUT DATA (TEST)
-- =========================
DELIMITER $$

CREATE PROCEDURE sp_GenerateCheckoutData()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE v_checkin_id INT;
    DECLARE v_checkin_time TIME;

    -- Cursor lấy các dòng đã check-in nhưng CHƯA check-out
    DECLARE cur_checkout CURSOR FOR
        SELECT CheckInID, CheckInTime
        FROM EMPLOYEE_CHECKIN
        WHERE CheckOutTime IS NULL;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN cur_checkout;

    checkout_loop: LOOP
        FETCH cur_checkout INTO v_checkin_id, v_checkin_time;
        IF done = 1 THEN
            LEAVE checkout_loop;
        END IF;

        -- Check-out ngẫu nhiên từ 18:30 → 20:00
        UPDATE EMPLOYEE_CHECKIN
        SET CheckOutTime =
            SEC_TO_TIME(
                TIME_TO_SEC('18:30:00')
                + FLOOR(RAND() * 5400) -- +0 → +90 phút
            )
        WHERE CheckInID = v_checkin_id;
    END LOOP;

    CLOSE cur_checkout;
END$$

DELIMITER ;


-- =========================
-- STORED PROCEDURE tính lại lương theo tháng
--TotalSalary = BaseSalary × WorkDays + Bonus − Penalty
-- =========================
DELIMITER $$

CREATE PROCEDURE sp_calculate_payroll_by_month(
    IN p_month INT,
    IN p_year INT
)
BEGIN
    UPDATE EMPLOYEE_PAYROLL
    SET TotalSalary =
        ROUND(
            (BaseSalary / 26 * WorkDays)
            + IFNULL(Bonus, 0)
            - IFNULL(Penalty, 0),
        0)
    WHERE Month = p_month
      AND Year  = p_year;
END$$

DELIMITER ;





-- =========================
-- TRIGGER: UPDATE WORKDAY & LATE
-- =========================
DELIMITER $$

CREATE TRIGGER trg_checkin_update_payroll
AFTER INSERT ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    DECLARE v_month INT;
    DECLARE v_year INT;

    SET v_month = MONTH(NEW.WorkDate);
    SET v_year  = YEAR(NEW.WorkDate);

    UPDATE EMPLOYEE_PAYROLL p
    SET
        p.WorkDays = (
            SELECT COUNT(*)
            FROM EMPLOYEE_CHECKIN c
            WHERE c.EmployeeID = NEW.EmployeeID
              AND MONTH(c.WorkDate) = v_month
              AND YEAR(c.WorkDate) = v_year
        ),
        p.LateWorkday = (
            SELECT COUNT(*)
            FROM EMPLOYEE_CHECKIN c
            WHERE c.EmployeeID = NEW.EmployeeID
              AND MONTH(c.WorkDate) = v_month
              AND YEAR(c.WorkDate) = v_year
              AND c.IsLate = 1
        )
    WHERE p.EmployeeID = NEW.EmployeeID
      AND p.Month = v_month
      AND p.Year = v_year;
END$$

DELIMITER ;


-- 1. Sinh check-in
CALL sp_GenerateCheckinData();

-- 2. Sinh check-out
CALL sp_GenerateCheckoutData();

-- =========================
-- TRIGGER: AUTO UPDATE BONUS
-- =========================
DELIMITER $$

CREATE TRIGGER trg_bonus_update_payroll
AFTER INSERT ON EMPLOYEE_BONUS
FOR EACH ROW
BEGIN
    UPDATE EMPLOYEE_PAYROLL
    SET Bonus = IFNULL(Bonus, 0) + NEW.Amount
    WHERE PayrollID = NEW.PayrollID;
END$$

DELIMITER ;

-- =========================
-- TRIGGER: AUTO UPDATE PENALTY
-- =========================
DELIMITER $$

CREATE TRIGGER trg_penalty_update_payroll
AFTER INSERT ON EMPLOYEE_PENALTY
FOR EACH ROW
BEGIN
    UPDATE EMPLOYEE_PAYROLL
    SET Penalty = IFNULL(Penalty, 0) + NEW.Amount
    WHERE PayrollID = NEW.PayrollID;
END$$

DELIMITER ;

-- =========================
-- BONUS TEST DATA (MONTH 6 → 12)
-- =========================

-- ===== THÁNG 6 =====
INSERT INTO EMPLOYEE_BONUS VALUES
('B0601','P01','Thưởng KPI tháng 6',1000000,'2025-06-30'),
('B0602','P02','Thưởng KPI tháng 6',800000,'2025-06-30'),
('B0603','P03','Thưởng KPI tháng 6',700000,'2025-06-30'),
('B0604','P04','Thưởng KPI tháng 6',900000,'2025-06-30'),
('B0605','P05','Thưởng KPI tháng 6',600000,'2025-06-30'),
('B0606','P06','Thưởng chuyên cần',500000,'2025-06-30'),
('B0607','P07','Thưởng chuyên cần',500000,'2025-06-30'),
('B0608','P08','Thưởng chuyên cần',500000,'2025-06-30'),
('B0609','P09','Thưởng chuyên cần',500000,'2025-06-30'),
('B0610','P10','Thưởng chuyên cần',500000,'2025-06-30');

-- ===== THÁNG 7 =====
INSERT INTO EMPLOYEE_BONUS VALUES
('B0701','P10','Thưởng KPI tháng 7',900000,'2025-07-31'),
('B0702','P11','Thưởng KPI tháng 7',850000,'2025-07-31'),
('B0703','P12','Thưởng KPI tháng 7',700000,'2025-07-31'),
('B0704','P13','Thưởng KPI tháng 7',950000,'2025-07-31'),
('B0705','P14','Thưởng KPI tháng 7',650000,'2025-07-31'),
('B0706','P15','Thưởng chuyên cần',500000,'2025-07-31'),
('B0707','P16','Thưởng chuyên cần',500000,'2025-07-31'),
('B0708','P17','Thưởng chuyên cần',500000,'2025-07-31'),
('B0709','P18','Thưởng chuyên cần',500000,'2025-07-31');


-- ===== THÁNG 8 =====
INSERT INTO EMPLOYEE_BONUS VALUES
('B0801','P19','Thưởng KPI tháng 8',920000,'2025-08-31'),
('B0802','P20','Thưởng KPI tháng 8',870000,'2025-08-31'),
('B0803','P21','Thưởng KPI tháng 8',720000,'2025-08-31'),
('B0804','P22','Thưởng KPI tháng 8',960000,'2025-08-31'),
('B0805','P23','Thưởng KPI tháng 8',680000,'2025-08-31'),
('B0806','P24','Thưởng chuyên cần',500000,'2025-08-31'),
('B0807','P25','Thưởng chuyên cần',500000,'2025-08-31'),
('B0808','P26','Thưởng chuyên cần',500000,'2025-08-31'),
('B0809','P27','Thưởng chuyên cần',500000,'2025-08-31');

-- ===== THÁNG 9 =====
INSERT INTO EMPLOYEE_BONUS VALUES
('B0901','P28','Thưởng KPI tháng 9',930000,'2025-09-30'),
('B0902','P29','Thưởng KPI tháng 9',880000,'2025-09-30'),
('B0903','P30','Thưởng KPI tháng 9',750000,'2025-09-30'),
('B0904','P31','Thưởng KPI tháng 9',970000,'2025-09-30'),
('B0905','P32','Thưởng KPI tháng 9',700000,'2025-09-30'),
('B0906','P33','Thưởng chuyên cần',500000,'2025-09-30'),
('B0907','P34','Thưởng chuyên cần',500000,'2025-09-30'),
('B0908','P35','Thưởng chuyên cần',500000,'2025-09-30'),
('B0909','P36','Thưởng chuyên cần',500000,'2025-09-30');


-- ===== THÁNG 10 =====
INSERT INTO EMPLOYEE_BONUS VALUES
('B1001','P37','Thưởng KPI tháng 10',940000,'2025-10-31'),
('B1002','P38','Thưởng KPI tháng 10',890000,'2025-10-31'),
('B1003','P39','Thưởng KPI tháng 10',760000,'2025-10-31'),
('B1004','P40','Thưởng KPI tháng 10',980000,'2025-10-31'),
('B1005','P41','Thưởng KPI tháng 10',720000,'2025-10-31'),
('B1006','P42','Thưởng chuyên cần',500000,'2025-10-31'),
('B1007','P43','Thưởng chuyên cần',500000,'2025-10-31'),
('B1008','P44','Thưởng chuyên cần',500000,'2025-10-31'),
('B1009','P45','Thưởng chuyên cần',500000,'2025-10-31');

-- ===== THÁNG 11 =====
INSERT INTO EMPLOYEE_BONUS VALUES
('B1101','P46','Thưởng KPI tháng 11',950000,'2025-11-30'),
('B1102','P47','Thưởng KPI tháng 11',900000,'2025-11-30'),
('B1103','P48','Thưởng KPI tháng 11',780000,'2025-11-30'),
('B1104','P49','Thưởng KPI tháng 11',990000,'2025-11-30'),
('B1105','P50','Thưởng KPI tháng 11',750000,'2025-11-30'),
('B1106','P51','Thưởng chuyên cần',500000,'2025-11-30'),
('B1107','P52','Thưởng chuyên cần',500000,'2025-11-30'),
('B1108','P53','Thưởng chuyên cần',500000,'2025-11-30'),
('B1109','P54','Thưởng chuyên cần',500000,'2025-11-30');

-- ===== THÁNG 12 =====
INSERT INTO EMPLOYEE_BONUS VALUES
('B1201','P55','Thưởng KPI tháng 12',1000000,'2025-12-31'),
('B1202','P56','Thưởng KPI tháng 12',950000,'2025-12-31'),
('B1203','P57','Thưởng KPI tháng 12',800000,'2025-12-31'),
('B1204','P58','Thưởng KPI tháng 12',1100000,'2025-12-31'),
('B1205','P59','Thưởng KPI tháng 12',850000,'2025-12-31'),
('B1206','P60','Thưởng chuyên cần',600000,'2025-12-31'),
('B1207','P61','Thưởng chuyên cần',600000,'2025-12-31'),
('B1208','P62','Thưởng chuyên cần',600000,'2025-12-31'),
('B1209','P63','Thưởng chuyên cần',600000,'2025-12-31');


-- =========================
--  PENALTY TEST DATA
-- =========================
INSERT INTO EMPLOYEE_PENALTY
(PenaltyID, PayrollID, Description, Amount, PenaltyDate)
VALUES
-- ===== THÁNG 6 =====
('PE0601','P01','Phạt đi trễ nhiều',300000,'2025-06-30'),
('PE0602','P02','Phạt đi trễ nhiều',400000,'2025-06-30'),
('PE0603','P03','Phạt đi trễ nhiều',500000,'2025-06-30'),
('PE0604','P04','Phạt đi trễ nhiều',200000,'2025-06-30'),
('PE0605','P05','Phạt đi trễ nhiều',350000,'2025-06-30'),
('PE0606','P06','Phạt đi trễ nhiều',450000,'2025-06-30'),
('PE0607','P07','Phạt đi trễ nhiều',250000,'2025-06-30'),
('PE0608','P08','Phạt đi trễ nhiều',300000,'2025-06-30'),
('PE0609','P09','Phạt đi trễ nhiều',500000,'2025-06-30'),

-- ===== THÁNG 7 =====
('PE0701','P10','Phạt đi trễ nhiều',300000,'2025-07-31'),
('PE0702','P11','Phạt đi trễ nhiều',400000,'2025-07-31'),
('PE0703','P12','Phạt đi trễ nhiều',500000,'2025-07-31'),
('PE0704','P13','Phạt đi trễ nhiều',200000,'2025-07-31'),
('PE0705','P14','Phạt đi trễ nhiều',350000,'2025-07-31'),
('PE0706','P15','Phạt đi trễ nhiều',450000,'2025-07-31'),
('PE0707','P16','Phạt đi trễ nhiều',250000,'2025-07-31'),
('PE0708','P17','Phạt đi trễ nhiều',300000,'2025-07-31'),
('PE0709','P18','Phạt đi trễ nhiều',500000,'2025-07-31'),

-- ===== THÁNG 8 =====
('PE0801','P19','Phạt đi trễ nhiều',300000,'2025-08-31'),
('PE0802','P20','Phạt đi trễ nhiều',400000,'2025-08-31'),
('PE0803','P21','Phạt đi trễ nhiều',500000,'2025-08-31'),
('PE0804','P22','Phạt đi trễ nhiều',200000,'2025-08-31'),
('PE0805','P23','Phạt đi trễ nhiều',350000,'2025-08-31'),
('PE0806','P24','Phạt đi trễ nhiều',450000,'2025-08-31'),
('PE0807','P25','Phạt đi trễ nhiều',250000,'2025-08-31'),
('PE0808','P26','Phạt đi trễ nhiều',300000,'2025-08-31'),
('PE0809','P27','Phạt đi trễ nhiều',500000,'2025-08-31'),

-- ===== THÁNG 9 =====
('PE0901','P28','Phạt đi trễ nhiều',300000,'2025-09-30'),
('PE0902','P29','Phạt đi trễ nhiều',400000,'2025-09-30'),
('PE0903','P30','Phạt đi trễ nhiều',500000,'2025-09-30'),
('PE0904','P31','Phạt đi trễ nhiều',200000,'2025-09-30'),
('PE0905','P32','Phạt đi trễ nhiều',350000,'2025-09-30'),
('PE0906','P33','Phạt đi trễ nhiều',450000,'2025-09-30'),
('PE0907','P34','Phạt đi trễ nhiều',250000,'2025-09-30'),
('PE0908','P35','Phạt đi trễ nhiều',300000,'2025-09-30'),
('PE0909','P36','Phạt đi trễ nhiều',500000,'2025-09-30'),

-- ===== THÁNG 10 =====
('PE1001','P37','Phạt đi trễ nhiều',300000,'2025-10-31'),
('PE1002','P38','Phạt đi trễ nhiều',400000,'2025-10-31'),
('PE1003','P39','Phạt đi trễ nhiều',500000,'2025-10-31'),
('PE1004','P40','Phạt đi trễ nhiều',200000,'2025-10-31'),
('PE1005','P41','Phạt đi trễ nhiều',350000,'2025-10-31'),
('PE1006','P42','Phạt đi trễ nhiều',450000,'2025-10-31'),
('PE1007','P43','Phạt đi trễ nhiều',250000,'2025-10-31'),
('PE1008','P44','Phạt đi trễ nhiều',300000,'2025-10-31'),
('PE1009','P45','Phạt đi trễ nhiều',500000,'2025-10-31'),

-- ===== THÁNG 11 =====
('PE1101','P46','Phạt đi trễ nhiều',300000,'2025-11-30'),
('PE1102','P47','Phạt đi trễ nhiều',400000,'2025-11-30'),
('PE1103','P48','Phạt đi trễ nhiều',500000,'2025-11-30'),
('PE1104','P49','Phạt đi trễ nhiều',200000,'2025-11-30'),
('PE1105','P50','Phạt đi trễ nhiều',350000,'2025-11-30'),
('PE1106','P51','Phạt đi trễ nhiều',450000,'2025-11-30'),
('PE1107','P52','Phạt đi trễ nhiều',250000,'2025-11-30'),
('PE1108','P53','Phạt đi trễ nhiều',300000,'2025-11-30'),
('PE1109','P54','Phạt đi trễ nhiều',500000,'2025-11-30'),

-- ===== THÁNG 12 =====
('PE1201','P55','Phạt đi trễ nhiều',300000,'2025-12-31'),
('PE1202','P56','Phạt đi trễ nhiều',400000,'2025-12-31'),
('PE1203','P57','Phạt đi trễ nhiều',500000,'2025-12-31'),
('PE1204','P58','Phạt đi trễ nhiều',200000,'2025-12-31'),
('PE1205','P59','Phạt đi trễ nhiều',350000,'2025-12-31'),
('PE1206','P60','Phạt đi trễ nhiều',450000,'2025-12-31'),
('PE1207','P61','Phạt đi trễ nhiều',250000,'2025-12-31'),
('PE1208','P62','Phạt đi trễ nhiều',300000,'2025-12-31'),
('PE1209','P63','Phạt đi trễ nhiều',500000,'2025-12-31');




-- 3. Tính lương
CALL sp_calculate_payroll_by_month(6, 2025);
CALL sp_calculate_payroll_by_month(7, 2025);
CALL sp_calculate_payroll_by_month(8, 2025);
CALL sp_calculate_payroll_by_month(9, 2025);
CALL sp_calculate_payroll_by_month(10, 2025);
CALL sp_calculate_payroll_by_month(11, 2025);
CALL sp_calculate_payroll_by_month(12, 2025);



-- =========================
-- VIEW: EMPLOYEE SALARY (REAL TIME)
-- =========================
CREATE OR REPLACE VIEW vw_EmployeeSalary AS
SELECT
    e.EmployeeID,
    e.FullName,
    p.Month,
    p.Year,
    p.WorkDays,
    p.BaseSalary,
    p.Bonus,
    p.Penalty,
    p.TotalSalary
FROM EMPLOYEE e
JOIN EMPLOYEE_PAYROLL p
    ON e.EmployeeID = p.EmployeeID
ORDER BY p.Year, p.Month, e.EmployeeID;
-- SELECT * FROM vw_EmployeeSalary;

-- NGOC

-- =========================
-- TABLE: PRODUCT_CATEGORY
-- =========================
CREATE TABLE IF NOT EXISTS PRODUCT_CATEGORY (
    CategoryID   VARCHAR(30)  PRIMARY KEY,
    CategoryName VARCHAR(100) NOT NULL
);

-- =========================
-- TABLE: PRODUCT
-- =========================
CREATE TABLE IF NOT EXISTS PRODUCT (
    ProductID    VARCHAR(30)  PRIMARY KEY,
    ProductName  VARCHAR(150) NOT NULL,
    CategoryID   VARCHAR(30)  NOT NULL,
    Quantity     INT NOT NULL DEFAULT 0,
    Unit         VARCHAR(20),
    CostPrice   DECIMAL(12,2) NOT NULL DEFAULT 0,
    Price        DECIMAL(12,2) NOT NULL DEFAULT 0,
    Description  VARCHAR(255),
    Image        VARCHAR(255),

    CONSTRAINT fk_product_category
        FOREIGN KEY (CategoryID)
        REFERENCES PRODUCT_CATEGORY(CategoryID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ;

CREATE INDEX idx_product_category ON PRODUCT(CategoryID);
CREATE INDEX idx_product_name ON PRODUCT(ProductName);

-- =========================
-- TABLE: PROMOTION
-- =========================
-- ===== PROMOTION =====
CREATE TABLE IF NOT EXISTS PROMOTION (
    PromoID     VARCHAR(30) PRIMARY KEY,
    PromoName   VARCHAR(100) NOT NULL,
    Description VARCHAR(255),
    PromoType   ENUM('percent','fixed') NOT NULL,
    Value       DECIMAL(12,2) NOT NULL DEFAULT 0,
    Status      ENUM('Active','Inactive') NOT NULL DEFAULT 'Active'
);

CREATE INDEX idx_promotion_status ON PROMOTION(Status);

-- ===== PROMOTION_PRODUCT =====
CREATE TABLE IF NOT EXISTS PROMOTION_PRODUCT (
    PromoID   VARCHAR(30) NOT NULL,
    ProductID VARCHAR(30) NOT NULL,
    PRIMARY KEY (PromoID, ProductID),
    UNIQUE (ProductID),

    CONSTRAINT fk_pp_promo
        FOREIGN KEY (PromoID)
        REFERENCES PROMOTION(PromoID)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_pp_product
        FOREIGN KEY (ProductID)
        REFERENCES PRODUCT(ProductID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE INDEX idx_pp_product ON PROMOTION_PRODUCT(ProductID);
CREATE INDEX idx_pp_promo   ON PROMOTION_PRODUCT(PromoID);

-- ===== CATEGORY =====
INSERT INTO PRODUCT_CATEGORY (CategoryID, CategoryName) VALUES
('C01', 'Cake'),
('C02', 'Baked'),
('C03', 'Cookie');

-- ===== PRODUCT (phải insert trước PROMOTION_PRODUCT) =====
INSERT INTO PRODUCT (ProductID, ProductName, CategoryID, Quantity, Unit, Price, Description, Image) VALUES
('PD01', 'Strawberry Short Cake', 'C01', 8, 'slice', 160000, 'Fresh strawberry...', ''),
('PD02', 'Lemon Short Cake', 'C01', 8, 'slice', 120000, 'Lemon curd on top', ''),
('PD03', 'W Cheesecake', 'C01', 16, 'slice', 135000, '', ''),
('PD04', 'Matcha Chiffon', 'C01', 8, 'slice', 135000, '', ''),
('PD05', 'Earl Grey Chiffon', 'C01', 8, 'slice', 100000, '', ''),
('PD06', 'Whole wheat Cookie', 'C03', 16, 'pack', 38000, '', ''),
('PD07', 'Nut Cookie', 'C03', 8, 'pack', 38000, '', ''),
('PD08', 'Choco Fondue', 'C03', 8, 'jar', 120000, '', ''),
('PD09', 'Choco Merigue', 'C03', 5, 'pack', 45000, '', ''),
('PD10', 'Lemon Cake', 'C02', 8, 'pack', 70000, '', ''),
('PD11', 'Choco Muffin', 'C02', 8, 'pack', 75000, '', ''),
('PD12', 'Earl Grey Financier', 'C02', 16, 'pack', 48000, '', '');


-- ===== PROMOTION (phải có StartTime/EndTime) =====
INSERT INTO PROMOTION
(PromoID, PromoName, Description, PromoType, Value, Status)
VALUES
('PR01', 'Bread Discount', 'Discount for bread products', 'percent', 10, 'Active'),
('PR02', 'Cake Special', 'Special discount for cakes', 'fixed', 20.00, 'Active'),
('PR03', 'Cookie Promo', 'Discount for cookie products',  'percent', 5, 'Inactive');

-- ===== PROMOTION_PRODUCT =====
INSERT INTO PROMOTION_PRODUCT (PromoID, ProductID) VALUES
('PR02', 'PD01'),
('PR02', 'PD02');


/* =========================================================
   IMPORT (header)
   - 1 lần restock = 1 ImportID
   ========================================================= */
CREATE TABLE IF NOT EXISTS IMPORT (
    ImportID   VARCHAR(30) PRIMARY KEY,
    ImportTime DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    Note       VARCHAR(255)
);

CREATE INDEX idx_import_time ON IMPORT(ImportTime);

/* =========================================================
   IMPORT_DETAIL (detail)
   - 1 import có nhiều sản phẩm
   - lưu số lượng + giá vốn
   ========================================================= */
CREATE TABLE IF NOT EXISTS IMPORT_DETAIL (
    ImportID   VARCHAR(30) NOT NULL,
    ProductID  VARCHAR(30) NOT NULL,
    Quantity   INT NOT NULL,
    CostPrice  DECIMAL(12,2) NOT NULL DEFAULT 0,

    PRIMARY KEY (ImportID, ProductID),

    CONSTRAINT fk_id_import
        FOREIGN KEY (ImportID)
        REFERENCES IMPORT(ImportID)
        ON UPDATE CASCADE
        ON DELETE CASCADE,

    CONSTRAINT fk_id_product
        FOREIGN KEY (ProductID)
        REFERENCES PRODUCT(ProductID)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
);

CREATE INDEX idx_import_detail_product ON IMPORT_DETAIL(ProductID);

/* =========================================================
   DAILY RESET QUANTITY = 0 on new day
   ========================================================= */
/* =========================================
   SYSTEM CONFIG (for daily reset)
   ========================================= */
CREATE TABLE IF NOT EXISTS SYSTEM_CONFIG (
    ConfigKey   VARCHAR(50) PRIMARY KEY,
    ConfigValue VARCHAR(50) NOT NULL
);

INSERT IGNORE INTO SYSTEM_CONFIG (ConfigKey, ConfigValue)
VALUES ('LAST_RESET_DATE', '2000-01-01');


-- IMPORT sample (1 lần nhập)
INSERT INTO IMPORT (ImportID, Note) VALUES
('IM001', 'Morning restock');

INSERT INTO IMPORT_DETAIL (ImportID, ProductID, Quantity, CostPrice) VALUES
('IM001', 'PD01', 6, 80000),
('IM001', 'PD02', 7, 60000),
('IM001', 'PD03', 5, 90000),
('IM001', 'PD04', 8, 85000),
('IM001', 'PD05', 6, 55000),
('IM001', 'PD06', 7, 50000),
('IM001', 'PD07', 5, 52000),
('IM001', 'PD08', 8, 70000),
('IM001', 'PD09', 6, 65000),
('IM001', 'PD10', 7, 60000),
('IM001', 'PD11', 5, 75000),
('IM001', 'PD12', 8, 80000);


-- Sync tồn kho + giá vốn theo import IM001
UPDATE PRODUCT p
JOIN IMPORT_DETAIL d ON p.ProductID = d.ProductID
SET 
    p.Quantity = d.Quantity,
    p.CostPrice = d.CostPrice
WHERE d.ImportID = 'IM001';

-- Huy id them AUTO_INCREMENT
CREATE TABLE `orders` (
  `OrderID` int(11) NOT NULL AUTO_INCREMENT,
  `OrderDate` datetime NOT NULL DEFAULT current_timestamp(),
  `CustomerID` int(11) NOT NULL,
  `Total` decimal(12,2) NOT NULL,
  `PaymentMethod` enum('Transfer','Cash') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;


CREATE TABLE `orderdetail` (
  `OrderDetailID` int(11) NOT NULL AUTO_INCREMENT,
  `OrderID` int(11) NOT NULL,
  `ProductID` varchar(30) NOT NULL,
  `Quantity` int(11) NOT NULL,
  `UnitPrice` decimal(12,2) NOT NULL,
  `CostPrice` decimal(12,2) NOT NULL,
  `PromoID` varchar(30) DEFAULT NULL,
  `DiscountAmount` decimal(12,2) DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;



CREATE TABLE `customer` (
  `CustomerID` int(10) NOT NULL AUTO_INCREMENT,
  `FullName` varchar(100) NOT NULL,
  `Phone` varchar(20) NOT NULL,
  `Gender` enum('Male','Female') NOT NULL,
  `DOB` date NOT NULL,
  `Email` varchar(100) DEFAULT NULL,
  `Address` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

DELIMITER $$

CREATE PROCEDURE sp_GenerateOrderData_Year2025()
BEGIN
    DECLARE v_month INT DEFAULT 1;
    DECLARE v_order_count INT;
    DECLARE v_i INT;

    DECLARE v_order_id INT;
    DECLARE v_customer INT;
    DECLARE v_pay ENUM('Cash','Transfer');
    DECLARE v_order_date DATETIME;

    DECLARE v_product VARCHAR(30);
    DECLARE v_qty INT;
    DECLARE v_price DECIMAL(12,2);
    DECLARE v_cost DECIMAL(12,2);
    DECLARE v_discount DECIMAL(12,2);
    DECLARE v_total DECIMAL(12,2);

    WHILE v_month <= 12 DO

        -- mỗi tháng 10–20 order
        SET v_order_count = 10 + FLOOR(RAND() * 11);
        SET v_i = 1;

        WHILE v_i <= v_order_count DO
            SET v_total = 0;

            SET v_customer = FLOOR(1 + RAND() * 3);
            SET v_pay = IF(RAND() > 0.5, 'Cash', 'Transfer');

            SET v_order_date =
                STR_TO_DATE(
                    CONCAT(
                        '2025-',
                        LPAD(v_month,2,'0'),
                        '-',
                        LPAD(1 + FLOOR(RAND()*26),2,'0'),
                        ' ',
                        LPAD(8 + FLOOR(RAND()*10),2,'0'),
                        ':',
                        LPAD(FLOOR(RAND()*60),2,'0'),
                        ':00'
                    ),
                    '%Y-%m-%d %H:%i:%s'
                );

            INSERT INTO orders (OrderDate, CustomerID, Total, PaymentMethod)
            VALUES (v_order_date, v_customer, 0, v_pay);

            SET v_order_id = LAST_INSERT_ID();

            -- mỗi order 1–4 sản phẩm
            SET @item_count = 1 + FLOOR(RAND() * 4);
            SET @j = 1;

            WHILE @j <= @item_count DO

                SELECT ProductID, Price, CostPrice
                INTO v_product, v_price, v_cost
                FROM PRODUCT
                WHERE Status = 'Active'
                ORDER BY RAND()
                LIMIT 1;

                SET v_qty = 1 + FLOOR(RAND() * 3);

                -- discount cho cake
                IF v_product IN ('PD01','PD02') AND RAND() > 0.5 THEN
                    SET v_discount = 20000;
                    SET @promo = 'PR02';
                ELSE
                    SET v_discount = 0;
                    SET @promo = NULL;
                END IF;

                INSERT INTO orderdetail
                (OrderID, ProductID, Quantity, UnitPrice, CostPrice, PromoID, DiscountAmount)
                VALUES
                (v_order_id, v_product, v_qty, v_price, v_cost, @promo, v_discount);

                SET v_total = v_total + (v_price * v_qty - v_discount);

                SET @j = @j + 1;
            END WHILE;

            UPDATE orders
            SET Total = v_total
            WHERE OrderID = v_order_id;

            SET v_i = v_i + 1;
        END WHILE;

        SET v_month = v_month + 1;
    END WHILE;
END$$

DELIMITER ;
--test data
CALL sp_GenerateOrderData_Year2025();


INSERT INTO `customer` (`CustomerID`, `FullName`, `Phone`, `Gender`, `DOB`, `Email`, `Address`) VALUES
(1, 'Huy', '0967923921', 'Male', '2015-01-06', NULL, NULL),
(2, 'Ngoc', '0911223344', 'Female', '2015-12-02', NULL, NULL),
(3, 'Visitor', '000000', 'Male', '0000-00-00', NULL, NULL),
(4, 'Nguyen Van An', '0901234567', 'Male', '1995-05-15', 'vana@gmail.com', '123 Le Loi, Quan 1'),
(5, 'Tran Thi Binh', '0912345678', 'Female', '1998-10-20', 'thib@yahoo.com', '456 Nguyen Hue, Quan 1'),
(6, 'Le Van Chuong', '0923456789', 'Male', '1990-01-05', 'vanc@gmail.com', '789 Vo Van Kiet, Quan 5'),
(7, 'Pham Minh Dot', '0934567890', 'Male', '2000-12-12', 'minhd@outlook.com', '101 Cach Mang Thang 8, Tan Binh'),
(8, 'Hoang Bao Em', '0945678901', 'Female', '1993-07-25', 'baoe@gmail.com', '202 Ly Thuong Kiet, Quan 10'),
(9, 'Do Thanh Phuc', '0956789012', 'Male', '1988-03-30', 'thanhf@gmail.com', '303 Phan Dang Luu, Phu Nhuan'),
(10, 'Bui Tuyet Nga', '0967890123', 'Female', '1997-09-18', 'tuyetg@hotmail.com', '404 Dien Bien Phu, Binh Thanh'),
(11, 'Dang Quang Hong', '0978901234', 'Male', '1992-11-11', 'quangh@gmail.com', '505 Hoang Van Thu, Tan Binh'),
(12, 'Vu Hoang Uy', '0989012345', 'Female', '1996-02-28', 'hoangi@gmail.com', '606 Cong Hoa, Tan Binh'),
(13, 'Luu Gia Kha', '0990123456', 'Male', '1991-06-10', 'giak@gmail.com', '707 Tran Hung Dao, Quan 5');




INSERT INTO `orderdetail` (`OrderDetailID`, `OrderID`, `ProductID`, `Quantity`, `UnitPrice`, `CostPrice`, `PromoID`, `DiscountAmount`) VALUES
(1, 1, 'PD01', 2, 160000.00, 80000.00, 'PR02', 20000.00),
(2, 2, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(3, 3, 'PD02', 2, 120000.00, 60000.00, NULL, 0.00),
(4, 4, 'PD05', 1, 100000.00, 55000.00, NULL, 0.00),
(5, 5, 'PD08', 1, 120000.00, 70000.00, NULL, 0.00),
(6, 6, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(7, 6, 'PD11', 1, 80000.00, 75000.00, NULL, 0.00),
(8, 7, 'PD12', 1, 48000.00, 80000.00, NULL, 0.00),
(9, 8, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(10, 9, 'PD07', 2, 38000.00, 52000.00, 'PR03', 1900.00),
(11, 10, 'PD08', 1, 120000.00, 70000.00, NULL, 0.00),
(12, 11, 'PD01', 2, 120000.00, 80000.00, 'PR02', 20000.00),
(13, 12, 'PD02', 2, 80000.00, 60000.00, NULL, 0.00),
(14, 14, 'PD08', 1, 70000.00, 120000.00, NULL, 0.00),
(15, 15, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(16, 17, 'PD01', 2, 160000.00, 80000.00, NULL, 0.00),
(17, 18, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(18, 19, 'PD04', 1, 85000.00, 85000.00, NULL, 0.00),
(19, 21, 'PD05', 1, 55000.00, 100000.00, NULL, 0.00),
(20, 22, 'PD08', 1, 120000.00, 70000.00, NULL, 0.00),
(21, 24, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(22, 25, 'PD03', 1, 135000.00, 90000.00, NULL, 0.00),
(23, 13, 'PD01', 3, 150000.00, 80000.00, 'PR02', 20000.00),
(24, 16, 'PD10', 2, 60000.00, 70000.00, NULL, 0.00),
(25, 20, 'PD04', 2, 135000.00, 85000.00, NULL, 0.00),
(26, 23, 'PD11', 3, 80000.00, 75000.00, NULL, 0.00),
(27, 26, 'PD11', 4, 75000.00, 75000.00, NULL, 0.00),
(28, 27, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(29, 28, 'PD01', 3, 160000.00, 80000.00, NULL, 0.00),
(30, 29, 'PD01', 2, 160000.00, 80000.00, NULL, 0.00),
(31, 30, 'PD03', 2, 135000.00, 90000.00, NULL, 0.00),
(32, 31, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(33, 32, 'PD02', 2, 60000.00, 60000.00, NULL, 0.00),
(34, 33, 'PD05', 1, 100000.00, 55000.00, NULL, 0.00),
(35, 34, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(36, 34, 'PD11', 1, 80000.00, 75000.00, NULL, 0.00),
(37, 35, 'PD08', 1, 120000.00, 70000.00, NULL, 0.00),
(38, 36, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(39, 37, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(40, 38, 'PD02', 2, 80000.00, 60000.00, NULL, 0.00),
(41, 39, 'PD03', 2, 135000.00, 90000.00, NULL, 0.00),
(42, 40, 'PD04', 1, 85000.00, 85000.00, NULL, 0.00),
(43, 41, 'PD08', 1, 120000.00, 70000.00, NULL, 0.00),
(44, 42, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(45, 43, 'PD03', 2, 135000.00, 90000.00, NULL, 0.00),
(46, 44, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(47, 45, 'PD05', 1, 100000.00, 55000.00, NULL, 0.00),
(48, 46, 'PD11', 3, 80000.00, 75000.00, NULL, 0.00),
(49, 47, 'PD02', 2, 80000.00, 60000.00, NULL, 0.00),
(50, 48, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(51, 49, 'PD07', 2, 38000.00, 52000.00, 'PR03', 1900.00),
(52, 50, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(53, 51, 'PD01', 2, 160000.00, 80000.00, NULL, 0.00),
(54, 52, 'PD01', 3, 150000.00, 80000.00, 'PR02', 10000.00),
(55, 53, 'PD04', 2, 135000.00, 85000.00, NULL, 0.00),
(56, 54, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(57, 55, 'PD11', 3, 80000.00, 75000.00, NULL, 0.00),
(58, 56, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(59, 57, 'PD11', 3, 80000.00, 75000.00, NULL, 0.00),
(60, 58, 'PD01', 2, 160000.00, 80000.00, NULL, 0.00),
(61, 59, 'PD01', 3, 160000.00, 80000.00, NULL, 0.00),
(62, 60, 'PD03', 2, 135000.00, 90000.00, NULL, 0.00),
(63, 61, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(64, 62, 'PD08', 1, 120000.00, 70000.00, NULL, 0.00),
(65, 63, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(66, 64, 'PD11', 3, 80000.00, 75000.00, NULL, 0.00),
(67, 65, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(68, 66, 'PD02', 2, 80000.00, 60000.00, NULL, 0.00),
(69, 67, 'PD03', 1, 135000.00, 90000.00, NULL, 0.00),
(70, 68, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(71, 69, 'PD04', 2, 135000.00, 85000.00, NULL, 0.00),
(72, 70, 'PD08', 1, 120000.00, 70000.00, NULL, 0.00),
(73, 71, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(74, 72, 'PD11', 3, 80000.00, 75000.00, NULL, 0.00),
(75, 73, 'PD01', 2, 160000.00, 80000.00, NULL, 0.00),
(76, 74, 'PD01', 3, 160000.00, 80000.00, NULL, 0.00),
(77, 75, 'PD04', 2, 135000.00, 85000.00, NULL, 0.00),
(78, 76, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(79, 77, 'PD08', 1, 120000.00, 70000.00, NULL, 0.00),
(80, 78, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(81, 79, 'PD11', 3, 80000.00, 75000.00, NULL, 0.00),
(82, 80, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(83, 81, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(84, 82, 'PD03', 1, 135000.00, 90000.00, NULL, 0.00),
(85, 83, 'PD02', 2, 80000.00, 60000.00, NULL, 0.00),
(86, 84, 'PD04', 2, 135000.00, 85000.00, NULL, 0.00),
(87, 85, 'PD08', 1, 120000.00, 70000.00, NULL, 0.00),
(88, 86, 'PD01', 2, 160000.00, 80000.00, NULL, 0.00),
(89, 87, 'PD02', 2, 80000.00, 60000.00, NULL, 0.00),
(90, 88, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(91, 89, 'PD11', 3, 80000.00, 75000.00, NULL, 0.00),
(92, 90, 'PD01', 3, 150000.00, 80000.00, 'PR02', 10000.00),
(93, 91, 'PD08', 1, 120000.00, 70000.00, NULL, 0.00),
(94, 92, 'PD04', 2, 135000.00, 85000.00, NULL, 0.00),
(95, 93, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(96, 94, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(97, 95, 'PD01', 2, 160000.00, 80000.00, NULL, 0.00),
(98, 96, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(99, 97, 'PD11', 3, 80000.00, 75000.00, NULL, 0.00),
(100, 98, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(101, 99, 'PD02', 2, 80000.00, 60000.00, NULL, 0.00),
(102, 100, 'PD04', 2, 135000.00, 85000.00, NULL, 0.00),
(103, 101, 'PD08', 1, 120000.00, 70000.00, NULL, 0.00),
(104, 102, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(105, 103, 'PD04', 2, 135000.00, 85000.00, NULL, 0.00),
(106, 104, 'PD03', 1, 135000.00, 90000.00, NULL, 0.00),
(107, 105, 'PD05', 1, 100000.00, 55000.00, NULL, 0.00),
(108, 106, 'PD11', 3, 80000.00, 75000.00, NULL, 0.00),
(109, 107, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(110, 108, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(111, 109, 'PD07', 2, 38000.00, 52000.00, 'PR03', 1900.00),
(112, 110, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(113, 111, 'PD01', 2, 160000.00, 80000.00, NULL, 0.00),
(114, 112, 'PD04', 1, 135000.00, 85000.00, NULL, 0.00),
(115, 113, 'PD02', 2, 80000.00, 60000.00, NULL, 0.00),
(116, 114, 'PD04', 2, 135000.00, 85000.00, NULL, 0.00),
(117, 115, 'PD08', 1, 120000.00, 70000.00, NULL, 0.00),
(118, 116, 'PD01', 2, 160000.00, 80000.00, NULL, 0.00),
(119, 117, 'PD01', 3, 150000.00, 80000.00, 'PR02', 10000.00),
(120, 118, 'PD03', 2, 135000.00, 90000.00, NULL, 0.00),
(121, 119, 'PD01', 1, 160000.00, 80000.00, NULL, 0.00),
(122, 123, 'PD08', 2, 120000.00, 70000.00, NULL, 0.00),
(123, 128, 'PD01', 3, 150000.00, 80000.00, 'PR02', 10000.00),
(124, 129, 'PD01', 2, 160000.00, 80000.00, NULL, 0.00),
(125, 136, 'PD01', 3, 160000.00, 80000.00, NULL, 0.00),
(126, 139, 'PD03', 3, 150000.00, 90000.00, 'PR02', 15000.00),
(127, 140, 'PD01', 4, 160000.00, 80000.00, NULL, 0.00),
(128, 141, 'PD01', 2, 160000.00, 80000.00, NULL, 0.00),
(129, 144, 'PD01', 3, 160000.00, 80000.00, NULL, 0.00);


INSERT INTO `orders` (`OrderID`, `OrderDate`, `CustomerID`, `Total`, `PaymentMethod`) VALUES
(1, '2025-01-05 10:30:00', 1, 320000.00, 'Cash'),
(2, '2025-01-07 14:15:00', 4, 135000.00, 'Transfer'),
(3, '2025-01-10 09:00:00', 5, 240000.00, 'Cash'),
(4, '2025-01-12 16:45:00', 2, 100000.00, 'Transfer'),
(5, '2025-01-15 11:20:00', 8, 120000.00, 'Cash'),
(6, '2025-01-18 19:10:00', 10, 215000.00, 'Transfer'),
(7, '2025-01-20 08:30:00', 12, 48000.00, 'Cash'),
(8, '2025-01-22 15:00:00', 3, 160000.00, 'Cash'),
(9, '2025-01-25 13:00:00', 7, 76000.00, 'Transfer'),
(10, '2025-01-28 17:30:00', 13, 120000.00, 'Cash'),
(11, '2025-02-01 08:30:00', 3, 240000.00, 'Cash'),
(12, '2025-02-02 09:45:00', 3, 160000.00, 'Cash'),
(13, '2025-02-05 14:20:00', 1, 450000.00, 'Transfer'),
(14, '2025-02-07 10:10:00', 3, 70000.00, 'Cash'),
(15, '2025-02-10 16:00:00', 3, 135000.00, 'Cash'),
(16, '2025-02-12 11:30:00', 5, 120000.00, 'Transfer'),
(17, '2025-02-14 19:00:00', 3, 320000.00, 'Cash'),
(18, '2025-02-14 20:30:00', 3, 160000.00, 'Cash'),
(19, '2025-02-16 08:00:00', 3, 85000.00, 'Cash'),
(20, '2025-02-18 15:45:00', 2, 270000.00, 'Transfer'),
(21, '2025-02-20 12:00:00', 3, 55000.00, 'Cash'),
(22, '2025-02-22 13:20:00', 3, 120000.00, 'Cash'),
(23, '2025-02-25 10:00:00', 8, 240000.00, 'Transfer'),
(24, '2025-02-26 17:15:00', 3, 160000.00, 'Cash'),
(25, '2025-02-28 21:00:00', 3, 135000.00, 'Cash'),
(26, '2025-03-01 10:00:00', 3, 300000.00, 'Cash'),
(27, '2025-03-03 15:30:00', 7, 160000.00, 'Transfer'),
(28, '2025-03-07 09:15:00', 3, 480000.00, 'Cash'),
(29, '2025-03-08 11:00:00', 3, 320000.00, 'Cash'),
(30, '2025-03-08 16:45:00', 10, 270000.00, 'Transfer'),
(31, '2025-03-10 14:20:00', 3, 135000.00, 'Cash'),
(32, '2025-03-12 08:45:00', 5, 120000.00, 'Transfer'),
(33, '2025-03-15 13:10:00', 3, 100000.00, 'Cash'),
(34, '2025-03-18 17:30:00', 2, 240000.00, 'Transfer'),
(35, '2025-03-20 10:20:00', 3, 120000.00, 'Cash'),
(36, '2025-03-22 15:50:00', 12, 160000.00, 'Cash'),
(37, '2025-03-25 09:00:00', 3, 135000.00, 'Cash'),
(38, '2025-03-27 19:15:00', 3, 160000.00, 'Cash'),
(39, '2025-03-29 14:00:00', 8, 270000.00, 'Transfer'),
(40, '2025-03-31 20:30:00', 3, 85000.00, 'Cash'),
(41, '2025-04-02 09:00:00', 3, 120000.00, 'Cash'),
(42, '2025-04-05 14:20:00', 3, 160000.00, 'Cash'),
(43, '2025-04-08 10:30:00', 1, 270000.00, 'Transfer'),
(44, '2025-04-10 16:00:00', 3, 135000.00, 'Cash'),
(45, '2025-04-12 11:15:00', 12, 100000.00, 'Cash'),
(46, '2025-04-15 08:45:00', 3, 240000.00, 'Cash'),
(47, '2025-04-18 19:00:00', 3, 160000.00, 'Cash'),
(48, '2025-04-20 13:20:00', 5, 135000.00, 'Transfer'),
(49, '2025-04-22 15:00:00', 3, 76000.00, 'Cash'),
(50, '2025-04-25 10:00:00', 3, 160000.00, 'Cash'),
(51, '2025-04-28 17:30:00', 10, 320000.00, 'Transfer'),
(52, '2025-04-29 18:00:00', 3, 450000.00, 'Cash'),
(53, '2025-04-30 09:00:00', 3, 270000.00, 'Cash'),
(54, '2025-04-30 14:30:00', 3, 160000.00, 'Cash'),
(55, '2025-04-30 19:45:00', 2, 240000.00, 'Transfer'),
(56, '2025-05-02 10:00:00', 3, 160000.00, 'Cash'),
(57, '2025-05-05 15:30:00', 8, 240000.00, 'Transfer'),
(58, '2025-05-08 09:15:00', 3, 320000.00, 'Cash'),
(59, '2025-05-11 11:00:00', 3, 480000.00, 'Cash'),
(60, '2025-05-11 16:45:00', 5, 270000.00, 'Transfer'),
(61, '2025-05-14 14:20:00', 3, 135000.00, 'Cash'),
(62, '2025-05-16 08:45:00', 13, 120000.00, 'Cash'),
(63, '2025-05-19 13:10:00', 3, 160000.00, 'Cash'),
(64, '2025-05-21 17:30:00', 3, 240000.00, 'Cash'),
(65, '2025-05-23 10:20:00', 1, 135000.00, 'Transfer'),
(66, '2025-05-25 15:50:00', 3, 160000.00, 'Cash'),
(67, '2025-05-27 09:00:00', 3, 135000.00, 'Cash'),
(68, '2025-05-29 19:15:00', 12, 160000.00, 'Cash'),
(69, '2025-05-30 14:00:00', 3, 270000.00, 'Cash'),
(70, '2025-05-31 20:30:00', 3, 120000.00, 'Cash'),
(71, '2025-06-02 10:00:00', 3, 160000.00, 'Cash'),
(72, '2025-06-05 15:30:00', 3, 240000.00, 'Cash'),
(73, '2025-06-08 09:15:00', 5, 320000.00, 'Transfer'),
(74, '2025-06-12 11:00:00', 3, 480000.00, 'Cash'),
(75, '2025-06-15 16:45:00', 3, 270000.00, 'Cash'),
(76, '2025-06-18 14:20:00', 1, 135000.00, 'Transfer'),
(77, '2025-06-21 08:45:00', 3, 120000.00, 'Cash'),
(78, '2025-06-23 13:10:00', 3, 160000.00, 'Cash'),
(79, '2025-06-25 17:30:00', 3, 240000.00, 'Cash'),
(80, '2025-06-26 10:20:00', 12, 135000.00, 'Cash'),
(81, '2025-06-27 15:50:00', 3, 160000.00, 'Cash'),
(82, '2025-06-28 09:00:00', 3, 135000.00, 'Cash'),
(83, '2025-06-29 19:15:00', 3, 160000.00, 'Cash'),
(84, '2025-06-30 14:00:00', 7, 270000.00, 'Transfer'),
(85, '2025-06-30 20:30:00', 3, 120000.00, 'Cash'),
(86, '2025-07-02 09:30:00', 3, 320000.00, 'Cash'),
(87, '2025-07-04 14:15:00', 3, 160000.00, 'Cash'),
(88, '2025-07-07 10:00:00', 3, 135000.00, 'Cash'),
(89, '2025-07-10 16:45:00', 3, 240000.00, 'Cash'),
(90, '2025-07-12 11:20:00', 2, 450000.00, 'Transfer'),
(91, '2025-07-15 08:30:00', 3, 120000.00, 'Cash'),
(92, '2025-07-18 19:10:00', 3, 270000.00, 'Cash'),
(93, '2025-07-20 13:00:00', 3, 160000.00, 'Cash'),
(94, '2025-07-22 15:45:00', 8, 135000.00, 'Transfer'),
(95, '2025-07-25 10:00:00', 3, 320000.00, 'Cash'),
(96, '2025-07-26 17:30:00', 3, 160000.00, 'Cash'),
(97, '2025-07-28 18:00:00', 10, 240000.00, 'Transfer'),
(98, '2025-07-29 09:00:00', 3, 135000.00, 'Cash'),
(99, '2025-07-30 14:30:00', 3, 160000.00, 'Cash'),
(100, '2025-07-31 19:45:00', 3, 270000.00, 'Cash'),
(101, '2025-08-02 10:00:00', 3, 120000.00, 'Cash'),
(102, '2025-08-05 15:30:00', 3, 160000.00, 'Cash'),
(103, '2025-08-08 09:15:00', 5, 270000.00, 'Transfer'),
(104, '2025-08-11 11:00:00', 3, 135000.00, 'Cash'),
(105, '2025-08-14 16:45:00', 1, 100000.00, 'Transfer'),
(106, '2025-08-16 14:20:00', 3, 240000.00, 'Cash'),
(107, '2025-08-18 08:45:00', 3, 160000.00, 'Cash'),
(108, '2025-08-20 13:10:00', 3, 135000.00, 'Cash'),
(109, '2025-08-22 17:30:00', 12, 76000.00, 'Cash'),
(110, '2025-08-24 10:20:00', 3, 160000.00, 'Cash'),
(111, '2025-08-25 15:50:00', 3, 320000.00, 'Cash'),
(112, '2025-08-27 09:00:00', 3, 135000.00, 'Cash'),
(113, '2025-08-28 19:15:00', 13, 160000.00, 'Cash'),
(114, '2025-08-30 14:00:00', 3, 270000.00, 'Cash'),
(115, '2025-08-31 20:30:00', 3, 120000.00, 'Cash'),
(116, '2025-09-05 10:00:00', 3, 320000.00, 'Cash'),
(117, '2025-09-10 15:30:00', 3, 450000.00, 'Cash'),
(118, '2025-09-15 09:15:00', 5, 270000.00, 'Transfer'),
(119, '2025-09-20 11:00:00', 3, 160000.00, 'Cash'),
(120, '2025-09-25 16:45:00', 3, 135000.00, 'Cash'),
(121, '2025-09-28 14:20:00', 1, 320000.00, 'Transfer'),
(122, '2025-09-30 08:45:00', 3, 160000.00, 'Cash'),
(123, '2025-10-05 13:10:00', 3, 240000.00, 'Cash'),
(124, '2025-10-12 17:30:00', 12, 120000.00, 'Cash'),
(125, '2025-10-15 10:20:00', 3, 160000.00, 'Cash'),
(126, '2025-10-20 15:50:00', 3, 270000.00, 'Cash'),
(127, '2025-10-25 09:00:00', 3, 135000.00, 'Cash'),
(128, '2025-10-30 19:15:00', 3, 450000.00, 'Cash'),
(129, '2025-10-31 20:00:00', 3, 320000.00, 'Cash'),
(130, '2025-11-05 14:00:00', 7, 240000.00, 'Transfer'),
(131, '2025-11-10 10:30:00', 3, 160000.00, 'Cash'),
(132, '2025-11-15 16:00:00', 3, 135000.00, 'Cash'),
(133, '2025-11-20 09:45:00', 2, 270000.00, 'Transfer'),
(134, '2025-11-25 11:20:00', 3, 160000.00, 'Cash'),
(135, '2025-11-28 19:10:00', 3, 120000.00, 'Cash'),
(136, '2025-12-05 08:30:00', 3, 480000.00, 'Cash'),
(137, '2025-12-10 13:00:00', 8, 320000.00, 'Transfer'),
(138, '2025-12-15 15:45:00', 3, 160000.00, 'Cash'),
(139, '2025-12-20 10:00:00', 3, 450000.00, 'Cash'),
(140, '2025-12-24 18:00:00', 3, 640000.00, 'Cash'),
(141, '2025-12-25 11:00:00', 3, 320000.00, 'Cash'),
(142, '2025-12-28 14:30:00', 10, 270000.00, 'Transfer'),
(143, '2025-12-30 19:45:00', 3, 160000.00, 'Cash'),
(144, '2025-12-31 21:00:00', 3, 480000.00, 'Cash');



-- CHỈNH SỬA BẢNG PRODUCT, THÊM ACTIVE, DEACTIVE
ALTER TABLE PRODUCT
ADD COLUMN Status ENUM('Active','Inactive') NOT NULL DEFAULT 'Active';

CREATE INDEX idx_product_status ON PRODUCT(Status);

-- =========================
-- USER & PERMISSION (FINAL - CORRECT)
-- =========================

CREATE USER IF NOT EXISTS 'a@gmail.com'@'localhost' IDENTIFIED BY '123';
CREATE USER IF NOT EXISTS 'b@gmail.com'@'localhost' IDENTIFIED BY '123';

-- ===== MANAGER: FULL CONTROL =====
GRANT ALL PRIVILEGES ON bakery_db.* TO 'a@gmail.com'@'localhost';

-- ===== EMPLOYEE (STAFF) =====

-- Xem thông tin cơ bản
GRANT SELECT ON bakery_db.EMPLOYEE TO 'b@gmail.com'@'localhost';

-- Attendance / Check-in
GRANT SELECT ON bakery_db.EMPLOYEE_CHECKIN TO 'b@gmail.com'@'localhost';
GRANT SELECT ON bakery_db.EMPLOYEE_ATTENDANCE_LOG TO 'b@gmail.com'@'localhost';

-- Product & Promotion
GRANT SELECT ON bakery_db.PRODUCT TO 'b@gmail.com'@'localhost';
GRANT SELECT ON bakery_db.PRODUCT_CATEGORY TO 'b@gmail.com'@'localhost';
GRANT SELECT ON bakery_db.PROMOTION TO 'b@gmail.com'@'localhost';
GRANT SELECT ON bakery_db.PROMOTION_PRODUCT TO 'b@gmail.com'@'localhost';

-- Lương: CHỈ QUA VIEW
GRANT SELECT ON bakery_db.vw_EmployeeSalary TO 'b@gmail.com'@'localhost';

-- Chỉ được check-in / check-out qua PROCEDURE
GRANT EXECUTE ON PROCEDURE bakery_db.sp_employee_checkin_by_email TO 'b@gmail.com'@'localhost';
GRANT EXECUTE ON PROCEDURE bakery_db.sp_employee_checkout_by_email TO 'b@gmail.com'@'localhost';

--Customer – CRUD
GRANT SELECT, INSERT, UPDATE, DELETE
ON bakery_db.customer
TO 'b@gmail.com'@'localhost';

--Orders – CRUD
GRANT SELECT, INSERT, UPDATE, DELETE
ON bakery_db.orders
TO 'b@gmail.com'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE
ON bakery_db.orderdetail
TO 'b@gmail.com'@'localhost';


FLUSH PRIVILEGES;

-- TEST
SHOW GRANTS FOR 'a@gmail.com'@'localhost';
SHOW GRANTS FOR 'b@gmail.com'@'localhost';