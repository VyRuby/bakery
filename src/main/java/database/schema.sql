-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: Jan 11, 2026 at 08:37 AM
-- Server version: 10.4.32-MariaDB
-- PHP Version: 8.2.12

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `bakery_db`
--

DELIMITER $$
--
-- Procedures
--
CREATE DEFINER=`root`@`localhost` PROCEDURE `sp_CalcSalary` ()   BEGIN
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

-- --------------------------------------------------------

--
-- Table structure for table `customer`
--

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
(3, 'Visitor', '000000', 'Male', '0000-00-00', NULL, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `employee`
--

CREATE TABLE `employee` (
  `EmployeeID` varchar(30) NOT NULL,
  `FullName` varchar(100) DEFAULT NULL,
  `DOB` date DEFAULT NULL,
  `Gender` enum('male','female') DEFAULT NULL,
  `Phone` varchar(30) DEFAULT NULL,
  `Email` varchar(100) DEFAULT NULL,
  `Address` varchar(255) DEFAULT NULL,
  `HireDate` date DEFAULT NULL,
  `Position` varchar(30) DEFAULT NULL,
  `Status` enum('Active','Inactive') DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `employee`
--

INSERT INTO `employee` (`EmployeeID`, `FullName`, `DOB`, `Gender`, `Phone`, `Email`, `Address`, `HireDate`, `Position`, `Status`) VALUES
('E01', 'Nguyen Hong Ngoc', '1998-01-10', 'female', '0901', 'a@gmail.com', 'HN', '2001-01-01', 'Manager', 'Active'),
('E02', 'Tran Van Huy', '1997-02-11', 'male', '0902', 'b@gmail.com', 'HCM', '2020-05-12', 'Staff', 'Active'),
('E03', 'Pham Quoc Anh', '2001-03-12', 'male', '0903', 'c@gmail.com', 'DN', '2019-06-15', 'Staff', 'Active'),
('E04', 'Pham Thi D', '1999-04-13', 'female', '0904', 'd@gmail.com', 'HN', '2022-02-10', 'Staff', 'Active'),
('E05', 'Hoang Van E', '1995-05-14', 'male', '0905', 'e@gmail.com', 'HCM', '2018-03-20', 'Staff', 'Inactive'),
('E06', 'Do Thi F', '1998-06-15', 'female', '0906', 'f@gmail.com', 'DN', '2021-07-01', 'Staff', 'Active'),
('E07', 'Vu Van G', '1997-07-16', 'male', '0907', 'g@gmail.com', 'HN', '2020-08-08', 'Staff', 'Active'),
('E08', 'Dang Thi H', '1996-08-17', 'female', '0908', 'h@gmail.com', 'HCM', '2019-09-09', 'Staff', 'Active'),
('E09', 'Bui Van I', '1995-09-18', 'male', '0909', 'i@gmail.com', 'DN', '2018-10-10', 'Staff', 'Active'),
('E10', 'Nguyen Thi J', '1999-10-19', 'female', '0910', 'j@gmail.com', 'HN', '2022-11-11', 'Staff', 'Active');

-- --------------------------------------------------------

--
-- Table structure for table `employee_bonus`
--

CREATE TABLE `employee_bonus` (
  `BonusID` varchar(30) NOT NULL,
  `PayrollID` varchar(30) DEFAULT NULL,
  `Description` varchar(255) DEFAULT NULL,
  `Amount` decimal(12,2) DEFAULT NULL,
  `BonusDate` date DEFAULT curdate()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `employee_checkin`
--

CREATE TABLE `employee_checkin` (
  `CheckInID` int(11) NOT NULL,
  `EmployeeID` varchar(30) DEFAULT NULL,
  `WorkDate` date DEFAULT curdate(),
  `CheckInTime` time DEFAULT curtime(),
  `CheckOutTime` time DEFAULT NULL,
  `IsLate` tinyint(1) DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Triggers `employee_checkin`
--
DELIMITER $$
CREATE TRIGGER `trg_checkin_late` BEFORE INSERT ON `employee_checkin` FOR EACH ROW BEGIN
    IF NEW.CheckInTime > '08:00:00' THEN
        SET NEW.IsLate = 1;
    ELSE
        SET NEW.IsLate = 0;
    END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_checkin_only_active` BEFORE INSERT ON `employee_checkin` FOR EACH ROW BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM EMPLOYEE
        WHERE EmployeeID = NEW.EmployeeID
          AND Status = 'Active'
    ) THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Employee is not Active – cannot check in';
    END IF;
END
$$
DELIMITER ;
DELIMITER $$
CREATE TRIGGER `trg_checkin_update_payroll` AFTER INSERT ON `employee_checkin` FOR EACH ROW BEGIN
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
END
$$
DELIMITER ;

-- --------------------------------------------------------

--
-- Table structure for table `employee_payroll`
--

CREATE TABLE `employee_payroll` (
  `PayrollID` varchar(30) NOT NULL,
  `EmployeeID` varchar(30) DEFAULT NULL,
  `LateWorkday` int(11) DEFAULT 0,
  `Month` tinyint(4) DEFAULT month(curdate()),
  `Year` smallint(6) DEFAULT year(curdate()),
  `BaseSalary` decimal(12,2) DEFAULT NULL,
  `WorkDays` int(11) DEFAULT 0,
  `Bonus` decimal(12,2) DEFAULT 0.00,
  `Penalty` decimal(12,2) DEFAULT 0.00,
  `TotalSalary` decimal(12,2) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `employee_payroll`
--

INSERT INTO `employee_payroll` (`PayrollID`, `EmployeeID`, `LateWorkday`, `Month`, `Year`, `BaseSalary`, `WorkDays`, `Bonus`, `Penalty`, `TotalSalary`) VALUES
('P01', 'E01', 0, 1, 2026, 8000000.00, 0, 0.00, 0.00, NULL),
('P02', 'E02', 0, 1, 2026, 8500000.00, 0, 0.00, 0.00, NULL),
('P03', 'E03', 0, 1, 2026, 7800000.00, 0, 0.00, 0.00, NULL),
('P04', 'E04', 0, 1, 2026, 9000000.00, 0, 0.00, 0.00, NULL),
('P05', 'E06', 0, 1, 2026, 8200000.00, 0, 0.00, 0.00, NULL),
('P06', 'E07', 0, 1, 2026, 8300000.00, 0, 0.00, 0.00, NULL),
('P07', 'E08', 0, 1, 2026, 8400000.00, 0, 0.00, 0.00, NULL),
('P08', 'E09', 0, 1, 2026, 8600000.00, 0, 0.00, 0.00, NULL),
('P09', 'E10', 0, 1, 2026, 8800000.00, 0, 0.00, 0.00, NULL);

-- --------------------------------------------------------

--
-- Table structure for table `employee_penalty`
--

CREATE TABLE `employee_penalty` (
  `PenaltyID` varchar(30) NOT NULL,
  `PayrollID` varchar(30) DEFAULT NULL,
  `Description` varchar(255) DEFAULT NULL,
  `Amount` decimal(12,2) DEFAULT NULL,
  `PenaltyDate` date DEFAULT curdate()
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `employee_penalty`
--

INSERT INTO `employee_penalty` (`PenaltyID`, `PayrollID`, `Description`, `Amount`, `PenaltyDate`) VALUES
('PEP01', 'P01', 'Phạt nghỉ nhiều', 300000.00, '2026-01-08'),
('PEP02', 'P02', 'Phạt nghỉ nhiều', 300000.00, '2026-01-08'),
('PEP03', 'P03', 'Phạt nghỉ nhiều', 300000.00, '2026-01-08'),
('PEP04', 'P04', 'Phạt nghỉ nhiều', 300000.00, '2026-01-08'),
('PEP05', 'P05', 'Phạt nghỉ nhiều', 300000.00, '2026-01-08'),
('PEP06', 'P06', 'Phạt nghỉ nhiều', 300000.00, '2026-01-08'),
('PEP07', 'P07', 'Phạt nghỉ nhiều', 300000.00, '2026-01-08'),
('PEP08', 'P08', 'Phạt nghỉ nhiều', 300000.00, '2026-01-08'),
('PEP09', 'P09', 'Phạt nghỉ nhiều', 300000.00, '2026-01-08');

-- --------------------------------------------------------

--
-- Table structure for table `orderdetail`
--

CREATE TABLE `orderdetail` (
  `OrderDetailID` int(11) NOT NULL,
  `OrderID` int(11) NOT NULL,
  `ProductID` varchar(30) NOT NULL,
  `UnitPrice` decimal(12,2) NOT NULL,
  `CostPrice` decimal(12,2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

-- --------------------------------------------------------

--
-- Table structure for table `orders`
--

CREATE TABLE `orders` (
  `OrderID` int(11) NOT NULL,
  `OrderDate` date NOT NULL,
  `CustomerID` varchar(30) NOT NULL,
  `Total` decimal(12,2) NOT NULL,
  `PaymentMethod` enum('Transfer','Cash') NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `orders`
--

INSERT INTO `orders` (`OrderID`, `OrderDate`, `CustomerID`, `Total`, `PaymentMethod`) VALUES
(1, '2026-01-11', '3', 10.00, 'Cash'),
(2, '2026-01-11', '3', 20.00, 'Cash'),
(3, '2026-01-11', '2', 10.00, 'Cash'),
(4, '2026-01-11', '2', 10.00, 'Cash');

-- --------------------------------------------------------

--
-- Table structure for table `product`
--

CREATE TABLE `product` (
  `ProductID` varchar(30) NOT NULL,
  `ProductName` varchar(150) NOT NULL,
  `CategoryID` varchar(30) NOT NULL,
  `Quantity` int(11) NOT NULL,
  `Unit` varchar(20) NOT NULL,
  `Price` decimal(12,2) NOT NULL,
  `Description` varchar(255) NOT NULL,
  `Image` varchar(255) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `product`
--

INSERT INTO `product` (`ProductID`, `ProductName`, `CategoryID`, `Quantity`, `Unit`, `Price`, `Description`, `Image`) VALUES
('B01', 'Bakery', 'AC1', 20, 'Piece', 10.00, 'nothing to say', 'girl1.png');

-- --------------------------------------------------------

--
-- Stand-in structure for view `vw_employeesalary`
-- (See below for the actual view)
--
CREATE TABLE `vw_employeesalary` (
`EmployeeID` varchar(30)
,`FullName` varchar(100)
,`Month` tinyint(4)
,`Year` smallint(6)
,`WorkDays` int(11)
,`BaseSalary` decimal(12,2)
,`Bonus` decimal(12,2)
,`Penalty` decimal(12,2)
,`TotalSalary` decimal(12,2)
);

-- --------------------------------------------------------

--
-- Structure for view `vw_employeesalary`
--
DROP TABLE IF EXISTS `vw_employeesalary`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`localhost` SQL SECURITY DEFINER VIEW `vw_employeesalary`  AS SELECT `e`.`EmployeeID` AS `EmployeeID`, `e`.`FullName` AS `FullName`, `p`.`Month` AS `Month`, `p`.`Year` AS `Year`, `p`.`WorkDays` AS `WorkDays`, `p`.`BaseSalary` AS `BaseSalary`, `p`.`Bonus` AS `Bonus`, `p`.`Penalty` AS `Penalty`, `p`.`TotalSalary` AS `TotalSalary` FROM (`employee` `e` join `employee_payroll` `p` on(`e`.`EmployeeID` = `p`.`EmployeeID`)) WHERE `p`.`Month` = month(curdate()) AND `p`.`Year` = year(curdate()) ;

--
-- Indexes for dumped tables
--

--
-- Indexes for table `customer`
--
ALTER TABLE `customer`
  ADD PRIMARY KEY (`CustomerID`);

--
-- Indexes for table `employee`
--
ALTER TABLE `employee`
  ADD PRIMARY KEY (`EmployeeID`);

--
-- Indexes for table `employee_bonus`
--
ALTER TABLE `employee_bonus`
  ADD PRIMARY KEY (`BonusID`),
  ADD KEY `fk_bonus_payroll` (`PayrollID`);

--
-- Indexes for table `employee_checkin`
--
ALTER TABLE `employee_checkin`
  ADD PRIMARY KEY (`CheckInID`),
  ADD UNIQUE KEY `uq_employee_date` (`EmployeeID`,`WorkDate`);

--
-- Indexes for table `employee_payroll`
--
ALTER TABLE `employee_payroll`
  ADD PRIMARY KEY (`PayrollID`),
  ADD UNIQUE KEY `uq_employee_month_year` (`EmployeeID`,`Month`,`Year`);

--
-- Indexes for table `employee_penalty`
--
ALTER TABLE `employee_penalty`
  ADD PRIMARY KEY (`PenaltyID`),
  ADD KEY `fk_penalty_payroll` (`PayrollID`);

--
-- Indexes for table `orderdetail`
--
ALTER TABLE `orderdetail`
  ADD PRIMARY KEY (`OrderDetailID`);

--
-- Indexes for table `orders`
--
ALTER TABLE `orders`
  ADD PRIMARY KEY (`OrderID`);

--
-- Indexes for table `product`
--
ALTER TABLE `product`
  ADD PRIMARY KEY (`ProductID`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `customer`
--
ALTER TABLE `customer`
  MODIFY `CustomerID` int(10) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT for table `employee_checkin`
--
ALTER TABLE `employee_checkin`
  MODIFY `CheckInID` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `orderdetail`
--
ALTER TABLE `orderdetail`
  MODIFY `OrderDetailID` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT for table `orders`
--
ALTER TABLE `orders`
  MODIFY `OrderID` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `employee_bonus`
--
ALTER TABLE `employee_bonus`
  ADD CONSTRAINT `fk_bonus_payroll` FOREIGN KEY (`PayrollID`) REFERENCES `employee_payroll` (`PayrollID`);

--
-- Constraints for table `employee_checkin`
--
ALTER TABLE `employee_checkin`
  ADD CONSTRAINT `fk_checkin_employee` FOREIGN KEY (`EmployeeID`) REFERENCES `employee` (`EmployeeID`);

--
-- Constraints for table `employee_payroll`
--
ALTER TABLE `employee_payroll`
  ADD CONSTRAINT `fk_payroll_employee` FOREIGN KEY (`EmployeeID`) REFERENCES `employee` (`EmployeeID`);

--
-- Constraints for table `employee_penalty`
--
ALTER TABLE `employee_penalty`
  ADD CONSTRAINT `fk_penalty_payroll` FOREIGN KEY (`PayrollID`) REFERENCES `employee_payroll` (`PayrollID`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
