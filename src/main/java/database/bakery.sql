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
    Gender ENUM('male','female'),
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

--=========================
--TABLE: EMPLOYEE_ATTENDANCE_LOG
--=========================
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
('E01','Nguyen Hong Ngoc','1998-01-10','female','0901','a@gmail.com','HN','2001-01-01','Manager','Active'),
('E02','Tran Van Huy','1997-02-11','male','0902','b@gmail.com','HCM','2020-05-12','Staff','Active'),
('E03','Pham Quoc Anh','2001-03-12','male','0903','c@gmail.com','DN','2019-06-15','Staff','Active'),
('E04','Pham Thi D','1999-04-13','female','0904','d@gmail.com','HN','2022-02-10','Staff','Active'),
('E05','Hoang Van E','1995-05-14','male','0905','e@gmail.com','HCM','2018-03-20','Staff','Inactive'),
('E06','Nguyen Tu Quyen','2004-05-12','female','0906','tuquyen@gmail.com','HN','2021-07-01','Staff','Active'),
('E07','Vu Van G','1997-07-16','male','0907','g@gmail.com','HN','2020-08-08','Staff','Active'),
('E08','Dang Thi H','1996-08-17','female','0908','h@gmail.com','HCM','2019-09-09','Staff','Active'),
('E09','Bui Van I','1995-09-18','male','0909','i@gmail.com','DN','2018-10-10','Staff','Active'),
('E10','Nguyen Thi J','1999-10-19','female','0910','j@gmail.com','HN','2022-11-11','Staff','Active');

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

        -- Check-out ngẫu nhiên từ 16:30 → 18:00
        UPDATE EMPLOYEE_CHECKIN
        SET CheckOutTime =
            SEC_TO_TIME(
                TIME_TO_SEC('16:30:00')
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


-- =========================
-- USER & PERMISSION
-- =========================

-- ⚠️ Tạo user (chỉ chạy 1 lần)
CREATE USER IF NOT EXISTS 'manager_user'@'localhost' IDENTIFIED BY '123';
CREATE USER IF NOT EXISTS 'employee_user'@'localhost' IDENTIFIED BY '123';

-- ===== MANAGER: FULL =====
GRANT ALL PRIVILEGES ON bakery_db.* TO 'manager_user'@'localhost';

-- ===== EMPLOYEE: CHỈ XEM + CHECK-IN / CHECK-OUT QUA PROCEDURE =====
GRANT SELECT ON bakery_db.EMPLOYEE TO 'employee_user'@'localhost';
GRANT SELECT ON bakery_db.EMPLOYEE_CHECKIN TO 'employee_user'@'localhost';
GRANT SELECT ON bakery_db.EMPLOYEE_ATTENDANCE_LOG TO 'employee_user'@'localhost';

-- Cho phép gọi procedure check-in / check-out
GRANT EXECUTE ON PROCEDURE bakery_db.sp_employee_checkin_by_email TO 'employee_user'@'localhost';
GRANT EXECUTE ON PROCEDURE bakery_db.sp_employee_checkout_by_email TO 'employee_user'@'localhost';

FLUSH PRIVILEGES;

-- Test quyền
SHOW GRANTS FOR 'manager_user'@'localhost';
SHOW GRANTS FOR 'employee_user'@'localhost';








