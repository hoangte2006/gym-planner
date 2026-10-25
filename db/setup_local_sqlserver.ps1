# Cau hinh SQL Server Express tren may cua BAN de ung dung Java ket noi duoc.
# CHAY VOI QUYEN ADMIN:  chuot phai PowerShell -> Run as administrator, roi:
#   powershell -ExecutionPolicy Bypass -File .\db\setup_local_sqlserver.ps1
#
# Script lam 4 viec (an toan, co the chay lai nhieu lan):
#   1. Bat TCP/IP, co dinh cong 1433 (JDBC khong dung duoc Shared Memory)
#   2. Bat che do dang nhap SQL Server + Windows (Mixed Mode)
#   3. Khoi dong lai dich vu SQL Server
#   4. Tao login gym_app (quyen db_owner tren database GymPlanner)
#
# Sau do mat khau nay phai trung voi db.password trong config.local.properties.

param(
    [string]$Instance = "SQLEXPRESS",
    [string]$Database = "GymPlanner",
    [string]$LoginName = "gym_app",
    [string]$Password = "Gym@Planner2026"
)

$ErrorActionPreference = "Stop"

$isAdmin = ([Security.Principal.WindowsPrincipal][Security.Principal.WindowsIdentity]::GetCurrent()
           ).IsInRole([Security.Principal.WindowsBuiltInRole]::Administrator)
if (-not $isAdmin) { throw "Hay chay PowerShell bang quyen Administrator." }

# Tim ten thu muc registry cua instance (vd MSSQL16.SQLEXPRESS)
$names = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\Instance Names\SQL"
$instId = (Get-ItemProperty $names).$Instance
if (-not $instId) { throw "Khong tim thay instance $Instance" }
$root = "HKLM:\SOFTWARE\Microsoft\Microsoft SQL Server\$instId\MSSQLServer"

Write-Host "1. Bat TCP/IP cong 1433 ..."
$tcp = "$root\SuperSocketNetLib\Tcp"
Set-ItemProperty $tcp -Name Enabled -Value 1
Get-ChildItem $tcp | ForEach-Object {
    # IP1, IP2, ... IPAll: bo cong dong, dat cong tinh 1433 o IPAll
    if ($_.PSChildName -eq "IPAll") {
        Set-ItemProperty $_.PSPath -Name TcpPort -Value "1433"
        Set-ItemProperty $_.PSPath -Name TcpDynamicPorts -Value ""
    }
}

Write-Host "2. Bat Mixed Mode (SQL + Windows) ..."
Set-ItemProperty $root -Name LoginMode -Value 2

Write-Host "3. Khoi dong lai dich vu SQL Server (mat vai giay) ..."
$svc = "MSSQL`$$Instance"
Restart-Service $svc -Force
(Get-Service $svc).WaitForStatus("Running", [TimeSpan]::FromSeconds(60))
Start-Sleep -Seconds 3

Write-Host "4. Tao login $LoginName ..."
$tsql = @"
IF NOT EXISTS (SELECT 1 FROM sys.server_principals WHERE name = N'$LoginName')
    CREATE LOGIN [$LoginName] WITH PASSWORD = N'$Password', CHECK_POLICY = OFF, DEFAULT_DATABASE = [$Database];
ELSE
    ALTER LOGIN [$LoginName] WITH PASSWORD = N'$Password', CHECK_POLICY = OFF;
IF NOT EXISTS (SELECT 1 FROM [$Database].sys.database_principals WHERE name = N'$LoginName')
    EXEC(N'USE [$Database]; CREATE USER [$LoginName] FOR LOGIN [$LoginName];');
EXEC(N'USE [$Database]; ALTER ROLE db_owner ADD MEMBER [$LoginName];');
"@
sqlcmd -S ".\$Instance" -E -C -b -Q $tsql
if ($LASTEXITCODE -ne 0) { throw "Tao login that bai." }

Write-Host ""
Write-Host "Kiem tra dang nhap bang TCP ..."
sqlcmd -S "tcp:localhost,1433" -U $LoginName -P $Password -d $Database -C -b -Q "SELECT COUNT(*) AS so_bang FROM sys.tables"
if ($LASTEXITCODE -ne 0) { throw "Khong ket noi duoc qua TCP." }
Write-Host "XONG. Sao chep config.example.properties thanh config.local.properties va dien db.password."
