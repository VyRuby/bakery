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

-- Huy id them AUTO_INCREMENT
CREATE TABLE `orders` (
  `OrderID` int(11) NOT NULL AUTO_INCREMENT,
  `OrderDate` datetime NOT NULL DEFAULT current_timestamp(),
  `CustomerID` int(11) NOT NULL,
  `Total` decimal(12,2) NOT NULL,
  `PaymentMethod` enum('Transfer','Cash') NOT NULL,
  PRIMARY KEY (`OrderID`)
) ENGINE=InnoDB;


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
(3, 'Visitor', '000000', 'Male', '0000-00-00', NULL, NULL);

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