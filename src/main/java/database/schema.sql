CREATE DATABASE payroll_db;
USE payroll_db;


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
    Month TINYINT,
    BaseSalary DECIMAL(12,2),
    WorkDays INT,
    Bonus DECIMAL(12,2) DEFAULT 0,
    Penalty DECIMAL(12,2) DEFAULT 0,
    TotalSalary DECIMAL(12,2),
    CONSTRAINT fk_payroll_employee
        FOREIGN KEY (EmployeeID)
        REFERENCES EMPLOYEE(EmployeeID),
    CONSTRAINT uq_employee_month UNIQUE (EmployeeID, Month)
);

-- =========================
-- TABLE: EMPLOYEE_BONUS
-- =========================
CREATE TABLE EMPLOYEE_BONUS (
    BonusID VARCHAR(30) PRIMARY KEY,
    PayrollID VARCHAR(30),
    Description VARCHAR(255),
    Amount DECIMAL(12,2),
    BonusDate DATE,
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
    PenaltyDate DATE,
    CONSTRAINT fk_penalty_payroll
        FOREIGN KEY (PayrollID)
        REFERENCES EMPLOYEE_PAYROLL(PayrollID)
);

-- =========================
-- INSERT TEST DATA (10 NHÂN VIÊN)
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
--INSERT PAYROLL (THÁNG 6)
-- =========================

INSERT INTO EMPLOYEE_PAYROLL
(PayrollID, EmployeeID, LateWorkday, Month, BaseSalary, WorkDays)
VALUES
('P01','E01',1,6,8000000,26),
('P02','E02',0,6,8500000,24),
('P03','E03',2,6,7800000,14),
('P04','E04',0,6,9000000,27),
('P05','E06',1,6,8200000,15),
('P06','E07',0,6,8300000,28),
('P07','E08',1,6,8400000,13),
('P08','E09',0,6,8600000,26),
('P09','E10',0,6,8800000,29);


-- =========================
--LOGIC BONUS & PENALTY
-- > 24 ngày → BONUS
-- < 15 ngày → PENALTY
-- =========================

-- BONUS: nếu WorkDays > 24 → +500k
INSERT INTO EMPLOYEE_BONUS
SELECT 
    CONCAT('B', PayrollID),
    PayrollID,
    'Thưởng chuyên cần',
    500000,
    CURDATE()
FROM EMPLOYEE_PAYROLL
WHERE WorkDays >= 24;

-- PENALTY: nếu WorkDays < 15 → -300k
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
--STORED PROCEDURE – TÍNH LƯƠNG (TOÀN BỘ)
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
            + (
                SELECT IFNULL(SUM(b.Amount),0)
                FROM EMPLOYEE_BONUS b
                WHERE b.PayrollID = p.PayrollID
              )
            - (
                SELECT IFNULL(SUM(pe.Amount),0)
                FROM EMPLOYEE_PENALTY pe
                WHERE pe.PayrollID = p.PayrollID
              );
END$$

DELIMITER ;
-- GỌI 1 LỆNH DUY NHẤT: "CALL sp_CalcSalary();"

-- =========================
--TRIGGER – AUTO UPDATE TOTAL SALARY
-- =========================

DELIMITER $$

CREATE TRIGGER trg_update_total_salary
BEFORE UPDATE ON EMPLOYEE_PAYROLL
FOR EACH ROW
BEGIN
    SET NEW.TotalSalary =
        NEW.BaseSalary + NEW.Bonus - NEW.Penalty;
END$$

DELIMITER ;




-- =========================
--VIEW – XEM BẢNG LƯƠNG (EMPLOYEE CHỈ XEM)
-- =========================
CREATE VIEW vw_EmployeeSalary AS
SELECT
    e.EmployeeID,
    e.FullName,
    p.Month,
    p.WorkDays,
    p.BaseSalary,
    p.Bonus,
    p.Penalty,
    p.TotalSalary
FROM EMPLOYEE e
JOIN EMPLOYEE_PAYROLL p
ON e.EmployeeID = p.EmployeeID;


-- =========================
--PHÂN QUYỀN (EMPLOYEE vs MANAGER)
-- =========================

-- EMPLOYEE: chỉ xem
GRANT SELECT ON vw_EmployeeSalary TO 'employee_user'@'localhost';

-- MANAGER: toàn quyền
GRANT ALL PRIVILEGES ON EMPLOYEE TO 'manager_user'@'localhost';
GRANT ALL PRIVILEGES ON EMPLOYEE_PAYROLL TO 'manager_user'@'localhost';
GRANT ALL PRIVILEGES ON EMPLOYEE_BONUS TO 'manager_user'@'localhost';
GRANT ALL PRIVILEGES ON EMPLOYEE_PENALTY TO 'manager_user'@'localhost';

