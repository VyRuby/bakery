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



    DECLARE cur CURSOR FOR
        SELECT EmployeeID FROM EMPLOYEE WHERE Status='Active';
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done=1;
    SET @ALLOW_PAST = 1;

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


-- CHỈNH SỬA BẢNG PRODUCT, THÊM ACTIVE, DEACTIVE
ALTER TABLE PRODUCT
ADD COLUMN Status ENUM('Active','Inactive') NOT NULL DEFAULT 'Active';

CREATE INDEX idx_product_status ON PRODUCT(Status);

-- =========================
-- USER & PERMISSION 
-- =========================

CREATE USER IF NOT EXISTS 'a@gmail.com'@'localhost' IDENTIFIED BY '123';
CREATE USER IF NOT EXISTS 'b@gmail.com'@'localhost' IDENTIFIED BY '123';

-- =========================
-- MANAGER: FULL CONTROL
-- =========================
GRANT ALL PRIVILEGES ON bakery_db.* TO 'a@gmail.com'@'localhost';

-- =========================
-- STAFF (EMPLOYEE)
-- =========================

-- ===== EMPLOYEE INFO (READ ONLY)
GRANT SELECT ON bakery_db.EMPLOYEE TO 'b@gmail.com'@'localhost';

-- ===== CHECK-IN / ATTENDANCE (READ LOG ONLY)
GRANT SELECT ON bakery_db.EMPLOYEE_CHECKIN TO 'b@gmail.com'@'localhost';
GRANT SELECT ON bakery_db.EMPLOYEE_ATTENDANCE_LOG TO 'b@gmail.com'@'localhost';

-- ===== PAYROLL (VIEW ONLY – KHÔNG ĐỤNG TABLE)
GRANT SELECT ON bakery_db.vw_payroll TO 'b@gmail.com'@'localhost';

-- ===== CHECK-IN / CHECK-OUT (ONLY VIA PROCEDURE)
GRANT EXECUTE ON PROCEDURE bakery_db.sp_CheckInByEmail TO 'b@gmail.com'@'localhost';
GRANT EXECUTE ON PROCEDURE bakery_db.sp_CheckOutByEmail TO 'b@gmail.com'@'localhost';

-- ===== PRODUCT / PROMOTION (SELL VIEW)
GRANT SELECT ON bakery_db.PRODUCT TO 'b@gmail.com'@'localhost';
GRANT SELECT ON bakery_db.PRODUCT_CATEGORY TO 'b@gmail.com'@'localhost';
GRANT SELECT ON bakery_db.PROMOTION TO 'b@gmail.com'@'localhost';
GRANT SELECT ON bakery_db.PROMOTION_PRODUCT TO 'b@gmail.com'@'localhost';

-- ===== CUSTOMER / ORDER (STAFF ĐƯỢC CRUD)
GRANT SELECT, INSERT, UPDATE, DELETE
ON bakery_db.customer
TO 'b@gmail.com'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE
ON bakery_db.orders
TO 'b@gmail.com'@'localhost';

GRANT SELECT, INSERT, UPDATE, DELETE
ON bakery_db.orderdetail
TO 'b@gmail.com'@'localhost';

FLUSH PRIVILEGES;

-- =========================
-- TEST
-- =========================
SHOW GRANTS FOR 'a@gmail.com'@'localhost';
SHOW GRANTS FOR 'b@gmail.com'@'localhost';



CREATE TABLE `customer` (
  `CustomerID` int(10) NOT NULL,
  `FullName` varchar(100) NOT NULL,
  `Phone` varchar(20) NOT NULL,
  `Gender` enum('Male','Female') NOT NULL,
  `DOB` date NOT NULL,
  `Email` varchar(100) DEFAULT NULL,
  `Address` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `customer`
--

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

-- --------------------------------------------------------

--
-- Table structure for table `orderdetail`
--

CREATE TABLE `orderdetail` (
  `OrderDetailID` int(11) NOT NULL,
  `OrderID` int(11) NOT NULL,
  `ProductID` varchar(30) NOT NULL,
  `Quantity` int(11) NOT NULL,
  `UnitPrice` decimal(12,2) NOT NULL,
  `CostPrice` decimal(12,2) NOT NULL,
  `PromoID` varchar(30) DEFAULT NULL,
  `DiscountAmount` decimal(12,2) DEFAULT 0.00
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `orderdetail`
--

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

-- --------------------------------------------------------

--
-- Table structure for table `orders`
--

CREATE TABLE `orders` (
  `OrderID` int(11) NOT NULL,
  `OrderDate` datetime NOT NULL DEFAULT current_timestamp(),
  `CustomerID` int(11) NOT NULL,
  `Total` decimal(12,2) NOT NULL,
  `PaymentMethod` enum('Transfer','Cash') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `orders`
--

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

--
-- Indexes for dumped tables
--

--
-- Indexes for table `customer`
--
ALTER TABLE `customer`
  ADD PRIMARY KEY (`CustomerID`),
  ADD UNIQUE KEY `uc_phone` (`Phone`);

--
-- Indexes for table `orderdetail`
--
ALTER TABLE `orderdetail`
  ADD PRIMARY KEY (`OrderDetailID`),
  ADD KEY `fk_order_ref` (`OrderID`);

--
-- Indexes for table `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`OrderID`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `customer`
--
ALTER TABLE `customer`
  MODIFY `CustomerID` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=14;

--
-- AUTO_INCREMENT for table `orderdetail`
--
ALTER TABLE `orderdetail`
  MODIFY `OrderDetailID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=130;

--
-- AUTO_INCREMENT for table `orders`
--
ALTER TABLE `orders`
  MODIFY `OrderID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=145;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `orderdetail`
--
ALTER TABLE `orderdetail`
  ADD CONSTRAINT `fk_order_ref` FOREIGN KEY (`OrderID`) REFERENCES `orders` (`OrderID`) ON DELETE CASCADE;
COMMIT;
