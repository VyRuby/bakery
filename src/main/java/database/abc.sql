-- =====================================
-- RESET DATABASE
-- =====================================
DROP DATABASE IF EXISTS bakery_db;
CREATE DATABASE bakery_db;
USE bakery_db;

-- =====================================
-- EMPLOYEE
-- =====================================
CREATE TABLE EMPLOYEE (
    EmployeeID VARCHAR(10) PRIMARY KEY,
    FullName VARCHAR(100),
    DOB DATE,
    Gender ENUM('Male','Female'),
    Phone VARCHAR(20),
    Email VARCHAR(100) UNIQUE,
    Address VARCHAR(100),
    HireDate DATE,
    Position ENUM('Manager','Staff'),
    BaseDailySalary INT,
    Status ENUM('Active','Inactive')
);

-- =====================================
-- INSERT EMPLOYEE
-- =====================================
INSERT INTO EMPLOYEE VALUES
('E01','Nguyen Hong Ngoc','1998-01-10','Female','0901','a@gmail.com','HN','2001-01-01','Manager',500000,'Active'),
('E02','Tran Van Huy','1997-02-11','Male','0902','b@gmail.com','HCM','2020-05-12','Staff',300000,'Active'),
('E03','Pham Quoc Anh','2001-03-12','Male','0903','c@gmail.com','DN','2019-06-15','Staff',300000,'Active'),
('E04','Pham Thi D','1999-04-13','Female','0904','d@gmail.com','HN','2022-02-10','Staff',300000,'Active'),
('E05','Hoang Van E','1995-05-14','Male','0905','e@gmail.com','HCM','2018-03-20','Staff',300000,'Inactive'),
('E06','Nguyen Tu Quyen','2004-05-12','Female','0906','tuquyen@gmail.com','HN','2021-07-01','Staff',300000,'Active'),
('E07','Vu Van G','1997-07-16','Male','0907','g@gmail.com','HN','2020-08-08','Staff',300000,'Active'),
('E08','Dang Thi H','1996-08-17','Female','0908','h@gmail.com','HCM','2019-09-09','Staff',300000,'Active'),
('E09','Bui Van I','1995-09-18','Male','0909','i@gmail.com','DN','2018-10-10','Staff',300000,'Active'),
('E10','Nguyen Thi J','1999-10-19','Female','0910','j@gmail.com','HN','2022-11-11','Staff',300000,'Active');

-- =====================================
-- CHECKIN / CHECKOUT
-- =====================================
CREATE TABLE EMPLOYEE_CHECKIN (
    CheckInID INT AUTO_INCREMENT PRIMARY KEY,
    EmployeeID VARCHAR(10),
    WorkDate DATE,
    CheckInTime TIME,
    CheckOutTime TIME,
    IsLate BOOLEAN,
    IsEarlyLeave BOOLEAN,
    UNIQUE(EmployeeID, WorkDate),
    FOREIGN KEY (EmployeeID) REFERENCES EMPLOYEE(EmployeeID)
);

-- =====================================
-- PAYROLL
-- =====================================
CREATE TABLE EMPLOYEE_PAYROLL (
    EmployeeID VARCHAR(10),
    Month INT,
    Year INT,
    WorkDays INT DEFAULT 0,
    LateEarlyDays INT DEFAULT 0,
    Bonus INT DEFAULT 0,
    Penalty INT DEFAULT 0,
    TotalSalary INT DEFAULT 0,
    Locked BOOLEAN DEFAULT 0,
    PRIMARY KEY (EmployeeID, Month, Year)
);

-- =====================================
-- ATTENDANCE LOG
-- =====================================
CREATE TABLE EMPLOYEE_ATTENDANCE_LOG (
    LogID INT AUTO_INCREMENT PRIMARY KEY,
    EmployeeID VARCHAR(10),
    WorkDate DATE,
    Action ENUM('CHECKIN','CHECKOUT'),
    ActionTime DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- =====================================
-- TRIGGER: CHECKIN VALIDATION (REALTIME + BATCH)
-- =====================================
DELIMITER $$

CREATE TRIGGER trg_checkin_before_ins
BEFORE INSERT ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    IF IFNULL(@ALLOW_PAST,0)=0 AND NEW.WorkDate <> CURDATE() THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Check-in only allowed for today';
    END IF;

    SET NEW.IsLate = (NEW.CheckInTime > '08:00:00');
    SET NEW.IsEarlyLeave = 0;
END$$

CREATE TRIGGER trg_checkin_before_upd
BEFORE UPDATE ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    IF IFNULL(@ALLOW_PAST,0)=0 AND OLD.WorkDate <> CURDATE() THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Cannot modify past attendance records';
    END IF;

    SET NEW.IsLate = (NEW.CheckInTime > '08:00:00');
    SET NEW.IsEarlyLeave =
        (NEW.CheckOutTime IS NOT NULL AND NEW.CheckOutTime < '17:00:00');
END$$

DELIMITER ;

-- =====================================
-- LOG CHECKIN / CHECKOUT
-- =====================================
DELIMITER $$

CREATE TRIGGER trg_log_checkin
AFTER INSERT ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    INSERT INTO EMPLOYEE_ATTENDANCE_LOG(EmployeeID,WorkDate,Action)
    VALUES (NEW.EmployeeID,NEW.WorkDate,'CHECKIN');
END$$

CREATE TRIGGER trg_log_checkout
AFTER UPDATE ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    IF NEW.CheckOutTime IS NOT NULL AND OLD.CheckOutTime IS NULL THEN
        INSERT INTO EMPLOYEE_ATTENDANCE_LOG(EmployeeID,WorkDate,Action)
        VALUES (NEW.EmployeeID,NEW.WorkDate,'CHECKOUT');
    END IF;
END$$

DELIMITER ;

-- =====================================
-- CORE: RECALC PAYROLL
-- =====================================
DELIMITER $$

CREATE PROCEDURE sp_RecalcPayroll(pEmp VARCHAR(10), pDate DATE)
BEGIN
    DECLARE m INT;
    DECLARE y INT;

    SET m = MONTH(pDate);
    SET y = YEAR(pDate);

    IF (SELECT Status FROM EMPLOYEE WHERE EmployeeID=pEmp)='Inactive' THEN
        DELETE FROM EMPLOYEE_PAYROLL
        WHERE EmployeeID=pEmp AND Month=m AND Year=y;
    ELSE
        INSERT INTO EMPLOYEE_PAYROLL(EmployeeID,Month,Year)
        VALUES(pEmp,m,y)
        ON DUPLICATE KEY UPDATE EmployeeID=EmployeeID;

        UPDATE EMPLOYEE_PAYROLL p
        JOIN (
            SELECT
                COUNT(*) wd,
                SUM(
                    CASE
                        WHEN IsLate=1 OR IsEarlyLeave=1 THEN 1
                        ELSE 0
                    END
                ) le
            FROM EMPLOYEE_CHECKIN
            WHERE EmployeeID=pEmp
              AND MONTH(WorkDate)=m
              AND YEAR(WorkDate)=y
        ) x
        JOIN EMPLOYEE e ON e.EmployeeID=pEmp
        SET
            p.WorkDays = x.wd,
            p.LateEarlyDays = x.le,
            p.Bonus = IF(x.wd>=25,300000,0),
            p.Penalty = x.le*200000,
            p.TotalSalary =
                x.wd*e.BaseDailySalary
                - x.le*200000
                + IF(x.wd>=25,300000,0)
        WHERE p.EmployeeID=pEmp
          AND p.Month=m
          AND p.Year=y
          AND p.Locked=0;
    END IF;
END$$

DELIMITER ;

-- =====================================
-- RECALC PAYROLL FOR WHOLE MONTH
-- =====================================
DELIMITER $$

CREATE PROCEDURE sp_RecalcPayroll_ByMonth(pMonth INT, pYear INT)
BEGIN
    DECLARE emp VARCHAR(10);
    DECLARE done INT DEFAULT 0;

    DECLARE cur CURSOR FOR
        SELECT EmployeeID
        FROM EMPLOYEE
        WHERE Status='Active';

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=1;

    OPEN cur;

    emp_loop: LOOP
        FETCH cur INTO emp;
        IF done=1 THEN
            LEAVE emp_loop;
        END IF;

        -- gọi lại core payroll cho từng nhân viên
        CALL sp_RecalcPayroll(
            emp,
            STR_TO_DATE(CONCAT(pYear,'-',pMonth,'-01'), '%Y-%m-%d')
        );

    END LOOP;

    CLOSE cur;
END$$

DELIMITER ;


-- =====================================
-- REALTIME UPDATE PAYROLL
-- =====================================
DELIMITER $$

CREATE TRIGGER trg_checkin_after_ins
AFTER INSERT ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    CALL sp_RecalcPayroll(NEW.EmployeeID, NEW.WorkDate);
END$$

CREATE TRIGGER trg_checkin_after_upd
AFTER UPDATE ON EMPLOYEE_CHECKIN
FOR EACH ROW
BEGIN
    IF NEW.CheckOutTime IS NOT NULL AND OLD.CheckOutTime IS NULL THEN
        CALL sp_RecalcPayroll(NEW.EmployeeID, NEW.WorkDate);
    END IF;
END$$

DELIMITER ;

-- =====================================
-- CHECKIN / CHECKOUT BY EMAIL
-- =====================================
DELIMITER $$

CREATE PROCEDURE sp_CheckInByEmail(pEmail VARCHAR(100))
BEGIN
    DECLARE vEmp VARCHAR(10);

    SELECT EmployeeID INTO vEmp
    FROM EMPLOYEE
    WHERE Email=pEmail AND Status='Active'
    LIMIT 1;

    IF vEmp IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT='Invalid email or inactive employee';
    END IF;

    IF EXISTS (
        SELECT 1 FROM EMPLOYEE_CHECKIN
        WHERE EmployeeID=vEmp AND WorkDate=CURDATE()
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT='Already checked in today';
    END IF;

    INSERT INTO EMPLOYEE_CHECKIN
    VALUES (NULL,vEmp,CURDATE(),CURTIME(),NULL,0,0);
END$$

CREATE PROCEDURE sp_CheckOutByEmail(pEmail VARCHAR(100))
BEGIN
    DECLARE vEmp VARCHAR(10);

    SELECT EmployeeID INTO vEmp
    FROM EMPLOYEE
    WHERE Email=pEmail AND Status='Active'
    LIMIT 1;

    IF vEmp IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT='Invalid email or inactive employee';
    END IF;

    UPDATE EMPLOYEE_CHECKIN
    SET CheckOutTime=CURTIME()
    WHERE EmployeeID=vEmp AND WorkDate=CURDATE();
END$$

DELIMITER ;

-- =====================================
-- GENERATE HISTORICAL CHECKIN (06–12/2025)
-- =====================================
DELIMITER $$

CREATE PROCEDURE sp_GenCheckin()
BEGIN
    DECLARE emp VARCHAR(10);
    DECLARE d DATE;
    DECLARE done INT DEFAULT 0;

    SET @ALLOW_PAST = 1;

    DECLARE cur CURSOR FOR
        SELECT EmployeeID FROM EMPLOYEE WHERE Status='Active';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=1;

    OPEN cur;
    emp_loop: LOOP
        FETCH cur INTO emp;
        IF done=1 THEN LEAVE emp_loop; END IF;

        SET d='2025-06-01';
        WHILE d<='2025-12-31' DO
            IF DAYOFWEEK(d) NOT IN (1,7) AND RAND()>0.15 THEN
                INSERT IGNORE INTO EMPLOYEE_CHECKIN
                VALUES (
                    NULL, emp, d,
                    ADDTIME('07:30:00',SEC_TO_TIME(RAND()*5400)),
                    ADDTIME('16:30:00',SEC_TO_TIME(RAND()*5400)),
                    0,0
                );
            END IF;
            SET d=DATE_ADD(d,INTERVAL 1 DAY);
        END WHILE;
    END LOOP;
    CLOSE cur;

    SET @ALLOW_PAST = 0;
END$$

DELIMITER ;

CALL sp_GenCheckin();

-- =====================================
-- RECALC PAYROLL FOR GENERATED DATA
-- =====================================
CALL sp_RecalcPayroll_ByMonth(6,2025);
CALL sp_RecalcPayroll_ByMonth(7,2025);
CALL sp_RecalcPayroll_ByMonth(8,2025);
CALL sp_RecalcPayroll_ByMonth(9,2025);
CALL sp_RecalcPayroll_ByMonth(10,2025);
CALL sp_RecalcPayroll_ByMonth(11,2025);
CALL sp_RecalcPayroll_ByMonth(12,2025);


-- =====================================
-- VIEW PAYROLL
-- =====================================
CREATE OR REPLACE VIEW vw_payroll AS
SELECT
    e.EmployeeID,
    e.FullName,
    p.Month,
    p.Year,
    p.WorkDays,
    p.LateEarlyDays,
    p.Bonus,
    p.Penalty,
    p.TotalSalary
FROM EMPLOYEE_PAYROLL p
JOIN EMPLOYEE e ON e.EmployeeID=p.EmployeeID
ORDER BY p.Year,p.Month,e.EmployeeID;
