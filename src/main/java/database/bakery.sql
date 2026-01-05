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
CREATE TABLE EMPLOYEE_PAYROLL (
    PayrollID VARCHAR(30) PRIMARY KEY,
    EmployeeID VARCHAR(30),
    LateWorkday INT DEFAULT 0,
    Month TINYINT DEFAULT (MONTH(CURDATE())),
    Year SMALLINT DEFAULT (YEAR(CURDATE())),
    BaseSalary DECIMAL(12,2),
    WorkDays INT DEFAULT 0,
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
-- INSERT EMPLOYEE DATA
-- =========================
INSERT INTO EMPLOYEE VALUES
('E01','Nguyen Hong Ngoc','1998-01-10','female','0901','a@gmail.com','HN','2001-01-01','Manager','Active'),
('E02','Tran Van Huy','1997-02-11','male','0902','b@gmail.com','HCM','2020-05-12','Staff','Active'),
('E03','Pham Quoc Anh','2001-03-12','male','0903','c@gmail.com','DN','2019-06-15','Staff','Active'),
('E04','Pham Thi D','1999-04-13','female','0904','d@gmail.com','HN','2022-02-10','Staff','Active'),
('E05','Hoang Van E','1995-05-14','male','0905','e@gmail.com','HCM','2018-03-20','Staff','Inactive'),
('E06','Do Thi F','1998-06-15','female','0906','f@gmail.com','DN','2021-07-01','Staff','Active'),
('E07','Vu Van G','1997-07-16','male','0907','g@gmail.com','HN','2020-08-08','Staff','Active'),
('E08','Dang Thi H','1996-08-17','female','0908','h@gmail.com','HCM','2019-09-09','Staff','Active'),
('E09','Bui Van I','1995-09-18','male','0909','i@gmail.com','DN','2018-10-10','Staff','Active'),
('E10','Nguyen Thi J','1999-10-19','female','0910','j@gmail.com','HN','2022-11-11','Staff','Active');

-- =========================
-- INSERT PAYROLL (REAL TIME)
-- =========================
INSERT INTO EMPLOYEE_PAYROLL
(PayrollID, EmployeeID, BaseSalary)
VALUES
('P01','E01',8000000),
('P02','E02',8500000),
('P03','E03',7800000),
('P04','E04',9000000),
('P05','E06',8200000),
('P06','E07',8300000),
('P07','E08',8400000),
('P08','E09',8600000),
('P09','E10',8800000);

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
-- TRIGGER: UPDATE WORKDAY & LATE
-- =========================
DELIMITER $$

CREATE TRIGGER trg_checkin_update_payroll
AFTER INSERT ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    DECLARE v_month INT;

    SET v_month = MONTH(NEW.WorkDate);

    UPDATE EMPLOYEE_PAYROLL
    SET WorkDays = IFNULL(WorkDays, 0) + 1
    WHERE EmployeeID = NEW.EmployeeID
      AND Month = v_month;

    IF NEW.IsLate = 1 THEN
        UPDATE EMPLOYEE_PAYROLL
        SET LateWorkday = IFNULL(LateWorkday, 0) + 1
        WHERE EmployeeID = NEW.EmployeeID
          AND Month = v_month;
    END IF;
END$$

DELIMITER ;

-- =========================
-- BONUS & PENALTY LOGIC
-- =========================
INSERT INTO EMPLOYEE_BONUS
SELECT
    CONCAT('B', PayrollID),
    PayrollID,
    'Thưởng chuyên cần',
    500000,
    CURDATE()
FROM EMPLOYEE_PAYROLL
WHERE WorkDays >= 24;

INSERT INTO EMPLOYEE_PENALTY
SELECT
    CONCAT('PE', PayrollID),
    PayrollID,
    'Phạt nghỉ nhiều',
    300000,
    CURDATE()
FROM EMPLOYEE_PAYROLL
WHERE WorkDays < 15;

-- =========================
-- STORED PROCEDURE: CALC SALARY
-- =========================
DELIMITER $$

CREATE PROCEDURE sp_CalcSalary()
BEGIN
    UPDATE EMPLOYEE_PAYROLL p
    SET
        Bonus = (
            SELECT IFNULL(SUM(b.Amount),0)
            FROM EMPLOYEE_BONUS b
            WHERE b.PayrollID = p.PayrollID
        ),
        Penalty = (
            SELECT IFNULL(SUM(pe.Amount),0)
            FROM EMPLOYEE_PENALTY pe
            WHERE pe.PayrollID = p.PayrollID
        ),
        TotalSalary = BaseSalary
            + IFNULL((
                SELECT SUM(b.Amount)
                FROM EMPLOYEE_BONUS b
                WHERE b.PayrollID = p.PayrollID
            ),0)
            - IFNULL((
                SELECT SUM(pe.Amount)
                FROM EMPLOYEE_PENALTY pe
                WHERE pe.PayrollID = p.PayrollID
            ),0);
END$$

DELIMITER ;

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
WHERE
    p.Month = MONTH(CURDATE())
    AND p.Year  = YEAR(CURDATE());

-- -- =========================
-- -- PERMISSION
-- -- =========================
-- GRANT ALL PRIVILEGES ON bakery_db.EMPLOYEE TO 'manager_user'@'localhost';
-- GRANT ALL PRIVILEGES ON bakery_db.EMPLOYEE_PAYROLL TO 'manager_user'@'localhost';
-- GRANT ALL PRIVILEGES ON bakery_db.EMPLOYEE_BONUS TO 'manager_user'@'localhost';
-- GRANT ALL PRIVILEGES ON bakery_db.EMPLOYEE_PENALTY TO 'manager_user'@'localhost';
-- GRANT ALL PRIVILEGES ON bakery_db.EMPLOYEE_CHECKIN TO 'manager_user'@'localhost';
-- 
-- GRANT SELECT ON bakery_db.EMPLOYEE TO 'employee_user'@'localhost';
-- GRANT SELECT ON bakery_db.EMPLOYEE_PAYROLL TO 'employee_user'@'localhost';
-- GRANT SELECT ON bakery_db.EMPLOYEE_BONUS TO 'employee_user'@'localhost';
-- GRANT SELECT ON bakery_db.EMPLOYEE_PENALTY TO 'employee_user'@'localhost';
-- GRANT SELECT ON bakery_db.EMPLOYEE_CHECKIN TO 'employee_user'@'localhost';
-- GRANT INSERT ON bakery_db.EMPLOYEE_CHECKIN TO 'employee_user'@'localhost'; --CHỈ CHECK-IN CHO CHÍNH MÌNH
-- 
-- FLUSH PRIVILEGES;
-- SHOW GRANTS FOR 'manager_user'@'localhost';
-- SHOW GRANTS FOR 'employee_user'@'localhost';

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



